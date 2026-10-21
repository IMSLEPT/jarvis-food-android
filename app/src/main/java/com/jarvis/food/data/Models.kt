package com.jarvis.food.data

import kotlinx.serialization.Serializable

/** Valori nutrizionali sempre riferiti a 100 g (o 100 ml). */
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
    /** base | off | custom | ai | recipe | quick */
    val source: String = "base",
    val liquid: Boolean = false,
    val category: String = ""
)

@Serializable
data class DiaryEntry(
    val id: String,
    val date: String,          // yyyy-MM-dd
    val meal: Int,             // 0 colazione, 1 pranzo, 2 cena, 3 spuntini
    val food: Food,
    val grams: Double,
    val time: Long
)

@Serializable
data class Ingredient(val food: Food, val grams: Double)

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
    val model: String = "openrouter/free",
    val apiKey: String = ""
)

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
    val glassMl: Int = 250
)

val MEAL_NAMES = listOf("Colazione", "Pranzo", "Cena", "Spuntini")
val MEAL_EMOJI = listOf("☀️", "🍝", "🌙", "🍎")
