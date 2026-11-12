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
import androidx.compose.foundation.layout.isImeVisible
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

    LaunchedEffect(s.activeChatId, s.chat.size, partial.length, busy) {
        val extra = if (busy) 1 else 0
        val n = s.chat.size + extra
        if (n > 0) listState.scrollToItem(n - 1)
    }

    LaunchedEffect(ready, s.ai.chatEngine, s.ai.localModel) {
        if (ready && s.ai.chatEngine == 0) com.jarvis.food.chat.LocalLlm.warm(ctx, s.ai)
    }

    fun send(t: String) { if (t.isNotBlank() && !busy) { Jarvis.send(ctx, t); input = "" } }

    val active = s.chat.isNotEmpty()
    val last = s.chat.lastOrNull()
    val suggestions = if (ready && !busy && active && last?.pending.isNullOrEmpty()) {
        if (last?.role == "assistant" && last.suggestions.isNotEmpty()) last.suggestions
        else com.jarvis.food.chat.Actions.smartSuggestions(s).take(4)
    } else emptyList()
    fun pick(text: String) { if (text.endsWith("…")) input = text.removeSuffix("…") + " " else send(text) }

    Column(Modifier.fillMaxSize().background(C.Bg).imePadding()) {
        ChatHeader(active, showSetup, { Jarvis.clear(); input = ""; showSetup = false }, { showSetup = !showSetup }, hero = !active && ready)
        if (showSetup || (!ready && !active)) {
            // Keep the motor settings scrollable without squeezing the composer.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) { ChatSetup() }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("chat-messages"), state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = if (active) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (active) 18.dp else 10.dp)) {
                if (!active) item { ChatIntro(ready, ::pick) }
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
private const val NEW_CHAT_ICON = "M21 11a9 9 0 0 1-9 9H4l1.5-4A9 9 0 1 1 21 11zM12 7v8M8 11h8"
private const val ENGINE_ICON = "M6 6h12v12H6zM10 10h4v4h-4zM9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatHeader(active: Boolean, setup: Boolean, onNew: () -> Unit, onSetup: () -> Unit, hero: Boolean = false) {
    // Con la tastiera aperta il banner si riduce a una riga: più spazio ai messaggi.
    val typing = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    Column(Modifier.fillMaxWidth().background(C.Bg).statusBarsPadding().testTag("chat-header")) {
        if (typing && active && !setup) {
            RefBannerFrame(true) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(start = 6.dp).size(36.dp), contentAlignment = Alignment.Center) { JarvisHeadLogo(32.dp) }
                    GapW(10.dp)
                    Text("Jarvis", style = T.Title.copy(fontSize = 22.sp, lineHeight = 28.sp, color = Color.White), modifier = Modifier.weight(1f))
                    Box(Modifier.size(48.dp).semantics { contentDescription = "Nuova chat" }.clickable(role = Role.Button, onClick = onNew), contentAlignment = Alignment.Center) {
                        AIcon(RIc.ChatPlus, Color.White, 24.dp, 2f)
                    }
                }
            }
        } else {
            NavyBanner("Jarvis", listOf("Nuova chat", "Modello", "Cronologia"), if (setup) 1 else 0,
                onSelect = { when (it) { 0 -> onNew(); 1 -> onSetup(); 2 -> Nav.go(Screen.ChatHistory) } },
                query = "", onQuery = {}, hint = "", supportingText = "Pasti, ricette, piano e spesa: scrivimi qui sotto.",
                icons = listOf(RIc.ChatPlus, RIc.Chip, RIc.History), logo = { JarvisHeadLogo(RD.BannerLogo) },
                descriptions = listOf("Nuova chat", if (setup) "Chiudi impostazioni motore" else "Motore della chat", "Cronologia delle chat"),
                accounts = true, onBack = if (Nav.stack.isNotEmpty()) ({ Nav.back() }) else null,
                actions = true)
            if (hero && !setup) ChatHero()
        }
    }
}

/** Smaller artwork and 40 dp surfaces, with a full 48 dp accessible touch target. */
@Composable
private fun ChatHeaderAction(icon: String, label: String, ink: Color = C.Navy, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(Modifier.size(48.dp).semantics { contentDescription = label }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp).aShadow(shape, 4.dp).clip(shape)
            .background(if (selected) C.Navy else C.Surface)
            .border(1.dp, if (selected) C.Navy else C.Navy.copy(alpha = .12f), shape), contentAlignment = Alignment.Center) {
            AIcon(icon, if (selected) Color.White else ink, 22.dp, 1.9f, modifier = Modifier.testTag("header-icon-$label"))
        }
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
    val canSend = input.isNotBlank() && !busy
    val shape = RoundedCornerShape(28.dp)
    Row(Modifier.fillMaxWidth().testTag("chat-composer").then(if (Nav.stack.isNotEmpty()) Modifier.navigationBarsPadding() else Modifier)
        .padding(start = 12.dp, end = 12.dp, top = if (active) 12.dp else 4.dp, bottom = if (active) 10.dp else 4.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input, onValueChange = onInput,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp).lightShadow(10.dp, 28.dp).testTag("chat-input"),
            placeholder = { Text("Scrivi a Jarvis…", style = T.Read.copy(color = C.Muted, fontSize = 14.sp, lineHeight = 19.sp), maxLines = 2) },
            textStyle = T.Read.copy(fontSize = 14.sp, lineHeight = 19.sp), shape = shape, maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFBFD0F5), unfocusedBorderColor = Color(0xFFE3E8F2), cursorColor = C.Accent,
                focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface, disabledContainerColor = C.SurfaceAlt, disabledBorderColor = C.Line
            )
        )
        GapW(10.dp)
        ChatSendControl(canSend, busy, 52.dp, onSend, onStop)
    }
}

@Composable
private fun ChatSendControl(canSend: Boolean, busy: Boolean, size: Dp, onSend: () -> Unit, onStop: () -> Unit) {
    Box(Modifier.size(size).orangeGlow(CircleShape, 8.dp).clip(CircleShape)
        .background(C.OrangeBrush)
        .semantics { contentDescription = if (busy) "Interrompi risposta" else "Invia messaggio" }
        .clickable(enabled = canSend || busy, role = Role.Button) { if (busy) onStop() else onSend() }, contentAlignment = Alignment.Center) {
        if (busy) Canvas(Modifier.size(22.dp)) {
            val w = this.size.width; val h = this.size.height
            drawRoundRect(Color.White, Offset(w * 0.25f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
        } else AIcon(Ic.Plane, if (canSend) Color.White else Color.White.copy(alpha = .7f), 24.dp, 2.2f)
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

/** Fascia blu che sfuma nella pagina, con la mascotte che saluta e il suo alone. */
@Composable
private fun ChatHero() {
    Box(Modifier.fillMaxWidth().height(176.dp).testTag("chat-hero")
        .background(Brush.verticalGradient(listOf(Color(0xFF0E2A78), C.Bg))), contentAlignment = Alignment.BottomCenter) {
        MascotChat(164.dp)
        Box(Modifier.fillMaxWidth().height(26.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg))))
    }
}

@Composable
private fun ChatIntro(ready: Boolean, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 0.dp)) {
        val card = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().lightShadow(14.dp, 22.dp).clip(card).background(Color.White).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Ciao! Sono Jarvis 👋", style = T.Title.copy(fontSize = 22.sp, lineHeight = 28.sp, color = RD.Ink))
            Text("Il tuo assistente per pasti, ricette, piano e spesa. Chiedimi qui.",
                style = T.Muted.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine), modifier = Modifier.padding(top = 4.dp))
        }
        if (ready) {
            Spacer(Modifier.height(12.dp))
            val tile = RoundedCornerShape(18.dp)
            CAPABILITIES.forEachIndexed { index, (_, title, example) ->
                val (icon, tone) = when (title) {
                    "Diario" -> Ic.Bowl to Tone.Orange; "Ricette" -> Ic.Receipt to Tone.Pink; "Piano" -> Ic.Calendar to Tone.Blue
                    "Spesa e dispensa" -> Ic.Cart to Tone.Teal; "Obiettivi" -> RIc.Target to Tone.Purple; else -> Ic.Timer to Tone.Indigo
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(if (index == CAPABILITIES.lastIndex) Modifier.testTag("chat-capabilities-last-row") else Modifier)
                    .heightIn(min = 64.dp).lightShadow(8.dp, 18.dp).clip(tile).background(Color.White)
                    .clickable(role = Role.Button) { onPick(example) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    GlowIcon(icon, tone.first, tone.second, 40.dp, 13.dp)
                    GapW(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = T.Label.copy(fontSize = 14.5.sp, lineHeight = 18.sp, color = RD.Ink, fontWeight = FontWeight.Bold))
                        Text("“$example”", style = T.Small.copy(fontSize = 12.5.sp, lineHeight = 17.sp))
                    }
                    AIcon(Ic.Right, C.Muted, 18.dp, 2.2f)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(24.dp).testTag("chat-intro-bottom"))
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
    val shape = RoundedCornerShape(16.dp)
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
            Box(Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.82f).coerceAtMost(420f).dp).clip(shape).background(Color(0xFFDCE8FF)).padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(m.text, style = T.Read.copy(color = RD.Ink))
            }
        } else AssistantCard { ChatResponseText(m.text) }
        if (m.pending.isNotEmpty()) {
            val shape = RoundedCornerShape(14.dp)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Danger.copy(alpha = 0.35f), shape).padding(14.dp)) {
                Text("Conferma eliminazione", style = T.Label.copy(color = C.Danger))
                m.pending.forEach { p -> Text("• " + p.label, style = T.Body, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.heightIn(min = 48.dp).weight(1f).clip(RoundedCornerShape(16.dp)).background(C.Danger).clickable(enabled = !Jarvis.busy) { Jarvis.confirm(m.id) }.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("Sì, elimina", style = T.BtnS.copy(color = Color.White))
                    }
                    Box(Modifier.heightIn(min = 48.dp).weight(1f).clip(RoundedCornerShape(16.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(16.dp))
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
            val shape = RoundedCornerShape(16.dp)
            Row(Modifier.heightIn(min = 48.dp).clip(shape)
                .then(if (primary) Modifier.background(C.NavyBrush) else Modifier.background(C.Surface).border(1.dp, C.Navy.copy(alpha = 0.3f), shape))
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
    SettingsCard(title = "Motore di Jarvis", subtitle = "Scegli dove elaborare le conversazioni") {
        SettingsSegment(listOf("Sul telefono", "Cloud gratuito"), cfg.chatEngine) { engine ->
            Store.update { it.copy(ai = it.ai.copy(chatEngine = engine)) }
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
