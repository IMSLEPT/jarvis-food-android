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


/** The same family/profile controls in every main destination, including Jarvis chat. */
@Composable
fun RowScope.HeaderAccountActions() {
    val s by Store.state.collectAsState()
    HeaderIconButton(Ic.Family, "Famiglia", badge = s.family != null) { Nav.go(Screen.Family) }
    GapW(8.dp)
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val requesters = androidx.compose.runtime.remember(date, meals.map { it.slotIndex }) {
        meals.associate { it.slotIndex to androidx.compose.foundation.relocation.BringIntoViewRequester() }
    }
    Text("I pasti di oggi", style = T.Title.copy(fontSize = 23.sp))
    GapH(14.dp)
    Row(Modifier.fillMaxWidth().testTag("plan-slot-strip").horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        meals.forEach { meal ->
            val selected = meal.slotIndex == selectedSlot
            val shape = RoundedCornerShape(24.dp)
            Row(Modifier.heightIn(min = 44.dp).testTag("plan-slot-${meal.diaryMeal}").clip(shape)
                .background(if (selected) C.Navy else Color.White).border(1.dp, if (selected) C.Navy else C.Line, shape)
                .selectable(selected, role = Role.Tab, onClick = {
                    selectedSlot = meal.slotIndex
                    scope.launch { requesters[meal.slotIndex]?.bringIntoView() }
                }).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AIcon(mealIcon(meal.diaryMeal), if (selected) Color.White else C.Navy, 20.dp); GapW(8.dp)
                Text(meal.slotName, style = T.Label.copy(color = if (selected) Color.White else C.Navy))
            }
        }
    }
    meals.forEach { meal ->
        GapH(16.dp)
        CompactPlanCard(s, date, meal, Modifier.bringIntoViewRequester(requesters.getValue(meal.slotIndex)))
    }
}

private fun mealIcon(id: Int) = when (id) { 0 -> Ic.MealBreakfast; 1 -> Ic.MealLunch; 2, 5 -> Ic.MealDinner; else -> Ic.MealSnack }

/** Native recipe, alternatives and diary actions in the image-first meal card. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactPlanCard(s: AppState, date: LocalDate, m: PlannedMeal, headingModifier: Modifier = Modifier) {
    val r = m.solved
    val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
    val expanded = LocalDensity.current.fontScale > 1.2f || LocalConfiguration.current.screenWidthDp < 360
    JCard(Modifier.testTag("plan-card-${m.diaryMeal}"), padding = PaddingValues(0.dp)) {
        FlowRow(headingModifier.fillMaxWidth().testTag("plan-meal-header-${m.diaryMeal}").padding(start = 18.dp, end = 18.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
            Row((if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1f)).clickable {
                Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal))
            }, verticalAlignment = Alignment.Top) {
                TonalIcon(mealIcon(m.diaryMeal), Color.White, C.AccentDeep, box = 34.dp, radius = 17.dp)
                GapW(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(m.slotName, style = T.Small.copy(fontSize = 11.sp))
                    Text(r.template.name, modifier = Modifier.testTag("plan-recipe-title-${m.diaryMeal}"),
                        style = T.Title2.copy(fontSize = 22.sp, lineHeight = 27.sp))
                }
            }
            Text(if (eaten) "Mangiato" else "Da mangiare", modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .background(if (eaten) A.GreenSoft else C.SurfaceAlt).padding(horizontal = 10.dp, vertical = 5.dp), style = T.Small.copy(color = if (eaten) A.GreenInk else C.Muted))
        }
        Column(Modifier.fillMaxWidth().testTag("plan-recipe-${m.diaryMeal}").clickable(role = Role.Button, onClickLabel = "Apri ricetta") {
            Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal))
        }, horizontalAlignment = Alignment.CenterHorizontally) {
            GapH(10.dp)
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.fillMaxWidth(.86f).aspectRatio(1.5f).testTag("plan-photo-${m.diaryMeal}"), recEmoji(r.template.meal), 0.dp, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        }
        FlowRow(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIcon(Ic.Clock, C.Navy, 24.dp); GapW(10.dp)
                Column {
                    Text("${r.template.minutes} min", style = T.Title2)
                    Text(com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }, style = T.Small)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIcon(Ic.Bowl, C.AccentDeep, 24.dp); GapW(10.dp)
                Text("${kcalFmt(r.total.kcal)} kcal", style = T.Title2.copy(fontSize = 21.sp))
            }
        }
        FlowRow(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Bg).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("P ${r.total.protein.r0()} g", style = T.Label.copy(fontSize = 15.sp, color = A.ProteinInk))
            Text("C ${r.total.carbs.r0()} g", style = T.Label.copy(fontSize = 15.sp, color = A.CarbInk))
            Text("G ${r.total.fat.r0()} g", style = T.Label.copy(fontSize = 15.sp, color = A.FatInk))
        }
        FlowRow(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
            CompactPlanAction("Alternative", Ic.Swap, C.Navy, (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1f)).testTag("plan-alternatives-${m.diaryMeal}")) { Nav.go(Screen.Alternatives(date, m.slotIndex)) }
            CompactPlanAction(if (eaten) "Mangiato" else "Segna mangiato", Ic.Check, if (eaten) A.GreenInk else Color.White,
                (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1.4f)).testTag("plan-eaten-${m.diaryMeal}"), primary = true, completed = eaten) {
                if (eaten) Store.uneatPlanned(date, m.slotIndex) else { Store.eatPlanned(date, m); Store.consumeFromPantry(r) }
            }
        }
    }
}

@Composable
private fun CompactPlanAction(text: String, icon: String, color: Color, modifier: Modifier, primary: Boolean = false, completed: Boolean = false, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(if (primary) { if (completed) A.GreenSoft else C.Navy } else C.Surface).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        AIcon(icon, color, 18.dp); GapW(8.dp)
        Text(text, style = T.Label.copy(color = color, fontSize = 13.sp), modifier = Modifier.weight(1f, fill = false))
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
        Column(Modifier.weight(1f).fillMaxWidth().background(C.Bg).testTag("main-scroll").verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)) {
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
