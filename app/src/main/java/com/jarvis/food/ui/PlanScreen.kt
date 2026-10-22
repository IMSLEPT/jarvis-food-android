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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Il tuo piano", style = T.Display)
        Text("Grammature calcolate su calorie e macro del tuo obiettivo", style = T.Muted)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(if (hasWeek) "↻ Rigenera settimana" else "Crea piano") {
                Store.generatePlan(monday); Toast.makeText(ctx, "Piano della settimana creato", Toast.LENGTH_SHORT).show()
            }
            GhostButton("Preferenze") { Nav.go(Screen.DietSettings) }
            GhostButton("🛒 Lista della spesa") {
                if (!hasWeek) Store.generatePlan(monday)
                Store.buildShopping(maxOf(LocalDate.now(), monday), 7 - (maxOf(LocalDate.now(), monday).toEpochDay() - monday.toEpochDay()).toInt())
                Nav.tab = 2; Nav.kitchenTab = 1
            }
        }
        Spacer(Modifier.height(14.dp))
        WeekPills(date, monday) { Nav.planDate = it }

        if (!hasWeek) {
            Spacer(Modifier.height(16.dp))
            JCard {
                Text("🥗", style = T.Big)
                Text("Crea la tua dieta personalizzata", style = T.Title)
                Spacer(Modifier.height(6.dp))
                Text("Jarvis sceglie piatti vari per ogni pasto e calcola le grammature di ogni ingrediente per rispettare " +
                        "le tue ${energy.baseKcal.r0()} kcal e i macronutrienti. Puoi cambiare qualsiasi piatto con un'alternativa.", style = T.Read)
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Crea il piano della settimana", Modifier.fillMaxWidth(), color = C.Accent) { Store.generatePlan(monday) }
            }
            return@Column
        }

        Spacer(Modifier.height(14.dp))
        DayTotalsCard(s, date, meals)
        Spacer(Modifier.height(16.dp))
        Text("Scorri le alternative di ogni piatto", style = T.Muted)
        Spacer(Modifier.height(8.dp))
        if (meals.isEmpty()) EmptyState("📅", "Nessun piatto per questo giorno", "Premi \"Rigenera settimana\" per completare il piano.")
        meals.forEach { m ->
            MealPlanCard(s, date, m)
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GhostButton("‹  Precedente") { Nav.planDate = date.minusDays(1) }
            PrimaryButton("Successivo  ›") { Nav.planDate = date.plusDays(1) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Valori calcolati dal database USDA FoodData Central su pesi a crudo. Le foto sono illustrative.", style = T.Small)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WeekPills(selected: LocalDate, monday: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).clickable { onSelect(selected.minusWeeks(1)) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Settimana precedente", tint = C.Muted)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0..6) {
                val d = monday.plusDays(i.toLong())
                val sel = d == selected
                val today = d == LocalDate.now()
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) C.Ink else C.Surface)
                        .border(1.dp, if (sel) C.Ink else if (today) C.Accent else C.Line, RoundedCornerShape(14.dp))
                        .clickable { onSelect(d) }.padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, IT).take(3).replaceFirstChar { it.uppercase() },
                        style = T.Small.copy(color = if (sel) C.OnAccent else C.Muted), maxLines = 1, softWrap = false)
                    Text(d.dayOfMonth.toString(), style = T.Label.copy(color = if (sel) C.OnAccent else C.Text), maxLines = 1, softWrap = false)
                }
            }
        }
        Box(Modifier.size(34.dp).clip(CircleShape).clickable { onSelect(selected.plusWeeks(1)) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Settimana successiva", tint = C.Muted)
        }
    }
}

@Composable
private fun DayTotalsCard(s: AppState, date: LocalDate, meals: List<PlannedMeal>) {
    val e = Store.energyFor(s, date)
    val tot = meals.fold(com.jarvis.food.engine.Macro.ZERO) { a, m -> a + m.solved.total }
    JCard(color = C.SurfaceAlt) {
        Row(verticalAlignment = Alignment.Top) {
            Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, IT).replaceFirstChar { it.uppercase() }, style = T.Title2, modifier = Modifier.weight(1f))
            Text("Totale con le tue scelte", style = T.Muted)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tot.kcal.r0(), style = T.Big)
            Spacer(Modifier.width(6.dp))
            Text("kcal", style = T.Title2.copy(color = C.Muted), modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            TotalCol("Proteine", tot.protein, e.protein, Modifier.weight(1f))
            TotalCol("Carboidrati", tot.carbs, e.carbs, Modifier.weight(1f))
            TotalCol("Grassi", tot.fat, e.fat, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { Nav.go(Screen.EnergyInfo) }) {
            Text("◎  Obiettivo: ${e.targetKcal.r0()} kcal", style = T.Muted)
            if (e.extraKcal > 0) { Spacer(Modifier.width(8.dp)); Tag("+${e.extraKcal.r0()} attività", C.Green) }
        }
        Text("Come è calcolato ›", style = T.Small.copy(color = C.AccentDeep),
            modifier = Modifier.padding(top = 6.dp).clickable { Nav.go(Screen.EnergyInfo) })
    }
}

@Composable
private fun TotalCol(label: String, v: Double, goal: Double, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = T.Small)
        Text("${v.r0()} g", style = T.Num)
        Text("su ${goal.r0()} g", style = T.Small)
    }
}

@Composable
fun MealPlanCard(s: AppState, date: LocalDate, m: PlannedMeal) {
    val db = Db.db
    val r = m.solved
    var open by remember(r.template.id) { mutableStateOf(false) }
    val slot = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }
    val eaten = slot?.eatenEntryIds?.isNotEmpty() == true
    val alts = remember(r.template.id, r.target, s.dietPrefs) {
        Planner.alternatives(db, r.template.meal, r.target, s.dietPrefs, emptySet()).map { it.template.id }
    }
    val idx = alts.indexOf(r.template.id)
    JCard(padding = PaddingValues(14.dp)) {
        Row {
            RecipeImage(r.template.id, r.template.image, s.recipePhotos[r.template.id],
                Modifier.size(width = 112.dp, height = 112.dp).clickable { openRecipe(date, m) }, mealEmoji(r.template.meal))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${m.slotName} · ${if (idx <= 0) "Proposto" else "Alternativa $idx"}", style = T.Small)
                Text(r.template.name, style = T.Dish, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { openRecipe(date, m) })
                Spacer(Modifier.height(2.dp))
                Text("${r.total.kcal.r0()} kcal", style = T.Num)
                Row(Modifier.clickable { open = !open }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (open) "Nascondi dettagli" else "Vedi dettagli e macro", style = T.Muted)
                    Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = C.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!open) Text(r.shortList(), style = T.Body.copy(color = C.Muted), maxLines = 3, overflow = TextOverflow.Ellipsis)
        else {
            Text("Ingredienti e grammature", style = T.Title2)
            Spacer(Modifier.height(4.dp))
            Text(r.shortList(), style = T.Body)
            Spacer(Modifier.height(10.dp))
            MacroTiles(r.total.protein, r.total.carbs, r.total.fat)
            Spacer(Modifier.height(8.dp))
            Text("Obiettivo del pasto: ${r.target.kcal.r0()} kcal · P ${r.target.protein.r0()} · C ${r.target.carbs.r0()} · G ${r.target.fat.r0()}", style = T.Small)
            Spacer(Modifier.height(10.dp))
            GhostButton("👨‍🍳  Ricetta completa", Modifier.fillMaxWidth()) { openRecipe(date, m) }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = C.Line)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (eaten) C.GreenSoft else C.Surface)
                    .border(1.dp, if (eaten) C.Green else C.Line, RoundedCornerShape(50))
                    .clickable {
                        if (eaten) Store.uneatPlanned(date, m.slotIndex)
                        else { Store.eatPlanned(date, m); Store.consumeFromPantry(r) }
                    }.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (eaten) Icon(Icons.Filled.Check, null, tint = C.Green, modifier = Modifier.size(16.dp))
                    Text(if (eaten) " Mangiato" else "Segna mangiato", style = T.Label.copy(color = if (eaten) C.Green else C.Text), maxLines = 1, softWrap = false)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).border(1.dp, C.Line, CircleShape)
                    .clickable(enabled = idx > 0 && !eaten) { Store.setSlotRecipe(date, m.slotIndex, alts[idx - 1]) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Precedente", tint = if (idx > 0) C.Text else C.Faint) }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (eaten) C.SurfaceHi else C.Ink)
                    .clickable(enabled = !eaten && alts.isNotEmpty()) {
                        val next = alts.getOrNull(idx + 1) ?: alts.first { it != r.template.id }
                        Store.setSlotRecipe(date, m.slotIndex, next)
                    }.padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Text("Cambia ›", style = T.Button, maxLines = 1, softWrap = false) }
        }
        Text("Vedi tutte le alternative", style = T.Small.copy(color = C.AccentDeep),
            modifier = Modifier.padding(top = 8.dp).clickable { Nav.go(Screen.Alternatives(date, m.slotIndex)) })
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
    val tags = listOf("lattosio" to "Senza lattosio", "glutine" to "Senza glutine", "pesce" to "Niente pesce", "crostacei" to "Niente crostacei",
        "carne" to "Vegetariano (niente carne)", "maiale" to "Niente maiale", "uova" to "Niente uova", "frutta_secca" to "Niente frutta secca", "soia" to "Niente soia")
    Column(Modifier.fillMaxSize()) {
        TopBar("Preferenze della dieta", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Esclusioni e intolleranze")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { (k, l) ->
                    Pill(l, k in p.exclude, C.Accent) { p = p.copy(exclude = if (k in p.exclude) p.exclude - k else p.exclude + k) }
                }
            }
            Text("Con \"Senza lattosio\" i piatti usano latte delattosato e formaggi stagionati come il Parmigiano.", style = T.Small, modifier = Modifier.padding(top = 6.dp))
            SectionTitle("Ripartizione delle calorie")
            p.slots.forEachIndexed { i, sl ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sl.name, style = T.Label, modifier = Modifier.width(96.dp))
                    Slider((shares[i] ?: sl.share).toFloat(), { shares[i] = it.toInt() }, valueRange = 5f..50f, modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceAlt))
                    Text("${shares[i]}%", style = T.Label, modifier = Modifier.width(44.dp))
                }
            }
            val sum = shares.values.sum()
            Text(if (sum == 100) "Totale 100% ✓" else "Totale $sum%: verrà riportato in proporzione a 100%", style = T.Small.copy(color = if (sum == 100) C.Green else C.Gold))
            SectionTitle("Tempo massimo di preparazione")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 40).forEach { mm -> Pill("$mm min", p.maxMinutes == mm) { p = p.copy(maxMinutes = mm) } }
            }
            if (p.disliked.isNotEmpty()) {
                SectionTitle("Piatti nascosti")
                p.disliked.forEach { id ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(Db.db.recipe(id)?.name ?: id, style = T.Body, modifier = Modifier.weight(1f))
                        Text("Ripristina", style = T.Label.copy(color = C.AccentDeep), modifier = Modifier.clickable { p = p.copy(disliked = p.disliked - id) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Salva e ricalcola il piano", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
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
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            JCard {
                Text("Oggi", style = T.Muted)
                Text("${e.targetKcal.r0()} kcal", style = T.Big.copy(color = C.AccentDeep))
                Spacer(Modifier.height(8.dp))
                MacroTiles(e.protein, e.carbs, e.fat)
            }
            Spacer(Modifier.height(12.dp))
            JCard {
                e.explanation.forEach { Text("• $it", style = T.Read, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text("L'obiettivo parte dal tuo fabbisogno di mantenimento (profilo e questionario) più l'obiettivo di peso, e non scende mai " +
                    "perché al mattino hai fatto pochi passi. Lo smartwatch aggiunge calorie solo quando la spesa misurata supera quella " +
                    "attesa, e dopo almeno 5 giorni di dati affina il fabbisogno sui tuoi valori reali. " +
                    "Metabolismo basale: Mifflin-St Jeor, o Katch-McArdle se indichi la massa grassa. " +
                    "Proteine: ${"%.1f".format(java.util.Locale.ITALY, com.jarvis.food.data.Nutrition.proteinPerKg(s.profile))} g per kg. " +
                    "Grassi: ${s.goals.fatPct}% delle calorie di base. Sono stime: se il peso non va nella direzione attesa per 2-3 settimane, " +
                    "correggi l'obiettivo dal Profilo.", style = T.Small.copy(fontStyle = FontStyle.Italic))
            Spacer(Modifier.height(10.dp))
            GhostButton("Modalità di calcolo dell'attività", Modifier.fillMaxWidth()) { Nav.go(Screen.Watch) }
        }
    }
}

/** Dettaglio micro del piatto: le voci principali in percentuale del riferimento giornaliero. */
@Composable
fun DishMicros(r: SolvedRecipe, male: Boolean) {
    val keys = listOf("fiber", "vitC", "vitA", "folate", "vitB12", "calcium", "iron", "magnesium", "potassium", "zinc")
    Column {
        keys.mapNotNull { Micros.byKey[it] }.forEach { d ->
            val v = r.micro(d.key)
            val ref = if (male) d.men else d.women
            val pct = if (ref > 0) v / ref * 100 else 0.0
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(d.name, style = T.Body, modifier = Modifier.width(110.dp))
                ProgressLine((pct / 100).toFloat(), if (pct >= 30) C.Green else C.Blue, Modifier.weight(1f), 7.dp)
                Text("  ${Micros.fmt(v, d.unit)}", style = T.Small, modifier = Modifier.width(78.dp))
            }
        }
        Text("Percentuale del riferimento giornaliero EFSA coperta da questo piatto.", style = T.Small)
    }
}

/** Per ordinare le alternative a parità: usato anche in Cucina. */
fun fitLabel(r: SolvedRecipe): String = when {
    Solver.fitScore(r) < 0.01 -> "precisione ottima"
    Solver.fitScore(r) < 0.05 -> "precisione buona"
    else -> "precisione indicativa"
}

@Suppress("unused") private fun prefsSummary(p: DietPrefs) = p.exclude.joinToString()
