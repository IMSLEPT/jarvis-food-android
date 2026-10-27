package com.jarvis.food.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.BodyMeasurements
import com.jarvis.food.data.Store
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MeasurementsScreen() {
    val s by Store.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recordForm = remember { BringIntoViewRequester() }
    var type by remember { mutableStateOf("vita") }
    var value by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showHelp by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    val guide = BodyMeasurements.guide(type)
    val history = s.measurements.filter { it.type == type }.sortedBy { it.date }
    val current = history.firstOrNull { it.date == date.toString() }
    val number = parseNum(value)
    val valid = number != null && BodyMeasurements.valid(type, number)
    val format = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
    fun select(key: String) { type = key; value = ""; saved = false; showHelp = false }
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Misure corporee", { Nav.back() })
        Column(Modifier.weight(1f).testTag("measurements-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Tocca il corpo o scegli una zona. La fascia arancione indica il riferimento per il metro.", style = T.Small)
            SettingsCard {
                BodyModelView(type, s.profile.male, ::select)
                Text("Modello illustrativo: individua i riferimenti sul tuo corpo. Non calcola le misure dalla tua immagine.", style = T.Small.copy(fontSize = 11.sp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BodyMeasurements.guides.forEach { item ->
                        Box(Modifier.testTag("measure-zone-${item.key}")) { Pill(item.name, type == item.key) { select(item.key) } }
                    }
                }
            }
            SettingsCard(title = "Dove misurare · ${guide.name}", warm = true, modifier = Modifier.testTag("measurement-guide")) {
                Text(guide.landmark, style = T.Label)
                TextButton(onClick = { showHelp = !showHelp }) { Text(if (showHelp) "Chiudi istruzioni" else "Come misurare, passo per passo", color = C.AccentDeep) }
                if (showHelp) guide.steps.forEachIndexed { index, instruction ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${index + 1}", style = T.Label.copy(color = C.AccentDeep), modifier = Modifier.width(26.dp))
                        Text(instruction, style = T.Small, modifier = Modifier.weight(1f))
                    }
                }
            }
            SettingsCard(title = "Registra ${guide.name.lowercase()}", modifier = Modifier.bringIntoViewRequester(recordForm)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Data della misura", style = T.Small); Text(date.format(format), style = T.Label) }
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d); saved = false }, date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    }) { Text("Cambia", color = C.AccentDeep) }
                }
                JField(value, { value = it; saved = false }, "${guide.name} (${guide.unit})", Modifier.testTag("measurement-value"), number = true, suffix = guide.unit)
                if (current != null) Text("Per questa data è già presente ${current.value.r1()} ${guide.unit}. Il salvataggio aggiorna questa registrazione.", style = T.Small)
                if (value.isNotBlank() && !valid) Text("Inserisci un valore positivo valido in ${guide.unit}.", style = T.Small.copy(color = C.Danger))
                if (saved) Text("Misura salvata nello storico", style = T.Label.copy(color = C.Green), modifier = Modifier.testTag("measurement-saved"))
            }
            SettingsHeading("Il tuo andamento", "${history.size} ${if (history.size == 1) "registrazione" else "registrazioni"} · ${guide.unit}")
            if (history.isEmpty()) SettingsCard { Text("La prima misura darà inizio al tuo storico.", style = T.Small) }
            else {
                SettingsCard {
                    val latest = history.last()
                    Text("Ultima misura", style = T.Small)
                    Text("${latest.value.r1()} ${guide.unit}", style = T.H2)
                    if (history.size > 1) {
                        val change = latest.value - history.first().value
                        Text("${if (change > 0) "+" else ""}${change.r1()} ${guide.unit} dalla prima registrazione", style = T.Small)
                    }
                    LineChart(history.map { it.value }, history.map { LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("d/M", Locale.ITALIAN)) }, C.Navy)
                }
                history.reversed().forEach { item ->
                    SettingsCard(modifier = Modifier.testTag("measurement-history-${item.date}")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(LocalDate.parse(item.date).format(format), style = T.Small)
                                Text("${item.value.r1()} ${guide.unit}", style = T.Label)
                            }
                            IconButton(onClick = { date = LocalDate.parse(item.date); value = amountText(item.value); saved = false; scope.launch { recordForm.bringIntoView() } }, modifier = Modifier.semantics { contentDescription = "Modifica misura del ${item.date}" }) { AIcon("M4 20l4.5-1 12-12a2.1 2.1 0 0 0-3-3l-12 12L4 20zM15.5 5.5l3 3", C.Navy, 22.dp) }
                            IconButton(onClick = { deleting = item.date }, modifier = Modifier.semantics { contentDescription = "Elimina misura del ${item.date}" }) { AIcon("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12", C.Faint, 20.dp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        GSave(if (current != null) "Aggiorna misura" else "Salva misura", enabled = valid) {
            Store.addMeasurement(date, type, number!!); value = ""; saved = true
        }
    }
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Elimina questa misura?") },
        text = { Text("${guide.name} · ${LocalDate.parse(deleting).format(format)}", style = T.Body) },
        confirmButton = { TextButton(onClick = {
            val key = type; val day = deleting
            Store.update { state -> state.copy(measurements = state.measurements.filterNot { it.type == key && it.date == day }) }
            deleting = null
        }) { Text("Elimina", color = C.Danger) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annulla", color = C.Navy) } }, containerColor = C.Surface)
}
