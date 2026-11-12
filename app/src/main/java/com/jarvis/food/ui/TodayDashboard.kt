package com.jarvis.food.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import kotlin.math.roundToInt

internal val DashboardOrange = Color(0xFFFF8000)

/** Display calculations only; target already includes any extra allowed by the energy engine. */
internal object TodayProgress {
    fun ratio(value: Double, goal: Double): Float =
        if (value.isFinite() && goal.isFinite() && goal > 0) (value / goal).coerceAtLeast(0.0).toFloat() else 0f
    fun remaining(eaten: Double, target: Double) = target - eaten
    fun remainingRatio(eaten: Double, target: Double) = ratio(remaining(eaten, target), target)
    fun percent(value: Double, goal: Double) = (ratio(value, goal) * 100).roundToInt()
}

internal object DashboardIcon {
    const val Muscle = "M4 19l1-7 4-8h4l2 2-2 3-3-1-1 5c3-3 9-2 10 2 1 4-3 6-7 6z"
    const val Wheat = "M10 21l4-17M13 10C8 10 7 6 8 4c4 0 6 3 5 6zM12 16c-5 0-6-4-5-6 4 0 6 3 5 6zM14 8c0-4 3-5 6-5 0 4-2 6-6 5zM13 14c0-4 3-5 6-5 0 4-2 6-6 5z"
    const val Fork = "M4 3v6c0 3 5 3 5 0V3M6.5 3v18M16 3c-3 3-3 8 0 9h2M18 3v18"
    const val Target = "M20 12a8 8 0 1 1-8-8M16 12a4 4 0 1 1-4-4M12 12l8-9M17 3h3v3"
    const val Runner = "M13 5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 1 0 0 3M9 9l4-2 3 4 4-1M13 7l-3 7 5 2-2 6M10 14l-4 6H2M9 9l-3 4H3"
    val Robot = Ic.rr(3.0, 8.0, 18.0, 13.0, 5.0) + "M12 8V4M1 13v4M23 13v4" + Ic.circ(8.0, 14.0, 0.8) + Ic.circ(16.0, 14.0, 0.8)
}

@Composable
internal fun DashboardRobot(selected: Boolean, idle: Color = C.Navy) {
    Box(Modifier.size(22.dp)) {
        AIcon(DashboardIcon.Robot, if (selected) Color.White else idle, 22.dp)
        Box(Modifier.align(Alignment.TopCenter).size(4.dp).clip(CircleShape).background(if (selected) Color.White else DashboardOrange))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ProposalNineCalendar(s: AppState, date: LocalDate, onPick: (LocalDate) -> Unit) {
    key(Nav.today) {
        val today = Nav.today
        val origin = 10000
        val pager = rememberPagerState(initialPage = (origin + CalendarWindow.pageFor(today, date)).coerceIn(0, 20000), pageCount = { 20001 })
        val currentDate by rememberUpdatedState(date)
        val pick by rememberUpdatedState(onPick)
        val scope = rememberCoroutineScope()
        val expanded = LocalDensity.current.fontScale > 1.5f
        LaunchedEffect(date, today) {
            val desired = (origin + CalendarWindow.pageFor(today, date)).coerceIn(0, 20000)
            if (pager.currentPage != desired) pager.scrollToPage(desired)
        }
        LaunchedEffect(pager, today) {
            snapshotFlow { pager.settledPage }.collect { page ->
                if (CalendarWindow.pageFor(today, currentDate) != page - origin)
                    pick(CalendarWindow.days(today, page - origin)[CalendarWindow.position(today, currentDate)])
            }
        }
        val shape = RoundedCornerShape(26.dp)
        Row(Modifier.fillMaxSize().testTag("week-strip").pointerInput(pager, today) {
            var distance = 0f
            detectHorizontalDragGestures(onDragStart = { distance = 0f }, onDragEnd = {
                if (kotlin.math.abs(distance) > 40.dp.toPx()) scope.launch {
                    pager.animateScrollToPage((pager.currentPage + if (distance < 0) 1 else -1).coerceIn(0, 20000))
                }
            }) { change, delta -> change.consume(); distance += delta }
        }.aShadow(shape, 7.dp, Color(0x24134B83)).clip(shape)
            .background(Color.White).border(1.dp, Color(0xFFE9EEF5), shape).padding(horizontal = 2.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            CalendarArrow(false, pager.currentPage > 0) { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) { page ->
                val days = CalendarWindow.days(today, page - origin)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.indices.toList().chunked(if (expanded) 4 else 7).forEach { group ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            group.forEachIndexed { n, i ->
                                val day = days[i]
                                val selected = day == date
                                val active = page == pager.currentPage
                                val logged = Store.entriesFor(s, day).isNotEmpty()
                                val tile = RoundedCornerShape(14.dp)
                                Column(Modifier.weight(1f).then(if (active) Modifier.testTag("week-day-$i") else Modifier)
                                    .clip(tile).background(if (selected) Brush.verticalGradient(listOf(Color(0xFFFFA25C), Color(0xFFF26A1B))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                                    
                                    .semantics { contentDescription = day.toString() }
                                    .selectableCalendar(selected) { pick(day) }.padding(top = 4.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, IT).take(if (LocalDensity.current.fontScale > 1.2f) 2 else 3).replaceFirstChar { it.uppercase() },
                                        modifier = if (active) Modifier.testTag("week-label-$i") else Modifier,
                                        style = T.Small.copy(fontSize = 12.sp, lineHeight = 16.sp, color = if (selected) Color.White else C.Navy, fontWeight = FontWeight.SemiBold), maxLines = 1, softWrap = false)
                                    Text(day.dayOfMonth.toString(), style = T.Num.copy(fontSize = 21.sp, lineHeight = 26.sp, color = if (selected) Color.White else C.Navy))
                                    GapH(3.dp)
                                    Box(Modifier.width(if (selected) 16.dp else 4.dp).height(3.dp).clip(CircleShape).background(if (selected) Color.White else if (logged) DashboardOrange else Color(0xFFC3C8DA)))
                                }
                                if (n < group.lastIndex) Box(Modifier.width(1.dp).height(39.dp).background(if (selected || days[group[n + 1]] == date) Color.Transparent else Color(0xFFDDE2EA)))
                            }
                        }
                    }
                }
            }
            CalendarArrow(true, pager.currentPage < 20000) { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(20000)) } }
        }
    }
}

private fun Modifier.selectableCalendar(selected: Boolean, onClick: () -> Unit) =
    this.semantics { this.selected = selected }.clickable(role = Role.Button, onClick = onClick)

@Composable
private fun CalendarArrow(next: Boolean, enabled: Boolean, action: () -> Unit) {
    Box(Modifier.width(40.dp).fillMaxHeight().testTag(if (next) "week-next" else "week-previous")
        .semantics { contentDescription = if (next) "Settimana successiva" else "Settimana precedente" }
        .clickable(enabled = enabled, role = Role.Button, onClick = action), contentAlignment = Alignment.Center) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(RD.Navy), contentAlignment = Alignment.Center) {
            AIcon(if (next) RIc.Right else RIc.Left, Color.White, 20.dp, 2.6f)
        }
    }
}

@Composable
internal fun TodayHero(s: AppState, date: LocalDate, hello: String, eaten: Double, target: Double, burned: Int, onPick: (LocalDate) -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
    Column(Modifier.fillMaxWidth().testTag("today-hero").aShadow(shape, 14.dp).clip(shape).background(RD.NavyBrush)
        .statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 18.dp)) {
        CalendarHeaderContent(s, "Oggi", date, "$hello,", onPick)
        GapH(14.dp)
        Column(Modifier.fillMaxWidth().testTag("today-calories").clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpen).padding(horizontal = 4.dp)) {
            CalorieContent(eaten, target, burned)
        }
    }
}

@Composable
internal fun TodayCalorieCard(eaten: Double, target: Double, burned: Int, onClick: () -> Unit) {
    ACard(Modifier.fillMaxWidth().testTag("today-calories"), radius = 28.dp, brush = RD.NavyBrush, padding = 16.dp, bordered = false, elevation = 16.dp, onClick = onClick) {
        CalorieContent(eaten, target, burned)
    }
}

@Composable
private fun CalorieContent(eaten: Double, target: Double, burned: Int) {
    val remaining = TodayProgress.remaining(eaten, target)
    // Video 4 novembre: la percentuale e la barra partono da 0 e crescono verso l'obiettivo.
    val fraction = TodayProgress.ratio(eaten, target).coerceIn(0f, 1f)
    val progress by animateFloatAsState(fraction, tween(650), label = "calorie-progress")
      Column {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val roomy = maxWidth / LocalDensity.current.fontScale >= 220.dp
            val info: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AIcon(Ic.Flame, DashboardOrange, 24.dp, fill = true); GapW(5.dp)
                        Text("Le tue calorie", style = T.Title2.copy(color = Color.White, fontSize = 16.sp, lineHeight = 21.sp))
                    }
                    GapH(4.dp)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val number: @Composable () -> Unit = {
                            Text(kcalFmt(kotlin.math.abs(remaining)), Modifier.testTag("calories-remaining"), style = T.Big.copy(fontSize = 44.sp, lineHeight = 48.sp, color = if (remaining < 0) Color(0xFFFFC1B5) else Color.White))
                        }
                        val unit: @Composable () -> Unit = {
                            Text(if (remaining < 0) "kcal oltre l’obiettivo" else "kcal disponibili", style = T.Small.copy(color = Color(0xFFE3EAF5), fontSize = 12.sp, lineHeight = 17.sp))
                        }
                        if (maxWidth / LocalDensity.current.fontScale >= 185.dp && remaining >= 0) Row(verticalAlignment = Alignment.CenterVertically) {
                            number(); GapW(6.dp); Box(Modifier.weight(1f).padding(top = 10.dp)) { unit() }
                        } else Column { number(); unit() }
                    }
                    GapH(8.dp)
                    Box(Modifier.testTag("calorie-bar").semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }) {
                        ABar(progress, Brush.horizontalGradient(listOf(DashboardOrange, Color(0xFFFFCB74))), Color(0x33FFFFFF), 10.dp)
                    }
                }
            }
            if (roomy) Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { info() }; GapW(8.dp)
                CalorieGauge(progress, target > 0, fraction)
            } else Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { CalorieGauge(progress, target > 0, fraction) }
                info()
            }
            AIcon(Ic.Right, Color.White, 17.dp, modifier = Modifier.align(Alignment.TopEnd))
        }
        GapH(12.dp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth / LocalDensity.current.fontScale < 180.dp
            val stats: @Composable RowScope.() -> Unit = {
                CalorieStat("Mangiate", eaten, DashboardIcon.Fork, Color.White, Modifier.weight(1f))
                DashboardDivider(36.dp, Color(0x446B85AB))
                CalorieStat("Obiettivo", target, DashboardIcon.Target, Color.White, Modifier.weight(1f))
                DashboardDivider(36.dp, Color(0x446B85AB))
                CalorieStat("Attività", burned.toDouble(), DashboardIcon.Runner, Color(0xFFABF0C5), Modifier.weight(1f))
            }
            if (narrow) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalorieStat("Mangiate", eaten, DashboardIcon.Fork, Color.White, Modifier.fillMaxWidth())
                CalorieStat("Obiettivo", target, DashboardIcon.Target, Color.White, Modifier.fillMaxWidth())
                CalorieStat("Attività", burned.toDouble(), DashboardIcon.Runner, Color(0xFFABF0C5), Modifier.fillMaxWidth())
            } else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x26FFFFFF)).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, content = stats)
        }
      }
}

@Composable
internal fun CalorieGauge(progress: Float, valid: Boolean, fraction: Float) {
    Box(Modifier.size(if (LocalDensity.current.fontScale > 1.2f) 108.dp else 104.dp).testTag("calorie-gauge").semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 10.dp.toPx(); val inset = stroke / 2
            val sz = Size(size.width - stroke, size.height - stroke)
            drawArc(Color(0x30FFFFFF), 135f, 270f, false, Offset(inset, inset), sz, style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress > 0) drawArc(Brush.sweepGradient(listOf(Color(0xFFFFB845), DashboardOrange, Color(0xFFFFB845))), 135f, 270f * progress, false, Offset(inset, inset), sz, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AIcon(Ic.Flame, DashboardOrange, 16.dp, fill = true)
            Text(if (valid) "${(fraction * 100).roundToInt()}%" else "—", style = T.Num.copy(color = Color.White, fontSize = 22.sp, lineHeight = 25.sp))
            Text("al target", style = T.Small.copy(color = Color.White, fontSize = 10.sp, lineHeight = 13.sp))
        }
    }
}

@Composable
private fun CalorieStat(label: String, value: Double, icon: String, tint: Color, modifier: Modifier) {
    val enlarged = LocalDensity.current.fontScale > 1.2f
    val text: @Composable () -> Unit = {
        Column(horizontalAlignment = if (enlarged) Alignment.CenterHorizontally else Alignment.Start) {
            Text(label, style = T.Small.copy(color = Color(0xFFE3EAF5), fontSize = 12.sp, lineHeight = 17.sp))
            Text("${kcalFmt(value)} kcal", Modifier.testTag("calories-$label"), style = T.Num.copy(color = tint, fontSize = 13.sp, lineHeight = 18.sp))
        }
    }
    if (enlarged) Column(modifier.padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AIcon(icon, Color.White, 21.dp); GapH(3.dp); text()
    } else Row(modifier.padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        AIcon(icon, Color.White, 21.dp); GapW(5.dp)
        Box(Modifier.weight(1f)) { text() }
    }
}

@Composable
private fun DashboardDivider(height: androidx.compose.ui.unit.Dp = 54.dp, color: Color = Color(0xFFDCE2EC)) {
    Box(Modifier.width(1.dp).height(height).background(color))
}

@Composable
internal fun TodayMacroCard(protein: Double, proteinGoal: Double, carbs: Double, carbsGoal: Double, fat: Double, fatGoal: Double) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("today-macros")) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroDial("Proteine", protein, proteinGoal, C.Protein, A.ProteinSoft, A.ProteinInk, DashboardIcon.Muscle, 44.dp, Modifier.fillMaxWidth())
            MacroDial("Carboidrati", carbs, carbsGoal, C.Carbs, A.CarbSoft, A.CarbInk, DashboardIcon.Wheat, 44.dp, Modifier.fillMaxWidth())
            MacroDial("Grassi", fat, fatGoal, C.Fat, A.FatSoft, A.FatInk, Ic.Drop, 44.dp, Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroDial("Proteine", protein, proteinGoal, C.Protein, A.ProteinSoft, A.ProteinInk, DashboardIcon.Muscle, 52.dp, Modifier.weight(1f))
            MacroDial("Carboidrati", carbs, carbsGoal, C.Carbs, A.CarbSoft, A.CarbInk, DashboardIcon.Wheat, 52.dp, Modifier.weight(1f))
            MacroDial("Grassi", fat, fatGoal, C.Fat, A.FatSoft, A.FatInk, Ic.Drop, 52.dp, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroDial(label: String, value: Double, goal: Double, color: Color, soft: Color, ink: Color, icon: String,
    ringSize: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    val fraction = TodayProgress.ratio(value, goal).coerceIn(0f, 1f)
    val progress by animateFloatAsState(fraction, tween(600), label = "macro-$label")
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.aShadow(shape, 8.dp).clip(shape).background(Brush.verticalGradient(listOf(soft, Color.White)))
        .clickable { Nav.go(Screen.DayNutrients(Nav.diaryDate)) }.padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(ringSize).testTag("macro-ring-$label").semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(1.dp)) {
                val stroke = 5.dp.toPx()
                val inset = stroke / 2
                val bounds = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White, -90f, 360f, false, Offset(inset, inset), bounds, style = Stroke(stroke))
                if (progress > 0) drawArc(color, -90f, progress * 360f, false, Offset(inset, inset), bounds,
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            AIcon(icon, color, ringSize * .46f, fill = true)
        }
        GapH(6.dp)
        Text(label, style = T.Label.copy(fontSize = 12.5.sp, lineHeight = 16.sp, color = ink), maxLines = 1)
        Text("${value.r0()} / ${goal.r0()} g", Modifier.testTag("macro-value-$label"),
            style = T.Num.copy(fontSize = 14.sp, lineHeight = 18.sp), maxLines = 1)
        GapH(4.dp)
        Text(if (goal > 0) "${TodayProgress.percent(value, goal)}%" else "—",
            Modifier.testTag("macro-percent-$label").clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 2.dp),
            style = T.Label.copy(fontSize = 12.sp, lineHeight = 15.sp, color = Color.White))
    }
}
