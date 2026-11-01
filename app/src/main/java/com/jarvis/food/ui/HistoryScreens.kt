package com.jarvis.food.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Store
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val historyDate = DateTimeFormatter.ofPattern("d MMMM yyyy", IT)

@Composable
fun WeightHistoryScreen() {
    val state by Store.state.collectAsState()
    val samples = remember(state.weights) { HistoryData.weights(state) }
    Column(Modifier.fillMaxSize().testTag("weight-history-screen")) {
        TopBar("Storico peso", onBack = { Nav.back() })
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Le tue pesate, dalla più recente.", style = T.Muted) }
            if (samples.isEmpty()) item { EmptyState("⚖️", "Nessuna pesata registrata", "Lo storico mostrerà le misurazioni salvate nel profilo.") }
            if (samples.size > 1) item { JCard { LineChart(samples.reversed().map { it.kg }, samples.reversed().map { LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("d MMM", IT)) }, C.Navy) } }
            items(samples, key = { it.date }) { sample -> JCard(Modifier.testTag("weight-history-${sample.date}")) {
                Text(LocalDate.parse(sample.date).format(historyDate), style = T.Small)
                Text("${sample.kg.r1()} kg", style = T.Title2)
            } }
        }
    }
}

@Composable
fun CalorieHistoryScreen() {
    val state by Store.state.collectAsState()
    var period by remember { mutableStateOf<Int?>(30) }
    val recorded = remember(state.diary) { HistoryData.calories(state) }
    val days = remember(recorded, period, Nav.today) { HistoryData.inPeriod(recorded, Nav.today, period) }
    Column(Modifier.fillMaxSize().testTag("calorie-history-screen")) {
        TopBar("Storico calorie", onBack = { Nav.back() })
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 giorni", 30 to "30 giorni", null to "Tutto").forEach { (value, text) ->
                    SquareAction(text, Modifier.weight(1f), tag = "calorie-history-period-${value ?: "all"}", isSelected = value == period) { period = value }
                }
            } }
            item { Text("${days.size} giorni con registrazioni. Le giornate possono essere parziali; i giorni senza dati non valgono zero calorie.", style = T.Small, modifier = Modifier.testTag("calorie-history-coverage")) }
            if (days.isEmpty()) item { EmptyState("📊", "Nessuna registrazione nel periodo", "Lo storico usa gli alimenti già salvati nel diario.") }
            items(days, key = { it.date.toString() }) { day -> JCard(Modifier.testTag("calorie-history-${day.date}")) {
                Text(day.date.format(historyDate), style = T.Small)
                Text("${kcalFmt(day.totals.kcal)} kcal", style = T.Title2)
                Text("P ${day.totals.protein.r0()} g · C ${day.totals.carbs.r0()} g · G ${day.totals.fat.r0()} g", style = T.Small)
                Text("${day.entries} alimenti registrati", style = T.Small)
            } }
        }
    }
}
