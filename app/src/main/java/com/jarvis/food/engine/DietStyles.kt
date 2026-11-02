package com.jarvis.food.engine

import com.jarvis.food.data.*
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

data class DietStyle(val id: String, val title: String, val description: String)
data class StyledPlanResult(val days: List<DayPlan> = emptyList(), val problem: String? = null)

/** Explicit ingredient rules. No fallback may silently discard a style or an exclusion. */
object DietStyles {
    val options = listOf(
        DietStyle("balanced", "Bilanciata e varia", "Varietà di fonti vegetali e animali, con preferenza per integrali, legumi, frutta e verdura. Non promette una salute ottimale per tutti."),
        DietStyle("mediterranean", "Mediterranea", "Preparazioni mediterranee, olio d’oliva, cereali, legumi, verdura e pesce."),
        DietStyle("omnivore", "Onnivora", "Tutti i gruppi alimentari, rispettando esclusioni e preferenze personali."),
        DietStyle("vegetarian", "Vegetariana", "Esclude carne e pesce; comprende uova e latticini compatibili con le tue esclusioni."),
        DietStyle("vegan", "Vegana", "Esclude gli ingredienti animali, compresi latte, uova e miele."),
        DietStyle("pescatarian", "Pescetariana", "Esclude la carne; comprende pesce, vegetali, uova e latticini."),
        DietStyle("japanese", "Giapponese", "Ricette del catalogo di cucina giapponese: riso, pesce, tofu, edamame e preparazioni coerenti."),
        DietStyle("keto", "Chetogenica", "Molto pochi carboidrati. Disponibile soltanto se i tuoi target sono già compatibili: non vengono ridotti automaticamente."),
        DietStyle("lowcarb", "A basso contenuto di carboidrati", "Richiede un target già inferiore a 130 g di carboidrati al giorno."),
        DietStyle("paleo", "Paleo", "Esclude cereali, legumi, latticini, zuccheri aggiunti e prodotti da forno; usa carne, pesce, uova, frutta, verdura e frutta a guscio."),
        DietStyle("dash", "DASH", "Preferenza per vegetali, integrali e latticini magri; controllo del sodio entro 2.300 mg/die dai valori noti del catalogo. Non è una prescrizione per l’ipertensione."),
        DietStyle("hypocaloric", "Ipocalorica", "Si applica solo quando il tuo obiettivo è già sotto il mantenimento stimato. Non riduce le kcal impostate."),
        DietStyle("highprotein", "Ad alto contenuto proteico", "Usa fonti proteiche varie, mantenendo i grammi di proteine già impostati; richiede un target coerente."),
        DietStyle("carnivore", "Carnivora", "Solo alimenti animali e sale. È una scelta molto restrittiva, non presentata come più salutare; potrebbe essere incompatibile con i tuoi macro."),
        DietStyle("glutenfree", "Senza glutine", "Esclude gli ingredienti con glutine identificati nel catalogo. Per celiachia verifica anche etichette e contaminazioni."),
        DietStyle("lactosefree", "Senza lattosio", "Esclude gli ingredienti con lattosio identificati nel catalogo.")
    )
    fun get(id: String) = options.firstOrNull { it.id == id } ?: options.first()
    private val meat = setOf("carne_bianca", "carne_rossa", "salumi")
    private val fish = setOf("pesce", "pesce_grasso")
    private val animal = meat + fish + setOf("uova", "latticini")
    private val sugars = setOf("zucchero", "miele", "sciroppo_acero", "marmellata", "confettura", "cioccolato", "cioccolato_latte")

    // Separate deterministic recipes for a strict animal-only style. They are resolved by ID
    // without changing the existing recipe catalogue or its photography.
    val strictRecipes: List<RecipeTemplate> = listOf(
        RecipeTemplate("style-carnivore-eggs", "Uova e albumi al burro", "colazione", 10,
            listOf(TemplateIngredient("uova",100.0,"p"),TemplateIngredient("albume",100.0,"p"),TemplateIngredient("burro",15.0,"f")),
            listOf("Sbatti {uova} e {albume}.", "Sciogli {burro} in una padella antiaderente, aggiungi le uova e cuoci mescolando finché sono completamente rapprese.")),
        RecipeTemplate("style-carnivore-steak", "Manzo in padella al burro", "pranzo", 15,
            listOf(TemplateIngredient("manzo_magro",180.0,"p"),TemplateIngredient("burro",25.0,"f")),
            listOf("Scalda la padella e sciogli {burro}.", "Aggiungi {manzo_magro} e cuoci bene su entrambi i lati; lascia riposare prima di servire.")),
        RecipeTemplate("style-carnivore-fish", "Salmone e uova al vapore", "cena", 20,
            listOf(TemplateIngredient("salmone",180.0,"p"),TemplateIngredient("uova",50.0,"p"),TemplateIngredient("burro",15.0,"f")),
            listOf("Cuoci {uova} in acqua fino a rassodarle, poi sgusciale.", "Cuoci {salmone} al vapore fino a completa cottura. Servi con le uova e {burro} sciolto.")),
        RecipeTemplate("style-carnivore-snack", "Yogurt greco intero e parmigiano", "spuntino", 5,
            listOf(TemplateIngredient("greco_intero",170.0,"p"),TemplateIngredient("parmigiano",30.0,"f")),
            listOf("Pesa {greco_intero} e {parmigiano}; servi lo yogurt e il formaggio separatamente."))
    )

    fun allowed(db: FoodDb, r: RecipeTemplate, id: String): Boolean {
        if (id != "carnivore" && r.id.startsWith("style-carnivore-")) return false
        val foods = r.ings.mapNotNull { db.food(it.food) }
        if (foods.size != r.ings.size) return false
        val groups = foods.map { it.group }.toSet()
        val ids = foods.map { it.id }.toSet()
        val tags = foods.flatMap { it.tags }.toSet()
        val info by lazy { RecipeMeta.info(db, r) }
        return when (id) {
            "vegetarian" -> groups.none { it in meat || it in fish }
            "vegan" -> groups.none { it in meat || it in fish || it == "uova" } &&
                foods.none { (it.group == "latticini" && it.id != "soia_drink") || it.id == "miele" || "lattosio" in it.tags || "uova" in it.tags }
            "pescatarian" -> groups.none { it in meat }
            "japanese" -> r.cuisine == "giapponese"
            "mediterranean" -> "mediterranea" in info.diets && "alcol" !in groups
            "paleo" -> groups.none { it in setOf("cereali", "cereali_integrali", "legumi", "latticini", "dolci", "salumi", "alcol", "integratori") } && ids.none { it in sugars } && "soia" !in tags
            "carnivore" -> foods.all { (it.group in animal && "soia" !in it.tags && it.id != "soia_drink") || it.id == "sale" }
            "dash" -> groups.none { it in setOf("salumi", "carne_rossa", "dolci", "alcol") } &&
                foods.none { it.id in setOf("burro", "olio_cocco", "olio_palma", "panna") } &&
                info.kcal > 0 && foods.sumOf { f -> val grams = r.ings.first { it.food == f.id }.g; (f.micro["sodium"] ?: 0.0) * grams / 100 } <= info.kcal
            "keto" -> "keto" in info.diets && "alcol" !in groups
            "lowcarb" -> info.carbs * 4 <= info.kcal * .4 && "alcol" !in groups
            "glutenfree" -> "glutine" !in tags
            "lactosefree" -> "lattosio" !in tags
            else -> "alcol" !in groups
        }
    }

    fun targetProblem(id: String, target: Macro, maintenance: Double? = null): String? = when {
        id == "keto" && (target.carbs > 50 || target.carbs * 4 > target.kcal * .10) -> "Il target è ${target.carbs.toInt()} g di carboidrati: non è compatibile con il profilo chetogenico dell’app (≤50 g e ≤10% delle kcal). I tuoi obiettivi restano invariati."
        id == "carnivore" && target.carbs > 20 -> "Il target di ${target.carbs.toInt()} g di carboidrati non è raggiungibile con un piano di soli alimenti animali. Scegli un altro stile mantenendo i tuoi target."
        id == "lowcarb" && target.carbs >= 130 -> "Il tuo target di carboidrati è ≥130 g/die. Non posso chiamare low carb un piano che lo mantiene."
        id == "highprotein" && target.protein * 4 < target.kcal * .20 -> "Le proteine impostate sono meno del 20% delle kcal. Non vengono aumentate automaticamente: scegli uno stile compatibile."
        id == "hypocaloric" && maintenance != null && target.kcal >= maintenance -> "L’obiettivo attuale non è sotto il mantenimento stimato. Non posso rendere ipocalorico il piano senza modificare le kcal; nessuna modifica applicata."
        else -> null
    }

    fun preferenceScore(db: FoodDb, r: RecipeTemplate, id: String): Double {
        if (id !in setOf("balanced", "mediterranean", "dash", "highprotein")) return 0.0
        val groups = db.recipeGroups(r)
        return (if (groups.any { it in setOf("cereali_integrali", "legumi", "verdura", "verdura_foglia", "frutta") }) -.25 else 0.0) +
            (if ("salumi" in groups || "dolci" in groups) 1.5 else 0.0)
    }

    fun build(db: FoodDb, prefs: DietPrefs, start: LocalDate, count: Int, existing: List<DayPlan>, id: String,
        targets: (LocalDate) -> Macro, maintenance: (LocalDate) -> Double): StyledPlanResult {
        if (id !in options.map { it.id }) return StyledPlanResult(problem = "Tipo di alimentazione non riconosciuto.")
        val selectedPrefs = prefs.copy(styleId = id)
        for (date in (0 until count).map { start.plusDays(it.toLong()) }) {
            targetProblem(id, targets(date), maintenance(date))?.let { return StyledPlanResult(problem = it) }
        }
        for (slot in prefs.slots) if (Planner.candidates(db, slot.meal, selectedPrefs).isEmpty())
            return StyledPlanResult(problem = "Nessuna ricetta ${slot.meal} compatibile con ${get(id).title} e le esclusioni attuali. Il piano resta invariato.")
        for (attempt in 0..3) {
            val generated = Planner.generate(db, selectedPrefs, start, count, targets(start), existing,
                seed = start.toEpochDay() + attempt * 331L, dayTargetForDate = targets).map { day ->
                val old = existing.firstOrNull { it.date == day.date }
                day.copy(styleId = id, slots = day.slots.map { slot -> old?.slots?.firstOrNull { it.slot == slot.slot && it.eatenEntryIds.isNotEmpty() } ?: slot })
            }
            var valid = true
            for (plan in generated) {
                if (plan.slots.size != prefs.slots.size || plan.slots.any { slot -> db.recipe(slot.recipeId)?.let { !allowed(db, it, id) } != false }) { valid = false; break }
                val target = targets(LocalDate.parse(plan.date))
                val meals = Planner.solveDay(db, plan, target, selectedPrefs)
                val actual = meals.fold(Macro.ZERO) { total, meal -> total + meal.solved.total }
                // Numeric approximation is reported to the user; targets themselves never change.
                val errors = listOf(abs(actual.kcal-target.kcal)/max(target.kcal,1.0), abs(actual.protein-target.protein)/max(target.protein,5.0),
                    abs(actual.carbs-target.carbs)/max(target.carbs,5.0), abs(actual.fat-target.fat)/max(target.fat,3.0))
                if (errors[0] > .10 || errors.drop(1).any { it > .15 }) { valid = false; break }
                if (targetProblem(id, actual, maintenance(LocalDate.parse(plan.date))) != null) { valid = false; break }
                if (id == "dash" && (meals.any { m -> m.solved.items.any { "sodium" !in it.food.micro } } || meals.sumOf { it.solved.micro("sodium") } > 2300)) { valid = false; break }
            }
            if (valid) return StyledPlanResult(generated)
        }
        return StyledPlanResult(problem = "Con il catalogo, i pasti già mangiati e le esclusioni attuali non riesco a rispettare insieme questo stile e i target (tolleranza di calcolo: kcal ±10%, macro ±15%). Nessuna modifica. Prova un altro stile o un periodo futuro.")
    }
}
