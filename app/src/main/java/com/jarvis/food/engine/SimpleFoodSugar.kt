package com.jarvis.food.engine

/** Conservative identity rules for these exact plain USDA catalogue records, per WHO definition.
 * This is classification of intrinsic sugars, not inference from a packaged product's name.
 * Brand products, sweetened variants and all other foods remain unknown. */
object SimpleFoodSugar {
    private val intrinsic = setOf("avena", "salmone", "sgombro", "sardine", "trota", "uova", "yogurt", "greco0", "greco_intero", "yogurt_magro", "mela", "banana", "arancia", "noci")
    fun free(food: DbFood): Double? = when {
        food.id in intrinsic -> 0.0
        food.id in setOf("miele", "zucchero", "succo_arancia") -> food.micro["sugar"]
        else -> null
    }
    fun added(food: DbFood): Double? = when {
        food.id in intrinsic -> 0.0
        food.id == "zucchero" -> food.micro["sugar"]
        else -> null
    }
    fun note(food: DbFood): String? = if (free(food) == null) null else
        "Classificazione OMS degli zuccheri del preciso alimento semplice USDA del catalogo; non è un’analisi del tuo prodotto di marca."
}
