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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.food.R
import com.jarvis.food.chat.Jarvis
import com.jarvis.food.chat.LocalModel
import com.jarvis.food.chat.ModelManager
import com.jarvis.food.data.ChatMsg
import com.jarvis.food.data.Store
import java.util.Locale

private fun gb(b: Long) = String.format(Locale.ITALY, "%.1f GB", b / 1_000_000_000.0)

@Composable
fun JarvisLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.jarvis_logo), "Jarvis", modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
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

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Jarvis", onBack = if (Nav.stack.isNotEmpty()) ({ Nav.back() }) else null) {
            Text("Nuova", style = T.Label.copy(color = C.Navy), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Jarvis.clear() }.padding(10.dp))
            Text(if (showSetup) "Chiudi" else "Motore", style = T.Label.copy(color = C.Navy),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showSetup = !showSetup }.padding(10.dp))
        }
        if (showSetup || !ready) {
            // Impostazioni del motore: scorrevoli, così la barra di scrittura resta sempre visibile e non viene schiacciata.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) { ChatSetup() }
        } else if (ready || s.chat.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (s.chat.isEmpty()) item { ChatIntro(ready, com.jarvis.food.chat.Actions.smartSuggestions(s).take(5)) { if (it.endsWith("…")) input = it.removeSuffix("…") + " " else send(it) } }
                items(s.chat, key = { it.id }) { m -> Bubble(m) }
                if (busy) item { Typing(partial, Jarvis.status) }
            }
        } else Spacer(Modifier.weight(1f))

        val last = s.chat.lastOrNull()
        if (ready && !busy && s.chat.isNotEmpty() && last?.pending.isNullOrEmpty()) {
            val chips = if (last != null && last.role == "assistant" && last.suggestions.isNotEmpty()) last.suggestions
            else com.jarvis.food.chat.Actions.smartSuggestions(s).take(4)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { q -> Pill(q, false) { if (q.endsWith("…")) input = q.removeSuffix("…") + " " else send(q) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = ready,
                placeholder = { Text(if (ready) "Scrivi a Jarvis…" else "Scarica o scegli un motore", style = T.Muted, maxLines = 1) },
                textStyle = T.Body, shape = RoundedCornerShape(26.dp), maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Navy, unfocusedBorderColor = C.Line, cursorColor = C.Accent,
                    focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface, disabledContainerColor = C.SurfaceAlt, disabledBorderColor = C.Line
                )
            )
            Spacer(Modifier.size(8.dp))
            val can = ready && input.isNotBlank() && !busy
            Box(Modifier.size(50.dp).clip(CircleShape).background(if (busy) C.Navy else if (can) C.Accent else C.SurfaceHi)
                .clickable(enabled = can || busy) { if (busy) Jarvis.stop() else send(input) }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val w = size.width; val h = size.height
                    if (busy) drawRoundRect(Color.White, Offset(w * 0.25f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f),
                        androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                    else {
                        val st = 2.4.dp.toPx(); val col = if (can) Color.White else C.Faint
                        drawLine(col, Offset(w / 2, h * 0.88f), Offset(w / 2, h * 0.14f), st, StrokeCap.Round)
                        drawLine(col, Offset(w / 2, h * 0.12f), Offset(w * 0.18f, h * 0.45f), st, StrokeCap.Round)
                        drawLine(col, Offset(w / 2, h * 0.12f), Offset(w * 0.82f, h * 0.45f), st, StrokeCap.Round)
                    }
                }
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
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(listOf(A.Tonal, A.GreenSoft, A.FastSoft)[rowIndex % 3]).border(1.dp, C.Line, RoundedCornerShape(18.dp))
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

@Composable
private fun Bubble(m: ChatMsg) {
    val mine = m.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.Top) {
            if (!mine) { JarvisLogo(28.dp, Modifier.padding(top = 2.dp)); Spacer(Modifier.size(8.dp)) }
            val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp)
            Box(Modifier.widthIn(max = 300.dp).clip(shape)
                .let { if (mine) it.background(C.Navy) else it.background(C.Surface).border(1.dp, C.Line, shape) }
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(m.text, style = T.Body.copy(color = if (mine) Color.White else C.Text))
            }
        }
        if (m.pending.isNotEmpty()) {
            val shape = RoundedCornerShape(18.dp)
            Column(Modifier.padding(start = 36.dp, top = 6.dp).widthIn(max = 320.dp).clip(shape).background(C.DangerSoft).border(1.dp, C.Danger.copy(alpha = 0.35f), shape).padding(14.dp)) {
                Text("Conferma eliminazione", style = T.Label.copy(color = C.Danger))
                m.pending.forEach { p -> Text("• " + p.label, style = T.Body, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.height(44.dp).weight(1f).clip(RoundedCornerShape(22.dp)).background(C.Danger).clickable(enabled = !Jarvis.busy) { Jarvis.confirm(m.id) }, contentAlignment = Alignment.Center) {
                        Text("Sì, elimina", style = T.BtnS.copy(color = Color.White), maxLines = 1)
                    }
                    Box(Modifier.height(44.dp).weight(1f).clip(RoundedCornerShape(22.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(22.dp))
                        .clickable(enabled = !Jarvis.busy) { Jarvis.cancelPending(m.id) }, contentAlignment = Alignment.Center) {
                        Text("Annulla", style = T.BtnS, maxLines = 1)
                    }
                }
            }
        }
        if (m.actions.isNotEmpty()) Column(Modifier.padding(start = if (mine) 0.dp else 36.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            m.actions.forEach { a ->
                val ok = a.startsWith("✓")
                val warn = a.startsWith("⚠")
                Text(a, style = T.Small.copy(color = if (ok) C.Green else if (warn) C.Danger else C.Muted),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (ok) C.GreenSoft else if (warn) C.DangerSoft else C.SurfaceAlt)
                        .padding(horizontal = 10.dp, vertical = 5.dp))
            }
            if (Jarvis.canUndo(m.id) && !Jarvis.busy) Text("Annulla le modifiche", style = T.Label.copy(color = C.AccentDeep),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Jarvis.undo(m.id) }.padding(vertical = 6.dp, horizontal = 2.dp))
        }
    }
}

@Composable
private fun Typing(partial: String, status: String) {
    Row(verticalAlignment = Alignment.Top) {
        JarvisLogo(28.dp, Modifier.padding(top = 2.dp)); Spacer(Modifier.size(8.dp))
        val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        Column(Modifier.widthIn(max = 300.dp).clip(shape).background(C.Surface).border(1.dp, C.Line, shape).padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (partial.isNotBlank()) Text(partial, style = T.Body)
            Text(status.ifBlank { "…" }, style = T.Small)
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
