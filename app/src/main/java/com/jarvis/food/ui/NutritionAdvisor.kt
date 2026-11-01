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
    if (!full) ASection("Equilibrio dei nutrienti", action = "Analisi completa") { Nav.go(Screen.Micronutrients) }
    if (review == null) {
        JCard { Text("Analizzo il diario sul telefono…", style = T.Small) }
        return
    }
    var notice by remember { mutableStateOf<NutritionNotice?>(null) }
    JCard(Modifier.testTag("nutrition-advisor"), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TonalIcon(Ic.Chart, C.Navy, C.NavySoft, box = 42.dp)
            GapW(12.dp)
            Column(Modifier.weight(1f)) {
                Text("Analisi degli ultimi ${review.window} giorni", style = T.Title2)
                Text("${review.usableDays.size}/${review.window} giorni sufficientemente registrati · ${review.recordedDays} con dati", modifier = Modifier.testTag("nutrition-coverage"), style = T.Small)
            }
        }
        GapH(12.dp)
        if (!review.applicable) Text("I confronti per adulti sani non vengono applicati a minori, gravidanza/allattamento o condizioni cliniche segnalate nel profilo. Puoi consultare i dati registrati con un professionista.", style = T.Small)
        if (review.usableDays.size < 3) Text("Servono almeno 3 giorni sufficientemente registrati per evidenziare tendenze. Questo è un criterio operativo dell’app, non una soglia clinica.", style = T.Small)
        Text("Completezza indicativa: giornate passate con almeno 3 pasti e il 70% dell’obiettivo energetico, oppure conferma manuale. Anche così possono mancare alimenti e condimenti.", style = T.Small)
        GapH(10.dp)
        listOf("vitD", "vitC", "vitB12").forEach { key ->
            val metric = review.nutrient(key)
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                FoodIllustration(when(key) { "vitD" -> "trota"; "vitC" -> "arancia"; else -> "uova" }, size = 42.dp)
                GapW(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(metric.name, style = T.Label)
                    Text(if (metric.average == null) "Dato non disponibile" else "${NutritionScience.fmt(metric.average)} ${metric.unit}/die · ${metric.knownEntries}/${metric.totalEntries} voci note", style = T.Small)
                    val percent = metric.percent
                    if (percent != null && review.applicable) {
                        GapH(4.dp)
                        ABar((percent / 100).toFloat().coerceIn(0f, 1f), androidx.compose.ui.graphics.SolidColor(if (percent >= 100) A.GreenInk else C.Accent), C.AccentSoft, 6.dp)
                    }
                }
                GapW(8.dp)
                Text(metric.percent?.let { "${it.r0()}%" } ?: "—", style = T.Label.copy(color = C.Navy))
            }
        }
        Text("Percentuali sui riferimenti EFSA solo con dati completi. Apporto alimentare stimato: non misura lo stato vitaminico e non diagnostica carenze. Per la D conta anche la sintesi cutanea.", style = T.Small)
        FoodSources(review, s)
    }
    GapH(12.dp)
    if (review.notices.isNotEmpty()) {
        JCard(padding = PaddingValues(16.dp)) {
            Text(if (review.notices.any { it.attention }) "Attenzione alle tue abitudini" else "Le tue abitudini alimentari", style = T.Title2)
            review.notices.take(if (full) 12 else 2).forEach { item ->
                GapH(10.dp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val food = item.entryId?.let { id -> s.diary.firstOrNull { it.id == id }?.food }
                    if (food != null) { FoodIllustration(food.name, food.group, 48.dp); GapW(10.dp) }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = T.Label.copy(color = if (item.attention) C.AccentDeep else C.Navy))
                        Text(item.explanation, style = T.Small)
                    }
                }
                TextButton(onClick = { notice = item }, modifier = Modifier.testTag("notice-${item.id}")) { Text("Scopri perché", color = C.Navy) }
                if (full) Text(item.action, style = T.Small)
                if (item.entryId != null) TextButton(onClick = { Nav.go(Screen.ReplaceFood(item.entryId)) }) { Text("Sostituisci alimento", color = C.AccentDeep) }
            }
            if (!full && review.notices.size > 2) TextButton(onClick = { Nav.go(Screen.Micronutrients) }) { Text("Tutti i consigli (${review.notices.size})", color = C.Navy) }
            BreakfastAlternatives(s)
        }
    }
    if (full) FullReviewDetails(review, s)
    CompleteDayControl(s)
    Text("Base scientifica ${NutritionEvidence.VERSION} · verificata il ${NutritionEvidence.REVIEWED}. Funziona offline; nessun aggiornamento automatico delle fonti.", style = T.Small, modifier = Modifier.padding(top = 8.dp))
    val selected = notice
    if (selected != null) AlertDialog(onDismissRequest = { notice = null }, title = { Text(selected.title, style = T.Title2) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(selected.explanation, style = T.Body)
            Text(selected.reference, style = T.Body)
            Text(selected.meaning, style = T.Body)
            Text(selected.action, style = T.Body)
            EvidenceLink(selected.sourceId)
            Text("Riferimenti per adulti sani; non sono diagnosi o prescrizioni individuali.", style = T.Small)
        }
    }, confirmButton = { TextButton(onClick = { notice = null }) { Text("Chiudi") } }, containerColor = C.Surface)
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
