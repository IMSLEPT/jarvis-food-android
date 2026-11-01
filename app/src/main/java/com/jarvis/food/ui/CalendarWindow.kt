package com.jarvis.food.ui

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Today is third in the initial window; browsing moves contiguous seven-day pages. */
object CalendarWindow {
    fun days(today: LocalDate, page: Int = 0): List<LocalDate> = (0..6).map { today.plusDays(page.toLong() * 7 + it - 2) }
    fun pageFor(today: LocalDate, selected: LocalDate): Int = Math.floorDiv(ChronoUnit.DAYS.between(today.minusDays(2), selected), 7L).toInt()
    fun position(today: LocalDate, selected: LocalDate): Int = Math.floorMod(ChronoUnit.DAYS.between(today.minusDays(2), selected), 7L).toInt()
    fun followToday(selected: LocalDate, previousToday: LocalDate, today: LocalDate): LocalDate =
        if (selected == previousToday) today else selected
}
