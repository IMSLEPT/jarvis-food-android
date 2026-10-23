package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================================
// Aurora: componenti che replicano il design (stessi colori, raggi, ombre e icone a tratto).
// ============================================================================================

/** Colori "inchiostro" delle etichette tonali e sfumature del design. */
object A {
    val Ink = Color(0xFF1D1B3A)
    val Muted = Color(0xFF5E5B7A)
    val Hair = Color(0x0F1D1B3A)              // bordo schede: inchiostro al 6%
    val Shadow = Color(0xFF1D1B3A)

    val ProteinSoft = Color(0xFFFFE4EC); val ProteinInk = Color(0xFFC41F4C)
    val CarbSoft = Color(0xFFE3EEFF); val CarbInk = Color(0xFF1D4ED8)
    val FatSoft = Color(0xFFFFF1D2); val FatInk = Color(0xFF925907)
    val WaterSoft = Color(0xFFDDF6FB); val WaterInk = Color(0xFF0E7490)
    val GreenSoft = Color(0xFFD9F6EA); val GreenInk = Color(0xFF047857)
    val FastSoft = Color(0xFFEDE7FF); val FastInk = Color(0xFF6D28D9)
    val Tonal = Color(0xFFFFE9E1); val TonalInk = Color(0xFFB4381E)
    val Link = Color(0xFFC93A1F)
    val Peach = Color(0xFFFFF8F0)
    val Dashed = Color(0xFFD9D4EC)

    /** Sfumatura "alba" del marchio. */
    val Dawn = Brush.linearGradient(listOf(Color(0xFFF7A07A), Color(0xFFF8C089)))
    val Sun = listOf(Color(0xFFF25E00), Color(0xFFFF8A1C), Color(0xFFFFB02E))
    val FastBar = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6AA6FF)))
    val FastBarV = Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF5AA9FF)))
    val WaterFill = Brush.verticalGradient(listOf(Color(0xFF5CD6EA), Color(0xFF3B82F6)))
}

// ---------- Icone a tratto (stessi path del design, griglia 24) ----------

object Ic {
    private fun f(v: Double): String = (if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString())
    fun circ(cx: Double, cy: Double, r: Double) = "M${f(cx - r)} ${f(cy)}a${f(r)} ${f(r)} 0 1 0 ${f(2 * r)} 0a${f(r)} ${f(r)} 0 1 0 ${f(-2 * r)} 0"
    fun rr(x: Double, y: Double, w: Double, h: Double, r: Double) =
        "M${f(x + r)} ${f(y)}h${f(w - 2 * r)}a${f(r)} ${f(r)} 0 0 1 ${f(r)} ${f(r)}v${f(h - 2 * r)}a${f(r)} ${f(r)} 0 0 1 ${f(-r)} ${f(r)}" +
            "h${f(-(w - 2 * r))}a${f(r)} ${f(r)} 0 0 1 ${f(-r)} ${f(-r)}v${f(-(h - 2 * r))}a${f(r)} ${f(r)} 0 0 1 ${f(r)} ${f(-r)}z"

    val Sun = circ(12.0, 12.0, 4.0) + "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6L7 7M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4"
    val Calendar = rr(4.0, 5.0, 16.0, 15.0, 3.0) + "M4 10h16M9 3v4M15 3v4"
    val Bowl = "M5 11h14v3a5 5 0 0 1-5 5h-4a5 5 0 0 1-5-5zM9 7c0-1.5 1.5-1.5 1.5-3M14 7c0-1.5 1.5-1.5 1.5-3"
    val Chart = "M4 19V5M4 19h16M8 15l4-4 3 3 5-6"
    val User = circ(12.0, 8.0, 4.0) + "M4 20c1-4 4-6 8-6s7 2 8 6"
    val Plus = "M12 5v14M5 12h14"
    val Minus = "M5 12h14"
    val Check = "M5 12.5l4.5 4.5L19 7.5"
    val Right = "M9 6l6 6-6 6"
    val Left = "M15 6l-6 6 6 6"
    val Camera = "M4 8.5h3l1.8-2.5h6.4L17 8.5h3V19H4z" + circ(12.0, 13.5, 3.6)
    val Fridge = rr(6.0, 3.0, 12.0, 18.0, 2.5) + "M6 10h12M9 6v1.5M9 13v3"
    val Timer = circ(12.0, 13.0, 7.5) + "M12 9v4l2.5 1.5M9.5 3h5"
    val Flame = "M12 2c1 4 5 6 5 11a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-4-1-6 1-10z"
    val Bolt = "M13 3L5 14h6l-1 7 8-11h-6z"
    val Drop = "M12 3s6 6.2 6 10.5a6 6 0 0 1-12 0C6 9.2 12 3 12 3z"
    val Spark = "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"
    val Heart = "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"
    val Search = circ(11.0, 11.0, 7.0) + "M20 20l-4-4"
    val Barcode = "M4 7V5a1 1 0 0 1 1-1h2M17 4h2a1 1 0 0 1 1 1v2M20 17v2a1 1 0 0 1-1 1h-2M7 20H5a1 1 0 0 1-1-1v-2M8 8v8M12 8v8M16 8v8"
    val Clock = circ(12.0, 12.0, 9.0) + "M12 7v5l3 2"
    val Swap = "M7 7h10l-3-3M17 17H7l3 3"
    val Steps = "M4 7l6 6 4-4 6 6M20 15v-4M20 15h-4"
    val Stop = rr(6.0, 6.0, 12.0, 12.0, 3.0)
    val Close = "M6 6l12 12M18 6L6 18"
    val Receipt = "M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6"
    val Family = circ(9.0, 8.0, 3.2) + "M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5" + circ(17.0, 9.5, 2.4) + "M16.5 14.6c2.7.2 4.5 2 4.5 4.9"
    val Cart = "M3 4h2.5l2 11h10.5l2-8H6.5" + circ(9.0, 19.5, 1.4) + circ(17.0, 19.5, 1.4)

    val MealBreakfast = "M5 12h14a7 7 0 0 1-14 0zM9 8c0-1.5 1.5-1.5 1.5-3M14 8c0-1.5 1.5-1.5 1.5-3"
    val MealLunch = "M4 10h16a8 8 0 0 1-16 0zM8 6l2-2M13 6l2-2"
    val MealSnack = "M12 7c-3-3-8 0-6 5s6 8 6 8 4-3 6-8-3-8-6-5zM12 7c0-2 1-3 2-4"
    val MealDinner = "M20 13.5A8 8 0 1 1 10.5 4a6.5 6.5 0 0 0 9.5 9.5z"
}

private val pathCache = HashMap<String, androidx.compose.ui.graphics.Path>()

private fun parsed(d: String): androidx.compose.ui.graphics.Path =
    pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Icona a tratto: [d] in griglia 24, tratto [stroke] (come nel design 1.9–2.6), estremi arrotondati. */
@Composable
fun AIcon(d: String, tint: Color, size: Dp = 24.dp, stroke: Float = 2f, fill: Boolean = false, modifier: Modifier = Modifier) {
    val path = remember(d) { parsed(d) }
    Canvas(modifier.size(size)) {
        val sc = this.size.minDimension / 24f
        withTransform({ scale(sc, sc, Offset.Zero) }) {
            if (fill) drawPath(path, tint)
            else drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Riquadro tonale con icona dentro (44 px, raggio 16 nel design). */
@Composable
fun TonalIcon(d: String, tint: Color, bg: Color, box: Dp = 44.dp, radius: Dp = 16.dp, size: Dp = 22.dp, stroke: Float = 2f, modifier: Modifier = Modifier) {
    Box(modifier.size(box).clip(RoundedCornerShape(radius)).background(bg), contentAlignment = Alignment.Center) { AIcon(d, tint, size, stroke) }
}

// ---------- Sfondo e schede ----------

/** Ombra morbida colorata tipo "0 22px 40px -26px". */
fun Modifier.aShadow(shape: Shape, elevation: Dp = 12.dp, color: Color = Color(0x661D1B3A)): Modifier =
    this.shadow(elevation * 0.4f, shape, ambientColor = color.copy(alpha = 0.10f), spotColor = color.copy(alpha = 0.14f))

/** Scheda bianca del design: raggio grande, bordo sottile, ombra morbida. */
@Composable
fun ACard(
    modifier: Modifier = Modifier, radius: Dp = 24.dp, color: Color = Color.White, brush: Brush? = null,
    padding: Dp = 16.dp, elevation: Dp = 10.dp, glow: Color = Color(0x4D1D1B3A), bordered: Boolean = true,
    onClick: (() -> Unit)? = null, content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier.aShadow(shape, elevation, glow).clip(shape)
    m = if (brush != null) m.background(brush) else m.background(color)
    if (bordered) m = m.border(1.dp, A.Hair, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(padding)) { content() }
}

/** Titolo di sezione (Bricolage 22) con link a destra (rosso-arancio, 13.5). */
@Composable
fun ASection(title: String, modifier: Modifier = Modifier, action: String? = null, top: Dp = 26.dp, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = top), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.H2, modifier = Modifier.weight(1f).padding(end = 12.dp))
        if (action != null) Text(action, style = T.Link, modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }.padding(vertical = 13.dp))
    }
}

/** Chip tonale con macro: "P 28 g" ecc. */
@Composable
fun MacroChip(text: String, bg: Color, ink: Color) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = T.Chip.copy(color = ink), maxLines = 1)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MacroChips(p: Double, c: Double, f: Double) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MacroChip("P ${p.r0()} g", A.ProteinSoft, A.ProteinInk)
        MacroChip("C ${c.r0()} g", A.CarbSoft, A.CarbInk)
        MacroChip("G ${f.r0()} g", A.FatSoft, A.FatInk)
    }
}

/** Pulsante principale arancione a pillola (58 px nel design). */
@Composable
fun APrimary(text: String, modifier: Modifier = Modifier, height: Dp = 52.dp, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = height).aShadow(shape, 10.dp, Color(0x73F25E00)).clip(shape)
            .background(if (enabled) C.OrangeBrush else Brush.verticalGradient(listOf(C.SurfaceHi, C.SurfaceHi)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = T.BtnM.copy(color = if (enabled) Color.White else C.Muted), textAlign = TextAlign.Center) }
}

/** Pulsante "inchiostro" navy (52 px) e variante tonale. */
@Composable
fun AInk(text: String, modifier: Modifier = Modifier, height: Dp = 52.dp, bg: Color = C.Navy, fg: Color = Color.White, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(modifier.heightIn(min = height).clip(shape).background(bg).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.BtnM.copy(color = fg), textAlign = TextAlign.Center)
    }
}

/** Cerchio navy con "+" (44 px), usato accanto ai pasti. */
@Composable
fun PlusButton(desc: String, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(C.Navy).semantics { contentDescription = desc }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(Ic.Plus, Color.White, size * 0.41f, 2.6f)
    }
}

/** Pulsante tondo bianco con freccia indietro (48 px) delle schermate secondarie. */
@Composable
fun BackCircle(onBack: () -> Unit) {
    Box(
        Modifier.size(48.dp).aShadow(CircleShape, 6.dp, Color(0x661D1B3A)).clip(CircleShape).background(Color.White).border(1.dp, A.Hair, CircleShape)
            .semantics { contentDescription = "Indietro" }.clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) { AIcon(Ic.Left, A.Ink, 22.dp, 2.4f) }
}

/** Selettore a pillole (pasti, piani digiuno...). [sel] = navy con testo bianco. */
@Composable
fun APill(label: String, selected: Boolean, modifier: Modifier = Modifier, height: Dp = 44.dp, hPad: Dp = 20.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = height).clip(shape).background(if (selected) C.Navy else Color.White)
            .border(1.dp, if (selected) A.Ink else Color(0x1A1D1B3A), shape).clickable(onClick = onClick).padding(horizontal = hPad, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = T.BtnS.copy(color = if (selected) Color.White else A.Ink), textAlign = TextAlign.Center) }
}

/** Barra di avanzamento sottile del design. */
@Composable
fun ABar(fraction: Float, fill: Brush, track: Color, height: Dp = 10.dp, modifier: Modifier = Modifier) {
    val r = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(r).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(r).background(fill))
    }
}

/** Tessera con iniziale colorata per alimenti senza foto. */
@Composable
fun InitialTile(name: String, bg: Color, ink: Color, size: Dp = 52.dp, radius: Dp = 19.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = T.Num.copy(color = ink, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold))
    }
}

private val TileColors = listOf(
    A.ProteinSoft to A.ProteinInk, A.GreenSoft to A.GreenInk, A.CarbSoft to A.CarbInk,
    A.FatSoft to A.FatInk, A.Tonal to A.TonalInk, A.FastSoft to A.FastInk
)

/** Colori tonali stabili per un nome (stessa lettera = stesso colore). */
fun tileColors(name: String): Pair<Color, Color> = TileColors[(name.hashCode().let { if (it < 0) -it else it }) % TileColors.size]

@Composable
fun GapH(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun GapW(w: Dp) = Spacer(Modifier.width(w))

/** Calorie con il punto delle migliaia, come nel design (1.318). */
fun kcalFmt(v: Double): String = v.let { kotlin.math.round(it).toInt() }.thousands()
