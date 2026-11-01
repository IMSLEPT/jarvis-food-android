package com.jarvis.food.ui

import com.jarvis.food.data.*
import java.time.LocalDate

data class RecordedCalories(val date: LocalDate, val totals: Totals, val entries: Int)
object HistoryData {
    fun weights(state: AppState) = state.weights.sortedByDescending { it.date }
    /** Only recorded dates; missing days are never converted to zero intake. */
    fun calories(state: AppState): List<RecordedCalories> = state.diary.groupBy { LocalDate.parse(it.date) }
        .map { (date, entries) -> RecordedCalories(date, Nutrition.sum(entries), entries.size) }.sortedByDescending { it.date }
    fun inPeriod(days: List<RecordedCalories>, today: LocalDate, period: Int?): List<RecordedCalories> =
        if (period == null) days else days.filter { it.date in today.minusDays(period.toLong() - 1)..today }
}
