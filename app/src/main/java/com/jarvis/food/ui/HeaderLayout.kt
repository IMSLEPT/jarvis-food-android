package com.jarvis.food.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import java.time.LocalDate

/** Identical outer geometry for calendar and navy headers at each text scale. */
object HeaderLayout {
    val ReferenceBlue = Color(0xFF0F1F52)
    @Composable fun titleHeight() = when { LocalDensity.current.fontScale > 1.5f -> 120.dp; LocalDensity.current.fontScale > 1.2f -> 76.dp; else -> 56.dp }
    @Composable fun tabsHeight() = when { LocalDensity.current.fontScale > 1.5f -> 144.dp; LocalDensity.current.fontScale > 1.2f -> 76.dp; else -> 48.dp }
    @Composable fun searchHeight() = when { LocalDensity.current.fontScale > 1.5f -> 72.dp; LocalDensity.current.fontScale > 1.2f -> 56.dp; else -> 48.dp }
    @Composable fun searchGap() = if (LocalDensity.current.fontScale > 1.2f && LocalDensity.current.fontScale <= 1.5f) 12.dp else 8.dp
    @Composable fun dateHeight() = if (LocalDensity.current.fontScale > 1.2f) 56.dp else 40.dp
    @Composable fun weekHeight() = when { LocalDensity.current.fontScale > 1.5f -> 168.dp; LocalDensity.current.fontScale > 1.2f -> 88.dp; else -> 64.dp }
}

@Composable
fun ScreenHeaderFrame(navy: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().background(C.Bg).padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
        Column(Modifier.fillMaxWidth().testTag("top-header-frame").clip(RoundedCornerShape(24.dp))
            .background(if (navy) HeaderLayout.ReferenceBlue else C.Bg).padding(horizontal = 8.dp, vertical = 8.dp), content = content)
    }
}

fun showDatePicker(context: android.content.Context, date: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
}

@Composable
fun CalendarPageHeader(s: AppState, title: String, date: LocalDate, greeting: String? = null,
    onPick: (LocalDate) -> Unit, onDateClick: (() -> Unit)? = null, dateTag: String = "calendar-date") {
    val large = LocalDensity.current.fontScale > 1.5f
    val context = LocalContext.current
    ScreenHeaderFrame(false) {
        Row(Modifier.fillMaxWidth().height(HeaderLayout.titleHeight()), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                if (greeting != null && !large) Text(greeting, Modifier.testTag("home-greeting"), style = T.Body.copy(fontSize = 14.sp, lineHeight = 18.sp))
                Text(if (greeting != null && !large) s.profile.name else title, style = T.Title.copy(fontSize = 28.sp, lineHeight = 34.sp),
                    maxLines = if (large) 2 else 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            HeaderAccountActions { showDatePicker(context, date, onPick) }
        }
        Row(Modifier.fillMaxWidth().height(HeaderLayout.dateHeight()), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight().testTag(dateTag).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { onDateClick?.invoke() ?: showDatePicker(context, date, onPick) }, contentAlignment = Alignment.CenterStart) {
                Text(fullDay(date), style = T.Title2.copy(fontSize = 16.sp, lineHeight = 22.sp))
            }
        }
        Box(Modifier.fillMaxWidth().height(HeaderLayout.weekHeight()), contentAlignment = Alignment.Center) { WeekStrip(s, date, onPick) }
    }
}
