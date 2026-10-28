package com.jarvis.food

import com.jarvis.food.ui.chartFraction
import com.jarvis.food.ui.weightPlotPositions
import org.junit.Assert.*
import org.junit.Test

class ChartDataTest {
    @Test fun zeroAndInvalidSamplesDoNotCreatePhantomBars() {
        assertEquals(0f, chartFraction(0.0, 2000.0), 0f)
        assertEquals(0f, chartFraction(100.0, 0.0), 0f)
        assertEquals(0f, chartFraction(Double.NaN, 2000.0), 0f)
        assertEquals(0f, chartFraction(100.0, Double.POSITIVE_INFINITY), 0f)
        assertEquals(0.005f, chartFraction(10.0, 2000.0), 0.000001f)
        assertEquals(1f, chartFraction(3000.0, 2000.0), 0f)
    }
    @Test fun noHistoryAndOneMeasurementCannotInventATrend() {
        assertTrue(weightPlotPositions(emptyList()).isEmpty())
        val points = weightPlotPositions(listOf(20L to 71.0))
        assertEquals(1, points.size)
        assertEquals(0.5f, points.single().x, 0f)
    }
    @Test fun irregularDatesUseTimeRatherThanEntryIndex() {
        val points = weightPlotPositions(listOf(10L to 72.0, 1L to 70.0, 2L to 71.0))
        assertEquals(0f, points[0].x, 0f)
        assertEquals(1f / 9f, points[1].x, 0.000001f)
        assertEquals(1f, points[2].x, 0f)
        assertTrue(points.first().y > points.last().y)
    }
    @Test fun flatOrRepeatedMeasurementsRemainFinite() {
        val points = weightPlotPositions(listOf(2L to 70.0, 2L to 70.0, 3L to 70.0))
        assertTrue(points.all { it.x.isFinite() && it.y.isFinite() })
        assertEquals(points.first().y, points.last().y, 0f)
        assertTrue(weightPlotPositions(listOf(1L to Double.NaN)).isEmpty())
    }
}
