package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.jarvis.food.data.Db
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.engine.MicroAnalysis
import kotlinx.coroutines.delay
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
fun TodayScreen() {
    val s by Store.state.collectAsState()
    val date = Nav.diaryDate
    val entries = Store.entriesFor(s, date)
    val totals = Nutrition.sum(entries)
    val e = Store.energyFor(s, date)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        val hello = when (LocalTime.now().hour) { in 5..11 -> "Buongiorno"; in 12..17 -> "Buon pomeriggio"; else -> "Buonasera" }
        Text("$hello, ${s.profile.name}", style = T.Muted)
        Text(dayLabel(date), style = T.Display)
        Spacer(Modifier.height(12.dp))
        WeekPills(date, date.with(java.time.DayOfWeek.MONDAY)) { Nav.diaryDate = it }

        Spacer(Modifier.height(14.dp))
        JCard(onClick = { Nav.go(Screen.DayNutrients(date)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Mangiate", totals.kcal.r0(), C.Text)
                    MiniStat(if (s.energy.mode == 2) "Attività (watch)" else "Attività", Store.burnedFor(s, date).toString(), C.Green)
                    MiniStat("Obiettivo", e.targetKcal.r0(), C.AccentDeep)
                }
                CalorieRing(totals.kcal, e.targetKcal)
            }
            if (e.extraKcal > 0) {
                Spacer(Modifier.height(8.dp))
                Text("+${e.extraKcal.r0()} kcal per l'attività di oggi, aggiunte ai carboidrati", style = T.Small.copy(color = C.Green))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroBar("Proteine", totals.protein, e.protein, C.Protein, Modifier.weight(1f))
                MacroBar("Carboidrati", totals.carbs, e.carbs, C.Carbs, Modifier.weight(1f))
                MacroBar("Grassi", totals.fat, e.fat, C.Fat, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("➕", "Aggiungi") { Nav.go(Screen.AddFood(date, defaultMeal())) }
            QuickAction("📷", "Foto piatto") { Nav.go(Screen.PhotoAi(date, defaultMeal())) }
            QuickAction("🧊", "Frigo e dispensa") { Nav.go(Screen.PantryScan("foto")) }
            QuickAction("⏱️", "Digiuno") { Nav.go(Screen.Fasting) }
        }

        PlanToday(s, date)
        NutrientInsight(s)
        FastingMini(s)

        SectionTitle("Diario")
        for (meal in 0..3) { MealCard(meal, date, s); Spacer(Modifier.height(10.dp)) }

        SectionTitle("Acqua")
        WaterCard(date, s)

        SectionTitle("Attività", action = if (s.energy.mode == 2) "Smartwatch" else "Aggiungi") {
            if (s.energy.mode == 2) Nav.go(Screen.Watch) else Nav.go(Screen.Activity(date))
        }
        ActivityCard(date, s)
        Spacer(Modifier.height(24.dp))
    }
}

fun defaultMeal(): Int = when (LocalTime.now().hour) { in 5..10 -> 0; in 11..15 -> 1; in 18..23 -> 2; else -> 3 }

@Composable
private fun QuickAction(emoji: String, label: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(50)).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji); Spacer(Modifier.width(6.dp)); Text(label, style = T.Label)
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column { Text(value, style = T.Num.copy(color = color)); Text(label, style = T.Small) }
}

/** Il piano di oggi in breve, con foto e spunta "mangiato". */
@Composable
private fun PlanToday(s: AppState, date: LocalDate) {
    val meals = Store.plannedMeals(s, date)
    SectionTitle("Il piano di ${if (date == LocalDate.now()) "oggi" else dayLabel(date).lowercase()}", action = "Apri") { Nav.planDate = date; Nav.tab = 1 }
    if (meals.isEmpty()) {
        JCard(color = C.AccentSoft, onClick = { Nav.planDate = date; Nav.tab = 1 }) {
            Text("Crea la tua dieta personalizzata", style = T.Title2)
            Text("Piatti con foto, ricette passo passo e grammature calcolate sui tuoi obiettivi.", style = T.Muted)
        }
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        meals.forEach { m ->
            val eaten = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == m.slotIndex }?.eatenEntryIds?.isNotEmpty() == true
            Column(Modifier.width(168.dp).clip(RoundedCornerShape(20.dp)).background(C.Surface).border(1.dp, C.Line, RoundedCornerShape(20.dp))
                .clickable { Nav.go(Screen.RecipeView(m.solved.template.id, m.solved.target, date, m.slotIndex, m.diaryMeal)) }) {
                Box {
                    RecipeImage(m.solved.template.id, m.solved.template.image, s.recipePhotos[m.solved.template.id],
                        Modifier.fillMaxWidth().height(108.dp), "🍽️", 0.dp)
                    if (eaten) Box(Modifier.padding(8.dp).clip(RoundedCornerShape(50)).background(C.Green).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("✓ mangiato", style = T.Small.copy(color = C.OnAccent))
                    }
                }
                Column(Modifier.padding(10.dp)) {
                    Text(m.slotName, style = T.Small)
                    Text(m.solved.template.name, style = T.Label, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(36.dp))
                    Text("${m.solved.total.kcal.r0()} kcal", style = T.Label.copy(color = C.AccentDeep))
                }
            }
        }
    }
}

/** Consigli sui micronutrienti ben visibili nella schermata principale. */
@Composable
private fun NutrientInsight(s: AppState) {
    val e = Store.energyFor(s, LocalDate.now())
    val rep = remember(s.diary.size, s.profile) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal) }
    SectionTitle("Vitamine e minerali", action = "Dettagli") { Nav.go(Screen.Micronutrients) }
    JCard(onClick = { Nav.go(Screen.Micronutrients) }) {
        if (rep.loggedDays == 0) {
            Text("Registra i pasti di almeno un giorno completo", style = T.Title2)
            Text("Poi qui vedrai quali vitamine e minerali ti mancano e cosa mangiare per migliorare.", style = T.Muted)
            return@JCard
        }
        val low = rep.low.take(3)
        if (low.isEmpty()) {
            Text("Ottimo equilibrio negli ultimi ${rep.loggedDays} giorni 🌿", style = T.Title2)
            Text("Nessun apporto medio sotto il 70% del riferimento.", style = T.Muted)
        } else {
            Text("Apporto basso negli ultimi ${rep.loggedDays} giorni", style = T.Title2)
            Spacer(Modifier.height(6.dp))
            low.forEach { st ->
                val tip = MicroAnalysis.tips(Db.db, st, s.dietPrefs.exclude, 1).firstOrNull()
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(C.GoldSoft), contentAlignment = Alignment.Center) {
                        Text("${st.pct.r0()}%", style = T.Small.copy(color = C.Text))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(st.def.name, style = T.Label)
                        if (tip != null) Text("Prova ${tip.grams.r0()} g di ${tip.food.name.lowercase()} (+${tip.pctOfRef.r0()}%)", style = T.Small)
                    }
                }
            }
        }
        val gap = rep.groups.filter { (it.daysSince ?: 99) >= 7 }.take(2)
        if (gap.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(gap.joinToString("  ·  ") { "${it.label}: ${it.daysSince?.let { d -> "$d giorni fa" } ?: "mai registrato"}" }, style = T.Small.copy(color = C.AccentDeep))
        }
    }
}

@Composable
private fun FastingMini(s: AppState) {
    val start = s.fasting.activeStart ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val hours = (now - start) / 3_600_000.0
    Spacer(Modifier.height(12.dp))
    JCard(onClick = { Nav.go(Screen.Fasting) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱️", style = T.Title); Spacer(Modifier.width(10.dp))
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
    var menu by remember { mutableStateOf(false) }
    JCard(padding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(MEAL_NAMES[meal], style = T.Title2)
                Text("${t.kcal.r0()} kcal · P ${t.protein.r0()} · C ${t.carbs.r0()} · G ${t.fat.r0()}", style = T.Small)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opzioni", tint = C.Muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Copia da ieri") }, onClick = { Store.copyMeal(date.minusDays(1), meal, date, meal); menu = false })
                    DropdownMenuItem(text = { Text("Copia su domani") }, onClick = { Store.copyMeal(date, meal, date.plusDays(1), meal); menu = false })
                    DropdownMenuItem(text = { Text("Riconosci da foto (AI)") }, onClick = { menu = false; Nav.go(Screen.PhotoAi(date, meal)) })
                    if (entries.isNotEmpty()) DropdownMenuItem(text = { Text("Svuota pasto", color = C.Danger) }, onClick = { entries.forEach { Store.removeEntry(it.id) }; menu = false })
                }
            }
            RoundIconButton(Icons.Filled.Add, "Aggiungi a ${MEAL_NAMES[meal]}", size = 36.dp) { Nav.go(Screen.AddFood(date, meal)) }
        }
        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            entries.forEach { e ->
                val et = Nutrition.of(e.food, e.grams)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { Nav.go(Screen.FoodDetail(e.food, date, meal, e.grams, e)) }
                    .padding(vertical = 7.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.food.name, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (e.food.source == "quick") "aggiunta rapida" else "${e.grams.r0()} ${if (e.food.liquid) "ml" else "g"}") +
                                (if (e.planRef != null) " · dal piano" else "") + if (e.food.brand.isNotBlank()) " · ${e.food.brand}" else "", style = T.Small, maxLines = 1)
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
                Text("${(ml / 1000.0).r1()} L", style = T.Num.copy(color = C.Blue))
                Text("su ${(goal / 1000.0).r1()} L · bicchiere ${s.glassMl} ml", style = T.Small)
            }
            GhostButton("−") { Store.setWater(date, ml - s.glassMl) }
            Spacer(Modifier.width(8.dp))
            PrimaryButton("+ ${s.glassMl} ml", color = C.Blue) { Store.setWater(date, ml + s.glassMl) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until glasses) {
                val on = i < filled
                val shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, topEnd = 3.dp)
                Box(Modifier.weight(1f).height(26.dp).clip(shape).background(if (on) C.Blue else C.BlueSoft).clickable { Store.setWater(date, (i + 1) * s.glassMl) })
            }
        }
    }
}

@Composable
private fun ActivityCard(date: LocalDate, s: AppState) {
    val list = s.activities.filter { it.date == date.toString() }
    val watch = s.energy.mode == 2
    val h = s.health[date.toString()]
    val steps = Store.stepsFor(s, date)
    var editSteps by remember(date) { mutableStateOf(false) }
    var stepsText by remember(date) { mutableStateOf(if (steps > 0) steps.toString() else "") }
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👟", style = T.Title); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${steps.thousands()} passi", style = T.Label)
                Text(if (watch) "dallo smartwatch" + (h?.let { " · agg. ${fmtTime(it.updatedAt)}" } ?: "") else "obiettivo ${s.goals.steps.thousands()}", style = T.Small)
            }
            if (!watch) GhostButton(if (editSteps) "Chiudi" else "Modifica") { editSteps = !editSteps }
        }
        Spacer(Modifier.height(8.dp))
        ProgressLine(steps / s.goals.steps.coerceAtLeast(1).toFloat(), C.Green)
        if (editSteps && !watch) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                JField(stepsText, { stepsText = it.filter { c -> c.isDigit() }.take(6) }, "Passi di oggi", Modifier.weight(1f), number = true)
                Spacer(Modifier.width(8.dp))
                PrimaryButton("Salva") { Store.setSteps(date, stepsText.toIntOrNull() ?: 0); editSteps = false }
            }
        }
        if (watch) h?.sessions?.forEach { w ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(w.title, style = T.Label)
                    Text("${fmtTime(w.start)} · ${((w.end - w.start) / 60000)} min", style = T.Small)
                }
                w.kcal?.let { Text("${it.r0()} kcal", style = T.Label.copy(color = C.Green)) }
            }
        }
        list.forEach { a ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(a.name, style = T.Label); Text("${a.minutes} min · inserita a mano", style = T.Small) }
                Text("${a.kcal} kcal", style = T.Label.copy(color = C.Green))
                Spacer(Modifier.width(6.dp))
                Text("✕", style = T.Label.copy(color = C.Faint), modifier = Modifier.clip(CircleShape).clickable { Store.removeActivity(a.id) }.padding(6.dp))
            }
        }
        if (s.energy.mode == 0 && (list.isNotEmpty() || steps > 0)) {
            Spacer(Modifier.height(8.dp))
            Text("Con il livello di attività fisso, le attività non cambiano l'obiettivo (sono già considerate). Puoi cambiarlo in Profilo → Smartwatch e attività.", style = T.Small)
        }
    }
}
