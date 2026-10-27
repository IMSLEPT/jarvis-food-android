package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jarvis.food.R

/**
 * Tema "Aurora" di Jarvis Food: sfondo ardesia chiaro, schede bianche, blu navy #17286B e arancione #F25E00.
 * Tutti i colori dell'app sono qui.
 */
object C {
    val Bg = Color(0xFFF8F9F8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF2F4F5)
    val SurfaceHi = Color(0xFFE7EBEF)
    val Line = Color(0xFFE9ECF1)
    val Text = Color(0xFF15264F)
    val Muted = Color(0xFF58647C)
    val Faint = Color(0xFFA6A4BC)
    val Accent = Color(0xFFF25E00)
    // Orange reserved for white button labels; the brighter brand accent stays on icons and charts.
    val ButtonOrange = Color(0xFFC94D00)
    val ButtonOrangeEnd = Color(0xFFBA4000)
    val AccentSoft = Color(0xFFFFE9E1)
    val AccentDeep = Color(0xFFC93A1F)
    val OnAccent = Color(0xFFFFFFFF)
    val Navy = Color(0xFF172653)
    val NavySoft = Color(0xFFE4E9F6)
    val NavyDeep = Color(0xFF132044)
    val Ink = Navy
    val Green = Color(0xFF10B981)
    val GreenSoft = Color(0xFFD9F6EA)
    val Blue = Color(0xFF3B82F6)
    val BlueSoft = Color(0xFFE3EEFF)
    val Gold = Color(0xFFF59E0B)
    val GoldSoft = Color(0xFFFFF1D2)
    val Carbs = Blue
    val Protein = Color(0xFFF43F6E)
    val Fat = Gold
    val Water = Color(0xFF5CD6EA)
    val Fasting = Color(0xFF8B5CF6)
    val Danger = Color(0xFFE5484D)
    val DangerSoft = Color(0xFFFDE3E4)

    /** Arancione dei pulsanti principali (bianco sopra). */
    val OrangeBrush = Brush.verticalGradient(listOf(ButtonOrange, ButtonOrangeEnd))
    /** Sfondo blu navy della scheda calorie. */
    val NavyBrush = Brush.linearGradient(listOf(NavyDeep, Color(0xFF203565)))
    val OrangeArc = listOf(Color(0xFFF25E00), Color(0xFFFF8A1C), Color(0xFFFFB02E))
    /** Neutral card fills; semantic colours stay on text, icons and chart marks. */
    fun cardSurface(tone: Color): Color =
        if (tone.alpha > 0.5f && tone.luminance() > 0.60f) Surface else tone
}

val Bricolage = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.Medium),
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold)
)

val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold)
)

object T {
    private val tight = (-0.6).sp
    val Display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = tight, color = C.Text)
    val H1 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp, color = C.Text)
    val H2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.4).sp, color = C.Text)
    val Title = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp, color = C.Text)
    val Title2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = C.Text)
    val Dish = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp, color = C.Text)
    val DishBig = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp, color = C.Text)
    val Body = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = C.Text)
    val Read = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = C.Text)
    val Muted = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val Small = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, color = C.Muted)
    val Label = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 19.sp, color = C.Text)
    val Button = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.OnAccent)
    val BtnL = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 16.5.sp, color = C.OnAccent)
    val BtnM = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 20.sp, color = C.OnAccent)
    val BtnS = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = C.Text)
    val Chip = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = C.Text)
    val Link = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = C.AccentDeep)
    val Eyebrow = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp, color = C.Muted)
    val Num = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, color = C.Text)
    val Big = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1).sp, color = C.Text)
    val Hero = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.6).sp, color = C.Text)
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Accent, onPrimary = C.OnAccent, secondary = C.Navy, background = C.Bg, onBackground = C.Text,
            surface = C.Surface, onSurface = C.Text, surfaceVariant = C.SurfaceAlt, onSurfaceVariant = C.Muted,
            outline = C.Line, error = C.Danger, surfaceContainer = C.Surface, surfaceContainerHigh = C.Surface,
            surfaceContainerLow = C.Surface, surfaceContainerHighest = C.SurfaceAlt
        )
    ) {
        CompositionLocalProvider(LocalTextStyle provides T.Body) { content() }
    }
}

/** Sfondo del design: ardesia chiaro in verticale con un alone bianco in alto. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color.White, Color.Transparent), center = Offset(size.width / 2f, -60.dp.toPx()), radius = 420.dp.toPx()))
            },
        content = content
    )
}
