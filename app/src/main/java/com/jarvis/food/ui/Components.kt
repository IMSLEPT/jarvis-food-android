package com.jarvis.food.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Formattazione ----------

fun Double.r0() = roundToInt().toString()
fun Double.r1(): String = String.format(Locale.ITALY, "%.1f", this)
fun Int.thousands(): String = String.format(Locale.ITALY, "%,d", this)
fun Long.thousands(): String = String.format(Locale.ITALY, "%,d", this)
fun parseNum(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()

// ---------- Contenitori ----------

@Composable
fun JCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = C.Surface,
    brush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier.fillMaxWidth()
        .aShadow(shape, 10.dp, Color(0x4D1D1B3A))
        .clip(shape).let { if (brush != null) it.background(brush) else it.background(color).border(1.dp, A.Hair, shape) }
    Column(modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding), content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.H2, modifier = Modifier.weight(1f).padding(end = 12.dp))
        if (action != null && onAction != null) {
            Text(action, style = T.Link, modifier = Modifier.clickable(onClick = onAction).padding(vertical = 13.dp))
        }
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { BackCircle(onBack); Spacer(Modifier.width(12.dp)) }
        Text(title, style = T.Title.copy(fontSize = 22.sp, lineHeight = 27.sp), modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

// ---------- Pulsanti ----------

/** Pulsante principale: arancione sfumato con testo bianco. Con [color] diverso dall'arancione diventa pieno (es. blu navy). */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val orange = color == null || color == C.Accent
    val fill: Modifier = when {
        !enabled -> Modifier.background(C.SurfaceHi, shape)
        orange -> Modifier.aShadow(shape, 8.dp, Color(0x66F25E00)).clip(shape).background(C.OrangeBrush, shape)
        else -> Modifier.clip(shape).background(color!!, shape)
    }
    Box(
        modifier.heightIn(min = 52.dp).then(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = T.BtnM.copy(fontWeight = FontWeight.Bold, color = if (enabled) C.OnAccent else C.Muted), textAlign = TextAlign.Center) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.Text, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(C.Surface).border(1.dp, Color(0x1A1D1B3A), RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = T.BtnM.copy(color = color), textAlign = TextAlign.Center) }
}

@Composable
fun Pill(text: String, selected: Boolean, color: Color = C.Ink, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(if (selected) color else C.Surface)
            .border(1.dp, if (selected) A.Ink else Color(0x1A1D1B3A), RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center
    ) { Text(text, style = T.BtnS.copy(color = if (selected) C.OnAccent else C.Text), maxLines = 1, softWrap = false) }
}

@Composable
fun RoundIconButton(icon: ImageVector, desc: String, bg: Color = C.Ink, tint: Color = C.OnAccent, size: Dp = 40.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label)
            if (subtitle != null) Text(subtitle, style = T.Small)
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Accent, checkedThumbColor = C.OnAccent, uncheckedTrackColor = C.SurfaceAlt,
                uncheckedBorderColor = C.Line, uncheckedThumbColor = C.Faint))
    }
}

@Composable
fun NavRow(title: String, subtitle: String? = null, emoji: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (emoji != null) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(tileColors(title).first), contentAlignment = Alignment.Center) { Text(emoji) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label)
            if (subtitle != null) Text(subtitle, style = T.Small)
        }
        Text("›", style = T.Title.copy(color = C.Faint))
    }
}

@Composable
fun JField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
           number: Boolean = false, singleLine: Boolean = true, suffix: String? = null) {
    val suffixContent: (@Composable () -> Unit)? = if (suffix == null) null else { @Composable { Text(suffix, style = T.Muted) } }
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = singleLine, textStyle = T.Body, suffix = suffixContent,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Accent, unfocusedBorderColor = C.Line, focusedLabelColor = C.AccentDeep, unfocusedLabelColor = C.Muted,
            cursorColor = C.Accent, focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface
        )
    )
}

@Composable
fun Tag(text: String, color: Color, bg: Color = color.copy(alpha = 0.13f)) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, style = T.Small.copy(color = color))
    }
}

@Composable
fun EmptyState(emoji: String, title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = T.Big)
        Spacer(Modifier.height(8.dp))
        Text(title, style = T.Title2, textAlign = TextAlign.Center)
        Text(text, style = T.Muted, textAlign = TextAlign.Center)
    }
}

/** Riquadro con un valore e un'etichetta (es. "Proteine 21 g"). */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = C.Text, bg: Color = C.SurfaceAlt) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(bg).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = T.Small)
        Spacer(Modifier.height(2.dp))
        Text(value, style = T.Num.copy(color = color, fontSize = T.Num.fontSize * 0.9f), maxLines = 1)
    }
}

@Composable
fun MacroTiles(p: Double, c: Double, f: Double, approx: Boolean = false) {
    val pre = if (approx) "≈ " else ""
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Proteine", "$pre${p.r0()} g", Modifier.weight(1f))
        StatTile("Carboidrati", "$pre${c.r0()} g", Modifier.weight(1f))
        StatTile("Grassi", "$pre${f.r0()} g", Modifier.weight(1f))
    }
}

@Composable
fun MacroDots(p: Double, c: Double, f: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Dot(C.Protein, "P ${p.r0()}g"); Dot(C.Carbs, "C ${c.r0()}g"); Dot(C.Fat, "G ${f.r0()}g") }
}

@Composable
fun Dot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = T.Small)
    }
}

// ---------- Immagini ricette ----------

/** Cache delle foto limitata in byte (sizeOf), così sfogliando molte ricette la memoria resta sotto controllo. */
private val bitmapCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

/** Decodifica una foto dell'utente ridotta (lato lungo ~1280 px) per non caricare in memoria 12+ megapixel. */
private fun decodeScaled(path: String, maxSide: Int = 1280): Bitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var sample = 1
    while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= maxSide) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** Foto della ricetta dagli asset (o foto scattata dall'utente). Mentre carica mostra un segnaposto elegante. */
@Composable
fun RecipeImage(recipeId: String, asset: String?, userPhoto: String?, modifier: Modifier, emoji: String = "🍽️", corner: Dp = 18.dp) {
    val ctx = LocalContext.current
    val key = userPhoto ?: asset ?: ""
    // Lo stato dipende dalla chiave: cambiando piatto si riparte dalla foto del NUOVO piatto (o dal segnaposto),
    // mai da quella precedente (produceState tiene il valore vecchio quando cambia la chiave).
    var bmp by remember(key) { mutableStateOf<Bitmap?>(bitmapCache.get(key)) }
    LaunchedEffect(key) {
        if (bmp != null || key.isEmpty()) return@LaunchedEffect
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val b = if (userPhoto != null) decodeScaled(File(userPhoto).absolutePath)
                else ctx.assets.open(asset!!).use { BitmapFactory.decodeStream(it) }
                b?.also { bitmapCache.put(key, it) }
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(corner)
    val b = bmp
    if (b != null) {
        Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(shape))
    } else {
        Box(modifier.clip(shape).background(Brush.linearGradient(listOf(C.AccentSoft, C.GoldSoft))), contentAlignment = Alignment.Center) {
            Text(emoji, style = T.Big)
        }
    }
}

// ---------- Grafici ----------

/** Anello calorie: progresso animato con arco arancione, rosso se si supera l'obiettivo. [onDark] per la scheda blu navy. */
@Composable
fun CalorieRing(eaten: Double, budget: Double, modifier: Modifier = Modifier, size: Dp = 170.dp, onDark: Boolean = false) {
    val target = if (budget <= 0) 0f else (eaten / budget).toFloat().coerceIn(0f, 1.2f)
    val p by animateFloatAsState(target, tween(800), label = "ring")
    val over = eaten > budget
    val track = if (onDark) Color.White.copy(alpha = 0.16f) else C.SurfaceAlt
    val textColor = if (onDark) Color.White else C.Text
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 15.dp.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                if (over) Brush.sweepGradient(listOf(C.Danger, C.Danger)) else Brush.sweepGradient(C.OrangeArc + C.OrangeArc.first()),
                135f, 270f * p.coerceAtMost(1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kotlin.math.abs(budget - eaten).r0(), style = T.Big.copy(color = if (over) (if (onDark) Color(0xFFFFB4A9) else C.Danger) else textColor))
            Text(if (over) "kcal in più" else "kcal rimanenti", style = T.Muted.copy(color = if (onDark) Color.White.copy(alpha = 0.75f) else C.Muted))
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, goal: Double, color: Color, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val p by animateFloatAsState(if (goal <= 0) 0f else (value / goal).toFloat().coerceIn(0f, 1f), tween(600), label = "macro")
    Column(modifier) {
        Text(label, style = T.Small.copy(color = if (onDark) Color.White.copy(alpha = 0.72f) else C.Muted))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(if (onDark) Color.White.copy(alpha = 0.16f) else C.SurfaceAlt)) {
            Box(Modifier.fillMaxWidth(p).height(8.dp).clip(RoundedCornerShape(50)).background(if (value > goal * 1.1) C.Danger else color))
        }
        Spacer(Modifier.height(5.dp))
        Text("${value.r0()} / ${goal.r0()} g", style = T.Label.copy(color = if (onDark) Color.White else C.Text))
    }
}

@Composable
fun ProgressLine(p: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, marker: Float? = null) {
    val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(600), label = "line")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(C.SurfaceAlt)) {
        Box(Modifier.fillMaxWidth(a).height(height).clip(RoundedCornerShape(50)).background(color))
        if (marker != null) Canvas(Modifier.fillMaxSize()) {
            val x = size.width * marker.coerceIn(0f, 1f)
            drawLine(C.Text.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
        }
    }
}

@Composable
fun MacroDonut(c: Double, p: Double, f: Double, size: Dp = 88.dp) {
    val kc = c * 4; val kp = p * 4; val kf = f * 9
    val tot = (kc + kp + kf).coerceAtLeast(0.001)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val st = 11.dp.toPx()
            val tl = Offset(st / 2, st / 2)
            val sz = Size(this.size.width - st, this.size.height - st)
            if (kc + kp + kf <= 0.001) drawArc(C.SurfaceAlt, 0f, 360f, false, tl, sz, style = Stroke(st))
            else {
                var start = -90f
                listOf(kp to C.Protein, kc to C.Carbs, kf to C.Fat).forEach { (v, col) ->
                    val sweep = (v / tot * 360).toFloat()
                    if (sweep > 0) drawArc(col, start + 1.5f, (sweep - 3f).coerceAtLeast(0.5f), false, tl, sz, style = Stroke(st, cap = StrokeCap.Round))
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(kp / tot * 100).r0()}%", style = T.Title2)
            Text("proteine", style = T.Small)
        }
    }
}

/** Istogramma con angoli arrotondati e linea obiettivo tratteggiata. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, goal: Double?, color: Color, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val maxV = max(values.maxOrNull() ?: 0.0, goal ?: 0.0).coerceAtLeast(1.0) * 1.12
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = (slot * 0.56f).coerceAtMost(28.dp.toPx())
            for (k in 1..3) {
                val y = size.height * k / 4f
                drawLine(C.Line, Offset(0f, y), Offset(size.width, y), 1f)
            }
            values.forEachIndexed { i, v ->
                val h = (v / maxV * size.height).toFloat().coerceAtLeast(if (v > 0) 4f else 3f)
                val over = goal != null && v > goal * 1.05
                val c = when { v <= 0 -> C.SurfaceAlt; over -> C.Danger; highlightLast && i == values.lastIndex -> color; else -> color.copy(alpha = 0.55f) }
                drawRoundRect(c, Offset(i * slot + (slot - bw) / 2, size.height - h), Size(bw, h), CornerRadius(bw / 2.5f, bw / 2.5f))
            }
            if (goal != null) {
                val y = (size.height - goal / maxV * size.height).toFloat()
                dashed(Offset(0f, y), Offset(size.width, y), C.Text.copy(alpha = 0.45f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            labels.forEach { Text(it, style = T.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

/** Grafico a linea morbida con area sfumata. */
@Composable
fun LineChart(values: List<Double>, labels: List<String>, color: Color, target: Double? = null, modifier: Modifier = Modifier) {
    if (values.isEmpty()) { Text("Nessun dato ancora", style = T.Muted, modifier = modifier.padding(vertical = 30.dp)); return }
    val all = values + listOfNotNull(target)
    val span = ((all.maxOrNull() ?: 1.0) - (all.minOrNull() ?: 0.0)).coerceAtLeast(1.0)
    val minV = (all.minOrNull() ?: 0.0) - span * 0.15
    val maxV = (all.maxOrNull() ?: 1.0) + span * 0.15
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            fun y(v: Double) = (size.height - (v - minV) / (maxV - minV) * size.height).toFloat()
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
            fun pt(i: Int) = Offset(if (values.size == 1) size.width / 2 else i * stepX, y(values[i]))
            for (k in 1..3) drawLine(C.Line, Offset(0f, size.height * k / 4f), Offset(size.width, size.height * k / 4f), 1f)
            if (target != null) dashed(Offset(0f, y(target)), Offset(size.width, y(target)), C.Green)
            val line = Path()
            for (i in values.indices) {
                val p = pt(i)
                if (i == 0) line.moveTo(p.x, p.y) else {
                    val prev = pt(i - 1); val mx = (prev.x + p.x) / 2
                    line.cubicTo(mx, prev.y, mx, p.y, p.x, p.y)
                }
            }
            val area = Path().apply { addPath(line); lineTo(pt(values.lastIndex).x, size.height); lineTo(pt(0).x, size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f))))
            drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            val last = pt(values.lastIndex)
            drawCircle(C.Surface, 7.dp.toPx(), last); drawCircle(color, 4.5.dp.toPx(), last)
        }
        if (labels.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labels.first(), style = T.Small)
            if (labels.size > 1) Text(labels.last(), style = T.Small)
        }
    }
}

private fun DrawScope.dashed(a: Offset, b: Offset, color: Color) {
    drawLine(color, a, b, strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
}

// ---------- Icone barra di navigazione (disegnate a mano) ----------

enum class NavIcon { Today, Plan, Kitchen, Progress, Profile }

@Composable
fun NavGlyph(icon: NavIcon, color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val s = size.minDimension
        val st = Stroke(width = s * 0.085f, cap = StrokeCap.Round)
        when (icon) {
            NavIcon.Today -> { // piatto e forchetta
                drawCircle(color, s * 0.30f, Offset(s * 0.58f, s * 0.52f), style = st)
                drawCircle(color, s * 0.14f, Offset(s * 0.58f, s * 0.52f), style = st)
                drawLine(color, Offset(s * 0.13f, s * 0.16f), Offset(s * 0.13f, s * 0.86f), st.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.06f, s * 0.16f), Offset(s * 0.06f, s * 0.38f), st.width * 0.8f, StrokeCap.Round)
                drawLine(color, Offset(s * 0.20f, s * 0.16f), Offset(s * 0.20f, s * 0.38f), st.width * 0.8f, StrokeCap.Round)
            }
            NavIcon.Plan -> { // calendario
                drawRoundRect(color, Offset(s * 0.12f, s * 0.2f), Size(s * 0.76f, s * 0.68f), CornerRadius(s * 0.12f), style = st)
                drawLine(color, Offset(s * 0.12f, s * 0.42f), Offset(s * 0.88f, s * 0.42f), st.width)
                drawLine(color, Offset(s * 0.34f, s * 0.1f), Offset(s * 0.34f, s * 0.28f), st.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.66f, s * 0.1f), Offset(s * 0.66f, s * 0.28f), st.width, StrokeCap.Round)
            }
            NavIcon.Kitchen -> { // cestino della spesa
                val p = Path().apply {
                    moveTo(s * 0.1f, s * 0.38f); lineTo(s * 0.9f, s * 0.38f); lineTo(s * 0.78f, s * 0.86f); lineTo(s * 0.22f, s * 0.86f); close()
                }
                drawPath(p, color, style = st)
                drawArc(color, 180f, 180f, false, Offset(s * 0.28f, s * 0.12f), Size(s * 0.44f, s * 0.52f), style = st)
            }
            NavIcon.Progress -> {
                drawLine(color, Offset(s * 0.18f, s * 0.84f), Offset(s * 0.18f, s * 0.56f), st.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.5f, s * 0.84f), Offset(s * 0.5f, s * 0.34f), st.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.82f, s * 0.84f), Offset(s * 0.82f, s * 0.14f), st.width, StrokeCap.Round)
            }
            NavIcon.Profile -> {
                drawCircle(color, s * 0.17f, Offset(s * 0.5f, s * 0.32f), style = st)
                drawArc(color, 200f, 140f, false, Offset(s * 0.16f, s * 0.58f), Size(s * 0.68f, s * 0.6f), style = st)
            }
        }
    }
}
