package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.MEAL_EMOJI
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
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
        else -> "${d.dayOfWeek.getDisplayName(TextStyle.FULL, IT).replaceFirstChar { it.uppercase() }} ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, IT)}"
    }
}

@Composable
fun DiaryScreen() {
    val s by Store.state.collectAsState()
    val date = Nav.diaryDate
    val entries = Store.entriesFor(s, date)
    val totals = Nutrition.sum(entries)
    val burned = Store.burnedFor(s, date).toDouble()
    val (gc, gp, gf) = Nutrition.goalGrams(s.goals)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        val hello = when (LocalTime.now().hour) { in 5..11 -> "Buongiorno"; in 12..17 -> "Buon pomeriggio"; else -> "Buonasera" }
        Text("$hello, ${s.profile.name}", style = T.Muted)
        Text(dayLabel(date), style = T.Display)
        Spacer(Modifier.height(12.dp))
        WeekStrip(date, s) { Nav.diaryDate = it }

        Spacer(Modifier.height(14.dp))
        JCard(onClick = { Nav.go(Screen.DayNutrients(date)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MiniStat("Mangiate", totals.kcal.r0(), C.Text)
                    MiniStat("Bruciate", burned.r0(), C.Accent)
                    MiniStat("Obiettivo", s.goals.kcal.toString(), C.Muted)
                }
                CalorieRing(totals.kcal, s.goals.kcal.toDouble(), burned)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroBar("Carboidrati", totals.carbs, gc, C.Carbs, Modifier.weight(1f))
                MacroBar("Proteine", totals.protein, gp, C.Protein, Modifier.weight(1f))
                MacroBar("Grassi", totals.fat, gf, C.Fat, Modifier.weight(1f))
            }
        }

        FastingMini(s)

        SectionTitle("Pasti")
        for (meal in 0..3) {
            MealCard(meal, date, s)
            Spacer(Modifier.height(10.dp))
        }

        SectionTitle("Acqua")
        WaterCard(date, s)

        SectionTitle("Attività", action = "Aggiungi") { Nav.go(Screen.Activity(date)) }
        ActivityCard(date, s)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, style = T.Title.copy(color = color))
        Text(label, style = T.Small)
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, s: AppState, onSelect: (LocalDate) -> Unit) {
    val monday = selected.with(DayOfWeek.MONDAY)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(selected.minusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.KeyboardArrowLeft, "Settimana precedente", tint = C.Muted)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val d = monday.plusDays(i.toLong())
                val sel = d == selected
                val kcal = Nutrition.sum(Store.entriesFor(s, d)).kcal
                val ratio = if (s.goals.kcal > 0) kcal / s.goals.kcal else 0.0
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (sel) C.Accent else C.Surface)
                        .clickable { onSelect(d) }
                        .padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, IT).take(3), style = T.Small.copy(color = if (sel) C.OnAccent else C.Muted))
                    Text(d.dayOfMonth.toString(), style = T.Label.copy(color = if (sel) C.OnAccent else C.Text))
                    Spacer(Modifier.height(3.dp))
                    val dot = when {
                        kcal <= 0 -> C.SurfaceHi
                        ratio > 1.1 -> C.Danger
                        else -> if (sel) C.OnAccent else C.Accent
                    }
                    Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
                }
            }
        }
        IconButton(onClick = { onSelect(selected.plusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.KeyboardArrowRight, "Settimana successiva", tint = C.Muted)
        }
    }
}

@Composable
private fun FastingMini(s: AppState) {
    val start = s.fasting.activeStart ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val hours = (now - start) / 3_600_000.0
    Spacer(Modifier.height(10.dp))
    JCard(onClick = { Nav.tab = 1 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱️", style = T.Title)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Digiuno ${s.fasting.planId} in corso", style = T.Label)
                Text("${hours.r1()} h su ${s.fasting.targetHours} h", style = T.Small)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressLine((hours / s.fasting.targetHours).toFloat(), C.Fasting)
    }
}

@Composable
private fun MealCard(meal: Int, date: LocalDate, s: AppState) {
    val entries = Store.entriesFor(s, date).filter { it.meal == meal }.sortedBy { it.time }
    val t = Nutrition.sum(entries)
    val target = s.goals.kcal * Nutrition.MEAL_SPLIT[meal]
    var menu by remember { mutableStateOf(false) }
    JCard(padding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(MEAL_EMOJI[meal], style = T.Title)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(MEAL_NAMES[meal], style = T.Title2)
                Text("${t.kcal.r0()} / ${target.r0()} kcal", style = T.Small)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opzioni", tint = C.Muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Copia da ieri") }, onClick = {
                        Store.copyMeal(date.minusDays(1), meal, date, meal); menu = false
                    })
                    DropdownMenuItem(text = { Text("Copia su domani") }, onClick = {
                        Store.copyMeal(date, meal, date.plusDays(1), meal); menu = false
                    })
                    DropdownMenuItem(text = { Text("Riconosci da foto (AI)") }, onClick = {
                        menu = false; Nav.go(Screen.PhotoAi(date, meal))
                    })
                    if (entries.isNotEmpty()) DropdownMenuItem(text = { Text("Svuota pasto", color = C.Danger) }, onClick = {
                        entries.forEach { Store.removeEntry(it.id) }; menu = false
                    })
                }
            }
            RoundIconButton(Icons.Filled.Add, "Aggiungi a ${MEAL_NAMES[meal]}", size = 36.dp) {
                Nav.go(Screen.AddFood(date, meal))
            }
        }
        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            entries.forEach { e ->
                val et = Nutrition.of(e.food, e.grams)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable { Nav.go(Screen.FoodDetail(e.food, date, meal, e.grams, e)) }
                        .padding(vertical = 7.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.food.name, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (if (e.food.source == "quick") "aggiunta rapida" else "${e.grams.r0()} ${if (e.food.liquid) "ml" else "g"}") + if (e.food.brand.isNotBlank()) " · ${e.food.brand}" else "",
                            style = T.Small, maxLines = 1
                        )
                    }
                    Text("${et.kcal.r0()} kcal", style = T.Label.copy(color = C.Muted))
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WaterCard(date: LocalDate, s: AppState) {
    val ml = s.water[date.toString()] ?: 0
    val goal = s.goals.waterMl
    val glasses = ((goal + s.glassMl - 1) / s.glassMl).coerceIn(1, 16)
    val filled = ml / s.glassMl
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${(ml / 1000.0).r1()} L", style = T.Title.copy(color = C.Water))
                Text("su ${(goal / 1000.0).r1()} L · bicchiere ${s.glassMl} ml", style = T.Small)
            }
            GhostButton("−") { Store.setWater(date, ml - s.glassMl) }
            Spacer(Modifier.width(8.dp))
            PrimaryButton("+ ${s.glassMl} ml", color = C.Water) { Store.setWater(date, ml + s.glassMl) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until glasses) {
                val on = i < filled
                Box(
                    Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, topEnd = 3.dp))
                        .background(if (on) C.Water else C.SurfaceHi)
                        .border(1.dp, if (on) C.Water else C.Line, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, topEnd = 3.dp))
                        .clickable { Store.setWater(date, (i + 1) * s.glassMl) }
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(date: LocalDate, s: AppState) {
    val list = s.activities.filter { it.date == date.toString() }
    val steps = s.steps[date.toString()] ?: 0
    var editSteps by remember(date) { mutableStateOf(false) }
    var stepsText by remember(date) { mutableStateOf(if (steps > 0) steps.toString() else "") }
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👟", style = T.Title)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${steps.thousands()} passi", style = T.Label)
                Text("obiettivo ${s.goals.steps.thousands()} · ${Nutrition.stepsKcal(steps, Store.currentWeight(s))} kcal", style = T.Small)
            }
            GhostButton(if (editSteps) "Chiudi" else "Modifica") { editSteps = !editSteps }
        }
        Spacer(Modifier.height(8.dp))
        ProgressLine(steps / s.goals.steps.coerceAtLeast(1).toFloat(), C.Accent)
        if (editSteps) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                JField(stepsText, { stepsText = it.filter { c -> c.isDigit() }.take(6) }, "Passi di oggi", Modifier.weight(1f), number = true)
                Spacer(Modifier.width(8.dp))
                PrimaryButton("Salva") { Store.setSteps(date, stepsText.toIntOrNull() ?: 0); editSteps = false }
            }
        }
        list.forEach { a ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = T.Label)
                    Text("${a.minutes} min", style = T.Small)
                }
                Text("−${a.kcal} kcal", style = T.Label.copy(color = C.Accent))
                Spacer(Modifier.width(6.dp))
                Text("✕", style = T.Label.copy(color = C.Faint), modifier = Modifier.clip(CircleShape).clickable { Store.removeActivity(a.id) }.padding(6.dp))
            }
        }
    }
}
