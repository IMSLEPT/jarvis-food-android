package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.*
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingGoalsScreen() {
    val s by Store.state.collectAsState()
    val original = remember { s.trainingNutrition }
    val base = remember { Store.energyFor(s.copy(trainingNutrition = TrainingNutrition()), LocalDate.now()) }
    val fallback = DailyTargets(base.baseKcal.roundToInt(), base.protein, (base.carbs - base.extraKcal / 4).coerceAtLeast(0.0), base.fat)
    fun values(t: DailyTargets) = listOf(t.kcal.toString(), amountText(t.protein), amountText(t.carbs), amountText(t.fat))
    var automatic by remember { mutableStateOf(original.automatic) }
    val initialLoad = AutomaticTraining.load(s.profile, original)
    var minutes by remember { mutableStateOf(initialLoad.minutes.toString()) }
    var trainingType by remember { mutableIntStateOf(initialLoad.type) }
    var intensity by remember { mutableIntStateOf(initialLoad.intensity) }
    var showCalculation by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(original.enabled) }
    var days by remember { mutableStateOf(original.trainingDays) }
    var onValues by remember { mutableStateOf(values(original.on ?: fallback)) }
    var offValues by remember { mutableStateOf(values(original.off ?: fallback)) }
    fun target(v: List<String>): DailyTargets? {
        val nums = v.map { parseNum(it) ?: return null }
        if (nums.any { !it.isFinite() } || nums[0] % 1.0 != 0.0) return null
        return DailyTargets(nums[0].toInt(), nums[1], nums[2], nums[3]).takeIf { it.valid }
    }
    val pendingSettings = original.copy(enabled = enabled, automatic = automatic, trainingDays = days,
        workout = TrainingLoad(minutes.toIntOrNull() ?: 0, trainingType, intensity))
    val estimate = AutomaticTraining.calculate(s.profile, Store.currentWeight(s), pendingSettings)
    val on = target(onValues)
    val off = target(offValues)
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Giorni ON e OFF", { Nav.back() })
        Column(Modifier.weight(1f).testTag("training-goals-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Obiettivi ON/OFF", style = T.Title2)
                        Text("Calorie e macro diversi per allenamento e riposo.", style = T.Small)
                    }
                    Switch(enabled, { enabled = it }, modifier = Modifier.testTag("training-enabled"), colors = SwitchDefaults.colors(checkedThumbColor = C.Surface, checkedTrackColor = C.Navy))
                }
                Text("Quando sono attivi vengono usati i valori impostati qui. Le attività restano registrate senza aggiungere altre calorie.", style = T.Small, modifier = Modifier.padding(top = 12.dp))
            }
            ASegment(listOf("Manuale", "Automatico Jarvis"), if (automatic) 1 else 0) { automatic = it == 1 }
            if (automatic) {
                JCard {
                    Text("Il tuo allenamento", style = T.Title2)
                    Text("Uso ${Store.currentWeight(s).r1()} kg, altezza ${s.profile.heightCm.r0()} cm, età ${Nutrition.age(s.profile)}, lavoro e passi dal profilo. Il calendario sotto determina quante sessioni fai a settimana.", style = T.Small)
                    TextButton(onClick = { Nav.go(Screen.ProfileEdit) }) { Text("Controlla dati del profilo", color = C.AccentDeep) }
                    Text("Tipo di allenamento", style = T.Label)
                    ASegment(Nutrition.TRAINING_LABELS, trainingType, vertical = true) { trainingType = it }
                    Spacer(Modifier.height(12.dp))
                    JField(minutes, { minutes = it.filter { c -> c.isDigit() }.take(3) }, "Minuti per sessione (pause comprese)", Modifier.testTag("auto-training-minutes"), number = true)
                    Spacer(Modifier.height(12.dp))
                    Text("Intensità media della sessione", style = T.Label)
                    ASegment(listOf("Leggera", "Moderata", "Intensa"), intensity, vertical = true) { intensity = it }
                    Text("Stima energetica, non una misura: le pause e lo sforzo reale possono cambiare il consumo.", style = T.Small, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Text("La tua settimana", style = T.H2)
            Text("Seleziona i giorni di allenamento. Gli altri saranno di riposo.", style = T.Small)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom").forEachIndexed { i, day ->
                    val isOn = i + 1 in days
                    val shape = RoundedCornerShape(16.dp)
                    Box(Modifier.testTag("training-weekday-${i + 1}").heightIn(min = 48.dp).widthIn(min = 48.dp).clip(shape)
                        .background(if (isOn) C.Navy else C.Surface).border(1.dp, C.Line, shape)
                        .semantics { selected = isOn; stateDescription = if (isOn) "Allenamento ON" else "Riposo OFF" }
                        .clickable(role = Role.Button) { days = if (isOn) days - (i + 1) else days + (i + 1) }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(day, style = T.Label.copy(color = if (isOn) C.Surface else C.Navy))
                    }
                }
            }
            if (automatic) {
                if (estimate != null) {
                    AutoTargetSummary("ON · Allenamento", estimate.on)
                    AutoTargetSummary("OFF · Riposo", estimate.off)
                    JCard {
                        Text("${estimate.sessions} allenamenti/settimana · media ${estimate.weeklyAverage.r0()} kcal", style = T.Label)
                        Text("Proteine mantenute anche nei giorni di riposo; il recupero continua. La differenza energetica va soprattutto ai carboidrati.", style = T.Small)
                        TextButton(onClick = { showCalculation = !showCalculation }) { Text(if (showCalculation) "Nascondi calcolo" else "Mostra come viene calcolato", color = C.AccentDeep) }
                        if (showCalculation) {
                            Text("Basale: ${estimate.bmr.r0()} kcal (${if (s.profile.bodyFatPct != null && s.profile.bodyFatPct!! in 5.0..55.0) "Katch-McArdle" else "Mifflin-St Jeor"}).\nMovimento quotidiano: × ${"%.3f".format(java.util.Locale.ITALY, estimate.movementFactor)}.\nAllenamento: (${estimate.met.r1()} MET − 1) × ${Store.currentWeight(s).r1()} kg × ${estimate.load.minutes} / 60 = ${estimate.sessionKcal.r0()} kcal nette.\nObiettivo peso: ${if (estimate.dailyAdjustment >= 0) "+" else ""}${estimate.dailyAdjustment.r0()} kcal/giorno, con limiti del modello.\nMedia settimanale: (ON × ${estimate.sessions} + OFF × ${7 - estimate.sessions}) / 7.\nP ${estimate.proteinPerKg.r1()} g/kg; grassi circa 27% o minimo indicativo 0,8 g/kg, carboidrati dalle calorie residue. La preferenza low carb modifica questa ripartizione. Arrotondamento calorie a 10 kcal.", style = T.Small)
                            Text("Fonti: Adult Compendium 2024 (MET); ISSN 2017, protein and exercise. Lavoro/passi e variazione del peso sono stime del modello dell'app, non una misura individuale.", style = T.Small, modifier = Modifier.padding(top = 8.dp))
                        }
                        TextButton(onClick = {
                            onValues = values(estimate.on); offValues = values(estimate.off); automatic = false
                        }) { Text("Modifica manualmente questa proposta", color = C.AccentDeep) }
                    }
                } else Text("Completa i dati del profilo e inserisci una durata fra 5 e 300 minuti. La proposta automatica è disponibile per adulti senza condizioni speciali indicate nel profilo; negli altri casi usa gli obiettivi concordati con il professionista.", style = T.Small.copy(color = C.Danger))
            } else {
                TargetFields("ON · Allenamento", "on", onValues) { i, value -> onValues = onValues.toMutableList().also { it[i] = value } }
                TargetFields("OFF · Riposo", "off", offValues) { i, value -> offValues = offValues.toMutableList().also { it[i] = value } }
            }
            Text("Puoi cambiare ON/OFF per una singola data da Oggi o Piano, senza modificare la settimana abituale.", style = T.Small)
            if (enabled && !automatic && (on == null || off == null)) Text("Inserisci calorie positive e quantità valide per tutti i macronutrienti, sia ON sia OFF.", style = T.Small.copy(color = C.Danger))
            Spacer(Modifier.height(8.dp))
        }
        GSave("Salva giorni ON/OFF", enabled = !enabled || (if (automatic) estimate != null else on != null && off != null)) {
            Store.setTrainingNutrition(pendingSettings.copy(on = on ?: original.on, off = off ?: original.off))
            Nav.back()
        }
    }
}

@Composable
private fun AutoTargetSummary(title: String, target: DailyTargets) {
    JCard {
        Text(title, style = T.Title2)
        Text("${target.kcal} kcal", style = T.H2.copy(color = C.Accent), modifier = Modifier.padding(top = 8.dp))
        Text("P ${target.protein.r1()} g · C ${target.carbs.r1()} g · G ${target.fat.r1()} g", style = T.Label)
    }
}

@Composable
private fun TargetFields(title: String, key: String, values: List<String>, onEdit: (Int, String) -> Unit) {
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (key == "on") C.Accent else C.Navy))
            Spacer(Modifier.width(8.dp)); Text(title, style = T.Title2)
        }
        Spacer(Modifier.height(16.dp))
        val labels = listOf("Calorie (kcal)", "Proteine (g)", "Carboidrati (g)", "Grassi (g)")
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 0..1) {
                    val i = row * 2 + column
                    JField(values[i], { value -> onEdit(i, value.filter { it.isDigit() || it == ',' || it == '.' }.take(8)) }, labels[i], Modifier.weight(1f).testTag("target-$key-$i"), number = true)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        val nums = values.map { parseNum(it) }
        if (nums.drop(1).all { it != null && it.isFinite() }) {
            val macroEnergy = nums[1]!! * 4 + nums[2]!! * 4 + nums[3]!! * 9
            Text("Energia dai macro: ${macroEnergy.r0()} kcal", style = T.Small)
            if (macroEnergy > 0 && macroEnergy <= 50000 && kotlin.math.abs(macroEnergy - (nums[0] ?: 0.0)) > 10) {
                TextButton(onClick = { onEdit(0, macroEnergy.roundToInt().toString()) }) { Text("Usa queste calorie", color = C.AccentDeep) }
            }
        }
    }
}

/** Date-specific control shared by diary and weekly plan. */
@Composable
fun DayTypeControl(s: AppState, date: LocalDate) {
    if (!s.trainingNutrition.active) return
    var dialog by remember(date) { mutableStateOf(false) }
    val settings = Store.resolvedTraining(s)
    val label = if (settings.isTraining(date)) "ON · Allenamento" else "OFF · Riposo"
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Line, shape).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).testTag("day-type-label").clickable { dialog = true }.padding(vertical = 8.dp)) {
            Text(label, style = T.Label)
            Text(if (date.toString() in settings.overrides) "Solo questa data · cambia" else "Da calendario · cambia", style = T.Small)
        }
        TextButton(onClick = { Nav.go(Screen.TrainingGoals) }) { Text("Configura", color = C.AccentDeep) }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("${dayLabel(date)} · tipo di giornata", style = T.Title2) }, text = {
        Column {
            Text("La modifica vale soltanto per questa data.", style = T.Small)
            listOf("ON · Allenamento" to true, "OFF · Riposo" to false, "Segui calendario settimanale" to null).forEach { (text, value) ->
                TextButton(onClick = { Store.setDayType(date, value); dialog = false }, modifier = Modifier.fillMaxWidth()) { Text(text, color = C.Navy) }
            }
        }
    }, confirmButton = { TextButton(onClick = { dialog = false }) { Text("Chiudi", color = C.Navy) } }, containerColor = C.Surface)
}
