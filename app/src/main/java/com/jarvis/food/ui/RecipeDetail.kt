package com.jarvis.food.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextOverflow
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.Practical
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.Db
import com.jarvis.food.data.Ingredient
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.RecipeMeta
import com.jarvis.food.engine.RecipeSearch
import com.jarvis.food.engine.Solver
import com.jarvis.food.engine.SolvedRecipe
import com.jarvis.food.engine.formatAmount
import com.jarvis.food.engine.original
import com.jarvis.food.engine.scaled
import java.time.LocalDate
import kotlin.math.abs

private val ROLE_DOT = mapOf("p" to C.Protein, "c" to C.Carbs, "f" to C.Fat, "v" to C.Green, "x" to Color(0xFFB8B4CC))
private val ROLE_NAME = mapOf("p" to "Proteine", "c" to "Carboidrati", "f" to "Grassi", "v" to "Verdure", "x" to "Condimento")

private fun shareText(r: SolvedRecipe): String = buildString {
    val t = r.template; val a = r.total
    appendLine(t.name)
    appendLine("1 porzione: ${a.kcal.r0()} kcal · P ${a.protein.r0()} g · C ${a.carbs.r0()} g · G ${a.fat.r0()} g · ${t.minutes} min")
    appendLine()
    appendLine("Ingredienti:")
    r.items.forEach { appendLine("- ${it.displayName()}: ${it.amountLabel()}") }
    appendLine()
    appendLine("Preparazione:")
    r.steps().forEachIndexed { i, st -> appendLine("${i + 1}. $st") }
    appendLine()
    append("Da Jarvis Food")
}

private fun toUserRecipe(r: SolvedRecipe, servings: Int): Recipe {
    val t = r.template
    return Recipe(
        Store.newId(), t.name + " (copia)", servings,
        r.items.map { Ingredient(it.food.toFood(), Math.round(it.grams * 10) / 10.0) },
        r.steps(), t.minutes, "", recEmoji(t.meal),
        description = RecipeMeta.description(Db.db, t), cuisine = t.cuisine, difficulty = t.difficulty, meal = t.meal
    )
}

@Composable
private fun ActionChip(icon: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier.height(44.dp).clip(shape).background(if (active) A.ProteinSoft else Color.White).border(1.dp, if (active) C.Protein else Color(0x1A1D1B3A), shape)
        .clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        AIcon(icon, if (active) A.ProteinInk else A.Ink, 18.dp, 2.2f, fill = active && icon == Ic.Heart)
        GapW(8.dp)
        Text(label, style = T.BtnS.copy(color = if (active) A.ProteinInk else A.Ink), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(15.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.Chip.copy(fontSize = 12.sp), maxLines = 1)
    }
}

@Composable
private fun CompareRow(label: String, orig: Double, adapted: Double, target: Double, unit: String, bold: Boolean = false) {
    val st = if (bold) T.Label else T.Small.copy(color = A.Ink)
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = st, modifier = Modifier.weight(1.1f))
        Text(orig.r0(), style = st, modifier = Modifier.weight(0.9f))
        Text(adapted.r0(), style = st.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.9f))
        Text(target.r0(), style = st, modifier = Modifier.weight(0.9f))
        val d = adapted - target
        Text((if (d >= 0) "+" else "−") + abs(d).r0() + unit, style = st.copy(color = if (abs(d) <= target * 0.05 + 1) A.GreenInk else A.TonalInk), modifier = Modifier.weight(0.9f))
    }
}

/** Ricetta completa (design Aurora) con adattamento ai propri obiettivi, porzioni, preferiti, piano, spesa e dispensa. */
@Composable
fun RecipeViewScreen(sc: Screen.RecipeView) {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val db = Db.db
    val tpl = sc.adHoc ?: db.recipe(sc.recipeId)
    if (tpl == null) { Column { TopBar("Ricetta", onBack = { Nav.back() }); EmptyState("🤔", "Ricetta non trovata", "") }; return }
    LaunchedEffect(tpl.id) { if (sc.adHoc == null) Store.recipeViewed(tpl.id) }

    var adapted by remember(tpl.id) { mutableStateOf(!sc.original) }
    var kind by remember(tpl.id) { mutableIntStateOf(if (sc.slot != null && sc.adHoc == null) 0 else 1) }
    var sideOn by remember(tpl.id) { mutableStateOf(false) }
    var manual by remember(tpl.id) { mutableStateOf<Macro?>(null) }
    var mK by remember(tpl.id) { mutableStateOf("") }
    var mP by remember(tpl.id) { mutableStateOf("") }
    var mC by remember(tpl.id) { mutableStateOf("") }
    var mG by remember(tpl.id) { mutableStateOf("") }
    var servings by remember(tpl.id) { mutableIntStateOf(1) }
    var meal by remember { mutableIntStateOf(sc.diaryMeal) }
    var detail by remember { mutableStateOf(false) }
    var adaptationExpanded by remember(tpl.id) { mutableStateOf(false) }
    var dlgPlan by remember { mutableStateOf(false) }
    var dlgFridge by remember { mutableStateOf(false) }
    val done = remember(tpl.id) { mutableStateListOf<Int>() }

    val dayRem = remember(s.diary, s.energy, s.goals, s.profile, sc.date) { dayRemaining(s, sc.date) }
    // "Mancante di oggi": ciò che resta nella giornata, diviso tra questo pasto e quelli ancora da fare (tenendo conto del piano)
    val smart = remember(s.diary, s.plan, s.dietPrefs, s.energy, s.goals, s.profile, tpl.meal) {
        nowContext(s, if (tpl.meal == "dessert") "spuntino" else tpl.meal, true)
    }
    val isToday = sc.date == LocalDate.now()
    val target = when (kind) {
        1 -> if (isToday && dayRem.kcal >= 80) smart.target else if (dayRem.kcal >= 80) dayRem else sc.target
        2 -> manual ?: sc.target
        else -> sc.target
    }
    val orig = remember(tpl.id, target) { Solver.original(tpl, db, target) }
    // versione con porzioni pratiche (immediata); giudizio, contorno e alternative si calcolano in background
    val adaptedBase = remember(tpl.id, target, adapted) { if (adapted) Solver.solve(tpl, db, target, practical = true) else null }
    val assess by produceState<Practical.Adaptation?>(null, tpl.id, target, adapted, s.dietPrefs) {
        value = null
        if (adapted) value = withContext(Dispatchers.Default) {
            Practical.adapt(db, tpl, target, s.dietPrefs.exclude, { Planner.allowed(db, it, s.dietPrefs) })
        }
    }
    val sideNow = assess?.side?.takeIf { sideOn }
    val adaptedR = adaptedBase?.let { b -> if (sideNow != null) Practical.withSide(b, sideNow.food, sideNow.grams) else b }
    val r = adaptedR ?: orig                                   // una porzione, come mostrata
    val rs = remember(r, servings) { r.scaled(servings.toDouble()) }  // quantità per N porzioni
    val steps = remember(rs) { rs.steps() }
    val info = remember(tpl.id) { RecipeMeta.info(db, tpl) }
    val pantry = remember(s.pantry) { pantryIds(s) }
    val fav = tpl.id in s.recipeFavs
    val slot = sc.slot?.let { sl -> Store.dayPlan(s, sc.date)?.slots?.firstOrNull { it.slot == sl } }
    val eaten = slot?.eatenEntryIds?.isNotEmpty() == true
    val planned = sc.slot != null && sc.adHoc == null

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth()) {
                    RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.fillMaxWidth().height(280.dp), recEmoji(tpl.meal), 0.dp)
                    Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color(0x80181430), Color.Transparent))))
                    tpl.credit?.takeIf { it.author.isNotBlank() }?.let { cr ->
                        Text(cr.author.take(40), style = T.Small.copy(color = Color.White),
                            modifier = Modifier.padding(start = 16.dp, top = 200.dp).clip(RoundedCornerShape(50))
                                .background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Column(
                        Modifier.padding(top = 240.dp).fillMaxWidth()
                            .aShadow(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), 14.dp, Color(0x591D1B3A))
                            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color(0xFFF3F5F8))
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp)
                    ) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(A.GreenSoft).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(RecipeMeta.label(RecipeMeta.MEALS, tpl.meal), style = T.Chip.copy(color = A.GreenInk))
                            }
                            Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(15.dp)).padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                AIcon(Ic.Clock, A.Ink, 14.dp, 2.2f); GapW(6.dp); Text("${tpl.minutes} min", style = T.Chip)
                            }
                            InfoChip(RecipeMeta.DIFFICULTY.getOrElse(tpl.difficulty) { "Facile" })
                            InfoChip(RecipeMeta.label(RecipeMeta.CUISINES, tpl.cuisine))
                        }
                        GapH(14.dp)
                        Text(tpl.name, style = T.H1.copy(fontSize = 33.sp, lineHeight = 37.sp))
                        GapH(8.dp)
                        Text(RecipeMeta.description(db, tpl), style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp, color = A.Muted))

                        // Azioni
                        GapH(14.dp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sc.adHoc == null) ActionChip(Ic.Heart, if (fav) "Preferita" else "Preferito", fav) { Store.toggleRecipeFav(tpl.id) }
                            ActionChip(Ic.Calendar, "Al piano") { dlgPlan = true }
                            ActionChip(Ic.Receipt, "Alla spesa") {
                                val n = Store.addRecipeToShopping(rs, tpl.name, pantry)
                                Toast.makeText(ctx, if (n > 0) "Aggiunti $n ingredienti alla spesa" else "Hai già tutto in dispensa", Toast.LENGTH_SHORT).show()
                            }
                            if (s.family != null) ActionChip(Ic.Family, "Spesa famiglia") {
                                com.jarvis.food.family.FamilyStore.addRecipe(tpl.name, rs.items.map { it.food.id to it.grams })
                                Toast.makeText(ctx, "Ingredienti aggiunti alla spesa della famiglia", Toast.LENGTH_SHORT).show()
                            }
                            ActionChip(Ic.Fridge, "Ho tutto?") { dlgFridge = true }
                            ActionChip(Ic.Swap, "Duplica") {
                                Store.saveRecipe(toUserRecipe(rs, servings)); Toast.makeText(ctx, "Copia salvata in Le mie ricette", Toast.LENGTH_SHORT).show()
                            }
                            ActionChip(Ic.Steps, "Modifica") { Nav.go(Screen.RecipeEditor(toUserRecipe(rs, servings))) }
                            ActionChip(Ic.Right, "Condividi") {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText(rs)) }
                                ctx.startActivity(Intent.createChooser(send, tpl.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }

                        // Valori nutrizionali
                        GapH(20.dp)
                        ACard(radius = 32.dp, padding = 18.dp, glow = Color(0x8C10B981),
                            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFEDF1F5)))) {
                            Column {
                                Text("${r.total.kcal.r0()} kcal a porzione", style = T.Title)
                                GapH(12.dp)
                                MacroChips(r.total.protein, r.total.carbs, r.total.fat)
                                GapH(14.dp)
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xB3FFFFFF)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text((if (adapted) "Adattata a: " else "Ricetta base. Il tuo pasto: ") + "${target.kcal.r0()} kcal · P ${target.protein.r0()} · C ${target.carbs.r0()} · G ${target.fat.r0()}" +
                                        (if (r.micro("fiber") > 0.5) " · fibre ${r.micro("fiber").r0()} g" else ""),
                                        style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                                }
                            }
                        }

                        // Adatta ai miei obiettivi
                        if (!planned) {
                            GapH(14.dp)
                            AInk(if (adaptationExpanded) "Chiudi adattamento" else "Adatta ai miei obiettivi", Modifier.fillMaxWidth(), bg = A.Tonal, fg = A.TonalInk) { adaptationExpanded = !adaptationExpanded }
                            GapH(10.dp)
                            if (adaptationExpanded) ACard(radius = 24.dp, padding = 16.dp, elevation = 6.dp) {
                                Column {
                                    Text("Riadatta ricetta", style = T.H2.copy(fontSize = 19.sp))
                                    Text("Ricalcolo le quantità per ciò che ti manca oggi, ma con porzioni pratiche (unità intere, niente frazioni scomode) e senza stravolgere la ricetta. Se non conviene, ti propongo un contorno o un'altra ricetta.", style = T.Small)
                                    GapH(12.dp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        APill("Originale", !adapted, Modifier.weight(1f), 42.dp) { adapted = false }
                                        APill("Adattata", adapted, Modifier.weight(1f), 42.dp) { adapted = true }
                                    }
                                    if (adapted) {
                                        GapH(12.dp)
                                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Pill("Mio pasto", kind == 0) { kind = 0; sideOn = false }
                                            Pill("Mancante oggi", kind == 1) { kind = 1; sideOn = false }
                                            Pill("Manuale", kind == 2) { kind = 2; sideOn = false }
                                        }
                                        GapH(6.dp)
                                        Text(when (kind) {
                                            0 -> "Quota di questo pasto sul tuo obiettivo giornaliero."
                                            1 -> if (dayRem.kcal >= 80) "Ti mancano oggi ${dayRem.kcal.r0()} kcal · P ${dayRem.protein.r0()} · C ${dayRem.carbs.r0()} · G ${dayRem.fat.r0()}" +
                                                (if (isToday) ": per questo pasto punto a ${target.kcal.r0()} kcal, il resto resta per i pasti ancora da fare." else ".")
                                                else "Hai già raggiunto l'obiettivo di oggi: uso la quota del pasto."
                                            else -> "Scegli tu calorie e macro; i campi vuoti usano la quota del pasto."
                                        }, style = T.Small)
                                        if (kind == 2) {
                                            GapH(8.dp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                JField(mK, { mK = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) }, "kcal", Modifier.weight(1f), number = true)
                                                JField(mP, { mP = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(4) }, "Proteine g", Modifier.weight(1f), number = true)
                                            }
                                            GapH(8.dp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                JField(mC, { mC = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(4) }, "Carboidrati g", Modifier.weight(1f), number = true)
                                                JField(mG, { mG = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(4) }, "Grassi g", Modifier.weight(1f), number = true)
                                            }
                                            GapH(8.dp)
                                            AInk("Applica", Modifier.fillMaxWidth(), height = 46.dp) {
                                                manual = Macro(parseNum(mK) ?: sc.target.kcal, parseNum(mP) ?: sc.target.protein, parseNum(mC) ?: sc.target.carbs, parseNum(mG) ?: sc.target.fat)
                                            }
                                        }
                                        GapH(12.dp)
                                        Row(Modifier.fillMaxWidth()) {
                                            Text("", Modifier.weight(1.1f))
                                            listOf("Originale", "Adattata", "Obiettivo", "Differenza").forEach { Text(it, style = T.Small.copy(fontSize = 11.sp), modifier = Modifier.weight(0.9f), maxLines = 1) }
                                        }
                                        HorizontalDivider(color = C.Line)
                                        val a = r.total; val o = orig.total
                                        CompareRow("kcal", o.kcal, a.kcal, target.kcal, "", bold = true)
                                        CompareRow("Proteine g", o.protein, a.protein, target.protein, "")
                                        CompareRow("Carboidrati g", o.carbs, a.carbs, target.carbs, "")
                                        CompareRow("Grassi g", o.fat, a.fat, target.fat, "")
                                        val errPct = if (target.kcal > 0) abs(a.kcal - target.kcal) / target.kcal * 100 else 0.0
                                        GapH(8.dp)
                                        val ad = assess
                                        if (ad == null) {
                                            Text("Valuto se l'adattamento è sensato…", style = T.Small.copy(color = A.Muted))
                                        } else {
                                            val lvl = if (sideNow != null) Practical.level(adaptedR ?: ad.practical) else ad.level
                                            val tone = when (lvl) { Practical.Level.GOOD -> A.GreenSoft to A.GreenInk; Practical.Level.LIMITED -> Color(0xFFFFF0C4) to Color(0xFF8A5A00); else -> Color(0xFFFFE0DC) to Color(0xFFB3261E) }
                                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(tone.first).padding(14.dp)) {
                                                Text(if (sideNow != null && lvl == Practical.Level.GOOD) "Con il contorno l'obiettivo è centrato." else ad.headline, style = T.Label.copy(color = tone.second, fontWeight = FontWeight.Bold))
                                                if (errPct > 5) Text("Scarto sulle calorie: ${if (a.kcal > target.kcal) "+" else "−"}${abs(a.kcal - target.kcal).r0()} kcal (${errPct.r0()}%).", style = T.Small.copy(color = tone.second))
                                                else Text("Calorie entro il 5% dall'obiettivo.", style = T.Small.copy(color = tone.second))
                                                ad.notes.forEach { n -> GapH(4.dp); Text("• $n", style = T.Small.copy(color = A.Ink)) }
                                            }
                                            ad.side?.let { sd ->
                                                GapH(10.dp)
                                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(18.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Completa con ${sd.label}", style = T.Label.copy(fontWeight = FontWeight.Bold))
                                                        Text("+${sd.macro.kcal.r0()} kcal · P ${sd.macro.protein.r0()} · C ${sd.macro.carbs.r0()} · G ${sd.macro.fat.r0()}", style = T.Small)
                                                    }
                                                    GapW(8.dp)
                                                    APill(if (sideOn) "Aggiunto" else "Aggiungi", sideOn, height = 40.dp, hPad = 16.dp) { sideOn = !sideOn }
                                                }
                                            }
                                            if (ad.alternatives.isNotEmpty()) {
                                                GapH(12.dp)
                                                Text("Ricette più adatte a ciò che ti manca", style = T.Label.copy(fontWeight = FontWeight.Bold))
                                                ad.alternatives.forEach { alt ->
                                                    GapH(8.dp)
                                                    val at = alt.solved.template
                                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(18.dp))
                                                        .clickable { Nav.go(Screen.RecipeView(at.id, target, sc.date, null, meal)) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        RecipeImage(at.id, at.image, s.recipePhotos[at.id], Modifier.size(56.dp), recEmoji(at.meal), 14.dp)
                                                        GapW(10.dp)
                                                        Column(Modifier.weight(1f)) {
                                                            Text(at.name, style = T.Label.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                            Text("${alt.solved.total.kcal.r0()} kcal · P ${alt.solved.total.protein.r0()} · ${at.minutes} min", style = T.Small)
                                                        }
                                                        AIcon(Ic.Right, A.Muted, 18.dp, 2.2f)
                                                    }
                                                }
                                            }
                                            Text("Valori calcolati dal database USDA sulle quantità mostrate: sono stime, perché dipendono dal prodotto reale e dalla cottura.", style = T.Small.copy(fontSize = 11.sp, color = A.Muted), modifier = Modifier.padding(top = 10.dp))
                                        }
                                        GapH(6.dp)
                                        TextButton(onClick = { adapted = false; kind = 0; sideOn = false; manual = null; mK = ""; mP = ""; mC = ""; mG = "" }) {
                                            Text("Ripristina la ricetta originale", color = C.AccentDeep, style = T.Link)
                                        }
                                    }
                                    GapH(8.dp)
                                    val missingMicro = r.items.filter { it.ing.role != "x" && it.food.micro.size < 8 }.map { it.displayName() }.distinct()
                                    Text("Calcolati dai dati USDA: calorie, proteine, carboidrati, grassi e fibre. Stimati: costo e tempi" +
                                        (if (missingMicro.isNotEmpty()) ". Vitamine e minerali incompleti per: ${missingMicro.take(4).joinToString()}." else ". Nessun dato nutrizionale principale mancante."),
                                        style = T.Small.copy(fontSize = 11.5.sp))
                                }
                            }
                        }

                        // Ingredienti
                        Row(Modifier.fillMaxWidth().padding(top = 30.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Ingredienti", style = T.H2, modifier = Modifier.weight(1f))
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White).border(1.dp, A.Hair, CircleShape).clickable(enabled = servings > 1) { servings-- }, contentAlignment = Alignment.Center) {
                                AIcon(Ic.Minus, if (servings > 1) A.Ink else A.Muted, 16.dp, 2.6f)
                            }
                            Text(if (servings == 1) "1 porzione" else "$servings porzioni", style = T.Label, modifier = Modifier.padding(horizontal = 10.dp))
                            Box(Modifier.size(36.dp).clip(CircleShape).background(C.Navy).clickable(enabled = servings < 12) { servings++ }, contentAlignment = Alignment.Center) {
                                AIcon(Ic.Plus, Color.White, 16.dp, 2.6f)
                            }
                        }
                        GapH(12.dp)
                        ACard(padding = 0.dp, elevation = 6.dp) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                rs.items.forEachIndexed { i, it ->
                                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(12.dp).clip(CircleShape).background(ROLE_DOT[it.ing.role] ?: C.Green))
                                        GapW(12.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text(it.displayName().replaceFirstChar { c -> c.uppercase() }, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                                            val cat = ROLE_NAME[it.ing.role] ?: ""
                                            val base = it.ing.g * servings
                                            val extra = if (adapted && it.ing.role != "x" && abs(it.grams - base) >= 1) " · base ${formatAmount(it.food, base, true)}" else ""
                                            val have = if (s.pantry.isNotEmpty() && it.ing.role != "x") (if (it.food.id in pantry) " · in dispensa" else " · da comprare") else ""
                                            Text(cat + extra + have, style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                                        }
                                        Text(it.amountLabel(), style = T.Num.copy(fontSize = 18.sp))
                                    }
                                    if (i < rs.items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                                }
                            }
                        }
                        Text("Pesi a crudo e al netto degli scarti. Valori nutrizionali per 1 porzione.", style = T.Small.copy(fontSize = 12.sp), modifier = Modifier.padding(top = 8.dp))

                        // Preparazione
                        Row(Modifier.fillMaxWidth().padding(top = 30.dp), verticalAlignment = Alignment.Bottom) {
                            Text("Preparazione", style = T.H2, modifier = Modifier.weight(1f))
                            Text("${done.size} di ${steps.size}", style = T.Chip.copy(fontSize = 13.sp, color = Color(0xFF047857)), modifier = Modifier.padding(bottom = 3.dp))
                        }
                        GapH(10.dp)
                        ABar(if (steps.isEmpty()) 0f else done.size / steps.size.toFloat(), Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF5AD7AF))), Color(0xFFD9F6EA), 8.dp)
                        GapH(14.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            steps.forEachIndexed { i, st ->
                                val d = i in done
                                val img = tpl.stepImages.getOrNull(i)
                                val shape = RoundedCornerShape(26.dp)
                                Row(
                                    Modifier.fillMaxWidth().aShadow(shape, 6.dp, Color(0x801D1B3A)).clip(shape).background(if (d) Color(0xFFE4F8EF) else Color.White)
                                        .clickable { if (d) done.remove(i) else done.add(i) }.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (d) C.Green else C.AccentSoft), contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", style = T.H2.copy(fontSize = 18.sp, color = if (d) Color.White else Color(0xFF8F2A14)))
                                    }
                                    GapW(14.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text("Passaggio ${i + 1}", style = T.Small.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = A.Muted))
                                        GapH(3.dp)
                                        Text(st, style = T.Body.copy(fontSize = 15.sp, lineHeight = 22.sp), modifier = Modifier.alpha(if (d) 0.6f else 1f))
                                        if (img != null) {
                                            GapH(10.dp)
                                            RecipeImage("${tpl.id}_s$i", img, null, Modifier.fillMaxWidth().height(130.dp), "🍳", 18.dp)
                                        }
                                    }
                                }
                            }
                        }
                        if (tpl.tip.isNotBlank()) {
                            GapH(12.dp)
                            ACard(color = A.GreenSoft, radius = 24.dp, padding = 14.dp, elevation = 0.dp, bordered = false) { Text("💡 ${tpl.tip}", style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp)) }
                        }

                        // Attrezzi, sostituzioni, allergeni, informazioni
                        ASection("Attrezzi", top = 30.dp)
                        GapH(8.dp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { RecipeMeta.tools(tpl).forEach { InfoChip(it) } }

                        val subs = RecipeMeta.substitutions(tpl)
                        if (subs.isNotEmpty()) {
                            ASection("Sostituzioni consigliate", top = 26.dp)
                            GapH(8.dp)
                            ACard(padding = 14.dp, elevation = 4.dp) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { subs.forEach { Text("• $it", style = T.Body.copy(fontSize = 14.sp)) } } }
                        }

                        ASection("Allergeni e compatibilità", top = 26.dp)
                        GapH(8.dp)
                        ACard(padding = 14.dp, elevation = 4.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (info.allergens.isEmpty()) "Nessun allergene principale rilevato negli ingredienti."
                                else "Contiene: " + info.allergens.joinToString { RecipeMeta.label(RecipeMeta.ALLERGENS, it) }, style = T.Label)
                                if (info.diets.isNotEmpty()) Text("Diete compatibili: " + RecipeMeta.DIETS.filter { it.first in info.diets }.joinToString { it.second }, style = T.Small.copy(color = A.Ink))
                                if (info.goals.isNotEmpty()) Text("Obiettivi: " + RecipeMeta.GOALS.filter { it.first in info.goals }.joinToString { it.second }, style = T.Small.copy(color = A.Ink))
                                Text("Costo stimato: ${RecipeMeta.costLabel(info.costEur)} (circa ${String.format(java.util.Locale.ITALY, "%.2f", info.costEur)} € a porzione, prezzi medi indicativi). Controlla sempre le etichette dei prodotti confezionati.", style = T.Small)
                            }
                        }

                        // Micronutrienti
                        ASection("Vitamine e minerali del piatto", top = 30.dp)
                        Text("Percentuale del riferimento giornaliero EFSA coperta da una porzione.", style = T.Small.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(top = 4.dp))
                        GapH(12.dp)
                        ACard(padding = 16.dp, elevation = 6.dp) { DishMicros(r, s.profile.male) }
                        Text("Fonti dei dati: USDA FoodData Central. Totale da usare per il diario; le etichette dei prodotti possono differire leggermente.",
                            style = T.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, color = A.Muted), modifier = Modifier.padding(top = 14.dp))

                        Text(if (detail) "Nascondi calcolo dettagliato" else "Mostra calcolo dettagliato e fonti", style = T.Link,
                            modifier = Modifier.padding(top = 10.dp).clickable { detail = !detail }.padding(vertical = 6.dp))
                        if (detail) {
                            ACard(padding = 12.dp, elevation = 4.dp) {
                                Column {
                                    Row { listOf("Ingrediente" to 2f, "kcal" to 1f, "P" to 0.8f, "C" to 0.8f, "G" to 0.8f).forEach { (h, w) -> Text(h, style = T.Small, modifier = Modifier.weight(w)) } }
                                    HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 6.dp))
                                    r.items.forEach { it ->
                                        val m = it.macro
                                        Row(Modifier.padding(vertical = 4.dp)) {
                                            Text("${it.displayName()} ${it.amountLabel()}", style = T.Small.copy(color = C.Text), modifier = Modifier.weight(2f), maxLines = 2)
                                            Text(m.kcal.r0(), style = T.Small.copy(color = C.Text), modifier = Modifier.weight(1f))
                                            Text(m.protein.r1(), style = T.Small.copy(color = C.Text), modifier = Modifier.weight(0.8f))
                                            Text(m.carbs.r1(), style = T.Small.copy(color = C.Text), modifier = Modifier.weight(0.8f))
                                            Text(m.fat.r1(), style = T.Small.copy(color = C.Text), modifier = Modifier.weight(0.8f))
                                        }
                                    }
                                    HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 6.dp))
                                    Row {
                                        Text("Totale", style = T.Label, modifier = Modifier.weight(2f))
                                        Text(r.total.kcal.r0(), style = T.Label, modifier = Modifier.weight(1f))
                                        Text(r.total.protein.r1(), style = T.Label, modifier = Modifier.weight(0.8f))
                                        Text(r.total.carbs.r1(), style = T.Label, modifier = Modifier.weight(0.8f))
                                        Text(r.total.fat.r1(), style = T.Label, modifier = Modifier.weight(0.8f))
                                    }
                                }
                            }
                            GapH(8.dp)
                            r.items.filter { it.ing.role != "x" }.forEach {
                                Text("• ${it.food.name}: USDA FDC ${it.food.fdc} (${it.food.usda})" + (it.food.note?.let { n -> ". $n" } ?: ""), style = T.Small)
                            }
                        }
                        GapH(120.dp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                BackCircle { Nav.back() }
                Box(Modifier.weight(1f))
                if (sc.adHoc == null) Box(Modifier.size(48.dp).aShadow(CircleShape, 6.dp, Color(0x661D1B3A)).clip(CircleShape).background(Color.White).clickable { Store.toggleRecipeFav(tpl.id) }, contentAlignment = Alignment.Center) {
                    AIcon(Ic.Heart, if (fav) C.Protein else A.Ink, 22.dp, 2.2f, fill = fav)
                }
            }
        }
    }
    // Barra azioni fluttuante
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val barShape = RoundedCornerShape(32.dp)
        Column(
            Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 14.dp).fillMaxWidth()
                .aShadow(barShape, 16.dp, Color(0x611D1B3A)).clip(barShape).background(Color(0xEBFFFFFF)).border(1.dp, Color(0xE6FFFFFF), barShape).padding(14.dp)
        ) {
            if (planned) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AInk("Alternativa", Modifier.weight(1f), height = 56.dp, bg = Color(0xFFF6F1EA), fg = A.Ink) { Nav.go(Screen.Alternatives(sc.date, sc.slot!!)) }
                    APrimary(if (eaten) "✓ Mangiato" else "Segna mangiato", Modifier.weight(1.3f), height = 56.dp) {
                        val pm = Store.plannedMeals(Store.current, sc.date).firstOrNull { it.slotIndex == sc.slot }
                        if (eaten) Store.uneatPlanned(sc.date, sc.slot!!)
                        else if (pm != null) { Store.eatPlanned(sc.date, pm); Store.consumeFromPantry(pm.solved) }
                    }
                }
            } else {
                MealChips(meal) { meal = it }
                GapH(10.dp)
                val btn = RoundedCornerShape(28.dp)
                Row(
                    Modifier.fillMaxWidth().height(56.dp).aShadow(btn, 10.dp, Color(0x73F25E00)).clip(btn).background(C.OrangeBrush).clickable {
                        Store.eatSolved(sc.date, meal, r)
                        Store.consumeFromPantry(r)
                        Toast.makeText(ctx, "Aggiunto al diario (1 porzione)", Toast.LENGTH_SHORT).show()
                        Nav.back()
                    },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    AIcon(Ic.Plus, Color.White, 20.dp, 2.8f); GapW(10.dp)
                    Text("Aggiungi a ${MEAL_LABEL.getValue(meal).lowercase()}", style = T.BtnL.copy(color = Color.White))
                }
            }
        }
    }

    if (dlgPlan) PlanDialog(tpl.id, tpl.meal, onDismiss = { dlgPlan = false })
    if (dlgFridge) {
        val need = RecipeSearch.fridgeCoverage(db, tpl, pantry)
        val miss = RecipeSearch.missing(db, tpl, pantry)
        val have = rs.items.filter { it.ing.role != "x" && it.food.id in pantry }
        AlertDialog(
            onDismissRequest = { dlgFridge = false }, containerColor = C.Surface,
            title = { Text("Ho gli ingredienti?", style = T.Title) },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    if (s.pantry.isEmpty()) {
                        Text("La dispensa è vuota. Fotografa il frigo o lo scontrino e Jarvis riconosce i prodotti.", style = T.Body)
                    } else {
                        Text("Hai il ${(need * 100).toInt()}% degli ingredienti principali (sale, pepe e olio si danno per scontati).", style = T.Label)
                        if (have.isNotEmpty()) { GapH(8.dp); Text("Ce li hai: " + have.joinToString { it.displayName() }, style = T.Small.copy(color = A.GreenInk)) }
                        if (miss.isNotEmpty()) { GapH(8.dp); Text("Ti mancano: " + miss.joinToString { (it.label ?: db.food(it.food)?.name ?: it.food).lowercase() }, style = T.Small.copy(color = A.TonalInk)) }
                        else { GapH(8.dp); Text("Hai tutto: puoi cucinarla subito.", style = T.Small.copy(color = A.GreenInk)) }
                    }
                }
            },
            confirmButton = {
                if (s.pantry.isEmpty()) TextButton(onClick = { dlgFridge = false; Nav.go(Screen.PantryScan("foto")) }) { Text("Fotografa il frigo", color = C.AccentDeep) }
                else if (miss.isNotEmpty()) TextButton(onClick = {
                    val n = Store.addRecipeToShopping(rs, tpl.name, pantry)
                    Toast.makeText(ctx, "Aggiunti $n ingredienti alla spesa", Toast.LENGTH_SHORT).show(); dlgFridge = false
                }) { Text("Aggiungi mancanti alla spesa", color = C.AccentDeep) }
                else TextButton(onClick = { dlgFridge = false }) { Text("Ok", color = C.AccentDeep) }
            },
            dismissButton = { TextButton(onClick = { dlgFridge = false }) { Text("Chiudi", color = C.Muted) } }
        )
    }
    }
}

@Composable
private fun PlanDialog(recipeId: String, mealKey: String, onDismiss: () -> Unit) {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val slots = s.dietPrefs.slots
    val key = if (mealKey == "dessert") "spuntino" else mealKey
    var slotIdx by remember { mutableIntStateOf(slots.indexOfFirst { it.meal == key }.coerceAtLeast(0)) }
    var dayOff by remember { mutableIntStateOf(0) }
    val days = remember { (0..6).map { LocalDate.now().plusDays(it.toLong()) } }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = C.Surface,
        title = { Text("Aggiungi al piano", style = T.Title) },
        text = {
            Column {
                Text("Giorno", style = T.Label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.forEachIndexed { i, d -> Pill(dayLabel(d), dayOff == i) { dayOff = i } }
                }
                Text("Pasto", style = T.Label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEachIndexed { i, sl -> Pill(sl.name, slotIdx == i) { slotIdx = i } }
                }
                Text("La ricetta sostituisce quella già prevista in quel pasto e viene calcolata sui tuoi obiettivi del giorno.", style = T.Small, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Store.planRecipe(days[dayOff], slotIdx, recipeId)
                Toast.makeText(ctx, "Aggiunta al piano: ${dayLabel(days[dayOff]).lowercase()}", Toast.LENGTH_SHORT).show(); onDismiss()
            }) { Text("Aggiungi", color = C.AccentDeep) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = C.Muted) } }
    )
}

@Composable
private fun RecipeMacroBar(label: String, v: Double, target: Double, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = T.Chip.copy(fontWeight = FontWeight.SemiBold))
            Text("${v.r0()} / ${target.r0()} g", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
        }
        GapH(5.dp)
        ABar((v / target.coerceAtLeast(1.0)).toFloat(), Brush.horizontalGradient(listOf(color, color)), Color(0xD9FFFFFF), 7.dp)
    }
}
