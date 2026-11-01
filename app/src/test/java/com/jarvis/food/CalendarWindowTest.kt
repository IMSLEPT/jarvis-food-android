package com.jarvis.food

import com.jarvis.food.ui.CalendarWindow
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalendarWindowTest {
    @Test fun todayIsThirdAcrossMonthYearAndLeapDayBoundaries() {
        for (value in listOf("2026-11-01", "2027-01-01", "2028-02-29", "2028-03-01", "2026-12-31")) {
            val today = LocalDate.parse(value)
            val days = CalendarWindow.days(today)
            assertEquals(7, days.distinct().size)
            assertEquals(today, days[2])
            assertEquals(today.minusDays(2), days.first())
            assertEquals(today.plusDays(4), days.last())
            days.zipWithNext().forEach { (a,b) -> assertEquals(a.plusDays(1), b) }
        }
    }
    @Test fun rolloverFollowsTodayAndPreservesHistoricalOrFutureSelections() {
        val previous = LocalDate.of(2026,12,31)
        val today = previous.plusDays(1)
        assertEquals(today, CalendarWindow.followToday(previous, previous, today))
        for (selection in listOf(previous.minusDays(1), previous.minusYears(1), previous.plusDays(3))) {
            assertEquals(selection, CalendarWindow.followToday(selection, previous, today))
        }
    }
    @Test fun clockOrTimezoneChangeCanMoveTodayBackwardsWithoutLosingHistory() {
        val previous = LocalDate.of(2027,1,1)
        val today = previous.minusDays(1)
        assertEquals(today, CalendarWindow.followToday(previous, previous, today))
        assertEquals(previous.minusWeeks(1), CalendarWindow.followToday(previous.minusWeeks(1), previous, today))
    }
}
