package com.jarvis.food.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val DM = DateTimeFormatter.ofPattern("d MMM", IT)

/** Giorni consecutivi (fino a oggi o ieri) con almeno un alimento registrato. */
fun streak(s: AppState): Int {
    val days = s.diary.map { it.date }.toSet()
    var d = LocalDate.now()
    if (d.toString() !in days) d = d.minusDays(1)
    var n = 0
    while (d.toString() in days) { n++; d = d.minusDays(1) }
    return n
}

@Composable
fun ProgressScreen() {
    val s by Store.state.collectAsState()
    var period by remember { mutableIntStateOf(7) }
    val today = LocalDate.now()
    val days = (period - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val daily = days.map { Nutrition.sum(Store.entriesFor(s, it)) }
    val logged = daily.filter { it.kcal > 0 }
    val avg = if (logged.isEmpty()) null else logged.reduce { a, b -> a + b }.scaled(1.0 / logged.size)
    val weight = Store.currentWeight(s)
    val bmi = Nutrition.bmi(weight, s.profile.heightCm)
    val labelOf: (LocalDate) -> String = { if (period <= 7) it.dayOfWeek.getDisplayName(TextStyle.NARROW, IT) else if (it.dayOfMonth % 5 == 0) it.dayOfMonth.toString() else "" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Progressi", style = T.Display)
        Text("🔥 ${streak(s)} giorni di fila registrati", style = T.Muted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("7 giorni", period == 7) { period = 7 }
            Pill("30 giorni", period == 30) { period = 30 }
            Pill("90 giorni", period == 90) { period = 90 }
        }

        SectionTitle("Vitamine e minerali", action = "Analisi completa") { Nav.go(Screen.Micronutrients) }
        MicroSummaryCard(s)

        SectionTitle("Peso", action = "Registra") { Nav.go(Screen.Weight) }
        JCard(onClick = { Nav.go(Screen.Weight) }) {
            Row {
                Stat("Attuale", "${weight.r1()} kg", C.Text)
                Spacer(Modifier.weight(1f))
                val diff = weight - s.profile.startWeight
                Stat("Da inizio", (if (diff > 0) "+" else "") + "${diff.r1()} kg", if (diff == 0.0) C.Muted else C.Accent)
                Spacer(Modifier.weight(1f))
                Stat("Obiettivo", "${s.profile.targetWeight.r1()} kg", C.Muted)
            }
            Spacer(Modifier.height(12.dp))
            val from = today.minusDays(period.toLong())
            val ws = s.weights.filter { !LocalDate.parse(it.date).isBefore(from) }
            LineChart(ws.map { it.kg }, ws.map { LocalDate.parse(it.date).format(DM) }, C.Accent, target = s.profile.targetWeight)
            Spacer(Modifier.height(8.dp))
            Text("IMC ${bmi.r1()} · ${Nutrition.bmiLabel(bmi)}", style = T.Small)
        }

        SectionTitle("Calorie")
        JCard {
            Row {
                Stat("Media giornaliera", avg?.kcal?.r0()?.plus(" kcal") ?: "—", C.Text)
                Spacer(Modifier.weight(1f))
                Stat("Giorni registrati", "${logged.size}/$period", C.Muted)
            }
            Spacer(Modifier.height(12.dp))
            BarChart(daily.map { it.kcal }, days.map(labelOf), Store.energyFor(s, today).baseKcal, C.Accent)
        }

        SectionTitle("Macronutrienti (media)")
        JCard {
            if (avg == null) Text("Registra qualche pasto per vedere le medie.", style = T.Muted)
            else {
                val (gc, gp, gf) = Nutrition.goalGrams(s.goals)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacroDonut(avg.carbs, avg.protein, avg.fat)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroBar("Carboidrati", avg.carbs, gc, C.Carbs)
                        MacroBar("Proteine", avg.protein, gp, C.Protein)
                        MacroBar("Grassi", avg.fat, gf, C.Fat)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Fibre ${avg.fiber.r1()} g · Zuccheri ${avg.sugar.r1()} g · Sale ${avg.salt.r1()} g", style = T.Small)
            }
        }

        SectionTitle("Acqua")
        JCard {
            val water = days.map { (s.water[it.toString()] ?: 0) / 1000.0 }
            BarChart(water, days.map(labelOf), s.goals.waterMl / 1000.0, C.Water)
            Text("Media ${(water.average()).r1()} L al giorno", style = T.Small, modifier = Modifier.padding(top = 6.dp))
        }

        SectionTitle("Passi e attività")
        JCard {
            val burned = days.map { Store.burnedFor(s, it).toDouble() }
            BarChart(burned, days.map(labelOf), null, C.Green)
            Text("Calorie bruciate in media: ${burned.average().r0()} kcal", style = T.Small, modifier = Modifier.padding(top = 6.dp))
        }

        SectionTitle("Misure corporee", action = "Apri") { Nav.go(Screen.Measurements) }
        JCard(onClick = { Nav.go(Screen.Measurements) }) {
            val latest = MEASURES.mapNotNull { (k, label) -> s.measurements.filter { it.type == k }.maxByOrNull { it.date }?.let { label to it.value } }
            if (latest.isEmpty()) Text("Vita, fianchi, petto, braccia, cosce e massa grassa.", style = T.Muted)
            latest.forEach { (l, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(l, style = T.Body, modifier = Modifier.weight(1f)); Text(v.r1(), style = T.Label)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

val MEASURES = listOf(
    "vita" to "Vita (cm)", "fianchi" to "Fianchi (cm)", "petto" to "Petto (cm)", "braccio" to "Braccio (cm)",
    "coscia" to "Coscia (cm)", "collo" to "Collo (cm)", "grasso" to "Massa grassa (%)"
)

@Composable
fun WeightScreen() {
    val s by Store.state.collectAsState()
    var kg by remember { mutableStateOf(Store.currentWeight(s).r1()) }
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Peso", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            JCard {
                Text("Peso di oggi", style = T.Title2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GhostButton("−0,1") { kg = ((parseNum(kg) ?: 0.0) - 0.1).r1() }
                    Spacer(Modifier.width(8.dp))
                    JField(kg, { kg = it }, "kg", Modifier.weight(1f), number = true, suffix = "kg")
                    Spacer(Modifier.width(8.dp))
                    GhostButton("+0,1") { kg = ((parseNum(kg) ?: 0.0) + 0.1).r1() }
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Salva", Modifier.fillMaxWidth(), enabled = (parseNum(kg) ?: 0.0) in 30.0..300.0) {
                    Store.addWeight(LocalDate.now(), parseNum(kg)!!)
                }
                if (s.goals.auto) Text("L'obiettivo calorico si aggiorna in automatico con il nuovo peso.", style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
            SectionTitle("Storico")
            s.weights.reversed().forEach { w ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalDate.parse(w.date).format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", IT)), style = T.Body, modifier = Modifier.weight(1f))
                    Text("${w.kg.r1()} kg", style = T.Label)
                    Spacer(Modifier.width(10.dp))
                    if (s.weights.size > 1) GhostButton("✕", color = C.Faint) { Store.removeWeight(w.date) }
                }
                HorizontalDivider(color = C.Line)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MeasurementsScreen() {
    val s by Store.state.collectAsState()
    var type by remember { mutableStateOf(MEASURES.first().first) }
    var value by remember { mutableStateOf("") }
    val hist = s.measurements.filter { it.type == type }.sortedBy { it.date }
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Misure corporee", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MEASURES.forEach { (k, l) -> Pill(l.substringBefore(" ("), type == k) { type = k; value = "" } }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                JField(value, { value = it }, MEASURES.first { it.first == type }.second, Modifier.weight(1f), number = true)
                Spacer(Modifier.width(8.dp))
                PrimaryButton("Salva", enabled = parseNum(value) != null) {
                    Store.addMeasurement(LocalDate.now(), type, parseNum(value)!!); value = ""
                }
            }
            Spacer(Modifier.height(14.dp))
            JCard { LineChart(hist.map { it.value }, hist.map { LocalDate.parse(it.date).format(DM) }, C.Carbs) }
            Spacer(Modifier.height(10.dp))
            hist.reversed().forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(LocalDate.parse(m.date).format(DM), style = T.Body, modifier = Modifier.weight(1f))
                    Text(m.value.r1(), style = T.Label)
                }
            }
        }
    }
}
