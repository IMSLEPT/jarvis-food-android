package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
        FlowRow(Modifier.fillMaxWidth().testTag("main-header").padding(top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, modifier = Modifier.testTag("main-header-title"), style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp))
            Row(verticalAlignment = Alignment.CenterVertically) { HeaderAccountActions() }
        }
        return
    }
    val separateGreeting = eyebrow != null && homeName == null && LocalDensity.current.fontScale > 1.2f
    Column(Modifier.fillMaxWidth().testTag("main-header").padding(top = 2.dp, bottom = 8.dp)) {
        if (separateGreeting) {
            HeaderGreeting(eyebrow!!)
            GapH(4.dp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            if (eyebrow != null && !separateGreeting) HeaderGreeting(eyebrow)
            if (homeName != null && !compact) {
                Text(homeName, style = T.Display.copy(fontSize = 28.sp, lineHeight = 32.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(title, style = T.Body.copy(color = C.Muted), maxLines = 2)
            } else {
                Text(title, style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp), maxLines = if (LocalDensity.current.fontScale > 1.2f) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
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
        style = T.Body.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = C.Navy),
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** The approved meal journey. The timeline never steals reading width at large font sizes. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PlanJourney(s: AppState, date: LocalDate, meals: List<PlannedMeal>) {
    val expanded = LocalDensity.current.fontScale > 1.2f || LocalConfiguration.current.screenWidthDp < 360
    Column(Modifier.fillMaxWidth()) {
        meals.forEachIndexed { index, meal ->
            val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == meal.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
            val icon = mealSymbol(meal.diaryMeal)
            val marker: @Composable () -> Unit = {
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (eaten) A.GreenInk else C.AccentDeep), contentAlignment = Alignment.Center) {
                    AIcon(if (eaten) Ic.Check else icon, Color.White, 21.dp, 1.8f)
                }
            }
            Box(Modifier.fillMaxWidth()) {
                if (!expanded && index < meals.lastIndex) Canvas(Modifier.matchParentSize()) {
                    drawLine(C.Line, Offset(18.dp.toPx(), 36.dp.toPx()), Offset(18.dp.toPx(), size.height), strokeWidth = 1.5.dp.toPx())
                }
                Row(Modifier.fillMaxWidth().padding(bottom = if (index < meals.lastIndex) 12.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!expanded) marker()
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (expanded) marker()
                            FlowRow(Modifier.weight(1f).testTag("plan-meal-header-${meal.diaryMeal}"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(MEAL_LABEL[meal.diaryMeal] ?: meal.slotName, style = T.Title2.copy(fontSize = 18.sp, lineHeight = 24.sp))
                                Text(if (eaten) "Mangiato" else "Da mangiare", modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (eaten) A.GreenSoft else C.SurfaceAlt).padding(horizontal = 10.dp, vertical = 4.dp), style = T.Small.copy(fontSize = 12.sp, color = if (eaten) A.GreenInk else C.Muted))
                            }
                        }
                        GapH(8.dp)
                        CompactPlanCard(s, date, meal)
                    }
                }
            }
        }
    }
}

/** Preserve the existing recipe, alternatives and diary actions inside the journey card. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactPlanCard(s: AppState, date: LocalDate, m: PlannedMeal) {
    val r = m.solved
    val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
    JCard(Modifier.testTag("plan-card-${m.diaryMeal}"), padding = PaddingValues(0.dp)) {
        val recipeModifier = Modifier.fillMaxWidth().testTag("plan-recipe-${m.diaryMeal}").clickable(role = Role.Button, onClickLabel = "Apri ricetta") { Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal)) }.padding(14.dp)
        if (LocalDensity.current.fontScale > 1.2f || LocalConfiguration.current.screenWidthDp < 360) {
            Column(recipeModifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.size(76.dp), recEmoji(r.template.meal), 18.dp)
                    GapW(12.dp)
                    Text("${r.template.minutes} MIN · ${com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }}", style = T.Eyebrow.copy(fontSize = 12.sp, letterSpacing = 0.sp), modifier = Modifier.weight(1f))
                    AIcon(Ic.Right, C.Navy, 18.dp)
                }
                GapH(12.dp)
                Text(r.template.name, modifier = Modifier.testTag("plan-recipe-title-${m.diaryMeal}"), style = T.Title2.copy(fontSize = 18.sp, lineHeight = 24.sp))
                GapH(4.dp)
                Text("${kcalFmt(r.total.kcal)} kcal", style = T.Label.copy(fontSize = 17.sp, color = C.Navy))
            }
        } else {
        Row(recipeModifier, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(r.template.name, modifier = Modifier.testTag("plan-recipe-title-${m.diaryMeal}"), style = T.Title2.copy(fontSize = 18.sp, lineHeight = 23.sp))
                GapH(8.dp)
                Text("${kcalFmt(r.total.kcal)} kcal", style = T.Label.copy(fontSize = 17.sp, color = C.Navy))
                GapH(2.dp)
                Text("${r.template.minutes} min · ${com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }}", style = T.Small.copy(fontSize = 12.sp))
            }
            GapW(12.dp)
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.size(92.dp), recEmoji(r.template.meal), 16.dp)
        }
        }
        FlowRow(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("P ${r.total.protein.r0()} g", style = T.Label.copy(fontSize = 13.sp, color = A.ProteinInk))
            Text("C ${r.total.carbs.r0()} g", style = T.Label.copy(fontSize = 13.sp, color = A.CarbInk))
            Text("G ${r.total.fat.r0()} g", style = T.Label.copy(fontSize = 13.sp, color = A.FatInk))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
        val expanded = LocalDensity.current.fontScale > 1.2f || LocalConfiguration.current.screenWidthDp < 360
        FlowRow(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = if (expanded) 1 else 2) {
            CompactPlanAction("Alternative", Ic.Swap, C.Navy, (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1f)).testTag("plan-alternatives-${m.diaryMeal}")) { Nav.go(Screen.Alternatives(date, m.slotIndex)) }
            CompactPlanAction(if (eaten) "Mangiato" else "Segna mangiato", Ic.Check, if (eaten) A.GreenInk else Color.White, (if (expanded) Modifier.fillMaxWidth() else Modifier.weight(1.4f)).testTag("plan-eaten-${m.diaryMeal}"), primary = true, completed = eaten) {
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
fun MainPage(title: String, eyebrow: String? = null, resetKey: Any? = null, homeName: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    val threshold = with(LocalDensity.current) { 64.dp.roundToPx() }
    val compact by androidx.compose.runtime.remember(scroll, threshold) { derivedStateOf { scroll.value > threshold } }
    androidx.compose.runtime.LaunchedEffect(resetKey) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        val expanded = LocalDensity.current.fontScale > 1.5f
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) { MainHeader(if (expanded && homeName != null) "Oggi" else title, if (expanded) null else eyebrow, if (expanded) null else homeName, compact) }
        Column(Modifier.weight(1f).fillMaxWidth().testTag("main-scroll").verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp)) {
            if (expanded && homeName != null) {
                if (eyebrow != null) HeaderGreeting(eyebrow)
                Text(homeName, modifier = Modifier.testTag("home-name"), style = T.Display.copy(fontSize = 30.sp, lineHeight = 34.sp))
                Text(title, style = T.Body.copy(color = C.Muted))
                GapH(12.dp)
            }
            content()
        }
    }
}
