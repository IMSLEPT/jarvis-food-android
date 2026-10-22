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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val Bg = Color(0xFFF3F5F8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFEBEEF4)
    val SurfaceHi = Color(0xFFDFE4EC)
    val Line = Color(0xFFE2E6EE)
    val Text = Color(0xFF141B34)
    val Muted = Color(0xFF667089)
    val Faint = Color(0xFFA7AFC0)
    val Accent = Color(0xFFF25E00)
    val AccentSoft = Color(0xFFFDEADB)
    val AccentDeep = Color(0xFFD14E00)
    val OnAccent = Color(0xFFFFFFFF)
    val Navy = Color(0xFF17286B)
    val NavySoft = Color(0xFFE4E9F6)
    val NavyDeep = Color(0xFF13225F)
    val Ink = Navy
    val Green = Color(0xFF5E9B7A)
    val GreenSoft = Color(0xFFE2F0E9)
    val Blue = Color(0xFF6C8FD8)
    val BlueSoft = Color(0xFFE3EBF9)
    val Gold = Color(0xFFF2B04A)
    val GoldSoft = Color(0xFFFBEED5)
    val Carbs = Blue
    val Protein = Color(0xFFF7A07A)
    val Fat = Gold
    val Water = Blue
    val Fasting = Color(0xFF8B8FC8)
    val Danger = Color(0xFFC2453B)
    val DangerSoft = Color(0xFFF6DEDB)

    /** Arancione dei pulsanti principali (bianco sopra). */
    val OrangeBrush = Brush.verticalGradient(listOf(Color(0xFFFF8A1C), Color(0xFFF25E00)))
    /** Sfondo blu navy della scheda calorie. */
    val NavyBrush = Brush.linearGradient(listOf(Color(0xFF13225F), Color(0xFF1B3382), Color(0xFF2447A3)))
    val OrangeArc = listOf(Color(0xFFF25E00), Color(0xFFFF8A1C), Color(0xFFFFB02E))
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
    val Display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 33.sp, color = C.Text)
    val Title = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = C.Text)
    val Title2 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = C.Text)
    val Dish = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, color = C.Text)
    val DishBig = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = C.Text)
    val Body = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = C.Text)
    val Read = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp, color = C.Text)
    val Muted = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val Small = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp, color = C.Muted)
    val Label = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = C.Text)
    val Button = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.OnAccent)
    val Num = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, color = C.Text)
    val Big = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 42.sp, color = C.Text)
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

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(C.Bg), content = content)
}
