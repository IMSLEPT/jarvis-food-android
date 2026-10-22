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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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

private class ItemCalc(val kcal: Double, val p: Double, val c: Double, val f: Double, val usda: String?)

private fun calc(e: EditableItem): ItemCalc {
    val g = parseNum(e.grams) ?: 0.0
    val dbf = e.base.foodId?.let { id -> com.jarvis.food.data.Db.db.food(id) }
    return if (dbf != null) ItemCalc(dbf.kcal * g / 100, dbf.protein * g / 100, dbf.carbs * g / 100, dbf.fat * g / 100, dbf.name)
    else {
        val k = if (e.base.grams > 0) g / e.base.grams else 0.0
        ItemCalc(e.base.kcal * k, e.base.protein * k, e.base.carbs * k, e.base.fat * k, null)
    }
}

/** Angoli "mirino" del design, disegnati sopra la foto. */
@Composable
private fun Brackets(modifier: Modifier) {
    Canvas(modifier) {
        val inset = 18.dp.toPx(); val len = 38.dp.toPx(); val sw = 4.dp.toPx(); val r = 14.dp.toPx()
        val w = size.width; val h = size.height
        val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            val p = Path().apply {
                moveTo(x, y + sy * len); lineTo(x, y + sy * r); quadraticBezierTo(x, y, x + sx * r, y); lineTo(x + sx * len, y)
            }
            drawPath(p, Color.White, style = st)
        }
        corner(inset, inset, 1f, 1f); corner(w - inset, inset, -1f, 1f)
        corner(inset, h - inset, 1f, -1f); corner(w - inset, h - inset, -1f, -1f)
    }
}

@Composable
private fun GramStep(plus: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFEBEFF3)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(if (plus) "M12 6v12M6 12h12" else "M6 12h12", A.Ink, 16.dp, 2.6f)
    }
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
    var online by remember { mutableStateOf(true) }
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

    val sel = items.filter { it.selected }
    val tot = sel.map { calc(it) }
    val photoShape = RoundedCornerShape(34.dp)

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackCircle { Nav.back() }
            GapW(12.dp)
            Column {
                Text("Foto AI · ${MEAL_NAMES[meal]}", style = T.Body.copy(fontSize = 13.sp, color = A.Muted))
                Text("Fotografa il tuo piatto", style = T.H1.copy(fontSize = 26.sp, lineHeight = 30.sp), maxLines = 1)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val b = bitmap
            Box(
                Modifier.padding(top = 6.dp).fillMaxWidth().height(330.dp).aShadow(photoShape, 14.dp, Color(0x80E88C60)).clip(photoShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFE9C2), Color(0xFFFFF4E3), Color(0xFFFFE4D6))))
            ) {
                if (b != null) Image(b.asImageBitmap(), "Foto del pasto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    AIcon(Ic.Camera, Color(0xFFB4381E), 54.dp, 1.8f)
                    Text("Scatta o scegli una foto", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A4A2E)), modifier = Modifier.padding(top = 10.dp))
                }
                Brackets(Modifier.fillMaxSize())
                if (items.isNotEmpty()) Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 32.dp).clip(RoundedCornerShape(50)).background(Color(0xFF047857)).padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AIcon(Ic.Check, Color.White, 16.dp, 3f)
                    GapW(8.dp)
                    Text("${items.size} ${if (items.size == 1) "alimento riconosciuto" else "alimenti riconosciuti"}", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                }
            }

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(C.Navy).clickable { camera.launch(photoUri) },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    AIcon(Ic.Camera, Color(0xFFFFB820), 22.dp, 2f); GapW(10.dp)
                    Text("Scatta", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                }
                Row(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(27.dp))
                        .clickable { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    AIcon(PhIc.Image, A.Ink, 22.dp, 2f); GapW(10.dp)
                    Text("Galleria", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                }
            }

            // Online / offline: toccando si lancia l'analisi con quel metodo
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(30.dp)).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.weight(1.4f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (online) C.Navy else Color.Transparent)
                        .clickable(enabled = !busy) { online = true; runOnline() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    AIcon(Ic.Spark, if (online) Color(0xFFFFB820) else A.Ink, 20.dp, 2f); GapW(8.dp)
                    Text("Analizza online", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = if (online) Color.White else A.Ink), maxLines = 1)
                }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (!online) C.Navy else Color.Transparent)
                        .clickable(enabled = !busy) { online = false; runOffline() },
                    contentAlignment = Alignment.Center
                ) { Text("Offline", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = if (!online) Color.White else A.Ink)) }
            }
            Text(
                when {
                    bitmap == null -> "Scatta o scegli una foto, poi tocca Analizza online oppure Offline."
                    s.ai.apiKey.isBlank() && online -> "Online: inserisci la chiave gratuita in Profilo → Intelligenza artificiale. Offline: riconosce il piatto senza internet."
                    online -> "Online: ${s.ai.model}; i valori nutrizionali vengono dal database USDA. Controlla sempre i valori: sono stime."
                    offlineOk -> "Offline: modello sul telefono (nomi in inglese)."
                    else -> "Offline: modello non incluso."
                },
                style = T.Body.copy(fontSize = 12.5.sp, lineHeight = 18.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)
            )
            if (s.ai.apiKey.isBlank()) Text("Configura AI online", style = T.Link, modifier = Modifier.padding(start = 4.dp).clickable { Nav.go(Screen.AiSettings) }.padding(vertical = 8.dp))
            if (b != null) ACard(Modifier.padding(top = 8.dp).fillMaxWidth(), radius = 24.dp, padding = 12.dp, elevation = 6.dp) {
                JField(hint, { hint = it }, "Dettagli utili (facoltativo, es. \"pasta 100 g cruda\")")
            }

            if (busy) Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), color = C.Accent, strokeWidth = 2.dp)
                GapW(12.dp); Text("Analisi in corso…", style = T.Muted)
            }
            error?.let { Text(it, style = T.Label.copy(color = C.Danger), modifier = Modifier.padding(vertical = 12.dp)) }

            if (items.isNotEmpty()) {
                val kcal = tot.sumOf { it.kcal }
                val heroShape = RoundedCornerShape(34.dp)
                Column(
                    Modifier.padding(top = 14.dp).fillMaxWidth().aShadow(heroShape, 14.dp, Color(0x99E88C60)).clip(heroShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDF1F5)))).border(1.dp, Color(0xE6FFFFFF), heroShape).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(kcalFmt(kcal), style = T.H1.copy(fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.7).sp))
                        Text("kcal", style = T.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = A.Muted), modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
                    }
                    Box(Modifier.padding(top = 10.dp)) { MacroChips(tot.sumOf { it.p }, tot.sumOf { it.c }, tot.sumOf { it.f }) }
                }
                GHead("Alimenti riconosciuti")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { it2 ->
                        val cc = calc(it2)
                        val g = parseNum(it2.grams) ?: 0.0
                        fun step(d: Int) {
                            val st = if (g < 50) 5 else if (g < 200) 10 else 25
                            it2.grams = (g + d * st).coerceAtLeast(0.0).toInt().toString()
                        }
                        ACard(Modifier.fillMaxWidth(), radius = 24.dp, padding = 12.dp, elevation = 6.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(if (it2.selected) Color(0xFF10B981) else Color(0xFFEBEFF3)).clickable { it2.selected = !it2.selected },
                                    contentAlignment = Alignment.Center
                                ) { if (it2.selected) AIcon(Ic.Check, Color.White, 20.dp, 3f) }
                                GapW(14.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(it2.base.name.replaceFirstChar { c -> c.uppercase() }, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), maxLines = 1)
                                    Text("${kcalFmt(cc.kcal)} kcal" + if (cc.usda == null) " · stima AI" else "", style = T.Small.copy(fontSize = 12.sp, color = if (cc.usda == null) A.FatInk else A.Muted), maxLines = 1)
                                }
                                GramStep(false) { step(-1) }
                                Text("${g.toInt()} g", style = T.H1.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center, modifier = Modifier.width(62.dp), maxLines = 1)
                                GramStep(true) { step(1) }
                            }
                        }
                    }
                }
                if (note.isNotBlank()) Text(note, style = T.Small.copy(color = A.Muted), modifier = Modifier.padding(start = 4.dp, top = 10.dp))
            }

            if (labels.isNotEmpty()) {
                GHead("Piatti probabili (offline)", "Tocca il più corretto per cercarlo e scegliere la porzione.")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { l ->
                        ACard(Modifier.fillMaxWidth(), radius = 22.dp, padding = 14.dp, elevation = 6.dp, onClick = { Nav.go(Screen.AddFood(date, meal, l.label)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(l.label, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                                Text("${(l.score * 100).toInt()}%", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.Muted))
                            }
                        }
                    }
                }
            }
            GapH(24.dp)
        }
        if (sel.isNotEmpty()) {
            val barShape = RoundedCornerShape(34.dp)
            Row(
                Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().aShadow(barShape, 14.dp, Color(0x8017286B)).clip(barShape).background(C.Navy).padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${sel.size} ${if (sel.size == 1) "alimento" else "alimenti"}", style = T.Body.copy(fontSize = 12.sp, color = Color(0xCCFFFFFF)))
                    Text("${kcalFmt(tot.sumOf { it.kcal })} kcal", style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp, color = Color.White))
                }
                val addShape = RoundedCornerShape(50)
                Row(
                    Modifier.height(54.dp).clip(addShape).background(C.OrangeBrush).clickable {
                        sel.forEach { e ->
                            val g = parseNum(e.grams) ?: e.base.grams
                            val dbf = e.base.foodId?.let { id -> com.jarvis.food.data.Db.db.food(id) }
                            Store.addEntry(date, meal, dbf?.toFood() ?: e.base.toEstimatedFood(), g)
                        }
                        Nav.backTo { it !is Screen.AddFood && it !is Screen.PhotoAi }
                    }.padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aggiungi", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    GapW(8.dp)
                    AIcon(Ic.Check, Color.White, 20.dp, 3f)
                }
            }
        }
    }
}

private object PhIc {
    val Image = Ic.rr(4.0, 4.0, 16.0, 16.0, 3.0) + Ic.circ(9.0, 9.0, 1.6) + "M4 17l5-5 4 4 3-3 4 4"
}
