package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.jarvis.food.R
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Home-only native vectors following the approved navy/orange reference. */
internal enum class TodayArt { Add, Camera, Fridge, Timer, Vitamins, Calendar, Activity }

@Composable
internal fun TodayArtwork(art: TodayArt, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 64f, size.height / 64f, pivot = Offset.Zero) {
            val navy = Brush.linearGradient(listOf(Color(0xFF425783), C.Navy), Offset(10f, 4f), Offset(50f, 58f))
            val paper = Brush.verticalGradient(listOf(Color.White, Color(0xFFDCE4F2)), 10f, 56f)
            val orange = Brush.verticalGradient(listOf(Color(0xFFFFB24F), C.Accent), 10f, 45f)
            when (art) {
                TodayArt.Add -> {
                    round(navy, 7f, 7f, 50f, 50f, 16f)
                    line(Color.White, 21f, 32f, 43f, 32f, 4.5f)
                    line(Color.White, 32f, 21f, 32f, 43f, 4.5f)
                }
                TodayArt.Camera -> {
                    val body = Path().apply {
                        moveTo(13f, 18f); lineTo(21f, 18f); lineTo(25f, 12f); lineTo(39f, 12f); lineTo(43f, 18f)
                        lineTo(51f, 18f); quadraticTo(59f, 18f, 59f, 26f); lineTo(59f, 49f)
                        quadraticTo(59f, 56f, 51f, 56f); lineTo(13f, 56f); quadraticTo(5f, 56f, 5f, 49f)
                        lineTo(5f, 26f); quadraticTo(5f, 18f, 13f, 18f); close()
                    }
                    drawPath(body, navy)
                    drawCircle(Color(0xFFF6F7FC), 13f, Offset(31f, 36f))
                    drawCircle(navy, 9f, Offset(31f, 36f))
                    drawCircle(C.Accent, 3.4f, Offset(49f, 25f))
                }
                TodayArt.Fridge -> {
                    round(navy, 12f, 4f, 40f, 56f, 9f)
                    round(paper, 16f, 8f, 32f, 20f, 5f)
                    round(paper, 16f, 32f, 32f, 24f, 5f)
                    line(C.Accent, 42f, 16f, 42f, 23f, 3.2f)
                    line(C.Accent, 42f, 38f, 42f, 46f, 3.2f)
                }
                TodayArt.Timer -> {
                    line(C.Navy, 32f, 5f, 32f, 12f, 5f)
                    round(navy, 25f, 2f, 14f, 5f, 2.5f)
                    drawCircle(navy, 25f, Offset(32f, 36f))
                    drawCircle(paper, 20f, Offset(32f, 36f))
                    drawArc(orange, -90f, 90f, true, Offset(12f, 16f), Size(40f, 40f))
                    line(C.Navy, 32f, 24f, 32f, 36f, 3.7f)
                    line(C.Navy, 32f, 36f, 41f, 42f, 3.7f)
                }
                TodayArt.Vitamins -> {
                    round(navy, 11f, 16f, 39f, 42f, 11f)
                    round(paper, 15f, 19f, 31f, 35f, 8f)
                    round(navy, 14f, 7f, 33f, 15f, 5f)
                    val leaf = Path().apply {
                        moveTo(23f, 43f); cubicTo(17f, 30f, 32f, 27f, 38f, 28f)
                        cubicTo(38f, 39f, 31f, 44f, 23f, 43f); close()
                    }
                    drawPath(leaf, orange)
                    line(C.AccentDeep, 22f, 46f, 33f, 34f, 1.8f)
                    round(paper, 33f, 49f, 25f, 12f, 6f)
                    round(orange, 45f, 49f, 13f, 12f, 6f)
                }
                TodayArt.Calendar -> {
                    round(navy, 8f, 13f, 48f, 46f, 12f)
                    round(paper, 12f, 18f, 40f, 37f, 8f)
                    round(orange, 12f, 18f, 40f, 10f, 3f)
                    line(C.Accent, 22f, 8f, 22f, 20f, 6f)
                    line(C.Accent, 42f, 8f, 42f, 20f, 6f)
                    line(C.Navy, 23f, 40f, 30f, 47f, 4.5f)
                    line(C.Navy, 30f, 47f, 43f, 33f, 4.5f)
                }
                TodayArt.Activity -> {
                    drawCircle(navy, 7f, Offset(40f, 12f))
                    line(C.Navy, 31f, 28f, 36f, 23f, 9f)
                    line(C.Navy, 36f, 23f, 45f, 32f, 8f)
                    line(C.Navy, 45f, 32f, 53f, 26f, 7f)
                    line(C.Navy, 31f, 27f, 24f, 32f, 8f)
                    line(C.Navy, 24f, 32f, 20f, 39f, 7f)
                    line(C.Navy, 34f, 28f, 28f, 41f, 10f)
                    line(C.Navy, 28f, 41f, 18f, 54f, 8f)
                    line(C.Navy, 28f, 40f, 41f, 46f, 8f)
                    line(C.Navy, 41f, 46f, 37f, 57f, 7f)
                    line(C.Accent, 8f, 24f, 17f, 24f, 3f)
                    line(C.Accent, 5f, 33f, 13f, 33f, 3f)
                    line(C.Accent, 3f, 42f, 12f, 42f, 3f)
                }
            }
        }
    }
}

@Composable
internal fun TodayIconBadge(art: TodayArt, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(Color(0xFFF0F3F9))) {
        TodayArtwork(art, Modifier.size(size))
    }
}

/** Keep the approved illustration and animate only the blue liquid behind its white drop. */
@Composable
internal fun TodayWaterBottle(fill: Float, modifier: Modifier = Modifier) {
    val bottle = ImageBitmap.imageResource(R.drawable.approved_water)
    val emptyLiquid = ImageBitmap.imageResource(R.drawable.approved_water_empty)
    Canvas(modifier) {
        val destination = IntSize(size.width.roundToInt(), size.height.roundToInt())
        if (destination.width > 0 && destination.height > 0) {
            drawImage(bottle, dstSize = destination)
            val waterline = size.height * (252f - 120f * fill.coerceIn(0f, 1f)) / bottle.height
            clipRect(bottom = waterline) {
                drawImage(emptyLiquid, dstSize = destination)
            }
        }
    }
}

private fun DrawScope.round(brush: Brush, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(brush, Offset(x, y), Size(w, h), CornerRadius(r))
private fun DrawScope.line(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, width: Float) =
    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)
