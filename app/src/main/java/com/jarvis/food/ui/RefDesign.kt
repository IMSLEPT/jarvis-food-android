package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isSpecified
import com.jarvis.food.R
import com.jarvis.food.data.Store

/**
 * Redesign del 4 novembre 2026: colori, misure e componenti condivisi ricavati dalle immagini di
 * riferimento di Alessandro. Ogni schermata usa questi elementi, così che componenti con lo stesso
 * ruolo abbiano ovunque la stessa grafica.
 */
object RD {
    val Navy = Color(0xFFF3F6FF)
    val NavyDeep = Color(0xFFF3F6FF)
    val NavyHi = Color(0xFF8FB4FF)
    val Ink = Color(0xFFF3F6FF)
    val Orange = Color(0xFFFF8A2A)
    val OrangeDeep = Color(0xFFFF6A10)
    val OrangeText = C.AccentDeep
    val OrangeLine = Color(0xFFFF8A2A)
    val Peach = Color(0xFF13244C)
    val PeachDeep = Color(0xFF1A2F5E)
    val PeachTile = Color(0xFF13244C)
    val Grey = Color(0xFF8E9CBF)
    val GreyLine = Color(0xFF1E3263)
    val GreyFill = Color(0xFF13244C)
    val Strip = Color(0xFF0C1A3D)
    val Green = Color(0xFF6E9F62)
    val NavyBrush = C.NavyBrush
    val ButtonBrush = C.OrangeBrush
    val OrangeBrush = C.OrangeBrush
    val PeachBrush = Brush.verticalGradient(listOf(Color(0xFF13244C), Color(0xFF13244C)))
    val WarmBrush = Brush.linearGradient(listOf(C.Surface, C.Surface))

    /** Banner pages share the same proportions. */
    @Composable fun bannerTitleHeight() = 64.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    @Composable fun stripHeight() = (if (LocalDensity.current.fontScale > 1.5f) 104.dp else 66.dp) * LocalDensity.current.fontScale.coerceAtLeast(1f)
    @Composable fun large() = LocalDensity.current.fontScale > 1.2f
    /** Calendario e profilo hanno la stessa misura in tutti i banner. */
    @Composable fun headerButton() = 48.dp
    /** Loghi dei banner: stessa impronta visiva su tutte le schede. */
    val BannerLogo = 34.dp
    /** Titoli dei banner: stessa dimensione su tutte le schede. */
    val BannerTitle = 26.sp
}

// ------------------------------------------------------------------------------- Icone

/** Icona bicolore: tratto navy più accento arancio (pieno o a tratto), griglia 24. */
@Composable
fun DuoIcon(navy: String, accent: String = "", size: Dp = 28.dp, accentFill: Boolean = false, stroke: Float = 1.9f,
    ink: Color = RD.Ink, tone: Color = RD.Orange, navyFill: String = "", modifier: Modifier = Modifier) {
    Box(modifier.size(size)) {
        if (navyFill.isNotEmpty()) AIcon(navyFill, ink, size, fill = true)
        if (accent.isNotEmpty() && accentFill) AIcon(accent, tone, size, fill = true)
        if (navy.isNotEmpty()) AIcon(navy, ink, size, stroke)
        if (accent.isNotEmpty() && !accentFill) AIcon(accent, tone, size, stroke)
    }
}

object RIc {
    private fun rr(x: Double, y: Double, w: Double, h: Double, r: Double) = Ic.rr(x, y, w, h, r)
    private fun c(x: Double, y: Double, r: Double) = Ic.circ(x, y, r)
    val Calendar = rr(3.5, 5.0, 17.0, 15.5, 3.0) + "M8 3v4M16 3v4"
    val CalendarBand = "M3.5 10h17"
    val CalendarDots = rr(7.0, 13.0, 2.4, 2.0, .6) + rr(10.8, 13.0, 2.4, 2.0, .6) + rr(14.6, 13.0, 2.4, 2.0, .6) + rr(7.0, 16.4, 2.4, 2.0, .6) + rr(10.8, 16.4, 2.4, 2.0, .6)
    val CalendarDot = c(12.0, 15.2, 1.7)
    val Dumbbell = rr(2.2, 8.5, 3.6, 7.0, 1.6) + rr(18.2, 8.5, 3.6, 7.0, 1.6) + rr(5.4, 6.5, 3.2, 11.0, 1.6) + rr(15.4, 6.5, 3.2, 11.0, 1.6)
    val DumbbellBar = "M8.6 12h6.8"
    val DumbbellBold = "M4 9v6M20 9v6M7 6.5v11M17 6.5v11M7 12h10"
    val Bed = "M3 5.5v14M3 15.5h18v4M21 15.5v-2.5a3 3 0 0 0-3-3H10.5v5.5"
    val BedFill = rr(10.5, 11.2, 10.3, 4.3, 1.4)
    val BedPillow = rr(4.6, 10.6, 4.6, 3.2, 1.4)
    val Check = "M5 12.5l4.5 4.5L19 7.5"
    val Close = "M6 6l12 12M18 6L6 18"
    val Chevron = "M6 9l6 6 6-6"
    val Clock = c(12.0, 12.0, 8.5) + "M12 7.5v4.8l3 1.7"
    val Stopwatch = c(12.0, 13.5, 7.5) + "M9.5 3h5M12 3v3"
    val StopwatchHand = "M12 9.5v4M12 13.5h.01"
    val StopwatchTicks = "M12 7.6v.4M12 19v.4M6.1 13.5h.4M17.5 13.5h.4M8 9.5l.3.3M15.7 17.2l.3.3M8 17.5l.3-.3M15.7 9.8l.3-.3"
    val Leaf = "M5 19c0-8.5 5-13.5 14.5-14 .4 9.6-4.6 14.6-14.5 14M5 19l8.5-8.5"
    val Refresh = "M19.5 9A8 8 0 0 0 5.2 7.8M4.5 4v4h4M4.5 15a8 8 0 0 0 14.3 1.2M19.5 20v-4h-4"
    val Swap = "M4 8h15l-3.5-3.5M20 16H5l3.5 3.5"
    val Barcode = "M4 8V5.5A1.5 1.5 0 0 1 5.5 4H8M16 4h2.5A1.5 1.5 0 0 1 20 5.5V8M20 16v2.5a1.5 1.5 0 0 1-1.5 1.5H16M8 20H5.5A1.5 1.5 0 0 1 4 18.5V16M8 8v8M11 8v8M14 8v8M17 8v8"
    val BowlSteam = "M4 12h16a8 8 0 0 1-16 0zM9 3.5c-1.2 1.4 1.2 2.4 0 4M12.5 3.5c-1.2 1.4 1.2 2.4 0 4M16 3.5c-1.2 1.4 1.2 2.4 0 4"
    val BowlSmile = "M4 11h16a8 8 0 0 1-16 0zM9 14.5c1.6 1.6 4.4 1.6 6 0M10 3.8l-1 3M14.5 3.8l-1 3"
    val Bowl = "M3.5 11.5h17a8.5 8.5 0 0 1-17 0z"
    val Steam = "M9 3.5c-1.2 1.4 1.2 2.4 0 4.5M14 3.5c-1.2 1.4 1.2 2.4 0 4.5"
    val Flame = "M12 2.5c1 3.8 5.5 6 5.5 11a5.5 5.5 0 0 1-11 0c0-2.4 1.2-3.7 2.4-4.6 0 2 1 3.2 2.1 3.2 0-4-1.3-6 1-9.6z"
    val FlameInner = "M12 12.5c1.4 1.3 2.6 2.4 2.6 4a2.6 2.6 0 0 1-5.2 0c0-1.5 1.2-2.6 2.6-4z"
    val Target = c(12.0, 12.0, 8.5) + c(12.0, 12.0, 4.8)
    val TargetCenter = c(12.0, 12.0, 1.9)
    val TargetArrow = "M12 12l7.5-7.5M17 4.5h2.5V7"
    val Plate = c(12.0, 12.0, 5.2)
    val ForkKnife = "M3.5 4v5c0 2 3 2 3 0V4M5 4v16M20.5 4c-2.4 1.2-2.4 7 0 7.5V20"
    val PlateHandles = "M5 14.5V20M20.5 14.5V20"
    val Bulb = "M9 18h6M10 21h4M8.5 14.5c-1.6-1.3-2.5-3-2.5-5a6 6 0 0 1 12 0c0 2-.9 3.7-2.5 5-.6.5-1 1.3-1 2.1V17h-5v-.4c0-.8-.4-1.6-1-2.1z"
    val BulbRays = "M12 1v1.5M3.5 4.5l1.1 1.1M20.5 4.5l-1.1 1.1M1 10.5h1.5M21.5 10.5H23"
    val Heart = "M12 20s-7.5-4.6-7.5-10.4A4.2 4.2 0 0 1 12 7a4.2 4.2 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z"
    val Bookmark = "M6 3.5h12v17l-6-3.8-6 3.8zM9.3 8.5h5.4M9.3 11.5h5.4"
    val Search = c(10.8, 10.8, 6.8) + "M20 20l-4.3-4.3"
    val Right = "M9 5.5l6.5 6.5L9 18.5"
    val Left = "M15 5.5L8.5 12l6.5 6.5"
    val Plus = "M12 5v14M5 12h14"
    val Person = c(10.0, 7.5, 3.8) + "M3 20c.7-4.2 3.4-6.5 7-6.5 1.4 0 2.6.3 3.6.9"
    val Gear = c(17.6, 17.6, 1.6) + "M17.6 13.6v1.4M17.6 20.2v1.4M13.6 17.6H15M20.2 17.6h1.4M14.8 14.8l1 1M19.4 19.4l1 1M14.8 20.4l1-1M19.4 15.8l1-1"
    // Linguette dei banner (bianche)
    val Fork = "M4 4v5a3 3 0 0 0 6 0V4M7 4v16M19 4c-3 1.5-4 5-4 9h4M19 4v16"
    val Basket = "M3 9.5h18l-1.8 9.2a2 2 0 0 1-2 1.6H6.8a2 2 0 0 1-2-1.6zM7.5 9.5L11 4M16.5 9.5L13 4M9.5 13v3.5M14.5 13v3.5"
    val Cart = "M2.5 3.5h2.8l2.3 11.5h10.6l2.1-8.3H6.2" + c(9.0, 19.3, 1.5) + c(17.0, 19.3, 1.5) + "M10.5 10h.01M14.5 10h.01"
    val Bars = rr(4.0, 13.0, 3.6, 7.0, 1.0) + rr(10.2, 8.5, 3.6, 11.5, 1.0) + rr(16.4, 4.0, 3.6, 16.0, 1.0)
    val LineChart = "M3.5 3.5v17h17M7 15.5l3.5-4 3 2.5 5.5-6.5" + c(7.0, 15.5, .6) + c(10.5, 11.5, .6) + c(13.5, 14.0, .6)
    val TargetRing = c(11.5, 12.5, 8.5) + c(11.5, 12.5, 5.0) + c(11.5, 12.5, 1.6) + "M11.5 12.5L20 4M17 3.5h3.5V7"
    val Pie = "M11 3.6a8.5 8.5 0 1 0 9.4 9.4H11z" + "M14 2.6v7.4h7.4A7.4 7.4 0 0 0 14 2.6z"
    val ChatPlus = "M12 3.5a8.5 8.5 0 0 0-7.4 12.7L3.5 20.5l4.4-1.1A8.5 8.5 0 1 0 12 3.5zM12 8.5v7M8.5 12h7"
    val Chip = rr(6.0, 6.0, 12.0, 12.0, 2.0) + "M9 3v3M12 3v3M15 3v3M9 18v3M12 18v3M15 18v3M3 9h3M3 12h3M3 15h3M18 9h3M18 12h3M18 15h3M10 15l2-6 2 6M10.7 13h2.6"
    val History = "M3.8 12a8.2 8.2 0 1 0 2.4-5.8M3.5 3.5v4.5H8M12 7.5v5l3.3 2"
    // Pasti (tessere)
    val Cup = "M4 9.5h12.5v4.5a5.5 5.5 0 0 1-5.5 5.5H9.5A5.5 5.5 0 0 1 4 14zM16.5 11h1.5a2.6 2.6 0 0 1 0 5.2h-1.8"
    val CupFill = "M5 13.2h10.5v.8a4.6 4.6 0 0 1-4.6 4.6H9.6A4.6 4.6 0 0 1 5 14z"
    val CupSteam = "M8 3c-1.2 1.3 1.2 2.4 0 4.2M11 3c-1.2 1.3 1.2 2.4 0 4.2M14 3c-1.2 1.3 1.2 2.4 0 4.2"
    val Apple = "M12 7.5c-1.6-1.2-6.5-1.6-7 3.6-.4 4.8 3 9.6 5.2 9.4.8 0 1.2-.5 1.8-.5s1 .5 1.8.5c2.2.2 5.6-4.6 5.2-9.4-.5-5.2-5.4-4.8-7-3.6zM12 7.5c0-1.6-.4-2.8-1.4-4"
    val AppleLeaf = "M12.4 6.4c.4-2.6 2.6-3.9 5-3.5-.3 2.4-2.4 3.9-5 3.5z"
    val Pasta = "M3.5 12h17a8.5 8.5 0 0 1-17 0zM7 12c.5-2.5 2.2-4 4-4.2M10 12c.3-1.8 1.6-3 3.5-3.2M13.5 12c.3-1.4 1.4-2.4 3-2.4"
    val PastaFork = "M18 2.5l-3.5 7.5M20.5 3.5L17 10"
    val Muffin = "M6 12.5h12l-1.5 7a1.5 1.5 0 0 1-1.5 1.2H9a1.5 1.5 0 0 1-1.5-1.2zM5.5 12.5c-1-3.5 1.7-6.3 4.4-6 .5-2 3.7-2.6 5-.4 2.8-.3 4.4 3 3.6 6.4M10.2 15v3.5M13.8 15v3.5"
    val MuffinTop = c(12.0, 4.6, 1.3)
    val Cloche = "M3 18.5h18M4.8 18.5a7.2 7.2 0 0 1 14.4 0M12 8.5v-2"
    val ClocheKnob = c(12.0, 5.0, 1.6) + "M2.5 21h19"
    val Moon = "M19.5 14.2A8 8 0 1 1 9.8 4.5a6.4 6.4 0 0 0 9.7 9.7z"
    val Star = "M17 2.8l.9 2 2 .9-2 .9-.9 2-.9-2-2-.9 2-.9z"
}

/** Icona tipo di pasto dentro la tessera pesca (immagine 4, uguale per tutti i pasti). */
@Composable
fun MealDuoIcon(meal: Int, size: Dp = 30.dp) {
    when (meal) {
        0 -> Box(Modifier.size(size)) { AIcon(RIc.CupFill, RD.Orange, size, fill = true); AIcon(RIc.Cup, RD.Ink, size, 2f); AIcon(RIc.CupSteam, RD.Orange, size, 2f) }
        4 -> Box(Modifier.size(size)) { AIcon(RIc.Apple, RD.Ink, size, 2f); AIcon(RIc.AppleLeaf, RD.Orange, size, fill = true) }
        1 -> Box(Modifier.size(size)) { AIcon(RIc.Pasta, RD.Ink, size, 2f); AIcon(RIc.PastaFork, RD.Orange, size, 2f) }
        3 -> Box(Modifier.size(size)) { AIcon(RIc.Muffin, RD.Ink, size, 2f); AIcon(RIc.MuffinTop, RD.Orange, size, fill = true) }
        2 -> Box(Modifier.size(size)) { AIcon(RIc.Cloche, RD.Ink, size, 2f); AIcon(RIc.ClocheKnob, RD.Orange, size, 2f) }
        else -> Box(Modifier.size(size)) { AIcon(RIc.Moon, RD.Ink, size, 2f); AIcon(RIc.Star, RD.Orange, size, fill = true) }
    }
}

/** Tessera pesca arrotondata con icona (immagini 2, 3, 4, 5, 14, 15). */
@Composable
fun PeachTile(size: Dp, radius: Dp = size * 0.3f, brush: Brush = RD.PeachBrush, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(brush), contentAlignment = Alignment.Center, content = content)
}

/** Separatore verticale arancio tra icona e testo (stesso spessore ovunque). */
@Composable
fun OrangeDivider(height: Dp = 30.dp, modifier: Modifier = Modifier) {
    Box(modifier.width(2.dp).height(height).clip(RoundedCornerShape(1.dp)).background(RD.OrangeLine))
}

/** Freccia in basso dentro il cerchio grigio chiaro (immagini 2, 4, 5). */
@Composable
fun ChevronCircle(size: Dp = 36.dp, bg: Color = RD.GreyFill, ink: Color = RD.Ink, up: Boolean = false) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        AIcon(if (up) "M6 15l6-6 6 6" else RIc.Chevron, ink, size * 0.55f, 2.6f)
    }
}

/** Ombra morbida delle schede di riferimento. */
fun Modifier.refShadow(shape: Shape, elevation: Dp = 8.dp): Modifier =
    this.shadow(elevation.coerceAtMost(6.dp) * 0.25f, shape, ambientColor = Color(0x0A0B1F44), spotColor = Color(0x140B1F44))

// ------------------------------------------------------------------------------- Profilo e calendario

/** Cerchio profilo: foto se presente, altrimenti la sagoma con ingranaggio (immagini 7, 10, 12). */
@Composable
fun RefProfileButton(size: Dp = 56.dp, onClick: () -> Unit = { Nav.go(Screen.Profile) }) {
    val s by Store.state.collectAsState()
    Box(Modifier.size(size).refShadow(CircleShape, 4.dp).clip(CircleShape).background(C.Surface)
        .border(3.dp, Color.White, CircleShape).testTag("header-profile")
        .semantics { contentDescription = "Apri profilo" }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        if (s.profile.photo != null) Avatar(s.profile.name, s.profile.photo, size - 6.dp)
        else ProfileGearIcon(size * 0.62f)
    }
}

@Composable
fun ProfileGearIcon(size: Dp) {
    Box(Modifier.size(size)) {
        Icon(Icons.Outlined.Person, null, tint = RD.Ink, modifier = Modifier.fillMaxSize().padding(end = size * 0.14f, bottom = size * 0.08f))
        Icon(Icons.Default.Settings, null, tint = RD.OrangeDeep, modifier = Modifier.align(Alignment.BottomEnd).size(size * 0.48f).background(C.Surface, CircleShape))
    }
}

/** Pulsante calendario bianco a quadrato arrotondato (immagini 6 e 7). */
@Composable
fun RefCalendarButton(size: Dp = 56.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(Modifier.size(size).refShadow(shape, 4.dp).clip(shape).background(C.Surface).testTag("header-calendar")
        .semantics { contentDescription = "Calendario" }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(RIc.Calendar + RIc.CalendarBand, RD.Ink, size * 0.55f, 2.2f, modifier = Modifier.testTag("header-icon-Calendario"))
    }
}

// ------------------------------------------------------------------------------- Banner

/** Cornice comune dei banner: stessi margini e raggi per tutte le schede. */
@Composable
fun RefBannerFrame(navy: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp)) {
        Column(Modifier.fillMaxWidth().testTag("top-header-frame").refShadow(shape, if (navy) 6.dp else 4.dp).clip(shape)
            .background(if (navy) RD.NavyBrush else RD.WarmBrush)
            .drawBehind {
                // Riflesso curvo in alto a destra, come nei riferimenti.
                val r = size.width * 0.62f
                if (!navy) drawCircle(Color(0x66FFFFFF), r, Offset(size.width * 1.02f, -r * 0.28f))
                            }
            .padding(horizontal = 12.dp, vertical = 8.dp), content = content)
    }
}

/** Linguette con pillola arancio sulla voce attiva e separatori sottili (immagini 9, 10, 11). */
@Composable
fun RefBannerTabs(labels: List<String>, icons: List<String>, selected: Int, onSelect: (Int) -> Unit, actions: Boolean,
    title: String, descriptions: List<String> = labels, labelTags: List<String> = emptyList()) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        fun widthsFor(iconAt: (Int) -> Boolean) = labels.mapIndexed { index, label ->
            val textWidth = with(density) { measurer.measure(label, style = T.Label.copy(fontSize = 14.sp, lineHeight = 19.sp)).size.width.toDp() }
            maxOf(if (iconAt(index)) 80.dp else 64.dp, textWidth + 20.dp + if (iconAt(index)) 26.dp else 0.dp)
        }
        fun sum(w: List<Dp>) = w.fold(0.dp) { acc, width -> acc + width } + 9.dp * (labels.size - 1).coerceAtLeast(0)
        // Se con tutte le icone i tab non entrano, l'icona resta solo sulla voce attiva: niente testo troncato.
        val full = widthsFor { icons.getOrNull(it) != null }
        val onlyActive = sum(full) > maxWidth && labels.size > 2
        val showIcon: (Int) -> Boolean = { icons.getOrNull(it) != null && (!onlyActive || it == selected) }
        val widths = if (onlyActive) widthsFor(showIcon) else full
        val total = sum(widths)
        val extra = if (labels.isEmpty()) 0.dp else (maxWidth - total).coerceAtLeast(0.dp) / labels.size
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            labels.indices.forEach { i ->
                val active = i == selected
                val pill = RoundedCornerShape(10.dp)
                val base = Modifier.width(widths[i] + extra).heightIn(min = 44.dp).testTag("banner-tab-$title-$i")
                    .clip(pill).background(if (active) Brush.horizontalGradient(listOf(Color(0x2EFFFFFF), Color(0x2EFFFFFF))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .semantics { contentDescription = descriptions.getOrElse(i) { labels[i] } }
                val control = if (actions) base.clickable(role = Role.Button) { onSelect(i) }
                    else base.semantics { this.selected = active }.clickable(role = Role.Tab) { onSelect(i) }
                Row(control.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    icons.getOrNull(i)?.takeIf { showIcon(i) }?.let { BannerTabIcon(it, 20.dp); GapW(6.dp) }
                    Text(labels[i], style = T.Label.copy(fontSize = 14.sp, lineHeight = 19.sp, color = Color.White),
                        modifier = Modifier.testTag(labelTags.getOrElse(i) { "banner-label-$title-$i" }), maxLines = 1, softWrap = false)
                }
                if (i < labels.lastIndex) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(Color(0x80C9D3E6)))
            }
        }
    }
}

/** Linguette: Dati ha un’icona piena, le altre sono a tratto. */
@Composable
private fun BannerTabIcon(icon: String, size: Dp) {
    when (icon) {
        RIc.Bars -> AIcon(RIc.Bars, Color.White, size, fill = true)
        else -> AIcon(icon, Color.White, size, 2.1f)
    }
}

/** Campo di ricerca bianco a pillola, con conteggio opzionale (immagini 9-12). */
@Composable
fun RefSearchShell(modifier: Modifier = Modifier, count: String? = null, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface)
        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AIcon(RIc.Search, Color(0xFFA3B1D1), 20.dp, 2.1f); GapW(10.dp)
        content()
        if (count != null) Box(Modifier.padding(start = 6.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF13244C)).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(count, style = T.Title2.copy(fontSize = 14.sp, lineHeight = 18.sp, color = RD.Ink), maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------------------- Loghi dei banner

/** Cappello da chef bianco con fascia arancio (immagini 11 e 12). */
@Composable
fun ChefHatLogo(size: Dp = 44.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawCircle(Color.White, 5.2f * u, Offset(7f * u, 9f * u))
        drawCircle(Color.White, 6.2f * u, Offset(12f * u, 7f * u))
        drawCircle(Color.White, 5.2f * u, Offset(17f * u, 9f * u))
        drawRoundRect(Color.White, Offset(5f * u, 9f * u), Size(14f * u, 6.5f * u), CornerRadius(1.2f * u))
        drawRoundRect(Color(0xFFF3F6FF), Offset(8.3f * u, 12f * u), Size(1.4f * u, 3.6f * u), CornerRadius(.7f * u))
        drawRoundRect(Color(0xFFF3F6FF), Offset(11.3f * u, 12f * u), Size(1.4f * u, 3.6f * u), CornerRadius(.7f * u))
        drawRoundRect(Color(0xFFF3F6FF), Offset(14.3f * u, 12f * u), Size(1.4f * u, 3.6f * u), CornerRadius(.7f * u))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF9B3D), Color(0xFFF26A1B)), startY = 16f * u, endY = 21f * u),
            Offset(5f * u, 16.4f * u), Size(14f * u, 4.4f * u), CornerRadius(1.4f * u))
    }
}

/** Tre barre crescenti pesca, arancio e bianca (immagine 10). */
@Composable
fun ProgressBarsLogo(size: Dp = 44.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawRoundRect(Color(0xFFFFB57A), Offset(2.5f * u, 12.5f * u), Size(5f * u, 9f * u), CornerRadius(1.2f * u))
        drawRoundRect(Color(0xFFF7892F), Offset(9.5f * u, 7.5f * u), Size(5f * u, 14f * u), CornerRadius(1.2f * u))
        drawRoundRect(Color.White, Offset(16.5f * u, 2.5f * u), Size(5f * u, 19f * u), CornerRadius(1.2f * u))
    }
}

/** Calendario bianco con base arancio (immagine 7). */
@Composable
fun PlanCalendarLogo(size: Dp = 46.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF9B3D), Color(0xFFF26A1B))), Offset(2.5f * u, 15f * u), Size(19f * u, 6.5f * u), CornerRadius(2.6f * u))
        drawRect(Color(0xFFF7892F), Offset(2.5f * u, 15f * u), Size(19f * u, 3f * u))
        drawRoundRect(Color.White, Offset(2.5f * u, 4.5f * u), Size(19f * u, 17f * u), CornerRadius(2.8f * u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.9f * u))
        drawLine(Color.White, Offset(2.5f * u, 9.5f * u), Offset(21.5f * u, 9.5f * u), 1.9f * u)
        drawLine(Color.White, Offset(7.5f * u, 2.5f * u), Offset(7.5f * u, 6.5f * u), 1.9f * u, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color.White, Offset(16.5f * u, 2.5f * u), Offset(16.5f * u, 6.5f * u), 1.9f * u, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Testa del robot Jarvis già presente nell'app (immagine 9). */
@Composable
fun JarvisHeadLogo(size: Dp = 52.dp) {
    Image(painterResource(R.drawable.jarvis_logo), null, Modifier.size(size), contentScale = ContentScale.Fit)
}

// ------------------------------------------------------------------------------- Pulsanti di scelta

enum class OptionStyle { Plain, Navy, Peach }

/**
 * Pulsante di scelta con icona, separatore arancio e testo (immagini 2 e 3): bordo navy sottile,
 * selezione navy piena (periodi) oppure pesca con bordo arancio (alimentazione, tipo di giornata).
 */
@Composable
fun RefOption(label: String, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null, subtitle: String? = null,
    isSelected: Boolean = false, selectedStyle: OptionStyle = OptionStyle.Peach, tag: String? = null, enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null, minHeight: Dp = 52.dp, centered: Boolean = false, labelSize: androidx.compose.ui.unit.TextUnit = 16.sp, onClick: () -> Unit) {
    val compactPeriod = tag?.startsWith("diet-period-") == true
    val shape = RoundedCornerShape(16.dp)
    val navy = isSelected && selectedStyle == OptionStyle.Navy
    val peach = isSelected && selectedStyle == OptionStyle.Peach
    val bg = when { navy -> RD.ButtonBrush; peach -> Brush.horizontalGradient(listOf(Color(0xFF13244C), Color(0xFF13244C))); else -> Brush.horizontalGradient(listOf(Color.White, Color.White)) }
    Row(modifier.heightIn(min = minHeight).then(if (tag != null) Modifier.testTag(tag) else Modifier)
        .semantics { selected = isSelected }
        .then(if (navy) Modifier.refShadow(shape, 3.dp) else Modifier).clip(shape).background(bg)
        .border(if (peach) 1.5.dp else 1.dp, when { peach -> C.Royal; navy -> C.Royal; else -> C.Royal.copy(alpha = if (enabled) 0.35f else 0.15f) }, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = if (compactPeriod) 10.dp else 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start) {
        if (icon != null) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            GapW(if (compactPeriod) 8.dp else 12.dp); OrangeDivider(if (subtitle != null) 40.dp else if (compactPeriod) 24.dp else 28.dp); GapW(if (compactPeriod) 8.dp else 14.dp)
        }
        Column(if (centered) Modifier else Modifier.weight(1f)) {
            val labelStyle = T.Title2.copy(fontSize = labelSize, lineHeight = labelSize * 1.3f, color = if (navy) Color.White else if (enabled) RD.Ink else C.Muted)
            // Le scelte affiancate (periodi) restano su una riga.
            val labelModifier = if (tag != null) Modifier.testTag("$tag-label") else Modifier
            if (centered) AutoFitText(label, labelStyle, modifier = labelModifier, minScale = 0.75f) else Text(label, modifier = labelModifier, style = labelStyle)
            if (subtitle != null) Text(subtitle, style = T.Small.copy(fontSize = 13.sp, lineHeight = 17.sp, color = if (navy) Color(0xFFDDE4F2) else Color(0xFFA3B1D1)))
        }
        if (trailing != null) { GapW(8.dp); trailing() }
        else if (isSelected) {
            GapW(8.dp)
            if (peach) Box(Modifier.size(40.dp).clip(CircleShape).background(RD.PeachDeep), contentAlignment = Alignment.Center) { AIcon(RIc.Check, RD.Orange, 24.dp, 3f) }
            else AIcon(RIc.Check, RD.Orange, if (compactPeriod) 20.dp else 24.dp, 3f)
        }
    }
}

/** Pulsante principale navy con icona arancio e separatore (immagini 1, 2, 3). */
@Composable
fun RefPrimaryButton(text: String, modifier: Modifier = Modifier, icon: String? = null, tag: String? = null, enabled: Boolean = true,
    height: Dp = 52.dp, divider: Boolean = true, singleLine: Boolean = false, textSize: androidx.compose.ui.unit.TextUnit = 16.sp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.heightIn(min = height).then(if (tag != null) Modifier.testTag(tag) else Modifier).refShadow(shape, if (enabled) 4.dp else 0.dp).clip(shape)
        .background(if (enabled) RD.ButtonBrush else Brush.verticalGradient(listOf(C.SurfaceHi, C.SurfaceHi)))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = if (divider) 16.dp else 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            AIcon(icon, if (divider) RD.Orange else Color.White, if (divider) 22.dp else 20.dp, 2.4f)
            if (divider) { GapW(10.dp); Box(Modifier.width(1.dp).height(24.dp).background(Color(0x66FFFFFF))); GapW(10.dp) } else GapW(8.dp)
        }
        val style = T.Label.copy(fontSize = textSize, lineHeight = textSize * 1.25f, color = if (enabled) Color.White else C.Muted)
        Text(text, modifier = Modifier.weight(1f, fill = false), style = style, textAlign = TextAlign.Center)
    }
}

/** Pulsante secondario bianco con bordo navy (immagini 1 e 2). */
@Composable
fun RefOutlineButton(text: String, modifier: Modifier = Modifier, icon: String? = null, tag: String? = null, enabled: Boolean = true,
    height: Dp = 50.dp, divider: Boolean = true, singleLine: Boolean = false, textSize: androidx.compose.ui.unit.TextUnit = 16.sp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.heightIn(min = height).then(if (tag != null) Modifier.testTag(tag) else Modifier).clip(shape).background(C.Surface)
        .border(1.5.dp, Color(0xFF1B4FD6).copy(alpha = 0.55f), shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = if (divider) 14.dp else 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            AIcon(icon, RD.Ink, if (divider) 24.dp else 20.dp, 2.4f)
            if (divider) { GapW(10.dp); OrangeDivider(24.dp); GapW(10.dp) } else GapW(8.dp)
        }
        val style = T.Label.copy(fontSize = textSize, lineHeight = textSize * 1.25f, color = RD.Ink)
        Text(text, modifier = Modifier.weight(1f, fill = false), style = style, textAlign = TextAlign.Center)
    }
}

/** Testo su una riga che si riduce leggermente invece di andare a capo o venire troncato. */
@Composable
fun AutoFitText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, minScale: Float = 0.62f,
    textAlign: TextAlign? = null) {
    var scale by androidx.compose.runtime.remember(text, style) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Text(text, modifier, style = style.copy(fontSize = if (style.fontSize.isSpecified) style.fontSize * scale else style.fontSize,
        lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight), maxLines = 1, softWrap = false,
        overflow = if (scale <= minScale) TextOverflow.Ellipsis else TextOverflow.Clip, textAlign = textAlign,
        onTextLayout = { if (it.hasVisualOverflow && scale > minScale) scale = (scale - 0.06f).coerceAtLeast(minScale) })
}

/**
 * Come [AutoFitText], ma più testi dello stesso gruppo (schede affiancate, righe di un elenco)
 * condividono la riduzione: restano tutti della stessa dimensione invece di averne uno più piccolo.
 */
@Composable
fun SharedFitText(text: String, style: androidx.compose.ui.text.TextStyle, scale: androidx.compose.runtime.MutableFloatState,
    modifier: Modifier = Modifier, minScale: Float = 0.72f) {
    val k = scale.floatValue
    Text(text, modifier, style = style.copy(fontSize = if (style.fontSize.isSpecified) style.fontSize * k else style.fontSize,
        lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * k else style.lineHeight), maxLines = 1, softWrap = false,
        overflow = if (k <= minScale) TextOverflow.Ellipsis else TextOverflow.Clip,
        onTextLayout = { if (it.hasVisualOverflow && scale.floatValue > minScale) scale.floatValue = (scale.floatValue - 0.04f).coerceAtLeast(minScale) })
}
