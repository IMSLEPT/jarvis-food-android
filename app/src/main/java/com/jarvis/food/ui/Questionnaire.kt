package com.jarvis.food.ui

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Goals
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.data.WeightEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Passi del questionario. */
enum class QStep(val title: String) {
    Welcome("Benvenuto"), About("Parlami di te"), Body("Il tuo corpo"), Job("La tua giornata"), Steps("Quanto cammini"),
    Training("I tuoi allenamenti"), Goal("Il tuo obiettivo"), Diet("Preferenze"), Plan("Il tuo piano")
}

/** Risposte del questionario, modificabili. Vale sia per la prima configurazione sia per le modifiche successive. */
class QDraft(p: Profile, val isNew: Boolean, weightNow: Double) {
    var name by mutableStateOf(p.name)
    var photo by mutableStateOf(p.photo)
    var male by mutableStateOf(p.male)
    var birth by mutableStateOf<LocalDate?>(
        p.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: if (isNew) null else LocalDate.of(p.birthYear, 6, 30)
    )
    var height by mutableStateOf(if (isNew) "" else p.heightCm.r0())
    var weight by mutableStateOf(if (isNew) "" else weightNow.r1())
    var target by mutableStateOf(if (isNew) "" else p.targetWeight.r1())
    var bodyFat by mutableStateOf(p.bodyFatPct?.r1() ?: "")
    var job by mutableIntStateOf(p.job)
    var steps by mutableIntStateOf(p.stepsLevel)
    var workouts by mutableIntStateOf(p.workouts)
    var workoutMin by mutableIntStateOf(p.workoutMin)
    var intensity by mutableIntStateOf(p.workoutIntensity)
    var trainingType by mutableIntStateOf(p.trainingType)
    var goal by mutableIntStateOf(p.goal)
    var rate by mutableDoubleStateOf(p.weeklyRate)
    var dietStyle by mutableIntStateOf(p.dietStyle)
    var special by mutableIntStateOf(p.special)
    private val weightKnown = weightNow

    /** Peso usato nei calcoli: quello inserito (nuovo profilo) oppure l'ultimo registrato. */
    fun weightKg(): Double = if (isNew) parseNum(weight) ?: 75.0 else weightKnown

    private fun approxActivity(): Int = when {
        job == 2 && workouts >= 4 -> 4
        workouts >= 6 -> 3
        workouts >= 3 -> 2
        workouts >= 1 -> 1
        job >= 1 || steps >= 3 -> 1
        else -> 0
    }

    fun toProfile(base: Profile): Profile {
        val bd = birth ?: LocalDate.of(base.birthYear, 6, 30)
        return base.copy(
            name = name.trim().ifBlank { "Tu" }, male = male, birthYear = bd.year, birthDate = bd.toString(),
            heightCm = parseNum(height) ?: base.heightCm,
            startWeight = if (isNew) (parseNum(weight) ?: base.startWeight) else base.startWeight,
            targetWeight = parseNum(target) ?: if (isNew) (parseNum(weight) ?: base.targetWeight) else base.targetWeight,
            bodyFatPct = parseNum(bodyFat)?.takeIf { it in 5.0..55.0 },
            job = job, stepsLevel = steps, workouts = workouts, workoutMin = workoutMin, workoutIntensity = intensity, trainingType = trainingType,
            goal = goal, weeklyRate = rate, dietStyle = dietStyle, special = special, activity = approxActivity(), detailed = true, photo = photo
        )
    }

    fun valid(step: QStep): Boolean = when (step) {
        QStep.About -> birth?.let { Nutrition.age(Profile(birthYear = it.year, birthDate = it.toString())) in 14..100 } == true &&
                (parseNum(height) ?: 0.0) in 120.0..230.0
        QStep.Body -> (!isNew || (parseNum(weight) ?: 0.0) in 30.0..300.0) && (parseNum(target) ?: 0.0) in 30.0..300.0 &&
                (bodyFat.isBlank() || (parseNum(bodyFat) ?: 0.0) in 5.0..55.0)
        else -> true
    }
}

// ---------------------------------------------------------------- Contenuto dei passi

@Composable
private fun StepHeader(title: String, why: String? = null) {
    Text(title, style = T.Display)
    if (why != null) { Spacer(Modifier.height(6.dp)); Text(why, style = T.Muted) }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun Hint(text: String) { Text(text, style = T.Small, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) }

@Composable
fun PhotoPicker(d: QDraft) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ProfilePhoto.save(ctx, uri)?.let { d.photo = it }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable { launcher.launch("image/*") }) { Avatar(d.name, d.photo, 84.dp) }
        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GhostButton(if (d.photo == null) "Aggiungi foto" else "Cambia foto") { launcher.launch("image/*") }
            if (d.photo != null) Text("Rimuovi", style = T.Label.copy(color = C.Danger),
                modifier = Modifier.clickable { ProfilePhoto.delete(d.photo); d.photo = null }.padding(6.dp))
        }
    }
}

@Composable
fun QContent(step: QStep, d: QDraft) {
    val ctx = LocalContext.current
    when (step) {
        QStep.Welcome -> {
            Text("🥗", style = T.Big)
            Spacer(Modifier.height(12.dp))
            Text("Benvenuto in\nJarvis Food", style = T.Display)
            Spacer(Modifier.height(10.dp))
            Text("Rispondo ad alcune domande per calcolare le tue calorie in modo preciso e onesto. Più rispondi, più il risultato è affidabile. " +
                    "Tutti i dati restano sul tuo telefono.", style = T.Muted)
            Spacer(Modifier.height(22.dp))
            PhotoPicker(d)
            Spacer(Modifier.height(16.dp))
            JField(d.name, { d.name = it }, "Come ti chiami?")
        }
        QStep.About -> {
            StepHeader(step.title, "Sesso, età e altezza determinano il metabolismo basale: le calorie che bruci a riposo.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Uomo", d.male) { d.male = true }
                Pill("Donna", !d.male) { d.male = false }
            }
            Spacer(Modifier.height(14.dp))
            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)
            Text("Data di nascita", style = T.Label)
            Spacer(Modifier.height(6.dp))
            GhostButton(d.birth?.format(fmt) ?: "Scegli la data", Modifier.fillMaxWidth()) {
                val b = d.birth ?: LocalDate.of(1995, 6, 15)
                DatePickerDialog(ctx, { _, y, m, day -> d.birth = LocalDate.of(y, m + 1, day) }, b.year, b.monthValue - 1, b.dayOfMonth).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                }.show()
            }
            Spacer(Modifier.height(10.dp))
            JField(d.height, { d.height = it }, "Altezza", number = true, suffix = "cm")
        }
        QStep.Body -> {
            StepHeader(step.title, "Il peso è la base del calcolo. La massa grassa, se la conosci, lo rende molto più accurato.")
            if (d.isNew) {
                JField(d.weight, { d.weight = it }, "Peso attuale", number = true, suffix = "kg")
                Spacer(Modifier.height(10.dp))
            } else Hint("Il peso attuale si aggiorna dalla sezione Peso: qui uso l'ultima pesata (${d.weightKg().r1()} kg).")
            JField(d.target, { d.target = it }, "Peso obiettivo", number = true, suffix = "kg")
            Spacer(Modifier.height(10.dp))
            JField(d.bodyFat, { d.bodyFat = it }, "Massa grassa (facoltativo)", number = true, suffix = "%")
            Hint("La trovi su una bilancia impedenziometrica o dal nutrizionista. Se la inserisci uso la formula di Katch-McArdle, che tiene conto della massa magra. " +
                    "Lascia vuoto se non la conosci: va benissimo.")
        }
        QStep.Job -> {
            StepHeader(step.title, "Il lavoro pesa sul dispendio più di quanto si pensi. Pensa a una giornata tipica, senza contare gli allenamenti.")
            Nutrition.JOB_LABELS.forEachIndexed { i, l -> Choice(l, d.job == i) { d.job = i } }
        }
        QStep.Steps -> {
            StepHeader(step.title, "Passi al giorno in media, tra casa, lavoro e commissioni. Se hai già uno smartwatch, guarda la media settimanale.")
            Nutrition.STEPS_LABELS.forEachIndexed { i, l -> Choice(l, d.steps == i) { d.steps = i } }
            Hint("Con lo smartwatch collegato questa stima viene affinata con i tuoi dati reali dopo qualche giorno.")
        }
        QStep.Training -> {
            StepHeader(step.title, "Gli allenamenti strutturati, non la camminata quotidiana (già contata sopra).")
            Text("Quante volte a settimana?", style = T.Label)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..7).forEach { n -> Pill(if (n == 0) "Mai" else "$n", d.workouts == n) { d.workouts = n } }
            }
            if (d.workouts > 0) {
                Spacer(Modifier.height(16.dp))
                Text("Durata di una sessione: ${d.workoutMin} minuti", style = T.Label)
                Slider(
                    d.workoutMin.toFloat(), { d.workoutMin = ((it / 15).toInt() * 15).coerceIn(15, 150) }, valueRange = 15f..150f, steps = 8,
                    colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceHi)
                )
                Spacer(Modifier.height(8.dp))
                Text("Tipo di allenamento", style = T.Label)
                Nutrition.TRAINING_LABELS.forEachIndexed { i, l -> Choice(l, d.trainingType == i) { d.trainingType = i } }
                Spacer(Modifier.height(8.dp))
                Text("Intensità", style = T.Label)
                Nutrition.INTENSITY_LABELS.forEachIndexed { i, l -> Choice(l, d.intensity == i) { d.intensity = i } }
            }
        }
        QStep.Goal -> {
            StepHeader(step.title)
            Choice("Dimagrire", d.goal == -1) { d.goal = -1 }
            Choice("Mantenere il peso", d.goal == 0) { d.goal = 0 }
            Choice("Aumentare massa muscolare", d.goal == 1) { d.goal = 1 }
            if (d.goal != 0) {
                Spacer(Modifier.height(16.dp))
                Text("Ritmo: ${d.rate.r1()} kg a settimana", style = T.Title2)
                Slider(
                    d.rate.toFloat(), { d.rate = (Math.round(it * 4) / 4.0).coerceAtLeast(0.25) }, valueRange = 0.25f..1f, steps = 2,
                    colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceHi)
                )
                Text(
                    if (d.goal == -1) "Un ritmo di 0,25–0,5 kg a settimana è il più sostenibile. Il deficit non supera mai il 25% del fabbisogno."
                    else "Per aumentare massa con poco grasso, 0,25 kg a settimana è un buon punto di partenza.", style = T.Small
                )
            }
        }
        QStep.Diet -> {
            StepHeader(step.title, "Come preferisci mangiare e se c'è qualcosa di cui tenere conto.")
            Text("Ripartizione dei macronutrienti", style = T.Label)
            listOf(
                "Bilanciato" to "Carboidrati, proteine e grassi in equilibrio: il più facile da seguire.",
                "Più proteine" to "Utile per sazietà e per proteggere i muscoli, soprattutto in deficit.",
                "Pochi carboidrati" to "Se preferisci mangiare così. A parità di calorie e proteine non è più efficace."
            ).forEachIndexed { i, (t, desc) -> Choice("$t · $desc", d.dietStyle == i) { d.dietStyle = i } }
            Spacer(Modifier.height(10.dp))
            Text("Situazioni particolari", style = T.Label)
            listOf(
                "Nessuna",
                "Gravidanza o allattamento (nessun deficit calorico)",
                "Patologia o terapia da seguire con il medico"
            ).forEachIndexed { i, l -> Choice(l, d.special == i) { d.special = i } }
            if (d.special != 0) Hint("Jarvis Food dà solo stime generali: per la tua situazione il riferimento è il medico o il nutrizionista.")
        }
        QStep.Plan -> PlanPreview(d)
    }
}

@Composable
fun PlanPreview(d: QDraft) {
    val p = d.toProfile(Profile(name = d.name))
    val w = d.weightKg()
    val bmr = Nutrition.bmr(p, w)
    val tdee = Nutrition.tdee(p, w)
    val g = Nutrition.autoGoals(p, w, Goals())
    val (c, pr, f) = Nutrition.goalGrams(g)
    val method = if (p.bodyFatPct != null) "Katch-McArdle" else "Mifflin-St Jeor"
    Text("Il tuo piano", style = T.Display)
    Spacer(Modifier.height(16.dp))
    JCard(brush = C.NavyBrush) {
        Text("Obiettivo giornaliero", style = T.Muted.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)))
        Text("${g.kcal} kcal", style = T.Big.copy(color = androidx.compose.ui.graphics.Color.White))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Stat("Carboidrati", "${c.r0()} g", androidx.compose.ui.graphics.Color(0xFF8FB0F0), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
            Stat("Proteine", "${pr.r0()} g", C.Protein, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
            Stat("Grassi", "${f.r0()} g", C.Fat, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Acqua: ${g.waterMl} ml al giorno", style = T.Label.copy(color = androidx.compose.ui.graphics.Color.White))
    }
    Spacer(Modifier.height(12.dp))
    JCard {
        Text("Come ci siamo arrivati", style = T.Title2)
        Spacer(Modifier.height(6.dp))
        Text("• Metabolismo basale: ${bmr.r0()} kcal ($method)", style = T.Body)
        Text("• Movimento quotidiano: +${(bmr * (Nutrition.neatFactor(p) - 1)).r0()} kcal", style = T.Body)
        if (p.workouts > 0) Text("• Allenamenti (media al giorno): +${Nutrition.exerciseKcalPerDay(p, w).r0()} kcal", style = T.Body)
        Text("• Fabbisogno di mantenimento: ${tdee.r0()} kcal", style = T.Label)
        val delta = Nutrition.safeDelta(p, tdee)
        if (delta != 0.0) Text("• Obiettivo peso: ${if (delta > 0) "+" else ""}${delta.r0()} kcal al giorno", style = T.Body)
        if (p.special == 1) Text("• Gravidanza o allattamento: nessun deficit calorico.", style = T.Body)
    }
    Spacer(Modifier.height(10.dp))
    Text("Sono stime: con lo smartwatch collegato si affinano sui tuoi giorni reali, e puoi cambiare ogni risposta dal Profilo. " +
            "Non sostituiscono il parere di un medico o di un nutrizionista.", style = T.Small)
}

// ---------------------------------------------------------------- Flusso guidato

/** Questionario a passi, usato alla prima apertura e dal Profilo ("Rifai il questionario"). */
@Composable
fun QuestionnaireFlow(d: QDraft, steps: List<QStep>, finishLabel: String, onBack: (() -> Unit)?, onFinish: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    val step = steps[i]
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(20.dp)) {
        ProgressLine((i + 1f) / steps.size, C.Accent, Modifier.padding(top = 8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp)) { QContent(step, d) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (i > 0) GhostButton("Indietro", Modifier.weight(1f)) { i-- }
            else if (onBack != null) GhostButton("Annulla", Modifier.weight(1f)) { onBack() }
            PrimaryButton(if (i == steps.lastIndex) finishLabel else "Avanti", Modifier.weight(2f), enabled = d.valid(step)) {
                if (i < steps.lastIndex) i++ else onFinish()
            }
        }
    }
}

/** Applica le risposte: aggiorna il profilo e, se il calcolo è automatico, ricalcola calorie e macro. */
fun applyDraft(d: QDraft) {
    Store.update { st ->
        val np = d.toProfile(st.profile)
        val weights = if (d.isNew) listOf(WeightEntry(LocalDate.now().toString(), np.startWeight)) else st.weights
        val w = if (d.isNew) np.startWeight else Store.currentWeight(st)
        st.copy(
            onboarded = true, profile = np, weights = weights,
            goals = if (d.isNew || st.goals.auto) Nutrition.autoGoals(np, w, st.goals) else st.goals
        )
    }
    Store.saveNow()
}

/** Schermata "Rifai il questionario" dal Profilo. */
@Composable
fun QuestionnaireScreen(only: QStep?) {
    val s = Store.current
    val d = remember { QDraft(s.profile, false, Store.currentWeight(s)) }
    val steps = remember {
        if (only == null) listOf(QStep.About, QStep.Body, QStep.Job, QStep.Steps, QStep.Training, QStep.Goal, QStep.Diet, QStep.Plan)
        else listOf(only, QStep.Plan)
    }
    QuestionnaireFlow(d, steps, "Salva e ricalcola", onBack = { Nav.back() }) { applyDraft(d); Nav.back() }
}
