package com.jarvis.food.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.jarvis.food.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.MealSchedule
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.engine.MicroAnalysis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

val IT: Locale = Locale.ITALIAN

fun dayLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Oggi"
        today.minusDays(1) -> "Ieri"
        today.plusDays(1) -> "Domani"
        else -> fullDay(d)
    }
}

/** "Giovedì 22 ottobre". */
fun fullDay(d: LocalDate): String =
    "${d.dayOfWeek.getDisplayName(TextStyle.FULL, IT).replaceFirstChar { it.uppercase() }} ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, IT)}"

fun defaultMeal(): Int {
    val candidate = mealForHour(LocalTime.now().hour)
    val selected = MealSchedule.selected(Store.current.dietPrefs)
    return if (candidate in selected) candidate else selected.firstOrNull { MEAL_ORDER.indexOf(it) >= MEAL_ORDER.indexOf(candidate) } ?: selected.firstOrNull() ?: 0
}

val MEAL_SOFT = mapOf(0 to A.FatSoft, 1 to A.GreenSoft, 3 to A.ProteinSoft, 2 to A.CarbSoft, 4 to A.FatSoft, 5 to A.FastSoft)
val MEAL_DOT = mapOf(0 to Color(0xFFFFB820), 1 to Color(0xFF10B981), 3 to Color(0xFFF43F6E), 2 to Color(0xFF3B82F6), 4 to Color(0xFFFF8A3D), 5 to Color(0xFF8B5CF6))
val MEAL_INK = mapOf(0 to A.FatInk, 1 to A.GreenInk, 3 to A.ProteinInk, 2 to A.CarbInk, 4 to A.FatInk, 5 to A.FastInk)

@Composable
fun TodayScreen() {
    val s by Store.state.collectAsState()
    val date = Nav.diaryDate
    val entries = Store.entriesFor(s, date)
    val totals = Nutrition.sum(entries)
    val e = Store.energyFor(s, date)
    val burned = Store.burnedFor(s, date)

    val hello = when (LocalTime.now().hour) { in 5..11 -> "Buongiorno"; in 12..17 -> "Buon pomeriggio"; else -> "Buonasera" }
    MainPage(fullDay(date), "$hello,", homeName = s.profile.name, header = {
        CalendarPageHeader(s, "Oggi", date, "$hello,", onPick = { Nav.diaryDate = it })
    }) {
        DayTypeControl(s, date)

        // ---- Hero calorie
        Spacer(Modifier.height(16.dp))
        TodayCalorieCard(totals.kcal, e.targetKcal, burned) { Nav.go(Screen.DayNutrients(date)) }

        // ---- Macro
        Spacer(Modifier.height(12.dp))
        TodayMacroCard(totals.protein, e.protein, totals.carbs, e.carbs, totals.fat, e.fat)

        Spacer(Modifier.height(20.dp))
        ACard(Modifier.fillMaxWidth(), radius = 22.dp, padding = 4.dp, elevation = 4.dp) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth / LocalDensity.current.fontScale < 320.dp) Column {
                Row(Modifier.fillMaxWidth()) {
                    QuickTile("Aggiungi", TodayArt.Add, Modifier.weight(1f)) { Nav.go(Screen.AddFood(date, defaultMeal())) }
                    QuickTile("Foto piatto", TodayArt.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
                }
                Row(Modifier.fillMaxWidth()) {
                    QuickTile("Frigo", TodayArt.Fridge, Modifier.weight(1f)) { Nav.go(Screen.PantryScan("foto")) }
                    QuickTile("Digiuno", TodayArt.Timer, Modifier.weight(1f)) { Nav.go(Screen.Fasting) }
                }
            } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                QuickTile("Aggiungi", TodayArt.Add, Modifier.weight(1f)) { Nav.go(Screen.AddFood(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Foto piatto", TodayArt.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Frigo", TodayArt.Fridge, Modifier.weight(1f)) { Nav.go(Screen.PantryScan("foto")) }
                HomeDivider()
                QuickTile("Digiuno", TodayArt.Timer, Modifier.weight(1f)) { Nav.go(Screen.Fasting) }
            }
            }
            }
        }

        // ---- Diario
        Spacer(Modifier.height(0.dp))
        ASection("Diario", action = "Gestisci pasti") { Nav.go(Screen.DietSettings) }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { for (meal in MealSchedule.visible(s, date.toString())) MealCard(meal, date, s) }

        if (!s.profile.detailed) {
            Spacer(Modifier.height(12.dp))
            ACard(color = Color.White, radius = 22.dp, padding = 14.dp, elevation = 4.dp, onClick = { Nav.go(Screen.Questionnaire(null)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TonalIcon(Ic.Receipt, A.TonalInk, A.Tonal, box = 40.dp, radius = 14.dp)
                    GapW(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Rendi il calcolo più preciso", style = T.Label)
                        Text("Completa il tuo profilo", style = T.Small)
                    }
                    AIcon(Ic.Right, C.Muted, 18.dp)
                }
            }
        }

        // ---- Cosa mangio adesso?
        Spacer(Modifier.height(12.dp))
        JarvisChefBanner()



        FastingMini(s)

        // ---- Acqua
        Spacer(Modifier.height(14.dp))
        WaterCard(date, s)

        // ---- Vitamine
        NutrientInsight(s)

        // ---- Attività
        Spacer(Modifier.height(14.dp))
        ActivityCard(date, s)
        Spacer(Modifier.height(30.dp))
    }
}

// ============================================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekStrip(s: AppState, date: LocalDate, onPick: (LocalDate) -> Unit = { Nav.diaryDate = it }) {
    key(Nav.today) {
        val today = Nav.today
        val origin = 10000
        val pager = rememberPagerState(initialPage = (origin + CalendarWindow.pageFor(today, date)).coerceIn(0, 20000), pageCount = { 20001 })
        val selectedDate by rememberUpdatedState(date)
        val pick by rememberUpdatedState(onPick)
        LaunchedEffect(date, today) {
            val desired = (origin + CalendarWindow.pageFor(today, date)).coerceIn(0, 20000)
            if (pager.currentPage != desired) pager.scrollToPage(desired)
        }
        LaunchedEffect(pager, today) {
            snapshotFlow { pager.settledPage }.collect { page ->
                val offset = page - origin
                if (CalendarWindow.pageFor(today, selectedDate) != offset) {
                    pick(CalendarWindow.days(today, offset)[CalendarWindow.position(today, selectedDate)])
                }
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize().testTag("week-strip"), verticalAlignment = Alignment.CenterVertically) { page ->
            WeekPage(s, date, CalendarWindow.days(today, page - origin), page == pager.currentPage, pick)
        }
    }
}

@Composable
private fun WeekPage(s: AppState, date: LocalDate, days: List<LocalDate>, active: Boolean, onPick: (LocalDate) -> Unit) {
    val expanded = LocalDensity.current.fontScale > 1.5f
    val labels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
    val fr = remember(s.diary, s.profile, s.goals, s.energy, s.trainingNutrition, s.health, s.activities, s.steps, s.weights, days) {
        (0..6).map { i ->
            val d = days[i]
            val k = Nutrition.sum(Store.entriesFor(s, d)).kcal
            if (k <= 0) 0f else (k / Store.energyFor(s, d).targetKcal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        }
    }
    Column(Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (0..6).toList().chunked(if (expanded) 4 else 7).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in group) {
            val d = days[i]
            val sel = d == date
            val p by animateFloatAsState(fr[i], tween(1000), label = "week")
            Column(Modifier.weight(1f).then(if (active) Modifier.testTag("week-day-$i") else Modifier).semantics { contentDescription = d.toString() }.clickable { onPick(d) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(labels[d.dayOfWeek.value - 1], modifier = if (active) Modifier.testTag("week-label-$i") else Modifier, style = T.Small.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = if (sel) A.Ink else A.Muted))
                Box(
                    Modifier.size(44.dp).aShadow(CircleShape, if (sel) 8.dp else 3.dp, if (sel) Color(0x991D1B3A) else Color(0x401D1B3A)).clip(CircleShape)
                        .background(if (sel) C.Navy else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(44.dp)) {
                        val st = 3.dp.toPx(); val inset = st / 2 + 3.dp.toPx()
                        val sz = Size(size.width - inset * 2, size.height - inset * 2)
                        if (!sel) drawArc(C.Line, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(1.dp.toPx()))
                        if (p > 0f) drawArc(if (sel) Color(0xFFF8C089) else Color(0xFFF59E6C), -90f, 360f * p, false, Offset(inset, inset), sz, style = Stroke(st, cap = StrokeCap.Round))
                    }
                    Text("${d.dayOfMonth}", style = T.Num.copy(fontSize = 15.sp, color = if (sel) Color.White else A.Ink))
                }
            }
        }
    }
        }
        }
}

@Composable
private fun QuickTile(label: String, art: TodayArt, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 76.dp).clip(RoundedCornerShape(18.dp))
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TodayArtwork(art, Modifier.size(46.dp).testTag("quick-icon-$label"))
        Spacer(Modifier.height(8.dp))
        Text(label, style = T.Small.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = C.Navy), textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeDivider() {
    Box(Modifier.width(1.dp).height(60.dp).background(C.Line))
}

// ---------------------------------------------------------------- Diario

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun MealCard(meal: Int, date: LocalDate, s: AppState) {
    val entries = Store.entriesFor(s, date).filter { it.meal == meal }.sortedBy { it.time }
    val t = Nutrition.sum(entries)
    var open by rememberSaveable(meal, date.toString()) { mutableStateOf(false) }
    val planned = remember(s.plan, s.dietPrefs, s.trainingNutrition, s.weights, s.profile, s.goals, s.energy, date) { Store.plannedMeals(s, date).firstOrNull { it.diaryMeal == meal } }
    val shape = RoundedCornerShape(18.dp)
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(250), label = "chev")
    val kcal = if (entries.isNotEmpty()) t.kcal else planned?.solved?.total?.kcal
    val subtitle = when {
        planned != null -> "Consigliato: ${kcalFmt(planned.solved.total.kcal)} kcal"
        entries.isNotEmpty() -> "P ${t.protein.r0()} · C ${t.carbs.r0()} · G ${t.fat.r0()} g"
        else -> "Nessun alimento"
    }
    val art = when (meal) {
        0 -> R.drawable.approved_meal_0
        1 -> R.drawable.approved_meal_1
        2 -> R.drawable.approved_meal_2
        3 -> R.drawable.approved_meal_3
        4 -> R.drawable.approved_meal_4
        else -> R.drawable.approved_meal_5
    }
    Column(Modifier.fillMaxWidth().testTag("diary-meal-$meal").refShadow(shape, 5.dp).clip(shape).background(Color.White)
        .border(1.dp, RD.GreyLine, shape).animateContentSize(tween(260))) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val large = maxWidth / LocalDensity.current.fontScale < 285.dp
        val photo: @Composable () -> Unit = {
            Image(painterResource(art), null, Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            GapW(6.dp)
            OrangeDivider(33.dp)
        }
        val pill: @Composable () -> Unit = {
            if (kcal != null) Row(Modifier.testTag("diary-kcal-$meal").clip(RoundedCornerShape(20.dp)).background(Color(0xFFFDE8D2))
                .padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                AIcon(RIc.Flame, RD.Orange, 13.dp, fill = true); GapW(4.dp)
                Text("${kcalFmt(kcal)} kcal", style = T.Title2.copy(fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF9A4A12)), maxLines = 1, softWrap = false)
            }
        }
        val controls: @Composable () -> Unit = {
            Box(Modifier.size(19.dp).rotate(chevron), contentAlignment = Alignment.Center) { AIcon(RIc.Chevron, RD.Ink, 17.dp, 2.5f) }
            GapW(3.dp)
            Box(Modifier.size(48.dp)
                .semantics { contentDescription = "Aggiungi a ${MEAL_LABEL.getValue(meal)}" }
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { Nav.go(Screen.AddFood(date, meal)) }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(41.dp).clip(RoundedCornerShape(12.dp)).background(RD.ButtonBrush), contentAlignment = Alignment.Center) {
                    AIcon(RIc.Plus, Color.White, 23.dp, 3f)
                }
            }
        }
        if (large) Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                photo(); GapW(12.dp)
                Text(MEAL_LABEL.getValue(meal), modifier = Modifier.weight(1f).testTag("diary-title-$meal"), style = T.Title.copy(fontSize = 19.sp, lineHeight = 24.sp, color = RD.Ink))
            }
            GapH(8.dp)
            Text(subtitle, style = T.Body.copy(fontSize = 14.sp, lineHeight = 19.sp, color = RD.Grey))
            GapH(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { pill() }; controls() }
        } else {
            val roomy = maxWidth / LocalDensity.current.fontScale >= 349.dp
            Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                photo()
                GapW(8.dp)
                Column(Modifier.weight(1f)) {
                    Text(MEAL_LABEL.getValue(meal), style = T.Title.copy(fontSize = 15.sp, lineHeight = 18.sp, color = RD.Ink), modifier = Modifier.testTag("diary-title-$meal"))
                    Text(subtitle, style = T.Body.copy(fontSize = 12.sp, lineHeight = 16.sp, color = RD.Grey))
                    if (!roomy && kcal != null) { GapH(6.dp); pill() }
                }
                if (roomy) {
                    GapW(6.dp)
                    pill()
                }
                GapW(4.dp)
                controls()
            }
        }
        }
        if (open) Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
            if (entries.isEmpty()) {
                val dash = Color(0xFFD9D4EC)
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).drawBehind {
                        drawRoundRect(dash, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                    }.clickable { Nav.go(Screen.AddFood(date, meal)) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Nessun alimento registrato", style = T.Small.copy(fontSize = 13.sp)) }
            } else entries.forEachIndexed { index, en ->
                val et = Nutrition.of(en.food, en.grams)
                val itemShape = RoundedCornerShape(17.dp)
                if (index > 0) Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().testTag("diary-entry-${en.id}").clip(itemShape).background(Color.White)
                        .border(1.dp, RD.GreyLine, itemShape).clickable { Nav.go(Screen.FoodDetail(en.food, date, meal, en.grams, en)) }
                        .height(IntrinsicSize.Min).padding(start = 11.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.padding(vertical = 5.dp).fillMaxHeight().width(2.dp).clip(CircleShape).background(RD.Orange))
                    GapW(9.dp)
                    Column(Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(en.food.name, style = T.Label.copy(fontSize = 15.sp, lineHeight = 19.sp, color = RD.Ink))
                        GapH(3.dp)
                        Text("${amountText(en.grams)} ${if (en.food.liquid) "ml" else "g"}" +
                            " · P ${et.protein.r0()} g · C ${et.carbs.r0()} g · G ${et.fat.r0()} g", style = T.Small)
                    }
                    Box(Modifier.padding(vertical = 5.dp).fillMaxHeight().width(1.dp).background(RD.GreyLine))
                    Column(Modifier.widthIn(min = 42.dp).padding(horizontal = 5.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(et.kcal.r0(), style = T.Title2.copy(fontSize = 17.sp, lineHeight = 19.sp, color = RD.Ink))
                        Text("kcal", style = T.Small.copy(fontSize = 11.sp, lineHeight = 13.sp))
                    }
                    Box(Modifier.size(48.dp).semantics { contentDescription = "Elimina ${en.food.name}" }
                        .clickable { Store.removeEntry(en.id) }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(InsightTile), contentAlignment = Alignment.Center) {
                            AIcon("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12", RD.Ink, 16.dp, 2f)
                        }
                    }
                }
            }
            var moreOpen by remember { mutableStateOf(false) }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(IntrinsicSize.Min).testTag("diary-actions-$meal"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealAction("Foto AI", Ic.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, meal)) }
                MealAction("Codice a barre", RIc.Barcode, Modifier.weight(1f).testTag("meal-barcode-$meal")) {
                    Nav.go(Screen.AddFood(date, meal, scanOnOpen = true))
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    MealAction("Altre azioni", Ic.circ(12.0, 12.0, 9.0) + "M8 12h.1M12 12h.1M16 12h.1", Modifier.fillMaxWidth()) { moreOpen = true }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(text = { Text("Copia da ieri") }, onClick = { moreOpen = false; Store.copyMeal(date.minusDays(1), meal, date, meal) })
                        DropdownMenuItem(text = { Text("Copia su domani") }, onClick = { moreOpen = false; Store.copyMeal(date, meal, date.plusDays(1), meal) })
                        if (entries.isNotEmpty()) DropdownMenuItem(text = { Text("Svuota", color = C.Danger) }, onClick = { moreOpen = false; entries.forEach { Store.removeEntry(it.id) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun MealAction(label: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Column(modifier.fillMaxHeight().heightIn(min = 68.dp).clip(shape).background(Color.White).border(1.dp, RD.GreyLine, shape)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
        .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AIcon(icon, RD.OrangeText, 25.dp, 2f)
        GapH(5.dp)
        Text(label, textAlign = TextAlign.Center, style = T.Chip.copy(fontSize = 12.sp, lineHeight = 15.sp, color = RD.Ink))
    }
}

private fun fastPhase(h: Double) = when {
    h < 4 -> "digestione"; h < 8 -> "calo della glicemia"; h < 12 -> "uso del glicogeno"; h < 18 -> "più grassi come energia"; else -> "chetosi lieve"
}

@Composable
private fun FastingMini(s: AppState) {
    val start = s.fasting.activeStart ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val hours = (now - start) / 3_600_000.0
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.padding(top = 14.dp).fillMaxWidth().aShadow(shape, 8.dp, Color(0xB38B5CF6)).clip(shape)
            .background(C.Surface)
            .clickable { Nav.go(Screen.Fasting) }.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TonalIcon(Ic.Timer, A.FastInk, Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Digiuno ${s.fasting.planId} in corso", style = T.Title2.copy(fontSize = 17.sp))
                Text("${hours.r1()} h su ${s.fasting.targetHours} h · fase: ${fastPhase(hours)}", style = T.Small.copy(color = Color(0xFF4B4570)))
            }
        }
        Spacer(Modifier.height(14.dp))
        ABar((hours / s.fasting.targetHours).toFloat(), A.FastBar, C.SurfaceHi)
    }
}

// ---------------------------------------------------------------- Acqua

@Composable
private fun WaterCard(date: LocalDate, s: AppState) {
    val ml = s.water[date.toString()] ?: 0
    val goal = s.goals.waterMl.coerceAtLeast(1)
    val fill by animateFloatAsState((ml.toFloat() / goal).coerceIn(0f, 1f), tween(900), label = "water")
    ACard(Modifier.fillMaxWidth().testTag("home-water"), radius = 24.dp, padding = 10.dp,
        elevation = 4.dp, glow = Color(0x2009194D)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val inline = maxWidth / LocalDensity.current.fontScale >= 292.dp
            val controls: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(48.dp).testTag("water-minus").clip(RoundedCornerShape(14.dp))
                        .semantics { contentDescription = "Rimuovi un bicchiere" }
                        .clickable(enabled = ml > 0, role = androidx.compose.ui.semantics.Role.Button) {
                            val current = Store.current
                            Store.setWater(date, ((current.water[date.toString()] ?: 0) - current.glassMl).coerceAtLeast(0))
                        }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F2F6)),
                            contentAlignment = Alignment.Center) {
                            AIcon(Ic.Minus, RD.Ink.copy(alpha = if (ml > 0) 1f else 0.4f), 18.dp, 2.4f)
                        }
                    }
                    Row(Modifier.heightIn(min = 52.dp).testTag("water-plus").clip(RoundedCornerShape(14.dp))
                        .background(RD.NavyBrush).semantics { contentDescription = "Aggiungi un bicchiere" }
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                            val current = Store.current
                            Store.setWater(date, (current.water[date.toString()] ?: 0) + current.glassMl)
                        }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AIcon(Ic.Plus, Color.White, 15.dp, 2.8f)
                        GapW(4.dp)
                        Text("Bicchiere", style = T.BtnS.copy(color = Color.White, fontSize = 12.sp))
                    }
                }
            }
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TodayWaterBottle(fill, Modifier.size(if (inline) 74.dp else 80.dp)
                        .testTag("water-bottle").semantics {
                            contentDescription = "$ml ml su $goal ml"
                            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(fill, 0f..1f)
                        })
                    GapW(6.dp)
                    Box(Modifier.width(1.dp).height(68.dp).background(RD.GreyLine))
                    GapW(9.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Acqua", style = T.Label.copy(color = RD.Ink))
                        Text("${waterLitres(ml)} L", Modifier.testTag("water-amount"),
                            style = T.Title.copy(color = RD.Ink, fontSize = 27.sp, lineHeight = 31.sp))
                        Text("su ${waterLitres(goal)} L", style = T.Small)
                        Text("Bicchiere da ${s.glassMl} ml", style = T.Small.copy(fontSize = 11.sp, lineHeight = 14.sp))
                    }
                    if (inline) {
                        GapW(3.dp)
                        controls()
                    }
                }
                if (!inline) Box(Modifier.fillMaxWidth().padding(top = 5.dp), contentAlignment = Alignment.CenterEnd) {
                    controls()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Vitamine

/** Consigli sui micronutrienti ben visibili nella schermata principale. */
@Composable
private fun NutrientInsight(s: AppState) {
    VitaminOverview(s)
}

// ---------------------------------------------------------------- Attività

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ActivityCard(date: LocalDate, s: AppState) {
    val list = s.activities.filter { it.date == date.toString() }
    val watch = s.energy.mode == 2
    val h = s.health[date.toString()]
    val steps = Store.stepsFor(s, date)
    val goal = s.goals.steps.coerceAtLeast(1)
    var editSteps by remember(date) { mutableStateOf(false) }
    var stepsText by remember(date) { mutableStateOf(if (steps > 0) steps.toString() else "") }
    val fr = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val p by animateFloatAsState(fr, tween(1100), label = "steps")
    Column(Modifier.testTag("home-activity")) {
        ReferenceInsightCard(TodayArt.Activity) {
            ReferenceInsightHeading("Attività", "${dayLabel(date)} · movimento giornaliero", DashboardIcon.Runner, "Andamento") {
                Nav.go(Screen.Trend("activity"))
            }
            val minutes = list.sumOf { it.minutes.toLong() } +
                (if (watch) h?.sessions?.sumOf { (it.end - it.start) / 60000 } ?: 0L else 0L)
            ReferenceMetric("Passi", steps.thousands(), p, barModifier = Modifier.testTag("activity-progress").semantics {
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(p, 0f..1f)
                contentDescription = "${steps.thousands()} passi su ${goal.thousands()}"
            }) { Nav.go(Screen.Trend("activity")) }
            ReferenceMetric("Calorie attive", "${Store.burnedFor(s, date)} kcal", null) {
                Nav.go(Screen.Trend("activity", metric = 1))
            }
            ReferenceMetric("Allenamento", "$minutes min", null) {
                Nav.go(if (watch) Screen.Watch else Screen.Activity(date))
            }
        }
        if (watch) h?.sessions?.take(2)?.forEach { w ->
            ActivityRow(w.title, "${fmtTime(w.start)} · ${((w.end - w.start) / 60000)} min", w.kcal?.r0())
        }
        list.forEach { a -> ActivityRow(a.name, "${a.minutes} min · inserita a mano", a.kcal.toString(), onRemove = { Store.removeActivity(a.id) }) }
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Smartwatch", style = T.Link, modifier = Modifier.heightIn(min = 48.dp).clickable { Nav.go(Screen.Watch) }.padding(vertical = 12.dp))
            if (!watch) {
                Text("Aggiungi attività", style = T.Link, modifier = Modifier.heightIn(min = 48.dp).clickable { Nav.go(Screen.Activity(date)) }.padding(vertical = 12.dp))
                Text(if (editSteps) "Chiudi" else "Modifica passi", style = T.Link, modifier = Modifier.heightIn(min = 48.dp).clickable { editSteps = !editSteps }.padding(vertical = 12.dp))
            }
        }
        if (editSteps && !watch) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(stepsText, { stepsText = it.filter { c -> c.isDigit() }.take(6) }, "Passi di oggi", Modifier.fillMaxWidth(), number = true)
                PrimaryButton("Salva", Modifier.fillMaxWidth()) { Store.setSteps(date, stepsText.toIntOrNull() ?: 0); editSteps = false }
            }
        }
        if (s.energy.mode == 0 && (list.isNotEmpty() || steps > 0)) {
            Spacer(Modifier.height(8.dp))
            Text("Con il livello di attività fisso, le attività non cambiano l'obiettivo (sono già considerate). Puoi cambiarlo in Profilo → Smartwatch e attività.", style = T.Small)
        }
    }
}

@Composable
private fun ActivityRow(title: String, sub: String, kcal: String?, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
        Text(title, style = T.Label.copy(fontSize = 14.sp, lineHeight = 19.sp))
        Text(sub, style = T.Small.copy(fontSize = 13.sp, lineHeight = 18.sp))
        }
        if (kcal != null) Text("$kcal kcal", style = T.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.GreenInk), modifier = Modifier.padding(start = 8.dp))
        if (onRemove != null) Box(Modifier.size(48.dp).clip(CircleShape).semantics { contentDescription = "Elimina $title" }
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onRemove), contentAlignment = Alignment.Center) {
        AIcon("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12", C.Muted, 20.dp)
        }
    }
}

/** Keep a 250 ml glass visible as 0,25 L instead of rounding to 0,3 L. */
private fun waterLitres(ml: Int): String = java.text.NumberFormat.getNumberInstance(Locale.ITALIAN).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 2
}.format(ml / 1000.0)
