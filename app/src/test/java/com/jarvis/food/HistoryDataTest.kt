package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.ui.HistoryData
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HistoryDataTest {
    private val food = Food("history-food","Alimento test",200.0,10.0,30.0,4.0)
    private fun entry(id: String,date: String,grams: Double) = DiaryEntry(id,date,1,food,grams,0L)
    @Test fun dailyTotalsUseRecordedGramsAndDoNotInventMissingDays() {
        val state=AppState(diary=listOf(entry("a","2026-10-30",150.0),entry("b","2026-10-30",50.0),entry("c","2026-11-01",75.0)))
        val rows=HistoryData.calories(state)
        assertEquals(listOf("2026-11-01","2026-10-30"),rows.map { it.date.toString() })
        assertEquals(150.0,rows[0].totals.kcal,0.0)
        assertEquals(400.0,rows[1].totals.kcal,0.0)
        assertEquals(20.0,rows[1].totals.protein,0.0)
        assertEquals(2,rows[1].entries)
        assertTrue(HistoryData.calories(AppState()).isEmpty())
    }
    @Test fun periodIncludesBoundariesAndAllHistoryWithoutTreatingFutureAsToday() {
        val today=LocalDate.of(2026,11,1)
        val rows=HistoryData.calories(AppState(diary=listOf(entry("a","2026-10-26",100.0),entry("b","2026-10-25",100.0),entry("c","2026-11-01",100.0),entry("d","2026-11-02",100.0))))
        assertEquals(listOf("2026-11-01","2026-10-26"),HistoryData.inPeriod(rows,today,7).map { it.date.toString() })
        assertEquals(rows,HistoryData.inPeriod(rows,today,null))
    }
    @Test fun weightsAreChronologicalAndReadingHistoryDoesNotRewriteEntries() {
        val original=listOf(WeightEntry("2026-10-02",72.0),WeightEntry("2026-11-01",71.0),WeightEntry("2026-09-20",73.0))
        val state=AppState(weights=original)
        assertEquals(listOf(71.0,72.0,73.0),HistoryData.weights(state).map { it.kg })
        assertEquals(original,state.weights)
        assertTrue(HistoryData.weights(AppState()).isEmpty())
    }
}
