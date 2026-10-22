package com.jarvis.food.ui

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.jarvis.food.data.DietPrefs
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
    Text(title, style = T.H1.copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp), modifier = Modifier.padding(top = 8.dp))
    if (why != null) Text(why, style = T.Body.copy(fontSize = 15.sp, lineHeight = 22.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 8.dp))
    GapH(20.dp)
}

@Composable
private fun Hint(text: String) { Text(text, style = T.Small.copy(fontSize = 12.5.sp, lineHeight = 18.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp)) }

@Composable
private fun QLabel(text: String) { Text(text, style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) }

/** Bottone tondo del design (−/+): tenendo premuto ripete, con accelerazione. */
@Composable
private fun HoldStep(plus: Boolean, onStep: () -> Unit) {
    val scope = rememberCoroutineScope()
    val cb = rememberUpdatedState(onStep)
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFE9E1)).pointerInput(Unit) {
            detectTapGestures(onPress = {
                cb.value()
                val job = scope.launch { delay(420); var n = 0; while (true) { cb.value(); n++; delay(if (n < 8) 130L else 55L) } }
                tryAwaitRelease(); job.cancel()
            })
        },
        contentAlignment = Alignment.Center
    ) { AIcon(if (plus) "M12 6v12M6 12h12" else "M6 12h12", Color(0xFFB4381E), 20.dp, 2.8f) }
}

/** Riga con valore grande e due pulsanti −/+ (Età, Altezza, Peso del design). */
@Composable
private fun StepRow(label: String, value: String, unit: String, divider: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = T.H1.copy(fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp))
                if (unit.isNotEmpty()) Text(" $unit", style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HoldStep(false, onMinus); HoldStep(true, onPlus) }
    }
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
}

@Composable
private fun QCard(content: @Composable () -> Unit) {
    ACard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 0.dp, elevation = 8.dp) { Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { content() } }
}

/** Scheda grande con icona tonale (sesso, obiettivo). */
@Composable
private fun BigChoice(selected: Boolean, icon: String, tint: Color, ink: Color, title: String, sub: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AChoice(selected, modifier.fillMaxWidth(), minHeight = if (sub == null) 76.dp else 84.dp, radius = if (sub == null) 26.dp else 28.dp, onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TonalIcon(icon, ink, tint, box = if (sub == null) 40.dp else 52.dp, radius = if (sub == null) 15.dp else 19.dp, size = if (sub == null) 22.dp else 26.dp)
            GapW(if (sub == null) 10.dp else 14.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = T.Body.copy(fontSize = if (sub == null) 16.sp else 16.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                if (sub != null) Text(sub, style = T.Small.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun PhotoPicker(d: QDraft) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ProfilePhoto.save(ctx, uri)?.let { d.photo = it }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable { launcher.launch("image/*") }) { Avatar(d.name, d.photo, 72.dp, shape = RoundedCornerShape(26.dp)) }
        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (d.photo == null) "Aggiungi foto" else "Cambia foto", style = T.Link, modifier = Modifier.clickable { launcher.launch("image/*") }.padding(vertical = 8.dp))
            if (d.photo != null) Text("Rimuovi", style = T.Label.copy(color = C.Danger),
                modifier = Modifier.clickable { ProfilePhoto.delete(d.photo); d.photo = null }.padding(vertical = 6.dp))
        }
    }
}

private val SEX_ICONS = listOf("M10 14a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM14 10l6-6M15 4h5v5", "M12 14a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM12 14v7M9 18h6")

@Composable
fun QContent(step: QStep, d: QDraft) {
    val ctx = LocalContext.current
    when (step) {
        QStep.Welcome -> {
            val heroShape = RoundedCornerShape(44.dp)
            Box(Modifier.fillMaxWidth().height(300.dp).aShadow(heroShape, 16.dp, Color(0xB3E88C60)).clip(heroShape)) {
                RecipeImage("welcome", "recipes/pancake_avena.webp", null, Modifier.fillMaxSize(), "🥗", 0.dp)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x731D1B3A))))
                WelcomeChip("Proteine", Color(0xFFF43F6E), Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 18.dp))
                WelcomeChip("Carboidrati", Color(0xFF3B82F6), Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 70.dp))
                WelcomeChip("Grassi", Color(0xFFF59E0B), Modifier.align(Alignment.BottomStart).padding(start = 34.dp, bottom = 22.dp))
            }
            Text("Benvenuto in", style = T.H1.copy(fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1.1).sp), modifier = Modifier.padding(top = 26.dp))
            Text("Jarvis Food", style = T.H1.copy(fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1.1).sp,
                brush = Brush.horizontalGradient(listOf(Color(0xFF34457A), Color(0xFF6B5A8E), Color(0xFFD2793F)))))
            Text("Il tuo diario alimentare, il piano dei pasti e le ricette in un posto solo. Rispondo ad alcune domande per calcolare le tue calorie in modo preciso e onesto: ti servono due minuti.",
                style = T.Body.copy(fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AIcon(PIc2.Shield, Color(0xFF047857), 18.dp, 2.2f)
                GapW(10.dp)
                Text("Nessun account: i dati restano sul tuo telefono.", style = T.Body.copy(fontSize = 13.sp, color = Color(0xFF4B4A6A)))
            }
            ACard(Modifier.padding(top = 18.dp).fillMaxWidth(), radius = 28.dp, padding = 16.dp, elevation = 8.dp) {
                Column {
                    PhotoPicker(d)
                    Spacer(Modifier.height(12.dp))
                    JField(d.name, { d.name = it }, "Come ti chiami?")
                }
            }
        }
        QStep.About -> {
            StepHeader(step.title, "Sesso, età e altezza determinano il metabolismo basale: le calorie che bruci a riposo.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigChoice(d.male, SEX_ICONS[0], A.CarbSoft, A.CarbInk, "Uomo", null, Modifier.weight(1f)) { d.male = true }
                BigChoice(!d.male, SEX_ICONS[1], A.ProteinSoft, A.ProteinInk, "Donna", null, Modifier.weight(1f)) { d.male = false }
            }
            Spacer(Modifier.height(14.dp))
            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)
            QCard {
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Data di nascita", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                        Text(d.birth?.format(fmt) ?: "Scegli la data", style = T.H1.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = if (d.birth == null) A.Muted else A.Ink))
                    }
                    AInk("Cambia", height = 44.dp, bg = Color(0xFFFFE9E1), fg = Color(0xFFB4381E)) {
                        val b = d.birth ?: LocalDate.of(1995, 6, 15)
                        DatePickerDialog(ctx, { _, y, m, day -> d.birth = LocalDate.of(y, m + 1, day) }, b.year, b.monthValue - 1, b.dayOfMonth).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                val h = parseNum(d.height)
                StepRow("Altezza", h?.r0() ?: "—", "cm", false,
                    { d.height = ((h ?: 171.0) - 1).coerceIn(120.0, 230.0).r0() }, { d.height = ((h ?: 169.0) + 1).coerceIn(120.0, 230.0).r0() })
            }
        }
        QStep.Body -> {
            StepHeader(step.title, "Il peso è la base del calcolo. La massa grassa, se la conosci, lo rende molto più accurato.")
            QCard {
                if (d.isNew) {
                    val w = parseNum(d.weight)
                    StepRow("Peso attuale", w?.r1() ?: "—", "kg", true,
                        { d.weight = (((w ?: 70.1) - 0.1) * 10).roundToInt().div(10.0).coerceIn(30.0, 300.0).r1() },
                        { d.weight = (((w ?: 69.9) + 0.1) * 10).roundToInt().div(10.0).coerceIn(30.0, 300.0).r1() })
                }
                val t = parseNum(d.target)
                val base = parseNum(d.weight) ?: 70.0
                StepRow("Peso obiettivo", t?.r1() ?: "—", "kg", false,
                    { d.target = (((t ?: (base + 0.1)) - 0.1) * 10).roundToInt().div(10.0).coerceIn(30.0, 300.0).r1() },
                    { d.target = (((t ?: (base - 0.1)) + 0.1) * 10).roundToInt().div(10.0).coerceIn(30.0, 300.0).r1() })
            }
            if (!d.isNew) Hint("Il peso attuale si aggiorna dalla sezione Peso: qui uso l'ultima pesata (${d.weightKg().r1()} kg).")
            else Hint("Tieni premuto − o + per correre veloce.")
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 28.dp, padding = 16.dp, elevation = 8.dp) {
                Column {
                    JField(d.bodyFat, { d.bodyFat = it }, "Massa grassa (facoltativo)", number = true, suffix = "%")
                    Hint("La trovi su una bilancia impedenziometrica o dal nutrizionista. Se la inserisci uso la formula di Katch-McArdle, che tiene conto della massa magra. " +
                            "Lascia vuoto se non la conosci: va benissimo.")
                }
            }
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
            Text("Quante volte a settimana?", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..7).forEach { n -> APill(if (n == 0) "Mai" else "$n", d.workouts == n, Modifier.widthIn(min = 52.dp)) { d.workouts = n } }
            }
            if (d.workouts > 0) {
                Spacer(Modifier.height(16.dp))
                QCard {
                    StepRow("Durata di una sessione", "${d.workoutMin}", "minuti", false,
                        { d.workoutMin = (d.workoutMin - 15).coerceIn(15, 150) }, { d.workoutMin = (d.workoutMin + 15).coerceIn(15, 150) })
                }
                QLabel("Tipo di allenamento")
                Nutrition.TRAINING_LABELS.forEachIndexed { i, l -> Choice(l, d.trainingType == i) { d.trainingType = i } }
                QLabel("Intensità")
                Nutrition.INTENSITY_LABELS.forEachIndexed { i, l -> Choice(l, d.intensity == i) { d.intensity = i } }
            }
        }
        QStep.Goal -> {
            StepHeader("Qual è il tuo obiettivo?", "Potrai cambiarlo quando vuoi dal profilo.")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigChoice(d.goal == -1, "M4 7l6 6 4-4 6 6M20 15v-4M20 15h-4", A.GreenSoft, A.GreenInk, "Dimagrire", "Un deficit calorico graduale") { d.goal = -1 }
                BigChoice(d.goal == 0, "M4 12h16M7 8l-3 4 3 4M17 8l3 4-3 4", A.CarbSoft, A.CarbInk, "Mantenere il peso", "Equilibrio tra quello che mangi e consumi") { d.goal = 0 }
                BigChoice(d.goal == 1, "M3 10v4M6 8v8M18 8v8M21 10v4M6 12h12", A.FastSoft, A.FastInk, "Aumentare massa muscolare", "Un piccolo surplus con più proteine") { d.goal = 1 }
            }
            if (d.goal != 0) {
                QLabel("Ritmo a settimana")
                val rates = listOf(0.25, 0.5, 0.75, 1.0)
                val sign = if (d.goal == 1) "+" else "−"
                ASegment(rates.map { "$sign${java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale.ITALY)).format(it)}" }, rates.indexOfFirst { kotlin.math.abs(it - d.rate) < 0.01 }) { d.rate = rates[it] }
                Hint(if (d.goal == -1) "Un ritmo di 0,25–0,5 kg a settimana è il più sostenibile. Il deficit non supera mai il 25% del fabbisogno."
                else "Per aumentare massa con poco grasso, 0,25 kg a settimana è un buon punto di partenza.")
            }
        }
        QStep.Diet -> {
            StepHeader(step.title, "Come preferisci mangiare e se c'è qualcosa di cui tenere conto.")
            Text("Ripartizione dei macronutrienti", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.padding(bottom = 4.dp))
            listOf(
                "Bilanciato" to "Carboidrati, proteine e grassi in equilibrio: il più facile da seguire.",
                "Più proteine" to "Utile per sazietà e per proteggere i muscoli, soprattutto in deficit.",
                "Pochi carboidrati" to "Se preferisci mangiare così. A parità di calorie e proteine non è più efficace."
            ).forEachIndexed { i, (t, desc) -> Choice("$t · $desc", d.dietStyle == i) { d.dietStyle = i } }
            QLabel("Situazioni particolari")
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

private object PIc2 {
    const val Shield = "M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6zM9 12l2 2 4-4"
}

@Composable
private fun WelcomeChip(text: String, dot: Color, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xE6FFFFFF)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        GapW(8.dp)
        Text(text, style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
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
    val shape34 = RoundedCornerShape(34.dp)
    val shape30 = RoundedCornerShape(30.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF10B981)), contentAlignment = Alignment.Center) { AIcon(Ic.Check, Color.White, 24.dp, 3f) }
        GapW(12.dp)
        Text("CALCOLO COMPLETATO", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), letterSpacing = 0.5.sp))
    }
    Text("Il tuo piano\nè pronto", style = T.H1.copy(fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1.1).sp), modifier = Modifier.padding(top = 12.dp))

    Row(
        Modifier.padding(top = 18.dp).fillMaxWidth().aShadow(shape34, 14.dp, Color(0x99E88C60)).clip(shape34)
            .background(Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDF1F5)))).border(1.dp, Color(0xE6FFFFFF), shape34).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 20.dp.toPx()
                val sz = Size(size.width - sw, size.height - sw)
                val off = Offset(sw / 2, sw / 2)
                val tot = (g.proteinPct + g.carbsPct + g.fatPct).coerceAtLeast(1).toFloat()
                var start = -90f
                listOf(g.proteinPct to Color(0xFFF43F6E), g.carbsPct to Color(0xFF3B82F6), g.fatPct to Color(0xFFF59E0B)).forEach { (pct, col) ->
                    val sweep = 360f * pct / tot
                    drawArc(col, start + 4f, (sweep - 8f).coerceAtLeast(1f), false, off, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(kcalFmt(g.kcal.toDouble()), style = T.H1.copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp), maxLines = 1)
                Text("kcal al giorno", style = T.Body.copy(fontSize = 12.5.sp, color = A.Muted))
            }
        }
        GapW(18.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(Triple(pr, "Proteine · ${g.proteinPct}%", Color(0xFFF43F6E)), Triple(c, "Carboidrati · ${g.carbsPct}%", Color(0xFF3B82F6)), Triple(f, "Grassi · ${g.fatPct}%", Color(0xFFF59E0B))).forEach { (gr, lab, col) ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(col))
                    GapW(10.dp)
                    Column {
                        Text("${gr.r0()} g", style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold))
                        Text(lab, style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                    }
                }
            }
        }
    }

    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).aShadow(shape30, 10.dp, Color(0x803B82F6)).clip(shape30).background(Brush.linearGradient(listOf(Color(0xFFDDF6FB), Color(0xFFE6EEFF)))).padding(16.dp)) {
            TonalIcon(Ic.Drop, Color(0xFF2563EB), Color.White, box = 40.dp, radius = 15.dp, size = 21.dp)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text(g.waterMl.thousands(), style = T.H1.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp), maxLines = 1)
                Text(" ml", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 3.dp))
            }
            Text("Acqua al giorno", style = T.Body.copy(fontSize = 12.5.sp, color = Color(0xFF4B4A6A)))
        }
        val delta = Nutrition.safeDelta(p, tdee)
        val pace = when { p.special == 1 || p.goal == 0 -> "stabile"; p.goal == -1 -> "−${p.weeklyRate.toString().replace('.', ',').removeSuffix(",0")}"; else -> "+${p.weeklyRate.toString().replace('.', ',').removeSuffix(",0")}" }
        Column(Modifier.weight(1f).aShadow(shape30, 10.dp, Color(0x80F59E0B)).clip(shape30).background(Brush.linearGradient(listOf(Color(0xFFE4F6EE), Color(0xFFFFF7E0)))).padding(16.dp)) {
            TonalIcon("M4 7l6 6 4-4 6 6M20 15v-4M20 15h-4", Color(0xFF047857), Color.White, box = 40.dp, radius = 15.dp, size = 21.dp)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text(pace, style = T.H1.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp), maxLines = 1)
                if (pace != "stabile") Text(" kg/sett", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(if (delta == 0.0) "Ritmo previsto" else "Ritmo previsto · ${if (delta > 0) "+" else "−"}${kotlin.math.abs(delta).r0()} kcal", style = T.Body.copy(fontSize = 12.5.sp, color = Color(0xFF4B4A6A)))
        }
    }

    ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 16.dp, elevation = 8.dp) {
        Column {
            Text("Come si distribuisce nella giornata", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
            val slots = DietPrefs().slots
            val cols = listOf(Color(0xFFFFB820), Color(0xFFFF6B4A), Color(0xFF8B5CF6), Color(0xFF3B82F6))
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                slots.forEachIndexed { i, sl -> Box(Modifier.weight(sl.share.toFloat()).height(12.dp).clip(RoundedCornerShape(6.dp)).background(cols[i % 4])) }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                slots.forEach { sl ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(kcalFmt(g.kcal * sl.share / 100.0), style = T.H1.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold))
                        Text(sl.name, style = T.Small.copy(fontSize = 11.5.sp, color = A.Muted), maxLines = 1)
                    }
                }
            }
        }
    }

    ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 16.dp, elevation = 8.dp) {
        Column {
            Text("Come ci siamo arrivati", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
            val line = T.Body.copy(fontSize = 13.5.sp, lineHeight = 20.sp, color = Color(0xFF4B4A6A))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("• Metabolismo basale: ${bmr.r0()} kcal ($method)", style = line)
                Text("• Movimento quotidiano: +${(bmr * (Nutrition.neatFactor(p) - 1)).r0()} kcal", style = line)
                if (p.workouts > 0) Text("• Allenamenti (media al giorno): +${Nutrition.exerciseKcalPerDay(p, w).r0()} kcal", style = line)
                Text("• Fabbisogno di mantenimento: ${tdee.r0()} kcal", style = line.copy(fontWeight = FontWeight.SemiBold, color = A.Ink))
                val delta = Nutrition.safeDelta(p, tdee)
                if (delta != 0.0) Text("• Obiettivo peso: ${if (delta > 0) "+" else ""}${delta.r0()} kcal al giorno", style = line)
                if (p.special == 1) Text("• Gravidanza o allattamento: nessun deficit calorico.", style = line)
            }
        }
    }
    Text("Sono stime: con lo smartwatch collegato si affinano sui tuoi giorni reali, e puoi cambiare ogni risposta dal Profilo › Obiettivi. " +
            "Non sostituiscono il parere di un medico o di un nutrizionista.", style = T.Small.copy(fontSize = 12.sp, lineHeight = 18.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
}

// ---------------------------------------------------------------- Flusso guidato

/** Questionario a passi, usato alla prima apertura e dal Profilo ("Rifai il questionario"). */
@Composable
fun QuestionnaireFlow(d: QDraft, steps: List<QStep>, finishLabel: String, onBack: (() -> Unit)?, onFinish: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    val step = steps[i]
    val last = i == steps.lastIndex
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.alpha(if (i == 0 && onBack == null) 0.35f else 1f)) {
                BackCircle { if (i > 0) i-- else onBack?.invoke() }
            }
            GapW(14.dp)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.indices.forEach { k ->
                    Box(
                        Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (k <= i) Brush.horizontalGradient(listOf(Color(0xFFF28B7A), Color(0xFFF8C089))) else SolidColor(Color(0xFFDDE3EA)))
                    )
                }
            }
            GapW(14.dp)
            Text("${i + 1}/${steps.size}", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.End)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)) { QContent(step, d) }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp)) {
            APrimary(
                when { step == QStep.Welcome -> "Iniziamo"; last -> finishLabel; else -> "Continua" },
                Modifier.fillMaxWidth(), enabled = d.valid(step)
            ) { if (!last) i++ else onFinish() }
            if (last && i > 0) Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { i-- }, contentAlignment = Alignment.Center) {
                Text("Modifica le risposte", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B4A6A)))
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
