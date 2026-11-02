package com.jarvis.food.ai

import java.util.Locale

/** Classifier scores are ranking scores, not calibrated probabilities or portion weights. */
internal object OfflineFoodLabels {
    private val italian = mapOf(
        "pizza" to "Pizza", "pasta" to "Pasta", "spaghetti" to "Spaghetti", "lasagna" to "Lasagne", "lasagne" to "Lasagne",
        "risotto" to "Risotto", "rice" to "Riso", "fried rice" to "Riso", "sushi" to "Sushi", "sashimi" to "Sashimi",
        "salad" to "Insalata", "caesar salad" to "Insalata", "greek salad" to "Insalata greca", "fruit salad" to "Macedonia",
        "quiche" to "Torta salata", "tart" to "Crostata", "apple pie" to "Torta di mele", "cheesecake" to "Cheesecake",
        "waffle" to "Waffle", "waffles" to "Waffle", "pancakes" to "Pancake", "pancake" to "Pancake", "omelette" to "Frittata",
        "french fries" to "Patate fritte", "baked potato" to "Patate al forno", "mashed potato" to "Purè di patate",
        "hamburger" to "Hamburger", "cheeseburger" to "Hamburger", "sandwich" to "Panino", "hot dog" to "Hot dog",
        "grilled salmon" to "Salmone", "salmon" to "Salmone", "fish" to "Pesce", "tuna" to "Tonno", "shrimp" to "Gamberi",
        "chicken" to "Pollo", "roast chicken" to "Pollo", "grilled chicken" to "Pollo", "steak" to "Bistecca",
        "egg" to "Uova", "eggs" to "Uova", "scrambled eggs" to "Uova strapazzate", "bread" to "Pane",
        "yogurt" to "Yogurt", "ice cream" to "Gelato", "chocolate cake" to "Torta al cioccolato", "tiramisu" to "Tiramisù",
        "banana" to "Banana", "apple" to "Mela", "orange" to "Arancia", "strawberry" to "Fragole", "strawberries" to "Fragole",
        "broccoli" to "Broccoli", "carrot" to "Carote", "tomato" to "Pomodori", "potato" to "Patate", "potatoes" to "Patate",
        "soup" to "Zuppa", "vegetable soup" to "Minestrone", "lentil soup" to "Zuppa di lenticchie",
        "spaghetti bolognese" to "Pasta al ragù", "spaghetti carbonara" to "Pasta alla carbonara", "ravioli" to "Ravioli",
        "gnocchi" to "Gnocchi", "bruschetta" to "Bruschetta", "caprese salad" to "Insalata caprese", "hummus" to "Hummus",
        "falafel" to "Falafel", "croissant" to "Cornetto", "oatmeal" to "Porridge", "porridge" to "Porridge"
    )
    fun readable(label: String): String? = italian[label.trim().lowercase(Locale.ROOT).replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")]

    fun select(raw: List<AiLabel>): List<AiLabel> {
        val ranked = raw.filter { it.score.isFinite() && it.score in 0f..1f }.sortedByDescending { it.score }
        val top = ranked.firstOrNull() ?: return emptyList()
        // Conservative abstention rule; does not claim a measured recognition accuracy.
        if (top.score < .20f || (ranked.getOrNull(1)?.let { top.score - it.score < .05f } == true)) return emptyList()
        return ranked.filter { it.score >= .20f && it.score >= top.score * .7f }
            .mapNotNull { item -> readable(item.label)?.let { AiLabel(it, item.score) } }.distinctBy { it.label }.take(3)
    }
}
