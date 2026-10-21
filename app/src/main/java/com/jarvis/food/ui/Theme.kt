package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jarvis.food.R

/** Tema "Dark premium" di Jarvis Food. Tutti i colori sono qui: per rifare la grafica basta cambiare questo file. */
object C {
    val Bg = Color(0xFF0B0D0C)
    val Surface = Color(0xFF151918)
    val SurfaceHi = Color(0xFF1F2422)
    val Line = Color(0x1AF4F6F2)
    val Text = Color(0xFFF4F6F2)
    val Muted = Color(0xFF9BA39E)
    val Faint = Color(0xFF5F6662)
    val Accent = Color(0xFFC5F06A)
    val AccentDeep = Color(0xFF8DBB2E)
    val OnAccent = Color(0xFF0B0D0C)
    val Carbs = Color(0xFF7DB7FF)
    val Protein = Color(0xFFFF8A7A)
    val Fat = Color(0xFFFFC76B)
    val Water = Color(0xFF5EC8FF)
    val Fasting = Color(0xFFA98BFF)
    val Danger = Color(0xFFFF6B6B)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

object T {
    val Display = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, color = C.Text)
    val Title = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = C.Text)
    val Title2 = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = C.Text)
    val Body = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = C.Text)
    val Muted = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val Small = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, color = C.Muted)
    val Label = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = C.Text)
    val Button = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.OnAccent)
    val Big = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, color = C.Text)
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Accent,
            onPrimary = C.OnAccent,
            secondary = C.Carbs,
            background = C.Bg,
            onBackground = C.Text,
            surface = C.Surface,
            onSurface = C.Text,
            surfaceVariant = C.SurfaceHi,
            onSurfaceVariant = C.Muted,
            outline = C.Line,
            error = C.Danger
        )
    ) {
        CompositionLocalProvider(LocalTextStyle provides T.Body) { content() }
    }
}

/** Sfondo scuro con un alone verde-lime in alto. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(C.Accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.85f, 0f)
                )
            },
        content = content
    )
}
