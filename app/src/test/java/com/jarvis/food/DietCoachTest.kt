package com.jarvis.food

import com.jarvis.food.data.AppState
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Food
import com.jarvis.food.engine.DietCoach
import com.jarvis.food.ui.TodayProgress
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DietCoachTest {
    private val today = LocalDate.of(2026, 11, 4)
    private val ctx = DietCoach.Context({ 2000.0 }, { 120.0 }, 2000)
    private fun food(name: String, group: String, kcal: Double = 100.0, protein: Double = 5.0) =
        Food(name.lowercase(), name, kcal = kcal, protein = protein, carbs = 10.0, fat = 3.0, group = group)
    private fun entry(day: Long, meal: Int, f: Food, g: Double = 100.0) =
        DiaryEntry("$day-$meal-${f.id}", today.minusDays(day).toString(), meal, f, g, 0L)

    @Test fun calorieProgressStartsFromZeroAndGrowsTowardsTarget() {
        assertEquals(0f, TodayProgress.ratio(0.0, 2080.0), 0f)
        assertEquals(103f / 2080f, TodayProgress.ratio(103.0, 2080.0), 0.0001f)
        assertEquals(1f, TodayProgress.ratio(2500.0, 2080.0).coerceIn(0f, 1f), 0f)
    }

    @Test fun weeklyAnalysisFindsRepeatedFoodsSweetsAndMissingVegetables() {
        val biscotti = food("Biscotti", "dolci", 450.0, 6.0)
        val pane = food("Pane bianco", "cereali", 270.0, 8.0)
        val diary = (0L..6L).flatMap { d -> listOf(entry(d, 0, biscotti, 60.0), entry(d, 1, pane, 150.0), entry(d, 2, pane, 120.0)) }
        val tips = DietCoach.analyze(AppState(diary = diary), today, 7, ctx)
        val ids = tips.map { it.id }
        assertTrue(ids.toString(), "coach-repeat" in ids)
        assertTrue(ids.toString(), "coach-sweets" in ids)
        assertTrue(ids.toString(), "coach-veg" in ids)
        assertTrue(tips.all { it.personal })
        assertTrue(tips.first { it.id == "coach-sweets" }.text.contains("7 giorni su 7"))
    }

    @Test fun emptyDiaryGivesNoPersonalAdviceAndSummaryHasNumbers() {
        assertTrue(DietCoach.analyze(AppState(), today, 30, ctx).isEmpty())
        val diary = listOf(entry(0, 0, food("Mela", "frutta", 52.0, 0.3), 150.0))
        val tips = DietCoach.analyze(AppState(diary = diary), today, 1, ctx)
        assertTrue(tips.any { it.id == "coach-veg" })
        val summary = DietCoach.summary(AppState(diary = diary), today, 1, ctx, tips)
        assertTrue(summary, summary.contains("78 kcal su 2000"))
    }
}
