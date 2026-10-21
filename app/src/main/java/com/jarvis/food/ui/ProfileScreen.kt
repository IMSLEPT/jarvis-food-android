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

@Composable
fun ProfileScreen() {
    val s by Store.state.collectAsState()
    val w = Store.currentWeight(s)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(C.Accent), contentAlignment = Alignment.Center) {
                Text(s.profile.name.take(1).uppercase(), style = T.Title.copy(color = C.OnAccent))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(s.profile.name, style = T.Title)
                Text("${Nutrition.age(s.profile)} anni · ${s.profile.heightCm.r0()} cm · ${w.r1()} kg", style = T.Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        JCard {
            Row {
                Stat("Obiettivo", "${Store.energyFor(s, LocalDate.now()).baseKcal.r0()} kcal", C.AccentDeep)
                Spacer(Modifier.weight(1f))
                Stat("Piano", when (s.profile.goal) { -1 -> "Dimagrire"; 1 -> "Massa"; else -> "Mantenere" }, C.Text)
                Spacer(Modifier.weight(1f))
                Stat("Serie", "${streak(s)} gg", C.Fat)
            }
        }
        SectionTitle("Il mio piano")
        JCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            NavRow("Profilo e obiettivo", "Dati personali, peso obiettivo, ritmo", "👤") { Nav.go(Screen.ProfileEdit) }
            HorizontalDivider(color = C.Line)
            NavRow("Calorie e macronutrienti", "${s.goals.carbsPct}% C · ${s.goals.proteinPct}% P · ${s.goals.fatPct}% G", "🎯") { Nav.go(Screen.Goals) }
            HorizontalDivider(color = C.Line)
            NavRow("Peso", "${w.r1()} kg", "⚖️") { Nav.go(Screen.Weight) }
            HorizontalDivider(color = C.Line)
            NavRow("Misure corporee", null, "📏") { Nav.go(Screen.Measurements) }
            HorizontalDivider(color = C.Line)
            NavRow("Preferenze della dieta", "Esclusioni, ripartizione dei pasti, tempi", "🥗") { Nav.go(Screen.DietSettings) }
        }
        SectionTitle("Strumenti")
        JCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            NavRow("Smartwatch e attività", when (s.energy.mode) { 2 -> "Amazfit / Health Connect collegato"; 1 -> "Attività inserite a mano"; else -> "Livello di attività fisso" }, "⌚") { Nav.go(Screen.Watch) }
            HorizontalDivider(color = C.Line)
            NavRow("Intelligenza artificiale", if (s.ai.apiKey.isBlank()) "Da configurare (gratuita)" else "Attiva · ${s.ai.model}", "✨") { Nav.go(Screen.AiSettings) }
            HorizontalDivider(color = C.Line)
            NavRow("Digiuno intermittente", if (s.fasting.activeStart != null) "In corso · ${s.fasting.planId}" else "Piano ${s.fasting.planId}", "⏱️") { Nav.go(Screen.Fasting) }
            HorizontalDivider(color = C.Line)
            NavRow("Promemoria", "Pasti, acqua, pesata, digiuno", "🔔") { Nav.go(Screen.RemindersSet) }
            HorizontalDivider(color = C.Line)
            NavRow("I miei alimenti", "${s.customFoods.size} alimenti · ${s.favorites.size} preferiti", "🧑‍🍳") { Nav.go(Screen.MyFoods) }
            HorizontalDivider(color = C.Line)
            NavRow("Backup e dati", "Esporta, importa, CSV", "💾") { Nav.go(Screen.Backup) }
            HorizontalDivider(color = C.Line)
            NavRow("Informazioni", "Versione ${BuildConfig.VERSION_NAME}", "ℹ️") { Nav.go(Screen.About) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ProfileEditScreen() {
    val s by Store.state.collectAsState()
    val p = s.profile
    var name by remember { mutableStateOf(p.name) }
    var male by remember { mutableStateOf(p.male) }
    var birth by remember { mutableStateOf(p.birthYear.toString()) }
    var height by remember { mutableStateOf(p.heightCm.r0()) }
    var target by remember { mutableStateOf(p.targetWeight.r1()) }
    var activity by remember { mutableIntStateOf(p.activity) }
    var goal by remember { mutableIntStateOf(p.goal) }
    var rate by remember { mutableDoubleStateOf(p.weeklyRate) }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Profilo e obiettivo", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JField(name, { name = it }, "Nome")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Uomo", male) { male = true }; Pill("Donna", !male) { male = false }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(birth, { birth = it.filter { c -> c.isDigit() }.take(4) }, "Anno di nascita", Modifier.weight(1f), number = true)
                JField(height, { height = it }, "Altezza", Modifier.weight(1f), number = true, suffix = "cm")
            }
            JField(target, { target = it }, "Peso obiettivo", number = true, suffix = "kg")
            Text("Attività", style = T.Title2)
            Nutrition.ACTIVITY_LABELS.forEachIndexed { i, l -> Choice(l, activity == i) { activity = i } }
            Text("Obiettivo", style = T.Title2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Dimagrire", goal == -1) { goal = -1 }
                Pill("Mantenere", goal == 0) { goal = 0 }
                Pill("Massa", goal == 1) { goal = 1 }
            }
            if (goal != 0) {
                Text("Ritmo: ${rate.r1()} kg/settimana", style = T.Label)
                Slider(
                    rate.toFloat(), { rate = (Math.round(it * 4) / 4.0).coerceAtLeast(0.25) }, valueRange = 0.25f..1f, steps = 2,
                    colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceHi)
                )
            }
        }
        PrimaryButton("Salva e ricalcola", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Store.update {
                val np = it.profile.copy(
                    name = name.ifBlank { "Tu" }, male = male, birthYear = birth.toIntOrNull() ?: it.profile.birthYear,
                    heightCm = parseNum(height) ?: it.profile.heightCm, targetWeight = parseNum(target) ?: it.profile.targetWeight,
                    activity = activity, goal = goal, weeklyRate = rate
                )
                val g = if (it.goals.auto) Nutrition.autoGoals(np, Store.currentWeight(it), it.goals) else it.goals
                it.copy(profile = np, goals = g)
            }
            Nav.back()
        }
    }
}

@Composable
fun GoalsScreen() {
    val s by Store.state.collectAsState()
    val g = s.goals
    var auto by remember { mutableStateOf(g.auto) }
    var kcal by remember { mutableStateOf(g.kcal.toString()) }
    var c by remember { mutableIntStateOf(g.carbsPct) }
    var p by remember { mutableIntStateOf(g.proteinPct) }
    var f by remember { mutableIntStateOf(g.fatPct) }
    var water by remember { mutableStateOf(g.waterMl.toString()) }
    var steps by remember { mutableStateOf(g.steps.toString()) }
    var glass by remember { mutableStateOf(s.glassMl.toString()) }
    val recommended = Nutrition.autoGoals(s.profile, Store.currentWeight(s), g)
    val k = if (auto) recommended.kcal.toDouble() else parseNum(kcal) ?: 0.0

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Obiettivi", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SwitchRow("Calcolo automatico", "Calorie e macro dal profilo e dal peso (consigliato)", auto) {
                auto = it
                if (it) { c = recommended.carbsPct; p = recommended.proteinPct; f = recommended.fatPct }
            }
            if (auto) JCard {
                Text("${recommended.kcal} kcal al giorno", style = T.Title.copy(color = C.Accent))
                Text("Basale ${Nutrition.bmr(s.profile, Store.currentWeight(s)).r0()} · fabbisogno ${Nutrition.tdee(s.profile, Store.currentWeight(s)).r0()} · ${if (Nutrition.dailyDelta(s.profile) >= 0) "+" else ""}${Nutrition.dailyDelta(s.profile).r0()} kcal per l'obiettivo", style = T.Small)
            } else JField(kcal, { kcal = it.filter { ch -> ch.isDigit() } }, "Calorie giornaliere", number = true, suffix = "kcal")

            Text("Ripartizione macronutrienti", style = T.Title2)
            MacroSlider("Carboidrati", c, C.Carbs, k, 4) { c = it }
            MacroSlider("Proteine", p, C.Protein, k, 4) { p = it }
            MacroSlider("Grassi", f, C.Fat, k, 9) { f = it }
            val sum = c + p + f
            Text(if (sum == 100) "Totale 100% ✓" else "Totale $sum%: deve essere 100%", style = T.Label.copy(color = if (sum == 100) C.Accent else C.Danger))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Bilanciato") { c = 50; p = 20; f = 30 }
                GhostButton("Proteico") { c = 40; p = 30; f = 30 }
                GhostButton("Low carb") { c = 25; p = 30; f = 45 }
            }
            Text("Altri obiettivi", style = T.Title2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(water, { water = it.filter { ch -> ch.isDigit() } }, "Acqua", Modifier.weight(1f), number = true, suffix = "ml")
                JField(glass, { glass = it.filter { ch -> ch.isDigit() } }, "Bicchiere", Modifier.weight(1f), number = true, suffix = "ml")
            }
            JField(steps, { steps = it.filter { ch -> ch.isDigit() } }, "Passi al giorno", number = true)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton("Salva", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = c + p + f == 100 && k >= 800) {
            Store.update {
                it.copy(
                    goals = it.goals.copy(
                        auto = auto, kcal = k.toInt(), carbsPct = c, proteinPct = p, fatPct = f,
                        waterMl = water.toIntOrNull()?.coerceIn(500, 6000) ?: it.goals.waterMl,
                        steps = steps.toIntOrNull()?.coerceIn(1000, 50000) ?: it.goals.steps
                    ),
                    glassMl = glass.toIntOrNull()?.coerceIn(100, 1000) ?: it.glassMl
                )
            }
            Nav.back()
        }
    }
}

@Composable
private fun MacroSlider(label: String, pct: Int, color: androidx.compose.ui.graphics.Color, kcal: Double, kcalPerG: Int, onChange: (Int) -> Unit) {
    Column {
        Row {
            Text(label, style = T.Label, modifier = Modifier.weight(1f))
            Text("$pct% · ${(kcal * pct / 100 / kcalPerG).r0()} g", style = T.Label.copy(color = color))
        }
        Slider(
            pct.toFloat(), { onChange(it.toInt()) }, valueRange = 5f..70f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = C.SurfaceHi)
        )
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
            Store.update { it.copy(ai = AiConfig(baseUrl = url.ifBlank { AiConfig().baseUrl }, model = model.ifBlank { AiConfig().model }, apiKey = key)) }
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
                    Text("• $n: ${c.title.take(60)} – ${c.author.take(60)}, ${c.license} (${c.source})", style = T.Small)
                }
                Text("Foto con licenze libere (Creative Commons / pubblico dominio) da Openverse e Wikimedia Commons. Sono illustrative.", style = T.Small)
            }
            JCard {
                Text("Avvertenza", style = T.Title2)
                Text("Calorie, macro e stime AI sono indicative e non sostituiscono il parere di un medico o di un nutrizionista.", style = T.Body)
            }
        }
    }
}
