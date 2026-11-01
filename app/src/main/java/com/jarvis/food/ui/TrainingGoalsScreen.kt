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
import androidx.compose.ui.platform.LocalDensity
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
            SettingsCard(title = "Modalità della giornata") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Attiva ON/OFF", style = T.Label)
                        Text("Calorie e macro diversi per allenamento e riposo.", style = T.Small)
                    }
                    Switch(enabled, { enabled = it }, modifier = Modifier.testTag("training-enabled"), colors = SwitchDefaults.colors(checkedThumbColor = C.Surface, checkedTrackColor = C.Navy))
                }
                Text("Quando sono attivi vengono usati i valori impostati qui. Le attività restano registrate senza aggiungere altre calorie.", style = T.Small, modifier = Modifier.padding(top = 12.dp))
            }
            SettingsSegment(listOf("Manuale", "Automatico Jarvis"), if (automatic) 1 else 0) { automatic = it == 1 }
            if (automatic) {
                SettingsCard(title = "Dati usati dal calcolo") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Peso" to "${Store.currentWeight(s).r1()} kg", "Altezza" to "${s.profile.heightCm.r0()} cm", "Età" to "${Nutrition.age(s.profile)} anni").forEach { (label, value) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Bg).padding(10.dp)) {
                                Text(label, style = T.Small)
                                Text(value, style = T.Label)
                            }
                        }
                    }
                    Text("Lavoro: ${Nutrition.JOB_LABELS[s.profile.job.coerceIn(0, 2)]}. Passi abituali: ${Nutrition.STEPS_LABELS[s.profile.stepsLevel.coerceIn(0, 4)]}.", style = T.Small)
                    TextButton(onClick = { Nav.go(Screen.ProfileEdit) }) { Text("Controlla dati del profilo", color = C.AccentDeep) }
                }
                SettingsCard(title = "Allenamento abituale", subtitle = "Configura una sessione tipo, comprese le pause.", warm = true) {
                    Text("Tipo di allenamento", style = T.Label)
                    Nutrition.TRAINING_LABELS.forEachIndexed { index, label ->
                        SettingsChoice(label, selected = trainingType == index) { trainingType = index }
                    }
                    JField(minutes, { minutes = it.filter { c -> c.isDigit() }.take(3) }, "Durata della sessione", Modifier.testTag("auto-training-minutes"), number = true, suffix = "min")
                    Text("Intensità media", style = T.Label)
                    SettingsSegment(listOf("Leggera", "Moderata", "Intensa"), intensity) { intensity = it }
                    Text("Il consumo è stimato: pause e sforzo reale possono modificarlo.", style = T.Small)
                }
            }
            SettingsCard(title = "La tua settimana", subtitle = "I giorni selezionati sono ON. Gli altri sono OFF.") {
                val labels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
                labels.indices.toList().chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { i ->
                            val isOn = i + 1 in days
                            val shape = RoundedCornerShape(14.dp)
                            Column(Modifier.weight(1f).testTag("training-weekday-${i + 1}").heightIn(min = 58.dp).clip(shape)
                                .background(if (isOn) C.Navy else C.Bg).border(1.dp, if (isOn) C.Navy else C.Line, shape)
                                .semantics { selected = isOn; stateDescription = if (isOn) "Allenamento ON" else "Riposo OFF" }
                                .clickable(role = Role.Button) { days = if (isOn) days - (i + 1) else days + (i + 1) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(labels[i], style = T.Label.copy(color = if (isOn) C.Surface else C.Navy))
                                Text(if (isOn) "ON" else "OFF", style = T.Small.copy(fontSize = 12.sp, color = if (isOn) C.Surface.copy(alpha = .8f) else C.Muted))
                            }
                        }
                    }
                }
            }
            SettingsHeading("I tuoi obiettivi", if (automatic) "Proposta calcolata dal profilo e dalla sessione tipo." else "Imposta calorie e grammi separatamente per ON e OFF.", warm = true)

            if (automatic) {
                if (estimate != null) {
                    AutoTargetSummary("ON · Allenamento", estimate.on)
                    AutoTargetSummary("OFF · Riposo", estimate.off)
                    SettingsCard(title = "Il calcolo di Jarvis") {
                        Text("${estimate.sessions} ${if (estimate.sessions == 1) "allenamento" else "allenamenti"}/settimana · media ${estimate.weeklyAverage.r0()} kcal", style = T.Label)
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
    val warm = title.startsWith("ON")
    SettingsCard(title = title, warm = warm) {
        Text("${target.kcal} kcal", style = T.H2.copy(fontSize = 28.sp, color = if (warm) C.Accent else C.Navy))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Proteine" to target.protein, "Carboidrati" to target.carbs, "Grassi" to target.fat).forEach { (label, grams) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.Bg).padding(10.dp)) {
                    Text(label, style = T.Small.copy(fontSize = 12.sp))
                    Text("${grams.r1()} g", style = T.Label)
                }
            }
        }
    }
}

@Composable
private fun TargetFields(title: String, key: String, values: List<String>, onEdit: (Int, String) -> Unit) {
    SettingsCard(title = title, warm = key == "on") {
        val labels = listOf("Calorie", "Proteine", "Carboidrati", "Grassi")
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 0..1) {
                    val i = row * 2 + column
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(labels[i], style = T.Label.copy(fontSize = 13.sp))
                        JField(values[i], { value -> onEdit(i, value.filter { it.isDigit() || it == ',' || it == '.' }.take(8)) }, if (i == 0) "kcal" else "g", Modifier.testTag("target-$key-$i"), number = true)
                    }
                }
            }
        }
        val nums = values.map { parseNum(it) }
        if (nums.drop(1).all { it != null && it.isFinite() }) {
            val energy = nums[1]!! * 4 + nums[2]!! * 4 + nums[3]!! * 9
            Text("Energia dai macro: ${energy.r0()} kcal", style = T.Small)
            if (energy > 0 && energy <= 50000 && kotlin.math.abs(energy - (nums[0] ?: 0.0)) > 10) TextButton(onClick = { onEdit(0, energy.roundToInt().toString()) }) { Text("Usa queste calorie", color = C.AccentDeep) }
        }
    }
}

/** Date-specific status: one compact bar in Oggi, a third summary cell in Piano. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTypeControl(s: AppState, date: LocalDate, compact: Boolean = false) {
    if (!s.trainingNutrition.active) return
    var dialog by remember(date) { mutableStateOf(false) }
    val settings = Store.resolvedTraining(s)
    val label = if (settings.isTraining(date)) "ON · Allenamento" else "OFF · Riposo"
    val caption = if (date.toString() in settings.overrides) "Solo questa data · cambia" else "Da calendario · cambia"
    val status: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("day-type-label")
            .clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { dialog = true }
            .padding(vertical = 2.dp), verticalArrangement = Arrangement.Center) {
            Text(label, style = T.Label.copy(fontSize = if (compact) 12.sp else 14.sp,
                lineHeight = if (compact) 18.sp else 20.sp, color = if (compact) C.AccentDeep else C.Navy))
            Text(caption, style = T.Small.copy(fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = if (compact) 15.sp else 17.sp))
        }
    }
    if (compact) status() else {
        val stacked = LocalDensity.current.fontScale > 1.2f
        FlowRow(Modifier.fillMaxWidth().testTag("today-day-type-bar").clip(RoundedCornerShape(14.dp))
            .background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
            maxItemsInEachRow = if (stacked) 1 else 2, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(if (stacked) Modifier.fillMaxWidth() else Modifier.weight(1f)) { status() }
            TextButton(onClick = { Nav.go(Screen.TrainingGoals) }, modifier = Modifier.heightIn(min = 48.dp).testTag("day-type-configure")) {
                Text("Configura", style = T.Label.copy(fontSize = 14.sp, lineHeight = 20.sp, color = C.AccentDeep))
            }
        }
    }
    if (dialog) DayTypeDialog(date, settings.overrides[date.toString()], onSelect = {
        Store.setDayType(date, it); dialog = false
    }, onClose = { dialog = false })
}

/** Match the reference: compact outlined controls, with full 48 dp touch targets. */
@Composable
private fun DayTypeDialog(date: LocalDate, override: Boolean?, onSelect: (Boolean?) -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = C.Surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * .8f).dp)
                .verticalScroll(rememberScrollState()).padding(18.dp).testTag("day-type-dialog")) {
                Text("${dayLabel(date)} · tipo di giornata", style = T.Title2.copy(fontSize = 18.sp, lineHeight = 24.sp))
                Spacer(Modifier.height(12.dp))
                Text("La modifica vale soltanto per questa data.", style = T.Small.copy(fontSize = 12.sp, lineHeight = 17.sp))
                Spacer(Modifier.height(8.dp))
                listOf("ON · Allenamento" to true, "OFF · Riposo" to false, "Segui calendario settimanale" to null).forEach { (text, value) ->
                    DayTypeDialogAction(text, Modifier.fillMaxWidth(), "day-type-choice-${value ?: "calendar"}", override == value) { onSelect(value) }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DayTypeDialogAction("Chiudi", Modifier.width(76.dp), "day-type-close", small = true, onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun DayTypeDialogAction(label: String, modifier: Modifier, tag: String, isSelected: Boolean = false,
    small: Boolean = false, onClick: () -> Unit) {
    val orange = androidx.compose.ui.graphics.Color(0xFFD46622)
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.heightIn(min = 48.dp).testTag(tag).semantics { selected = isSelected }
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (small) 34.dp else 40.dp).clip(shape)
            .background(if (isSelected) androidx.compose.ui.graphics.Color(0xFFFFE5D2) else C.Surface)
            .border(1.dp, if (isSelected) orange else C.Navy, shape).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = T.Label.copy(fontSize = 14.sp, lineHeight = 20.sp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (isSelected) { Spacer(Modifier.width(6.dp)); AIcon(Ic.Check, orange, 18.dp) }
        }
    }
}
