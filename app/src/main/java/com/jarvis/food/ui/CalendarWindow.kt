package com.jarvis.food.ui

import java.time.LocalDate

/** Selection never changes the rolling window: today's index is always two. */
object CalendarWindow {
    fun days(today: LocalDate): List<LocalDate> = (0..6).map { today.plusDays(it.toLong() - 2) }
    fun followToday(selected: LocalDate, previousToday: LocalDate, today: LocalDate): LocalDate =
        if (selected == previousToday) today else selected
}
