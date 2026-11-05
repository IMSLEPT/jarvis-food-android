package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.*
import com.jarvis.food.engine.NutritionEvidence
import com.jarvis.food.engine.NutritionScience
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class TrendSample(val date: LocalDate, val value: Double?, val complete: Boolean = true)

/** Read-only history: selecting a day or a period never inserts missing measurements. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendDetailScreen(kind: String, initialDays: Int = 7, initialMetric: Int = 0) {
    val s by Store.state.collectAsState()
    var days by remember { mutableIntStateOf(initialDays) }
    var metric by remember(kind, initialMetric) { mutableIntStateOf(initialMetric.coerceIn(0, when (kind) { "macro", "vitamins" -> 2; "activity" -> 1; else -> 0 })) }
    var picked by remember(days, metric) { mutableIntStateOf(days - 1) }
    val title = when (kind) { "macro" -> "Macronutrienti"; "water" -> "Acqua"; "activity" -> "Passi e attività"; else -> "Vitamine e minerali" }
    val options = when (kind) { "macro" -> listOf("Proteine", "Carboidrati", "Grassi"); "activity" -> listOf("Passi", "Calorie attività"); "vitamins" -> listOf("Vitamina C", "Vitamina D", "Vitamina B12"); else -> emptyList() }
    val key = when (kind) { "macro" -> listOf("protein", "carbs", "fat")[metric]; "vitamins" -> listOf("vitC", "vitD", "vitB12")[metric]; else -> "" }
    val unit = when (kind) { "water" -> "L"; "activity" -> if (metric == 0) "passi" else "kcal"; "vitamins" -> if (metric == 0) "mg" else "µg"; else -> "g" }
    val dates = (days - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val samples = dates.map { date ->
        val rows = Store.entriesFor(s, date)
        when (kind) {
            "water" -> TrendSample(date, s.water[date.toString()]?.div(1000.0))
            "activity" -> {
                val hasData = if (metric == 0) date.toString() in s.steps || date.toString() in s.health
                    else date.toString() in s.health || s.activities.any { it.date == date.toString() }
                TrendSample(date, if (!hasData) null else if (metric == 0) Store.stepsFor(s, date).toDouble() else Store.burnedFor(s, date).toDouble())
            }
            else -> {
                val known = rows.mapNotNull { row -> NutritionScience.per100(row.food, key)?.times(row.grams / 100) }
                TrendSample(date, if (known.isEmpty()) null else known.sum(), rows.isNotEmpty() && known.size == rows.size)
            }
        }
    }
    val selected = samples[picked]
    val energy = Store.energyFor(s, selected.date)
    val reference = when (kind) {
        "macro" -> listOf(energy.protein, energy.carbs, energy.fat)[metric]
        "water" -> s.goals.waterMl / 1000.0
        "activity" -> if (metric == 0) s.goals.steps.toDouble() else null
        else -> NutritionEvidence.reference(key, s.profile)
    }
    val maximum = maxOf(samples.mapNotNull { it.value }.maxOrNull() ?: 0.0, reference ?: 0.0, 1.0)
    val chartInk = TrendPalette.ink(kind, metric)
    val chartBrush = TrendPalette.bars(kind, metric)
    val known = samples.filter { it.value != null }
    val vitamins = kind == "vitamins"
    Column(Modifier.fillMaxSize().navigationBarsPadding().testTag("trend-$kind")) {
        TopBar(title, onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)) {
            val periods = listOf(7, 14, 30)
            if (vitamins) {
                val large = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (large) 2 else 3,
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    periods.forEach { period ->
                        RefOption("$period giorni", Modifier.weight(1f), isSelected = period == days,
                            selectedStyle = OptionStyle.Navy, minHeight = 48.dp, centered = true, labelSize = 13.sp, trailing = {}) { days = period }
                    }
                }
                GapH(8.dp)
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (large) 2 else 3,
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { i, name ->
                        RefOption(name, Modifier.weight(1f), isSelected = metric == i,
                            minHeight = 48.dp, centered = true, labelSize = 12.sp, trailing = {}) { metric = i }
                    }
                }
            } else {
                ASegment(periods.map { "$it giorni" }, periods.indexOf(days)) { days = periods[it] }
                if (options.isNotEmpty()) {
                    GapH(12.dp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEachIndexed { i, name -> Pill(name, metric == i) { metric = i } }
                    }
                }
            }
            GapH(12.dp)
            JCard {
                if (vitamins) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(RD.NavyBrush).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AIcon(RIc.Leaf, RD.Orange, 21.dp)
                        GapW(8.dp)
                        Column(Modifier.weight(1f)) {
                            Text(options[metric], style = T.Title2.copy(color = Color.White))
                            Text("Andamento ultimi $days giorni", style = T.Small.copy(color = Color.White))
                        }
                        reference?.let {
                            GapW(6.dp)
                            Column(Modifier.clip(RoundedCornerShape(10.dp)).background(RD.NavyHi).padding(7.dp)) {
                                Text("Riferimento EFSA", style = T.Small.copy(fontSize = 11.sp, color = Color.White))
                                Text("${it.r1()} $unit", style = T.Label.copy(color = RD.Orange))
                            }
                        }
                    }
                } else Text(if (options.isEmpty()) title else options[metric], style = T.Title2)
                GapH(6.dp)
                Text("Tocca una colonna per vedere la giornata", style = T.Small)
                GapH(18.dp)
                Box(Modifier.fillMaxWidth().height(180.dp).testTag("trend-chart")) {
                    Canvas(Modifier.fillMaxSize()) {
                        reference?.let { target -> val y = size.height * (1 - chartFraction(target, maximum)); drawLine(chartInk.copy(alpha = .45f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx()) }
                    }
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (days == 7) 8.dp else 3.dp), verticalAlignment = Alignment.Bottom) {
                        samples.forEachIndexed { i, sample ->
                            Box(Modifier.weight(1f).fillMaxHeight().testTag("trend-day-$i")
                                .semantics { contentDescription = "${sample.date}: ${sample.value?.r1() ?: "non registrato"} $unit" }
                                .clickable { picked = i }.then(if (i == picked) Modifier.border(2.dp, chartInk, RoundedCornerShape(8.dp)) else Modifier), contentAlignment = Alignment.BottomCenter) {
                                if (sample.value != null && sample.value > 0) Box(Modifier.fillMaxWidth().fillMaxHeight(chartFraction(sample.value, maximum)).clip(RoundedCornerShape(6.dp))
                                    .background(chartBrush))
                                else Box(Modifier.fillMaxWidth().height(2.dp).background(C.Line))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dates.first().format(DateTimeFormatter.ofPattern("d MMM", IT)), style = T.Small)
                    Text(dates.last().format(DateTimeFormatter.ofPattern("d MMM", IT)), style = T.Small)
                }
                GapH(12.dp)
                Text(fullDay(selected.date), style = T.Label)
                Text(selected.value?.let { "${it.r1()} $unit${if (selected.complete) "" else " · quantità minima nota"}" } ?: "Nessun dato registrato", Modifier.testTag("trend-selected-value"), style = T.Title2)
                if (!vitamins) reference?.let { Text("Obiettivo impostato: ${it.r1()} $unit", style = T.Small) }
                if (kind in listOf("macro", "vitamins")) Text("Somma dei pasti registrati; un diario incompleto non rappresenta tutta la giornata.", style = T.Small)
            }
            GapH(14.dp)
            Text("${known.size}/$days giorni con dati", style = T.Label)
            Text(if (known.isEmpty()) "Registra o sincronizza dei dati per vedere l’andamento."
                else "Media dei giorni con dati: ${known.mapNotNull { it.value }.average().r1()} $unit", style = T.Small)
            if (kind == "vitamins") {
                Text("I valori assenti restano sconosciuti. Le colonne parziali mostrano soltanto le quantità note e non diagnosticano carenze.", style = T.Small)
                GapH(12.dp)
                RefOutlineButton("Consigli, dati e fonti", Modifier.fillMaxWidth(), icon = Ic.Receipt, divider = false) { Nav.go(Screen.Micronutrients) }
            }
            GapH(18.dp)
            Text("Storico giornaliero", style = T.Title2)
            samples.asReversed().forEach { sample ->
                Row(Modifier.fillMaxWidth().clickable { picked = dates.indexOf(sample.date) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sample.date.format(DateTimeFormatter.ofPattern("d MMM", IT)), style = T.Body)
                    Text(sample.value?.let { "${it.r1()} $unit${if (sample.complete) "" else " · parziale"}" } ?: "Non registrato", style = T.Small)
                }
            }
            GapH(24.dp)
        }
    }
}
