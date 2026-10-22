package com.jarvis.food.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.jarvis.food.engine.formatAmount
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.DietPrefs
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Micros
import com.jarvis.food.engine.PlannedMeal
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.SolvedRecipe
import com.jarvis.food.engine.Solver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private fun mealEmoji(meal: String) = when (meal) { "colazione" -> "☕"; "pranzo" -> "🍝"; "cena" -> "🍽️"; else -> "🍎" }

@Composable
fun PlanScreen() {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val date = Nav.planDate
    val monday = date.with(DayOfWeek.MONDAY)
    val hasWeek = (0..6).any { Store.dayPlan(s, monday.plusDays(it.toLong())) != null }
    val meals = Store.plannedMeals(s, date)
    val energy = Store.energyFor(s, date)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Il tuo piano", style = T.H1)
        Text("Grammature calcolate su calorie e macro del tuo obiettivo", style = T.Muted.copy(fontSize = 14.sp, lineHeight = 20.sp), modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(if (hasWeek) "Rigenera settimana" else "Crea piano") {
                Store.generatePlan(monday); Toast.makeText(ctx, "Piano della settimana creato", Toast.LENGTH_SHORT).show()
            }
            GhostButton("Preferenze") { Nav.go(Screen.DietSettings) }
            GhostButton("Lista della spesa") {
                if (!hasWeek) Store.generatePlan(monday)
                Store.buildShopping(maxOf(LocalDate.now(), monday), 7 - (maxOf(LocalDate.now(), monday).toEpochDay() - monday.toEpochDay()).toInt())
                Nav.tab = 2; Nav.kitchenTab = 1
            }
        }
        Spacer(Modifier.height(16.dp))
        WeekStrip(s, date) { Nav.planDate = it }

        if (!hasWeek) {
            Spacer(Modifier.height(18.dp))
            ACard(radius = 34.dp, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFEDF1F5))), glow = Color(0x99FF963C), padding = 20.dp) {
                Column {
                    Text("Crea la tua dieta personalizzata", style = T.Title)
                    Spacer(Modifier.height(6.dp))
                    Text("Jarvis sceglie piatti vari per ogni pasto e calcola le grammature di ogni ingrediente per rispettare " +
                            "le tue ${energy.baseKcal.r0()} kcal e i macronutrienti. Puoi cambiare qualsiasi piatto con un'alternativa.", style = T.Read)
                    Spacer(Modifier.height(16.dp))
                    APrimary("Crea il piano della settimana", Modifier.fillMaxWidth()) { Store.generatePlan(monday) }
                }
            }
            return@Column
        }

        Spacer(Modifier.height(18.dp))
        DayTotalsCard(s, date, meals)
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            AIcon(Ic.Swap, A.Muted, 16.dp, 2f)
            Text("  Tocca un'alternativa per cambiare il piatto", style = T.Muted.copy(fontSize = 13.sp))
        }
        Spacer(Modifier.height(14.dp))
        if (meals.isEmpty()) EmptyState("📅", "Nessun piatto per questo giorno", "Premi \"Rigenera settimana\" per completare il piano.")
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) { meals.forEach { m -> MealPlanCard(s, date, m) } }
        Spacer(Modifier.height(18.dp))
        Text("Valori calcolati dal database USDA FoodData Central su pesi a crudo. Le foto sono illustrative.", style = T.Small)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun DayTotalsCard(s: AppState, date: LocalDate, meals: List<PlannedMeal>) {
    val e = Store.energyFor(s, date)
    val tot = meals.fold(com.jarvis.food.engine.Macro.ZERO) { a, m -> a + m.solved.total }
    ACard(radius = 34.dp, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFEDF1F5))), glow = Color(0x99FF963C), padding = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 12.dp)) {
            Text("Totale con le tue scelte", style = T.Muted.copy(fontWeight = FontWeight.Medium))
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Bottom) {
                Text(kcalFmt(tot.kcal), style = T.Hero.copy(fontSize = 54.sp, lineHeight = 56.sp))
                Text("  kcal", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 7.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xCCFFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Obiettivo ${kcalFmt(e.targetKcal)}", style = T.Small.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp))
                }
            }
            if (e.extraKcal > 0) Text("+${e.extraKcal.r0()} kcal dall'attività", style = T.Small.copy(color = A.GreenInk, fontWeight = FontWeight.SemiBold))
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xCCFFFFFF))) {
                meals.forEach { m -> Box(Modifier.weight(m.solved.total.kcal.toFloat().coerceAtLeast(1f)).height(16.dp).background(MEAL_DOT[m.diaryMeal] ?: C.Accent)) }
                val rest = (e.targetKcal - tot.kcal).toFloat()
                if (rest > 0f) Box(Modifier.weight(rest).height(16.dp))
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                meals.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(MEAL_DOT[m.diaryMeal] ?: C.Accent))
                        Text("  ${MEAL_LABEL[m.diaryMeal] ?: m.slotName}", style = T.Small.copy(fontSize = 11.5.sp), maxLines = 1)
                    }
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalCol("Proteine", tot.protein, e.protein, C.Protein, A.ProteinSoft, Modifier.weight(1f))
                TotalCol("Carboidrati", tot.carbs, e.carbs, C.Blue, A.CarbSoft, Modifier.weight(1f))
                TotalCol("Grassi", tot.fat, e.fat, C.Gold, A.FatSoft, Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 6.dp).height(44.dp).clickable { Nav.go(Screen.EnergyInfo) }, verticalAlignment = Alignment.CenterVertically) {
                Text("Come è calcolato", style = T.Link.copy(color = A.Link))
                AIcon(Ic.Right, A.Link, 16.dp, 2.4f)
            }
        }
    }
}

@Composable
private fun TotalCol(label: String, v: Double, goal: Double, color: Color, soft: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xD1FFFFFF)).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(v.r0(), style = T.Num.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold))
            Text(" g", style = T.Small.copy(fontSize = 11.5.sp), modifier = Modifier.padding(bottom = 3.dp))
        }
        Text("$label · ${goal.r0()}", style = T.Small.copy(fontSize = 11.5.sp), maxLines = 1, softWrap = false)
        ABar((if (goal > 0) v / goal else 0.0).toFloat(), Brush.horizontalGradient(listOf(color, color)), soft, 6.dp, Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPlanCard(s: AppState, date: LocalDate, m: PlannedMeal) {
    val db = Db.db
    val r = m.solved
    val slot = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }
    val eaten = slot?.eatenEntryIds?.isNotEmpty() == true
    val alts = remember(r.template.id, r.target, s.dietPrefs) { Planner.alternatives(db, r.template.meal, r.target, s.dietPrefs, emptySet()) }
    val idx = alts.indexOfFirst { it.template.id == r.template.id }
    val dot = MEAL_DOT[m.diaryMeal] ?: C.Accent
    val shape = RoundedCornerShape(34.dp)
    Column(Modifier.fillMaxWidth().aShadow(shape, 12.dp, Color(0x731D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape)) {
        Box(Modifier.fillMaxWidth().height(230.dp).clickable { openRecipe(date, m) }) {
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id], Modifier.fillMaxSize(), mealEmoji(r.template.meal), 0.dp)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0x8C181430))))
            Row(Modifier.padding(14.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xEDFFFFFF)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                Text("  ${MEAL_LABEL[m.diaryMeal] ?: m.slotName} · ${if (idx <= 0) "Proposto" else "Alternativa $idx"}", style = T.Chip.copy(color = A.Ink), maxLines = 1)
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(14.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xEDFFFFFF)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                AIcon(Ic.Clock, A.Ink, 14.dp, 2.2f)
                Text("  ${r.template.minutes} min", style = T.Chip.copy(color = A.Ink))
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(r.template.name, style = T.Dish.copy(fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f).clickable { openRecipe(date, m) })
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(r.total.kcal.r0(), style = T.Title.copy(fontSize = 28.sp, lineHeight = 30.sp))
                    Text("kcal", style = T.Small.copy(fontSize = 11.5.sp))
                }
            }
            Box(Modifier.padding(top = 12.dp)) { MacroChips(r.total.protein, r.total.carbs, r.total.fat) }

            Text("ALTERNATIVE", style = T.Eyebrow.copy(letterSpacing = 0.3.sp), modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()).padding(vertical = 6.dp, horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                alts.take(12).forEach { a ->
                    val sel = a.template.id == r.template.id
                    val ringShape = RoundedCornerShape(24.dp)
                    Box(
                        Modifier.size(72.dp).then(if (sel) Modifier.border(3.dp, dot, ringShape) else Modifier).padding(if (sel) 3.dp else 0.dp)
                            .clip(ringShape).clickable(enabled = !eaten) { Store.setSlotRecipe(date, m.slotIndex, a.template.id) }
                    ) {
                        RecipeImage(a.template.id, a.template.image, s.recipePhotos[a.template.id], Modifier.fillMaxSize().let { if (sel) it else it.alpha(0.62f) }, mealEmoji(a.template.meal), 24.dp)
                    }
                }
            }
            Text("Vedi tutte le alternative", style = T.Link, modifier = Modifier.padding(top = 2.dp).clickable { Nav.go(Screen.Alternatives(date, m.slotIndex)) }.padding(vertical = 8.dp))

            Text("INGREDIENTI E GRAMMATURE", style = T.Eyebrow.copy(letterSpacing = 0.3.sp), modifier = Modifier.padding(top = 10.dp))
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.items.filter { it.ing.role != "x" }.forEach { it2 ->
                    Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFF6EC)).padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it2.displayName().replaceFirstChar { c -> c.uppercase() }, style = T.Body.copy(fontSize = 13.sp), maxLines = 1)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(formatAmount(it2.food, it2.grams, short = true), style = T.Label.copy(fontSize = 13.sp), maxLines = 1)
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(C.Navy).clickable { openRecipe(date, m) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Apri ricetta", style = T.BtnM.copy(color = Color.White))
                    Spacer(Modifier.width(8.dp))
                    AIcon(Ic.Right, Color.White, 16.dp, 2.4f)
                }
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (eaten) A.GreenSoft else A.Tonal).clickable {
                    if (eaten) Store.uneatPlanned(date, m.slotIndex) else { Store.eatPlanned(date, m); Store.consumeFromPantry(r) }
                }, contentAlignment = Alignment.Center) {
                    Text(if (eaten) "Mangiato ✓" else "Aggiungi al diario", style = T.BtnM.copy(color = if (eaten) A.GreenInk else Color(0xFF8F2A14)), maxLines = 1)
                }
            }
        }
    }
}

private fun openRecipe(date: LocalDate, m: PlannedMeal) =
    Nav.go(Screen.RecipeView(m.solved.template.id, m.solved.target, date, m.slotIndex, m.diaryMeal))

@Composable
fun AlternativesScreen(date: LocalDate, slot: Int) {
    val s by Store.state.collectAsState()
    val m = Store.plannedMeals(s, date).firstOrNull { it.slotIndex == slot }
    Column(Modifier.fillMaxSize()) {
        TopBar("Trova alternativa", onBack = { Nav.back() })
        if (m == null) { EmptyState("🤔", "Pasto non trovato", ""); return@Column }
        val alts = remember(m.solved.target, s.dietPrefs) { Planner.alternatives(Db.db, m.solved.template.meal, m.solved.target, s.dietPrefs) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("${m.slotName}: obiettivo ${m.solved.target.kcal.r0()} kcal · P ${m.solved.target.protein.r0()} g · C ${m.solved.target.carbs.r0()} g · G ${m.solved.target.fat.r0()} g", style = T.Muted)
            Text("Ogni piatto è ricalcolato per avvicinarsi il più possibile a questi valori. In alto i più precisi.", style = T.Small)
            Spacer(Modifier.height(10.dp))
            alts.forEach { a ->
                AltRow(s, a, a.template.id == m.solved.template.id) {
                    Store.setSlotRecipe(date, slot, a.template.id); Nav.back()
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AltRow(s: AppState, a: SolvedRecipe, current: Boolean, onPick: () -> Unit) {
    JCard(padding = PaddingValues(12.dp), onClick = onPick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecipeImage(a.template.id, a.template.image, s.recipePhotos[a.template.id], Modifier.size(76.dp), mealEmoji(a.template.meal), 14.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.template.name, style = T.Title2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${a.total.kcal.r0()} kcal · P ${a.total.protein.r0()} · C ${a.total.carbs.r0()} · G ${a.total.fat.r0()}", style = T.Small)
                Text(a.shortList(), style = T.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (current) Tag("attuale", C.Green)
            else Tag("${if (a.kcalErrorPct >= 0) "+" else ""}${a.kcalErrorPct.r0()}%", if (kotlin.math.abs(a.kcalErrorPct) <= 5) C.Green else C.Gold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietSettingsScreen() {
    val s by Store.state.collectAsState()
    var p by remember { mutableStateOf(s.dietPrefs) }
    val shares = remember { mutableStateMapOf<Int, Int>().apply { p.slots.forEachIndexed { i, sl -> put(i, sl.share) } } }
    val tags = listOf("lattosio" to "Senza lattosio", "glutine" to "Senza glutine", "frutta_secca" to "Frutta a guscio", "uova" to "Uova", "pesce" to "Pesce",
        "crostacei" to "Crostacei", "carne" to "Carne", "maiale" to "Maiale", "soia" to "Soia")
    // Preset nell'ordine dei pasti dell'app: colazione, pranzo, merenda, cena.
    val splits = listOf(
        "Classica · 25 / 35 / 10 / 30" to listOf(25, 35, 10, 30),
        "Cena leggera · 25 / 40 / 10 / 25" to listOf(25, 40, 10, 25),
        "Pranzo forte · 20 / 45 / 5 / 30" to listOf(20, 45, 5, 30)
    )
    val kcal = Store.energyFor(s, LocalDate.now()).baseKcal
    val cur = p.slots.indices.map { shares[it] ?: p.slots[it].share }
    val selSplit = splits.indexOfFirst { it.second.size == cur.size && it.second == cur }
    val mealColors = listOf(Color(0xFFFFB820), Color(0xFFFF6B4A), Color(0xFF8B5CF6), Color(0xFF3B82F6))
    val sum = cur.sum().coerceAtLeast(1)

    Column(Modifier.fillMaxSize()) {
        TopBar("Preferenze della dieta", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            GHead("Preferenze della dieta", "Gli alimenti esclusi non compaiono nel piano né nei suggerimenti.")
            ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                Column {
                    Text("Esclusioni", style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.padding(bottom = 10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { (k, l) ->
                            AChip(l, k in p.exclude) { p = p.copy(exclude = if (k in p.exclude) p.exclude - k else p.exclude + k) }
                        }
                    }
                    if ("lattosio" in p.exclude) Text("Con \"Senza lattosio\" i piatti usano latte delattosato e formaggi stagionati come il Parmigiano.",
                        style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 10.dp))
                }
            }

            GHead("Ripartizione dei pasti")
            ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                Column {
                    ASegment(splits.map { it.first }, selSplit, vertical = true) { i -> splits[i].second.forEachIndexed { j, v -> if (j < p.slots.size) shares[j] = v } }
                    Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        cur.forEachIndexed { i, v -> Box(Modifier.weight(v.coerceAtLeast(1).toFloat()).height(14.dp).clip(RoundedCornerShape(7.dp)).background(mealColors[i % 4])) }
                    }
                    p.slots.forEachIndexed { i, sl ->
                        val share = cur[i]
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(mealColors[i % 4]))
                            GapW(10.dp)
                            Text(sl.name, style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = A.Ink), modifier = Modifier.weight(1f))
                            Text("$share%", style = T.Small.copy(fontSize = 13.sp, color = A.Muted))
                            Text("${kcalFmt(kcal * share / sum)} kcal", style = T.H1.copy(fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
                                modifier = Modifier.width(92.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1)
                        }
                        if (i < p.slots.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                    }
                    if (sum != 100) Text("Totale $sum%: verrà riportato in proporzione a 100%.", style = T.Small.copy(fontSize = 12.5.sp, color = A.FatInk))
                }
            }

            GHead("Tempo massimo di preparazione")
            ASegment(listOf(15, 25, 40).map { "$it min" }, listOf(15, 25, 40).indexOf(p.maxMinutes)) { i -> p = p.copy(maxMinutes = listOf(15, 25, 40)[i]) }

            if (p.disliked.isNotEmpty()) {
                GHead("Piatti nascosti")
                ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.disliked.forEach { id ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Db.db.recipe(id)?.name ?: id, style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = A.Ink), modifier = Modifier.weight(1f))
                                Text("Ripristina", style = T.Link, modifier = Modifier.clickable { p = p.copy(disliked = p.disliked - id) }.padding(8.dp))
                            }
                        }
                    }
                }
            }
            GapH(16.dp)
        }
        GSave("Salva e ricalcola il piano") {
            val np = p.copy(slots = p.slots.mapIndexed { i, sl -> sl.copy(share = shares[i] ?: sl.share) })
            Store.setDietPrefs(np)
            Store.generatePlan(maxOf(LocalDate.now(), Nav.planDate.with(DayOfWeek.MONDAY)),
                7 - (maxOf(LocalDate.now(), Nav.planDate.with(DayOfWeek.MONDAY)).toEpochDay() - Nav.planDate.with(DayOfWeek.MONDAY).toEpochDay()).toInt().coerceIn(0, 6))
            Nav.back()
        }
    }
}

/** Spiegazione di come è calcolato l'obiettivo del giorno. */
@Composable
fun EnergyInfoScreen() {
    val s by Store.state.collectAsState()
    val e = Store.energyFor(s, LocalDate.now())
    Column(Modifier.fillMaxSize()) {
        TopBar("Come calcolo il tuo obiettivo", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 120.dp)) {
            val hero = RoundedCornerShape(34.dp)
            Column(
                Modifier.padding(top = 4.dp).fillMaxWidth().aShadow(hero, 14.dp, Color(0x99E88C60)).clip(hero)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFE9DC), Color(0xFFFFF5E6)))).border(1.dp, Color(0xE6FFFFFF), hero).padding(22.dp)
            ) {
                Text("OGGI", style = T.Eyebrow.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB4381E)))
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(kcalFmt(e.targetKcal), style = T.H1.copy(fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-1.4).sp))
                    Text(" kcal", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(bottom = 8.dp))
                }
                GapH(14.dp)
                MacroChips(e.protein, e.carbs, e.fat)
            }
            ACard(Modifier.padding(top = 14.dp).fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Come ci siamo arrivati", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = A.Ink))
                    e.explanation.forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(Color(0xFFF28B7A)))
                            GapW(12.dp)
                            Text(it, style = T.Body.copy(fontSize = 14.5.sp, lineHeight = 21.sp, color = Color(0xFF3B3A5C)))
                        }
                    }
                }
            }
            Text("L'obiettivo parte dal tuo fabbisogno di mantenimento (profilo e questionario) più l'obiettivo di peso, e non scende mai " +
                    "perché al mattino hai fatto pochi passi. Lo smartwatch aggiunge calorie solo quando la spesa misurata supera quella " +
                    "attesa, e dopo almeno 5 giorni di dati affina il fabbisogno sui tuoi valori reali. " +
                    "Metabolismo basale: Mifflin-St Jeor, o Katch-McArdle se indichi la massa grassa. " +
                    "Proteine: ${"%.1f".format(java.util.Locale.ITALY, com.jarvis.food.data.Nutrition.proteinPerKg(s.profile))} g per kg. " +
                    "Grassi: ${s.goals.fatPct}% delle calorie di base. Sono stime: se il peso non va nella direzione attesa per 2-3 settimane, " +
                    "correggi l'obiettivo dal Profilo.", style = T.Small.copy(fontSize = 12.5.sp, lineHeight = 18.sp, color = A.Muted),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp))
            GapH(14.dp)
            AInk("Modalità di calcolo dell'attività", Modifier.fillMaxWidth(), height = 54.dp) { Nav.go(Screen.Watch) }
        }
    }
}

/** Dettaglio micro del piatto: le voci principali in percentuale del riferimento giornaliero. */
@Composable
fun DishMicros(r: SolvedRecipe, male: Boolean) {
    val keys = listOf("vitD", "fiber", "vitC", "vitA", "folate", "vitB12", "calcium", "iron", "magnesium", "potassium", "zinc")
    val rows = keys.mapNotNull { Micros.byKey[it] }.map { d ->
        val ref = if (male) d.men else d.women
        d to (if (ref > 0) r.micro(d.key) / ref * 100 else 0.0)
    }.sortedByDescending { it.second }.take(8)
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        rows.forEach { (d, pct) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.name, style = T.Body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.width(92.dp), maxLines = 1)
                GapW(12.dp)
                ABar((pct / 100).toFloat(), Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF5AA9FF))), A.CarbSoft, 10.dp, Modifier.weight(1f))
                GapW(12.dp)
                Text("${pct.toInt()}%", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.CarbInk), modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

/** Per ordinare le alternative a parità: usato anche in Cucina. */
fun fitLabel(r: SolvedRecipe): String = when {
    Solver.fitScore(r) < 0.01 -> "precisione ottima"
    Solver.fitScore(r) < 0.05 -> "precisione buona"
    else -> "precisione indicativa"
}

@Suppress("unused") private fun prefsSummary(p: DietPrefs) = p.exclude.joinToString()
