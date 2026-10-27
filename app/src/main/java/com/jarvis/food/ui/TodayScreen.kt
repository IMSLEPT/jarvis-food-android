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

fun defaultMeal(): Int = mealForHour(LocalTime.now().hour)

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

        // ---- Hero calorie
        Spacer(Modifier.height(20.dp))
        HeroCard(totals.kcal, e.targetKcal, burned, e.extraKcal) { Nav.go(Screen.DayNutrients(date)) }

        // ---- Macro
        Spacer(Modifier.height(12.dp))
        ACard(Modifier.fillMaxWidth(), radius = 20.dp, padding = 4.dp, elevation = 4.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MacroRing("Proteine", totals.protein, e.protein, C.Protein, A.ProteinSoft, A.ProteinInk, Modifier.weight(1f))
                HomeDivider()
                MacroRing("Carboidrati", totals.carbs, e.carbs, C.Blue, A.CarbSoft, A.CarbInk, Modifier.weight(1f))
                HomeDivider()
                MacroRing("Grassi", totals.fat, e.fat, C.Gold, A.FatSoft, A.FatInk, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(20.dp))
        ACard(Modifier.fillMaxWidth(), radius = 22.dp, padding = 4.dp, elevation = 4.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                QuickTile("Aggiungi", Ic.Plus, Modifier.weight(1f), primary = true) { Nav.go(Screen.AddFood(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Foto piatto", Ic.Camera, Modifier.weight(1f)) { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
                HomeDivider()
                QuickTile("Frigo", Ic.Fridge, Modifier.weight(1f)) { Nav.go(Screen.PantryScan("foto")) }
                HomeDivider()
                QuickTile("Digiuno", Ic.Timer, Modifier.weight(1f)) { Nav.go(Screen.Fasting) }
            }
        }

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


        // ---- Diario
        Spacer(Modifier.height(10.dp))
        Text("Diario", style = T.H2)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { for (meal in MEAL_ORDER) MealCard(meal, date, s) }

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
    val monday = date.with(DayOfWeek.MONDAY)
    val labels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
    val fr = remember(s.diary.size, s.profile, s.goals, s.energy, date) {
        (0..6).map { i ->
            val d = monday.plusDays(i.toLong())
            val k = Nutrition.sum(Store.entriesFor(s, d)).kcal
            if (k <= 0) 0f else (k / Store.energyFor(s, d).targetKcal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        }
    }
    Row(
        Modifier.fillMaxWidth().pointerInput(date) {
            var dx = 0f
            detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = {
                if (dx > 90f) onPick(date.minusWeeks(1)) else if (dx < -90f) onPick(date.plusWeeks(1))
            }) { _, d -> dx += d }
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0..6) {
            val d = monday.plusDays(i.toLong())
            val sel = d == date
            val p by animateFloatAsState(fr[i], tween(1000), label = "week")
            Column(Modifier.weight(1f).clickable { onPick(d) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(labels[i], style = T.Small.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (sel) A.Ink else A.Muted))
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

@Composable
private fun HeroCard(eaten: Double, target: Double, burned: Int, extra: Double, onClick: () -> Unit) {
    val over = eaten > target
    val fraction = if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f
    val p by animateFloatAsState(fraction, tween(800), label = "hero")
    ACard(Modifier.fillMaxWidth(), radius = 26.dp, brush = C.NavyBrush, padding = 20.dp, bordered = false, onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Le tue calorie", style = T.Title2.copy(color = Color.White), modifier = Modifier.weight(1f))
                AIcon(Ic.Right, Color.White.copy(alpha = 0.8f), 18.dp)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                val stacked = maxWidth / LocalDensity.current.fontScale < 260.dp
                val ring: @Composable () -> Unit = {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(100.dp)) {
                        val st = 9.dp.toPx(); val inset = st / 2; val sz = Size(size.width - st, size.height - st)
                        drawArc(Color(0x29FFFFFF), 135f, 270f, false, Offset(inset, inset), sz, style = Stroke(st, cap = StrokeCap.Round))
                        if (p > 0f) drawArc(if (over) Brush.sweepGradient(listOf(C.Danger, C.Danger)) else Brush.sweepGradient(C.OrangeArc + C.OrangeArc.first()), 135f, 270f * p, false, Offset(inset, inset), sz, style = Stroke(st, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(fraction * 100).toInt()}%", style = T.Big.copy(color = Color.White, fontSize = 30.sp, lineHeight = 34.sp))
                        Text("dell’obiettivo", style = T.Small.copy(color = Color(0xFFDCE5FA), fontSize = 10.sp))
                    }
                }
                }
                val remaining: @Composable () -> Unit = {
                Column {
                    Text(if (over) "Sei oltre di" else "Ti restano", style = T.Small.copy(color = Color(0xFFDCE5FA)))
                    Text(kcalFmt(kotlin.math.abs(target - eaten)), style = T.Big.copy(fontSize = 36.sp, lineHeight = 42.sp, color = if (over) Color(0xFFFFB4A9) else Color.White), maxLines = 1)
                    Text("kcal per ${if (Nav.diaryDate == LocalDate.now()) "oggi" else dayLabel(Nav.diaryDate).lowercase()}", style = T.Small.copy(color = Color(0xFFDCE5FA)))
                }
                }
                if (stacked) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    remaining(); GapH(10.dp); ring()
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { remaining() }; GapW(16.dp); ring()
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x29FFFFFF)))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                HeroStat("Mangiate", kcalFmt(eaten), Color.White, Modifier.weight(1f))
                HeroStat("Obiettivo", kcalFmt(target), Color.White, Modifier.weight(1f))
                HeroStat("Attività", if (extra > 0) "+${extra.r0()}" else burned.toString(), Color(0xFFB6F0D8), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = T.Small.copy(color = Color(0xFFDCE5FA), fontSize = 11.sp))
        Text(value, style = T.Num.copy(color = color, fontSize = 18.sp), maxLines = 1)
    }
}

@Composable
private fun MacroRing(label: String, value: Double, goal: Double, color: Color, soft: Color, ink: Color, modifier: Modifier) {
    val fraction = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
    val p by animateFloatAsState(fraction, tween(600), label = "macro")
    Column(modifier.clickable { Nav.go(Screen.DayNutrients(Nav.diaryDate)) }.padding(10.dp)) {
        Text(label, style = T.Small.copy(color = ink, fontSize = 11.sp))
        Text("${value.r0()} g", style = T.Num.copy(fontSize = 21.sp), modifier = Modifier.padding(top = 4.dp))
        Text("su ${goal.r0()} g", style = T.Small.copy(fontSize = 11.sp))
        ABar(p, Brush.horizontalGradient(listOf(color, color)), soft, 5.dp, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun QuickTile(label: String, icon: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val ink = C.Navy
    Column(modifier.heightIn(min = 88.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (primary) C.Accent else Color.Transparent), contentAlignment = Alignment.Center) {
            AIcon(icon, if (primary) Color.White else ink, if (primary) 28.dp else 42.dp, 2f, modifier = Modifier.testTag("quick-icon-$label"))
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
private fun MealCard(meal: Int, date: LocalDate, s: AppState) {
    val entries = Store.entriesFor(s, date).filter { it.meal == meal }.sortedBy { it.time }
    val t = Nutrition.sum(entries)
    var open by rememberSaveable(meal, date.toString()) { mutableStateOf(false) }
    val planned = remember(s.plan, s.dietPrefs, date) { Store.plannedMeals(s, date).firstOrNull { it.diaryMeal == meal } }
    val shape = RoundedCornerShape(26.dp)
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(250), label = "chev")
    Column(Modifier.fillMaxWidth().aShadow(shape, 6.dp, Color(0x591D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape).animateContentSize(tween(260))) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            MealArt(meal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(MEAL_LABEL.getValue(meal), style = T.Title2.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (entries.isNotEmpty()) "${t.kcal.r0()} kcal · P ${t.protein.r0()} · C ${t.carbs.r0()} · G ${t.fat.r0()}"
                    else if (planned != null) "Consigliato: ${planned.solved.total.kcal.r0()} kcal" else "Nessun alimento",
                    style = T.Small, maxLines = 1
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
                    Modifier.fillMaxWidth().height(44.dp).drawBehind {
                        drawRoundRect(dash, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                    }.clickable { Nav.go(Screen.AddFood(date, meal)) },
                    contentAlignment = Alignment.Center
                ) { Text("Nessun alimento ancora · tocca per aggiungere", style = T.Small.copy(fontSize = 13.sp)) }
            } else entries.forEach { en ->
                val et = Nutrition.of(en.food, en.grams)
                val tpl = en.planRef?.substringAfter('#', "")?.toIntOrNull()?.let { slot -> Store.plannedMeals(s, date).firstOrNull { it.slotIndex == slot }?.solved?.template }
                Row(
                    Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Surface)
                        .clickable { Nav.go(Screen.FoodDetail(en.food, date, meal, en.grams, en)) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tpl != null) RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.size(48.dp), "🍽️", 16.dp)
                    else { val (bg, ink) = tileColors(en.food.name); InitialTile(en.food.name, bg, ink, 48.dp, 16.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(en.food.name, style = T.Label.copy(fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (en.food.source == "quick") "aggiunta rapida" else "P ${et.protein.r0()} · C ${et.carbs.r0()} · G ${et.fat.r0()}" +
                                (if (en.planRef == null) " · ${en.grams.r1().replace(",0", "")} ${if (en.food.liquid) "ml" else "g"}" else ""),
                            style = T.Small, maxLines = 1
                        )
                    }
                    Text(et.kcal.r0(), style = T.Title2.copy(fontSize = 16.sp), modifier = Modifier.padding(horizontal = 6.dp))
                    Box(Modifier.size(34.dp).clip(CircleShape).clickable { Store.removeEntry(en.id) }, contentAlignment = Alignment.Center) {
                        AIcon("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12", C.Faint, 18.dp, 2f)
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealAction("Aggiungi") { Nav.go(Screen.AddFood(date, meal)) }
                MealAction("Foto AI") { Nav.go(Screen.PhotoAi(date, meal)) }
                MealAction("Copia da ieri") { Store.copyMeal(date.minusDays(1), meal, date, meal) }
                MealAction("Copia su domani") { Store.copyMeal(date, meal, date.plusDays(1), meal) }
                if (entries.isNotEmpty()) MealAction("Svuota", danger = true) { entries.forEach { Store.removeEntry(it.id) } }
            }
        }
    }
}

@Composable
private fun MealAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
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
private fun WaterCard(date: LocalDate, s: AppState) {
    val ml = s.water[date.toString()] ?: 0
    val goal = s.goals.waterMl.coerceAtLeast(1)
    val fill by animateFloatAsState((ml.toFloat() / goal).coerceIn(0f, 1f), tween(900), label = "water")
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.fillMaxWidth().aShadow(shape, 8.dp, Color(0xB306B6D4)).clip(shape)
            .background(C.Surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val glass = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
        Box(Modifier.size(width = 78.dp, height = 112.dp).clip(glass).background(C.SurfaceAlt).border(2.dp, C.Line, glass)) {
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text((ml / 1000.0).r1(), style = T.Title.copy(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold))
                Text("  su ${(goal / 1000.0).r1()} L", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 5.dp))
            }
            Text("bicchiere da ${s.glassMl} ml", style = T.Small.copy(color = Color(0xFF4B6A85)))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xE6FFFFFF)).clickable { Store.setWater(date, (ml - s.glassMl).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) {
                    AIcon(Ic.Minus, A.Ink, 18.dp, 2.6f)
                }
                Row(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(C.Navy).clickable { Store.setWater(date, ml + s.glassMl) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Plus, Color.White, 16.dp, 2.8f)
                    Spacer(Modifier.width(8.dp))
                    Text("Bicchiere", style = T.BtnS.copy(color = Color.White), maxLines = 1)
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
    val rep = remember(s.diary.size, s.profile) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal) }
    ASection("Da migliorare", action = "Analisi completa") { Nav.go(Screen.Micronutrients) }
    ACard(radius = 28.dp, elevation = 6.dp, glow = Color(0x591D1B3A), onClick = { Nav.go(Screen.Micronutrients) }) {
        Column(Modifier.fillMaxWidth()) {
            if (rep.loggedDays == 0) {
                Text("Registra i pasti di almeno un giorno completo", style = T.Title2)
                Text("Poi qui vedrai quali vitamine e minerali ti mancano e cosa mangiare per migliorare.", style = T.Muted)
                return@Column
            }
            val low = rep.low.take(3)
            if (low.isEmpty()) {
                Text("Ottimo equilibrio negli ultimi ${rep.loggedDays} giorni", style = T.Title2)
                Text("Nessun apporto medio sotto il 70% del riferimento.", style = T.Muted)
            } else {
                Text("Apporto basso negli ultimi ${rep.loggedDays} giorni registrati", style = T.Muted)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    low.forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(st.def.name, style = T.Label, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(12.dp))
                            ABar((st.pct / 100.0).toFloat(), A.Dawn, Color(0xFFFFE9E1), 10.dp, Modifier.weight(1f))
                            Text("${st.pct.r0()}%", style = T.Label.copy(color = A.Link, fontSize = 13.sp), modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
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
                        Text("passi", style = T.Small.copy(fontSize = 11.sp))
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
            Text(sub, style = T.Small.copy(fontSize = 11.5.sp))
        }
        if (kcal != null) Text("$kcal kcal", style = T.Label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.GreenInk))
        if (onRemove != null) Text("✕", style = T.Label.copy(color = C.Faint), modifier = Modifier.clip(CircleShape).clickable(onClick = onRemove).padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 2.dp))
    }
}
