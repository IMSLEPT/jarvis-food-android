package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
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
fun MainHeader(title: String, eyebrow: String? = null, homeName: String? = null) {
    val s by Store.state.collectAsState()
    val separateGreeting = eyebrow != null && homeName == null && LocalDensity.current.fontScale > 1.2f
    Column(Modifier.fillMaxWidth().testTag("main-header").padding(top = 4.dp, bottom = 12.dp)) {
        if (separateGreeting) {
            HeaderGreeting(eyebrow!!)
            GapH(4.dp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            if (eyebrow != null && !separateGreeting) HeaderGreeting(eyebrow)
            if (homeName != null) {
                Text(homeName, style = T.Display.copy(fontSize = 30.sp, lineHeight = 34.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(title, style = T.Body.copy(color = C.Muted), maxLines = 2)
            } else {
                Text(title, style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        HeaderIconButton(Ic.Family, "Famiglia", badge = s.family != null) { Nav.go(Screen.Family) }
        GapW(8.dp)
        HeaderControl("Apri profilo", onClick = { Nav.go(Screen.Profile) }) {
            Avatar(s.profile.name, s.profile.photo, 42.dp, shape = RoundedCornerShape(12.dp))
        }
        }
    }
}

@Composable
private fun HeaderGreeting(text: String) {
    Text(text, modifier = Modifier.testTag("home-greeting"),
        style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = C.Navy),
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** Same native plan actions and calculations, in the compact card approved in the prototype. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactPlanCard(s: AppState, date: LocalDate, m: PlannedMeal) {
    val r = m.solved
    val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
    val warm = m.slotIndex % 2 == 1
    val accent = if (warm) C.Accent else C.Navy
    val soft = if (warm) C.AccentSoft else C.NavySoft
    JCard(Modifier.testTag("plan-card-${m.diaryMeal}"), padding = PaddingValues(0.dp)) {
        FlowRow(Modifier.fillMaxWidth().testTag("plan-meal-header-${m.diaryMeal}").background(soft).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                GapW(10.dp)
                Text(MEAL_LABEL[m.diaryMeal] ?: m.slotName,
                    style = T.Title2.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = C.Navy))
            }
            MacroChip(if (eaten) "Mangiato" else "Da mangiare", if (eaten) A.GreenSoft else C.Surface,
                if (eaten) A.GreenInk else C.Muted)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(accent.copy(alpha = 0.14f)))
        Row(Modifier.fillMaxWidth().testTag("plan-recipe-${m.diaryMeal}").clickable { Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal)) }.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.size(82.dp), recEmoji(r.template.meal), 18.dp)
            GapW(12.dp)
            Column(Modifier.weight(1f)) {
                Text("${r.template.minutes} MIN · ${com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }}", style = T.Eyebrow.copy(fontSize = 11.sp, letterSpacing = 0.sp))
                Text(r.template.name, style = T.Title2.copy(fontSize = 16.sp, lineHeight = 22.sp))
                GapH(4.dp)
                Text("${kcalFmt(r.total.kcal)} kcal", style = T.Label.copy(fontSize = 17.sp, color = C.Navy))
            }
            AIcon(Ic.Right, C.Navy, 18.dp)
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) { MacroChips(r.total.protein, r.total.carbs, r.total.fat) }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
        Row(Modifier.fillMaxWidth().background(C.Bg.copy(alpha = 0.7f)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactPlanAction("Alternative", Ic.Swap, C.Navy, Modifier.weight(1f).testTag("plan-alternatives-${m.diaryMeal}")) { Nav.go(Screen.Alternatives(date, m.slotIndex)) }
            CompactPlanAction(if (eaten) "Mangiato ✓" else "Segna mangiato", Ic.Check, if (eaten) A.GreenInk else A.TonalInk, Modifier.weight(1f).testTag("plan-eaten-${m.diaryMeal}")) {
                if (eaten) Store.uneatPlanned(date, m.slotIndex) else { Store.eatPlanned(date, m); Store.consumeFromPantry(r) }
            }
        }
    }
}

@Composable
private fun CompactPlanAction(text: String, icon: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        AIcon(icon, color, 18.dp); GapW(8.dp)
        Text(text, style = T.Label.copy(color = color, fontSize = 13.sp), modifier = Modifier.weight(1f, fill = false))
    }
}

/** The header and bottom navigation stay in place while app-owned content scrolls. */
@Composable
fun MainPage(title: String, eyebrow: String? = null, resetKey: Any? = null, homeName: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(resetKey) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) { MainHeader(title, eyebrow, homeName) }
        Column(Modifier.weight(1f).fillMaxWidth().testTag("main-scroll").verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp), content = content)
    }
}
