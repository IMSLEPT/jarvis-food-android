package com.jarvis.food.chat

import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.FastingPlans
import com.jarvis.food.data.Food
import com.jarvis.food.data.Ingredient
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.PantryItem
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.RecipeFilter
import com.jarvis.food.engine.RecipeMeta
import com.jarvis.food.engine.RecipeSearch
import com.jarvis.food.engine.RecipeTemplate
import com.jarvis.food.engine.Solver
import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.original
import com.jarvis.food.engine.scaled
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.QStep
import com.jarvis.food.ui.Screen
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

data class ActionCall(val name: String, val args: JSONObject)

/** Esito di un'azione eseguita nell'app. [retry]: l'esito va comunicato al modello perché possa correggersi. */
data class ActionResult(val ok: Boolean, val text: String, val retry: Boolean = false,
                        /** Se valorizzato l'azione NON è stata eseguita: elimina dati e aspetta la conferma dell'utente (testo = cosa verrebbe eliminato). */
                        val confirm: String? = null)

data class Parsed(val text: String, val actions: List<ActionCall>, val quick: List<String>)

/** Protocollo testuale con cui Jarvis agisce nell'app: blocchi <action>{...}</action> e risposte rapide <quick>...</quick>. */
object Actions {
    private val actionRe = Regex("<action>(.*?)</action>", RegexOption.DOT_MATCHES_ALL)
    private val quickRe = Regex("<quick>(.*?)</quick>", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): Parsed {
        val acts = actionRe.findAll(raw).mapNotNull { m ->
            val body = m.groupValues[1]
            val s = body.indexOf('{'); val e = body.lastIndexOf('}')
            if (s < 0 || e <= s) return@mapNotNull null
            runCatching {
                val o = JSONObject(body.substring(s, e + 1))
                ActionCall(o.getString("name").trim(), o.optJSONObject("args") ?: JSONObject())
            }.getOrNull()
        }.toList()
        val quick = quickRe.findAll(raw).map { it.groupValues[1].trim() }.filter { it.isNotBlank() && it.length <= 60 }.distinct().take(4).toList()
        val text = visible(raw)
        return Parsed(text, acts, quick)
    }

    /** Testo da mostrare: senza blocchi azione/risposte rapide, anche se ancora incompleti durante lo streaming. */
    fun visible(raw: String): String {
        var t = actionRe.replace(raw, "")
        t = quickRe.replace(t, "")
        val cut = listOf(t.indexOf("<action"), t.indexOf("<quick")).filter { it >= 0 }.minOrNull()
        if (cut != null) t = t.substring(0, cut)
        return t.replace("```json", "").replace("```", "").trim()
    }


    // ------------------------------------------------------------------ Esecuzione

    private fun JSONObject.str(k: String): String? = optString(k, "").trim().takeIf { it.isNotEmpty() && it != "null" }
    private fun JSONObject.num(k: String): Double? {
        if (!has(k) || isNull(k)) return null
        val v = opt(k)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
            else -> null
        }
    }
    private fun JSONObject.flag(k: String): Boolean? {
        if (!has(k) || isNull(k)) return null
        return when (val v = opt(k)) { is Boolean -> v; is String -> v.lowercase() in setOf("true", "si", "sì", "yes", "1"); is Number -> v.toInt() != 0; else -> null }
    }
    private fun JSONObject.list(k: String): List<String> {
        val a = optJSONArray(k) ?: return str(k)?.let { listOf(it) } ?: emptyList()
        return (0 until a.length()).mapNotNull { a.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
    }

    private fun byHour(h: Int) = when (h) { in 5..10 -> 0; in 11..15 -> 1; in 16..17 -> 3; in 18..22 -> 2; else -> 5 }

    /** Pasto riconosciuto dal testo (6 pasti del diario), null se non indicato o non riconosciuto. */
    fun parseMeal(s: String?, hour: Int = LocalTime.now().hour): Int? {
        val t = FoodDb.normalize(s ?: "")
        if (t.isBlank()) return null
        return when {
            "colazione" in t || "breakfast" in t -> 0
            "pranzo" in t || "lunch" in t -> 1
            "dopocena" in t || "serale" in t || "spuntino sera" in t -> 5
            "cena" in t || "dinner" in t -> 2
            "mattutin" in t || "mattina" in t || "meta mattina" in t -> 4
            "merenda" in t || "pomerigg" in t -> 3
            "spuntino" in t || "snack" in t -> when { hour < 12 -> 4; hour < 18 -> 3; else -> 5 }
            else -> null
        }
    }
    fun mealIndex(s: String?, hour: Int = LocalTime.now().hour): Int = parseMeal(s, hour) ?: byHour(hour)

    private fun dateOf(s: String?): LocalDate = when (s?.lowercase()?.trim()) {
        null, "", "oggi" -> LocalDate.now()
        "ieri" -> LocalDate.now().minusDays(1)
        "altroieri" -> LocalDate.now().minusDays(2)
        "domani" -> LocalDate.now().plusDays(1)
        else -> runCatching { LocalDate.parse(s) }.getOrDefault(LocalDate.now())
    }

    private fun dateLabel(d: LocalDate): String = when (d) {
        LocalDate.now() -> "oggi"; LocalDate.now().minusDays(1) -> "ieri"; LocalDate.now().plusDays(1) -> "domani"; else -> d.toString()
    }

    private fun findFood(name: String, st: AppState): Food? {
        st.customFoods.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        val db = Db.db
        db.search(name, 3).firstOrNull()?.let { return it.toFood() }
        db.match(name)?.let { return it.toFood() }
        // Prova con la prima parola significativa ("petto di pollo alla griglia" → "pollo").
        name.split(" ").filter { it.length > 3 }.forEach { w -> db.search(w, 1).firstOrNull()?.let { return it.toFood() } }
        return st.customFoods.firstOrNull { it.name.contains(name, ignoreCase = true) }
    }

    private fun recompute(extra: (AppState) -> AppState = { it }) {
        Store.update { st0 ->
            val st = extra(st0)
            if (st.goals.auto) st.copy(goals = Nutrition.autoGoals(st.profile, Store.currentWeight(st), st.goals)) else st
        }
    }

    private fun f0(d: Double) = d.roundToInt().toString()

    private fun words(q: String) = FoodDb.normalize(q).split(" ").filter { it.length >= 3 }.ifEmpty { listOf(FoodDb.normalize(q)) }

    private fun nameMatches(name: String, q: String): Boolean {
        val n = FoodDb.normalize(name)
        val w = words(q)
        return w.all { it in n } || w.all { n.contains(it.take(5)) }
    }

    private fun describe(e: com.jarvis.food.data.DiaryEntry) =
        "${e.food.name} ${f0(e.grams)} g (${com.jarvis.food.data.MEAL_NAMES[e.meal.coerceIn(0, 5)].lowercase()}, ${dateLabel(LocalDate.parse(e.date))})"

    private fun entriesMatching(st: AppState, date: LocalDate, q: String, meal: Int?) =
        Store.entriesFor(st, date).filter { (meal == null || it.meal == meal) && nameMatches(it.food.name, q) }

    private class RecipeRef(val tpl: RecipeTemplate?, val mine: Recipe?) {
        val id get() = tpl?.id ?: mine!!.id
        val name get() = tpl?.name ?: mine!!.name
    }

    /** Trova una ricetta per nome: esatta, oppure l'unica che contiene tutte le parole; se sono più d'una restituisce l'elenco. */
    private fun resolveRecipe(name: String, st: AppState): Pair<RecipeRef?, ActionResult?> {
        val q = FoodDb.normalize(name)
        if (q.isBlank()) return null to ActionResult(false, "Manca il nome della ricetta.", true)
        st.recipes.firstOrNull { FoodDb.normalize(it.name) == q }?.let { return RecipeRef(null, it) to null }
        val db = Db.db
        db.recipes.firstOrNull { FoodDb.normalize(it.name) == q }?.let { return RecipeRef(it, null) to null }
        val cands = st.recipes.filter { nameMatches(it.name, name) }.map { RecipeRef(null, it) } +
                db.recipes.filter { t -> val n = FoodDb.normalize(t.name); words(name).all { it in n } }.map { RecipeRef(it, null) }
        return when {
            cands.isEmpty() -> null to ActionResult(false, "Non trovo nessuna ricetta chiamata \"$name\".", true)
            cands.size == 1 -> cands[0] to null
            else -> null to ActionResult(false, "Ho trovato più ricette (${cands.size}): " + cands.sortedBy { it.name.length }.take(5).joinToString("; ") { it.name } + ". Chiedi all'utente quale intende.", true)
        }
    }

    private fun targetOf(t: RecipeTemplate): Macro = RecipeMeta.info(Db.db, t).let { Macro(it.kcal, it.protein, it.carbs, it.fat) }

    private fun mealFor(recipeMeal: String): Int? = when (recipeMeal) { "colazione" -> 0; "pranzo" -> 1; "cena" -> 2; "dessert" -> 5; else -> null }

    private fun needConfirm(label: String) = ActionResult(false, label, confirm = label)

    private val EXCLUDE_TAGS = setOf("lattosio", "glutine", "pesce", "crostacei", "carne", "maiale", "uova", "frutta_secca", "soia")

    private fun pickKeys(options: List<Pair<String, String>>, inputs: List<String>): List<String> = inputs.mapNotNull { i ->
        val n = FoodDb.normalize(i).replace(" ", "_")
        options.firstOrNull { it.first == n || FoodDb.normalize(it.second).replace(" ", "_").contains(n) && n.length >= 4 }?.first
    }.distinct()

    /** Azioni che eliminano dati: non vengono eseguite finché l'utente non conferma nella chat. */
    val DESTRUCTIVE = setOf("delete_entry", "clear_meal", "delete_recipe", "delete_weight", "delete_activity", "remove_pantry", "remove_shopping")

    /** Esegue una azione. Va chiamata sul thread principale (può cambiare la schermata). [confirmed]: l'utente ha confermato l'eliminazione. */
    fun execute(a: ActionCall, confirmed: Boolean = false): ActionResult =
        runCatching { run(a, confirmed) }.getOrElse { ActionResult(false, "Non sono riuscito a eseguire ${a.name}: ${it.message}") }

    private fun run(a: ActionCall, confirmed: Boolean): ActionResult {
        val args = a.args
        val st = Store.current
        val pantryIds = st.pantry.mapNotNull { it.foodId }.toSet()
        return when (a.name) {
            "log_food" -> {
                val name = args.str("food") ?: return ActionResult(false, "Manca il nome dell'alimento.", true)
                val grams = args.num("grams")?.takeIf { it in 1.0..3000.0 }
                    ?: return ActionResult(false, "Quantità mancante o non valida per $name.", true)
                val food = findFood(name, st) ?: return ActionResult(false, "Non trovo \"$name\" nel database alimenti.", true)
                val date = dateOf(args.str("date")); val meal = mealIndex(args.str("meal"))
                Store.addEntry(date, meal, food, grams)
                val kcal = Nutrition.of(food, grams).kcal
                ActionResult(true, "Registrati ${f0(grams)} g di ${food.name} (${f0(kcal)} kcal) a ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()}")
            }
            "log_recipe" -> {
                val (ref, err) = resolveRecipe(args.str("recipe") ?: "", st)
                if (ref == null) return err!!
                val portions = (args.num("portions") ?: 1.0).coerceIn(0.25, 20.0)
                val date = dateOf(args.str("date"))
                val meal = parseMeal(args.str("meal")) ?: ref.tpl?.let { mealFor(it.meal) } ?: byHour(LocalTime.now().hour)
                val kcal: Double
                if (ref.tpl != null) {
                    val t = ref.tpl
                    val solved = Solver.original(t, Db.db, targetOf(t)).scaled(portions)
                    Store.eatSolved(date, meal, solved)
                    kcal = RecipeMeta.info(Db.db, t).kcal * portions
                } else {
                    val r = ref.mine!!
                    val food = Nutrition.recipeAsFood(r)
                    Store.addEntry(date, meal, food, portions * (food.servingGrams ?: 100.0))
                    kcal = Nutrition.recipeTotals(r).kcal / r.servings.coerceAtLeast(1) * portions
                }
                ActionResult(true, "Registrata la ricetta ${ref.name} (${portions.toString().removeSuffix(".0").replace(".", ",")} porz., ${f0(kcal)} kcal) a ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()}")
            }
            "delete_entry" -> {
                val q = args.str("food") ?: return ActionResult(false, "Manca l'alimento da eliminare.", true)
                val date = dateOf(args.str("date")); val meal = parseMeal(args.str("meal"))
                val found = entriesMatching(st, date, q, meal)
                if (found.isEmpty()) return ActionResult(false, "Non trovo \"$q\" nel diario di ${dateLabel(date)}.", true)
                val label = "Eliminare dal diario: " + found.joinToString("; ") { describe(it) }
                if (!confirmed) return needConfirm(label)
                found.forEach { Store.removeEntry(it.id) }
                ActionResult(true, "Eliminato dal diario: " + found.joinToString("; ") { describe(it) })
            }
            "edit_entry" -> {
                val q = args.str("food") ?: return ActionResult(false, "Manca l'alimento da modificare.", true)
                val date = dateOf(args.str("date")); val meal = parseMeal(args.str("meal"))
                val found = entriesMatching(st, date, q, meal)
                if (found.isEmpty()) return ActionResult(false, "Non trovo \"$q\" nel diario di ${dateLabel(date)}.", true)
                val e = found.maxByOrNull { it.time }!!
                val g = args.num("grams")?.takeIf { it in 1.0..3000.0 }
                val nm = parseMeal(args.str("new_meal"))
                if (g == null && nm == null) return ActionResult(false, "Dimmi la nuova quantità (grams) o il nuovo pasto (new_meal).", true)
                Store.updateEntry(e.id, g ?: e.grams, nm ?: e.meal)
                ActionResult(true, "Modificato: ${e.food.name} → ${f0(g ?: e.grams)} g a ${com.jarvis.food.data.MEAL_NAMES[nm ?: e.meal].lowercase()}" +
                        if (found.size > 1) " (ce n'erano ${found.size}: ho cambiato l'ultimo inserito)" else "")
            }
            "copy_meal" -> {
                val from = parseMeal(args.str("from_meal")) ?: return ActionResult(false, "Quale pasto devo copiare (from_meal)?", true)
                val fromDate = dateOf(args.str("from_date") ?: "ieri"); val toDate = dateOf(args.str("to_date"))
                val to = parseMeal(args.str("to_meal")) ?: from
                val n = Store.entriesFor(st, fromDate).count { it.meal == from }
                if (n == 0) return ActionResult(false, "Non c'è nessun alimento a ${com.jarvis.food.data.MEAL_NAMES[from].lowercase()} di ${dateLabel(fromDate)}.")
                Store.copyMeal(fromDate, from, toDate, to)
                ActionResult(true, "Copiati $n alimenti: ${com.jarvis.food.data.MEAL_NAMES[from].lowercase()} di ${dateLabel(fromDate)} → ${com.jarvis.food.data.MEAL_NAMES[to].lowercase()} di ${dateLabel(toDate)}")
            }
            "clear_meal" -> {
                val meal = parseMeal(args.str("meal")) ?: return ActionResult(false, "Quale pasto devo svuotare?", true)
                val date = dateOf(args.str("date"))
                val found = Store.entriesFor(st, date).filter { it.meal == meal }
                if (found.isEmpty()) return ActionResult(false, "A ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()} di ${dateLabel(date)} non c'è niente da eliminare.")
                val label = "Svuotare ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()} di ${dateLabel(date)} (${found.size} alimenti: ${found.joinToString(", ") { it.food.name }})"
                if (!confirmed) return needConfirm(label)
                found.forEach { Store.removeEntry(it.id) }
                ActionResult(true, "Svuotato ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()} di ${dateLabel(date)} (${found.size} alimenti)")
            }
            "add_water" -> {
                val ml = args.num("ml")?.toInt()?.takeIf { it in 50..3000 } ?: return ActionResult(false, "Quantità d'acqua non valida.", true)
                val date = dateOf(args.str("date"))
                Store.setWater(date, (st.water[date.toString()] ?: 0) + ml)
                ActionResult(true, "Aggiunti $ml ml di acqua")
            }
            "log_steps" -> {
                val n = args.num("steps")?.toInt()?.takeIf { it in 100..80000 } ?: return ActionResult(false, "Numero di passi non valido.", true)
                Store.setSteps(dateOf(args.str("date")), n)
                ActionResult(true, "Passi di ${dateLabel(dateOf(args.str("date")))} impostati a $n")
            }
            "log_weight" -> {
                val kg = args.num("kg")?.takeIf { it in 30.0..300.0 } ?: return ActionResult(false, "Peso non valido.", true)
                Store.addWeight(dateOf(args.str("date")), kg)
                ActionResult(true, "Peso registrato: ${"%.1f".format(Locale.ITALY, kg)} kg")
            }
            "delete_weight" -> {
                val date = dateOf(args.str("date"))
                val w = st.weights.firstOrNull { it.date == date.toString() } ?: return ActionResult(false, "Non c'è nessun peso registrato per ${dateLabel(date)}.")
                val label = "Eliminare il peso di ${dateLabel(date)} (${"%.1f".format(Locale.ITALY, w.kg)} kg)"
                if (!confirmed) return needConfirm(label)
                Store.removeWeight(date.toString())
                ActionResult(true, "Eliminato il peso di ${dateLabel(date)}")
            }
            "log_activity" -> {
                val name = args.str("name") ?: "Attività"
                val min = args.num("minutes")?.toInt()?.takeIf { it in 1..600 } ?: return ActionResult(false, "Durata non valida.", true)
                val kcal = args.num("kcal")?.toInt() ?: Nutrition.activityKcal(5.0, Store.currentWeight(st), min)
                Store.addActivity(dateOf(args.str("date")), name, min, kcal)
                ActionResult(true, "Attività registrata: $name, $min min (~$kcal kcal)")
            }
            "delete_activity" -> {
                val date = dateOf(args.str("date")); val q = args.str("name")
                val found = st.activities.filter { it.date == date.toString() && (q == null || nameMatches(it.name, q)) }
                if (found.isEmpty()) return ActionResult(false, "Non trovo attività${q?.let { " \"$it\"" } ?: ""} di ${dateLabel(date)}.", true)
                val label = "Eliminare l'attività di ${dateLabel(date)}: " + found.joinToString("; ") { "${it.name} ${it.minutes} min" }
                if (!confirmed) return needConfirm(label)
                found.forEach { Store.removeActivity(it.id) }
                ActionResult(true, "Attività eliminate: " + found.joinToString("; ") { it.name })
            }
            "set_goal" -> {
                val g = when (args.str("goal")?.lowercase()) {
                    "dimagrire", "perdere peso" -> -1
                    "mantenere", "mantenimento" -> 0
                    "aumentare", "massa" -> 1
                    else -> return ActionResult(false, "Obiettivo non riconosciuto (dimagrire, mantenere, aumentare).", true)
                }
                val rate = (args.num("kg_per_week") ?: st.profile.weeklyRate).coerceIn(0.25, 1.0)
                recompute { it.copy(profile = it.profile.copy(goal = g, weeklyRate = if (g == 0) it.profile.weeklyRate else rate)) }
                val g2 = Store.current.goals.kcal
                ActionResult(true, "Obiettivo: ${when (g) { -1 -> "dimagrire"; 1 -> "aumentare massa"; else -> "mantenere" }}" +
                        (if (g != 0) " (${"%.2f".format(Locale.ITALY, rate)} kg a settimana)" else "") + " · ora $g2 kcal")
            }
            "set_target_weight" -> {
                val kg = args.num("kg")?.takeIf { it in 30.0..300.0 } ?: return ActionResult(false, "Peso obiettivo non valido.", true)
                Store.update { it.copy(profile = it.profile.copy(targetWeight = kg)) }
                ActionResult(true, "Peso obiettivo: ${"%.1f".format(Locale.ITALY, kg)} kg")
            }
            "set_calories" -> {
                val kcal = args.num("kcal")?.toInt()?.takeIf { it in 1000..6000 } ?: return ActionResult(false, "Calorie non valide (1000-6000).", true)
                Store.update { it.copy(goals = it.goals.copy(auto = false, kcal = kcal)) }
                ActionResult(true, "Calorie impostate a mano: $kcal kcal al giorno (calcolo automatico spento)")
            }
            "use_auto_calc" -> {
                Store.update { it.copy(goals = Nutrition.autoGoals(it.profile, Store.currentWeight(it), it.goals.copy(auto = true))) }
                ActionResult(true, "Calcolo automatico riattivato: ${Store.current.goals.kcal} kcal")
            }
            "set_water_goal" -> {
                val ml = args.num("ml")?.toInt()?.takeIf { it in 500..6000 } ?: return ActionResult(false, "Obiettivo acqua non valido (500-6000 ml).", true)
                Store.update { it.copy(goals = it.goals.copy(waterMl = ml)) }
                ActionResult(true, "Obiettivo acqua: $ml ml al giorno")
            }
            "set_steps_goal" -> {
                val n = args.num("steps")?.toInt()?.takeIf { it in 1000..40000 } ?: return ActionResult(false, "Obiettivo passi non valido (1000-40000).", true)
                Store.update { it.copy(goals = it.goals.copy(steps = n)) }
                ActionResult(true, "Obiettivo passi: $n al giorno")
            }
            "set_macros" -> {
                val preset = args.str("preset")?.lowercase()?.replace(" ", "")?.replace("_", "")
                val idx = when (preset) { "bilanciato" -> 0; "proteico", "piuproteine" -> 1; "lowcarb", "pochicarboidrati" -> 2; else -> null }
                if (idx != null && st.goals.auto) {
                    recompute { it.copy(profile = it.profile.copy(dietStyle = idx)) }
                    val g = Store.current.goals
                    ActionResult(true, "Macro ${Nutrition.PRESETS[idx].name}: ${g.carbsPct}% C · ${g.proteinPct}% P · ${g.fatPct}% G")
                } else {
                    val (c, p, f) = if (idx != null) Nutrition.PRESETS[idx].let { Triple(it.carbs, it.protein, it.fat) }
                    else Triple(args.num("carbs")?.toInt(), args.num("protein")?.toInt(), args.num("fat")?.toInt()).let { t ->
                        if (t.first == null || t.second == null || t.third == null) return ActionResult(false, "Servono preset oppure carbs, protein e fat in percentuale.", true)
                        Triple(t.first!!, t.second!!, t.third!!)
                    }
                    if (c + p + f !in 99..101 || minOf(c, p, f) < 5) return ActionResult(false, "Le percentuali devono sommare 100 (ognuna almeno 5).", true)
                    Store.update { it.copy(goals = it.goals.copy(auto = false, carbsPct = c, proteinPct = p, fatPct = f)) }
                    ActionResult(true, "Macro impostati: $c% C · $p% P · $f% G (calcolo automatico spento)")
                }
            }
            "update_profile" -> {
                if (!st.profile.detailed) {
                    Nav.go(Screen.Questionnaire(null))
                    return ActionResult(true, "Ho aperto il questionario: rispondi alle domande e il calcolo sarà preciso")
                }
                val ch = mutableListOf<String>()
                var p = st.profile
                args.str("job")?.lowercase()?.let { j ->
                    val v = when { j.contains("sed") -> 0; j.contains("pied") || j.contains("mov") -> 1; j.contains("fis") -> 2; else -> null }
                    if (v != null) { p = p.copy(job = v); ch += "lavoro: ${Nutrition.JOB_LABELS[v].substringBefore(" (").lowercase()}" }
                }
                args.num("steps_per_day")?.let { n ->
                    val v = when { n < 4000 -> 0; n < 7000 -> 1; n < 10000 -> 2; n < 14000 -> 3; else -> 4 }
                    p = p.copy(stepsLevel = v); ch += "passi: ${Nutrition.STEPS_LABELS[v]}"
                }
                args.num("workouts_per_week")?.toInt()?.takeIf { it in 0..14 }?.let { p = p.copy(workouts = it); ch += "allenamenti: $it a settimana" }
                args.num("workout_minutes")?.toInt()?.takeIf { it in 10..240 }?.let { p = p.copy(workoutMin = it); ch += "durata: $it min" }
                args.str("intensity")?.lowercase()?.let { s ->
                    val v = when { s.startsWith("legg") -> 0; s.startsWith("mod") -> 1; s.startsWith("int") -> 2; else -> null }
                    if (v != null) { p = p.copy(workoutIntensity = v); ch += "intensità: ${s}" }
                }
                args.str("training_type")?.lowercase()?.let { s ->
                    val v = when { s.contains("pes") || s.contains("cali") -> 1; s.contains("card") || s.contains("cors") -> 2; s.contains("squad") -> 3; s.contains("mis") -> 0; else -> null }
                    if (v != null) { p = p.copy(trainingType = v); ch += "tipo: ${Nutrition.TRAINING_LABELS[v].lowercase()}" }
                }
                args.num("body_fat")?.takeIf { it in 5.0..55.0 }?.let { p = p.copy(bodyFatPct = it); ch += "massa grassa: ${"%.1f".format(Locale.ITALY, it)}%" }
                args.num("height_cm")?.takeIf { it in 120.0..230.0 }?.let { p = p.copy(heightCm = it); ch += "altezza: ${f0(it)} cm" }
                if (ch.isEmpty()) return ActionResult(false, "Nessun campo del profilo riconosciuto.", true)
                recompute { it.copy(profile = p) }
                ActionResult(true, "Profilo aggiornato (${ch.joinToString(", ")}) · ora ${Store.current.goals.kcal} kcal")
            }
            "start_fast" -> {
                val id = args.str("plan") ?: "16:8"
                val plan = FastingPlans.plans.firstOrNull { it.id == id || it.fastHours.toString() == id.substringBefore(":") }
                    ?: return ActionResult(false, "Piano di digiuno non riconosciuto ($id).", true)
                if (st.fasting.activeStart != null) return ActionResult(false, "C'è già un digiuno in corso: terminalo prima (stop_fast).")
                Store.startFast(plan.id, plan.fastHours)
                ActionResult(true, "Digiuno ${plan.title} avviato")
            }
            "stop_fast" -> {
                if (st.fasting.activeStart == null) return ActionResult(false, "Non c'è nessun digiuno in corso.")
                Store.stopFast(true)
                ActionResult(true, "Digiuno terminato e salvato")
            }
            "set_diet" -> {
                val add = args.list("exclude_add").map { FoodDb.normalize(it).replace(" ", "_") }
                val rem = args.list("exclude_remove").map { FoodDb.normalize(it).replace(" ", "_") }
                val bad = (add + rem).filter { it !in EXCLUDE_TAGS }
                if (bad.isNotEmpty()) return ActionResult(false, "Esclusioni non valide: ${bad.joinToString()}. Valide: ${EXCLUDE_TAGS.joinToString()}.", true)
                val mins = args.num("max_minutes")?.toInt()?.takeIf { it in 5..180 }
                if (add.isEmpty() && rem.isEmpty() && mins == null) return ActionResult(false, "Dimmi cosa escludere (exclude_add / exclude_remove) o il tempo massimo (max_minutes).", true)
                val p = st.dietPrefs
                val ex = (p.exclude + add).distinct().filter { it !in rem }
                Store.setDietPrefs(p.copy(exclude = ex, maxMinutes = mins ?: p.maxMinutes))
                ActionResult(true, "Preferenze alimentari: esclusi ${if (ex.isEmpty()) "nessuno" else ex.joinToString(", ")}${mins?.let { " · tempo massimo $it min" } ?: ""}")
            }
            "generate_plan" -> {
                val days = args.num("days")?.toInt()?.coerceIn(1, 14) ?: 7
                Store.generatePlan(LocalDate.now(), days)
                ActionResult(true, "Piano alimentare creato per $days giorni")
            }
            "plan_recipe" -> {
                val (ref, err) = resolveRecipe(args.str("recipe") ?: "", st)
                if (ref == null) return err!!
                val t = ref.tpl ?: return ActionResult(false, "Le ricette create da te non si possono ancora inserire nel piano: usa log_recipe per registrarle nel diario.")
                val slots = st.dietPrefs.slots
                val key = (parseMeal(args.str("meal"))?.let { m -> when (m) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; else -> "spuntino" } } ?: if (t.meal == "dessert") "spuntino" else t.meal)
                val idx = slots.indexOfFirst { it.meal == key }.takeIf { it >= 0 } ?: 0
                val date = dateOf(args.str("date"))
                Store.planRecipe(date, idx, t.id)
                ActionResult(true, "Nel piano di ${dateLabel(date)} (${slots[idx].name.lowercase()}): ${t.name}")
            }
            "add_shopping" -> {
                val item = args.str("item") ?: return ActionResult(false, "Manca l'articolo.", true)
                Store.addShoppingManual(item)
                ActionResult(true, "Aggiunto alla spesa: $item")
            }
            "check_shopping" -> {
                val q = args.str("item") ?: return ActionResult(false, "Manca l'articolo.", true)
                val checked = args.flag("checked") ?: true
                val found = st.shopping.filter { nameMatches(it.name, q) && it.checked != checked }
                if (found.isEmpty()) return ActionResult(false, "Non trovo \"$q\" nella lista della spesa (o è già nello stato richiesto).", true)
                found.forEach { Store.toggleShopping(it.id) }
                ActionResult(true, "${if (checked) "Spuntato" else "Riattivato"}: ${found.joinToString(", ") { it.name }}")
            }
            "remove_shopping" -> {
                val q = args.str("item") ?: return ActionResult(false, "Manca l'articolo.", true)
                val found = st.shopping.filter { nameMatches(it.name, q) }
                if (found.isEmpty()) return ActionResult(false, "Non trovo \"$q\" nella lista della spesa.", true)
                val label = "Togliere dalla spesa: " + found.joinToString(", ") { it.name }
                if (!confirmed) return needConfirm(label)
                found.forEach { Store.removeShopping(it.id) }
                ActionResult(true, "Tolto dalla spesa: " + found.joinToString(", ") { it.name })
            }
            "build_shopping" -> {
                val days = args.num("days")?.toInt()?.coerceIn(1, 14) ?: 7
                if (Store.plannedMeals(st, LocalDate.now()).isEmpty() && st.plan.isEmpty()) return ActionResult(false, "Non c'è ancora un piano: creane uno (generate_plan) e poi preparo la spesa.", true)
                Store.buildShopping(LocalDate.now(), days)
                ActionResult(true, "Lista della spesa preparata per $days giorni (${Store.current.shopping.size} articoli)")
            }
            "shopping_to_pantry" -> {
                val n = st.shopping.count { it.checked }
                if (n == 0) return ActionResult(false, "Non c'è nessun articolo spuntato da spostare in dispensa.")
                Store.moveCheckedToPantry()
                ActionResult(true, "$n articoli spostati in dispensa")
            }
            "add_pantry" -> {
                val name = args.str("item") ?: return ActionResult(false, "Manca l'alimento.", true)
                val f = Db.db.search(name, 1).firstOrNull() ?: Db.db.match(name)
                Store.addPantry(listOf(PantryItem(Store.newId(), f?.id, f?.name ?: name, args.num("grams")?.takeIf { it > 0 }, source = "manuale")))
                ActionResult(true, "In dispensa: ${f?.name ?: name}" + (args.num("grams")?.let { " (${f0(it)} g)" } ?: ""))
            }
            "remove_pantry" -> {
                val q = args.str("item") ?: return ActionResult(false, "Manca l'alimento.", true)
                val found = st.pantry.filter { nameMatches(it.name, q) }
                if (found.isEmpty()) return ActionResult(false, "Non trovo \"$q\" in dispensa.", true)
                val label = "Togliere dalla dispensa: " + found.joinToString(", ") { it.name }
                if (!confirmed) return needConfirm(label)
                found.forEach { Store.removePantry(it.id) }
                ActionResult(true, "Tolto dalla dispensa: " + found.joinToString(", ") { it.name })
            }
            "find_recipes" -> {
                val meals = args.list("meal").mapNotNull { m -> when (parseMeal(m)) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; 3, 4, 5 -> "spuntino"; else -> if (FoodDb.normalize(m).contains("dessert")) "dessert" else null } }.toSet()
                val filter = RecipeFilter(
                    query = args.str("query") ?: "", meals = meals,
                    kcalMax = args.num("max_kcal")?.toInt(), kcalMin = args.num("min_kcal")?.toInt(),
                    proteinMin = args.num("min_protein")?.toInt(), carbsMax = args.num("max_carbs")?.toInt(), fatMax = args.num("max_fat")?.toInt(),
                    maxMinutes = args.num("max_minutes")?.toInt(),
                    diets = pickKeys(RecipeMeta.DIETS, args.list("diet")).toSet(), goals = pickKeys(RecipeMeta.GOALS, args.list("goal")).toSet(),
                    cuisines = pickKeys(RecipeMeta.CUISINES, args.list("cuisine")).toSet(),
                    fridge = args.flag("fridge") ?: false, onlyFavs = args.flag("favorites") ?: false,
                    sort = args.str("sort")?.takeIf { it in setOf("kcal", "proteine", "tempo") } ?: "rilevanza"
                )
                val hits = RecipeSearch.search(Db.db, filter, pantryIds, st.recipeFavs.toSet(), st.dietPrefs.disliked.toSet())
                if (hits.isEmpty()) return ActionResult(false, "Nessuna ricetta corrisponde a questi filtri: provo con filtri meno stretti?", true)
                Nav.go(Screen.RecipeList(filter, args.str("title") ?: "Risultati della ricerca"))
                ActionResult(true, "Aperto l'elenco: ${hits.size} ricette trovate")
            }
            "open_recipe" -> {
                val (ref, err) = resolveRecipe(args.str("recipe") ?: "", st)
                if (ref == null) return err!!
                if (ref.tpl != null) Nav.go(Screen.RecipeView(ref.tpl.id, targetOf(ref.tpl), LocalDate.now(), null, mealFor(ref.tpl.meal) ?: byHour(LocalTime.now().hour), original = true))
                else Nav.go(Screen.UserRecipeDetail(ref.mine!!))
                ActionResult(true, "Aperta la ricetta: ${ref.name}")
            }
            "what_now" -> { Nav.go(Screen.WhatNow); ActionResult(true, "Aperto: cosa mangiare adesso") }
            "fav_recipe" -> {
                val (ref, err) = resolveRecipe(args.str("recipe") ?: "", st)
                if (ref == null) return err!!
                if (ref.tpl == null) return ActionResult(false, "I preferiti valgono per le ricette del catalogo.")
                val want = args.flag("favorite") ?: true
                if ((ref.id in st.recipeFavs) == want) return ActionResult(true, "${ref.name} era già ${if (want) "tra i preferiti" else "fuori dai preferiti"}")
                Store.toggleRecipeFav(ref.id)
                ActionResult(true, if (want) "Aggiunta ai preferiti: ${ref.name}" else "Tolta dai preferiti: ${ref.name}")
            }
            "add_recipe_shopping" -> {
                val (ref, err) = resolveRecipe(args.str("recipe") ?: "", st)
                if (ref == null) return err!!
                val n = if (ref.tpl != null) Store.addRecipeToShopping(Solver.original(ref.tpl, Db.db, targetOf(ref.tpl)), ref.name, pantryIds)
                else Store.addUserRecipeToShopping(ref.mine!!, 1.0, pantryIds)
                if (n == 0) ActionResult(true, "Per ${ref.name} hai già tutto in dispensa") else ActionResult(true, "Aggiunti $n ingredienti alla spesa per ${ref.name}")
            }
            "create_recipe" -> {
                val name = args.str("name") ?: return ActionResult(false, "Manca il nome della ricetta.", true)
                val arr = args.optJSONArray("ingredients") ?: return ActionResult(false, "Mancano gli ingredienti: elenco di {\"food\":\"pasta\",\"grams\":160}.", true)
                val ings = mutableListOf<Ingredient>(); val missing = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val fn = o.str("food") ?: continue
                    val g = o.num("grams")?.takeIf { it in 1.0..5000.0 }
                    val f = findFood(fn, st)
                    if (f == null || g == null) { missing += fn; continue }
                    ings += Ingredient(f, g)
                }
                if (ings.size < 2) return ActionResult(false, "Servono almeno 2 ingredienti riconosciuti, con quantità in grammi." + if (missing.isNotEmpty()) " Non validi: ${missing.joinToString()}." else "", true)
                val servings = args.num("servings")?.toInt()?.coerceIn(1, 30) ?: 1
                val steps = args.list("steps")
                val mealKey = args.str("meal")?.let { m -> when (parseMeal(m)) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; null -> if (FoodDb.normalize(m).contains("dessert")) "dessert" else ""; else -> "spuntino" } } ?: ""
                val emoji = when (mealKey) { "colazione" -> "🥣"; "cena" -> "🍲"; "spuntino" -> "🍎"; "dessert" -> "🍰"; else -> "🍽️" }
                val r = Recipe(Store.newId(), name, servings, ings, steps, args.num("minutes")?.toInt()?.coerceIn(1, 600) ?: 0, "", emoji,
                    description = args.str("description") ?: "", cuisine = pickKeys(RecipeMeta.CUISINES, args.list("cuisine")).firstOrNull() ?: "",
                    difficulty = args.num("difficulty")?.toInt()?.coerceIn(0, 3) ?: 0, meal = mealKey)
                Store.saveRecipe(r)
                val per = Nutrition.recipeTotals(r).scaled(1.0 / servings)
                Nav.go(Screen.UserRecipeDetail(r))
                ActionResult(true, "Ricetta \"$name\" salvata: ${f0(per.kcal)} kcal a porzione (P ${f0(per.protein)} · C ${f0(per.carbs)} · G ${f0(per.fat)})" +
                        if (missing.isNotEmpty()) " · NON trovati nel database ed esclusi dal calcolo: ${missing.joinToString()}" else "")
            }
            "delete_recipe" -> {
                val q = args.str("recipe") ?: return ActionResult(false, "Manca il nome della ricetta.", true)
                val mine = st.recipes.filter { nameMatches(it.name, q) }
                if (mine.isEmpty()) return ActionResult(false, "Non trovo nessuna tua ricetta chiamata \"$q\" (le ricette del catalogo non si eliminano).", true)
                if (mine.size > 1 && mine.none { FoodDb.normalize(it.name) == FoodDb.normalize(q) })
                    return ActionResult(false, "Ho trovato più tue ricette: ${mine.joinToString("; ") { it.name }}. Chiedi all'utente quale eliminare.", true)
                val r = mine.firstOrNull { FoodDb.normalize(it.name) == FoodDb.normalize(q) } ?: mine[0]
                val label = "Eliminare la tua ricetta \"${r.name}\""
                if (!confirmed) return needConfirm(label)
                r.photo?.let { runCatching { java.io.File(it).delete() } }
                Store.deleteRecipe(r.id)
                ActionResult(true, "Ricetta eliminata: ${r.name}")
            }
            "open_screen" -> {
                val target = FoodDb.normalize(args.str("screen") ?: return ActionResult(false, "Schermata mancante.", true))
                val today = LocalDate.now()
                Nav.clear()
                when (target) {
                    "oggi", "home", "diario" -> Nav.tab = 0
                    "piano" -> Nav.tab = 1
                    "cucina" -> Nav.tab = 2
                    "dispensa" -> { Nav.tab = 2; Nav.kitchenTab = 0 }
                    "spesa", "lista della spesa" -> { Nav.tab = 2; Nav.kitchenTab = 1 }
                    "ricette" -> { Nav.tab = 2; Nav.kitchenTab = 2 }
                    "progressi" -> Nav.tab = 3
                    "profilo" -> Nav.tab = 4
                    "obiettivi" -> Nav.go(Screen.Goals)
                    "smartwatch", "watch" -> Nav.go(Screen.Watch)
                    "peso" -> Nav.go(Screen.Weight)
                    "misure" -> Nav.go(Screen.Measurements)
                    "digiuno" -> Nav.go(Screen.Fasting)
                    "storico digiuno" -> Nav.go(Screen.FastingHistory)
                    "questionario" -> Nav.go(Screen.Questionnaire(null))
                    "calcolo" -> Nav.go(Screen.EnergyInfo)
                    "micronutrienti", "vitamine" -> Nav.go(Screen.Micronutrients)
                    "nutrienti", "nutrienti di oggi" -> Nav.go(Screen.DayNutrients(today))
                    "attivita" -> Nav.go(Screen.Activity(today))
                    "preferenze", "dieta" -> Nav.go(Screen.DietSettings)
                    "i miei alimenti", "alimenti" -> Nav.go(Screen.MyFoods)
                    "cosa mangio adesso", "cosa adesso" -> Nav.go(Screen.WhatNow)
                    "nuova ricetta" -> Nav.go(Screen.RecipeEditor(null))
                    "le mie ricette" -> Nav.go(Screen.RecipeList(RecipeFilter(), "Le mie ricette", mine = true))
                    "backup" -> Nav.go(Screen.Backup)
                    "foto", "foto ai" -> Nav.go(Screen.PhotoAi(today, mealIndex(args.str("meal"))))
                    "cerca", "aggiungi cibo" -> Nav.go(Screen.AddFood(dateOf(args.str("date")), mealIndex(args.str("meal")), args.str("query") ?: ""))
                    else -> return ActionResult(false, "Schermata sconosciuta: $target.")
                }
                ActionResult(true, "Aperto: $target")
            }
            else -> ActionResult(false, "Azione sconosciuta: ${a.name}", true)
        }
    }

    // ------------------------------------------------------------------ Onestà sulle azioni

    private val claimRe = Regex("(?i)(\\b(ho|l'ho|ti ho|abbiamo)\\s+(registrato|aggiunto|eliminato|cancellato|impostato|avviato|creato|salvato|modificato|segnato|inserito|copiato|tolto|pianificato|aperto)\\b|^\\s*(ok[,.! ]+)?(fatto|registrat[oaie]|aggiunt[oaie]|eliminat[oaie]|cancellat[oaie]|impostat[oaie]|avviat[oaie]|creat[oaie]|salvat[oaie]|modificat[oaie]|inserit[oaie]|copiat[oaie]|tolt[oaie])\\b)")

    /** Il testo dice di aver fatto qualcosa nell'app? Serve a non lasciar passare "Registrato." quando nessuna azione è stata eseguita. */
    fun claimsDone(text: String): Boolean = claimRe.containsMatchIn(text)

    // ------------------------------------------------------------------ Suggerimenti dinamici

    /** Proposte cliccabili basate sullo stato reale dell'app (non un elenco fisso). */
    fun smartSuggestions(s: AppState, now: LocalTime = LocalTime.now()): List<String> {
        val today = LocalDate.now()
        val entries = Store.entriesFor(s, today)
        val e = Store.energyFor(s, today)
        val tot = Nutrition.sum(entries)
        val out = mutableListOf<String>()
        val h = now.hour
        if (s.fasting.activeStart != null) out += "Termina il digiuno"
        if (entries.none { it.meal == 0 } && h in 5..11) out += "Ho fatto colazione con…"
        if (entries.none { it.meal == 1 } && h in 11..15) out += "Per pranzo ho mangiato…"
        if (entries.none { it.meal == 2 } && h in 18..22) out += "Per cena ho mangiato…"
        val water = s.water[today.toString()] ?: 0
        if (water < s.goals.waterMl / 2 && h >= 12) out += "Aggiungi 250 ml di acqua"
        val left = e.targetKcal - tot.kcal
        if (entries.isNotEmpty() && left > 150) out += "Cosa posso mangiare con ${f0(left)} kcal?"
        if (left < -100) out += "Ho superato le calorie: come recupero?"
        if (s.plan.none { it.date >= today.toString() }) out += "Crea il piano di 7 giorni"
        else out += "Cosa c'è nel piano di oggi?"
        if (s.pantry.isNotEmpty()) out += "Che ricetta posso fare con la dispensa?"
        if (s.shopping.any { it.checked }) out += "Sposta la spesa spuntata in dispensa"
        if (s.weights.isEmpty() || s.weights.last().date != today.toString()) out += "Oggi peso 79,5 kg"
        if (s.recipes.isEmpty()) out += "Crea una ricetta proteica veloce"
        out += listOf("Cerca ricette sotto 400 kcal", "Che macro mi conviene?")
        return out.distinct().take(6)
    }

    // ------------------------------------------------------------------ Contesto per il modello

    private val actionHeader = """AZIONI
Per agire nell'app scrivi una breve frase e poi un blocco <action>{"name":"...","args":{...}}</action> (anche più blocchi). date: "oggi", "ieri", "domani" o AAAA-MM-GG. meal: colazione, spuntino mattutino, pranzo, merenda, cena, spuntino serale.
- log_food {"food":"pollo","grams":150,"meal":"pranzo","date":"oggi"}
- add_water {"ml":250}, log_steps {"steps":8000}, log_weight {"kg":79.4}
- log_activity {"name":"Corsa","minutes":30,"kcal":300}
- open_screen {"screen":"oggi|piano|dispensa|spesa|ricette|le mie ricette|nuova ricetta|progressi|profilo|obiettivi|smartwatch|peso|misure|digiuno|questionario|calcolo|vitamine|nutrienti|attivita|preferenze|alimenti|backup|foto|cerca","meal":"pranzo","query":"yogurt"}"""

    private class Group(val keys: Regex, val names: List<String>, val doc: String)

    private val groups = listOf(
        Group(Regex("elimin|cancell|\\btogl|rimuov|modific|cambia|sposta|copia|svuota|sbagliat|correggi|annulla"),
            listOf("edit_entry", "delete_entry", "clear_meal", "copy_meal", "delete_weight", "delete_activity"),
            """- edit_entry {"food":"pollo","meal":"pranzo","grams":200,"new_meal":"cena"} cambia quantità o pasto
- delete_entry {"food":"pollo","meal":"pranzo","date":"oggi"}, clear_meal {"meal":"cena","date":"ieri"}, delete_weight {"date":"oggi"}, delete_activity {"name":"Corsa","date":"oggi"} (eliminano: l'app chiede conferma all'utente)
- copy_meal {"from_meal":"pranzo","from_date":"ieri","to_meal":"pranzo","to_date":"oggi"}"""),
        Group(Regex("ricett|cerc|idee|idea|cosa (mangi|cucin|prepar|posso)|preferit|crea|invent|piatt|\\bsotto \\d|vegetarian|vegan|proteic|veloce|kcal|cucina"),
            listOf("log_recipe", "find_recipes", "open_recipe", "what_now", "fav_recipe", "add_recipe_shopping", "plan_recipe", "create_recipe", "delete_recipe"),
            """- log_recipe {"recipe":"nome ricetta","portions":1,"meal":"cena"} registra una ricetta (del catalogo o tua)
- find_recipes {"query":"pollo","meal":"cena","max_kcal":500,"min_protein":30,"max_minutes":20,"diet":"vegetariana","goal":"alto_proteico","cuisine":"italiana","favorites":false,"fridge":false,"sort":"kcal|proteine|tempo"} apre l'elenco filtrato (tutti i campi facoltativi)
- open_recipe {"recipe":"nome"}, what_now {} (cosa mangiare adesso), fav_recipe {"recipe":"nome","favorite":true}
- add_recipe_shopping {"recipe":"nome"}, plan_recipe {"recipe":"nome","date":"domani","meal":"cena"}
- create_recipe {"name":"...","servings":2,"minutes":20,"meal":"pranzo","ingredients":[{"food":"pasta","grams":160},{"food":"passata","grams":300}],"steps":["..."]} salva una ricetta tua: grammi TOTALI crudi, le calorie le calcola l'app
- delete_recipe {"recipe":"nome"} (solo ricette tue, con conferma)"""),
        Group(Regex("piano|settiman|spesa|compr|dispensa|frigo|ingredient|scorte"),
            listOf("generate_plan", "plan_recipe", "add_pantry", "remove_pantry", "add_shopping", "check_shopping", "remove_shopping", "build_shopping", "shopping_to_pantry"),
            """- generate_plan {"days":7}, plan_recipe {"recipe":"nome","date":"domani","meal":"cena"}
- add_pantry {"item":"uova","grams":300}, remove_pantry {"item":"uova"} (conferma)
- add_shopping {"item":"uova"}, check_shopping {"item":"uova","checked":true}, remove_shopping {"item":"uova"} (conferma)
- build_shopping {"days":7} (dal piano), shopping_to_pantry {} (articoli spuntati → dispensa)"""),
        Group(Regex("calori|macro|obiettiv|dimagr|\\bmassa|\\bpeso|protein|diet|acqua|passi|profilo|allenam|esclu|intoller|allerg|lattosio|glutine|tdee|fabbisogno"),
            listOf("set_goal", "set_target_weight", "set_calories", "use_auto_calc", "set_macros", "set_water_goal", "set_steps_goal", "update_profile", "set_diet"),
            """- set_goal {"goal":"dimagrire|mantenere|aumentare","kg_per_week":0.5}, set_target_weight {"kg":78}
- set_calories {"kcal":2400}, use_auto_calc {}, set_macros {"preset":"bilanciato|proteico|low_carb"} o {"carbs":40,"protein":30,"fat":30}
- set_water_goal {"ml":2500}, set_steps_goal {"steps":9000}, delete_weight {"date":"oggi"} (conferma)
- update_profile {"job":"seduto|in_piedi|fisico","steps_per_day":8000,"workouts_per_week":3,"workout_minutes":45,"intensity":"leggero|moderato|intenso","training_type":"misto|pesi|cardio|squadra","body_fat":18,"height_cm":178}
- set_diet {"exclude_add":["lattosio"],"exclude_remove":["pesce"],"max_minutes":30} (tag: lattosio, glutine, pesce, crostacei, carne, maiale, uova, frutta_secca, soia)"""),
        Group(Regex("digiun|16:8|18:6|fasting"), listOf("start_fast", "stop_fast"),
            """- start_fast {"plan":"16:8"} / stop_fast {}""")
    )

    /** Documenta solo le azioni utili alla richiesta (o già nominate): un prompt corto è più veloce e più preciso sul telefono. */
    fun actionDocs(hint: String): String {
        val h = hint.lowercase()
        val picked = groups.filter { g -> g.keys.containsMatchIn(h) || g.names.any { it in h } }
        return picked.joinToString("\n") { it.doc }
    }

    fun systemPrompt(s: AppState, hint: String = ""): String {
        val today = LocalDate.now()
        val w = Store.currentWeight(s)
        val p = s.profile
        val e = Store.energyFor(s, today)
        val entries = Store.entriesFor(s, today)
        val tot = Nutrition.sum(entries)
        val water = s.water[today.toString()] ?: 0
        val fast = s.fasting.activeStart?.let { "in corso da ${((System.currentTimeMillis() - it) / 3_600_000)} h (${s.fasting.planId})" } ?: "nessuno"
        val diary = if (entries.isEmpty()) "nulla ancora" else entries.takeLast(12).joinToString("; ") {
            "${com.jarvis.food.data.MEAL_NAMES[it.meal].lowercase()}: ${it.food.name} ${f0(it.grams)} g (${f0(Nutrition.of(it.food, it.grams).kcal)} kcal)"
        }
        val goalTxt = when (p.goal) { -1 -> "dimagrire"; 1 -> "aumentare massa"; else -> "mantenere" }
        val prefs = listOf("bilanciato", "più proteine", "pochi carboidrati")[p.dietStyle.coerceIn(0, 2)]
        val quest = if (p.detailed) "lavoro ${Nutrition.JOB_LABELS[p.job.coerceIn(0, 2)].substringBefore(" (").lowercase()}, passi ${Nutrition.STEPS_LABELS[p.stepsLevel.coerceIn(0, 4)]}, " +
                "allenamenti ${p.workouts}/sett da ${p.workoutMin} min" else "questionario non ancora compilato (calcolo meno preciso)"
        val yEntries = Store.entriesFor(s, today.minusDays(1))
        val yTot = Nutrition.sum(yEntries)
        val week = (1..7).map { d -> Nutrition.sum(Store.entriesFor(s, today.minusDays(d.toLong()))).kcal }.filter { it > 0 }
        val planToday = Store.plannedMeals(s, today).joinToString("; ") { "${it.solved.template.name} (${it.solved.template.meal})" }.ifBlank { "nessuno" }
        val extra = buildString {
            append("Ieri: ${if (yEntries.isEmpty()) "nulla registrato" else "${f0(yTot.kcal)} kcal (P ${f0(yTot.protein)}, C ${f0(yTot.carbs)}, G ${f0(yTot.fat)})"}")
            if (week.size >= 2) append("; media ultimi giorni registrati ${f0(week.average())} kcal")
            append(". Ultimi pesi: ${s.weights.takeLast(4).joinToString(", ") { "${it.date.takeLast(5)} ${"%.1f".format(Locale.ITALY, it.kg)}" }.ifBlank { "nessuno" }}.\n")
            append("Piano di oggi: $planToday. Dispensa: ${s.pantry.take(14).joinToString(", ") { it.name }.ifBlank { "vuota" }}${if (s.pantry.size > 14) "…" else ""}. ")
            append("Spesa: ${s.shopping.count { !it.checked }} articoli da comprare. Preferiti/ricette tue: ${s.recipes.take(8).joinToString(", ") { it.name }.ifBlank { "nessuna" }}. Esclusioni: ${s.dietPrefs.exclude.joinToString(", ").ifBlank { "nessuna" }}.")
        }
        val dow = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
        return """Sei Jarvis, l'assistente personale dentro l'app Jarvis Food (calorie, macro, piano alimentare, digiuno, smartwatch). Parli italiano, diretto e cordiale, risposte brevi (massimo 4 frasi) salvo richiesta. Usi solo i dati qui sotto, non inventi numeri. Non fai diagnosi mediche.

DATI ($dow ${today}, ${LocalTime.now().hour}:${"%02d".format(LocalTime.now().minute)})
Utente: ${p.name}, ${if (p.male) "uomo" else "donna"}, ${Nutrition.age(p)} anni, ${f0(p.heightCm)} cm, peso ${"%.1f".format(Locale.ITALY, w)} kg, obiettivo peso ${"%.1f".format(Locale.ITALY, p.targetWeight)} kg, scopo: $goalTxt${if (p.goal != 0) " ${"%.2f".format(Locale.ITALY, p.weeklyRate)} kg/sett" else ""}. Preferenza macro: $prefs. $quest.
Obiettivo di oggi: ${f0(e.targetKcal)} kcal (base ${f0(e.baseKcal)}), macro P ${f0(e.protein)} g, C ${f0(e.carbs)} g, G ${f0(e.fat)} g; ripartizione ${s.goals.carbsPct}% C ${s.goals.proteinPct}% P ${s.goals.fatPct}% G; calcolo ${if (s.goals.auto) "automatico" else "manuale"}. Basale ${f0(e.bmr)} kcal.
Oggi: mangiate ${f0(tot.kcal)} kcal (P ${f0(tot.protein)}, C ${f0(tot.carbs)}, G ${f0(tot.fat)}), acqua $water ml su ${s.goals.waterMl}, passi ${Store.stepsFor(s, today)}, attività ${Store.burnedFor(s, today)} kcal, digiuno: $fast.
Diario di oggi: $diary.
$extra

${actionHeader}
${actionDocs(hint)}

REGOLE
- Se la domanda riguarda dati che hai già qui sopra (calorie, macro, peso, obiettivo, cosa ha mangiato, piano, dispensa) RISPONDI subito con i numeri dei DATI: non chiedere ciò che già sai.
- Prima di agire controlla di avere tutti i dati. Se manca qualcosa (quantità, pasto, valore) NON agire: fai UNA sola domanda breve.
- Non dire mai di aver fatto qualcosa se non scrivi il blocco <action>: senza blocco non è successo nulla. L'esito vero (✓ o ⚠) lo mostra l'app.
- Le eliminazioni non le esegui tu: scrivi il blocco e l'app chiede la conferma all'utente con due pulsanti. Non dire "ho eliminato".
- I tag <quick> sono possibili RISPOSTE dell'utente (massimo 4 parole, mai domande) e si usano solo se hai fatto una domanda o per suggerire il passo successivo.
- Per cambiare calorie, obiettivo o profilo, se l'utente non è stato esplicito, spiega l'effetto e chiedi conferma con <quick>Sì, applica</quick><quick>No</quick>.
- Dopo aver agito conferma in una frase. Se un'azione non riesce, dillo e proponi un'alternativa.
- Consigli su calorie e macro: usa i dati sopra e spiega in breve il ragionamento (proteine in g/kg, deficit sostenibile).
- Non scrivere mai JSON fuori da un blocco <action>.

ESEMPI (usa i numeri veri dei DATI, non questi)
Utente: Ho mangiato 150 g di pollo a pranzo
Jarvis: Registrato.<action>{"name":"log_food","args":{"food":"pollo","grams":150,"meal":"pranzo"}}</action>
Utente: Ho mangiato una pasta
Jarvis: Quanta ne hai mangiata?<quick>80 g</quick><quick>100 g</quick><quick>150 g</quick>
Utente: Quante calorie devo mangiare oggi?
Jarvis: Il tuo obiettivo di oggi è 2500 kcal e te ne restano 1300. Proteine: 160 g.
Utente: Bevo 500 ml d'acqua
Jarvis: Aggiunti.<action>{"name":"add_water","args":{"ml":500}}</action>
Utente: Pesavo 80, oggi sono 79,4
Jarvis: Registrato, 600 g in meno.<action>{"name":"log_weight","args":{"kg":79.4}}</action>"""
    }
}
