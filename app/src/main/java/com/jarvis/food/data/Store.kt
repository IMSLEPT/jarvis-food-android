package com.jarvis.food.data

import android.content.Context
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
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

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
        if (::file.isInitialized) return
        file = File(context.filesDir, "jarvis_food.json")
        if (file.exists()) {
            runCatching { json.decodeFromString(AppState.serializer(), file.readText()) }
                .onSuccess { _state.value = it }
                .onFailure {
                    // File corrotto: lo conservo come copia invece di perderlo.
                    file.copyTo(File(context.filesDir, "jarvis_food_corrotto_${System.currentTimeMillis()}.json"), true)
                }
        }
    }

    fun update(block: (AppState) -> AppState) {
        _state.update(block)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            saveNow()
        }
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
        val s = json.decodeFromString(AppState.serializer(), text)
        _state.value = s
        saveNow()
    }

    fun resetAll() {
        _state.value = AppState()
        saveNow()
    }

    fun newId() = UUID.randomUUID().toString()

    // ---- Azioni ----

    fun addEntry(date: LocalDate, meal: Int, food: Food, grams: Double) = update {
        it.copy(diary = it.diary + DiaryEntry(newId(), date.toString(), meal, food, grams, System.currentTimeMillis()))
    }

    fun updateEntry(id: String, grams: Double, meal: Int) = update { s ->
        s.copy(diary = s.diary.map { if (it.id == id) it.copy(grams = grams, meal = meal) else it })
    }

    fun removeEntry(id: String) = update { s -> s.copy(diary = s.diary.filterNot { it.id == id }) }

    fun copyMeal(from: LocalDate, meal: Int, to: LocalDate, toMeal: Int) = update { s ->
        val copies = s.diary.filter { it.date == from.toString() && it.meal == meal }
            .map { it.copy(id = newId(), date = to.toString(), meal = toMeal, time = System.currentTimeMillis()) }
        s.copy(diary = s.diary + copies)
    }

    fun toggleFavorite(food: Food) = update { s ->
        if (s.favorites.any { it.id == food.id }) s.copy(favorites = s.favorites.filterNot { it.id == food.id })
        else s.copy(favorites = listOf(food) + s.favorites)
    }

    fun saveCustomFood(food: Food) = update { s ->
        s.copy(customFoods = listOf(food) + s.customFoods.filterNot { it.id == food.id })
    }

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
        s.copy(measurements = s.measurements.filterNot { it.date == date.toString() && it.type == type } +
                Measurement(date.toString(), type, value))
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
            (listOf(FastingSession(start, System.currentTimeMillis(), f.targetHours)) + f.history).take(200)
        else f.history
        s.copy(fasting = f.copy(activeStart = null, history = hist))
    }

    // ---- Letture ----

    fun entriesFor(s: AppState, date: LocalDate) = s.diary.filter { it.date == date.toString() }
    fun burnedFor(s: AppState, date: LocalDate) = s.activities.filter { it.date == date.toString() }.sumOf { it.kcal } +
            Nutrition.stepsKcal(s.steps[date.toString()] ?: 0, currentWeight(s))

    fun currentWeight(s: AppState) = s.weights.lastOrNull()?.kg ?: s.profile.startWeight

    /** Alimenti usati di recente, senza duplicati. */
    fun recentFoods(s: AppState, limit: Int = 30): List<Pair<Food, Double>> =
        s.diary.sortedByDescending { it.time }.distinctBy { it.food.id }.take(limit).map { it.food to it.grams }

    /** Alimenti più usati (per suggerimenti rapidi). */
    fun frequentFoods(s: AppState, meal: Int, limit: Int = 8): List<Pair<Food, Double>> =
        s.diary.filter { it.meal == meal }.groupBy { it.food.id }
            .entries.sortedByDescending { it.value.size }.take(limit)
            .map { e -> e.value.last().food to (e.value.map { it.grams }.average().roundToInt().toDouble()) }
}
