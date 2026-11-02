package com.jarvis.food.engine

/** Stable cells in the bundled 6 x 6 illustration atlas. No network request or generated food inference. */
object FoodArtCatalog {
    const val COLUMNS = 6
    const val COUNT = 36
    const val ASSET = "illustrations/food-atlas-v1.png"
    data class Art(val cell: Int?, val emoji: String, val specific: Boolean)
    private val matches = listOf(
        35 to listOf("porridge"), 34 to listOf("burro di arachidi", "crema di arachidi", "arachidi spalm"),
        0 to listOf("trota", "trote"), 1 to listOf("salmone"), 2 to listOf("sgombro"), 3 to listOf("sardine", "sardina"),
        4 to listOf("uovo", "uova", "albume"), 5 to listOf("fung", "champignon"), 6 to listOf("banana", "banane"),
        7 to listOf("yogurt", "skyr", "kefir"), 8 to listOf("arancia", "arance", "mandarin"), 9 to listOf("mela", "mele"),
        10 to listOf("noci", "mandorl", "anacard", "nocciole", "pistacch", "arachidi"), 11 to listOf("avena", "fiocchi"),
        12 to listOf("ceci", "lenticch", "fagiol", "legumi"), 13 to listOf("spinac", "bietol"), 14 to listOf("broccoli", "broccolo"),
        15 to listOf("carot"), 16 to listOf("pomodor"), 17 to listOf("pane", "fette biscottate"),
        18 to listOf("riso", "risotto"), 19 to listOf("pasta", "spaghetti", "penne", "fusilli"),
        20 to listOf("pollo", "tacchino"), 21 to listOf("manzo", "bovino", "vitello"),
        22 to listOf("formaggio", "parmigian", "grana", "pecorino", "mozzarella", "ricotta"), 23 to listOf("latte"),
        24 to listOf("olio", "olive"), 25 to listOf("cioccolat", "cacao"), 26 to listOf("fragol"), 27 to listOf("avocado"),
        28 to listOf("patat"), 29 to listOf("acqua"), 30 to listOf("tonno"), 31 to listOf("tofu"),
        32 to listOf("insalat", "valeriana", "lattuga"), 33 to listOf("pera", "pere")
    )
    fun resolve(name: String, group: String = ""): Art {
        val n = FoodDb.normalize(name)
        val cell = matches.firstOrNull { (_, terms) -> terms.any { term ->
            n.split(' ', '-', ':').any { token -> (if (term in setOf("mela", "mele", "pera", "pere", "pane", "riso", "latte", "olio", "olive", "uovo", "uova")) token == term else token.startsWith(term)) } || (term.contains(' ') && n.contains(term))
        } }?.first
        if (cell != null) return Art(cell, "🍽️", true)
        val emoji = when {
            group.startsWith("pesce") -> "🐟"
            group.startsWith("verdura") -> "🥬"
            group.startsWith("carne") -> "🍗"
            group == "frutta" -> "🍎"
            group == "legumi" -> "🫘"
            else -> "🍽️"
        }
        return Art(null, emoji, false)
    }
}
