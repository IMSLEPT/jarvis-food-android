package com.jarvis.food.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Store
import com.jarvis.food.engine.PlannedMeal
import java.time.LocalDate

/** App-owned header, shared by the five main destinations. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MainHeader(title: String, eyebrow: String? = null, homeName: String? = null, compact: Boolean = false) {
    if (LocalDensity.current.fontScale > 1.5f) {
        FlowRow(Modifier.fillMaxWidth().testTag("main-header").padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, modifier = Modifier.testTag("main-header-title"), style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
            Row(verticalAlignment = Alignment.CenterVertically) { HeaderAccountActions() }
        }
        return
    }
    val separateGreeting = eyebrow != null && homeName == null && LocalDensity.current.fontScale > 1.2f
    Column(Modifier.fillMaxWidth().testTag("main-header").padding(top = 4.dp, bottom = 12.dp)) {
        if (separateGreeting) {
            HeaderGreeting(eyebrow!!)
            GapH(4.dp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            if (eyebrow != null && !separateGreeting) HeaderGreeting(eyebrow)
            if (homeName != null && !compact) {
                Text(homeName, style = T.Display.copy(fontSize = 30.sp, lineHeight = 34.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(title, style = T.Body.copy(color = C.Muted), maxLines = 2)
            } else {
                Text(title, style = T.Title.copy(fontSize = if (title == "Piano") 30.sp else 23.sp, lineHeight = if (title == "Piano") 36.sp else 28.sp), maxLines = if (LocalDensity.current.fontScale > 1.2f) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            }
        }
        HeaderAccountActions()
        }
    }
}


/** Matching profile control everywhere; calendar is supplied by Today and Plan. */
@Composable
fun RowScope.HeaderAccountActions(onCalendar: (() -> Unit)? = null) {
    val s by Store.state.collectAsState()
    if (onCalendar != null) {
        HeaderIconButton(Ic.Calendar, "Calendario", onClick = onCalendar)
        GapW(8.dp)
    }
    HeaderControl("Apri profilo", onClick = { Nav.go(Screen.Profile) }) {
        Avatar(s.profile.name, s.profile.photo, 42.dp, shape = RoundedCornerShape(12.dp))
    }
}

@Composable
private fun HeaderGreeting(text: String) {
    Text(text, modifier = Modifier.testTag("home-greeting"),
        style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = C.Navy),
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** Every saved meal is rendered in chronological order with the same large photograph. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlanJourney(s: AppState, date: LocalDate, meals: List<PlannedMeal>) {
    if (meals.isEmpty()) return
    var selectedSlot by androidx.compose.runtime.saveable.rememberSaveable(date.toString()) { androidx.compose.runtime.mutableStateOf(meals.first().slotIndex) }
    val activeSlot = selectedSlot.takeIf { index -> meals.any { it.slotIndex == index } } ?: meals.first().slotIndex
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val requesters = androidx.compose.runtime.remember(date, meals.map { it.slotIndex }) {
        meals.associate { it.slotIndex to androidx.compose.foundation.relocation.BringIntoViewRequester() }
    }
    Text("I pasti di oggi", style = T.Title.copy(fontSize = 23.sp))
    GapH(14.dp)
    Row(Modifier.fillMaxWidth().testTag("plan-slot-strip").horizontalScroll(rememberScrollState()).padding(vertical = 6.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        meals.forEach { meal ->
            val selected = meal.slotIndex == activeSlot
            // Immagine 4: pillola con tessera pesca, separatore arancio, nome e freccia nel cerchio.
            val shape = RoundedCornerShape(18.dp)
            Row(Modifier.heightIn(min = 56.dp).testTag("plan-slot-${meal.diaryMeal}")
                .refShadow(shape, if (selected) 5.dp else 2.dp).clip(shape)
                .background(RD.NavyBrush)
                .border(if (selected) 2.dp else 1.dp, if (selected) DashboardOrange else Color(0xFF244576), shape)
                .selectable(selected, role = Role.Tab, onClick = {
                    selectedSlot = meal.slotIndex
                    scope.launch { requesters[meal.slotIndex]?.bringIntoView() }
                }).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PeachTile(40.dp, radius = 12.dp, brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFDEBDC), Color(0xFFFFF5EE)))) { MealDuoIcon(meal.diaryMeal, 26.dp) }
                GapW(12.dp); OrangeDivider(32.dp); GapW(12.dp)
                Text(meal.slotName, style = T.Title.copy(fontSize = 16.sp, lineHeight = 21.sp, color = Color.White))
                GapW(12.dp)
                ChevronCircle(34.dp, if (selected) Color(0xFF245698) else Color(0xFF173969), Color.White)
            }
        }
    }
    meals.forEach { meal ->
        GapH(16.dp)
        CompactPlanCard(s, date, meal, Modifier.bringIntoViewRequester(requesters.getValue(meal.slotIndex)))
    }
}

private fun mealIcon(id: Int) = when (id) { 0 -> Ic.MealBreakfast; 1 -> Ic.MealLunch; 2, 5 -> Ic.MealDinner; else -> Ic.MealSnack }

/** Immagine 1: scheda del pasto con foto, riquadro tempo/kcal/macro e azioni navy, bianca e arancio. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactPlanCard(s: AppState, date: LocalDate, m: PlannedMeal, headingModifier: Modifier = Modifier) {
    val r = m.solved
    val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
    val expanded = LocalDensity.current.fontScale > 1.2f || LocalConfiguration.current.screenWidthDp < 380
    val shape = RoundedCornerShape(PremiumScale.cardRadius)
    Column(Modifier.fillMaxWidth().testTag("plan-card-${m.diaryMeal}").refShadow(shape, 6.dp).clip(shape).background(Color.White)
        .border(1.dp, Color(0xFFE6E8EE), shape)
        .drawBehindWarm()) {
        Column(headingModifier.fillMaxWidth().testTag("plan-meal-header-${m.diaryMeal}").padding(start = PremiumScale.inset, end = PremiumScale.inset, top = PremiumScale.inset)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(PremiumScale.iconTile).clip(RoundedCornerShape(12.dp)).background(RD.OrangeBrush), contentAlignment = Alignment.Center) {
                        AIcon(RIc.BowlSmile, Color.White, PremiumScale.icon, 2.4f)
                    }
                    GapW(10.dp)
                    Box(Modifier.width(1.dp).height(28.dp).background(RD.GreyLine))
                    GapW(10.dp)
                    Text(m.slotName, style = T.Title2.copy(fontSize = PremiumScale.label, lineHeight = PremiumScale.labelLine, color = RD.Ink), modifier = Modifier.weight(1f))
                }
                Text(if (eaten) "✓ Mangiato" else "Da mangiare", modifier = (if (expanded) Modifier.fillMaxWidth() else Modifier)
                    .clip(RoundedCornerShape(18.dp)).background(if (eaten) A.GreenSoft else Color(0xFFEFEDEB))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = T.Label.copy(fontSize = 12.sp, lineHeight = 17.sp, color = if (eaten) A.GreenInk else RD.Ink))
            }
            GapH(12.dp)
            Text(r.template.name, modifier = Modifier.fillMaxWidth().testTag("plan-recipe-title-${m.diaryMeal}").clickable(role = Role.Button) {
                Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal))
            }, style = T.Title.copy(fontSize = PremiumScale.title, lineHeight = PremiumScale.titleLine, color = RD.Ink))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("plan-recipe-${m.diaryMeal}")
            .clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClickLabel = "Apri ricetta") {
                Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal))
            }) {
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.fillMaxWidth().then(if (s.recipePhotos[r.template.id] != null || EditorialPhotos.asset(r.template.id, r.template.image) != null) Modifier.aspectRatio(2.25f) else Modifier.height(64.dp)).testTag("plan-photo-${m.diaryMeal}"), recEmoji(r.template.meal), 24.dp, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        // Riquadro informazioni: tempo, calorie e macro con barre colorate.
        val strip = RoundedCornerShape(16.dp)
        // Barre: quota del fabbisogno giornaliero di ciascun macro coperta da questo pasto.
        val target = Store.energyFor(s, date)
        val time: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIcon(RIc.Clock, RD.Ink, PremiumScale.icon, 2.4f); GapW(8.dp)
                Column {
                    Text("${r.template.minutes} min", style = T.Title.copy(fontSize = PremiumScale.label, lineHeight = PremiumScale.labelLine, color = RD.Ink))
                    Text(com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }, style = T.Body.copy(fontSize = 14.sp, lineHeight = 18.sp, color = RD.Grey))
                }
            }
        }
        val kcal: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DuoIcon(RIc.Bowl, RIc.Steam, PremiumScale.icon, stroke = 2.4f, ink = RD.OrangeDeep, tone = RD.OrangeDeep); GapW(6.dp)
                Text("${kcalFmt(r.total.kcal)} kcal", style = T.Title.copy(fontSize = PremiumScale.title, lineHeight = PremiumScale.titleLine, color = RD.Ink))
            }
        }
        val macros: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroBar("P", r.total.protein, target.protein, Color(0xFFE2294F), Color(0xFFFAD0DA))
                MacroBar("C", r.total.carbs, target.carbs, Color(0xFF1F63E8), Color(0xFFD3E1FB))
                MacroBar("G", r.total.fat, target.fat, A.FatInk, Color(0xFFFCE3C2), Color(0xFFF7A93B))
            }
        }
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
            // Tempo, kcal e macro in una riga solo se c'è spazio anche con il carattere ingrandito.
            val oneRow = !expanded && maxWidth >= 392.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale
            if (expanded) Column(Modifier.fillMaxWidth().clip(strip).background(RD.Strip).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                time(); kcal(); macros()
            } else if (oneRow) Row(Modifier.fillMaxWidth().clip(strip).background(RD.Strip).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                time(); GapW(8.dp)
                Box(Modifier.width(1.dp).height(44.dp).background(RD.GreyLine)); GapW(8.dp)
                kcal(); GapW(8.dp)
                Box(Modifier.width(1.dp).height(44.dp).background(RD.GreyLine)); GapW(8.dp)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { macros() }
            } else Column(Modifier.fillMaxWidth().clip(strip).background(RD.Strip).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { time() }
                    Box(Modifier.width(1.dp).height(44.dp).background(RD.GreyLine)); GapW(12.dp)
                    Box(Modifier.weight(1.2f)) { kcal() }
                }
                GapH(10.dp)
                Box(Modifier.fillMaxWidth().height(1.dp).background(RD.GreyLine))
                GapH(10.dp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { macros() }
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
            // Riferimento: due pulsanti della stessa altezza, testo su una riga.
            val eatenModifier = (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1.3f)).testTag("plan-eaten-${m.diaryMeal}")
            if (eaten) RefOutlineButton("Mangiato", eatenModifier, icon = RIc.Check, divider = false, height = PremiumScale.actionHeight, textSize = 14.sp) { Store.uneatPlanned(date, m.slotIndex) }
            else RefPrimaryButton("Segna mangiato", eatenModifier, icon = RIc.Check, divider = false, height = PremiumScale.actionHeight, textSize = 14.sp) { Store.eatPlanned(date, m); Store.consumeFromPantry(r) }
            RefOutlineButton("Alternative", (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1f)).testTag("plan-alternatives-${m.diaryMeal}"), icon = RIc.Swap, divider = false,
                height = PremiumScale.actionHeight, textSize = 14.sp) {
                Nav.go(Screen.Alternatives(date, m.slotIndex))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).heightIn(min = 48.dp).testTag("plan-barcode-${m.diaryMeal}")
            .clickable(role = Role.Button) { Nav.go(Screen.AddFood(date, m.diaryMeal, scanOnOpen = true)) },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            AIcon(RIc.Barcode, RD.OrangeDeep, PremiumScale.icon, 2.2f); GapW(10.dp)
            Text("Codice a barre", style = T.Title.copy(fontSize = 15.sp, lineHeight = 21.sp, color = RD.OrangeText))
        }
        GapH(6.dp)
    }
}

/** Sfumatura pesca leggera in alto a destra della scheda pasto (immagine 1). */
private fun Modifier.drawBehindWarm(): Modifier = this.drawBehind {
    drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0x33FFD9BF), Color.Transparent),
        center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.85f, 0f))
}

/** Macro con barra di avanzamento sul target del pasto. */
@Composable
private fun MacroBar(letter: String, grams: Double, target: Double, ink: Color, track: Color, fill: Color = ink) {
    val fraction = if (target > 0) (grams / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$letter ${grams.r0()} g", style = T.Title2.copy(fontSize = 14.sp, lineHeight = 19.sp, color = ink), maxLines = 1, softWrap = false)
        GapH(5.dp)
        Box(Modifier.width(40.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(track)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(fill.copy(alpha = 0.75f)))
        }
    }
}

/** The header and bottom navigation stay in place while app-owned content scrolls. */
@Composable
fun MainPage(title: String, eyebrow: String? = null, resetKey: Any? = null, homeName: String? = null, header: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    val threshold = with(LocalDensity.current) { 64.dp.roundToPx() }
    val compact by androidx.compose.runtime.remember(scroll, threshold) { derivedStateOf { scroll.value > threshold } }
    androidx.compose.runtime.LaunchedEffect(resetKey) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
        val expanded = LocalDensity.current.fontScale > 1.5f
        if (header != null) header() else Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) { MainHeader(if (expanded && homeName != null) "Oggi" else title, if (expanded) null else eyebrow, if (expanded) null else homeName, compact) }
        Column(Modifier.weight(1f).fillMaxWidth().background(C.Bg).testTag("main-scroll").verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            if (expanded && homeName != null) {
                if (eyebrow != null) HeaderGreeting(eyebrow)
                Text(homeName, modifier = Modifier.testTag("home-name"), style = T.Display.copy(fontSize = 30.sp, lineHeight = 34.sp))
                if (header == null) Text(title, style = T.Body.copy(color = C.Muted))
                GapH(12.dp)
            }
            content()
        }
    }
}
