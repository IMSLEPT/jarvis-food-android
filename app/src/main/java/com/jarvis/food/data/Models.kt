package com.jarvis.food.data

import kotlinx.serialization.Serializable

/**
 * Alimento. Valori nutrizionali sempre riferiti a 100 g (o 100 ml).
 * [micro] contiene fibre, zuccheri, grassi saturi, sodio, vitamine e minerali con le chiavi di [Micros].
 */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val brand: String = "",
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val satFat: Double? = null,
    val salt: Double? = null,
    val servingName: String? = null,
    val servingGrams: Double? = null,
    val barcode: String? = null,
    /** db | off | custom | ai | recipe | quick | base */
    val source: String = "db",
    val liquid: Boolean = false,
    val category: String = "",
    val micro: Map<String, Double> = emptyMap(),
    val fdc: Int? = null,
    val group: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class DiaryEntry(
    val id: String,
    val date: String,          // yyyy-MM-dd
    val meal: Int,             // 0 colazione, 1 pranzo, 2 cena, 3 spuntini
    val food: Food,
    val grams: Double,
    val time: Long,
    /** Se l'alimento arriva dal piano: data e indice del pasto pianificato. */
    val planRef: String? = null
)

@Serializable
data class Ingredient(val food: Food, val grams: Double)

/** Ricetta creata dall'utente (editor). Le ricette del database sono in [RecipeTemplate]. */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val servings: Int,
    val ingredients: List<Ingredient>,
    val steps: List<String> = emptyList(),
    val minutes: Int = 0,
    val category: String = "",
    val emoji: String = "🍽️",
    val builtin: Boolean = false
)

@Serializable
data class Profile(
    val name: String = "Alessandro",
    val male: Boolean = true,
    val birthYear: Int = 1997,
    val heightCm: Double = 175.0,
    val startWeight: Double = 75.0,
    val targetWeight: Double = 75.0,
    /** 0 sedentario … 4 molto attivo */
    val activity: Int = 1,
    /** -1 dimagrire, 0 mantenere, 1 aumentare */
    val goal: Int = 0,
    /** kg a settimana (positivo) */
    val weeklyRate: Double = 0.5
)

@Serializable
data class Goals(
    val kcal: Int = 2000,
    val carbsPct: Int = 50,
    val proteinPct: Int = 20,
    val fatPct: Int = 30,
    val waterMl: Int = 2000,
    val steps: Int = 8000,
    val auto: Boolean = true
)

@Serializable
data class WeightEntry(val date: String, val kg: Double)

@Serializable
data class Measurement(val date: String, val type: String, val value: Double)

@Serializable
data class ActivityEntry(
    val id: String,
    val date: String,
    val name: String,
    val minutes: Int,
    val kcal: Int
)

@Serializable
data class FastingSession(val start: Long, val end: Long, val targetHours: Int)

@Serializable
data class Fasting(
    val planId: String = "16:8",
    val activeStart: Long? = null,
    val targetHours: Int = 16,
    val history: List<FastingSession> = emptyList()
)

@Serializable
data class AiConfig(
    val baseUrl: String = "https://openrouter.ai/api/v1",
    /** Modello preferito; se non risponde si provano i successivi in [AiConfig.FALLBACKS]. */
    val model: String = DEFAULT_MODEL,
    val apiKey: String = ""
) {
    companion object {
        /** Gemma 4 31B: modello open-weight di Google con visione, gratuito su OpenRouter (ottobre 2026). */
        const val DEFAULT_MODEL = "google/gemma-4-31b-it:free"
        val FALLBACKS = listOf("google/gemma-4-31b-it:free", "nvidia/nemotron-3.5-super-vl:free", "openrouter/free")
    }
}

@Serializable
data class Reminders(
    val meals: Boolean = false,
    val breakfast: Int = 8 * 60,
    val lunch: Int = 13 * 60,
    val dinner: Int = 20 * 60,
    val water: Boolean = false,
    val weighIn: Boolean = false,
    val weighInTime: Int = 7 * 60 + 30,
    val fasting: Boolean = true
)

// ---------------- Piano alimentare ----------------

/** Un pasto del piano: quale ricetta è scelta per quello slot. Le grammature si calcolano sempre dagli obiettivi. */
@Serializable
data class PlanSlot(
    val slot: Int,                // indice in DietPrefs.slots
    val recipeId: String,
    val eatenEntryIds: List<String> = emptyList()
)

@Serializable
data class DayPlan(val date: String, val slots: List<PlanSlot>)

@Serializable
data class MealSlotDef(val name: String, val meal: String, val share: Int, val diaryMeal: Int)

@Serializable
data class DietPrefs(
    val slots: List<MealSlotDef> = listOf(
        MealSlotDef("Colazione", "colazione", 25, 0),
        MealSlotDef("Pranzo", "pranzo", 35, 1),
        MealSlotDef("Merenda", "spuntino", 10, 3),
        MealSlotDef("Cena", "cena", 30, 2)
    ),
    /** Tag da escludere: lattosio, glutine, pesce, crostacei, carne, maiale, uova, frutta_secca, soia */
    val exclude: List<String> = emptyList(),
    /** Ricette che non vuoi più vedere */
    val disliked: List<String> = emptyList(),
    val maxMinutes: Int = 40
)

// ---------------- Smartwatch / Health Connect ----------------

@Serializable
data class WorkoutSession(
    val title: String,
    val type: Int,
    val start: Long,
    val end: Long,
    val kcal: Double? = null
)

@Serializable
data class HealthDay(
    val date: String,
    val steps: Long = 0,
    /** Calorie attive registrate dal dispositivo (null = non disponibili). */
    val activeKcal: Double? = null,
    val totalKcal: Double? = null,
    val sessions: List<WorkoutSession> = emptyList(),
    val restingHr: Double? = null,
    val avgHr: Double? = null,
    val sleepMinutes: Long? = null,
    val updatedAt: Long = 0
)

/** 0 = livello di attività fisso del profilo; 1 = sedentario + attività registrate a mano; 2 = smartwatch (Health Connect). */
@Serializable
data class EnergySettings(
    val mode: Int = 0,
    /** Quota delle calorie attive dello smartwatch da "restituire" al budget (i dispositivi da polso tendono a sovrastimare). */
    val eatBackPct: Int = 80,
    val watchConnected: Boolean = false,
    val importWeight: Boolean = true,
    val lastSync: Long = 0,
    val syncError: String? = null
)

// ---------------- Dispensa e spesa ----------------

@Serializable
data class PantryItem(
    val id: String,
    val foodId: String? = null,
    val name: String,
    val grams: Double? = null,
    val addedAt: Long = System.currentTimeMillis(),
    /** foto | scontrino | manuale | spesa */
    val source: String = "manuale"
)

@Serializable
data class ShoppingItem(
    val id: String,
    val foodId: String? = null,
    val name: String,
    val needGrams: Double = 0.0,
    val packs: Int = 0,
    val packGrams: Double = 0.0,
    val shop: String = "Altro",
    val checked: Boolean = false,
    val manual: Boolean = false,
    val note: String = ""
)

@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val profile: Profile = Profile(),
    val goals: Goals = Goals(),
    val diary: List<DiaryEntry> = emptyList(),
    val water: Map<String, Int> = emptyMap(),
    val steps: Map<String, Int> = emptyMap(),
    val weights: List<WeightEntry> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val activities: List<ActivityEntry> = emptyList(),
    val customFoods: List<Food> = emptyList(),
    val favorites: List<Food> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val fasting: Fasting = Fasting(),
    val ai: AiConfig = AiConfig(),
    val reminders: Reminders = Reminders(),
    val glassMl: Int = 250,
    // v2
    val plan: List<DayPlan> = emptyList(),
    val dietPrefs: DietPrefs = DietPrefs(),
    val health: Map<String, HealthDay> = emptyMap(),
    val energy: EnergySettings = EnergySettings(),
    val pantry: List<PantryItem> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val shoppingRange: String = "",
    val recipePhotos: Map<String, String> = emptyMap()
)

val MEAL_NAMES = listOf("Colazione", "Pranzo", "Cena", "Spuntini")
val MEAL_EMOJI = listOf("☀️", "🍝", "🌙", "🍎")
