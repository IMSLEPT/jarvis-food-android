package com.jarvis.food

import com.jarvis.food.ui.TodayProgress
import org.junit.Assert.*
import org.junit.Test

class TodayProgressTest {
    @Test fun remainingUsesAdjustedDailyTargetExactlyOnce() {
        assertEquals(2056.0, TodayProgress.remaining(94.0, 2150.0), 0.001)
        assertEquals(2056f / 2150f, TodayProgress.remainingRatio(94.0, 2150.0), 0.0001f)
        assertEquals(-150.0, TodayProgress.remaining(2300.0, 2150.0), 0.001)
        assertEquals(0f, TodayProgress.remainingRatio(2300.0, 2150.0), 0.001f)
    }
    @Test fun macroPercentKeepsExcessWhileTheDialCanClamp() {
        assertEquals(3, TodayProgress.percent(3.0, 114.0))
        assertEquals(0, TodayProgress.percent(1.0, 279.0))
        assertEquals(12, TodayProgress.percent(8.0, 65.0))
        assertEquals(150, TodayProgress.percent(150.0, 100.0))
        assertEquals(0f, TodayProgress.ratio(10.0, 0.0), 0f)
        assertEquals(0f, TodayProgress.ratio(Double.NaN, 100.0), 0f)
    }
}
