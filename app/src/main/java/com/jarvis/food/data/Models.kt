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
    val tags: List<String> = emptyList(),
    /** Grammi di alimento cotto per grammo di crudo (stima da dati USDA); null se non applicabile. */
    val cooked: Double? = null
)

@Serializable
data class DiaryEntry(
    val id: String,
    val date: String,          // yyyy-MM-dd
    val meal: Int,             // 0 colazione, 1 pranzo, 2 cena, 3 merenda, 4 spuntino mattutino, 5 spuntino serale
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
    val builtin: Boolean = false,
    /** Descrizione, foto (percorso file), cucina, difficoltà e pasto: facoltativi, per le ricette create dall'utente. */
    val description: String = "",
    val photo: String? = null,
    val cuisine: String = "",
    val difficulty: Int = 1,
    val meal: String = ""
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
    val weeklyRate: Double = 0.5,
    // ---- Questionario esteso (calcolo del fabbisogno più preciso) ----
    /** Data di nascita completa (yyyy-MM-dd); se assente si usa [birthYear]. */
    val birthDate: String? = null,
    /** Percentuale di massa grassa, se la conosci: abilita la formula di Katch-McArdle. */
    val bodyFatPct: Double? = null,
    /** Lavoro: 0 seduto, 1 in piedi o in movimento, 2 fisico. */
    val job: Int = 0,
    /** Passi giornalieri tipici: 0 meno di 4.000, 1 4-7.000, 2 7-10.000, 3 10-14.000, 4 oltre 14.000. */
    val stepsLevel: Int = 2,
    /** Allenamenti a settimana. */
    val workouts: Int = 3,
    val workoutMin: Int = 45,
    /** 0 leggero, 1 moderato, 2 intenso. */
    val workoutIntensity: Int = 1,
    /** 0 misto, 1 pesi/calisthenics, 2 cardio (corsa, bici, nuoto), 3 sport di squadra. */
    val trainingType: Int = 0,
    /** true se il profilo è stato compilato con il questionario esteso (altrimenti vale il vecchio livello [activity]). */
    val detailed: Boolean = false,
    /** Preferenza macro: 0 bilanciato, 1 proteico, 2 low carb. */
    val dietStyle: Int = 0,
    /** 0 nessuna, 1 gravidanza o allattamento, 2 patologie da seguire col medico. */
    val special: Int = 0,
    /** Foto profilo (file nella memoria privata dell'app). */
    val photo: String? = null
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
    val apiKey: String = "",
    /** Motore del chatbot Jarvis: 0 sul telefono (illimitato, offline), 1 cloud gratuito. */
    val chatEngine: Int = 0,
    /** Modello sul telefono: 0 Gemma 4 E2B (veloce), 1 Gemma 4 E4B (più intelligente). */
    val localModel: Int = 1,
    /** Accelerazione GPU per il modello sul telefono (sperimentale: più veloce ma non su tutti i dispositivi). */
    val chatGpu: Boolean = false
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
    val recipePhotos: Map<String, String> = emptyMap(),
    /** Ricette del catalogo preferite, viste di recente (più recente per prima) e numero di aperture. */
    val recipeFavs: List<String> = emptyList(),
    val recipeRecent: List<String> = emptyList(),
    val recipeViews: Map<String, Int> = emptyMap(),
    /** Conversazione con Jarvis (ultimi messaggi). */
    val chat: List<ChatMsg> = emptyList(),
    /** Collegamento alla famiglia (spesa condivisa); null se non collegato. */
    val family: FamilyLink? = null
)

/** Collegamento di questo telefono alla famiglia: codice d'invito (contiene la chiave di cifratura), il proprio id e nome, giorni del piano da condividere. */
@Serializable
data class FamilyLink(val code: String, val memberId: String, val name: String, val span: Int = 3, val lastSync: Long = 0)

/** Messaggio della chat con Jarvis. [role] è "user" o "assistant". */
@Serializable
data class ChatMsg(
    val id: String,
    val role: String,
    val text: String,
    val time: Long,
    /** Azioni eseguite da Jarvis nell'app per questo messaggio (testo leggibile). */
    val actions: List<String> = emptyList(),
    /** Risposte rapide suggerite sotto il messaggio. */
    val suggestions: List<String> = emptyList(),
    /** Azioni che eliminano dati e aspettano la conferma dell'utente (non ancora eseguite). */
    val pending: List<PendingCall> = emptyList()
)

/** Azione della chat in attesa di conferma: [label] descrive cosa verrebbe eliminato, [args] sono gli argomenti in JSON. */
@Serializable
data class PendingCall(val name: String, val args: String, val label: String)

val MEAL_NAMES = listOf("Colazione", "Pranzo", "Cena", "Merenda", "Spuntino mattutino", "Spuntino serale")
val MEAL_EMOJI = listOf("☀️", "🍝", "🌙", "🍎")
