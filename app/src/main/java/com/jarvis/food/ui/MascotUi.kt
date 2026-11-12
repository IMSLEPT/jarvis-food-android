package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.food.R

/** Anello arancione luminoso inclinato dietro la mascotte, come nelle immagini di riferimento. */
@Composable
fun MascotRing(modifier: Modifier = Modifier, tilt: Float = -12f) {
    Canvas(modifier.rotate(tilt)) {
        val w = size.width; val h = size.height
        val oval = Size(w * 0.98f, h * 0.36f)
        val tl = Offset((w - oval.width) / 2f, h * 0.60f)
        val glow = Brush.horizontalGradient(listOf(Color(0x00FF9A2E), Color(0xFFFFA63D), Color(0xFFFFD27A), Color(0x00FF9A2E)))
        drawOval(glow, tl, oval, style = Stroke(width = h * 0.075f), alpha = 0.28f)
        drawOval(glow, tl, oval, style = Stroke(width = h * 0.028f))
    }
}

/** Mascotte "pollice in su" di Oggi, con alone. Il bordo inferiore si nasconde dietro la scheda sottostante. */
@Composable
fun MascotOggi(height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(height * 1.06f, height), contentAlignment = Alignment.BottomCenter) {
        MascotRing(Modifier.fillMaxSize())
        Image(painterResource(R.drawable.mascot_oggi), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/** Mascotte che saluta, usata come banner della chat. */
@Composable
fun MascotChat(height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(height * 1.07f, height), contentAlignment = Alignment.BottomCenter) {
        MascotRing(Modifier.fillMaxSize(), tilt = -10f)
        Image(painterResource(R.drawable.mascot_chat), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/** Riquadro con sfumatura e icona bianca a tono pieno: lo stesso linguaggio dei tile di Oggi. */
@Composable
fun GlowIcon(icon: String, top: Color, bottom: Color, box: Dp = 44.dp, radius: Dp = 14.dp, glyph: Dp = box * 0.55f, duo: Boolean = true, modifier: Modifier = Modifier,
    solid: Boolean = false, accent: String? = null) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(box).clip(shape).background(Brush.verticalGradient(listOf(top, bottom))), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent), endY = box.value * 1.4f)))
        if (solid) AIcon(icon, Color.White, glyph, fill = true) else AIcon(icon, Color.White, glyph, 1.9f, duo = duo)
        if (accent != null) AIcon(accent, bottom, glyph, 2.2f)
    }
}

/** Colori (alto, basso) dei riquadri icona. */
object Tone {
    val Orange = Color(0xFFFF9438) to Color(0xFFF2600F)
    val Blue = Color(0xFF4C8DFF) to Color(0xFF1B4FD6)
    val Purple = Color(0xFF9B7BFF) to Color(0xFF6D3BE0)
    val Teal = Color(0xFF2FD3B8) to Color(0xFF0E9C8F)
    val Green = Color(0xFF3DDC84) to Color(0xFF12A150)
    val Pink = Color(0xFFFF7AA2) to Color(0xFFE23C6E)
    val Gold = Color(0xFFFFCB47) to Color(0xFFF5A000)
    val Indigo = Color(0xFF5B6CFF) to Color(0xFF2B3BC4)
    val Sky = Color(0xFF52C8F5) to Color(0xFF1E88D8)
}
