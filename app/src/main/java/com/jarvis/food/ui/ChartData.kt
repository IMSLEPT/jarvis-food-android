package com.jarvis.food.ui

/** Display projections keep missing/zero values distinct from measured data. */
internal fun chartFraction(value: Double, maximum: Double): Float =
    if (!value.isFinite() || !maximum.isFinite() || maximum <= 0.0) 0f
    else (value / maximum).toFloat().coerceIn(0f, 1f)

internal data class PlotPoint(val x: Float, val y: Float)

/** Time spacing reflects actual dates; a single measurement remains a single point. */
internal fun weightPlotPositions(samples: List<Pair<Long, Double>>): List<PlotPoint> {
    val valid = samples.filter { it.second.isFinite() }.sortedBy { it.first }
    if (valid.isEmpty()) return emptyList()
    if (valid.size == 1) return listOf(PlotPoint(0.5f, 0.5f))
    val start = valid.first().first
    val span = (valid.last().first - start).coerceAtLeast(1L).toDouble()
    val low = valid.minOf { it.second } - 0.2
    val high = valid.maxOf { it.second } + 0.2
    return valid.map { (day, kg) -> PlotPoint(((day - start) / span).toFloat(), ((high - kg) / (high - low)).toFloat()) }
}

/** A maintenance target is reached only while the current weight matches it. */
internal fun weightGoalFraction(start: Double, current: Double, target: Double): Float {
    if (!start.isFinite() || !current.isFinite() || !target.isFinite()) return 0f
    return if (start == target) {
        if (kotlin.math.abs(current - target) < 0.05) 1f else 0f
    } else ((start - current) / (start - target)).toFloat().coerceIn(0f, 1f)
}
