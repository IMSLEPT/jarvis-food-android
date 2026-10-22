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

/** Posizione del pulsante flottante (rispetto all'angolo in basso a destra): resta dove l'utente lo trascina. */
private object FabPos {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
}

/** Pulsante flottante che apre la chat con Jarvis. Si può trascinare per liberare il contenuto sotto. */
@Composable
fun ChatFab(modifier: Modifier = Modifier) {
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current
    val maxX = with(density) { (cfg.screenWidthDp.dp - 80.dp).toPx() }
    val maxY = with(density) { (cfg.screenHeightDp.dp - 260.dp).toPx() }
    Box(
        modifier.offset { IntOffset(FabPos.x.roundToInt(), FabPos.y.roundToInt()) }
            .size(52.dp).shadow(8.dp, CircleShape, ambientColor = Color(0x5517286B), spotColor = Color(0x5517286B))
            .clip(CircleShape).background(C.Surface).border(2.dp, C.Accent, CircleShape)
            .pointerInput(maxX, maxY) {
                detectDragGestures { change, drag ->
                    change.consume()
                    FabPos.x = (FabPos.x + drag.x).coerceIn(-maxX, 0f)
                    FabPos.y = (FabPos.y + drag.y).coerceIn(-maxY, 0f)
                }
            }
            .clickable { Nav.go(Screen.Chat) },
        contentAlignment = Alignment.Center
    ) { JarvisLogo(44.dp) }
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
        TopBar("Jarvis", onBack = { Nav.back() }) {
            Text("Nuova", style = T.Label.copy(color = C.Navy), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Jarvis.clear() }.padding(10.dp))
            Text(if (showSetup) "Chiudi" else "Motore", style = T.Label.copy(color = C.Navy),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showSetup = !showSetup }.padding(10.dp))
        }
        if (showSetup || !ready) Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { ChatSetup() }

        if (ready || s.chat.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (s.chat.isEmpty()) item { ChatIntro(ready) { send(it) } }
                items(s.chat, key = { it.id }) { m -> Bubble(m) }
                if (busy) item { Typing(partial, Jarvis.status) }
            }
        } else Spacer(Modifier.weight(1f))

        val last = s.chat.lastOrNull()
        if (ready && !busy && last != null && last.role == "assistant" && last.suggestions.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                last.suggestions.forEach { q -> Pill(q, false) { send(q) } }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), enabled = ready,
                placeholder = { Text(if (ready) "Scrivi a Jarvis…" else "Configura il motore per iniziare", style = T.Muted) },
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

@Composable
private fun ChatIntro(ready: Boolean, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        JarvisLogo(84.dp)
        Spacer(Modifier.height(10.dp))
        Text("Ciao, sono Jarvis", style = T.Title)
        Text("Chiedimi consigli su calorie e macro, oppure dimmi cosa hai mangiato: lo registro io. " +
                "Se mi manca qualcosa te lo chiedo.", style = T.Muted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        if (ready) {
            Spacer(Modifier.height(10.dp))
            listOf("Ho mangiato 150 g di pollo a pranzo", "Quante kcal mi restano oggi?", "Che macro mi conviene?", "Aggiungi 500 ml di acqua").forEach {
                Box(Modifier.padding(vertical = 4.dp)) { GhostButton(it) { onPick(it) } }
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
        if (m.actions.isNotEmpty()) Column(Modifier.padding(start = if (mine) 0.dp else 36.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            m.actions.forEach { a ->
                val ok = a.startsWith("✓")
                val warn = a.startsWith("⚠")
                Text(a, style = T.Small.copy(color = if (ok) C.Green else if (warn) C.Danger else C.Muted),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (ok) C.GreenSoft else if (warn) C.DangerSoft else C.SurfaceAlt)
                        .padding(horizontal = 10.dp, vertical = 5.dp))
            }
            if (Jarvis.canUndo(m.id) && !Jarvis.busy) Text("Annulla le modifiche", style = T.Label.copy(color = C.Accent),
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
                    Text("✓ ${model.title} pronto", style = T.Label.copy(color = C.Green))
                    Spacer(Modifier.height(6.dp))
                    GhostButton("Elimina il modello (libera ${gb(model.bytes)})") { ModelManager.delete(ctx, model) }
                }
                downloading == model -> {
                    ProgressLine((got.toFloat() / model.bytes), C.Accent)
                    Text("Download ${gb(got)} di ${gb(model.bytes)}. Tieni l'app aperta; se si interrompe riparte da dove era.", style = T.Small,
                        modifier = Modifier.padding(vertical = 6.dp))
                    GhostButton("Annulla") { ModelManager.cancel() }
                }
                else -> {
                    val part = ModelManager.partial(ctx, model)
                    PrimaryButton(if (part > 0) "Riprendi il download (${gb(model.bytes - part)} rimanenti)" else "Scarica ${model.title} (${gb(model.bytes)})",
                        Modifier.fillMaxWidth()) { ModelManager.start(ctx, model) }
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
