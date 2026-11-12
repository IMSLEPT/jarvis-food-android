package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
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
 * Palette C di Jarvis Food: fondo caldo chiaro, schede bianche, navy e arancio.
 * Tutti i colori dell'app sono qui.
 */
object C {
    // Tela blu profondo, superfici vetro scuro, testo quasi bianco, arancio luminoso come unico accento.
    val Bg = Color(0xFF050D24)
    val Surface = Color(0xFF0C1A3D)
    val SurfaceAlt = Color(0xFF13244C)
    val SurfaceHi = Color(0xFF1A2F5E)
    val Line = Color(0xFF1E3263)
    val ProfileBorder = Color(0xFF1E3263)
    val Text = Color(0xFFF3F6FF)
    val Muted = Color(0xFFA3B1D1)
    val Faint = Color(0xFF8091B8)
    val Accent = Color(0xFFFF7A1A)
    val ButtonOrange = Color(0xFFFF6A10)
    val ButtonOrangeEnd = Color(0xFFFF5A0A)
    val AccentSoft = Color(0x26FF7A1A)
    val AccentDeep = Color(0xFFFF9A55)
    val OnAccent = Color(0xFFFFFFFF)
    /** Inchiostro chiaro: testo e icone su fondo scuro (ex navy). */
    val Navy = Color(0xFFF3F6FF)
    val NavySoft = Color(0x1F5C8DFF)
    val NavyDeep = Color(0xFF0B1B4A)
    /** Blu elettrico pieno per riempimenti selezionati. */
    val Royal = Color(0xFF1F5BE6)
    val Ink = Navy
    val OnOrange = Color.White
    val ProfileSurface = Surface
    val ProfileMuted = Muted
    val ProfileOrange = Color(0xFFFF8A3D)
    val Green = Color(0xFF34D399)
    val GreenSoft = Color(0x2634D399)
    val Blue = Color(0xFF5C93FF)
    val BlueSoft = Color(0x265C93FF)
    val Gold = Color(0xFFFFB83D)
    val GoldSoft = Color(0x26FFB83D)
    val Carbs = Blue
    val Protein = Color(0xFFFF6B8E)
    val Fat = Gold
    val Water = Color(0xFF4FD1F5)
    val Fasting = Blue
    val Danger = Color(0xFFFF6B6F)
    val DangerSoft = Color(0x26FF6B6F)

    val OrangeBrush = Brush.linearGradient(listOf(Color(0xFFFF8A2A), Color(0xFFFF5A0A)))
    /** Blu elettrico lucido della scheda principale. */
    val NavyBrush = Brush.verticalGradient(listOf(Color(0xFF0E34A0), Color(0xFF1B52D6)))
    val OrangeArc = listOf(Color(0xFFFF7A1A), Color(0xFFFF9A3C), Color(0xFFFFB866))
    fun cardSurface(tone: Color): Color =
        if (tone.alpha > 0.5f && tone.luminance() > 0.60f) Surface else tone
}

/** Stile Apple: un solo sans neutro (DM Sans) anche per i titoli. */
val Bricolage = FontFamily(
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_semibold, FontWeight.Bold),
    Font(R.font.dmsans_semibold, FontWeight.ExtraBold)
)

val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold)
)

object T {
    private val tight = (-0.6).sp
    val Display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = tight, color = C.Text)
    val H1 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.8).sp, color = C.Text)
    val H2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp, color = C.Text)
    val Title = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.4).sp, color = C.Text)
    val Title2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = C.Text)
    val Dish = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp, color = C.Text)
    val DishBig = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp, color = C.Text)
    val Body = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = C.Text)
    val Read = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = C.Text)
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
    val Big = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp, color = C.Text)
    val Hero = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 50.sp, lineHeight = 54.sp, letterSpacing = (-1.6).sp, color = C.Text)
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Accent, onPrimary = C.OnAccent, secondary = C.AccentDeep, background = C.Bg, onBackground = C.Text,
            surface = C.Surface, onSurface = C.Text, surfaceVariant = C.SurfaceAlt, onSurfaceVariant = C.Muted,
            outline = C.Line, error = C.Danger, surfaceContainer = C.Surface, surfaceContainerHigh = C.Surface,
            surfaceContainerLow = C.Surface, surfaceContainerHighest = C.SurfaceAlt
        ),
        typography = Typography(
            headlineLarge = T.H1, headlineMedium = T.Title, headlineSmall = T.H2,
            titleLarge = T.Title2, titleMedium = T.Label, titleSmall = T.Label,
            bodyLarge = T.Read, bodyMedium = T.Body, bodySmall = T.Small,
            labelLarge = T.BtnM.copy(color = Color.Unspecified), labelMedium = T.Label, labelSmall = T.Small
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(14.dp), extraLarge = RoundedCornerShape(16.dp))
    ) {
        CompositionLocalProvider(LocalTextStyle provides T.Body) { content() }
    }
}

/** Tela blu profondo con alone blu in alto e brace arancione sul fondo. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color(0x662B6BFF), Color.Transparent), center = Offset(size.width * 0.15f, -60.dp.toPx()), radius = 460.dp.toPx()))
                drawRect(Brush.radialGradient(listOf(Color(0x26FF7A1A), Color.Transparent), center = Offset(size.width * 1.1f, size.height * 0.92f), radius = 420.dp.toPx()))
            },
        content = content
    )
}
