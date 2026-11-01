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
    @Test fun swipePagesActuallyMoveVisibleDatesAcrossLeapDayAndNewYear() {
        for (today in listOf(LocalDate.of(2028,2,29), LocalDate.of(2026,12,31))) {
            for (page in listOf(-52,-2,-1,0,1,2,52)) {
                val days = CalendarWindow.days(today, page)
                assertEquals(today.plusWeeks(page.toLong()), days[2])
                assertEquals(7, days.distinct().size)
                for ((index,date) in days.withIndex()) {
                    assertEquals(page, CalendarWindow.pageFor(today,date))
                    assertEquals(index, CalendarWindow.position(today,date))
                }
            }
        }
    }
    @Test fun datePickerAndTodayResetFindTheCorrectPageEvenBeforeTheInitialWindow() {
        val today = LocalDate.of(2026,11,1)
        for (delta in listOf(-365,-15,-10,-3,-2,-1,0,1,4,5,6,14,365)) {
            val selected = today.plusDays(delta.toLong())
            assertEquals(selected, CalendarWindow.days(today,CalendarWindow.pageFor(today,selected))[CalendarWindow.position(today,selected)])
        }
        assertEquals(0,CalendarWindow.pageFor(today,today))
        assertEquals(2,CalendarWindow.position(today,today))
    }

}
