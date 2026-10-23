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
fun MainHeader(title: String, eyebrow: String? = null) {
    val s by Store.state.collectAsState()
    Row(Modifier.fillMaxWidth().testTag("main-header").padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            if (eyebrow != null) Text(eyebrow, modifier = Modifier.testTag("home-greeting"),
                style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = C.Navy),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(title, style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White).border(1.dp, C.Line, CircleShape)
            .semantics { contentDescription = "Famiglia" }.clickable { Nav.go(Screen.Family) }, contentAlignment = Alignment.Center) {
            AIcon(Ic.Family, A.Ink, 22.dp)
            if (s.family != null) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(7.dp).clip(CircleShape).background(C.Green))
        }
        GapW(8.dp)
        Avatar(s.profile.name, s.profile.photo, 48.dp, Modifier.semantics { contentDescription = "Apri profilo" }.clickable { Nav.go(Screen.Profile) })
    }
}

/** Same native plan actions and calculations, in the compact card approved in the prototype. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactPlanCard(s: AppState, date: LocalDate, m: PlannedMeal) {
    val r = m.solved
    val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
    JCard(padding = PaddingValues(0.dp)) {
        FlowRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroChip(MEAL_LABEL[m.diaryMeal] ?: m.slotName, C.NavySoft, C.Navy)
            MacroChip(if (eaten) "Mangiato" else "Da mangiare", if (eaten) A.GreenSoft else A.Tonal, if (eaten) A.GreenInk else A.TonalInk)
        }
        Row(Modifier.fillMaxWidth().clickable { Nav.go(Screen.RecipeView(r.template.id, r.target, date, m.slotIndex, m.diaryMeal)) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.size(76.dp), recEmoji(r.template.meal), 18.dp)
            GapW(12.dp)
            Column(Modifier.weight(1f)) {
                Text("${r.template.minutes} MIN · ${com.jarvis.food.engine.RecipeMeta.DIFFICULTY.getOrElse(r.template.difficulty) { "" }}", style = T.Eyebrow.copy(fontSize = 11.sp, letterSpacing = 0.sp))
                Text(r.template.name, style = T.Title2.copy(fontSize = 16.sp))
                GapH(4.dp)
                Text("${kcalFmt(r.total.kcal)} kcal · P ${r.total.protein.r0()} g", style = T.Small)
            }
            AIcon(Ic.Right, C.Navy, 18.dp)
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) { MacroChips(r.total.protein, r.total.carbs, r.total.fat) }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactPlanAction("Alternative", Ic.Swap, C.Navy, Modifier.weight(1f)) { Nav.go(Screen.Alternatives(date, m.slotIndex)) }
            CompactPlanAction(if (eaten) "Mangiato ✓" else "Segna mangiato", Ic.Check, if (eaten) A.GreenInk else A.TonalInk, Modifier.weight(1f)) {
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
fun MainPage(title: String, eyebrow: String? = null, resetKey: Any? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(resetKey) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) { MainHeader(title, eyebrow) }
        Column(Modifier.weight(1f).fillMaxWidth().testTag("main-scroll").verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp), content = content)
    }
}
