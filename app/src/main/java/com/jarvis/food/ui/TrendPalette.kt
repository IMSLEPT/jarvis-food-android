package com.jarvis.food.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** The same semantic colours in overview, details and selected days. */
object TrendPalette {
    val WaterTop = Color(0xFF29D0E7)
    val WaterBottom = Color(0xFF1688F9)
    val Activity = Color(0xFF10B981)
    fun ink(kind: String, metric: Int = 0): Color = when (kind) {
        "water" -> WaterBottom
        "activity" -> Activity
        "macro" -> listOf(C.Protein, C.Carbs, C.Fat)[metric]
        else -> C.Carbs
    }
    fun bars(kind: String, metric: Int = 0): Brush = if (kind == "water")
        Brush.verticalGradient(listOf(WaterTop, WaterBottom)) else SolidColor(ink(kind, metric))
}
