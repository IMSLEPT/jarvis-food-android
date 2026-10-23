package com.jarvis.food.engine

/**
 * Caratteristiche di una ricetta ricavate dagli ingredienti e dalle grammature di base (1 porzione):
 * calorie e macro, diete compatibili, obiettivi nutrizionali, metodi, allergeni e costo stimato.
 * Tutto calcolato dai dati USDA del database, non scritto a mano, così le categorie non possono sbagliare.
 */
data class RecipeInfo(
    val id: String,
    val kcal: Double, val protein: Double, val carbs: Double, val fat: Double, val fiber: Double,
    /** Costo stimato per porzione in euro (prezzi medi al kg per gruppo alimentare: indicativo). */
    val costEur: Double,
    val diets: Set<String>, val goals: Set<String>, val methods: Set<String>,
    val allergens: Set<String>, val foodIds: Set<String>, val mainFoodIds: Set<String>
)

object RecipeMeta {
    val MEALS = listOf("colazione" to "Colazione", "pranzo" to "Pranzo", "cena" to "Cena", "spuntino" to "Spuntino", "dessert" to "Dessert")
    val KCAL_BUCKETS = listOf(
        "<100" to (0.0 to 100.0), "100-200" to (100.0 to 200.0), "200-300" to (200.0 to 300.0), "300-400" to (300.0 to 400.0),
        "400-500" to (400.0 to 500.0), "500-600" to (500.0 to 600.0), "600-700" to (600.0 to 700.0), ">700" to (700.0 to 99999.0)
    )
    val GOALS = listOf(
        "alto_proteico" to "Alto contenuto proteico", "poche_cal" to "Poche calorie", "pochi_grassi" to "Pochi grassi",
        "low_carb" to "Pochi carboidrati", "fibre" to "Ricco di fibre", "bilanciato" to "Bilanciato",
        "ipertrofia" to "Ipertrofia", "ricomposizione" to "Ricomposizione corporea", "dimagrimento" to "Dimagrimento"
    )
    val DIETS = listOf(
        "mediterranea" to "Mediterranea", "vegetariana" to "Vegetariana", "vegana" to "Vegana", "keto" to "Keto",
        "senza_lattosio" to "Senza lattosio", "senza_glutine" to "Senza glutine", "senza_zuccheri" to "Senza zuccheri aggiunti",
        "pescetariana" to "Pescetariana"
    )
    val METHODS = listOf(
        "veloce" to "Veloci (≤ 15 min)", "pochi_ingredienti" to "Pochi ingredienti", "economico" to "Economiche",
        "meal_prep" to "Meal prep", "pranzo_ufficio" to "Pranzo in ufficio", "freddo" to "Piatti freddi",
        "padella" to "In padella", "forno" to "Al forno", "friggitrice" to "Friggitrice ad aria",
        "senza_cottura" to "Senza cottura", "anticipo" to "Da preparare in anticipo"
    )
    val CUISINES = listOf(
        "italiana" to "Italiana", "messicana" to "Messicana", "asiatica" to "Asiatica", "americana" to "Americana",
        "giapponese" to "Giapponese", "indiana" to "Indiana", "mediterranea" to "Mediterranea", "altre" to "Altre cucine"
    )
    val ALLERGENS = listOf(
        "glutine" to "Glutine", "lattosio" to "Lattosio", "uova" to "Uova", "frutta_secca" to "Frutta a guscio",
        "soia" to "Soia", "pesce" to "Pesce", "crostacei" to "Crostacei"
    )
    val DIFFICULTY = listOf("", "Facile", "Media", "Impegnativa")

    fun label(list: List<Pair<String, String>>, key: String) = list.firstOrNull { it.first == key }?.second ?: key

    private val MEAT = setOf("carne_bianca", "carne_rossa", "salumi")
    private val FISH = setOf("pesce", "pesce_grasso")
    private val ADDED_SUGAR = setOf("zucchero", "miele", "marmellata", "crema_nocciole", "biscotti", "cornetto", "cornflakes", "granola", "fondente")
    /** Prezzi medi indicativi in €/kg per gruppo alimentare. */
    private val EUR_KG = mapOf(
        "cereali" to 2.0, "cereali_integrali" to 3.0, "legumi" to 4.0, "verdura" to 3.0, "verdura_foglia" to 7.0, "frutta" to 3.5,
        "latticini" to 9.0, "uova" to 5.0, "carne_bianca" to 9.0, "carne_rossa" to 14.0, "pesce" to 15.0, "pesce_grasso" to 16.0,
        "frutta_secca" to 15.0, "semi" to 12.0, "olio" to 9.0, "condimenti" to 8.0, "spezie" to 30.0, "dolci" to 6.0, "tuberi" to 1.5,
        "grassi" to 8.0, "bevande" to 2.0, "salumi" to 20.0, "erbe" to 20.0, "integratori" to 30.0, "alcol" to 5.0
    )

    private val cache = HashMap<String, RecipeInfo>()

    @Synchronized
    fun info(db: FoodDb, t: RecipeTemplate): RecipeInfo = cache.getOrPut(t.id) { compute(db, t) }

    private fun compute(db: FoodDb, t: RecipeTemplate): RecipeInfo {
        val ing = t.ings.mapNotNull { i -> db.food(i.food)?.let { it to i.g } }
        var k = 0.0; var p = 0.0; var c = 0.0; var f = 0.0; var fib = 0.0; var eur = 0.0
        for ((food, g) in ing) {
            k += food.kcal * g / 100; p += food.protein * g / 100; c += food.carbs * g / 100; f += food.fat * g / 100
            fib += (food.micro["fiber"] ?: 0.0) * g / 100
            if (food.id != "sale") eur += (EUR_KG[food.group] ?: 5.0) * g / 1000
        }
        val groups = ing.map { it.first.group }.toSet()
        val ids = ing.map { it.first.id }.toSet()
        val tags = ing.flatMap { it.first.tags }.toSet()
        val kk = k.coerceAtLeast(1.0)
        val pp = p * 4 / kk; val cp = c * 4 / kk; val fp = f * 9 / kk

        val diets = buildSet {
            val veg = groups.none { it in MEAT || it in FISH }
            if (veg) add("vegetariana")
            if (veg && groups.none { it == "latticini" || it == "uova" } && "miele" !in ids && tags.none { it == "lattosio" || it == "uova" }) add("vegana")
            if (groups.none { it in MEAT }) add("pescetariana")
            if ("lattosio" !in tags) add("senza_lattosio")
            if ("glutine" !in tags) add("senza_glutine")
            if (cp <= 0.12 && fp >= 0.45) add("keto")
            if (ids.none { it in ADDED_SUGAR }) add("senza_zuccheri")
            if (t.cuisine in setOf("italiana", "mediterranea") && groups.none { it == "carne_rossa" || it == "salumi" }) add("mediterranea")
        }
        val goals = buildSet {
            if (pp >= 0.28 && p >= 25) add("alto_proteico")
            if (k <= 350) add("poche_cal")
            if (fp <= 0.22) add("pochi_grassi")
            if (cp <= 0.25) add("low_carb")
            if (fib >= 8) add("fibre")
            if (pp in 0.18..0.35 && cp in 0.35..0.55 && fp in 0.20..0.35) add("bilanciato")
            if (k >= 600 && p >= 35) add("ipertrofia")
            if (pp >= 0.25 && k in 350.0..600.0) add("ricomposizione")
            if (k <= 450 && p >= 20) add("dimagrimento")
        }
        val main = t.ings.filter { it.role != "x" }
        val methods = buildSet {
            addAll(t.methods)
            if (t.minutes <= 15) add("veloce")
            if (main.size <= 5) add("pochi_ingredienti")
            if (eur <= 1.8) add("economico")
        }
        val allergens = buildSet {
            tags.forEach { tg -> if (ALLERGENS.any { it.first == tg }) add(tg) }
            if ("frutta_secca" in tags) add("frutta_secca")
        }
        return RecipeInfo(t.id, k, p, c, f, fib, eur, diets, goals, methods, allergens, ids, main.map { it.food }.toSet())
    }

    fun costLabel(eur: Double) = when { eur <= 1.8 -> "€ economica"; eur <= 3.2 -> "€€ media"; else -> "€€€ più cara" }

    /** Attrezzi da cucina ricavati dal metodo e dagli ingredienti. */
    fun tools(t: RecipeTemplate): List<String> = buildList {
        val m = t.methods
        if ("padella" in m) add("Padella antiaderente")
        if ("forno" in m) add("Forno e teglia con carta forno")
        if ("friggitrice" in m) add("Friggitrice ad aria")
        if (t.steps.any { it.contains("frull", true) }) add("Frullatore")
        if (t.steps.any { it.contains("pentol", true) || it.contains("bollent", true) || it.contains("bolli", true) || it.contains("Cuoci", true) && "padella" !in m && "forno" !in m }) add("Pentola")
        if (t.steps.any { it.contains("microonde", true) }) add("Microonde")
        if (t.steps.any { it.contains("wok", true) }) add("Wok o padella ampia")
        add("Coltello e tagliere")
    }.distinct()

    /** Sostituzioni consigliate per alcuni ingredienti (stesso ruolo, simile profilo nutrizionale). */
    private val SUBS = mapOf(
        "pollo" to "tacchino (stessi grammi)", "tacchino" to "pollo (stessi grammi)", "pollo_cosce" to "petto di pollo (meno grassi)",
        "salmone" to "trota o sgombro", "merluzzo" to "branzino o nasello (stessi grammi)", "tonno_nat" to "sgombro al naturale",
        "gamberi" to "calamari o merluzzo", "pasta" to "pasta integrale (più fibre) o di legumi", "riso" to "riso integrale, farro o orzo",
        "basmati" to "riso integrale o quinoa", "couscous" to "bulgur o quinoa", "pane" to "pane integrale", "piadina" to "tortilla integrale",
        "greco0" to "yogurt di soia (versione vegetale)", "latte_ps" to "bevanda di soia o di mandorla (senza lattosio)",
        "ricotta" to "tofu (vegano)", "mozzarella" to "mozzarella light o tofu", "feta" to "ricotta salata o tofu",
        "parmigiano" to "pecorino (più sapido, usane meno)", "uova" to "tofu strapazzato (vegano)", "burro" to "olio extravergine (metà quantità)",
        "panna" to "latte di cocco o yogurt greco", "macinato" to "macinato di pollo o di tacchino", "olio" to "olio di semi di sesamo o di avocado",
        "miele" to "sciroppo d'acero o datteri", "arachidi" to "crema di mandorle", "avena" to "fiocchi di riso o di quinoa (senza glutine)"
    )
    fun substitutions(t: RecipeTemplate): List<String> =
        t.ings.mapNotNull { i -> SUBS[i.food]?.let { "${i.label?.replaceFirstChar { c -> c.uppercase() } ?: i.food.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }} → $it" } }.take(5)

    /** Breve descrizione ricavata dai dati della ricetta (cucina, pasto, tempo e ingredienti principali). */
    fun description(db: FoodDb, t: RecipeTemplate): String {
        val main = t.ings.filter { it.role != "x" }.take(4).map { (it.label ?: db.food(it.food)?.name ?: it.food.replace('_', ' ')).lowercase() }
        val meal = when (t.meal) { "colazione" -> "colazione"; "pranzo" -> "pranzo"; "cena" -> "cena"; "dessert" -> "dessert"; else -> "spuntino" }
        val cu = label(CUISINES, t.cuisine).lowercase()
        return "Ricetta $cu per $meal, pronta in ${t.minutes} minuti" + if (main.isNotEmpty()) ": " + main.joinToString(", ") + "." else "."
    }
}
