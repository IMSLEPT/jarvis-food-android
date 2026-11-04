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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
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
    val big = LocalDensity.current.fontScale > 1.2f
    val context = LocalContext.current
    val navy = greeting == null
    val ink = if (navy) Color.White else RD.Ink
    RefBannerFrame(navy) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val textWidth = with(density) { (maxWidth - 112.dp).coerceAtLeast(80.dp).roundToPx() }
            val caption = if (large) date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", IT)).replaceFirstChar { it.uppercase() } else fullDay(date)
            // Both tabs reserve the actual name/date height, including long names and larger fonts.
            // This keeps the week strip stationary without shrinking or clipping text.
            val nameHeight = with(density) { measurer.measure(if (large) "Oggi" else s.profile.name,
                style = T.Title.copy(fontSize = RD.BannerTitle, lineHeight = 31.sp), constraints = Constraints(maxWidth = textWidth)).size.height.toDp() }
            val captionHeight = with(density) { measurer.measure(caption,
                style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp), constraints = Constraints(maxWidth = textWidth)).size.height.toDp() }
            val greetingHeight = with(density) { if (large) 0.dp else 17.sp.toDp() }
            val sharedTitleHeight = maxOf(RD.bannerTitleHeight(), nameHeight + captionHeight + greetingHeight + 8.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = sharedTitleHeight), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).testTag(dateTag)
                .semantics { contentDescription = fullDay(date) }
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { onDateClick?.invoke() ?: showDatePicker(context, date, onPick) }
                .padding(vertical = 4.dp, horizontal = 4.dp)) {
                if (greeting != null && !large) Text(greeting, Modifier.testTag("home-greeting"),
                    style = T.Small.copy(color = RD.Grey))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (navy) { PlanCalendarLogo(28.dp); GapW(8.dp) }
                    Text(if (greeting != null && !large) s.profile.name else title,
                        style = T.Title.copy(fontSize = RD.BannerTitle, lineHeight = 31.sp, color = ink), modifier = Modifier.weight(1f))
                }
                Text(if (large) date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", IT)).replaceFirstChar { it.uppercase() } else fullDay(date),
                    style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = if (navy) Color(0xFFDCE5F5) else RD.Grey))
            }
            GapW(4.dp)
            RefCalendarButton(RD.headerButton()) { showDatePicker(context, date, onPick) }
            GapW(4.dp)
            RefProfileButton(RD.headerButton())
        }
        }
        GapH(6.dp)
        Box(Modifier.fillMaxWidth().height(RD.stripHeight()), contentAlignment = Alignment.Center) {
            ProposalNineCalendar(s, date, onPick)
        }
        GapH(2.dp)
    }
}
