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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jarvis.food.R

/**
 * Tema chiaro di Jarvis Food. Palette: crema #FAF9F5, inchiostro #141413, terracotta #D97757,
 * blu #6A9BCC, verde salvia #788C5D. Tutti i colori dell'app sono qui.
 */
object C {
    val Bg = Color(0xFFFAF9F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF3F1EA)
    val SurfaceHi = Color(0xFFE8E6DC)
    val Line = Color(0xFFE8E6DC)
    val Text = Color(0xFF141413)
    val Muted = Color(0xFF6E6C64)
    val Faint = Color(0xFFB0AEA5)
    val Accent = Color(0xFFD97757)
    val AccentSoft = Color(0xFFF7E3DA)
    val AccentDeep = Color(0xFFB85C3E)
    val OnAccent = Color(0xFFFFFFFF)
    val Ink = Color(0xFF141413)
    val Green = Color(0xFF788C5D)
    val GreenSoft = Color(0xFFE6EBDD)
    val Blue = Color(0xFF6A9BCC)
    val BlueSoft = Color(0xFFE1EBF5)
    val Gold = Color(0xFFD9A441)
    val GoldSoft = Color(0xFFF7EBD2)
    val Carbs = Blue
    val Protein = Accent
    val Fat = Gold
    val Water = Blue
    val Fasting = Color(0xFF8B7BB8)
    val Danger = Color(0xFFC2453B)
    val DangerSoft = Color(0xFFF6DEDB)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_semibold, FontWeight.SemiBold),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic)
)

object T {
    val Display = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, color = C.Text)
    val Title = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = C.Text)
    val Title2 = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = C.Text)
    val Dish = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, color = C.Text)
    val DishBig = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, color = C.Text)
    val Body = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = C.Text)
    val Read = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp, color = C.Text)
    val Muted = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val Small = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp, color = C.Muted)
    val Label = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = C.Text)
    val Button = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.OnAccent)
    val Num = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp, color = C.Text)
    val Big = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 42.sp, color = C.Text)
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Accent, onPrimary = C.OnAccent, secondary = C.Blue, background = C.Bg, onBackground = C.Text,
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
