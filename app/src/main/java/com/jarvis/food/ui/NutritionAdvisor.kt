package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
private fun rememberReview(s: AppState, days: Int): NutritionReview? {
    val today = LocalDate.now()
    val review by produceState<NutritionReview?>(null, s.diary, s.profile, s.goals, s.energy, s.trainingNutrition, s.weights, s.nutritionCompleteDays, days, today) {
        val state = s
        value = withContext(Dispatchers.Default) { NutritionScience.analyze(state, today, days) { Store.energyFor(state, it).targetKcal } }
    }
    return review
}

@Composable
fun NutritionAdvisor(s: AppState, full: Boolean = false, days: Int = 7) {
    val review = rememberReview(s, days)
    if (review == null) {
        if (full) Text("Analizzo il diario…", style = T.Small)
        return
    }
    val suggestions = NutritionScience.actionable(review)
    // The Home has no empty panel, loading block, vitamins list or generic food suggestions.
    if (!full && suggestions.isEmpty()) return
    var selected by remember { mutableStateOf<NutritionNotice?>(null) }
    var showData by remember { mutableStateOf(false) }
    var showAll by remember(days) { mutableStateOf(false) }
    if (!full) ASection("Un consiglio per te", action = "Dettagli") { Nav.go(Screen.Micronutrients) }
    if (full) {
        Text("${review.usableDays.size}/${review.window} giorni utilizzabili", Modifier.testTag("nutrition-coverage"), style = T.Small)
        GapH(10.dp)
        if (suggestions.isEmpty()) JCard {
            Text("Nessun suggerimento da mostrare", style = T.Title2)
            Text(if (review.usableDays.size < 3) "Registra più giornate complete per valutare le abitudini."
                else "Nessun consiglio specifico dai dati disponibili: non è una valutazione clinica.", style = T.Small)
        }
    }
    suggestions.take(if (full && showAll) suggestions.size else if (full) 2 else 1).forEach { notice ->
        val alternatives = noticeAlternatives(s, notice)
        val current = notice.entryId?.let { id -> s.diary.firstOrNull { it.id == id } }
        GapH(10.dp)
        JCard(Modifier.testTag("nutrition-advisor-${notice.id}"), padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current != null) FoodIllustration(current.food.name, current.food.group, 44.dp)
                else TonalIcon(if (notice.attention) Ic.Spark else Ic.Bowl, C.AccentDeep, C.AccentSoft, box = 40.dp)
                GapW(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(notice.title, style = T.Label)
                    Text(notice.explanation, style = T.Small)
                }
            }
            GapH(8.dp)
            Text(shortAdvice(notice, review), style = T.Small.copy(color = C.Navy))
            if (alternatives.isNotEmpty()) {
                GapH(10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    alternatives.take(2).forEach { (food, grams) ->
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(C.Bg)
                            .clickable { Nav.go(Screen.FoodDetail(food.toFood(), LocalDate.now(), defaultMeal(), grams)) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            FoodIllustration(food.name, food.group, 40.dp); GapW(6.dp)
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = T.Small.copy(color = C.Navy))
                                Text("${grams.r0()} ${if (food.liquid) "ml" else "g"}", style = T.Label)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { selected = notice }, modifier = Modifier.testTag("notice-${notice.id}")) { Text("Scopri perché", color = C.Navy) }
                if (notice.entryId != null) TextButton(onClick = { Nav.go(Screen.ReplaceFood(notice.entryId)) }) { Text("Sostituisci", color = C.AccentDeep) }
            }
        }
    }
    if (full) {
        if (suggestions.size > 2) TextButton(onClick = { showAll = !showAll }) {
            Text(if (showAll) "Mostra solo l’essenziale" else "Altri consigli (${suggestions.size - 2})", color = C.Navy)
        }
        GapH(10.dp)
        TextButton(onClick = { showData = !showData }, modifier = Modifier.testTag("nutrition-data-toggle")) {
            Text(if (showData) "Nascondi dati e fonti" else "Dati e fonti", color = C.Navy)
        }
        if (showData) {
            Text("Completezza stimata da almeno 3 pasti e 70% dell’energia attesa nei giorni passati, oppure confermata da te. I dati mancanti non diventano zero. I consigli sulle vitamine richiedono almeno il 70% dei giorni della finestra con dati completi: criteri dell’app, non soglie cliniche.", style = T.Small)
            FullReviewDetails(review, s)
            Text("Base ${NutritionEvidence.VERSION} · verifica ${NutritionEvidence.REVIEWED} · fonti consultabili online, analisi disponibile offline.", style = T.Small)
        }
        CompleteDayControl(s)
    }
    selected?.let { notice ->
        val alternatives = noticeAlternatives(s, notice)
        val current = notice.entryId?.let { id -> s.diary.firstOrNull { it.id == id } }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(notice.title, style = T.Title2) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(notice.explanation, style = T.Body)
                current?.let { Text("${it.food.name}: ${it.grams.r0()} g nella registrazione selezionata.", style = T.Small) }
                Text(notice.meaning, style = T.Body)
                Text(notice.reference, style = T.Small)
                Text(notice.action, style = T.Small)
                alternatives.forEach { (food, grams) ->
                    Text("${food.name} · ${grams.r0()} ${if (food.liquid) "ml" else "g"}: ${(food.kcal * grams / 100).r0()} kcal · P ${(food.protein * grams / 100).r0()} · C ${(food.carbs * grams / 100).r0()} · G ${(food.fat * grams / 100).r0()} g", style = T.Small)
                    Text("Porzione indicativa, da adattare al pasto; non è un massimo di sicurezza.", style = T.Small)
                }
                if (notice.id == "free-sugar") BreakfastAlternatives(s)
                EvidenceLink(notice.sourceId)
                EvidenceLink("portions")
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Chiudi") } }, containerColor = C.Surface)
    }
}

private fun shortAdvice(n: NutritionNotice, r: NutritionReview): String = when (n.id) {
    "free-sugar" -> "L’eccesso abituale favorisce carie e aumento di peso. Obiettivo: meno di ${NutritionScience.fmt((r.nutrient("kcal").average ?: 0.0) * .10 / 4)} g/die di zuccheri liberi. Prova yogurt bianco o avena."
    "sodium" -> "Troppo sale contribuisce all’aumento della pressione. Obiettivo: meno di 5 g di sale totali al giorno. Prova alimenti meno salati."
    "saturated" -> "Sostituisci parte dei saturi con grassi insaturi. Obiettivo: non oltre ${NutritionScience.fmt((r.nutrient("kcal").average ?: 0.0) * .10 / 9)} g/die di saturi."
    "fiber" -> "Obiettivo: almeno 25 g/die di fibre. Prova legumi e avena, aumentando gradualmente."
    else -> "Prova una fonte diversa nel piano. È un suggerimento alimentare, non una diagnosi di carenza."
}

private fun noticeAlternatives(s: AppState, n: NutritionNotice): List<Pair<DbFood, Double>> {
    val ids = when (n.id) {
        "free-sugar" -> listOf("greco0" to 125.0, "avena" to 30.0)
        "saturated" -> listOf("olio" to 10.0, "ceci" to 150.0)
        "sodium" -> listOf("pollo" to 100.0, "greco0" to 125.0)
        "fiber" -> listOf("ceci" to 150.0, "avena" to 30.0)
        "intake-vitC" -> listOf("arancia" to 150.0)
        "intake-vitB12" -> listOf("uova" to 50.0, "salmone" to 150.0)
        "intake-vitD" -> listOf("trota" to 150.0, "salmone" to 150.0)
        else -> emptyList()
    }
    return ids.mapNotNull { (id, g) -> Db.db.food(id)?.takeIf { f -> f.tags.none { it in s.dietPrefs.exclude } }?.let { it to g } }
}

@Composable
private fun FoodSources(review: NutritionReview, s: AppState) {
    val metric = listOf("vitD", "vitC", "vitB12").map { review.nutrient(it) }.filter { it.percent != null && it.percent!! < 100 }.minByOrNull { it.percent!! }
    val key = metric?.key ?: "vitD"
    val suggestions = remember(key, metric?.average, s.dietPrefs.exclude) {
        Db.db.foods.filter { f -> (f.micro[key] ?: 0.0) > 0 && f.tags.none { it in s.dietPrefs.exclude } &&
            f.group !in setOf("spezie", "erbe", "integratori", "alcol", "condimenti", "frattaglie") && !f.name.contains("fegato", true) }
            .sortedByDescending { (it.micro[key] ?: 0.0) / max(it.kcal, 20.0) }.distinctBy { FoodArtCatalog.resolve(it.name).cell ?: it.id }.take(6)
    }
    if (suggestions.isEmpty()) return
    GapH(12.dp)
    Text(if (metric != null && review.usableDays.size >= 3) "Fonti di ${metric.name.lowercase()} da considerare" else "Fonti alimentari da esplorare", style = T.Title2)
    Text("Illustrazioni indicative. Una porzione non garantisce di correggere una carenza; scegli in base al pasto e alle tue preferenze.", style = T.Small)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        suggestions.forEach { food ->
            val portion = NutritionScience.standardPortion(food.toFood()) ?: MicroAnalysis.typicalPortion(food)
            val amount = (food.micro[key] ?: 0.0) * portion / 100
            Column(Modifier.width(108.dp).clip(RoundedCornerShape(18.dp)).clickable { Nav.go(Screen.FoodDetail(food.toFood(), LocalDate.now(), defaultMeal(), portion)) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FoodIllustration(food.name, food.group, 76.dp)
                GapH(6.dp)
                Text(food.name, style = T.Label)
                Text("${portion.r0()} ${if (food.liquid) "ml" else "g"} · porzione indicativa", style = T.Small)
                Text("${NutritionScience.fmt(amount)} ${review.nutrient(key).unit} di ${review.nutrient(key).name.lowercase()}", style = T.Small)
            }
        }
    }
    EvidenceLink(key)
}

@Composable
private fun BreakfastAlternatives(s: AppState) {
    val today = LocalDate.now()
    var selected by remember { mutableStateOf<SolvedRecipe?>(null) }
    var forTomorrow by remember { mutableStateOf(false) }
    val date = if (forTomorrow) today.plusDays(1) else today
    val current = Store.plannedMeals(s, date).firstOrNull { it.diaryMeal == 0 }
    val alternatives by produceState<List<SolvedRecipe>>(emptyList(), date, s.plan, s.dietPrefs, s.goals, s.energy, s.trainingNutrition, s.profile, s.weights) {
        val state = s
        value = withContext(Dispatchers.Default) {
            val meal = Store.plannedMeals(state, date).firstOrNull { it.diaryMeal == 0 }
            if (meal == null) emptyList() else Planner.alternatives(Db.db, "colazione", meal.solved.target, state.dietPrefs)
                .filter { it.template.id != meal.solved.template.id && it.items.all { i -> "fiber" in i.food.micro } }
                .sortedWith(compareBy<SolvedRecipe> { c -> c.toFood().freeSugar?.times(c.grams / 100) ?: Double.MAX_VALUE }
                    .thenByDescending { it.micro("fiber") + it.total.protein * 0.2 }).take(2)
        }
    }
    GapH(12.dp)
    Text("Cosa puoi fare", style = T.Title2)
    Text("Alternative calcolate sul tuo Piano. Il contenuto di zuccheri aggiunti non viene dedotto dai totali.", style = T.Small)
    TextButton(onClick = { forTomorrow = !forTomorrow }) { Text(if (forTomorrow) "Colazione di domani · cambia" else "Colazione di oggi · cambia", color = C.Navy) }
    if (current == null) {
        Text("Crea prima il piano per questa data per confrontare porzioni e nutrienti.", style = T.Small)
        TextButton(onClick = { Nav.planDate = date; Nav.tab = 1; Nav.clear() }) { Text("Apri Piano", color = C.Navy) }
    }
    alternatives.forEach { candidate ->
        GapH(10.dp)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Bg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodIllustration(candidate.template.name, size = 62.dp)
            GapW(10.dp)
            Column(Modifier.weight(1f)) {
                Text(candidate.template.name, style = T.Label)
                Text("${candidate.total.kcal.r0()} kcal · P ${candidate.total.protein.r0()} · C ${candidate.total.carbs.r0()} · G ${candidate.total.fat.r0()} g", style = T.Small)
                Text("Fibre ${candidate.micro("fiber").r1()} g · prima ${current?.solved?.takeIf { c -> c.items.all { "fiber" in it.food.micro } }?.micro("fiber")?.r1() ?: "non disponibile"} g", style = T.Small)
                val after = candidate.toFood()
                val before = current?.solved?.toFood()
                val afterFree = after.freeSugar?.times(candidate.grams / 100)
                val beforeFree = before?.freeSugar?.times((current?.solved?.grams ?: 0.0) / 100)
                Text("Zuccheri liberi: ${NutritionScience.fmt(afterFree)} g · prima ${NutritionScience.fmt(beforeFree)} g", style = T.Small)
                Text("Aggiunti: ${NutritionScience.fmt(after.addedSugar?.times(candidate.grams / 100))} g · prima ${NutritionScience.fmt(before?.addedSugar?.times((current?.solved?.grams ?: 0.0) / 100))} g", style = T.Small)
                if (afterFree != null && beforeFree != null && afterFree < beforeFree) Text("Meno zuccheri liberi, in base agli ingredienti noti", style = T.Small.copy(color = A.GreenInk))
                Text("Prima: ${current?.solved?.total?.kcal?.r0() ?: "—"} kcal · P ${current?.solved?.total?.protein?.r0() ?: "—"} · C ${current?.solved?.total?.carbs?.r0() ?: "—"} · G ${current?.solved?.total?.fat?.r0() ?: "—"} g", style = T.Small)
                TextButton(onClick = { selected = candidate }) { Text("Confronta e sostituisci", color = C.Navy) }
            }
        }
    }
    val replacement = selected
    if (replacement != null && current != null) {
        val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == current.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Sostituire nel Piano?", style = T.Title2) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("${dayLabel(date)} · ${current.slotName}: ${replacement.template.name}", style = T.Body)
                replacement.items.forEach { Text("${it.food.name}: ${formatAmount(it.food, it.grams)}", style = T.Small) }
                if (eaten) Text("Questo pasto è già stato registrato. Scegli la colazione di domani per modificare solo il piano futuro.", style = T.Small.copy(color = C.AccentDeep))
                Text("Il diario non verrà modificato da questa operazione.", style = T.Small)
            }
        }, confirmButton = { TextButton(enabled = !eaten, onClick = {
            val fresh = Store.dayPlan(Store.current, date)?.slots?.firstOrNull { it.slot == current.slotIndex }
            if (fresh != null && fresh.eatenEntryIds.isEmpty()) Store.setSlotRecipe(date, current.slotIndex, replacement.template.id)
            selected = null
        }) { Text("Conferma sostituzione") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Annulla") } }, containerColor = C.Surface)
    }
}

@Composable
private fun CompleteDayControl(s: AppState) {
    val date = LocalDate.now().toString()
    var confirm by remember { mutableStateOf(false) }
    if (s.diary.none { it.date == date }) return
    TextButton(onClick = { confirm = true }, modifier = Modifier.testTag("nutrition-complete-day")) {
        Text(if (date in s.nutritionCompleteDays) "Giornata confermata completa · modifica" else "Hai finito di registrare oggi?", color = C.Navy)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Completezza del diario", style = T.Title2) }, text = {
        Text("Conferma solo se hai registrato tutti i pasti, le bevande e i condimenti di oggi. Una giornata con pochi dati non descrive automaticamente una carenza.", style = T.Body)
    }, confirmButton = { TextButton(onClick = {
        Store.update { it.copy(nutritionCompleteDays = (it.nutritionCompleteDays + date).distinct()) }; confirm = false
    }) { Text("Confermo giornata completa") } }, dismissButton = { TextButton(onClick = {
        Store.update { it.copy(nutritionCompleteDays = it.nutritionCompleteDays - date) }; confirm = false
    }) { Text("Non è completa") } }, containerColor = C.Surface)
}

@Composable
private fun FullReviewDetails(review: NutritionReview, s: AppState) {
    GapH(12.dp)
    JCard {
        Text("Nutrienti registrati", style = T.Title2)
        Text("Le medie si riferiscono ai ${review.usableDays.size} giorni utilizzabili. Le voci parziali sono somme minime note: i valori mancanti non diventano zero.", style = T.Small)
        review.nutrients.forEach { metric ->
            GapH(10.dp)
            Text(metric.name, style = T.Label)
            Text("${NutritionScience.fmt(metric.average)} ${metric.unit}/die · dati ${metric.knownEntries}/${metric.totalEntries}" +
                if (metric.complete) "" else " · parziale/non disponibile", style = T.Small)
            if (metric.reference != null) Text("${if (metric.key == "sodium") "Obiettivo OMS: meno di" else "Riferimento"} ${NutritionScience.fmt(metric.reference)} ${metric.unit}/die", style = T.Small)
        }
        GapH(12.dp)
        Text("Confronto giornaliero con il tuo obiettivo", style = T.Title2)
        review.usableDays.forEach { value ->
            val date = LocalDate.parse(value)
            val rows = s.diary.filter { it.date == value }
            val energy = rows.sumOf { (NutritionScience.per100(it.food, "kcal") ?: 0.0) * it.grams / 100 }
            val goal = Store.energyFor(s, date)
            Text("${dayLabel(date)} · ${energy.r0()} / ${goal.targetKcal.r0()} kcal impostate", style = T.Small)
        }
        Text("Il confronto con l’obiettivo personale non è un limite di sicurezza. Non si deducono disturbi o carenze dallo scostamento.", style = T.Small)
        GapH(12.dp)
        Text("Varietà registrata: ${review.uniqueFoods} alimenti distinti, ${review.groups} gruppi riconoscibili. Nessun punteggio clinico viene dedotto da questi conteggi.", style = T.Small)
    }
    GapH(12.dp)
    JCard {
        Text("Quantità e frequenze osservate", style = T.Title2)
        Text("Le porzioni standard SINU sono unità di confronto, non limiti massimi né prescrizioni. Pesi a crudo/al netto degli scarti salvo diversa indicazione del prodotto; per il cotto serve la conversione.", style = T.Small)
        review.habits.forEach { habit ->
            GapH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodIllustration(habit.food.name, habit.food.group, 48.dp); GapW(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(habit.food.name, style = T.Label)
                    Text("${habit.days}/${review.window} giorni · ${habit.occurrences} registrazioni · ${habit.gramsPerOccurrence.r0()} ${if (habit.food.liquid) "ml" else "g"} medi per registrazione", style = T.Small)
                    habit.standardGrams?.let { Text("Porzione standard indicativa: ${it.r0()} ${if (habit.food.liquid) "ml" else "g"}", style = T.Small) }
                }
            }
        }
        EvidenceLink("portions"); EvidenceLink("variety")
        Text("Le frequenze dei gruppi alimentari dipendono dal modello dietetico e dall’energia: consulta gli esempi CREA, che non costituiscono limiti settimanali di sicurezza.", style = T.Small)
    }
    GapH(12.dp)
    JCard {
        Text("Fonti scientifiche consultabili", style = T.Title2)
        NutritionEvidence.sources.forEach { EvidenceLink(it.id) }
        Text("Valori nutrizionali: database USDA FoodData Central (identificativo FDC nelle schede), etichette/Open Food Facts e alimenti personali. L’affidabilità dipende anche dalla corrispondenza del prodotto e dalla completezza delle registrazioni.", style = T.Small)
    }
}

@Composable
private fun EvidenceLink(id: String) {
    val source = NutritionEvidence.source(id)
    val uri = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    TextButton(onClick = { failed = runCatching { uri.openUri(source.url) }.isFailure }) {
        Text("${source.title} · ${source.year} ↗", style = T.Small.copy(color = C.Navy))
    }
    if (failed) Text("Collegamento: ${source.url}", style = T.Small)
}

@Composable
fun ScientificNutritionScreen() {
    val s by Store.state.collectAsState()
    var days by remember { mutableIntStateOf(7) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Vitamine, nutrienti e abitudini", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
            ASegment(listOf("7 giorni", "14 giorni", "30 giorni"), listOf(7,14,30).indexOf(days)) { days = listOf(7,14,30)[it] }
            GapH(12.dp)
            NutritionAdvisor(s, full = true, days = days)
            GapH(24.dp)
        }
    }
}
