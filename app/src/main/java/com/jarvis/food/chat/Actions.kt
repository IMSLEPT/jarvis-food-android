package com.jarvis.food.chat

import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.FastingPlans
import com.jarvis.food.data.Food
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.QStep
import com.jarvis.food.ui.Screen
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

data class ActionCall(val name: String, val args: JSONObject)

/** Esito di un'azione eseguita nell'app. [retry]: l'esito va comunicato al modello perché possa correggersi. */
data class ActionResult(val ok: Boolean, val text: String, val retry: Boolean = false)

data class Parsed(val text: String, val actions: List<ActionCall>, val quick: List<String>)

/** Protocollo testuale con cui Jarvis agisce nell'app: blocchi <action>{...}</action> e risposte rapide <quick>...</quick>. */
object Actions {
    private val actionRe = Regex("<action>(.*?)</action>", RegexOption.DOT_MATCHES_ALL)
    private val quickRe = Regex("<quick>(.*?)</quick>", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): Parsed {
        val acts = actionRe.findAll(raw).mapNotNull { m ->
            val body = m.groupValues[1]
            val s = body.indexOf('{'); val e = body.lastIndexOf('}')
            if (s < 0 || e <= s) return@mapNotNull null
            runCatching {
                val o = JSONObject(body.substring(s, e + 1))
                ActionCall(o.getString("name").trim(), o.optJSONObject("args") ?: JSONObject())
            }.getOrNull()
        }.toList()
        val quick = quickRe.findAll(raw).map { it.groupValues[1].trim() }.filter { it.isNotBlank() && it.length <= 60 }.distinct().take(4).toList()
        val text = visible(raw)
        return Parsed(text, acts, quick)
    }

    /** Testo da mostrare: senza blocchi azione/risposte rapide, anche se ancora incompleti durante lo streaming. */
    fun visible(raw: String): String {
        var t = actionRe.replace(raw, "")
        t = quickRe.replace(t, "")
        val cut = listOf(t.indexOf("<action"), t.indexOf("<quick")).filter { it >= 0 }.minOrNull()
        if (cut != null) t = t.substring(0, cut)
        return t.replace("```json", "").replace("```", "").trim()
    }

    // ------------------------------------------------------------------ Esecuzione

    private fun JSONObject.str(k: String): String? = optString(k, "").trim().takeIf { it.isNotEmpty() && it != "null" }
    private fun JSONObject.num(k: String): Double? {
        if (!has(k) || isNull(k)) return null
        val v = opt(k)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
            else -> null
        }
    }

    private fun mealIndex(s: String?): Int = when (s?.lowercase()?.trim()) {
        "colazione", "breakfast" -> 0
        "pranzo", "lunch" -> 1
        "cena", "dinner" -> 2
        "spuntino", "merenda", "snack" -> 3
        else -> when (LocalTime.now().hour) { in 5..10 -> 0; in 11..15 -> 1; in 18..23 -> 2; else -> 3 }
    }

    private fun dateOf(s: String?): LocalDate = when (s?.lowercase()?.trim()) {
        null, "", "oggi" -> LocalDate.now()
        "ieri" -> LocalDate.now().minusDays(1)
        else -> runCatching { LocalDate.parse(s) }.getOrDefault(LocalDate.now())
    }

    private fun findFood(name: String, st: AppState): Food? {
        st.customFoods.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        val db = Db.db
        db.search(name, 3).firstOrNull()?.let { return it.toFood() }
        db.match(name)?.let { return it.toFood() }
        // Prova con la prima parola significativa ("petto di pollo alla griglia" → "pollo").
        name.split(" ").filter { it.length > 3 }.forEach { w -> db.search(w, 1).firstOrNull()?.let { return it.toFood() } }
        return st.customFoods.firstOrNull { it.name.contains(name, ignoreCase = true) }
    }

    private fun recompute(extra: (AppState) -> AppState = { it }) {
        Store.update { st0 ->
            val st = extra(st0)
            if (st.goals.auto) st.copy(goals = Nutrition.autoGoals(st.profile, Store.currentWeight(st), st.goals)) else st
        }
    }

    private fun f0(d: Double) = d.roundToInt().toString()

    /** Esegue una azione. Va chiamata sul thread principale (può cambiare la schermata). */
    fun execute(a: ActionCall): ActionResult = runCatching { run(a) }.getOrElse { ActionResult(false, "Non sono riuscito a eseguire ${a.name}: ${it.message}") }

    private fun run(a: ActionCall): ActionResult {
        val args = a.args
        val st = Store.current
        return when (a.name) {
            "log_food" -> {
                val name = args.str("food") ?: return ActionResult(false, "Manca il nome dell'alimento.", true)
                val grams = args.num("grams")?.takeIf { it in 1.0..3000.0 }
                    ?: return ActionResult(false, "Quantità mancante o non valida per $name.", true)
                val food = findFood(name, st) ?: return ActionResult(false, "Non trovo \"$name\" nel database alimenti.", true)
                val date = dateOf(args.str("date")); val meal = mealIndex(args.str("meal"))
                Store.addEntry(date, meal, food, grams)
                val kcal = Nutrition.of(food, grams).kcal
                ActionResult(true, "Registrati ${f0(grams)} g di ${food.name} (${f0(kcal)} kcal) a ${com.jarvis.food.data.MEAL_NAMES[meal].lowercase()}")
            }
            "add_water" -> {
                val ml = args.num("ml")?.toInt()?.takeIf { it in 50..3000 } ?: return ActionResult(false, "Quantità d'acqua non valida.", true)
                val date = dateOf(args.str("date"))
                Store.setWater(date, (st.water[date.toString()] ?: 0) + ml)
                ActionResult(true, "Aggiunti $ml ml di acqua")
            }
            "log_weight" -> {
                val kg = args.num("kg")?.takeIf { it in 30.0..300.0 } ?: return ActionResult(false, "Peso non valido.", true)
                Store.addWeight(dateOf(args.str("date")), kg)
                ActionResult(true, "Peso registrato: ${"%.1f".format(Locale.ITALY, kg)} kg")
            }
            "log_activity" -> {
                val name = args.str("name") ?: "Attività"
                val min = args.num("minutes")?.toInt()?.takeIf { it in 1..600 } ?: return ActionResult(false, "Durata non valida.", true)
                val kcal = args.num("kcal")?.toInt() ?: Nutrition.activityKcal(5.0, Store.currentWeight(st), min)
                Store.addActivity(dateOf(args.str("date")), name, min, kcal)
                ActionResult(true, "Attività registrata: $name, $min min (~$kcal kcal)")
            }
            "set_goal" -> {
                val g = when (args.str("goal")?.lowercase()) {
                    "dimagrire", "perdere peso" -> -1
                    "mantenere", "mantenimento" -> 0
                    "aumentare", "massa" -> 1
                    else -> return ActionResult(false, "Obiettivo non riconosciuto (dimagrire, mantenere, aumentare).", true)
                }
                val rate = (args.num("kg_per_week") ?: st.profile.weeklyRate).coerceIn(0.25, 1.0)
                recompute { it.copy(profile = it.profile.copy(goal = g, weeklyRate = if (g == 0) it.profile.weeklyRate else rate)) }
                val g2 = Store.current.goals.kcal
                ActionResult(true, "Obiettivo: ${when (g) { -1 -> "dimagrire"; 1 -> "aumentare massa"; else -> "mantenere" }}" +
                        (if (g != 0) " (${"%.2f".format(Locale.ITALY, rate)} kg a settimana)" else "") + " · ora $g2 kcal")
            }
            "set_target_weight" -> {
                val kg = args.num("kg")?.takeIf { it in 30.0..300.0 } ?: return ActionResult(false, "Peso obiettivo non valido.", true)
                Store.update { it.copy(profile = it.profile.copy(targetWeight = kg)) }
                ActionResult(true, "Peso obiettivo: ${"%.1f".format(Locale.ITALY, kg)} kg")
            }
            "set_calories" -> {
                val kcal = args.num("kcal")?.toInt()?.takeIf { it in 1000..6000 } ?: return ActionResult(false, "Calorie non valide (1000-6000).", true)
                Store.update { it.copy(goals = it.goals.copy(auto = false, kcal = kcal)) }
                ActionResult(true, "Calorie impostate a mano: $kcal kcal al giorno (calcolo automatico spento)")
            }
            "use_auto_calc" -> {
                Store.update { it.copy(goals = Nutrition.autoGoals(it.profile, Store.currentWeight(it), it.goals.copy(auto = true))) }
                ActionResult(true, "Calcolo automatico riattivato: ${Store.current.goals.kcal} kcal")
            }
            "set_macros" -> {
                val preset = args.str("preset")?.lowercase()?.replace(" ", "")?.replace("_", "")
                val idx = when (preset) { "bilanciato" -> 0; "proteico", "piuproteine" -> 1; "lowcarb", "pochicarboidrati" -> 2; else -> null }
                if (idx != null && st.goals.auto) {
                    recompute { it.copy(profile = it.profile.copy(dietStyle = idx)) }
                    val g = Store.current.goals
                    ActionResult(true, "Macro ${Nutrition.PRESETS[idx].name}: ${g.carbsPct}% C · ${g.proteinPct}% P · ${g.fatPct}% G")
                } else {
                    val (c, p, f) = if (idx != null) Nutrition.PRESETS[idx].let { Triple(it.carbs, it.protein, it.fat) }
                    else Triple(args.num("carbs")?.toInt(), args.num("protein")?.toInt(), args.num("fat")?.toInt()).let { t ->
                        if (t.first == null || t.second == null || t.third == null) return ActionResult(false, "Servono preset oppure carbs, protein e fat in percentuale.", true)
                        Triple(t.first!!, t.second!!, t.third!!)
                    }
                    if (c + p + f !in 99..101 || minOf(c, p, f) < 5) return ActionResult(false, "Le percentuali devono sommare 100 (ognuna almeno 5).", true)
                    Store.update { it.copy(goals = it.goals.copy(auto = false, carbsPct = c, proteinPct = p, fatPct = f)) }
                    ActionResult(true, "Macro impostati: $c% C · $p% P · $f% G (calcolo automatico spento)")
                }
            }
            "update_profile" -> {
                if (!st.profile.detailed) {
                    Nav.go(Screen.Questionnaire(null))
                    return ActionResult(true, "Ho aperto il questionario: rispondi alle domande e il calcolo sarà preciso")
                }
                val ch = mutableListOf<String>()
                var p = st.profile
                args.str("job")?.lowercase()?.let { j ->
                    val v = when { j.contains("sed") -> 0; j.contains("pied") || j.contains("mov") -> 1; j.contains("fis") -> 2; else -> null }
                    if (v != null) { p = p.copy(job = v); ch += "lavoro: ${Nutrition.JOB_LABELS[v].substringBefore(" (").lowercase()}" }
                }
                args.num("steps_per_day")?.let { n ->
                    val v = when { n < 4000 -> 0; n < 7000 -> 1; n < 10000 -> 2; n < 14000 -> 3; else -> 4 }
                    p = p.copy(stepsLevel = v); ch += "passi: ${Nutrition.STEPS_LABELS[v]}"
                }
                args.num("workouts_per_week")?.toInt()?.takeIf { it in 0..14 }?.let { p = p.copy(workouts = it); ch += "allenamenti: $it a settimana" }
                args.num("workout_minutes")?.toInt()?.takeIf { it in 10..240 }?.let { p = p.copy(workoutMin = it); ch += "durata: $it min" }
                args.str("intensity")?.lowercase()?.let { s ->
                    val v = when { s.startsWith("legg") -> 0; s.startsWith("mod") -> 1; s.startsWith("int") -> 2; else -> null }
                    if (v != null) { p = p.copy(workoutIntensity = v); ch += "intensità: ${s}" }
                }
                args.str("training_type")?.lowercase()?.let { s ->
                    val v = when { s.contains("pes") || s.contains("cali") -> 1; s.contains("card") || s.contains("cors") -> 2; s.contains("squad") -> 3; s.contains("mis") -> 0; else -> null }
                    if (v != null) { p = p.copy(trainingType = v); ch += "tipo: ${Nutrition.TRAINING_LABELS[v].lowercase()}" }
                }
                args.num("body_fat")?.takeIf { it in 5.0..55.0 }?.let { p = p.copy(bodyFatPct = it); ch += "massa grassa: ${"%.1f".format(Locale.ITALY, it)}%" }
                args.num("height_cm")?.takeIf { it in 120.0..230.0 }?.let { p = p.copy(heightCm = it); ch += "altezza: ${f0(it)} cm" }
                if (ch.isEmpty()) return ActionResult(false, "Nessun campo del profilo riconosciuto.", true)
                recompute { it.copy(profile = p) }
                ActionResult(true, "Profilo aggiornato (${ch.joinToString(", ")}) · ora ${Store.current.goals.kcal} kcal")
            }
            "start_fast" -> {
                val id = args.str("plan") ?: "16:8"
                val plan = FastingPlans.plans.firstOrNull { it.id == id || it.fastHours.toString() == id.substringBefore(":") }
                    ?: return ActionResult(false, "Piano di digiuno non riconosciuto ($id).", true)
                Store.startFast(plan.id, plan.fastHours)
                ActionResult(true, "Digiuno ${plan.title} avviato")
            }
            "stop_fast" -> {
                if (st.fasting.activeStart == null) return ActionResult(false, "Non c'è nessun digiuno in corso.")
                Store.stopFast(true)
                ActionResult(true, "Digiuno terminato e salvato")
            }
            "generate_plan" -> {
                val days = args.num("days")?.toInt()?.coerceIn(1, 14) ?: 7
                Store.generatePlan(LocalDate.now(), days)
                ActionResult(true, "Piano alimentare creato per $days giorni")
            }
            "add_shopping" -> {
                val item = args.str("item") ?: return ActionResult(false, "Manca l'articolo.", true)
                Store.addShoppingManual(item)
                ActionResult(true, "Aggiunto alla spesa: $item")
            }
            "open_screen" -> {
                val target = args.str("screen")?.lowercase() ?: return ActionResult(false, "Schermata mancante.", true)
                Nav.clear()
                when (target) {
                    "oggi", "home" -> Nav.tab = 0
                    "piano" -> Nav.tab = 1
                    "cucina", "dispensa", "spesa" -> Nav.tab = 2
                    "progressi" -> Nav.tab = 3
                    "profilo" -> Nav.tab = 4
                    "obiettivi" -> Nav.go(Screen.Goals)
                    "smartwatch", "watch" -> Nav.go(Screen.Watch)
                    "peso" -> Nav.go(Screen.Weight)
                    "digiuno" -> Nav.go(Screen.Fasting)
                    "questionario" -> Nav.go(Screen.Questionnaire(null))
                    "calcolo" -> Nav.go(Screen.EnergyInfo)
                    "micronutrienti", "vitamine" -> Nav.go(Screen.Micronutrients)
                    else -> return ActionResult(false, "Schermata sconosciuta: $target.")
                }
                ActionResult(true, "Aperto: $target")
            }
            else -> ActionResult(false, "Azione sconosciuta: ${a.name}", true)
        }
    }

    // ------------------------------------------------------------------ Contesto per il modello

    fun systemPrompt(s: AppState): String {
        val today = LocalDate.now()
        val w = Store.currentWeight(s)
        val p = s.profile
        val e = Store.energyFor(s, today)
        val entries = Store.entriesFor(s, today)
        val tot = Nutrition.sum(entries)
        val water = s.water[today.toString()] ?: 0
        val fast = s.fasting.activeStart?.let { "in corso da ${((System.currentTimeMillis() - it) / 3_600_000)} h (${s.fasting.planId})" } ?: "nessuno"
        val diary = if (entries.isEmpty()) "nulla ancora" else entries.takeLast(12).joinToString("; ") {
            "${com.jarvis.food.data.MEAL_NAMES[it.meal].lowercase()}: ${it.food.name} ${f0(it.grams)} g (${f0(Nutrition.of(it.food, it.grams).kcal)} kcal)"
        }
        val goalTxt = when (p.goal) { -1 -> "dimagrire"; 1 -> "aumentare massa"; else -> "mantenere" }
        val prefs = listOf("bilanciato", "più proteine", "pochi carboidrati")[p.dietStyle.coerceIn(0, 2)]
        val quest = if (p.detailed) "lavoro ${Nutrition.JOB_LABELS[p.job.coerceIn(0, 2)].substringBefore(" (").lowercase()}, passi ${Nutrition.STEPS_LABELS[p.stepsLevel.coerceIn(0, 4)]}, " +
                "allenamenti ${p.workouts}/sett da ${p.workoutMin} min" else "questionario non ancora compilato (calcolo meno preciso)"
        val dow = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
        return """Sei Jarvis, l'assistente personale dentro l'app Jarvis Food (calorie, macro, piano alimentare, digiuno, smartwatch). Parli italiano, diretto e cordiale, risposte brevi (massimo 4 frasi) salvo richiesta. Usi solo i dati qui sotto, non inventi numeri. Non fai diagnosi mediche.

DATI ($dow ${today}, ${LocalTime.now().hour}:${"%02d".format(LocalTime.now().minute)})
Utente: ${p.name}, ${if (p.male) "uomo" else "donna"}, ${Nutrition.age(p)} anni, ${f0(p.heightCm)} cm, peso ${"%.1f".format(Locale.ITALY, w)} kg, obiettivo peso ${"%.1f".format(Locale.ITALY, p.targetWeight)} kg, scopo: $goalTxt${if (p.goal != 0) " ${"%.2f".format(Locale.ITALY, p.weeklyRate)} kg/sett" else ""}. Preferenza macro: $prefs. $quest.
Obiettivo di oggi: ${f0(e.targetKcal)} kcal (base ${f0(e.baseKcal)}), macro P ${f0(e.protein)} g, C ${f0(e.carbs)} g, G ${f0(e.fat)} g; ripartizione ${s.goals.carbsPct}% C ${s.goals.proteinPct}% P ${s.goals.fatPct}% G; calcolo ${if (s.goals.auto) "automatico" else "manuale"}. Basale ${f0(e.bmr)} kcal.
Oggi: mangiate ${f0(tot.kcal)} kcal (P ${f0(tot.protein)}, C ${f0(tot.carbs)}, G ${f0(tot.fat)}), acqua $water ml su ${s.goals.waterMl}, passi ${Store.stepsFor(s, today)}, attività ${Store.burnedFor(s, today)} kcal, digiuno: $fast.
Diario di oggi: $diary.

AZIONI
Per agire nell'app scrivi una breve frase e poi un blocco <action>{"name":"...","args":{...}}</action> (anche più blocchi). Azioni:
- log_food {"food":"pollo","grams":150,"meal":"pranzo","date":"oggi"} registra un alimento (meal: colazione, pranzo, cena, spuntino)
- add_water {"ml":250}
- log_weight {"kg":79.4}
- log_activity {"name":"Corsa","minutes":30,"kcal":300}
- set_goal {"goal":"dimagrire|mantenere|aumentare","kg_per_week":0.5}
- set_target_weight {"kg":78}
- set_calories {"kcal":2400} (calorie a mano)
- use_auto_calc {} (torna al calcolo automatico)
- set_macros {"preset":"bilanciato|proteico|low_carb"} oppure {"carbs":40,"protein":30,"fat":30}
- update_profile {"job":"seduto|in_piedi|fisico","steps_per_day":8000,"workouts_per_week":3,"workout_minutes":45,"intensity":"leggero|moderato|intenso","training_type":"misto|pesi|cardio|squadra","body_fat":18,"height_cm":178}
- start_fast {"plan":"16:8"} / stop_fast {}
- generate_plan {"days":7}
- add_shopping {"item":"uova"}
- open_screen {"screen":"oggi|piano|cucina|progressi|profilo|obiettivi|smartwatch|peso|digiuno|questionario|calcolo|vitamine"}

REGOLE
- Prima di agire controlla di avere tutti i dati. Se manca qualcosa (quantità, pasto, valore) NON agire: fai UNA sola domanda breve e proponi 2-4 risposte rapide, ognuna in un tag <quick>150 g</quick>.
- Per cambiare calorie, obiettivo o profilo, se l'utente non è stato esplicito, spiega l'effetto e chiedi conferma con <quick>Sì, applica</quick><quick>No</quick>.
- Dopo aver agito conferma in una frase e, se utile, proponi il passo successivo con <quick>.
- Consigli su calorie e macro: usa i dati sopra e spiega in breve il ragionamento (proteine in g/kg, deficit sostenibile).
- Non scrivere mai JSON fuori da un blocco <action>."""
    }
}
