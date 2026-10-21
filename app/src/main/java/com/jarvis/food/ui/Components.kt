package com.jarvis.food.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Formattazione ----------

fun Double.r0() = roundToInt().toString()
fun Double.r1(): String = String.format(Locale.ITALY, "%.1f", this)
fun Int.thousands(): String = String.format(Locale.ITALY, "%,d", this)
fun parseNum(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()

// ---------- Contenitori ----------

@Composable
fun JCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(C.Surface)
        .border(1.dp, C.Line, RoundedCornerShape(22.dp))
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.Title2, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = T.Label.copy(color = C.Accent), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = C.Text) }
        } else Spacer(Modifier.width(12.dp))
        Text(title, style = T.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

// ---------- Pulsanti ----------

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = C.Accent, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else C.SurfaceHi)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = T.Button.copy(color = if (enabled) C.OnAccent else C.Faint)) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.Text, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = T.Label.copy(color = color)) }
}

@Composable
fun Pill(text: String, selected: Boolean, color: Color = C.Accent, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else C.SurfaceHi)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, style = T.Label.copy(color = if (selected) C.OnAccent else C.Text)) }
}

@Composable
fun RoundIconButton(icon: ImageVector, desc: String, bg: Color = C.Accent, tint: Color = C.OnAccent, size: Dp = 40.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label)
            if (subtitle != null) Text(subtitle, style = T.Small)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Accent, checkedThumbColor = C.OnAccent, uncheckedTrackColor = C.SurfaceHi)
        )
    }
}

@Composable
fun NavRow(title: String, subtitle: String? = null, emoji: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) { Text(emoji, style = T.Title2); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label)
            if (subtitle != null) Text(subtitle, style = T.Small)
        }
        Text("›", style = T.Title.copy(color = C.Faint))
    }
}

// ---------- Campi ----------

@Composable
fun JField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    number: Boolean = false, singleLine: Boolean = true, suffix: String? = null
) {
    val suffixContent: (@Composable () -> Unit)? = if (suffix == null) null else {
        @Composable { Text(suffix, style = T.Muted) }
    }
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = singleLine, textStyle = T.Body,
        suffix = suffixContent,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Accent, unfocusedBorderColor = C.Line,
            focusedLabelColor = C.Accent, unfocusedLabelColor = C.Muted,
            cursorColor = C.Accent, focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface
        )
    )
}

// ---------- Grafica dati ----------

/** Anello calorie: progresso animato, rosso se si supera l'obiettivo. */
@Composable
fun CalorieRing(eaten: Double, goal: Double, burned: Double, modifier: Modifier = Modifier, size: Dp = 168.dp) {
    val budget = goal + burned
    val target = if (budget <= 0) 0f else (eaten / budget).toFloat().coerceIn(0f, 1.2f)
    val p by animateFloatAsState(target, tween(700), label = "ring")
    val over = eaten > budget
    val remaining = (budget - eaten)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(C.SurfaceHi, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                if (over) C.Danger else C.Accent, 135f, 270f * p.coerceAtMost(1f), false,
                Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kotlin.math.abs(remaining).r0(), style = T.Big.copy(color = if (over) C.Danger else C.Text))
            Text(if (over) "kcal in più" else "kcal rimanenti", style = T.Muted)
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, goal: Double, color: Color, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(if (goal <= 0) 0f else (value / goal).toFloat().coerceIn(0f, 1f), tween(600), label = "macro")
    Column(modifier) {
        Text(label, style = T.Small)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)).background(C.SurfaceHi)) {
            Box(Modifier.fillMaxWidth(p).height(7.dp).clip(RoundedCornerShape(50)).background(if (value > goal * 1.1) C.Danger else color))
        }
        Spacer(Modifier.height(4.dp))
        Text("${value.r0()} / ${goal.r0()} g", style = T.Label)
    }
}

@Composable
fun ProgressLine(p: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(600), label = "line")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(C.SurfaceHi)) {
        Box(Modifier.fillMaxWidth(a).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun MacroDots(p: Double, c: Double, f: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Dot(C.Carbs, "C ${c.r0()}g"); Dot(C.Protein, "P ${p.r0()}g"); Dot(C.Fat, "G ${f.r0()}g")
    }
}

@Composable
private fun Dot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = T.Small)
    }
}

/** Istogramma con linea obiettivo opzionale. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, goal: Double?, color: Color, modifier: Modifier = Modifier) {
    val maxV = max(values.maxOrNull() ?: 0.0, goal ?: 0.0).coerceAtLeast(1.0) * 1.1
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = slot * 0.55f
            values.forEachIndexed { i, v ->
                val h = (v / maxV * size.height).toFloat()
                val over = goal != null && v > goal * 1.05
                drawRoundRect(
                    color = if (v <= 0) C.SurfaceHi else if (over) C.Danger.copy(alpha = 0.85f) else color,
                    topLeft = Offset(i * slot + (slot - bw) / 2, size.height - h.coerceAtLeast(3f)),
                    size = Size(bw, h.coerceAtLeast(3f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(bw / 3, bw / 3)
                )
            }
            if (goal != null) {
                val y = (size.height - goal / maxV * size.height).toFloat()
                dashed(Offset(0f, y), Offset(size.width, y), C.Muted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach { Text(it, style = T.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

/** Grafico a linea (es. peso) con punti. */
@Composable
fun LineChart(values: List<Double>, labels: List<String>, color: Color, target: Double? = null, modifier: Modifier = Modifier) {
    if (values.isEmpty()) {
        Text("Nessun dato ancora", style = T.Muted, modifier = modifier.padding(vertical = 30.dp))
        return
    }
    val all = values + listOfNotNull(target)
    val minV = (all.minOrNull() ?: 0.0) - 1
    val maxV = (all.maxOrNull() ?: 1.0) + 1
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            fun y(v: Double) = (size.height - (v - minV) / (maxV - minV) * size.height).toFloat()
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
            if (target != null) dashed(Offset(0f, y(target)), Offset(size.width, y(target)), C.Accent.copy(alpha = 0.6f))
            val path = Path()
            values.forEachIndexed { i, v ->
                val pt = Offset(if (values.size == 1) size.width / 2 else i * stepX, y(v))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { i, v ->
                val pt = Offset(if (values.size == 1) size.width / 2 else i * stepX, y(v))
                drawCircle(C.Bg, 5.dp.toPx(), pt)
                drawCircle(color, 3.5.dp.toPx(), pt)
            }
        }
        if (labels.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labels.first(), style = T.Small)
            if (labels.size > 1) Text(labels.last(), style = T.Small)
        }
    }
}

private fun DrawScope.dashed(a: Offset, b: Offset, color: Color) {
    drawLine(color, a, b, strokeWidth = 1.5.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
}

// ---------- Icone barra di navigazione (disegnate a mano) ----------

enum class NavIcon { Diary, Fasting, Recipes, Progress, Profile }

@Composable
fun NavGlyph(icon: NavIcon, color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val s = size.minDimension
        val st = Stroke(width = s * 0.085f, cap = StrokeCap.Round)
        when (icon) {
            NavIcon.Diary -> { // piatto con forchetta
                drawCircle(color, s * 0.30f, Offset(s * 0.56f, s * 0.52f), style = st)
                drawCircle(color, s * 0.15f, Offset(s * 0.56f, s * 0.52f), style = st)
                drawLine(color, Offset(s * 0.12f, s * 0.18f), Offset(s * 0.12f, s * 0.86f), st.width, StrokeCap.Round)
            }
            NavIcon.Fasting -> { // cronometro
                drawCircle(color, s * 0.34f, Offset(s * 0.5f, s * 0.56f), style = st)
                drawLine(color, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.5f, s * 0.36f), st.width, StrokeCap.Round)
                drawLine(color, Offset(s * 0.42f, s * 0.1f), Offset(s * 0.58f, s * 0.1f), st.width, StrokeCap.Round)
            }
            NavIcon.Recipes -> { // libro aperto
                val p = Path().apply {
                    moveTo(s * 0.5f, s * 0.26f); lineTo(s * 0.1f, s * 0.18f); lineTo(s * 0.1f, s * 0.8f)
                    lineTo(s * 0.5f, s * 0.88f); lineTo(s * 0.9f, s * 0.8f); lineTo(s * 0.9f, s * 0.18f); close()
                }
                drawPath(p, color, style = st)
                drawLine(color, Offset(s * 0.5f, s * 0.26f), Offset(s * 0.5f, s * 0.88f), st.width, StrokeCap.Round)
            }
            NavIcon.Progress -> { // grafico
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

@Composable
fun EmptyState(emoji: String, title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = T.Big)
        Spacer(Modifier.height(8.dp))
        Text(title, style = T.Title2, textAlign = TextAlign.Center)
        Text(text, style = T.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.16f)).border(BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = T.Small.copy(color = color))
    }
}
