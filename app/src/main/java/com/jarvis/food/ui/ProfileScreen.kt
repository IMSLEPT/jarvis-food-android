package com.jarvis.food.ui

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.food.BuildConfig
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.data.AiConfig
import com.jarvis.food.engine.Activities
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.notify.Reminders
import java.time.LocalDate

private object PIc {
    val Target = Ic.circ(12.0, 12.0, 9.0) + Ic.circ(12.0, 12.0, 4.5) + Ic.circ(12.0, 12.0, 0.8)
    val Scale = "M5 5h14l1 14H4zM9 9a3 3 0 0 0 6 0"
    val Ruler = "M3 15L15 3l6 6L9 21zM8 10l2 2M11 7l2 2M5 13l2 2"
    val Leaf = "M5 19c0-9 5-14 15-14 0 10-5 15-14 15zM5 19l8-8"
    val Watch = Ic.rr(7.0, 6.0, 10.0, 12.0, 3.0) + "M9 6l1-3h4l1 3M9 18l1 3h4l1-3M12 10v2l1.5 1"
    val Sparks = "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"
    val Fast = Ic.circ(12.0, 13.0, 7.5) + "M12 9v4l2.5 1.5M9.5 3h5"
    val Bell = "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15zM10 20a2 2 0 0 0 4 0"
    val Cook = "M4 11h16a8 8 0 0 1-16 0zM8 7l1.5-3M13 7l1.5-3"
    val Save = "M5 4h11l3 3v13H5zM8 4v5h7V4M8 20v-6h8v6"
    val Info = Ic.circ(12.0, 12.0, 9.0) + "M12 11v5M12 8h.01"
    val Chat = "M4 5h16v11H9l-5 4z"
    val Shield = "M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6zM9 12l2 2 4-4"
}

@Composable
private fun ProfRow(icon: String, bg: Color, ink: Color, title: String, sub: String?, divider: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        TonalIcon(icon, ink, bg, box = 46.dp, radius = 17.dp, size = 23.dp)
        GapW(14.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
            if (sub != null) Text(sub, style = T.Small.copy(fontSize = 12.5.sp, lineHeight = 17.sp, color = A.Muted), maxLines = 1)
        }
        AIcon(Ic.Right, Color(0xFF8A87A3), 18.dp, 2.4f)
    }
    if (divider) Box(Modifier.padding(start = 60.dp).fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
}

@Composable
private fun ProfStat(value: String, label: String, modifier: Modifier, big: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xD1FFFFFF)).padding(horizontal = if (big) 8.dp else 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.H1.copy(fontSize = if (big) 19.sp else 17.sp, lineHeight = 22.sp, letterSpacing = (-0.3).sp), maxLines = 1)
        Text(label, style = T.Small.copy(fontSize = 11.5.sp, color = A.Muted), maxLines = 1)
    }
}

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val w = Store.currentWeight(s)
    var confirmReset by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportJson().toByteArray()) }
        }.onSuccess { Toast.makeText(ctx, "Backup salvato", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "Errore: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    val kcal = Store.energyFor(s, LocalDate.now()).baseKcal
    val shape34 = RoundedCornerShape(34.dp)
    val shape30 = RoundedCornerShape(30.dp)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp)) {
        Text("Profilo", style = T.H1)

        // Scheda utente
        Column(
            Modifier.padding(top = 16.dp).fillMaxWidth().aShadow(shape34, 14.dp, Color(0x99E88C60)).clip(shape34)
                .background(Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDF1F5)))).border(1.dp, Color(0xE6FFFFFF), shape34).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.profile.name, s.profile.photo, 72.dp, Modifier.aShadow(RoundedCornerShape(26.dp), 8.dp, Color(0xB3E88C60)).clickable { Nav.go(Screen.ProfileEdit) }, RoundedCornerShape(26.dp))
                GapW(16.dp)
                Column(Modifier.weight(1f)) {
                    Text(s.profile.name, style = T.H1.copy(fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp), maxLines = 1)
                    Text("${Nutrition.age(s.profile)} anni · ${w.r1()} kg", style = T.Body.copy(fontSize = 13.5.sp, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
                }
            }
            GapH(16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfStat(when (s.profile.goal) { -1 -> "Dimagrire"; 1 -> "Massa"; else -> "Mantenere" }, "Piano", Modifier.weight(1f))
                ProfStat(kcalFmt(kcal), "kcal al giorno", Modifier.weight(1f))
                ProfStat("${s.profile.targetWeight.r1().removeSuffix(",0")} kg", "Obiettivo", Modifier.weight(1f))
            }
        }

        Text("Il mio piano", style = T.H2, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
        ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 0.dp, elevation = 8.dp) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                ProfRow(Ic.User, A.Tonal, A.TonalInk, "Profilo e obiettivo", "Foto, dati personali, obiettivo, ritmo") { Nav.go(Screen.ProfileEdit) }
                ProfRow(PIc.Target, A.FatSoft, A.FatInk, "Calorie e macronutrienti", "${s.goals.carbsPct}% C · ${s.goals.proteinPct}% P · ${s.goals.fatPct}% G") { Nav.go(Screen.Goals) }
                ProfRow(PIc.Scale, A.CarbSoft, A.CarbInk, "Peso", "${w.r1()} kg") { Nav.go(Screen.Weight) }
                ProfRow(PIc.Ruler, A.GreenSoft, A.GreenInk, "Misure corporee", "Vita, fianchi, petto, braccia, cosce") { Nav.go(Screen.Measurements) }
                ProfRow(PIc.Leaf, A.FastSoft, A.FastInk, "Preferenze della dieta", "Esclusioni, ripartizione dei pasti, tempi", divider = false) { Nav.go(Screen.DietSettings) }
            }
        }

        Text("Strumenti", style = T.H2, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
        ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 0.dp, elevation = 8.dp) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                ProfRow(PIc.Chat, A.Tonal, A.TonalInk, "Chat con Jarvis", "Fai domande e dagli comandi · ${if (s.ai.chatEngine == 0) "sul telefono" else "cloud"}") { Nav.go(Screen.Chat) }
                ProfRow(PIc.Watch, A.GreenSoft, A.GreenInk, "Smartwatch e attività",
                    when (s.energy.mode) { 2 -> "Amazfit / Health Connect collegato"; 1 -> "Attività inserite a mano"; else -> "Livello di attività fisso" }) { Nav.go(Screen.Watch) }
                ProfRow(PIc.Sparks, A.FatSoft, A.FatInk, "Intelligenza artificiale", if (s.ai.apiKey.isBlank()) "Da configurare (gratuita)" else "Attiva · ${s.ai.model}") { Nav.go(Screen.AiSettings) }
                ProfRow(PIc.Fast, A.FastSoft, A.FastInk, "Digiuno intermittente", if (s.fasting.activeStart != null) "In corso · ${s.fasting.planId}" else "Piano ${s.fasting.planId}") { Nav.go(Screen.Fasting) }
                ProfRow(PIc.Bell, A.ProteinSoft, A.ProteinInk, "Promemoria", "Pasti, acqua, pesata, digiuno") { Nav.go(Screen.RemindersSet) }
                ProfRow(PIc.Cook, A.Tonal, A.TonalInk, "I miei alimenti", "${s.customFoods.size} alimenti · ${s.favorites.size} preferiti") { Nav.go(Screen.MyFoods) }
                ProfRow(PIc.Save, A.CarbSoft, A.CarbInk, "Backup e dati", "Esporta, importa, CSV") { Nav.go(Screen.Backup) }
                ProfRow(PIc.Info, Color(0xFFEEEAF5), Color(0xFF4B4570), "Informazioni", "Versione ${BuildConfig.VERSION_NAME}", divider = false) { Nav.go(Screen.About) }
            }
        }

        // Privacy
        Column(
            Modifier.padding(top = 20.dp).fillMaxWidth().aShadow(shape30, 12.dp, Color(0x9910B981)).clip(shape30)
                .background(Brush.linearGradient(listOf(Color(0xFFE4F6EE), Color(0xFFE7EFFA)))).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                TonalIcon(PIc.Shield, A.GreenInk, Color.White, box = 46.dp, radius = 17.dp, size = 23.dp)
                GapW(14.dp)
                Column {
                    Text("I tuoi dati restano sul telefono", style = T.H1.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp))
                    Text("Nessun account, nessun server. Esporta un backup ogni tanto e salvalo su Google Drive: puoi sceglierlo direttamente nella finestra di salvataggio.",
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF3F5A52)), modifier = Modifier.padding(top = 4.dp))
                }
            }
            AInk("Esporta backup", Modifier.padding(top = 14.dp).fillMaxWidth(), height = 50.dp) { export.launch("jarvis-food-backup-${LocalDate.now()}.json") }
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(min = 48.dp).clickable { confirmReset = true }, contentAlignment = Alignment.Center) {
            Text("Cancella tutti i dati", style = T.Body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB42318)))
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, containerColor = C.Surface,
        title = { Text("Cancellare tutto?", style = T.Title) },
        text = { Text("Diario, peso, ricette e impostazioni verranno eliminati. L'operazione non si può annullare: esporta prima un backup.", style = T.Muted) },
        confirmButton = { TextButton(onClick = { Store.resetAll(); confirmReset = false; Nav.clear(); Nav.tab = 0 }) { Text("Cancella", color = C.Danger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annulla", color = C.Muted) } }
    )
}

@Composable
fun ProfileEditScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val p = s.profile
    var name by remember { mutableStateOf(p.name) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ProfilePhoto.save(ctx, uri)?.let { path -> Store.update { it.copy(profile = it.profile.copy(photo = path)) } }
    }
    val age = Nutrition.age(p)
    val w = Store.currentWeight(s)

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Profilo e obiettivi", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clickable { picker.launch("image/*") }) { Avatar(p.name, p.photo, 84.dp) }
                    Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GhostButton(if (p.photo == null) "Aggiungi foto" else "Cambia foto") { picker.launch("image/*") }
                        if (p.photo != null) Text("Rimuovi", style = T.Label.copy(color = C.Danger),
                            modifier = Modifier.clickable { ProfilePhoto.delete(p.photo); Store.update { it.copy(profile = it.profile.copy(photo = null)) } }.padding(6.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                JField(name, { name = it }, "Nome")
            }
            if (!p.detailed) JCard(color = C.AccentSoft, onClick = { Nav.go(Screen.Questionnaire(null)) }) {
                Text("Rendi il calcolo più preciso", style = T.Title2)
                Text("Rispondi alle domande su lavoro, passi e allenamenti: il fabbisogno smette di essere una stima generica.", style = T.Muted)
            }
            SectionTitle("Le mie risposte")
            JCard(padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                NavRow("Dati personali", "${if (p.male) "Uomo" else "Donna"} · $age anni · ${p.heightCm.r0()} cm", "👤") { Nav.go(Screen.Questionnaire(QStep.About)) }
                HorizontalDivider(color = C.Line)
                NavRow("Peso e composizione", "Ora ${w.r1()} kg · obiettivo ${p.targetWeight.r1()} kg" +
                        (p.bodyFatPct?.let { " · grasso ${it.r0()}%" } ?: ""), "⚖️") { Nav.go(Screen.Questionnaire(QStep.Body)) }
                HorizontalDivider(color = C.Line)
                NavRow("Lavoro", Nutrition.JOB_LABELS[p.job.coerceIn(0, 2)], "💼") { Nav.go(Screen.Questionnaire(QStep.Job)) }
                HorizontalDivider(color = C.Line)
                NavRow("Passi al giorno", Nutrition.STEPS_LABELS[p.stepsLevel.coerceIn(0, 4)], "👟") { Nav.go(Screen.Questionnaire(QStep.Steps)) }
                HorizontalDivider(color = C.Line)
                NavRow("Allenamenti", if (p.workouts == 0) "Nessuno" else "${p.workouts} a settimana · ${p.workoutMin} min · ${Nutrition.TRAINING_LABELS[p.trainingType.coerceIn(0, 3)].lowercase()}", "🏋️") { Nav.go(Screen.Questionnaire(QStep.Training)) }
                HorizontalDivider(color = C.Line)
                NavRow("Obiettivo", when (p.goal) { -1 -> "Dimagrire · ${p.weeklyRate.r1()} kg a settimana"; 1 -> "Aumentare massa · ${p.weeklyRate.r1()} kg a settimana"; else -> "Mantenere il peso" }, "🎯") { Nav.go(Screen.Questionnaire(QStep.Goal)) }
                HorizontalDivider(color = C.Line)
                NavRow("Preferenze", when (p.dietStyle) { 1 -> "Più proteine"; 2 -> "Pochi carboidrati"; else -> "Bilanciato" } +
                        (if (p.special != 0) " · situazione particolare" else ""), "🥗") { Nav.go(Screen.Questionnaire(QStep.Diet)) }
            }
            PrimaryButton("Rifai tutto il questionario", Modifier.fillMaxWidth()) { Nav.go(Screen.Questionnaire(null)) }
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton("Salva nome", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = name.isNotBlank() && name.trim() != p.name, color = C.Navy) {
            Store.update { it.copy(profile = it.profile.copy(name = name.trim())) }
            Nav.back()
        }
    }
}

private val FAC = java.text.DecimalFormat("0.###", java.text.DecimalFormatSymbols(java.util.Locale.ITALY))
private fun fac(d: Double) = FAC.format(d)

private val ACT_NAMES = listOf("Sedentario", "Leggero", "Moderato", "Attivo", "Molto attivo")
private val ACT_DESC = listOf(
    "Lavoro d'ufficio, pochi movimenti", "1–3 allenamenti a settimana", "3–5 allenamenti a settimana",
    "6–7 allenamenti a settimana", "Lavoro fisico o doppi allenamenti"
)

private data class Pace(val name: String, val rate: String, val goal: Int, val kg: Double)

private val PACES = listOf(
    Pace("Aumentare massa", "+0,25 kg/sett", 1, 0.25),
    Pace("Mantenere il peso", "stabile", 0, 0.0),
    Pace("Dimagrire piano", "−0,25 kg/sett", -1, 0.25),
    Pace("Dimagrire deciso", "−0,5 kg/sett", -1, 0.5)
)

@Composable
fun GoalsScreen() {
    val s by Store.state.collectAsState()
    val g = s.goals
    val w = Store.currentWeight(s)
    var auto by remember { mutableStateOf(g.auto) }
    var kcalTxt by remember { mutableStateOf(g.kcal.toString()) }
    var act by remember { mutableIntStateOf(s.profile.activity.coerceIn(0, 4)) }
    var goal by remember { mutableIntStateOf(s.profile.goal) }
    var rate by remember { mutableDoubleStateOf(s.profile.weeklyRate) }
    var c by remember { mutableIntStateOf(g.carbsPct) }
    var p by remember { mutableIntStateOf(g.proteinPct) }
    var f by remember { mutableIntStateOf(g.fatPct) }
    var water by remember { mutableIntStateOf(g.waterMl) }
    var steps by remember { mutableIntStateOf(g.steps) }
    var glass by remember { mutableIntStateOf(s.glassMl) }

    val pr = s.profile.copy(activity = act, goal = goal, weeklyRate = rate)
    val recommended = Nutrition.autoGoals(pr, w, g)
    val k = if (auto) recommended.kcal.toDouble() else parseNum(kcalTxt) ?: 0.0
    val bmr = Nutrition.bmr(pr, w)
    val tdee = Nutrition.tdee(pr, w)
    val delta = Nutrition.safeDelta(pr, tdee)

    // Con il calcolo automatico, cambiando attività o ritmo i macro seguono la raccomandazione (non all'apertura).
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(act, goal, rate, auto) {
        if (!opened) opened = true
        else if (auto) { c = recommended.carbsPct; p = recommended.proteinPct; f = recommended.fatPct }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Obiettivi", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
            // Hero: come è calcolato
            val heroShape = RoundedCornerShape(34.dp)
            Column(
                Modifier.padding(top = 6.dp).fillMaxWidth().aShadow(heroShape, 14.dp, Color(0x99E88C60)).clip(heroShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDF1F5)))).border(1.dp, Color(0xE6FFFFFF), heroShape)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Text("IL TUO OBIETTIVO GIORNALIERO", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A4A2E), letterSpacing = 0.5.sp))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                    Text(kcalFmt(k), style = T.H1.copy(fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.7).sp))
                    Text("kcal", style = T.Body.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A4A2E)), modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                }
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xD1FFFFFF)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    val lab = TextStyle(fontFamily = T.Body.fontFamily, fontSize = 13.5.sp, color = Color(0xFF4B4A6A))
                    val num = T.Body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = A.Ink)
                    Row {
                        Text(buildAnnotatedString {
                            append("Metabolismo basale ")
                            withStyle(SpanStyle(color = Color(0xFF6B6885))) { append(if (pr.bodyFatPct != null) "(Katch-McArdle)" else "(Mifflin-St Jeor)") }
                        }, style = lab, modifier = Modifier.weight(1f))
                        Text(kcalFmt(bmr), style = num)
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        Text("× attività ${fac(if (bmr > 0) tdee / bmr else 1.0)}", style = lab, modifier = Modifier.weight(1f))
                        Text(kcalFmt(tdee), style = num)
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        Text(when {
                            delta == 0.0 -> "Mantenimento"
                            delta > 0 -> "Surplus per ${fac(rate)} kg/sett"
                            else -> "Deficit per ${fac(rate)} kg/sett"
                        }, style = lab, modifier = Modifier.weight(1f))
                        Text((if (delta > 0) "+ " else if (delta < 0) "− " else "") + kcalFmt(kotlin.math.abs(delta)), style = num)
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfStat("${Nutrition.age(pr)}", "anni", Modifier.weight(1f), big = true)
                    ProfStat(pr.heightCm.r0(), "cm", Modifier.weight(1f), big = true)
                    ProfStat(w.r1(), "kg", Modifier.weight(1f), big = true)
                }
                Text("Il calcolo è una stima, non una prescrizione.", style = T.Small.copy(fontSize = 11.5.sp, lineHeight = 17.sp, color = Color(0xFF8A4A2E)), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
            }

            // Calcolo automatico / manuale
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 16.dp, elevation = 8.dp) {
                Column {
                    SwitchRow("Calcolo automatico", "Calorie e macro dal profilo e dal peso (consigliato)", auto) {
                        auto = it
                        if (it) { c = recommended.carbsPct; p = recommended.proteinPct; f = recommended.fatPct }
                        else kcalTxt = recommended.kcal.toString()
                    }
                    if (!auto) {
                        JField(kcalTxt, { kcalTxt = it.filter { ch -> ch.isDigit() } }, "Calorie giornaliere", number = true, suffix = "kcal", modifier = Modifier.padding(top = 8.dp))
                        val diff = k - recommended.kcal
                        if (k > 0 && kotlin.math.abs(diff) >= 100) Text(
                            "Per il tuo obiettivo il calcolo consiglia ${recommended.kcal} kcal: stai impostando ${if (diff > 0) "+" else ""}${diff.r0()} kcal.",
                            style = T.Small.copy(color = if (k < Nutrition.minKcal(pr)) C.Danger else A.Muted), modifier = Modifier.padding(top = 8.dp))
                        if (k in 1.0..(Nutrition.minKcal(pr) - 1.0)) Text(
                            "Sotto ${Nutrition.minKcal(pr)} kcal al giorno è meglio farsi seguire da un medico.", style = T.Small.copy(color = C.Danger), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Attività
            GHead("Quanto sei attivo?")
            if (!s.profile.detailed) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ACT_NAMES.forEachIndexed { i, name ->
                        val sel = i == act
                        AChoice(sel, Modifier.fillMaxWidth(), onClick = { act = i }) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                                    Text(ACT_DESC[i], style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                                }
                                Text("× ${fac(Nutrition.ACTIVITY_FACTORS[i])}", style = T.H1.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp, color = if (sel) Color(0xFFD9411E) else Color(0xFF6B6885)))
                            }
                        }
                    }
                }
            } else {
                AChoice(true, Modifier.fillMaxWidth(), onClick = { Nav.go(Screen.Questionnaire(null)) }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Calcolata dal tuo questionario", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                            Text("Lavoro, passi e allenamenti: tocca per modificarli", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        }
                        Text("× ${fac(if (bmr > 0) tdee / bmr else 1.0)}", style = T.H1.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp, color = Color(0xFFD9411E)))
                        GapW(8.dp)
                        AIcon(Ic.Right, Color(0xFF8A87A3), 18.dp, 2.4f)
                    }
                }
            }

            // Ritmo
            GHead("Obiettivo e ritmo")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PACES.chunked(2).forEach { rowPaces ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowPaces.forEach { pc ->
                            val sel = goal == pc.goal && (pc.goal == 0 || kotlin.math.abs(rate - pc.kg) < 0.01)
                            AChoice(sel, Modifier.weight(1f), minHeight = 76.dp, onClick = { goal = pc.goal; if (pc.goal != 0) rate = pc.kg }) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(pc.name, style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                                    Text(pc.rate, style = T.H1.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp, color = if (sel) Color(0xFFD9411E) else Color(0xFF6B6885)), modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Macronutrienti
            GHead("Macronutrienti")
            val advice = if (k >= 800) Nutrition.macroAdvice(pr, w, k) else null
            val protKg = if (w > 0) k * p / 100.0 / 4.0 / w else 0.0
            ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 20.dp, elevation = 8.dp) {
                Column {
                    Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.weight(p.coerceAtLeast(1).toFloat()).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF43F6E)))
                        Box(Modifier.weight(c.coerceAtLeast(1).toFloat()).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF3B82F6)))
                        Box(Modifier.weight(f.coerceAtLeast(1).toFloat()).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF59E0B)))
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Triple("Proteine", p to 4, Color(0xFFF43F6E)), Triple("Carboidrati", c to 4, Color(0xFF3B82F6)), Triple("Grassi", f to 9, Color(0xFFD97706))).forEach { (n, pk, col) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(k * pk.first / 100 / pk.second).r0()} g", style = T.H1.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp, color = col), maxLines = 1)
                                Text("$n · ${pk.first}%", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), maxLines = 1)
                            }
                        }
                    }
                    Box(Modifier.padding(vertical = 18.dp).fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Proteine per kg", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                            Text("Consigliato ${"%.1f".format(java.util.Locale.ITALY, Nutrition.proteinPerKg(pr))} g/kg", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        }
                        fun setProt(kg: Double) {
                            if (k < 800 || w <= 0) return
                            val np = (kg * w * 4 / k * 100).roundToInt().coerceIn(10, 45)
                            p = np; c = (100 - np - f).coerceAtLeast(5); f = 100 - np - c
                        }
                        AStepBtn(false, A.ProteinSoft, A.ProteinInk) { setProt(((protKg - 0.1) * 10).roundToInt() / 10.0) }
                        Text("%.1f".format(java.util.Locale.ITALY, protKg), style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center, modifier = Modifier.width(68.dp))
                        AStepBtn(true, A.ProteinSoft, A.ProteinInk) { setProt(((protKg + 0.1) * 10).roundToInt() / 10.0) }
                    }
                    Text("Quota di grassi", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                    val fats = listOf(25, 30, 35)
                    ASegment(fats.map { "$it%" }, fats.indexOf(f)) { i -> f = fats[i]; c = (100 - p - f).coerceAtLeast(5); p = 100 - c - f }
                    Text("I carboidrati prendono le calorie che restano.", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 10.dp))
                }
            }
            if (advice != null) {
                val best = Nutrition.PRESETS[advice.best]
                ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                    Column {
                        Text("Ti conviene: ${best.name}", style = T.H1.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp))
                        Text(advice.reason, style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 4.dp))
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Nutrition.PRESETS.forEachIndexed { i, pc ->
                                val on = c == pc.carbs && p == pc.protein && f == pc.fat
                                AChoice(on, Modifier.fillMaxWidth(), minHeight = 56.dp, radius = 20.dp, onClick = { c = pc.carbs; p = pc.protein; f = pc.fat }) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Row {
                                            Text(pc.name + if (i == advice.best) " ★" else "", style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                                            Text("${pc.carbs}C ${pc.protein}P ${pc.fat}G · ${"%.1f".format(java.util.Locale.ITALY, advice.perKg[i])} g/kg", style = T.Small.copy(fontSize = 12.sp, color = if (i == advice.best) Color(0xFFD9411E) else A.Muted))
                                        }
                                        Text(advice.notes[i], style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Acqua
            val waterShape = RoundedCornerShape(30.dp)
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().aShadow(waterShape, 12.dp, Color(0x993B82F6)).clip(waterShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFDDF6FB), Color(0xFFE6EEFF)))).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TonalIcon(Ic.Drop, Color(0xFF2563EB), Color.White, box = 52.dp, radius = 19.dp, size = 26.dp)
                GapW(14.dp)
                Column(Modifier.weight(1f)) {
                    Text("Obiettivo acqua", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                    Text("${water.thousands()} ml", style = T.H1.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp), maxLines = 1)
                }
                AStepBtn(false, Color.White, Color(0xFF2563EB)) { water = (water - 250).coerceIn(500, 6000) }
                GapW(8.dp)
                AStepBtn(true, Color.White, Color(0xFF2563EB)) { water = (water + 250).coerceIn(500, 6000) }
            }

            // Altri obiettivi: passi e bicchiere
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 16.dp, elevation = 8.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Passi al giorno", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                            Text("Obiettivo giornaliero", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        }
                        AStepBtn(false, A.GreenSoft, A.GreenInk) { steps = (steps - 500).coerceIn(1000, 50000) }
                        Text(steps.thousands(), style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center, modifier = Modifier.width(76.dp), maxLines = 1)
                        AStepBtn(true, A.GreenSoft, A.GreenInk) { steps = (steps + 500).coerceIn(1000, 50000) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Bicchiere", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                            Text("Quanta acqua aggiunge un tocco", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        }
                        AStepBtn(false, A.WaterSoft, A.WaterInk) { glass = (glass - 50).coerceIn(100, 1000) }
                        Text("$glass ml", style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center, modifier = Modifier.width(76.dp), maxLines = 1)
                        AStepBtn(true, A.WaterSoft, A.WaterInk) { glass = (glass + 50).coerceIn(100, 1000) }
                    }
                }
            }
            GapH(8.dp)
        }
        GSave("Salva obiettivi", enabled = c + p + f == 100 && k >= 800) {
            Store.update {
                it.copy(
                    profile = it.profile.copy(activity = act, goal = goal, weeklyRate = rate),
                    goals = it.goals.copy(auto = auto, kcal = k.toInt(), carbsPct = c, proteinPct = p, fatPct = f, waterMl = water, steps = steps),
                    glassMl = glass
                )
            }
            Nav.back()
        }
    }
}

private fun fmtMin(m: Int) = "%02d:%02d".format(m / 60, m % 60)

@Composable
fun RemindersScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val r = s.reminders
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun set(block: (com.jarvis.food.data.Reminders) -> com.jarvis.food.data.Reminders) {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        Store.update { it.copy(reminders = block(it.reminders)) }
        Store.saveNow()
        Reminders.rescheduleAll(ctx)
    }

    fun pickTime(current: Int, onPick: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Promemoria", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            JCard {
                SwitchRow("Pasti", "Ti ricordo di registrare colazione, pranzo e cena", r.meals) { v -> set { it.copy(meals = v) } }
                if (r.meals) {
                    listOf(0 to r.breakfast, 1 to r.lunch, 2 to r.dinner).forEach { (i, t) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(MEAL_NAMES[i], style = T.Body, modifier = Modifier.weight(1f))
                            GhostButton(fmtMin(t)) {
                                pickTime(t) { m -> set { when (i) { 0 -> it.copy(breakfast = m); 1 -> it.copy(lunch = m); else -> it.copy(dinner = m) } } }
                            }
                        }
                    }
                }
                HorizontalDivider(color = C.Line)
                SwitchRow("Acqua", "Ogni 2 ore dalle 9 alle 21", r.water) { v -> set { it.copy(water = v) } }
                HorizontalDivider(color = C.Line)
                SwitchRow("Pesata", "Ogni mattina", r.weighIn) { v -> set { it.copy(weighIn = v) } }
                if (r.weighIn) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Orario", style = T.Body, modifier = Modifier.weight(1f))
                    GhostButton(fmtMin(r.weighInTime)) { pickTime(r.weighInTime) { m -> set { it.copy(weighInTime = m) } } }
                }
                HorizontalDivider(color = C.Line)
                SwitchRow("Fine digiuno", "Avviso quando raggiungi l'obiettivo", r.fasting) { v -> set { it.copy(fasting = v) } }
            }
            Spacer(Modifier.height(12.dp))
            if (Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                JCard {
                    Text("Orari precisi", style = T.Title2)
                    Text("Per ricevere i promemoria all'orario esatto concedi il permesso \"Sveglie e promemoria\". Senza, Android può ritardarli di qualche minuto.", style = T.Muted)
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Apri impostazioni") {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiSettingsScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    var key by remember { mutableStateOf(s.ai.apiKey) }
    var model by remember { mutableStateOf(s.ai.model) }
    var url by remember { mutableStateOf(s.ai.baseUrl) }
    val offline = remember { FoodAi.offlineAvailable(ctx) }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Intelligenza artificiale", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChatSetup()
            JCard {
                Text("Offline · sul telefono", style = T.Title2)
                Text(
                    if (offline) "Attiva. Classificatore alimentare open source di Google (circa 2.000 piatti), funziona senza internet. Riconosce il piatto, la porzione la scegli tu."
                    else "Il modello offline non è incluso in questa build.",
                    style = T.Muted
                )
            }
            JCard {
                Text("Online · gratuita", style = T.Title2)
                Text("Riconosce piatti, frigo, dispensa e scontrini e crea ricette con ciò che hai. Predefinito: Gemma 4 31B di Google, " +
                        "modello open-weight con visione, gratuito su OpenRouter; se non risponde provo Nemotron VL e il router gratuito. " +
                        "Limite indicativo del piano gratuito: circa 200 richieste al giorno. L'AI riconosce gli alimenti: calorie e nutrienti li calcola sempre l'app dal database.", style = T.Muted)
                Spacer(Modifier.height(8.dp))
                Text("1. Crea un account gratuito su openrouter.ai\n2. Vai su Keys → Create key\n3. Incolla la chiave qui sotto", style = T.Body)
                Spacer(Modifier.height(8.dp))
                GhostButton("Apri openrouter.ai/keys") {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys"))) }
                }
            }
            JField(key, { key = it.trim() }, "Chiave API")
            JField(model, { model = it.trim() }, "Modello")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Gemma 4", model == AiConfig.DEFAULT_MODEL) { model = AiConfig.DEFAULT_MODEL; url = AiConfig().baseUrl }
                Pill("Nemotron VL", model.startsWith("nvidia/nemotron-3.5-super-vl")) { model = "nvidia/nemotron-3.5-super-vl:free"; url = AiConfig().baseUrl }
                Pill("Automatico", model == "openrouter/free") { model = "openrouter/free"; url = AiConfig().baseUrl }
            }
            JField(url, { url = it.trim() }, "Indirizzo API (compatibile OpenAI)")
            Text("Puoi usare qualsiasi servizio compatibile OpenAI con un modello che accetta immagini. La foto viene inviata solo quando premi \"Analizza online\".", style = T.Small)
            GhostButton("Ripristina predefiniti") { val d = AiConfig(); model = d.model; url = d.baseUrl }
        }
        PrimaryButton("Salva", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Store.update { it.copy(ai = it.ai.copy(baseUrl = url.ifBlank { AiConfig().baseUrl }, model = model.ifBlank { AiConfig().model }, apiKey = key)) }
            Toast.makeText(ctx, "Impostazioni AI salvate", Toast.LENGTH_SHORT).show()
            Nav.back()
        }
    }
}

@Composable
fun BackupScreen() {
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportJson().toByteArray()) }
        }.onSuccess { Toast.makeText(ctx, "Backup salvato", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "Errore: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching {
            val sb = StringBuilder("data;pasto;alimento;marca;quantita;kcal;carboidrati;proteine;grassi\n")
            Store.current.diary.sortedBy { it.date }.forEach { e ->
                val t = Nutrition.of(e.food, e.grams)
                sb.append("${e.date};${MEAL_NAMES[e.meal]};${e.food.name.replace(";", ",")};${e.food.brand.replace(";", ",")};" +
                        "${e.grams.r0()};${t.kcal.r0()};${t.carbs.r1()};${t.protein.r1()};${t.fat.r1()}\n")
            }
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray()) }
        }.onSuccess { Toast.makeText(ctx, "CSV esportato", Toast.LENGTH_SHORT).show() }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Backup e dati", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JCard {
                Text("I tuoi dati restano sul telefono", style = T.Title2)
                Text("Nessun account, nessun server. Esporta un backup ogni tanto e salvalo su Google Drive: puoi scegliere Drive direttamente nella finestra di salvataggio.", style = T.Muted)
            }
            PrimaryButton("Esporta backup (.json)", Modifier.fillMaxWidth()) { export.launch("jarvis-food-backup-${LocalDate.now()}.json") }
            GhostButton("Importa backup", Modifier.fillMaxWidth()) { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
            GhostButton("Esporta diario in CSV (Excel)", Modifier.fillMaxWidth()) { csv.launch("jarvis-food-diario-${LocalDate.now()}.csv") }
            Spacer(Modifier.height(16.dp))
            GhostButton("Cancella tutti i dati", Modifier.fillMaxWidth(), color = C.Danger) { confirmReset = true }
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, containerColor = C.Surface,
        title = { Text("Cancellare tutto?", style = T.Title) },
        text = { Text("Diario, peso, ricette e impostazioni verranno eliminati. L'operazione non si può annullare: esporta prima un backup.", style = T.Muted) },
        confirmButton = { TextButton(onClick = { Store.resetAll(); confirmReset = false; Nav.clear(); Nav.tab = 0 }) { Text("Cancella", color = C.Danger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annulla", color = C.Muted) } }
    )

    pendingImport?.let { txt ->
        AlertDialog(
            onDismissRequest = { pendingImport = null }, containerColor = C.Surface,
            title = { Text("Importare il backup?", style = T.Title) },
            text = { Text("I dati attuali verranno sostituiti da quelli del file.", style = T.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    Store.importJson(txt)
                        .onSuccess { Toast.makeText(ctx, "Backup importato", Toast.LENGTH_SHORT).show(); Reminders.rescheduleAll(ctx) }
                        .onFailure { Toast.makeText(ctx, "File non valido: ${it.message}", Toast.LENGTH_LONG).show() }
                    pendingImport = null
                }) { Text("Importa", color = C.Accent) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Annulla", color = C.Muted) } }
        )
    }
}

@Composable
fun ActivityScreen(date: LocalDate) {
    val s by Store.state.collectAsState()
    var selected by remember { mutableStateOf<Activities.Activity?>(null) }
    var minutes by remember { mutableStateOf("30") }
    var customName by remember { mutableStateOf("") }
    var customKcal by remember { mutableStateOf("") }
    val w = Store.currentWeight(s)

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Aggiungi attività", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("Calorie stimate con i valori MET e il tuo peso (${w.r1()} kg).", style = T.Small)
            Spacer(Modifier.height(10.dp))
            Activities.activities.forEach { a ->
                Choice("${a.emoji}  ${a.name}", selected == a) { selected = a }
            }
            SectionTitle("Oppure inserisci a mano")
            JField(customName, { customName = it }, "Attività")
            Spacer(Modifier.height(8.dp))
            JField(customKcal, { customKcal = it.filter { c -> c.isDigit() } }, "Calorie bruciate", number = true, suffix = "kcal")
        }
        Column(Modifier.navigationBarsPadding().padding(16.dp)) {
            JField(minutes, { minutes = it.filter { c -> c.isDigit() } }, "Durata", number = true, suffix = "min")
            Spacer(Modifier.height(8.dp))
            val min = minutes.toIntOrNull() ?: 0
            val kcal = when {
                customName.isNotBlank() && customKcal.isNotBlank() -> customKcal.toIntOrNull() ?: 0
                selected != null -> Nutrition.activityKcal(selected!!.met, w, min)
                else -> 0
            }
            PrimaryButton("Aggiungi · $kcal kcal", Modifier.fillMaxWidth(), enabled = kcal > 0) {
                val name = if (customName.isNotBlank() && customKcal.isNotBlank()) customName else selected!!.name
                Store.addActivity(date, name, min, kcal)
                Nav.back()
            }
        }
    }
}

@Composable
fun DayNutrientsScreen(date: LocalDate) {
    val s by Store.state.collectAsState()
    val entries = Store.entriesFor(s, date)
    val t = Nutrition.sum(entries)
    val (gc, gp, gf) = Nutrition.goalGrams(s.goals)
    Column(Modifier.fillMaxSize()) {
        TopBar("Nutrienti · ${dayLabel(date)}", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacroDonut(t.carbs, t.protein, t.fat, 110.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${t.kcal.r0()} kcal", style = T.Big)
                        Text("su ${s.goals.kcal} kcal", style = T.Muted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            JCard {
                listOf(
                    Triple("Carboidrati", t.carbs to gc, C.Carbs),
                    Triple("Proteine", t.protein to gp, C.Protein),
                    Triple("Grassi", t.fat to gf, C.Fat),
                    Triple("Fibre (consigliate ≥ 25 g)", t.fiber to 25.0, C.Accent),
                    Triple("Zuccheri (limite consigliato 50 g)", t.sugar to 50.0, C.Fat),
                    Triple("Grassi saturi (limite 10% kcal)", t.satFat to s.goals.kcal * 0.10 / 9, C.Protein),
                    Triple("Sale (limite OMS 5 g)", t.salt to 5.0, C.Water)
                ).forEach { (label, pair, color) ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row {
                            Text(label, style = T.Label, modifier = Modifier.weight(1f))
                            Text("${pair.first.r1()} / ${pair.second.r0()} g", style = T.Label.copy(color = color))
                        }
                        Spacer(Modifier.height(4.dp))
                        ProgressLine((pair.first / pair.second).toFloat(), color)
                    }
                }
                Text("Fibre, zuccheri, saturi e sale si sommano solo per gli alimenti che riportano questi valori.", style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
            SectionTitle("Per pasto")
            JCard {
                MEAL_NAMES.forEachIndexed { i, n ->
                    val mt = Nutrition.sum(entries.filter { it.meal == i })
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(n, style = T.Body, modifier = Modifier.weight(1f))
                        Text("${mt.kcal.r0()} kcal", style = T.Label)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize()) {
        TopBar("Informazioni", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Jarvis Food ${BuildConfig.VERSION_NAME}", style = T.Title)
            Text("Diario alimentare personale. Dati salvati solo sul telefono.", style = T.Muted)
            JCard {
                Text("Fonti", style = T.Title2)
                Text("• Prodotti confezionati: Open Food Facts (openfoodfacts.org), database aperto con licenza ODbL.\n" +
                        "• Alimenti e ricette: USDA FoodData Central, SR Legacy (dominio pubblico); ogni ingrediente riporta il suo FDC ID.\n" +
                        "• Vitamine e minerali: valori di riferimento EFSA (Dietary Reference Values per adulti).\n" +
                        "• Smartwatch: dati letti da Health Connect (es. app Zepp per Amazfit).\n" +
                        "• AI online: modelli gratuiti tramite OpenRouter; testo degli scontrini letto sul telefono con ML Kit.\n" +
                        "• Calcolo fabbisogno: formula di Mifflin-St Jeor.\n" +
                        "• Attività: valori MET del Compendium of Physical Activities.\n" +
                        "• AI offline: classificatore alimentare open source di Google (TensorFlow Lite).", style = T.Body)
            }
            JCard {
                Text("Foto delle ricette", style = T.Title2)
                com.jarvis.food.data.Db.db.recipes.mapNotNull { r -> r.credit?.takeIf { it.author.isNotBlank() }?.let { r.name to it } }.forEach { (n, c) ->
                    Text("• $n: foto di ${c.author.take(40)} (${c.source})", style = T.Small)
                }
                Text("Foto da Pixabay (pixabay.com), Pixabay Content License: uso gratuito, attribuzione non obbligatoria ma riportata qui per correttezza. Le foto sono illustrative: il piatto preparato con i tuoi grammi può apparire diverso.", style = T.Small)
            }
            JCard {
                Text("Avvertenza", style = T.Title2)
                Text("Calorie, macro e stime AI sono indicative e non sostituiscono il parere di un medico o di un nutrizionista.", style = T.Body)
            }
        }
    }
}
