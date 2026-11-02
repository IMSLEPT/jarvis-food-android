package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Store
import com.jarvis.food.engine.DietStyles
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DietStyleDialog(s: AppState, date: LocalDate, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(Store.prefsFor(s, date).styleId) }
    var days by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val energy = Store.energyFor(s, date)
    val problem = (0 until days).firstNotNullOfOrNull { offset ->
        val e = Store.energyFor(s, date.plusDays(offset.toLong()))
        DietStyles.targetProblem(selected, e.macro, e.expectedTdee)
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Alimentazione del piano", style = T.Title2) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()).testTag("diet-style-dialog")) {
                Text("Dal ${fullDay(date).lowercase()}", style = T.Label)
                GapH(8.dp)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Giorno", 7 to "7 giorni", 14 to "14 giorni", 30 to "30 giorni").forEach { (n, label) ->
                        Box(Modifier.testTag("diet-period-$n")) { Pill(label, days == n) { if (!busy) { days = n; error = null } } }
                    }
                }
                GapH(12.dp)
                Text("Target invariati", style = T.Label)
                Text("${energy.targetKcal.r0()} kcal · P ${energy.protein.r0()} g · C ${energy.carbs.r0()} g · G ${energy.fat.r0()} g", style = T.Small)
                Text("Ogni giorno mantiene i propri target, compresi giorni ON/OFF. Il piano usa approssimazioni: kcal ±10%, macro ±15%; nessuna modifica se non riesce a rispettarle.", style = T.Small)
                GapH(12.dp)
                DietStyles.options.forEach { option ->
                    Row(Modifier.fillMaxWidth().testTag("diet-style-${option.id}").clip(RoundedCornerShape(16.dp))
                        .background(if (selected == option.id) C.AccentSoft else C.Surface)
                        .selectable(selected == option.id, enabled = !busy, role = Role.RadioButton, onClick = { selected = option.id; error = null })
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(option.title, style = T.Label)
                            if (selected == option.id) Text(option.description, style = T.Small)
                            if (selected == option.id && problem != null) Text(problem, style = T.Small.copy(color = C.Danger))
                        }
                        if (selected == option.id) { GapW(8.dp); AIcon(Ic.Check, C.AccentDeep, 18.dp) }
                    }
                    GapH(4.dp)
                }
                (error ?: problem)?.let { Text(it, Modifier.testTag("diet-style-error"), style = T.Small.copy(color = C.Danger)) }
                if (busy) Text("Calcolo le combinazioni e verifico i target…", style = T.Small)
                Text("Il diario e i pasti già registrati si conservano. La conferma aggiorna soltanto il periodo scelto e ricorda lo stile per i prossimi piani.", style = T.Small)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && problem == null, modifier = Modifier.testTag("diet-style-confirm"), onClick = {
                busy = true; error = null
                scope.launch {
                    val result = try { Store.regenerateStyle(date, days, selected) }
                    catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; "Calcolo non riuscito. Il piano resta invariato: ${e.message ?: "riprova"}" }
                    finally { busy = false }
                    if (result == null) onDismiss() else error = result
                }
            }) { Text(if (busy) "Calcolo…" else "Conferma e aggiorna") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Annulla") } },
        containerColor = C.Surface)
}
