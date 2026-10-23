package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Indici dei pasti del diario: 0 colazione, 1 pranzo, 2 cena, 3 merenda, 4 spuntino mattutino, 5 spuntino serale. */
val MEAL_ORDER = listOf(0, 4, 1, 3, 2, 5)
val MEAL_LABEL = mapOf(0 to "Colazione", 4 to "Spuntino mattutino", 1 to "Pranzo", 3 to "Merenda", 2 to "Cena", 5 to "Spuntino serale")

/** Nome breve per chip e pulsanti stretti. */
val MEAL_SHORT = mapOf(0 to "Colazione", 4 to "Spuntino AM", 1 to "Pranzo", 3 to "Merenda", 2 to "Cena", 5 to "Spuntino PM")

/** Pasto consigliato in base all'ora. */
fun mealForHour(h: Int): Int = when (h) {
    in 5..8 -> 0; 9, 10 -> 4; in 11..14 -> 1; in 15..17 -> 3; in 18..21 -> 2; else -> 5
}

/** Riquadro con una piccola illustrazione colorata del pasto (disegnata, senza emoji). */
@Composable
fun MealArt(meal: Int, box: Dp = 48.dp, radius: Dp = 16.dp, modifier: Modifier = Modifier) {
    val (c1, c2) = when (meal) {
        0 -> Color(0xFFFFEBBE) to Color(0xFFFFD27A)
        4 -> Color(0xFFFFE6CC) to Color(0xFFFFC48A)
        1 -> Color(0xFFD9F6E8) to Color(0xFFA7E8C8)
        3 -> Color(0xFFFFE0E6) to Color(0xFFFFB9C7)
        2 -> Color(0xFFE1EBFF) to Color(0xFFB7CEFF)
        else -> Color(0xFF454A8F) to Color(0xFF282B63)
    }
    Box(modifier.size(box).clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(c1, c2))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(box * 0.8f)) { drawMeal(meal, size.minDimension / 48f) }
    }
}

private fun sparkle(cx: Float, cy: Float, r: Float): Path = Path().apply {
    moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r)
    quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
}

private fun DrawScope.drawMeal(meal: Int, u: Float) {
    when (meal) {
        0 -> { // tazza di caffè con vapore
            drawOval(Color(0x33000000), Offset(7 * u, 38 * u), Size(34 * u, 6 * u))
            drawOval(Color.White, Offset(6 * u, 36 * u), Size(36 * u, 7 * u))
            drawArc(Color(0xFFE8573A), -80f, 160f, false, Offset(29 * u, 20 * u), Size(12 * u, 12 * u), style = Stroke(3.2f * u))
            val body = Path().apply {
                moveTo(9 * u, 19 * u); lineTo(35 * u, 19 * u); lineTo(33 * u, 34 * u)
                quadraticTo(32 * u, 39 * u, 27 * u, 39 * u); lineTo(17 * u, 39 * u); quadraticTo(12 * u, 39 * u, 11 * u, 34 * u); close()
            }
            drawPath(body, Brush.verticalGradient(listOf(Color(0xFFFF9A62), Color(0xFFE8573A)), startY = 19 * u, endY = 39 * u))
            drawOval(Color(0xFF6B3A24), Offset(9 * u, 16.5f * u), Size(26 * u, 6 * u))
            drawOval(Color(0x55FFFFFF), Offset(13 * u, 17.5f * u), Size(8 * u, 2 * u))
            val steam = Path().apply {
                moveTo(17 * u, 13 * u); cubicTo(13 * u, 10 * u, 21 * u, 8 * u, 17 * u, 3 * u)
                moveTo(26 * u, 13 * u); cubicTo(22 * u, 10 * u, 30 * u, 8 * u, 26 * u, 3 * u)
            }
            drawPath(steam, Color(0xCCFFFFFF), style = Stroke(2.4f * u, cap = StrokeCap.Round))
        }
        4 -> { // arancia con foglia
            drawOval(Color(0x2A000000), Offset(10 * u, 41 * u), Size(28 * u, 5 * u))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB23D), Color(0xFFF26B00)), center = Offset(19 * u, 21 * u), radius = 22 * u), 16 * u, Offset(24 * u, 27 * u))
            drawCircle(Color(0x66FFFFFF), 4 * u, Offset(17 * u, 20 * u))
            drawCircle(Color(0xFF8A4B06), 1.4f * u, Offset(24 * u, 12 * u))
            val leaf = Path().apply {
                moveTo(25 * u, 11 * u); cubicTo(28 * u, 3 * u, 37 * u, 4 * u, 38 * u, 6 * u)
                cubicTo(36 * u, 13 * u, 29 * u, 14 * u, 25 * u, 11 * u); close()
            }
            drawPath(leaf, Color(0xFF3FA66B))
            drawLine(Color(0xFF2B7D4E), Offset(26 * u, 11 * u), Offset(35 * u, 7 * u), 1.2f * u, StrokeCap.Round)
        }
        1 -> { // ciotola colorata (insalata)
            drawCircle(Color(0xFFEF4B4B), 6.5f * u, Offset(15 * u, 25 * u))
            drawCircle(Color(0xFF3FB871), 7.5f * u, Offset(24 * u, 21 * u))
            drawCircle(Color(0xFFFFC53D), 5.5f * u, Offset(33 * u, 25 * u))
            drawCircle(Color(0xFF9BDB6A), 4 * u, Offset(21 * u, 27 * u))
            drawCircle(Color(0xFFF59E0B), 3.5f * u, Offset(29 * u, 18 * u))
            drawArc(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCFE3F5)), startY = 30 * u, endY = 46 * u), 0f, 180f, true, Offset(5 * u, 18 * u), Size(38 * u, 28 * u))
            drawRoundRect(Color.White, Offset(3 * u, 30 * u), Size(42 * u, 4.5f * u), CornerRadius(2.2f * u))
            drawRoundRect(Color(0x1A000000), Offset(3 * u, 34 * u), Size(42 * u, 1.2f * u), CornerRadius(u))
        }
        3 -> { // mela
            drawOval(Color(0x2A000000), Offset(10 * u, 41 * u), Size(28 * u, 5 * u))
            val apple = Path().apply {
                moveTo(24 * u, 14 * u)
                cubicTo(30 * u, 8 * u, 43 * u, 14 * u, 40 * u, 28 * u)
                cubicTo(38 * u, 38 * u, 30 * u, 43 * u, 24 * u, 40 * u)
                cubicTo(18 * u, 43 * u, 10 * u, 38 * u, 8 * u, 28 * u)
                cubicTo(5 * u, 14 * u, 18 * u, 8 * u, 24 * u, 14 * u); close()
            }
            drawPath(apple, Brush.radialGradient(listOf(Color(0xFFFF6B6B), Color(0xFFD7263D)), center = Offset(17 * u, 21 * u), radius = 26 * u))
            drawOval(Color(0x66FFFFFF), Offset(12 * u, 18 * u), Size(5 * u, 9 * u))
            drawLine(Color(0xFF7A4A1E), Offset(24 * u, 14 * u), Offset(26 * u, 7 * u), 2.2f * u, StrokeCap.Round)
            val leaf = Path().apply {
                moveTo(26 * u, 10 * u); cubicTo(29 * u, 4 * u, 36 * u, 5 * u, 37 * u, 7 * u)
                cubicTo(35 * u, 12 * u, 29 * u, 13 * u, 26 * u, 10 * u); close()
            }
            drawPath(leaf, Color(0xFF3FA66B))
        }
        2 -> { // piatto con posate
            drawCircle(Color(0x22000000), 17 * u, Offset(24 * u, 27 * u))
            drawCircle(Color.White, 17 * u, Offset(24 * u, 25 * u))
            drawCircle(Color(0xFFE6EAF2), 12 * u, Offset(24 * u, 25 * u), style = Stroke(1.2f * u))
            drawOval(Brush.linearGradient(listOf(Color(0xFFFF9B6B), Color(0xFFE8693A))), Offset(14 * u, 20 * u), Size(15 * u, 10 * u))
            drawCircle(Color(0xFF3FB871), 3.2f * u, Offset(31 * u, 22 * u))
            drawCircle(Color(0xFF6BCB77), 2.6f * u, Offset(30 * u, 29 * u))
            drawCircle(Color(0xFFFFC53D), 2.2f * u, Offset(18 * u, 32 * u))
            // forchetta e coltello
            drawLine(Color(0xFF8A94A8), Offset(4 * u, 14 * u), Offset(4 * u, 42 * u), 2.2f * u, StrokeCap.Round)
            drawLine(Color(0xFF8A94A8), Offset(1.5f * u, 8 * u), Offset(1.5f * u, 15 * u), 1.4f * u, StrokeCap.Round)
            drawLine(Color(0xFF8A94A8), Offset(6.5f * u, 8 * u), Offset(6.5f * u, 15 * u), 1.4f * u, StrokeCap.Round)
            drawRoundRect(Color(0xFF8A94A8), Offset(43 * u, 8 * u), Size(3 * u, 20 * u), CornerRadius(1.5f * u))
            drawLine(Color(0xFF8A94A8), Offset(44.5f * u, 27 * u), Offset(44.5f * u, 42 * u), 2.2f * u, StrokeCap.Round)
        }
        else -> { // luna e stelle (spuntino serale)
            val moon = Path.combine(
                PathOperation.Difference,
                Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(8 * u, 9 * u), Size(30 * u, 30 * u))) },
                Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(18 * u, 5 * u), Size(28 * u, 28 * u))) }
            )
            drawPath(moon, Brush.linearGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB63D)), start = Offset(8 * u, 9 * u), end = Offset(30 * u, 39 * u)))
            drawPath(sparkle(36 * u, 13 * u, 4.5f * u), Color(0xFFFFE9A8))
            drawPath(sparkle(40 * u, 28 * u, 3 * u), Color(0xFFFFFFFF))
            drawPath(sparkle(27 * u, 40 * u, 3.2f * u), Color(0xFFBFD0FF))
            drawCircle(Color(0xFFFFD27A), 1.6f * u, Offset(12 * u, 9 * u))
        }
    }
}

/** Selettore dei pasti: chip a larghezza naturale in riga scorrevole (nessun testo tagliato). */
@Composable
fun MealChips(selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MEAL_ORDER.forEach { m ->
            val sel = m == selected
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(if (sel) C.Navy else Color.White)
                    .border(1.dp, if (sel) C.Navy else A.Hair, RoundedCornerShape(22.dp)).clickable { onSelect(m) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) { Text(MEAL_LABEL.getValue(m), style = T.BtnS.copy(color = if (sel) Color.White else A.Ink), maxLines = 1, softWrap = false) }
        }
    }
}
