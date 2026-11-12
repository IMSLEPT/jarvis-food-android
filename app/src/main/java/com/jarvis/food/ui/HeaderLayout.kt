package com.jarvis.food.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
        Column(Modifier.fillMaxWidth().testTag("top-header-frame").clip(RoundedCornerShape(16.dp))
            .background(if (navy) HeaderLayout.ReferenceBlue else C.Bg).padding(horizontal = 8.dp, vertical = 8.dp), content = content)
    }
}

fun showDatePicker(context: android.content.Context, date: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
}

@Composable
fun CalendarPageHeader(s: AppState, title: String, date: LocalDate, greeting: String? = null,
    onPick: (LocalDate) -> Unit, onDateClick: (() -> Unit)? = null, dateTag: String = "calendar-date") {
    RefBannerFrame(navy = true) { CalendarHeaderContent(s, title, date, greeting, onPick, onDateClick, dateTag) }
}

@Composable
fun ColumnScope.CalendarHeaderContent(s: AppState, title: String, date: LocalDate, greeting: String? = null,
    onPick: (LocalDate) -> Unit, onDateClick: (() -> Unit)? = null, dateTag: String = "calendar-date") {
    val large = LocalDensity.current.fontScale > 1.5f
    val context = LocalContext.current
    run {
        if (greeting == null || large) BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val showSun = maxWidth >= 270.dp && !large
            val buttonSize = RD.headerButton()
            val textWidth = with(density) { (maxWidth - buttonSize * 2 - 16.dp - (if (showSun) 54.dp else 0.dp)).coerceAtLeast(48.dp).roundToPx() }
            val caption = if (large) date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", IT)).replaceFirstChar { it.uppercase() } else fullDay(date)
            // Both tabs reserve the actual name/date height, including long names and larger fonts.
            // This keeps the week strip stationary without shrinking or clipping text.
            val nameHeight = with(density) { measurer.measure(s.profile.name,
                style = T.Title.copy(fontSize = RD.BannerTitle, lineHeight = 31.sp), constraints = Constraints(maxWidth = textWidth)).size.height.toDp() }
            val captionHeight = with(density) { measurer.measure(caption,
                style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp), constraints = Constraints(maxWidth = textWidth)).size.height.toDp() }
            val greetingHeight = with(density) {
                measurer.measure("Buon pomeriggio,", style = T.Label, constraints = Constraints(maxWidth = textWidth)).size.height.toDp()
            }
            val sharedTitleHeight = maxOf(RD.bannerTitleHeight(), nameHeight + captionHeight + greetingHeight + 8.dp)
            Row(Modifier.fillMaxWidth().heightIn(min = sharedTitleHeight), verticalAlignment = Alignment.CenterVertically) {
                if (greeting != null && showSun) {
                    WelcomeSun(Modifier.size(38.dp))
                    GapW(7.dp)
                    Box(Modifier.width(1.dp).height(45.dp).background(Color(0xFFE7ECF4)))
                    GapW(8.dp)
                }
                Column(Modifier.weight(1f).testTag(dateTag)
                    .semantics { contentDescription = fullDay(date) }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onDateClick?.invoke() ?: showDatePicker(context, date, onPick) }
                    .padding(vertical = 4.dp, horizontal = 4.dp)) {
                    if (greeting != null) Text(greeting, Modifier.testTag("home-greeting"),
                        style = T.Label.copy(color = Color.White))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (greeting == null) { PlanCalendarLogo(28.dp); GapW(8.dp) }
                        Text(if (greeting != null) s.profile.name else title,
                            style = T.Title.copy(fontSize = RD.BannerTitle, lineHeight = 31.sp, color = Color.White),
                            modifier = Modifier.weight(1f).then(if (greeting != null) Modifier.testTag("home-name") else Modifier))
                    }
                    Text(if (large) date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", IT)).replaceFirstChar { it.uppercase() } else fullDay(date),
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFDCE5F5)))
                }
                GapW(4.dp)
                RefCalendarButton(RD.headerButton()) { showDatePicker(context, date, onPick) }
                GapW(4.dp)
                RefProfileButton(RD.headerButton())
            }
        }
        else Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.jarvis.food.R.drawable.jarvis_logo), null, Modifier.size(30.dp))
                GapW(6.dp)
                Text("Jarvis Food", style = T.Title.copy(fontSize = 18.sp, lineHeight = 22.sp, color = Color.White))
                Spacer(Modifier.weight(1f))
                RefCalendarButton(44.dp) { showDatePicker(context, date, onPick) }
                GapW(8.dp)
                RefProfileButton(44.dp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).testTag(dateTag).semantics { contentDescription = fullDay(date) }
                    .clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { onDateClick?.invoke() ?: showDatePicker(context, date, onPick) }
                    .padding(vertical = 6.dp)) {
                    Text(greeting!!, Modifier.testTag("home-greeting"), style = T.Label.copy(fontSize = 15.sp, color = Color(0xFFB9C8EC)))
                    Text(s.profile.name, Modifier.testTag("home-name"), style = T.Title.copy(fontSize = 30.sp, lineHeight = 35.sp, color = Color.White), maxLines = 2)
                    Text(fullDay(date), style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFB9C8EC)))
                }
                MascotOggi(132.dp, Modifier.offset(x = 10.dp, y = 4.dp))
            }
        }
        GapH(6.dp)
        Box(Modifier.fillMaxWidth().height(RD.stripHeight()), contentAlignment = Alignment.Center) {
            ProposalNineCalendar(s, date, onPick)
        }
        GapH(2.dp)
    }
}

@Composable
private fun WelcomeSun(modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.width / 40f
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        drawCircle(DashboardOrange, 9f * unit, point(20f, 23f))
        for ((start, end) in listOf(
            point(20f, 5f) to point(20f, 10f),
            point(7f, 9f) to point(11f, 13f),
            point(33f, 9f) to point(29f, 13f),
            point(2f, 23f) to point(7f, 23f),
            point(33f, 23f) to point(38f, 23f),
            point(8f, 34f) to point(11f, 31f),
            point(32f, 34f) to point(29f, 31f)
        )) drawLine(DashboardOrange, start, end, 3f * unit, StrokeCap.Round)
        drawPath(Path().apply {
            moveTo(unit, 36f * unit)
            quadraticTo(20f * unit, 30f * unit, 39f * unit, 36f * unit)
        }, Color.White, style = Stroke(2.4f * unit, cap = StrokeCap.Round))
    }
}
