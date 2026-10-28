package com.jarvis.food.ui

/** One rounded outline family for navigation, recipe categories and empty states. */
private object Symbols {
    val Leaf = "M4 20l12-12M4 16C2 7 10 3 21 3c0 11-5 18-13 16M10 14v-4M14 10h4"
    val Wheat = "M12 21V4M12 8C5 8 5 4 5 4c7 0 7 4 7 4M12 13c-7 0-7-4-7-4 7 0 7 4 7 4M12 8c7 0 7-4 7-4-7 0-7 4-7 4M12 13c7 0 7-4 7-4-7 0-7 4-7 4M12 18c-7 0-7-4-7-4 7 0 7 4 7 4M12 18c7 0 7-4 7-4-7 0-7 4-7 4"
    val Fish = "M4 12c4-7 10-7 15 0-5 7-11 7-15 0zM4 12l-3-4v8zM15 10h.1M10 6l2-3 2 3M10 18l2 3 2-3"
    val Dumbbell = "M3 9v6M6 6v12M18 6v12M21 9v6M6 12h12"
    val Target = Ic.circ(12.0,12.0,9.0) + Ic.circ(12.0,12.0,5.0) + Ic.circ(12.0,12.0,1.0)
    val Globe = Ic.circ(12.0,12.0,9.0) + "M3 12h18M12 3c-6 5-6 13 0 18M12 3c6 5 6 13 0 18"
    val Pan = "M4 12h13a6 6 0 0 1-12 0M17 12l5-4M8 8V5M13 8V5"
    val Oven = Ic.rr(4.0,3.0,16.0,18.0,2.0) + "M4 8h16M8 5.5h.1M12 5.5h.1M16 5.5h.1M7 11h10v7H7z"
    val Dessert = "M4 13h16v8H4zM4 17h16M6 13l3-7 11 7M9 6V3M7 3h4"
    val Milk = "M8 3h8v4l3 4v10H5V11l3-4zM8 7h8M5 12h14"
    val Snow = "M12 3v18M4 7l16 10M4 17L20 7M9 5l3 3 3-3M9 19l3-3 3 3M4 10l4-1V5M20 14l-4 1v4M4 14l4 1v4M20 10l-4-1V5"
    val Box = Ic.rr(3.0,7.0,18.0,14.0,2.0) + "M8 7V3h8v4M3 12h18M10 12v3h4v-3"
    val Balance = "M12 3v17M7 21h10M4 7h16M6 7l-4 8h8zM18 7l-4 8h8z"
}

fun mealSymbol(meal: Int): String = when (meal) {
    0 -> Ic.MealBreakfast
    1 -> Ic.MealLunch
    2 -> Ic.Bowl
    5 -> Ic.MealDinner
    else -> Ic.MealSnack
}

fun styleSymbol(emoji: String, label: String = ""): String {
    val name = label.lowercase()
    return when {
        "kcal" in name || "calorie" in name -> Ic.Flame
        " min" in name || "tempo" in name || "anticipo" in name -> Ic.Clock
        "tutte le ricette" in name -> Ic.Search
        emoji in setOf("☕", "🥐", "🥞") || "colazione" in name -> Ic.MealBreakfast
        emoji in setOf("🍎", "🍏", "🍊") || "spuntino" in name || "merenda" in name -> Ic.MealSnack
        emoji in setOf("🍰", "🧁", "🍬") || "dessert" in name -> Symbols.Dessert
        emoji in setOf("💪", "🏋️") || "proteic" in name || "ipertrofia" in name -> Symbols.Dumbbell
        emoji == "🎯" || "obiettivo" in name || "dimagrimento" in name -> Symbols.Target
        emoji == "⚖️" || "bilanciat" in name -> Symbols.Balance
        emoji in setOf("🔄", "🔁") || "ricomposizione" in name -> Ic.Swap
        emoji == "🌾" || "glutine" in name || "fibre" in name -> Symbols.Wheat
        emoji == "🐟" || "pescetarian" in name -> Symbols.Fish
        emoji == "🥛" || "lattosio" in name -> Symbols.Milk
        emoji in setOf("🌿", "🌱", "🥕", "🥦", "🥬", "🍃", "🍅") || "vegan" in name || "vegetarian" in name -> Symbols.Leaf
        emoji == "❄️" || "freddo" in name -> Symbols.Snow
        emoji == "🔥" || "forno" in name -> Symbols.Oven
        emoji == "🍳" || "padella" in name -> Symbols.Pan
        emoji == "💨" || "friggitrice" in name -> Symbols.Oven
        emoji == "⚡" || "veloce" in name -> Ic.Bolt
        emoji == "✨" || "ingredienti" in name -> Ic.Spark
        emoji in setOf("🍱", "💼") || "ufficio" in name || "meal prep" in name -> Symbols.Box
        emoji in setOf("🌍", "🌊", "🇮🇹", "🌮", "🥢", "🍣", "🍔", "🍛") || "cucina" in name -> Symbols.Globe
        emoji == "💧" || "acqua" in name -> Ic.Drop
        emoji in setOf("📅", "🗓️") -> Ic.Calendar
        emoji in setOf("🔎", "🔍") -> Ic.Search
        emoji == "💶" || "economic" in name -> Ic.Receipt
        else -> Ic.Bowl
    }
}
