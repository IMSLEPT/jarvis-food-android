package com.jarvis.food.ui

internal fun fastingElapsed(now: Long, start: Long?): Long = if (start == null) 0L else (now - start).coerceAtLeast(0L)
internal fun fastingClock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60)
}
