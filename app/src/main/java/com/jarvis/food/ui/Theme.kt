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
 * Palette C di Jarvis Food: fondo caldo chiaro, schede bianche, navy e arancio.
 * Tutti i colori dell'app sono qui.
 */
object C {
    val Bg = Color(0xFF081533)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFEBEAE7)
    val SurfaceHi = Color(0xFFE4E8EF)
    val Line = Color(0xFFE4E8EF)
    val ProfileBorder = Color(0xFFE4E8EF)
    val Text = Color(0xFF09194D)
    val Muted = Color(0xFF5E6678)
    val Faint = Color(0xFF6B7280)
    val Accent = Color(0xFFEA7138)
    // Brand orange stays on icons; white button labels need the darker variant.
    val ButtonOrange = Color(0xFFFF7A1A)
    val ButtonOrangeEnd = Color(0xFFFF5A0A)
    val AccentSoft = Color(0xFFFFF2EA)
    val AccentDeep = Color(0xFFB94C1C)
    val OnAccent = Color(0xFFFFFFFF)
    val Navy = Color(0xFF09194D)
    val NavySoft = Color(0xFFECF2FE)
    val NavyDeep = Color(0xFF14234B)
    val Ink = Navy
    val OnOrange = Color.White
    val ProfileSurface = Color(0xFFFEFEFE)
    val ProfileMuted = C.Muted
    val ProfileOrange = Color(0xFFDC6F2C)
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
    val Fasting = Navy
    val Danger = Color(0xFFE5484D)
    val DangerSoft = Color(0xFFFDE3E4)

    /** Arancione del + Aggiungi; scritte bianche come richiesto. */
    val OrangeBrush = Brush.verticalGradient(listOf(ButtonOrange, ButtonOrangeEnd))
    /** Sfondo blu navy della scheda calorie. */
    val NavyBrush = Brush.linearGradient(listOf(Color(0xFF0C2A7A), Color(0xFF1349BF), Color(0xFF2A7BF0)))
    val OrangeArc = listOf(Color(0xFFEA7138), Color(0xFFF68A46), Color(0xFFFFAD60))
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
    val Display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = tight, color = C.Text)
    val H1 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.8).sp, color = C.Text)
    val H2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp, color = Color.White)
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
    val Link = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFFFF9A4D))
    val Eyebrow = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp, color = C.Muted)
    val Num = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, color = C.Text)
    val Big = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp, color = C.Text)
    val Hero = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 50.sp, lineHeight = 54.sp, letterSpacing = (-1.6).sp, color = C.Text)
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Navy, onPrimary = C.OnAccent, secondary = C.AccentDeep, background = C.Bg, onBackground = C.Text,
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
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(24.dp))
    ) {
        CompositionLocalProvider(LocalTextStyle provides T.Body) { content() }
    }
}

/** Sfondo caldo chiaro con un alone avorio in alto. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF081533), Color(0xFF0A1F52), Color(0xFF081533))))
                drawRect(Brush.radialGradient(listOf(Color(0x661F7BFF), Color.Transparent), center = Offset(size.width * 0.95f, -20.dp.toPx()), radius = 520.dp.toPx()))
                drawRect(Brush.radialGradient(listOf(Color(0x33FF6A00), Color.Transparent), center = Offset(size.width * 1.05f, size.height * 0.92f), radius = 460.dp.toPx()))
            },
        content = content
    )
}
