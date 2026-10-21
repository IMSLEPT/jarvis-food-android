package com.jarvis.food.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jarvis.food.ai.AiItem
import com.jarvis.food.ai.AiLabel
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Carica un'immagine ridotta (max ~1600 px) già ruotata correttamente. */
fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 1600): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 28) {
        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { dec, info, _ ->
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width; val h = info.size.height
            val scale = maxSide.toFloat() / maxOf(w, h)
            if (scale < 1f) dec.setTargetSize((w * scale).toInt(), (h * scale).toInt())
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}.getOrNull()

private class EditableItem(item: AiItem) {
    val base = item
    var selected by mutableStateOf(true)
    var grams by mutableStateOf(item.grams.toInt().toString())
}

@Composable
fun PhotoAiScreen(date: LocalDate, meal: Int) {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<EditableItem>() }
    val labels = remember { mutableStateListOf<AiLabel>() }
    val offlineOk = remember { FoodAi.offlineAvailable(ctx) }

    val photoFile = remember { File(ctx.cacheDir, "foto").apply { mkdirs() }.let { File(it, "pasto.jpg") } }
    val photoUri = remember { FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", photoFile) }

    fun reset() { items.clear(); labels.clear(); error = null; note = "" }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) scope.launch {
            reset()
            bitmap = withContext(Dispatchers.IO) { loadBitmap(ctx, photoUri) }
            if (bitmap == null) error = "Impossibile leggere la foto."
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            reset()
            bitmap = withContext(Dispatchers.IO) { loadBitmap(ctx, uri) }
            if (bitmap == null) error = "Impossibile leggere l'immagine."
        }
    }

    fun runOnline() {
        val b = bitmap ?: return
        busy = true; reset()
        scope.launch {
            runCatching { FoodAi.analyzeMeal(s.ai, com.jarvis.food.data.Db.db, b, hint) }
                .onSuccess { (list, n) ->
                    items.addAll(list.map { EditableItem(it) }); note = n
                    if (list.isEmpty()) error = n.ifBlank { "Nessun alimento riconosciuto." }
                }
                .onFailure { error = it.message ?: "Errore sconosciuto" }
            busy = false
        }
    }

    fun runOffline() {
        val b = bitmap ?: return
        busy = true; reset()
        scope.launch {
            runCatching { FoodAi.analyzeOffline(ctx, b) }
                .onSuccess { labels.addAll(it); if (it.isEmpty()) error = "Nessun piatto riconosciuto." }
                .onFailure { error = it.message }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Foto AI · ${MEAL_NAMES[meal]}", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val b = bitmap
            if (b == null) {
                JCard {
                    Text("📸", style = T.Big)
                    Text("Fotografa il tuo piatto", style = T.Title)
                    Text("L'AI riconosce gli alimenti e stima porzioni e calorie. Controlla sempre i valori: sono stime.", style = T.Muted)
                }
            } else {
                Image(
                    b.asImageBitmap(), "Foto del pasto",
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Scatta", Modifier.weight(1f)) { camera.launch(photoUri) }
                GhostButton("Galleria", Modifier.weight(1f)) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }

            if (b != null) {
                Spacer(Modifier.height(16.dp))
                JField(hint, { hint = it }, "Dettagli utili (facoltativo, es. \"pasta 100 g cruda\")")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("✨ Analizza online", Modifier.weight(1f), enabled = !busy) { runOnline() }
                    GhostButton("Offline", Modifier.weight(0.7f)) { if (!busy) runOffline() }
                }
                Text(
                    if (s.ai.apiKey.isBlank()) "Online: inserisci la chiave gratuita in Profilo → AI foto. Offline: riconosce il piatto senza internet."
                    else "Online: ${s.ai.model}; i valori nutrizionali vengono dal database USDA. Offline: " + if (offlineOk) "modello sul telefono (nomi in inglese)." else "modello non incluso.",
                    style = T.Small, modifier = Modifier.padding(top = 6.dp)
                )
                if (s.ai.apiKey.isBlank()) Text(
                    "Configura AI online", style = T.Label.copy(color = C.Accent),
                    modifier = Modifier.padding(top = 4.dp).clickable { Nav.go(Screen.AiSettings) }
                )
            }

            if (busy) Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), color = C.Accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp)); Text("Analisi in corso…", style = T.Muted)
            }
            error?.let { Text(it, style = T.Label.copy(color = C.Danger), modifier = Modifier.padding(vertical = 12.dp)) }

            if (items.isNotEmpty()) {
                SectionTitle("Alimenti riconosciuti")
                items.forEach { it2 ->
                    val g = parseNum(it2.grams) ?: 0.0
                    val dbf = it2.base.foodId?.let { id -> com.jarvis.food.data.Db.db.food(id) }
                    val kc: Double; val pp: Double; val cc: Double; val ff: Double
                    if (dbf != null) { kc = dbf.kcal * g / 100; pp = dbf.protein * g / 100; cc = dbf.carbs * g / 100; ff = dbf.fat * g / 100 }
                    else { val k = if (it2.base.grams > 0) g / it2.base.grams else 0.0; kc = it2.base.kcal * k; pp = it2.base.protein * k; cc = it2.base.carbs * k; ff = it2.base.fat * k }
                    JCard(Modifier.padding(bottom = 8.dp), padding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(it2.selected, { v -> it2.selected = v }, colors = CheckboxDefaults.colors(checkedColor = C.Green, checkmarkColor = C.OnAccent))
                            Column(Modifier.weight(1f)) {
                                Text(it2.base.name.replaceFirstChar { c -> c.uppercase() }, style = T.Label)
                                Text(if (dbf != null) "= ${dbf.name} (USDA)" else "non nel database: stima AI", style = T.Small.copy(color = if (dbf != null) C.Green else C.Gold))
                                Text("${kc.r0()} kcal · P ${pp.r0()} · C ${cc.r0()} · G ${ff.r0()}", style = T.Small)
                            }
                            JField(it2.grams, { v -> it2.grams = v }, "g", Modifier.width(84.dp), number = true)
                        }
                    }
                }
                if (note.isNotBlank()) Text(note, style = T.Small)
            }

            if (labels.isNotEmpty()) {
                SectionTitle("Piatti probabili (offline)")
                Text("Tocca il più corretto per cercarlo e scegliere la porzione.", style = T.Small)
                labels.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface)
                            .clickable { Nav.go(Screen.AddFood(date, meal, l.label)) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(l.label, style = T.Label, modifier = Modifier.weight(1f))
                        Text("${(l.score * 100).toInt()}%", style = T.Small)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (items.any { it.selected }) {
            PrimaryButton("Aggiungi ${items.count { it.selected }} alimenti", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                items.filter { it.selected }.forEach { e ->
                    val g = parseNum(e.grams) ?: e.base.grams
                    val dbf = e.base.foodId?.let { id -> com.jarvis.food.data.Db.db.food(id) }
                    Store.addEntry(date, meal, dbf?.toFood() ?: e.base.toEstimatedFood(), g)
                }
                Nav.backTo { it !is Screen.AddFood && it !is Screen.PhotoAi }
            }
        }
    }
}
