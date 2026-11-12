package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.platform.testTag
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
    val Ink = C.Text
    val Muted = C.Muted
    val Hair = Color(0x14162853)              // bordo schede: inchiostro all'8%
    val Shadow = Color(0xFF1D1B3A)

    val ProteinSoft = Color(0xFFFFE4EC); val ProteinInk = Color(0xFFC41F4C)
    val CarbSoft = Color(0xFFE3EEFF); val CarbInk = Color(0xFF1D4ED8)
    val FatSoft = Color(0xFFFFF1D2); val FatInk = Color(0xFF925907)
    val WaterSoft = Color(0xFFDDF6FB); val WaterInk = Color(0xFF0E7490)
    val GreenSoft = Color(0xFFD9F6EA); val GreenInk = Color(0xFF047857)
    val FastSoft = C.NavySoft; val FastInk = C.Navy
    val Tonal = C.AccentSoft; val TonalInk = C.AccentDeep
    val Link = C.AccentDeep
    val Peach = C.AccentSoft
    val Dashed = C.Line

    /** Sfumatura "alba" del marchio. */
    val Dawn = Brush.linearGradient(listOf(C.Accent, Color(0xFFFFAD60)))
    val Sun = listOf(C.Accent, Color(0xFFF68A46), Color(0xFFFFAD60))
    val FastBar = C.NavyBrush
    val FastBarV = C.NavyBrush
    val WaterFill = Brush.verticalGradient(listOf(Color(0xFF5CD6EA), Color(0xFF3B82F6)))
}

// ---------- Icone a tratto (stessi path del design, griglia 24) ----------

object Ic {
    private fun f(v: Double): String = (if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString())
    fun circ(cx: Double, cy: Double, r: Double) = "M${f(cx - r)} ${f(cy)}a${f(r)} ${f(r)} 0 1 0 ${f(2 * r)} 0a${f(r)} ${f(r)} 0 1 0 ${f(-2 * r)} 0"
    fun rr(x: Double, y: Double, w: Double, h: Double, r: Double) =
        "M${f(x + r)} ${f(y)}h${f(w - 2 * r)}a${f(r)} ${f(r)} 0 0 1 ${f(r)} ${f(r)}v${f(h - 2 * r)}a${f(r)} ${f(r)} 0 0 1 ${f(-r)} ${f(r)}" +
            "h${f(-(w - 2 * r))}a${f(r)} ${f(r)} 0 0 1 ${f(-r)} ${f(-r)}v${f(-(h - 2 * r))}a${f(r)} ${f(r)} 0 0 1 ${f(r)} ${f(-r)}z"

    // Set ridisegnato: stesso peso di tratto, angoli morbidi, forme chiuse (compatibili con il tono pieno "duo").
    val Sun = circ(12.0, 12.0, 4.2) + "M12 2.8v2.2M12 19v2.2M2.8 12H5M19 12h2.2M5.5 5.5l1.6 1.6M16.9 16.9l1.6 1.6M5.5 18.5l1.6-1.6M16.9 7.1l1.6-1.6"
    val Calendar = rr(3.5, 5.0, 17.0, 15.5, 4.0) + "M3.5 10h17M8 3v3.5M16 3v3.5"
    val Bowl = "M4 11h16v1a8 8 0 0 1-16 0zM9 7.4c0-1.5 1.3-1.5 1.3-3M14 7.4c0-1.5 1.3-1.5 1.3-3"
    val Chart = rr(4.0, 12.0, 4.0, 8.0, 1.3) + rr(10.0, 4.0, 4.0, 16.0, 1.3) + rr(16.0, 8.0, 4.0, 12.0, 1.3)
    val User = circ(12.0, 8.0, 4.0) + "M4.5 20.5c.8-3.8 3.8-6 7.5-6s6.7 2.2 7.5 6z"
    val Plus = "M12 5.5v13M5.5 12h13"
    val Minus = "M5.5 12h13"
    val Check = "M5 12.8l4.4 4.4L19 7.4"
    val Right = "M9.5 6l6 6-6 6"
    val Left = "M14.5 6l-6 6 6 6"
    val Camera = "M3.5 9.5A2.5 2.5 0 0 1 6 7h1.6l1.2-1.8a1.5 1.5 0 0 1 1.3-.7h3.8a1.5 1.5 0 0 1 1.3.7L16.4 7H18a2.5 2.5 0 0 1 2.5 2.5v8A2.5 2.5 0 0 1 18 20H6a2.5 2.5 0 0 1-2.5-2.5z" + circ(12.0, 13.0, 3.5)
    val Fridge = rr(5.5, 2.5, 13.0, 19.0, 3.5) + "M5.5 10h13M9 6v1.6M9 13v3"
    val Timer = circ(12.0, 13.2, 7.6) + "M12 9.4v4l2.6 1.6M9.8 3h4.4"
    val Flame = "M12 2.8c.9 3.7 5.6 5.6 5.6 10.6a5.6 5.6 0 0 1-11.2 0c0-2.3 1.2-3.7 2.4-4.5.1 1.9 1 3 2.1 3.1-.3-3.6.4-6.3 1.1-9.2z"
    val Bolt = "M13.2 2.8L5.5 13.6h5.7l-.7 7.6 8-10.9h-5.8z"
    val Drop = "M12 3.2c3 3.4 6 6.5 6 10.3a6 6 0 0 1-12 0c0-3.8 3-6.9 6-10.3z"
    val Spark = "M12 3c.6 4.4 2.6 6.4 7 7-4.4.6-6.4 2.6-7 7-.6-4.4-2.6-6.4-7-7 4.4-.6 6.4-2.6 7-7z"
    val Heart = "M12 20.2C7 16.8 4 13.6 4 9.9a4.1 4.1 0 0 1 8-1.3 4.1 4.1 0 0 1 8 1.3c0 3.7-3 6.9-8 10.3z"
    val Search = circ(10.8, 10.8, 6.6) + "M20 20l-4.6-4.6"
    val Barcode = "M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2M8 8v8M12 8v8M16 8v8"
    val Clock = circ(12.0, 12.0, 9.0) + "M12 7.2V12l3.2 2"
    val Swap = "M6 8h12l-3-3M18 16H6l3 3"
    val Steps = "M3.5 17l6-6 4 4 7-7.5M15 7.5h5.5V13"
    val Stop = rr(6.0, 6.0, 12.0, 12.0, 3.5)
    val Close = "M6.5 6.5l11 11M17.5 6.5l-11 11"
    val Receipt = "M6 3.5h12v17.2l-2.25-1.6-2.25 1.6L12 19.1l-1.5 1.6-2.25-1.6L6 20.7zM9 8.5h6M9 12.5h6"
    val Cart = "M3 4.5h2.4l2.1 10.5h10.4l1.9-7.6H6.3" + circ(9.0, 19.4, 1.4) + circ(17.0, 19.4, 1.4)

    val MealBreakfast = "M4 10h12.5v4.5a5.5 5.5 0 0 1-5.5 5.5H9.5A5.5 5.5 0 0 1 4 14.5zM16.5 11.5h1.4a2.6 2.6 0 0 1 0 5.2h-1.6M8 3.5c-1 1.2 1 2 0 3.5M12 3.5c-1 1.2 1 2 0 3.5"
    val MealLunch = "M3.5 11.5h17a8.5 8.5 0 0 1-17 0zM8 7.5l1.2-3M12.5 7.5l1.2-3"
    val MealSnack = "M12 7.5c-1.6-1.2-6.5-1.6-7 3.6-.4 4.8 3 9.6 5.2 9.4.8 0 1.2-.5 1.8-.5s1 .5 1.8.5c2.2.2 5.6-4.6 5.2-9.4-.5-5.2-5.4-4.8-7-3.6zM12 7.5c0-1.6-.4-2.8-1.4-4"
    val MealDinner = "M19.5 14.2A8 8 0 1 1 9.8 4.5a6.4 6.4 0 0 0 9.7 9.7z"
}

private val pathCache = HashMap<String, androidx.compose.ui.graphics.Path>()

private fun parsed(d: String): androidx.compose.ui.graphics.Path =
    pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Icona a tratto: [d] in griglia 24, tratto [stroke] (come nel design 1.9–2.6), estremi arrotondati. */
@Composable
fun AIcon(d: String, tint: Color, size: Dp = 24.dp, stroke: Float = 2f, fill: Boolean = false, modifier: Modifier = Modifier, duo: Boolean = false) {
    val path = remember(d) { parsed(d) }
    Canvas(modifier.size(size)) {
        val sc = this.size.minDimension / 24f
        withTransform({ scale(sc, sc, Offset.Zero) }) {
            if (fill) drawPath(path, tint)
            else if (duo) { drawPath(path, tint.copy(alpha = tint.alpha * 0.22f)); drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
            else drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Shared white/navy header controls, matching the Jarvis app identity. */
@Composable
fun HeaderControl(label: String, selected: Boolean = false, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.size(48.dp).aShadow(shape, 6.dp).clip(shape)
        .background(if (selected) C.Navy else C.Surface)
        .border(1.dp, if (selected) C.Navy else C.Navy.copy(alpha = 0.12f), shape)
        .semantics { contentDescription = label }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center, content = content)
}

@Composable
fun HeaderIconButton(icon: String, label: String, ink: Color = C.Navy, selected: Boolean = false, badge: Boolean = false, onClick: () -> Unit) {
    HeaderControl(label, selected, onClick) {
        AIcon(icon, if (selected) Color.White else ink, 30.dp, 1.9f, modifier = Modifier.testTag("header-icon-$label"))
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(8.dp).clip(CircleShape)
            .background(C.Accent).border(1.5.dp, C.Surface, CircleShape))
    }
}

/** Riquadro tonale con icona dentro (44 px, raggio 16 nel design). */
@Composable
fun TonalIcon(d: String, tint: Color, bg: Color, box: Dp = 44.dp, radius: Dp = 16.dp, size: Dp = 22.dp, stroke: Float = 2f, modifier: Modifier = Modifier) {
    Box(modifier.size(box).clip(RoundedCornerShape(radius)).background(C.cardSurface(bg)), contentAlignment = Alignment.Center) { AIcon(d, tint, size, stroke) }
}

// ---------- Sfondo e schede ----------

/** Ombra morbida colorata tipo "0 22px 40px -26px". */
fun Modifier.aShadow(shape: Shape, elevation: Dp = 12.dp, color: Color = Color(0x661D1B3A)): Modifier =
    this.shadow(elevation.coerceAtMost(14.dp) * 0.5f, shape, ambientColor = Color(0x1A0B1F44), spotColor = Color(0x330B1F44))

/** Ombra morbida delle schede bianche (riferimento: schede sospese su sfondo chiaro). */
fun Modifier.lightShadow(elevation: Dp = 16.dp, radius: Dp = 20.dp): Modifier =
    this.shadow(elevation * 0.5f, RoundedCornerShape(radius), ambientColor = Color(0x1A0B1F44), spotColor = Color(0x330B1F44))

/** Alone arancione misurato sotto i pulsanti principali. */
fun Modifier.orangeGlow(shape: Shape, elevation: Dp = 12.dp): Modifier =
    this.shadow(elevation, shape, ambientColor = Color(0x66FF7A1A), spotColor = Color(0xB3FF7A1A))

/** Scheda bianca del design: raggio grande, bordo sottile, ombra morbida. */
@Composable
fun ACard(
    modifier: Modifier = Modifier, radius: Dp = 20.dp, color: Color = Color.White, brush: Brush? = null,
    padding: Dp = 16.dp, elevation: Dp = 10.dp, glow: Color = Color(0x4D1D1B3A), bordered: Boolean = false,
    onClick: (() -> Unit)? = null, content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    var m = modifier.aShadow(shape, elevation, glow).clip(shape)
    m = if (brush != null) m.background(brush) else m.background(C.cardSurface(color))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(padding)) { content() }
}

/** Titolo di sezione (Bricolage 22) con link a destra (rosso-arancio, 13.5). */
@Composable
fun ASection(title: String, modifier: Modifier = Modifier, action: String? = null, top: Dp = 24.dp, onAction: (() -> Unit)? = null) {
    val expanded = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
    val link: @Composable () -> Unit = {
        if (action != null) Box(Modifier.heightIn(min = 48.dp).clickable(enabled = onAction != null, role = Role.Button) { onAction?.invoke() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(action, style = T.Link, textAlign = TextAlign.End) }
    }
    if (expanded && action != null) Column(modifier.fillMaxWidth().padding(top = top)) {
        Text(title, style = T.H2)
        Box(Modifier.align(Alignment.End)) { link() }
    } else Row(modifier.fillMaxWidth().padding(top = top), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.H2, modifier = Modifier.weight(1f).padding(end = 12.dp))
        link()
    }
}

/** Chip tonale con macro: "P 28 g" ecc. */
@Composable
fun MacroChip(text: String, bg: Color, ink: Color) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
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

/** Pulsante principale: gradiente arancione, riflesso in alto, alone morbido. */
@Composable
fun APrimary(text: String, modifier: Modifier = Modifier, height: Dp = 54.dp, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.heightIn(min = height).let { if (enabled) it.orangeGlow(shape, 10.dp) else it }.clip(shape)
            .background(if (enabled) C.OrangeBrush else Brush.verticalGradient(listOf(C.SurfaceHi, C.SurfaceHi)))
            .let { if (enabled) it.background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent), endY = 60f)) else it }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = T.BtnM.copy(fontWeight = FontWeight.Bold, color = if (enabled) C.OnOrange else C.Muted), textAlign = TextAlign.Center)
    }
}

/** Pulsante secondario: bianco, bordo sottile, testo navy. */
@Composable
fun AInk(text: String, modifier: Modifier = Modifier, height: Dp = 54.dp, bg: Color = C.Navy, fg: Color = Color.White, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val dark = bg == C.Navy
    Box(modifier.heightIn(min = height).clip(shape).background(if (dark) RD.NavyBrush else Brush.linearGradient(listOf(C.cardSurface(bg), C.cardSurface(bg))))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.BtnM.copy(color = fg, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
    }
}

/** Cerchio navy con "+" (44 px), usato accanto ai pasti. */
@Composable
fun PlusButton(desc: String, size: Dp = 48.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).orangeGlow(CircleShape, 8.dp).clip(CircleShape).background(C.OrangeBrush).semantics { contentDescription = desc }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(Ic.Plus, Color.White, size * 0.42f, 2.8f)
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
fun APill(label: String, selected: Boolean, modifier: Modifier = Modifier, height: Dp = 48.dp, hPad: Dp = 16.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.heightIn(min = maxOf(height, 48.dp)).clip(shape).background(if (selected) C.OrangeBrush else Brush.linearGradient(listOf(Color.White, Color.White)))
            .border(1.dp, if (selected) Color.Transparent else Color(0xFFDDE2EB), shape).clickable(onClick = onClick).padding(horizontal = hPad, vertical = 10.dp),
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
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(C.cardSurface(bg)), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = T.Num.copy(color = ink, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold))
    }
}

private val TileColors = listOf(
    C.Surface to C.Navy, C.Surface to C.Navy, C.Surface to C.Navy,
    C.Surface to C.Navy, C.Surface to C.Navy, C.Surface to C.Navy
)

/** Colori tonali stabili per un nome (stessa lettera = stesso colore). */
fun tileColors(name: String): Pair<Color, Color> = TileColors[(name.hashCode().let { if (it < 0) -it else it }) % TileColors.size]

@Composable
fun GapH(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun GapW(w: Dp) = Spacer(Modifier.width(w))

/** Calorie con il punto delle migliaia, come nel design (1.318). */
fun kcalFmt(v: Double): String = v.let { kotlin.math.round(it).toInt() }.thousands()

/** Banner promozionale: fascia blu con bagliori, testo, pulsante arancione e illustrazione a destra. */
@Composable
fun PromoBanner(title: String, subtitle: String, cta: String, modifier: Modifier = Modifier, onClick: () -> Unit, trailing: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().lightShadow(18.dp, 24.dp).clip(shape).background(RD.NavyBrush)
        .drawBehind {
            drawRect(Brush.radialGradient(listOf(Color(0x552B6BFF), Color.Transparent), center = Offset(size.width * 0.1f, 0f), radius = size.width * 0.8f))
            drawRect(Brush.radialGradient(listOf(Color(0x59FF7A1A), Color.Transparent), center = Offset(size.width * 1.0f, size.height), radius = size.width * 0.55f))
        }
        .clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = T.Title2.copy(color = Color.White, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold))
                Text(subtitle, style = T.Small.copy(color = Color(0xFFC5D3F2), fontSize = 13.sp, lineHeight = 18.sp), modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.orangeGlow(RoundedCornerShape(14.dp), 8.dp).clip(RoundedCornerShape(14.dp)).background(C.OrangeBrush).padding(start = 16.dp, end = 12.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cta, style = T.BtnS.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.width(6.dp)); AIcon(Ic.Right, Color.White, 16.dp, 2.8f)
                }
            }
            Box(Modifier.width(112.dp).heightIn(min = 112.dp), content = trailing)
        }
    }
}
