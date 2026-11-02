package com.jarvis.food.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Store
import com.jarvis.food.engine.DietStyles
import com.jarvis.food.data.Db
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DietStyleDialog(s: AppState, date: LocalDate, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(Store.prefsFor(s, date).styleId) }
    var days by remember { mutableIntStateOf(1) }
    var maxMinutes by remember { mutableIntStateOf(s.dietPrefs.maxMinutes.coerceAtLeast(5)) }
    var chooseTime by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val energy = Store.energyFor(s, date)
    val targetProblem = (0 until days).firstNotNullOfOrNull { offset ->
        val e = Store.energyFor(s, date.plusDays(offset.toLong()))
        DietStyles.targetProblem(selected, e.macro, e.expectedTdee)
    }
    val availability = remember(selected, maxMinutes, s.dietPrefs) {
        DietStyles.availability(Db.db, s.dietPrefs.copy(maxMinutes = maxMinutes), selected)
    }
    val problem = targetProblem ?: availability.message(DietStyles.get(selected).title, maxMinutes)
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(horizontal = 16.dp, vertical = 24.dp).widthIn(max = 560.dp).fillMaxWidth()
            .height((LocalConfiguration.current.screenHeightDp * .84f).dp), shape = RoundedCornerShape(24.dp), color = C.Surface) {
            Column(Modifier.padding(18.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("diet-style-dialog"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Alimentazione del piano", style = T.Title2)
                    Text("Dal ${fullDay(date).lowercase()}", style = T.Small)
                    GapH(12.dp)
                    Text("Per quanto tempo?", style = T.Label)
                }
                listOf(listOf(1 to "Giorno", 7 to "7 giorni"), listOf(14 to "14 giorni", 30 to "30 giorni")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (n, label) -> SquareAction(label, Modifier.weight(1f), tag = "diet-period-$n", isSelected = days == n, enabled = !busy) { days = n; error = null } }
                    }
                }
                SquareAction("Tempo per pasto: $maxMinutes min", Modifier.fillMaxWidth(), tag = "diet-time-toggle", enabled = !busy) { chooseTime = !chooseTime }
                if (chooseTime) {
                    (listOf(5, 10, 15, 20, 25, 30, 40, 60) + maxMinutes).distinct().sorted().chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { n -> SquareAction("$n min", Modifier.weight(1f), tag = "diet-time-$n", isSelected = maxMinutes == n, enabled = !busy) { maxMinutes = n; error = null } }
                        }
                    }
                    Text("Il tempo scelto viene salvato solo con la conferma e vale anche per i prossimi piani.", style = T.Small)
                }
                Text("Scegli l’alimentazione", style = T.Title2)
                DietStyles.options.forEach { option ->
                    SquareAction(option.title, Modifier.fillMaxWidth(), tag = "diet-style-${option.id}", isSelected = selected == option.id,
                        subtitle = if (selected == option.id) option.description else null, enabled = !busy) { selected = option.id; error = null }
                    if (selected == option.id && problem != null) {
                        Text(problem, style = T.Small.copy(color = C.Danger))
                        if (targetProblem == null && availability.requiredMinutes != null) {
                            SquareAction("Usa almeno ${availability.requiredMinutes} min per pasto", Modifier.fillMaxWidth(), tag = "diet-style-use-required-time", enabled = !busy) {
                                maxMinutes = maxOf(maxMinutes, availability.requiredMinutes); error = null
                            }
                        }
                    }
                    if (selected == option.id && selected == "healthy") {
                        Text("È una selezione più mirata degli ingredienti rispetto a Bilanciata e varia. Non garantisce da sola la copertura di tutti i nutrienti né una salute ottimale.", style = T.Small)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SquareAction("OMS", Modifier.weight(1f)) { runCatching { uri.openUri("https://www.who.int/news-room/fact-sheets/detail/healthy-diet") } }
                            SquareAction("CREA", Modifier.weight(1f)) { runCatching { uri.openUri("https://sapermangiare.an.crea.gov.it/483/linee-guida.html") } }
                        }
                    }
                }
                Column {
                    SquareAction(if (details) "Nascondi obiettivi e dettagli" else "Obiettivi e dettagli", Modifier.fillMaxWidth()) { details = !details }
                    if (details) {
                        GapH(8.dp)
                        Text("Target invariati", style = T.Label)
                        Text("${energy.targetKcal.r0()} kcal · P ${energy.protein.r0()} g · C ${energy.carbs.r0()} g · G ${energy.fat.r0()} g", style = T.Small)
                        Text("Ogni giorno mantiene i propri target, compresi i giorni ON/OFF. Tolleranze del piano: kcal ±10%, macro ±15%. Se non si possono rispettare, il piano resta invariato.", style = T.Small)
                    }
                    GapH(8.dp)
                    Text("Il diario e i pasti già registrati si conservano. La conferma aggiorna soltanto il periodo scelto e ricorda lo stile per i prossimi piani.", style = T.Small)
                    (error ?: problem)?.let { GapH(8.dp); Text(it, Modifier.testTag("diet-style-error"), style = T.Small.copy(color = C.Danger)) }

                }
            }
                    GapH(12.dp)
                    SquareAction(if (busy) "Calcolo…" else "Conferma e aggiorna", Modifier.fillMaxWidth(), tag = "diet-style-confirm", primary = true, enabled = !busy && problem == null) {
                        busy = true; error = null
                        scope.launch {
                            val result = try { Store.regenerateStyle(date, days, selected, maxMinutes) }
                            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; "Calcolo non riuscito. Il piano resta invariato: riprova." }
                            finally { busy = false }
                            if (result == null) onDismiss() else error = result
                        }
                    }
                    GapH(8.dp)
                    SquareAction("Annulla", Modifier.fillMaxWidth(), enabled = !busy, onClick = onDismiss)
                }
            }
        }
    }
