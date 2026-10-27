package com.jarvis.food.data

import android.content.Context
import com.jarvis.food.engine.DayEnergy
import com.jarvis.food.engine.Energy
import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.PlannedMeal
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.Shopping
import com.jarvis.food.engine.SolvedRecipe
import com.jarvis.food.engine.toFood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Database alimenti/ricette caricato dagli asset dell'app. */
object Db {
    lateinit var db: FoodDb
        private set
    val ready get() = ::db.isInitialized

    fun init(context: Context) {
        if (ready) return
        val foods = context.assets.open("foods.json").bufferedReader().use { it.readText() }
        val recipes = context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        db = FoodDb.parse(foods, recipes)
    }

    /** Per i test sulla JVM. */
    fun set(d: FoodDb) { db = d }
}

/** Stato dell'app salvato in un unico file JSON nella memoria privata del telefono. */
object Store {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    private lateinit var file: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    val current: AppState get() = _state.value

    fun init(context: Context) {
        Db.init(context)
        // Pre-calcolo in background di conteggi e valori nutrizionali delle ricette: la scheda Ricette si apre subito.
        scope.launch { runCatching { com.jarvis.food.engine.RecipeCats.get(Db.db) } }
        if (::file.isInitialized) return
        file = File(context.filesDir, "jarvis_food.json")
        if (file.exists()) {
            runCatching { json.decodeFromString(AppState.serializer(), file.readText()) }
                .onSuccess { _state.value = migrate(it) }
                .onFailure {
                    // File corrotto: lo conservo come copia invece di perderlo.
                    file.copyTo(File(context.filesDir, "jarvis_food_corrotto_${System.currentTimeMillis()}.json"), true)
                }
        }
    }

    /** Adegua i dati salvati dalle versioni precedenti. */
    private fun migrate(s: AppState): AppState {
        var n = s
        if (n.ai.model == "openrouter/free") n = n.copy(ai = n.ai.copy(model = AiConfig.DEFAULT_MODEL))
        return n
    }

    fun update(block: (AppState) -> AppState) {
        _state.update(block)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch { delay(300); saveNow() }
    }

    @Synchronized
    fun saveNow() {
        if (!::file.isInitialized) return
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppState.serializer(), _state.value))
        tmp.renameTo(file)
    }

    fun exportJson(): String = json.encodeToString(AppState.serializer(), _state.value)

    fun importJson(text: String): Result<Unit> = runCatching {
        _state.value = migrate(json.decodeFromString(AppState.serializer(), text))
        saveNow()
    }

    fun resetAll() { _state.value = AppState(); saveNow() }

    fun newId() = UUID.randomUUID().toString()

    // ---------------------------------------------------------------- Diario

    fun addEntry(date: LocalDate, meal: Int, food: Food, grams: Double, planRef: String? = null): String {
        val id = newId()
        update { it.copy(diary = it.diary + DiaryEntry(id, date.toString(), meal, food, grams, System.currentTimeMillis(), planRef)) }
        return id
    }

    fun updateEntry(id: String, grams: Double, meal: Int) = update { s ->
        s.copy(diary = s.diary.map { if (it.id == id) it.copy(grams = grams, meal = meal) else it })
    }

    fun replaceEntry(id: String, food: Food, grams: Double) = update { s ->
        s.copy(diary = DiaryEdits.replace(s.diary, id, food, grams))
    }

    fun setTrainingNutrition(settings: TrainingNutrition) {
        require(!settings.enabled || if (settings.automatic) AutomaticTraining.calculate(current.profile, currentWeight(current), settings) != null
            else settings.on?.valid == true && settings.off?.valid == true)
        update { it.copy(trainingNutrition = settings.copy(trainingDays = settings.trainingDays.filter { d -> d in 1..7 }.distinct().sorted())) }
    }

    fun updateDayTargets(date: LocalDate, change: (DailyTargets) -> DailyTargets) = update { s ->
        val n = resolvedTraining(s)
        val target = n.target(date) ?: return@update s
        val next = change(target)
        require(next.valid)
        s.copy(trainingNutrition = if (n.isTraining(date)) n.copy(on = next, automatic = false) else n.copy(off = next, automatic = false))
    }

    fun setDayType(date: LocalDate, training: Boolean?) = update {
        it.copy(trainingNutrition = it.trainingNutrition.withOverride(date, training))
    }

    fun removeEntry(id: String) = update { s ->
        s.copy(diary = s.diary.filterNot { it.id == id },
            plan = s.plan.map { dp -> dp.copy(slots = dp.slots.map { ps -> ps.copy(eatenEntryIds = ps.eatenEntryIds - id) }) })
    }

    fun copyMeal(from: LocalDate, meal: Int, to: LocalDate, toMeal: Int) = update { s ->
        val copies = s.diary.filter { it.date == from.toString() && it.meal == meal }
            .map { it.copy(id = newId(), date = to.toString(), meal = toMeal, time = System.currentTimeMillis(), planRef = null) }
        s.copy(diary = s.diary + copies)
    }

    fun toggleFavorite(food: Food) = update { s ->
        if (s.favorites.any { it.id == food.id }) s.copy(favorites = s.favorites.filterNot { it.id == food.id })
        else s.copy(favorites = listOf(food) + s.favorites)
    }

    fun saveCustomFood(food: Food) = update { s -> s.copy(customFoods = listOf(food) + s.customFoods.filterNot { it.id == food.id }) }
    fun deleteCustomFood(id: String) = update { s ->
        s.copy(customFoods = s.customFoods.filterNot { it.id == id }, favorites = s.favorites.filterNot { it.id == id })
    }

    fun setWater(date: LocalDate, ml: Int) = update { it.copy(water = it.water + (date.toString() to max(0, ml))) }
    fun setSteps(date: LocalDate, steps: Int) = update { it.copy(steps = it.steps + (date.toString() to max(0, steps))) }

    fun addWeight(date: LocalDate, kg: Double) = update { s ->
        val list = (s.weights.filterNot { it.date == date.toString() } + WeightEntry(date.toString(), kg)).sortedBy { it.date }
        val ns = s.copy(weights = list)
        if (ns.goals.auto) ns.copy(goals = Nutrition.autoGoals(ns.profile, list.last().kg, ns.goals)) else ns
    }

    fun removeWeight(date: String) = update { s -> s.copy(weights = s.weights.filterNot { it.date == date }) }

    fun addMeasurement(date: LocalDate, type: String, value: Double) = update { s ->
        s.copy(measurements = s.measurements.filterNot { it.date == date.toString() && it.type == type } + Measurement(date.toString(), type, value))
    }

    fun addActivity(date: LocalDate, name: String, minutes: Int, kcal: Int) = update {
        it.copy(activities = it.activities + ActivityEntry(newId(), date.toString(), name, minutes, kcal))
    }

    fun removeActivity(id: String) = update { s -> s.copy(activities = s.activities.filterNot { it.id == id }) }

    fun saveRecipe(r: Recipe) = update { s -> s.copy(recipes = listOf(r) + s.recipes.filterNot { it.id == r.id }) }
    fun deleteRecipe(id: String) = update { s -> s.copy(recipes = s.recipes.filterNot { it.id == id }) }

    fun startFast(planId: String, hours: Int, start: Long = System.currentTimeMillis()) = update {
        it.copy(fasting = it.fasting.copy(planId = planId, targetHours = hours, activeStart = start))
    }

    fun stopFast(save: Boolean) = update { s ->
        val f = s.fasting
        val start = f.activeStart ?: return@update s
        val hist = if (save && System.currentTimeMillis() - start > 60_000)
            (listOf(FastingSession(start, System.currentTimeMillis(), f.targetHours)) + f.history).take(200) else f.history
        s.copy(fasting = f.copy(activeStart = null, history = hist))
    }

    // ---------------------------------------------------------------- Letture

    fun entriesFor(s: AppState, date: LocalDate) = s.diary.filter { it.date == date.toString() }
    fun currentWeight(s: AppState) = s.weights.lastOrNull()?.kg ?: s.profile.startWeight

    fun resolvedTraining(s: AppState): TrainingNutrition {
        val n = s.trainingNutrition
        if (!n.enabled || !n.automatic) return n
        val estimate = AutomaticTraining.calculate(s.profile, currentWeight(s), n) ?: return n.copy(enabled = false)
        return n.copy(on = estimate.on, off = estimate.off)
    }

    /** Fabbisogno del giorno con l'eventuale adattamento all'attività. */
    fun energyFor(s: AppState, date: LocalDate): DayEnergy {
        val frac = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 86400.0 else 1.0
        val w = currentWeight(s)
        val base = Energy.forDay(
            s.profile, s.goals, s.energy, w, s.health[date.toString()],
            s.activities.filter { it.date == date.toString() }, s.steps[date.toString()] ?: 0, frac,
            Energy.calibration(s.profile, w, s.energy, s.health, date)
        )
        val n = resolvedTraining(s)
        val result = n.apply(base, date)
        return if (n.enabled && n.automatic) result.copy(explanation = listOf(
            "Stima automatica ON/OFF dal profilo e dall'allenamento configurato; cambia con peso e impostazioni.") + result.explanation)
        else if (s.trainingNutrition.enabled && s.trainingNutrition.automatic && !n.enabled)
            base.copy(explanation = listOf("ON/OFF automatico non disponibile con i dati attuali: rivedi Profilo e Giorni ON e OFF. Uso gli obiettivi generali.") + base.explanation)
        else result
    }

    /** Calorie bruciate con l'attività da mostrare nel diario (non sempre aggiunte al budget: vedi [DayEnergy]). */
    fun burnedFor(s: AppState, date: LocalDate): Int {
        val h = s.health[date.toString()]
        return if (s.energy.mode == 2 && h != null) energyFor(s, date).activeRaw.roundToInt()
        else s.activities.filter { it.date == date.toString() }.sumOf { it.kcal } + Nutrition.stepsKcal(s.steps[date.toString()] ?: 0, currentWeight(s))
    }

    fun stepsFor(s: AppState, date: LocalDate): Long =
        if (s.energy.mode == 2) s.health[date.toString()]?.steps ?: 0 else (s.steps[date.toString()] ?: 0).toLong()

    fun recentFoods(s: AppState, limit: Int = 30): List<Pair<Food, Double>> =
        s.diary.sortedByDescending { it.time }.distinctBy { it.food.id }.take(limit).map { it.food to it.grams }

    fun frequentFoods(s: AppState, meal: Int, limit: Int = 8): List<Pair<Food, Double>> =
        s.diary.filter { it.meal == meal }.groupBy { it.food.id }.entries.sortedByDescending { it.value.size }.take(limit)
            .map { e -> e.value.last().food to (e.value.map { it.grams }.average().roundToInt().toDouble()) }

    // ---------------------------------------------------------------- Piano alimentare

    fun dayPlan(s: AppState, date: LocalDate): DayPlan? = s.plan.firstOrNull { it.date == date.toString() }

    private val mealCache = HashMap<String, List<PlannedMeal>>()

    /** Pasti pianificati con grammature calcolate sull'obiettivo del giorno (con cache). */
    fun plannedMeals(s: AppState, date: LocalDate): List<PlannedMeal> {
        val dp = dayPlan(s, date) ?: return emptyList()
        val e = energyFor(s, date)
        val key = "${dp.hashCode()}|${s.dietPrefs.hashCode()}|${e.macro}"
        synchronized(mealCache) {
            mealCache[key]?.let { return it }
            val r = Planner.solveDay(Db.db, dp, e.macro, s.dietPrefs)
            if (mealCache.size > 60) mealCache.clear()
            mealCache[key] = r
            return r
        }
    }

    /** Crea il piano per 7 giorni da [start], mantenendo i giorni precedenti. */
    fun generatePlan(start: LocalDate, days: Int = 7) = update { s ->
        val e = energyFor(s, start)
        val base = com.jarvis.food.engine.Macro(e.baseKcal, e.protein, e.carbs - e.extraKcal / 4, e.fat)
        val newDays = Planner.generate(Db.db, s.dietPrefs, start, days, base, s.plan,
            dayTargetForDate = if (s.trainingNutrition.active) ({ date: LocalDate -> energyFor(s, date).macro }) else null)
        val keepEaten = s.plan.filter { dp -> LocalDate.parse(dp.date) in start..start.plusDays(days - 1L) && dp.slots.any { it.eatenEntryIds.isNotEmpty() } }
        val merged = newDays.map { nd ->
            val old = keepEaten.firstOrNull { it.date == nd.date } ?: return@map nd
            nd.copy(slots = nd.slots.map { ns -> old.slots.firstOrNull { it.slot == ns.slot && it.eatenEntryIds.isNotEmpty() } ?: ns })
        }
        val kept = s.plan.filter { dp -> LocalDate.parse(dp.date) !in start..start.plusDays(days - 1L) && LocalDate.parse(dp.date) >= LocalDate.now().minusDays(60) }
        s.copy(plan = (kept + merged).sortedBy { it.date })
    }

    fun setSlotRecipe(date: LocalDate, slot: Int, recipeId: String) = update { s ->
        s.copy(plan = s.plan.map { dp ->
            if (dp.date != date.toString()) dp
            else dp.copy(slots = dp.slots.map { if (it.slot == slot) it.copy(recipeId = recipeId) else it })
        })
    }

    /** Registra nel diario il pasto pianificato, come piatto unico con le grammature calcolate. */
    fun eatPlanned(date: LocalDate, meal: PlannedMeal) {
        val id = addEntry(date, meal.diaryMeal, meal.solved.toFood(), meal.solved.grams, "${date}#${meal.slotIndex}")
        update { s ->
            s.copy(plan = s.plan.map { dp ->
                if (dp.date != date.toString()) dp
                else dp.copy(slots = dp.slots.map { if (it.slot == meal.slotIndex) it.copy(eatenEntryIds = it.eatenEntryIds + id) else it })
            })
        }
    }

    fun eatSolved(date: LocalDate, diaryMeal: Int, solved: SolvedRecipe) = addEntry(date, diaryMeal, solved.toFood(), solved.grams)

    fun uneatPlanned(date: LocalDate, slot: Int) {
        val ids = dayPlan(current, date)?.slots?.firstOrNull { it.slot == slot }?.eatenEntryIds ?: return
        ids.forEach { removeEntry(it) }
    }

    fun setDietPrefs(p: DietPrefs) = update { MealSchedule.apply(it, p) }

    // ---------------------------------------------------------------- Dispensa e spesa

    fun addPantry(items: List<PantryItem>) = update { s ->
        // Stesso alimento già presente: sommo le quantità.
        var list = s.pantry
        for (it in items) {
            val ex = list.firstOrNull { p -> p.foodId != null && p.foodId == it.foodId }
            list = if (ex != null) list.map { p -> if (p.id == ex.id) p.copy(grams = sumNullable(p.grams, it.grams), addedAt = it.addedAt) else p }
            else list + it
        }
        s.copy(pantry = list)
    }

    private fun sumNullable(a: Double?, b: Double?) = if (a == null && b == null) null else (a ?: 0.0) + (b ?: 0.0)

    fun updatePantry(item: PantryItem) = update { s -> s.copy(pantry = s.pantry.map { if (it.id == item.id) item else it }) }
    fun removePantry(id: String) = update { s -> s.copy(pantry = s.pantry.filterNot { it.id == id }) }
    fun clearPantry() = update { it.copy(pantry = emptyList()) }

    /** Rigenera la lista della spesa per i pasti pianificati nell'intervallo indicato. */
    fun buildShopping(from: LocalDate, days: Int) {
        val s = current
        val meals = (0 until days).flatMap { d -> plannedMeals(s, from.plusDays(d.toLong())).map { it.solved } }
        val list = Shopping.build(Db.db, meals, s.pantry, s.shopping, ::newId)
        update { it.copy(shopping = list, shoppingRange = "$from|$days") }
    }

    fun toggleShopping(id: String) = update { s -> s.copy(shopping = s.shopping.map { if (it.id == id) it.copy(checked = !it.checked) else it }) }
    fun addShoppingManual(name: String) = update { s ->
        s.copy(shopping = s.shopping + ShoppingItem(newId(), null, name.trim(), shop = "Altro", manual = true))
    }
    fun removeShopping(id: String) = update { s -> s.copy(shopping = s.shopping.filterNot { it.id == id }) }

    /** Articoli spuntati -> in dispensa, poi tolti dalla lista. */
    fun moveCheckedToPantry() {
        val checked = current.shopping.filter { it.checked }
        addPantry(checked.map {
            PantryItem(newId(), it.foodId, it.name, if (it.foodId != null) it.packs * it.packGrams else null, source = "spesa")
        })
        update { s -> s.copy(shopping = s.shopping.filterNot { it.checked }) }
    }

    /** Dopo aver mangiato un pasto pianificato, scala gli ingredienti dalla dispensa (se presenti). */
    fun consumeFromPantry(solved: SolvedRecipe) = update { s ->
        s.copy(pantry = s.pantry.mapNotNull { p ->
            val used = solved.items.filter { "db:" + it.food.id == p.foodId || it.food.id == p.foodId }.sumOf { it.grams }
            when {
                used <= 0 || p.grams == null -> p
                p.grams - used <= 1 -> null
                else -> p.copy(grams = p.grams - used)
            }
        })
    }

    // ---------------------------------------------------------------- Ricette: preferiti, recenti, piano, spesa

    fun toggleRecipeFav(id: String) = update { s ->
        s.copy(recipeFavs = if (id in s.recipeFavs) s.recipeFavs - id else listOf(id) + s.recipeFavs)
    }

    fun recipeViewed(id: String) = update { s ->
        s.copy(recipeRecent = (listOf(id) + s.recipeRecent.filter { it != id }).take(12), recipeViews = s.recipeViews + (id to ((s.recipeViews[id] ?: 0) + 1)))
    }

    /** Mette una ricetta del catalogo nel piano di un giorno (sostituisce quella dello stesso pasto, o aggiunge il pasto). */
    fun planRecipe(date: LocalDate, slot: Int, recipeId: String) = update { s ->
        val d = date.toString()
        val existing = s.plan.firstOrNull { it.date == d }
        val updated = if (existing == null) DayPlan(d, listOf(PlanSlot(slot, recipeId)))
        else if (existing.slots.any { it.slot == slot }) existing.copy(slots = existing.slots.map { if (it.slot == slot) it.copy(recipeId = recipeId, eatenEntryIds = emptyList()) else it })
        else existing.copy(slots = (existing.slots + PlanSlot(slot, recipeId)).sortedBy { it.slot })
        s.copy(plan = (s.plan.filterNot { it.date == d } + updated).sortedBy { it.date })
    }

    /** Aggiunge gli ingredienti di una ricetta alla lista della spesa (voci manuali: non vengono perse se la lista si rigenera). */
    fun addRecipeToShopping(solved: SolvedRecipe, label: String, pantryFoodIds: Set<String>): Int {
        val add = solved.items.filter { it.ing.role != "x" && it.food.id !in pantryFoodIds && it.food.id !in Shopping.STAPLES }.map {
            ShoppingItem(newId(), it.food.id, it.food.name, needGrams = Math.round(it.grams).toDouble(), packs = 1, packGrams = it.food.pack ?: 100.0,
                shop = it.food.shop, manual = true, note = "${it.amountLabel()} · $label")
        }
        if (add.isNotEmpty()) update { s -> s.copy(shopping = s.shopping + add) }
        return add.size
    }

    /** Aggiunge alla spesa gli ingredienti di una ricetta dell'utente (per [factor] volte la ricetta), saltando ciò che c'è già in dispensa. */
    fun addUserRecipeToShopping(r: Recipe, factor: Double, pantryFoodIds: Set<String>): Int {
        val add = r.ingredients.mapNotNull { ing ->
            val dbId = if (ing.food.id.startsWith("db:")) ing.food.id.removePrefix("db:") else null
            if (dbId != null && (dbId in pantryFoodIds || dbId in Shopping.STAPLES)) return@mapNotNull null
            val d = dbId?.let { Db.db.food(it) }
            val g = Math.round(ing.grams * factor).toDouble()
            ShoppingItem(newId(), dbId, ing.food.name, needGrams = g, packs = 1, packGrams = d?.pack ?: 100.0,
                shop = d?.shop ?: "Altro", manual = true, note = "${g.toInt()} ${if (ing.food.liquid) "ml" else "g"} · ${r.name}")
        }
        if (add.isNotEmpty()) update { s -> s.copy(shopping = s.shopping + add) }
        return add.size
    }

    fun setEnergy(e: EnergySettings) = update { it.copy(energy = e) }
}
