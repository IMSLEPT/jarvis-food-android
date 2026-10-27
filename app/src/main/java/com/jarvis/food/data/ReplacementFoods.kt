package com.jarvis.food.data

/** One photographed dish may contain several ingredients; it still replaces exactly one diary entry. */
object ReplacementFoods {
    fun combine(portions: List<Pair<Food, Double>>, id: String): Pair<Food, Double> {
        require(portions.isNotEmpty() && portions.all { it.second.isFinite() && it.second > 0 })
        if (portions.size == 1) return portions.single()
        val grams = portions.sumOf { it.second }
        val total = portions.fold(Totals()) { sum, (food, amount) -> sum + Nutrition.of(food, amount) }
        val k = 100.0 / grams
        val micro = portions.flatMap { (food, amount) -> food.micro.map { it.key to it.value * amount / 100 } }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() * k }
        return Food(id, portions.joinToString(" + ") { it.first.name }.take(240),
            kcal = total.kcal * k, protein = total.protein * k, carbs = total.carbs * k, fat = total.fat * k,
            fiber = total.fiber * k, sugar = total.sugar * k, satFat = total.satFat * k, salt = total.salt * k,
            source = "ai", micro = micro) to grams
    }
}
