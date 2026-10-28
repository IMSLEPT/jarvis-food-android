package com.jarvis.food.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.MealSchedule
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.engine.MicroAnalysis
import kotlinx.coroutines.delay
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
    MainPage(fullDay(date), "$hello,", homeName = s.profile.name) {
        // ---- Settimana
        Spacer(Modifier.height(8.dp))
        WeekStrip(s, date)
        DayTypeControl(s, date)

        // ---- Hero calorie
        Spacer(Modifier.height(16.dp))
        HeroCard(totals.kcal, e.targetKcal, burned, e.extraKcal) { Nav.go(Screen.DayNutrients(date)) }

        // ---- Macro
        Spacer(Modifier.height(12.dp))
        ACard(Modifier.fillMaxWidth(), radius = 20.dp, padding = 4.dp, elevation = 4.dp) {
            if (LocalDensity.current.fontScale > 1.5f) Column {
                MacroRing("Proteine", totals.protein, e.protein, C.Protein, A.ProteinSoft, A.ProteinInk, Modifier.fillMaxWidth())
                MacroRing("Carboidrati", totals.carbs, e.carbs, C.Blue, A.CarbSoft, A.CarbInk, Modifier.fillMaxWidth())
                MacroRing("Grassi", totals.fat, e.fat, C.Gold, A.FatSoft, A.FatInk, Modifier.fillMaxWidth())
            } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MacroRing("Proteine", totals.protein, e.protein, C.Protein, A.ProteinSoft, A.ProteinInk, Modifier.weight(1f))
                HomeDivider()
                MacroRing("Carboidrati", totals.carbs, e.carbs, C.Blue, A.CarbSoft, A.CarbInk, Modifier.weight(1f))
                HomeDivider()
                MacroRing("Grassi", totals.fat, e.fat, C.Gold, A.FatSoft, A.FatInk, Modifier.weight(1f))
            }
            }
        }

        Spacer(Modifier.height(20.dp))
        ACard(Modifier.fillMaxWidth(), radius = 22.dp, padding = 4.dp, elevation = 4.dp) {
            if (LocalDensity.current.fontScale > 1.5f) Column {
                Row(Modifier.fillMaxWidth()) {
                    QuickTile("Aggiungi", Ic.Plus, Modifier.weight(1f), primary = true) { Nav.go(Screen.AddFood(date, defaultMeal())) }
                    QuickTile("Foto piatto", Ic.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
                }
                Row(Modifier.fillMaxWidth()) {
                    QuickTile("Frigo", Ic.Fridge, Modifier.weight(1f)) { Nav.go(Screen.PantryScan("foto")) }
                    QuickTile("Digiuno", Ic.Timer, Modifier.weight(1f)) { Nav.go(Screen.Fasting) }
                }
            } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                QuickTile("Aggiungi", Ic.Plus, Modifier.weight(1f), primary = true) { Nav.go(Screen.AddFood(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Foto piatto", Ic.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Frigo", Ic.Fridge, Modifier.weight(1f)) { Nav.go(Screen.PantryScan("foto")) }
                HomeDivider()
                QuickTile("Digiuno", Ic.Timer, Modifier.weight(1f)) { Nav.go(Screen.Fasting) }
            }            }

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
        ACard(color = A.Tonal, brush = Brush.horizontalGradient(listOf(C.AccentSoft, C.AccentSoft)), radius = 24.dp, bordered = false, elevation = 0.dp, padding = 14.dp, onClick = { Nav.go(Screen.WhatNow) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { AIcon(Ic.Spark, A.TonalInk, 22.dp, 2f) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cosa mangio adesso?", style = T.Title2)
                    Text("Piatti su misura per i macro che ti restano oggi.", style = T.Small)
                }
                AIcon(Ic.Right, C.Navy, 20.dp, 2.4f)
            }
        }



        FastingMini(s)

        // ---- Acqua
        Spacer(Modifier.height(14.dp))
        WaterCard(date, s)

        // ---- Vitamine
        NutrientInsight(s)

        // ---- Attività
        ASection("Attività", action = if (s.energy.mode == 2) "Smartwatch" else "Aggiungi") {
            if (s.energy.mode == 2) Nav.go(Screen.Watch) else Nav.go(Screen.Activity(date))
        }
        Spacer(Modifier.height(0.dp))
        ActivityCard(date, s)
        Spacer(Modifier.height(30.dp))
    }
}

// ============================================================================================

@Composable
fun WeekStrip(s: AppState, date: LocalDate, onPick: (LocalDate) -> Unit = { Nav.diaryDate = it }) {
    val expanded = LocalDensity.current.fontScale > 1.5f
    val monday = date.with(DayOfWeek.MONDAY)
    val labels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
    val fr = remember(s.diary, s.profile, s.goals, s.energy, s.trainingNutrition, s.health, s.activities, s.steps, s.weights, date) {
        (0..6).map { i ->
            val d = monday.plusDays(i.toLong())
            val k = Nutrition.sum(Store.entriesFor(s, d)).kcal
            if (k <= 0) 0f else (k / Store.energyFor(s, d).targetKcal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        }
    }
    Column(
        Modifier.fillMaxWidth().pointerInput(date) {
            var dx = 0f
            detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = {
                if (dx > 90f) onPick(date.minusWeeks(1)) else if (dx < -90f) onPick(date.plusWeeks(1))
            }) { _, d -> dx += d }
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (0..6).toList().chunked(if (expanded) 4 else 7).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in group) {
            val d = monday.plusDays(i.toLong())
            val sel = d == date
            val p by animateFloatAsState(fr[i], tween(1000), label = "week")
            Column(Modifier.weight(1f).clickable { onPick(d) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(labels[i], modifier = Modifier.testTag("week-label-$i"), style = T.Small.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) A.Ink else A.Muted))
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun HeroCard(eaten: Double, target: Double, burned: Int, extra: Double, onClick: () -> Unit) {
    val over = eaten > target
    val fraction = if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f
    val p by animateFloatAsState(fraction, tween(800), label = "hero")
    ACard(Modifier.fillMaxWidth(), radius = 24.dp, brush = C.NavyBrush, padding = 16.dp, bordered = false, onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Le tue calorie", style = T.Label.copy(color = Color(0xFFDCE5FA)), modifier = Modifier.weight(1f))
                AIcon(Ic.Right, Color.White, 20.dp)
            }
            GapH(8.dp)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(kcalFmt(kotlin.math.abs(target - eaten)), style = T.Big.copy(fontSize = 36.sp, lineHeight = 42.sp, color = if (over) Color(0xFFFFB4A9) else Color.White))
                Text(if (over) "kcal oltre l’obiettivo" else "kcal disponibili", style = T.Small.copy(color = Color(0xFFDCE5FA)), modifier = Modifier.padding(top = 18.dp))
            }
            GapH(12.dp)
            ABar(p, Brush.horizontalGradient(listOf(C.Accent, Color(0xFFFFAD60))), Color(0x33FFFFFF), 6.dp)
            GapH(12.dp)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mangiate ${kcalFmt(eaten)}", style = T.Small.copy(color = Color.White))
                Text("Obiettivo ${kcalFmt(target)}", style = T.Small.copy(color = Color.White))
                if (burned > 0 || extra > 0) Text("Attività ${if (extra > 0) "+${extra.r0()}" else burned.toString()}", style = T.Small.copy(color = Color(0xFFB6F0D8)))
            }
        }
    }
}

@Composable
private fun MacroRing(label: String, value: Double, goal: Double, color: Color, soft: Color, ink: Color, modifier: Modifier) {
    val fraction = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
    val p by animateFloatAsState(fraction, tween(600), label = "macro")
    Column(modifier.clickable { Nav.go(Screen.DayNutrients(Nav.diaryDate)) }.padding(10.dp)) {
        Text(label, style = T.Small.copy(color = ink, fontSize = 12.sp))
        Text("${value.r0()} g", style = T.Num.copy(fontSize = 21.sp), modifier = Modifier.padding(top = 4.dp))
        Text("su ${goal.r0()} g", style = T.Small.copy(fontSize = 12.sp))
        ABar(p, Brush.horizontalGradient(listOf(color, color)), soft, 5.dp, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun QuickTile(label: String, icon: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val ink = C.Navy
    Column(modifier.heightIn(min = 76.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (primary) C.Accent else Color.Transparent), contentAlignment = Alignment.Center) {
            AIcon(icon, if (primary) Color.White else ink, if (primary) 28.dp else 40.dp, 2f, modifier = Modifier.testTag("quick-icon-$label"))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = T.Small.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ink), textAlign = TextAlign.Center)
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
    val shape = RoundedCornerShape(24.dp)
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(250), label = "chev")
    Column(Modifier.fillMaxWidth().aShadow(shape, 6.dp, Color(0x591D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape).animateContentSize(tween(260))) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            MealArt(meal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(MEAL_LABEL.getValue(meal), style = T.Title2.copy(fontSize = 17.sp))
                Text(
                    if (entries.isNotEmpty()) "${t.kcal.r0()} kcal · P ${t.protein.r0()} · C ${t.carbs.r0()} · G ${t.fat.r0()}"
                    else if (planned != null) "Consigliato: ${planned.solved.total.kcal.r0()} kcal" else "Nessun alimento",
                    style = T.Small
                )
            }
            if (entries.isNotEmpty()) Box(Modifier.padding(end = 8.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("${entries.size}", style = T.Chip.copy(fontSize = 12.sp, color = A.Muted))
            }
            Box(Modifier.size(32.dp).rotate(chevron), contentAlignment = Alignment.Center) { AIcon("M6 9l6 6 6-6", A.Muted, 20.dp, 2.4f) }
            Spacer(Modifier.width(6.dp))
            PlusButton("Aggiungi a ${MEAL_LABEL.getValue(meal)}") { Nav.go(Screen.AddFood(date, meal)) }
        }
        if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
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
                val itemShape = RoundedCornerShape(16.dp)
                if (index > 0) Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().testTag("diary-entry-${en.id}").clip(itemShape).background(C.Bg.copy(alpha = 0.65f))
                        .border(1.dp, C.Line, itemShape).clickable { Nav.go(Screen.FoodDetail(en.food, date, meal, en.grams, en)) }
                        .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(en.food.name, style = T.Label.copy(fontSize = 15.sp, lineHeight = 20.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text("${amountText(en.grams)} ${if (en.food.liquid) "ml" else "g"}" +
                            " · P ${et.protein.r0()} g · C ${et.carbs.r0()} g · G ${et.fat.r0()} g", style = T.Small)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(et.kcal.r0(), style = T.Title2.copy(fontSize = 16.sp))
                        Text("kcal", style = T.Small.copy(fontSize = 12.sp))
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).semantics { contentDescription = "Elimina ${en.food.name}" }
                        .clickable { Store.removeEntry(en.id) }, contentAlignment = Alignment.Center) {
                        AIcon("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12", C.Faint, 18.dp, 2f)
                    }
                }
            }
            var moreOpen by remember { mutableStateOf(false) }
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealAction("Foto AI") { Nav.go(Screen.PhotoAi(date, meal)) }
                Box {
                    MealAction("Altre azioni") { moreOpen = true }
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
private fun MealAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, style = T.Chip.copy(fontSize = 12.5.sp, color = if (danger) C.Danger else A.TonalInk), maxLines = 1, softWrap = false)
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun WaterCard(date: LocalDate, s: AppState) {
    val ml = s.water[date.toString()] ?: 0
    val goal = s.goals.waterMl.coerceAtLeast(1)
    val fill by animateFloatAsState((ml.toFloat() / goal).coerceIn(0f, 1f), tween(900), label = "water")
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().aShadow(shape, 8.dp, Color(0xB306B6D4)).clip(shape)
            .background(C.Surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val glass = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
        Box(Modifier.size(width = if (LocalDensity.current.fontScale > 1.2f) 48.dp else 64.dp, height = 96.dp).clip(glass).background(C.SurfaceAlt).border(2.dp, C.Line, glass)) {
            Canvas(Modifier.fillMaxSize()) {
                val h = size.height * fill
                if (h > 0f) {
                    val top = size.height - h
                    drawRect(A.WaterFill, Offset(0f, top), Size(size.width, h))
                    val w = Path().apply {
                        moveTo(0f, top); val seg = size.width / 4
                        for (i in 0 until 4) { quadraticTo(seg * i + seg / 2, top - 8.dp.toPx(), seg * (i + 1), top) }
                        lineTo(size.width, top + 12.dp.toPx()); lineTo(0f, top + 12.dp.toPx()); close()
                    }
                    drawPath(w, Color(0xFF5CD6EA))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Acqua", style = T.Label.copy(fontSize = 13.sp, color = A.WaterInk))
            Text("${(ml / 1000.0).r1()} L", style = T.Title.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold))
            Text("su ${(goal / 1000.0).r1()} L", style = T.Small)
            Text("bicchiere da ${s.glassMl} ml", style = T.Small.copy(color = Color(0xFF4B6A85)))
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xE6FFFFFF)).clickable { Store.setWater(date, (ml - s.glassMl).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) {
                    AIcon(Ic.Minus, A.Ink, 18.dp, 2.6f)
                }
                Row(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(C.Navy).clickable { Store.setWater(date, ml + s.glassMl) }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Plus, Color.White, 16.dp, 2.8f)
                    Spacer(Modifier.width(8.dp))
                    Text("Bicchiere", style = T.BtnS.copy(color = Color.White))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Vitamine

/** Consigli sui micronutrienti ben visibili nella schermata principale. */
@Composable
private fun NutrientInsight(s: AppState) {
    val e = Store.energyFor(s, LocalDate.now())
    val rep = remember(s.diary, s.profile, e.baseKcal) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal) }
    ASection("Equilibrio dei nutrienti", action = "Analisi completa") { Nav.go(Screen.Micronutrients) }
    ACard(radius = 28.dp, elevation = 6.dp, glow = Color(0x591D1B3A), onClick = { Nav.go(Screen.Micronutrients) }) {
        Column(Modifier.fillMaxWidth()) {
            if (rep.loggedDays == 0) {
                Text("Registra i pasti di almeno un giorno completo", style = T.Title2)
                Text("Le stime si basano soltanto sugli alimenti che registri.", style = T.Muted)
                return@Column
            }
            val low = rep.low.take(3)
            if (low.isEmpty()) {
                Text("Ottimo equilibrio negli ultimi ${rep.loggedDays} giorni", style = T.Title2)
                Text("Nessun apporto medio sotto il 70% del riferimento.", style = T.Muted)
            } else {
                Text("Stima su ${rep.loggedDays} ${if (rep.loggedDays == 1) "giorno registrato" else "giorni registrati"}. Le registrazioni incomplete possono abbassare i valori.", style = T.Muted)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    low.forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(st.def.name, style = T.Label, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            ABar((st.pct / 100.0).toFloat(), A.Dawn, Color(0xFFFFE9E1), 8.dp, Modifier.weight(1f))
                            Text("${st.pct.r0()}%", style = T.Label.copy(color = A.Link, fontSize = 13.sp), modifier = Modifier.widthIn(min = 44.dp), textAlign = TextAlign.End)
                        }
                    }
                }
                val tip = MicroAnalysis.tips(Db.db, low.first(), s.dietPrefs.exclude, 1).firstOrNull()
                if (tip != null) Box(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text("Prova ${tip.grams.r0()} g di ${tip.food.name.lowercase()}: copre il ${tip.pctOfRef.r0()}% di ${low.first().def.name.lowercase()} che ti manca.",
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF064E3B)))
                }
            }
            val gap = rep.groups.filter { (it.daysSince ?: 99) >= 7 }.take(2)
            if (gap.isNotEmpty()) Text(gap.joinToString("  ·  ") { "${it.label}: ${it.daysSince?.let { d -> "$d giorni fa" } ?: "mai registrato"}" },
                style = T.Small.copy(color = A.Link), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

// ---------------------------------------------------------------- Attività

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
    ACard(radius = 28.dp, elevation = 6.dp, glow = Color(0x591D1B3A)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(96.dp)) {
                        val st = 12.dp.toPx(); val inset = st / 2 + 2.dp.toPx(); val sz = Size(size.width - inset * 2, size.height - inset * 2)
                        drawArc(Color(0xFFD9F6EA), 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(st))
                        if (p > 0f) drawArc(Color(0xFF10B981), -90f, 360f * p, false, Offset(inset, inset), sz, style = Stroke(st, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(fr * 100).toInt()}%", style = T.Num.copy(fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold))
                        Text("passi", style = T.Small.copy(fontSize = 12.sp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(steps.thousands(), style = T.Num.copy(fontSize = 20.sp))
                        Text("  su ${goal.thousands()}", style = T.Small.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), modifier = Modifier.padding(bottom = 2.dp))
                    }
                    if (watch) h?.sessions?.take(2)?.forEach { w ->
                        ActivityRow(w.title, "${fmtTime(w.start)} · ${((w.end - w.start) / 60000)} min", w.kcal?.r0())
                    }
                    list.forEach { a -> ActivityRow(a.name, "${a.minutes} min · inserita a mano", a.kcal.toString(), onRemove = { Store.removeActivity(a.id) }) }
                    if (!watch) Text(if (editSteps) "Chiudi" else "Modifica passi", style = T.Link, modifier = Modifier.clickable { editSteps = !editSteps }.padding(vertical = 2.dp))
                }
            }
            if (editSteps && !watch) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JField(stepsText, { stepsText = it.filter { c -> c.isDigit() }.take(6) }, "Passi di oggi", Modifier.weight(1f), number = true)
                    Spacer(Modifier.width(8.dp))
                    PrimaryButton("Salva") { Store.setSteps(date, stepsText.toIntOrNull() ?: 0); editSteps = false }
                }
            }
            if (s.energy.mode == 0 && (list.isNotEmpty() || steps > 0)) {
                Spacer(Modifier.height(8.dp))
                Text("Con il livello di attività fisso, le attività non cambiano l'obiettivo (sono già considerate). Puoi cambiarlo in Profilo → Smartwatch e attività.", style = T.Small)
            }
        }
    }
}

@Composable
private fun ActivityRow(title: String, sub: String, kcal: String?, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label.copy(fontSize = 13.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.Small.copy(fontSize = 12.sp))
        }
        if (kcal != null) Text("$kcal kcal", style = T.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.GreenInk))
        if (onRemove != null) Text("✕", style = T.Label.copy(color = C.Faint), modifier = Modifier.clip(CircleShape).clickable(onClick = onRemove).padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 2.dp))
    }
}
