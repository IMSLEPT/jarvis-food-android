package com.jarvis.food.data

/** The diary and plan use the same six stable meal IDs. Slot positions may change; IDs never do. */
object MealSchedule {
    val order = listOf(0, 4, 1, 3, 2, 5)
    val definitions = listOf(
        MealSlotDef("Colazione", "colazione", 20, 0),
        MealSlotDef("Spuntino mattutino", "spuntino", 10, 4),
        MealSlotDef("Pranzo", "pranzo", 30, 1),
        MealSlotDef("Merenda", "spuntino", 10, 3),
        MealSlotDef("Cena", "cena", 25, 2),
        MealSlotDef("Spuntino serale", "spuntino", 5, 5)
    )
    fun valid(prefs: DietPrefs) = prefs.slots.isNotEmpty() && prefs.slots.all { it.diaryMeal in order && it.share > 0 } &&
        prefs.slots.map { it.diaryMeal }.distinct().size == prefs.slots.size
    fun selected(prefs: DietPrefs) = order.filter { meal -> prefs.slots.any { it.diaryMeal == meal } }
    fun visible(s: AppState, date: String) = order.filter { it in selected(s.dietPrefs) || s.diary.any { e -> e.date == date && e.meal == it } }
    fun withMeals(prefs: DietPrefs, meals: List<Int>): DietPrefs {
        val ids = order.filter { it in meals }
        require(ids.isNotEmpty())
        val slots = ids.map { id -> prefs.slots.firstOrNull { it.diaryMeal == id } ?: definitions.first { it.diaryMeal == id } }
        return prefs.copy(slots = slots)
    }
    /** Remap existing plans and diary plan links by stable meal ID. Never delete logged food. */
    fun apply(s: AppState, prefs: DietPrefs): AppState {
        require(valid(prefs))
        val old = s.dietPrefs.slots
        val indexMap = old.mapIndexedNotNull { i, def -> prefs.slots.indexOfFirst { it.diaryMeal == def.diaryMeal }.takeIf { it >= 0 }?.let { i to it } }.toMap()
        val plan = s.plan.map { day -> day.copy(slots = day.slots.mapNotNull { slot -> indexMap[slot.slot]?.let { slot.copy(slot = it) } }) }
        val diary = s.diary.map { entry ->
            val parts = entry.planRef?.split('#')
            val oldIndex = parts?.lastOrNull()?.toIntOrNull()
            if (parts?.size != 2 || oldIndex == null) entry else entry.copy(planRef = indexMap[oldIndex]?.let { "${parts[0]}#$it" })
        }
        return s.copy(dietPrefs = prefs, plan = plan, diary = diary)
    }
    fun advice(p: Profile): Pair<List<Int>, String> = when {
        p.goal == 1 || p.workouts >= 3 -> listOf(0, 1, 3, 2) to
            "Proposta pratica: 3 pasti e una merenda, utile vicino all'allenamento. Distribuisci le proteine tra i pasti. Aggiungi altri spuntini se aiutano con fame, orari e quantità; sei pasti non sono obbligatori."
        p.goal == -1 -> listOf(0, 1, 2) to
            "Proposta pratica: colazione, pranzo e cena; aggiungi uno spuntino se ti aiuta a gestire la fame. Il numero di pasti non aumenta da solo il dimagrimento: contano soprattutto il totale giornaliero e la continuità."
        else -> listOf(0, 1, 3, 2) to
            "Proposta pratica: 3 pasti e una merenda. Puoi scegliere da uno a sei momenti in base a fame e orari, mantenendo gli stessi obiettivi giornalieri."
    }
}
