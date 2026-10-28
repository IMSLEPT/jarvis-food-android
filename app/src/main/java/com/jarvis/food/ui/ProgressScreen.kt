package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.roundToInt

private val DM = DateTimeFormatter.ofPattern("d MMM", IT)

/** Giorni consecutivi (fino a oggi o ieri) con almeno un alimento registrato. */
fun streak(s: AppState): Int {
    val days = s.diary.map { it.date }.toSet()
    var d = LocalDate.now()
    if (d.toString() !in days) d = d.minusDays(1)
    var n = 0
    while (d.toString() in days) { n++; d = d.minusDays(1) }
    return n
}

/** Colonne del design: altezza relativa a [max], etichette sotto (se presenti), colonna scelta evidenziata. */
@Composable
private fun BarsRow(
    values: List<Double>, max: Double, height: Dp, radius: Dp, gap: Dp,
    fill: (Int, Boolean) -> Brush, labels: List<String>? = null, sel: Int = -1, onPick: ((Int) -> Unit)? = null, goal: Double? = null, goals: List<Double>? = null
) {
    Box(Modifier.fillMaxWidth().height(height)) {
        if (goals != null && max > 0) {
            Canvas(Modifier.fillMaxSize()) {
                val step = size.width / goals.size.coerceAtLeast(1)
                goals.forEachIndexed { i, target ->
                    val y = size.height * (1f - (target / max).toFloat().coerceIn(0f, 1f))
                    drawLine(C.Accent, Offset(i * step, y), Offset((i + 1) * step, y), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
            }
        } else if (goal != null && max > 0) {
            val frac = (goal / max).toFloat().coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxSize()) {
                val yy = size.height * (1f - frac)
                drawLine(Color(0xFFFF8A5C), Offset(0f, yy), Offset(size.width, yy), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, v ->
                val shape = RoundedCornerShape(radius)
                val click = if (onPick != null) Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onPick(i) } else Modifier
                Box(Modifier.weight(1f).fillMaxHeight().then(click), contentAlignment = Alignment.BottomCenter) {
                    val bar = Modifier.fillMaxWidth().fillMaxHeight(chartFraction(v, max))
                    Box((if (i == sel) bar.aShadow(shape, 8.dp, Color(0xA6E88C60)) else bar).clip(shape).background(fill(i, i == sel)))
                }
            }
        }
    }
    if (labels != null) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            labels.forEachIndexed { i, l ->
                Text(l, style = T.Chip.copy(fontSize = 12.sp, color = if (i == sel) A.Ink else A.Muted), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun ProgressScreen() {
    val s by Store.state.collectAsState()
    var period by remember { mutableIntStateOf(7) }
    var picked by remember(period) { mutableIntStateOf(period - 1) }
    val today = LocalDate.now()
    val days = (period - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val daily = days.map { Nutrition.sum(Store.entriesFor(s, it)) }
    val hasLog = days.map { Store.entriesFor(s, it).isNotEmpty() }
    val logged = daily.filterIndexed { i, _ -> hasLog[i] }
    val avg = if (logged.isEmpty()) null else logged.reduce { a, b -> a + b }.scaled(1.0 / logged.size)
    val weight = Store.currentWeight(s)
    val bmi = Nutrition.bmi(weight, s.profile.heightCm)
    val dailyGoals = days.map { Store.energyFor(s, it).targetKcal }
    val goalKcal = dailyGoals.filterIndexed { i, _ -> hasLog[i] }.ifEmpty { dailyGoals }.average()
    val expanded = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.5f
    val letter: (LocalDate) -> String = { it.dayOfWeek.getDisplayName(TextStyle.SHORT, IT).replace(".", "").let { text -> if (expanded) text.take(2) else text }.replaceFirstChar { c -> c.uppercase() } }
    val loggedDays = s.diary.map { it.date }.toSet()
    val shape30 = RoundedCornerShape(24.dp)

    MainPage("Progressi") {
        Text("I tuoi numeri, giorno dopo giorno", style = T.Body.copy(fontSize = 14.sp, color = A.Muted), modifier = Modifier.padding(top = 4.dp))

        // Intervallo
        val seg = RoundedCornerShape(26.dp)
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(seg).background(Color(0xD9FFFFFF)).border(1.dp, A.Hair, seg).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(7 to "7 giorni", 30 to "30 giorni").forEach { (d, l) ->
                val sel = period == d
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(22.dp)).background(if (sel) C.Navy else Color.Transparent).clickable { period = d }, contentAlignment = Alignment.Center) {
                    Text(l, style = T.BtnS.copy(color = if (sel) Color.White else A.Ink))
                }
            }
        }

        // Serie
        val streakDays = streak(s)
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().aShadow(shape30, 12.dp, Color(0xB3FF963C)).clip(shape30)
            .background(C.Surface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(Color.White), contentAlignment = Alignment.Center) { AIcon(Ic.Flame, C.Accent, 30.dp, fill = true) }
                GapW(14.dp)
                Column {
                    Text(if (streakDays == 1) "1 giorno di fila" else "$streakDays giorni di fila", style = T.H1.copy(fontSize = 26.sp, lineHeight = 30.sp))
                    Text(if (streakDays > 0) "hai registrato i pasti ogni giorno" else "registra un pasto per iniziare la serie", style = T.Small.copy(fontSize = 13.sp, color = A.Muted))
                }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (6 downTo 0).map { today.minusDays(it.toLong()) }.forEach { d ->
                    val isToday = d == today
                    val did = d.toString() in loggedDays
                    Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(17.dp)).background(if (isToday) C.Navy else if (did) Color.White else Color(0x80FFFFFF)), contentAlignment = Alignment.Center) {
                        Text(letter(d), style = T.Chip.copy(fontSize = 12.sp, color = if (isToday) Color.White else A.Ink))
                    }
                }
            }
        }

        // Peso
        ASection("Peso", action = "Registra", top = 28.dp) { Nav.go(Screen.Weight) }
        val from = today.minusDays((period - 1).toLong())
        val ws = s.weights.filter { !LocalDate.parse(it.date).isBefore(from) }.sortedBy { it.date }
        val wd = if (ws.size >= 2) ws.last().kg - ws.first().kg else 0.0
        Column(Modifier.fillMaxWidth().aShadow(shape30, 12.dp, Color(0x8C3B82F6)).clip(shape30).background(Color.White).border(1.dp, A.Hair, shape30)
            .clickable { Nav.go(Screen.Weight) }.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)) {
            androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(weight.r1(), style = T.H1.copy(fontSize = 46.sp, lineHeight = 48.sp))
                    GapW(10.dp)
                    Text("kg", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 5.dp))
                }
                if (ws.size >= 2) {
                    val down = wd <= 0
                    val ink = if (down) Color(0xFF065F46) else A.FatInk
                    Row(Modifier.padding(bottom = 6.dp).heightIn(min = 30.dp).clip(RoundedCornerShape(15.dp)).background(C.Surface).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AIcon(if (down) "M12 5v14M6 13l6 6 6-6" else "M12 19V5M6 11l6-6 6 6", ink, 14.dp, 2.6f)
                        GapW(4.dp)
                        Text((if (wd > 0) "+" else if (wd < 0) "−" else "") + kotlin.math.abs(wd).r1() + " kg", style = T.Chip.copy(fontWeight = FontWeight.Bold, color = ink))
                    }
                }
            }
            Text("IMC ${bmi.r1()} · ${Nutrition.bmiLabel(bmi)}", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
            WeightLine(ws.map { LocalDate.parse(it.date).toEpochDay() to it.kg })
            if (ws.isNotEmpty()) Text(if (ws.size == 1) "Una misurazione: registra un altro peso per vedere l’andamento." else "${ws.first().date} – ${ws.last().date}", style = T.Small, modifier = Modifier.testTag("weight-history-caption").padding(top = 8.dp))
            val sw = s.profile.startWeight
            val tw = s.profile.targetWeight
            val left = kotlin.math.abs(weight - tw)
            val prog = weightGoalFraction(sw, weight, tw)
            Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Surface).padding(horizontal = 14.dp, vertical = 12.dp)) {
                androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
                    Text(if (left < 0.05) "Obiettivo raggiunto" else "Obiettivo ${tw.r1().removeSuffix(",0")} kg", modifier = Modifier.testTag("weight-goal-title"), style = T.Chip.copy(fontWeight = FontWeight.SemiBold))
                    Text("${left.r1()} kg da raggiungere", modifier = Modifier.testTag("weight-goal-remaining"), style = T.Chip.copy(fontWeight = FontWeight.Bold, color = A.CarbInk))
                }
                GapH(8.dp)
                ABar(prog, Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF5AA9FF))), C.SurfaceHi, 8.dp,
                    Modifier.testTag("weight-goal-progress").semantics { progressBarRangeInfo = ProgressBarRangeInfo(prog, 0f..1f) })
            }
        }

        // Calorie
        ASection("Calorie", top = 28.dp)
        GapH(12.dp)
        val maxK = maxOf((dailyGoals.maxOrNull() ?: goalKcal) * 1.27, (daily.maxOfOrNull { it.kcal } ?: 0.0) * 1.05)
        val dName = DateTimeFormatter.ofPattern("EEE d", IT)
        Column(Modifier.fillMaxWidth().aShadow(shape30, 12.dp, Color(0x99E88C60)).clip(shape30).background(Color.White).border(1.dp, A.Hair, shape30).padding(16.dp)) {
            Text(avg?.kcal?.let { kcalFmt(it) } ?: "—", style = T.H1.copy(fontSize = 36.sp, lineHeight = 42.sp))
            Text("kcal in media · obiettivo medio ${kcalFmt(goalKcal)}", style = T.Small)
            Text("${logged.size} ${if (logged.size == 1) "giorno registrato" else "giorni registrati"} su $period · anche giornate parziali", style = T.Small, modifier = Modifier.testTag("calorie-sample-size").padding(top = 4.dp))
            val pk = picked.coerceIn(0, days.size - 1)
            val diff = (daily[pk].kcal - dailyGoals[pk]).roundToInt()
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                val detail = if (!hasLog[pk]) "Nessun pasto registrato"
                    else if (days[pk] == today) "${kcalFmt(daily[pk].kcal)} kcal · giornata in corso"
                    else "${kcalFmt(daily[pk].kcal)} kcal registrate · ${(if (diff >= 0) "+" else "−")}${kotlin.math.abs(diff)} dal target"
                Text(days[pk].format(dName).replace(".", "").replaceFirstChar { it.uppercase() } + " · " + detail,
                    style = T.Chip.copy(fontSize = 13.sp, color = C.AccentDeep), modifier = Modifier.testTag("calorie-day-detail"))
            }
            GapH(12.dp)
            BarsRow(daily.map { it.kcal }, maxK, 190.dp, if (period == 7) 14.dp else 5.dp, if (period == 7) 10.dp else 3.dp,
                fill = { i, sel ->
                    if (sel) Brush.verticalGradient(listOf(Color(0xFFF28B7A), Color(0xFFF59E6C), Color(0xFFF8C089)))
                    else SolidColor(if (i == days.size - 1) Color(0xFFFFD9C4) else if (daily[i].kcal > dailyGoals[i] * 1.1) Color(0xFFFFC2A3) else Color(0xFFF2A277))
                },
                labels = if (period == 7) days.map(letter) else null, sel = pk, onPick = { picked = it }, goal = goalKcal, goals = if (s.trainingNutrition.active) dailyGoals else null)
        }

        // Macro
        ASection("Macronutrienti (media)", top = 24.dp)
        GapH(12.dp)
        Column(Modifier.fillMaxWidth().aShadow(shape30, 12.dp, Color(0x661D1B3A)).clip(shape30).background(Color.White).border(1.dp, A.Hair, shape30).padding(16.dp)) {
            if (avg == null) Text("Registra qualche pasto per vedere le medie.", style = T.Muted)
            else {
                val kp = avg.protein * 4
                val kc = avg.carbs * 4
                val kf = avg.fat * 9
                val tot = (kp + kc + kf).coerceAtLeast(1.0)
                val pp = (kp / tot * 100).roundToInt()
                val pc = (kc / tot * 100).roundToInt()
                val pf = 100 - pp - pc
                Row(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp))) {
                    Box(Modifier.weight(pp.coerceAtLeast(1).toFloat()).fillMaxHeight().background(C.Protein))
                    Box(Modifier.weight(pc.coerceAtLeast(1).toFloat()).fillMaxHeight().background(C.Carbs))
                    Box(Modifier.weight(pf.coerceAtLeast(1).toFloat()).fillMaxHeight().background(C.Fat))
                }
                androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroShare("$pp%", "Proteine", "${avg.protein.r0()} g al giorno", A.ProteinSoft, A.ProteinInk, Modifier.fillMaxWidth(if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) 1f else 0.3f))
                    MacroShare("$pc%", "Carboidrati", "${avg.carbs.r0()} g al giorno", A.CarbSoft, A.CarbInk, Modifier.fillMaxWidth(if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) 1f else 0.3f))
                    MacroShare("$pf%", "Grassi", "${avg.fat.r0()} g al giorno", A.FatSoft, A.FatInk, Modifier.fillMaxWidth(if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) 1f else 0.3f))
                }
                Text("Fibre ${avg.fiber.r1()} g · Zuccheri ${avg.sugar.r1()} g · Sale ${avg.salt.r1()} g", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 12.dp))
            }
        }

        // Acqua
        ASection("Acqua", top = 28.dp)
        GapH(12.dp)
        val water = days.map { (s.water[it.toString()] ?: 0) / 1000.0 }
        Column(Modifier.fillMaxWidth().aShadow(shape30, 12.dp, Color(0xB306B6D4)).clip(shape30).background(C.Surface).padding(16.dp)) {
            BarsRow(water, maxOf(water.maxOrNull() ?: 0.0, s.goals.waterMl / 1000.0 * 1.1).coerceAtLeast(0.1), 110.dp, if (period == 7) 12.dp else 5.dp, if (period == 7) 8.dp else 3.dp,
                fill = { i, _ -> if (i == days.size - 1) Brush.verticalGradient(listOf(Color(0xFF9FE6F1), Color(0xFF8DB8FA))) else Brush.verticalGradient(listOf(Color(0xFF5CD6EA), Color(0xFF3B82F6))) },
                labels = if (period == 7) days.map(letter) else null, sel = days.size - 1)
            Text("Media ${water.average().r1()} L al giorno · ultimi $period giorni", style = T.Chip.copy(fontSize = 13.sp, color = Color(0xFF0E5A72)), modifier = Modifier.padding(top = 12.dp))
        }

        // Passi e attività
        ASection("Passi e attività", top = 28.dp)
        GapH(12.dp)
        val burned = days.map { Store.burnedFor(s, it).toDouble() }
        Column(Modifier.fillMaxWidth().aShadow(shape30, 12.dp, Color(0x8C10B981)).clip(shape30).background(Color.White).border(1.dp, A.Hair, shape30).padding(16.dp)) {
            BarsRow(burned, (burned.maxOrNull() ?: 1.0).coerceAtLeast(1.0) * 1.02, 110.dp, if (period == 7) 12.dp else 5.dp, if (period == 7) 8.dp else 3.dp,
                fill = { i, _ -> SolidColor(if (i == days.size - 1) Color(0xFF6FDDB8) else Color(0xFF10B981)) })
            Text("Calorie bruciate in media: ${burned.average().r0()} kcal", style = T.Chip.copy(fontSize = 13.sp, color = Color(0xFF065F46)), modifier = Modifier.padding(top = 12.dp))
        }

        // Misure corporee
        ASection("Misure corporee", action = "Apri", top = 28.dp) { Nav.go(Screen.Measurements) }
        val latest = MEASURES.mapNotNull { (k, label) ->
            val list = s.measurements.filter { it.type == k }.sortedBy { it.date }
            list.lastOrNull()?.let { Triple(label, it.value, if (list.size >= 2) it.value - list[list.size - 2].value else null) }
        }.take(4)
        if (latest.isEmpty()) {
            ACard(radius = 24.dp, padding = 16.dp, elevation = 2.dp, onClick = { Nav.go(Screen.Measurements) }) {
                Text("Vita, fianchi, petto, braccia, cosce e massa grassa: tocca per iniziare.", style = T.Muted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                latest.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, v, d) ->
                            val unit = if (label.contains("%")) "%" else "cm"
                            val name = label.substringBefore(" (")
                            ACard(Modifier.weight(1f), radius = 24.dp, padding = 0.dp, elevation = 0.dp, onClick = { Nav.go(Screen.Measurements) }) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Text(name, style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(v.r1().removeSuffix(",0"), style = T.H1.copy(fontSize = 26.sp, lineHeight = 30.sp))
                                        GapW(4.dp)
                                        Text(unit, style = T.Chip.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(bottom = 3.dp))
                                    }
                                    if (d != null) Text((if (d > 0) "+" else if (d < 0) "−" else "") + kotlin.math.abs(d).r1() + " $unit",
                                        style = T.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (d > 0) A.CarbInk else A.GreenInk))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Vitamine e minerali
        ASection("Vitamine e minerali", top = 28.dp)
        GapH(12.dp)
        MicroSummaryCard(s)
    }
}

@Composable
private fun MacroShare(pct: String, name: String, sub: String, bg: Color, ink: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(C.cardSurface(bg)).padding(12.dp)) {
        Text(pct, style = T.H1.copy(fontSize = 22.sp, lineHeight = 26.sp, color = ink))
        Text(name, style = T.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        Text(sub, style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
    }
}

/** Andamento del peso: area sfumata blu, linea 3,5 px e pallino finale (come nel design). */
@Composable
private fun WeightLine(samples: List<Pair<Long, Double>>) {
    val points = weightPlotPositions(samples)
    if (points.isEmpty()) {
        Text("Nessuna misurazione nel periodo selezionato.", style = T.Muted,
            modifier = Modifier.fillMaxWidth().testTag("weight-empty").padding(vertical = 24.dp))
        return
    }
    Canvas(Modifier.padding(top = 12.dp).fillMaxWidth().height(if (points.size == 1) 64.dp else 130.dp)
        .testTag("weight-chart").semantics { contentDescription = if (points.size == 1) "Una misurazione del peso" else "Andamento di ${points.size} misurazioni del peso" }) {
        val inset = 8.dp.toPx()
        val pts = points.map { Offset(inset + it.x * (size.width - 2 * inset), inset + it.y * (size.height - 2 * inset)) }
        val base = size.height - inset
        drawLine(C.Line, Offset(inset, base), Offset(size.width - inset, base), 1.dp.toPx())
        if (pts.size > 1) {
            val line = androidx.compose.ui.graphics.Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            val area = androidx.compose.ui.graphics.Path().apply { addPath(line); lineTo(pts.last().x, base); lineTo(pts.first().x, base); close() }
            drawPath(area, Brush.verticalGradient(listOf(Color(0x403B82F6), Color.Transparent)))
            drawPath(line, C.Blue, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        pts.forEach { drawCircle(C.Blue, 4.dp.toPx(), it) }
    }
}

val MEASURES = listOf(
    "vita" to "Vita (cm)", "fianchi" to "Fianchi (cm)", "petto" to "Petto (cm)", "braccio" to "Braccio (cm)",
    "coscia" to "Coscia (cm)", "collo" to "Collo (cm)", "grasso" to "Massa grassa (%)"
)

@Composable
fun WeightScreen() {
    val s by Store.state.collectAsState()
    var kg by remember { mutableStateOf(Store.currentWeight(s).r1()) }
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Peso", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsCard(title = "La pesata di oggi") {
                JField(kg, { kg = it }, "Peso", number = true, suffix = "kg")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton("−0,1 kg", Modifier.weight(1f)) { kg = ((parseNum(kg) ?: 0.0) - 0.1).r1() }
                    GhostButton("+0,1 kg", Modifier.weight(1f)) { kg = ((parseNum(kg) ?: 0.0) + 0.1).r1() }
                }
                PrimaryButton("Salva peso", Modifier.fillMaxWidth(), enabled = (parseNum(kg) ?: 0.0) in 30.0..300.0) { Store.addWeight(LocalDate.now(), parseNum(kg)!!) }
                if (s.goals.auto || s.trainingNutrition.automatic) Text("Il nuovo peso viene usato dal calcolo automatico degli obiettivi.", style = T.Small)
            }
            SettingsHeading("Il tuo storico", "Le pesate sono ordinate dalla più recente.", warm = true)
            if (s.weights.size > 1) SettingsCard { LineChart(s.weights.map { it.kg }, s.weights.map { LocalDate.parse(it.date).format(DM) }, C.Navy) }
            s.weights.reversed().forEach { item ->
                SettingsCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(LocalDate.parse(item.date).format(DateTimeFormatter.ofPattern("d MMMM yyyy", IT)), style = T.Small)
                            Text("${item.kg.r1()} kg", style = T.Title2)
                        }
                        if (s.weights.size > 1) GhostButton("✕", color = C.Faint) { Store.removeWeight(item.date) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// Interactive measurements are implemented in BodyMeasurementsScreen.kt.
