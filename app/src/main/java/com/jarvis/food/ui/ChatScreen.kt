package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.R
import com.jarvis.food.chat.Jarvis
import com.jarvis.food.chat.LocalModel
import com.jarvis.food.chat.ModelManager
import com.jarvis.food.data.ChatMsg
import com.jarvis.food.data.Store
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun gb(b: Long) = String.format(Locale.ITALY, "%.1f GB", b / 1_000_000_000.0)

@Composable
fun JarvisLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.25f)).background(C.Navy), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.jarvis_logo), "Jarvis", Modifier.fillMaxSize().padding(size * 0.08f), contentScale = ContentScale.Fit)
    }
}

@Composable
fun ChatScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    var showSetup by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Leggere gli stati del download fa ricomporre la schermata quando il modello diventa pronto.
    @Suppress("UNUSED_VARIABLE") val dl = ModelManager.downloading
    val ready = Jarvis.ready(ctx, s.ai)
    val busy = Jarvis.busy
    val partial = Jarvis.partial

    LaunchedEffect(s.chat.size, partial.length, busy) {
        val extra = if (busy) 1 else 0
        val n = s.chat.size + extra
        if (n > 0) listState.scrollToItem(n - 1)
    }

    fun send(t: String) { if (t.isNotBlank() && ready && !busy) { Jarvis.send(ctx, t); input = "" } }

    val active = s.chat.isNotEmpty()
    val last = s.chat.lastOrNull()
    val suggestions = if (ready && !busy && active && last?.pending.isNullOrEmpty()) {
        if (last?.role == "assistant" && last.suggestions.isNotEmpty()) last.suggestions
        else com.jarvis.food.chat.Actions.smartSuggestions(s).take(4)
    } else emptyList()
    fun pick(text: String) { if (text.endsWith("…")) input = text.removeSuffix("…") + " " else send(text) }

    Column(Modifier.fillMaxSize().imePadding()) {
        ChatHeader(active, showSetup, { Jarvis.clear() }, { showSetup = !showSetup })
        if (showSetup || !ready) {
            // Keep the motor settings scrollable without squeezing the composer.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) { ChatSetup() }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("chat-messages"), state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = if (active) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (active) 18.dp else 10.dp)) {
                if (!active) item { ChatIntro(ready, com.jarvis.food.chat.Actions.smartSuggestions(s).take(5), ::pick) }
                itemsIndexed(s.chat, key = { _, message -> message.id }) { index, message ->
                    val day = chatDay(message.time)
                    if (index == 0 || chatDay(s.chat[index - 1].time) != day) ChatDaySeparator(day)
                    Bubble(message, if (message.id == last?.id) suggestions else emptyList(), ::pick)
                }
                if (busy) item { Typing(partial, Jarvis.status) }
            }
        }
        ChatComposer(input, { input = it }, ready, busy, active, { send(input) }, { Jarvis.stop() })
    }
}

// Same Aurora icon grid as the rest of the app: compose a chat, and a car engine.
private const val NEW_CHAT_ICON = "M13 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7M16 3l5 5M10 14l1-4 8-8 4 4-8 8-5 1z"
private const val ENGINE_ICON = "M8 7V5h7l3 3h3v10h-4l-2 3H9l-3-3H3V9h3l2-2zM10 2h5M12.5 2v3M1 10v6M1 13h2M23 10v6M21 13h2"

@Composable
private fun ChatHeader(active: Boolean, setup: Boolean, onNew: () -> Unit, onSetup: () -> Unit) {
    val actions: @Composable () -> Unit = {
        ChatHeaderAction(NEW_CHAT_ICON, "Nuova chat", A.Tonal, A.TonalInk, onNew)
        Spacer(Modifier.size(6.dp))
        ChatHeaderAction(ENGINE_ICON, if (setup) "Chiudi impostazioni motore" else "Motore della chat", C.NavySoft, C.Navy, onSetup)
    }
    if (!active) {
        TopBar("Jarvis", onBack = if (Nav.stack.isNotEmpty()) ({ Nav.back() }) else null) { actions() }
    } else Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (Nav.stack.isNotEmpty()) {
            ChatHeaderAction(Ic.Left, "Indietro", Color.White, C.Navy) { Nav.back() }
            Spacer(Modifier.size(8.dp))
        }
        JarvisLogo(40.dp)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text("Jarvis", style = T.Title.copy(fontSize = 22.sp, color = C.Navy))
            Text("Il tuo assistente alimentare", style = T.Small, maxLines = 2)
        }
        actions()
    }
}

@Composable
private fun ChatHeaderAction(icon: String, label: String, background: Color, ink: Color, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(19.dp)).background(background)
        .semantics { contentDescription = label }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(icon, ink, 23.dp, 1.9f)
    }
}

private fun chatDay(time: Long): LocalDate = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun ChatDaySeparator(day: LocalDate) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> "Oggi"
        today.minusDays(1) -> "Ieri"
        else -> day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALY))
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(C.Line))
        Text(label, style = T.Small)
        Box(Modifier.weight(1f).height(1.dp).background(C.Line))
    }
}

@Composable
private fun ChatComposer(input: String, onInput: (String) -> Unit, ready: Boolean, busy: Boolean, active: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    val canSend = ready && input.isNotBlank() && !busy
    val shape = RoundedCornerShape(if (active) 28.dp else 26.dp)
    Row(Modifier.fillMaxWidth().then(if (Nav.stack.isNotEmpty()) Modifier.navigationBarsPadding() else Modifier)
        .padding(start = 12.dp, end = 12.dp, top = if (active) 12.dp else 4.dp, bottom = if (active) 10.dp else 2.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input, onValueChange = onInput,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp).then(if (active) Modifier.aShadow(shape, 9.dp, Color(0x2617286B)) else Modifier).testTag("chat-input"), enabled = ready,
            placeholder = { Text(if (ready) "Scrivi a Jarvis…" else "Scarica o scegli un motore", style = if (active) T.Read.copy(color = C.Muted) else T.Muted, maxLines = 1) },
            textStyle = if (active) T.Read else T.Body, shape = shape, maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend() }),
            trailingIcon = if (active) ({ Box(Modifier.padding(end = 6.dp)) { ChatSendControl(canSend, busy, 44.dp, onSend, onStop) } }) else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = C.Navy, unfocusedBorderColor = C.Line, cursorColor = C.Accent,
                focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface, disabledContainerColor = C.SurfaceAlt, disabledBorderColor = C.Line
            )
        )
        if (!active) {
            Spacer(Modifier.size(8.dp))
            ChatSendControl(canSend, busy, 50.dp, onSend, onStop)
        }
    }
}

@Composable
private fun ChatSendControl(canSend: Boolean, busy: Boolean, size: Dp, onSend: () -> Unit, onStop: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape)
        .background(if (busy) Brush.linearGradient(listOf(C.Navy, C.Navy)) else if (canSend) C.OrangeBrush else Brush.linearGradient(listOf(C.SurfaceHi, C.SurfaceHi)))
        .semantics { contentDescription = if (busy) "Interrompi risposta" else "Invia messaggio" }
        .clickable(enabled = canSend || busy, role = Role.Button) { if (busy) onStop() else onSend() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            val w = this.size.width; val h = this.size.height
            if (busy) drawRoundRect(Color.White, Offset(w * 0.25f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
            else {
                val stroke = 2.4.dp.toPx(); val ink = if (canSend) C.OnOrange else C.Faint
                drawLine(ink, Offset(w / 2, h * 0.88f), Offset(w / 2, h * 0.14f), stroke, StrokeCap.Round)
                drawLine(ink, Offset(w / 2, h * 0.12f), Offset(w * 0.18f, h * 0.45f), stroke, StrokeCap.Round)
                drawLine(ink, Offset(w / 2, h * 0.12f), Offset(w * 0.82f, h * 0.45f), stroke, StrokeCap.Round)
            }
        }
    }
}

private val CAPABILITIES = listOf(
    Triple("🍽️", "Diario", "Ho mangiato 150 g di pollo a pranzo"),
    Triple("📖", "Ricette", "Cerca una cena vegetariana sotto 500 kcal"),
    Triple("📅", "Piano", "Crea il piano di 7 giorni"),
    Triple("🛒", "Spesa e dispensa", "Metti nella spesa le uova"),
    Triple("🎯", "Obiettivi", "Che macro mi conviene?"),
    Triple("⏱️", "Digiuno", "Avvia il digiuno 16:8")
)

@Composable
private fun ChatIntro(ready: Boolean, smart: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        JarvisLogo(76.dp)
        Spacer(Modifier.height(8.dp))
        Text("Ciao, sono Jarvis", style = T.Title)
        Text("Uso l'app al posto tuo: registro i pasti, cerco ricette, preparo piano e spesa. Ti dico sempre cosa ho fatto davvero e prima di eliminare qualcosa ti chiedo conferma.",
            style = T.Muted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        if (ready) {
            Spacer(Modifier.height(8.dp))
            CAPABILITIES.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (emoji, title, example) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(18.dp))
                            .clickable { onPick(example) }.padding(12.dp)) {
                            Text("$emoji  $title", style = T.Label)
                            Spacer(Modifier.height(4.dp))
                            Text("“$example”", style = T.Small)
                        }
                    }
                }
            }
            if (smart.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Per te adesso", style = T.Label, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    smart.forEach { q -> Pill(q, false) { onPick(q) } }
                }
            }
        }
    }
}

private val CHAT_QUANTITY = Regex("""(?<![\p{L}\d])\d+(?:[.,]\d+)?\s*(?:kcal|kg|mg|mcg|µg|g|ml|l|%)(?![\p{L}\d])""", RegexOption.IGNORE_CASE)

@Composable
private fun ChatResponseText(text: String) {
    val formatted = remember(text) {
        buildAnnotatedString {
            append(text)
            CHAT_QUANTITY.findAll(text).forEach { match -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1) }
        }
    }
    Text(formatted, style = T.Read)
}

@Composable
private fun AssistantCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().aShadow(shape, 8.dp, Color(0x2417286B)).clip(shape).background(C.Surface).border(1.dp, C.Line, shape).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarvisLogo(32.dp)
            Spacer(Modifier.size(10.dp))
            Text("Jarvis", style = T.Title2.copy(color = C.Navy))
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun Bubble(m: ChatMsg, suggestions: List<String>, onPick: (String) -> Unit) {
    val mine = m.role == "user"
    Column(Modifier.fillMaxWidth().testTag("chat-message-${m.id}"), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        if (mine) {
            val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 6.dp)
            Box(Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.82f).coerceAtMost(420f).dp).clip(shape).background(C.NavyBrush).padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(m.text, style = T.Read.copy(color = Color.White))
            }
        } else AssistantCard { ChatResponseText(m.text) }
        if (m.pending.isNotEmpty()) {
            val shape = RoundedCornerShape(20.dp)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Danger.copy(alpha = 0.35f), shape).padding(14.dp)) {
                Text("Conferma eliminazione", style = T.Label.copy(color = C.Danger))
                m.pending.forEach { p -> Text("• " + p.label, style = T.Body, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.heightIn(min = 48.dp).weight(1f).clip(RoundedCornerShape(24.dp)).background(C.Danger).clickable(enabled = !Jarvis.busy) { Jarvis.confirm(m.id) }.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("Sì, elimina", style = T.BtnS.copy(color = Color.White))
                    }
                    Box(Modifier.heightIn(min = 48.dp).weight(1f).clip(RoundedCornerShape(24.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(24.dp))
                        .clickable(enabled = !Jarvis.busy) { Jarvis.cancelPending(m.id) }.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("Annulla", style = T.BtnS)
                    }
                }
            }
        }
        if (m.actions.isNotEmpty()) Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            m.actions.forEach { a ->
                val ok = a.startsWith("✓")
                val warn = a.startsWith("⚠")
                Text(a, style = T.Small.copy(color = if (ok) A.GreenInk else if (warn) C.Danger else C.Muted),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (ok) C.Surface else if (warn) C.DangerSoft else C.SurfaceAlt)
                        .padding(horizontal = 12.dp, vertical = 7.dp))
            }
            if (Jarvis.canUndo(m.id) && !Jarvis.busy) Text("Annulla le modifiche", style = T.Label.copy(color = C.AccentDeep),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Jarvis.undo(m.id) }.padding(vertical = 10.dp, horizontal = 2.dp))
        }
        if (suggestions.isNotEmpty()) ChatSuggestions(suggestions, onPick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatSuggestions(suggestions: List<String>, onPick: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("chat-suggestions"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { text ->
            val primary = text.trim().lowercase(Locale.ITALY).startsWith("sì") || text.equals("Applica", true)
            val shape = RoundedCornerShape(24.dp)
            Row(Modifier.heightIn(min = 48.dp).clip(shape)
                .then(if (primary) Modifier.background(C.OrangeBrush) else Modifier.background(C.Surface).border(1.dp, C.Navy.copy(alpha = 0.3f), shape))
                .clickable(role = Role.Button) { onPick(text) }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (primary) { AIcon(Ic.Check, C.OnOrange, 18.dp, 2.3f); Spacer(Modifier.size(8.dp)) }
                Text(text, style = T.BtnM.copy(color = if (primary) C.OnOrange else C.Navy))
            }
        }
    }
}

@Composable
private fun Typing(partial: String, status: String) {
    AssistantCard {
        if (partial.isNotBlank()) { ChatResponseText(partial); Spacer(Modifier.height(10.dp)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(A.TonalInk))
            Text(status.ifBlank { "Jarvis sta scrivendo…" }, style = T.Small)
        }
    }
}

/** Scelta del motore della chat e gestione del modello sul telefono. Usata nella chat e nelle impostazioni AI. */
@Composable
fun ChatSetup() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val cfg = s.ai
    val model = LocalModel.of(cfg.localModel)
    val downloading = ModelManager.downloading
    val got = ModelManager.downloaded
    val ready = ModelManager.isReady(ctx, model)
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    fun askNotif() { if (android.os.Build.VERSION.SDK_INT >= 33) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    JCard {
        Text("Motore di Jarvis", style = T.Title2)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Sul telefono", cfg.chatEngine == 0) { Store.update { it.copy(ai = it.ai.copy(chatEngine = 0)) } }
            Pill("Cloud gratuito", cfg.chatEngine == 1) { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1)) } }
        }
        Spacer(Modifier.height(10.dp))
        if (cfg.chatEngine == 0) {
            Text("Modello open source (Apache-2.0) che gira sul tuo telefono: nessun limite di domande, nessuna attesa di rete, " +
                    "i tuoi dati non escono dal dispositivo. Si scarica una volta sola.", style = T.Muted)
            Spacer(Modifier.height(10.dp))
            LocalModel.entries.forEach { m ->
                Choice("${m.title} · ${gb(m.bytes)} — ${m.note}", cfg.localModel == m.ordinal) {
                    if (downloading == null) Store.update { it.copy(ai = it.ai.copy(localModel = m.ordinal)) }
                }
            }
            Spacer(Modifier.height(6.dp))
            when {
                ready -> {
                    Text("✓ ${model.title} pronto", style = T.Label.copy(color = A.GreenInk))
                    Spacer(Modifier.height(6.dp))
                    GhostButton("Elimina il modello (libera ${gb(model.bytes)})") { ModelManager.delete(ctx, model) }
                }
                downloading == model -> {
                    ProgressLine((got.toFloat() / model.bytes), C.Accent)
                    Text("Download ${gb(got)} di ${gb(model.bytes)}. Continua anche se chiudi l'app o blocchi lo schermo: lo vedi nella notifica. Se si interrompe riparte da dove era.", style = T.Small,
                        modifier = Modifier.padding(vertical = 6.dp))
                    GhostButton("Annulla") { com.jarvis.food.chat.ModelDownloadService.cancel(ctx) }
                }
                else -> {
                    val part = ModelManager.partial(ctx, model)
                    PrimaryButton(if (part > 0) "Riprendi il download (${gb(model.bytes - part)} rimanenti)" else "Scarica ${model.title} (${gb(model.bytes)})",
                        Modifier.fillMaxWidth()) { askNotif(); com.jarvis.food.chat.ModelDownloadService.start(ctx, model) }
                    Text("Gratuito, senza account. Usa il Wi-Fi: sono diversi gigabyte.", style = T.Small, modifier = Modifier.padding(top = 6.dp))
                }
            }
            ModelManager.error?.let { Text(it, style = T.Small.copy(color = C.Danger), modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(6.dp))
            SwitchRow("Accelerazione GPU (sperimentale)", "Più veloce su alcuni telefoni; se la chat si blocca, disattivala.", cfg.chatGpu) { v ->
                com.jarvis.food.chat.LocalLlm.unload()
                Store.update { it.copy(ai = it.ai.copy(chatGpu = v)) }
            }
        } else {
            Text("Usa i modelli gratuiti di OpenRouter (Gemma 4 e altri). Sono più potenti ma hanno un limite giornaliero di richieste: " +
                    "per domande illimitate usa il motore sul telefono.", style = T.Muted)
            Spacer(Modifier.height(8.dp))
            Text(if (cfg.apiKey.isBlank()) "Manca la chiave gratuita." else "Chiave configurata ✓", style = T.Label.copy(color = if (cfg.apiKey.isBlank()) C.Danger else C.Green))
            Spacer(Modifier.height(6.dp))
            GhostButton("Impostazioni chiave") { Nav.go(Screen.AiSettings) }
        }
    }
}
