package com.jarvis.food.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Db
import com.jarvis.food.data.Food
import com.jarvis.food.data.Ingredient
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Solver
import com.jarvis.food.engine.formatAmount
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

private val ROLE_DOT = mapOf("p" to C.Protein, "c" to C.Carbs, "f" to C.Fat, "v" to C.Green, "x" to Color(0xFFB8B4CC))
private val ROLE_NAME = mapOf("p" to "Proteine", "c" to "Carboidrati", "f" to "Grassi", "v" to "Verdure", "x" to "Condimento")

/** Ricetta completa (design Aurora): foto a tutta larghezza, foglio arrotondato, valori, ingredienti, passaggi, vitamine. */
@Composable
fun RecipeViewScreen(sc: Screen.RecipeView) {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val tpl = sc.adHoc ?: Db.db.recipe(sc.recipeId)
    if (tpl == null) { Column { TopBar("Ricetta", onBack = { Nav.back() }); EmptyState("🤔", "Ricetta non trovata", "") }; return }
    val r = remember(tpl, sc.target) { Solver.solve(tpl, Db.db, sc.target) }
    val steps = remember(r) { r.steps() }
    val done = remember(tpl.id) { mutableStateListOf<Int>() }
    var meal by remember { mutableIntStateOf(sc.diaryMeal) }
    var detail by remember { mutableStateOf(false) }
    val slot = sc.slot?.let { sl -> Store.dayPlan(s, sc.date)?.slots?.firstOrNull { it.slot == sl } }
    val eaten = slot?.eatenEntryIds?.isNotEmpty() == true
    val planned = sc.slot != null && sc.adHoc == null

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth()) {
                    RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.fillMaxWidth().height(470.dp), "🍽️", 0.dp)
                    Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color(0x80181430), Color.Transparent))))
                    tpl.credit?.takeIf { it.author.isNotBlank() }?.let { cr ->
                        Text(cr.author.take(40), style = T.Small.copy(color = Color.White),
                            modifier = Modifier.padding(start = 16.dp, top = 352.dp).clip(RoundedCornerShape(50))
                                .background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    // Foglio
                    Column(
                        Modifier.padding(top = 400.dp).fillMaxWidth()
                            .aShadow(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp), 14.dp, Color(0x591D1B3A))
                            .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(Color(0xFFF3F5F8))
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(A.GreenSoft).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(tpl.meal.replaceFirstChar { it.uppercase() }, style = T.Chip.copy(color = A.GreenInk))
                            }
                            Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(15.dp)).padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                AIcon(Ic.Clock, A.Ink, 14.dp, 2.2f); GapW(6.dp); Text("${tpl.minutes} min", style = T.Chip)
                            }
                            Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(15.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text("1 persona", style = T.Chip)
                            }
                        }
                        GapH(14.dp)
                        Text(tpl.name, style = T.H1.copy(fontSize = 33.sp, lineHeight = 37.sp))
                        GapH(8.dp)
                        Text("Ricetta calcolata sul tuo obiettivo del pasto. Pesi degli ingredienti a crudo.", style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp, color = A.Muted))

                        // Valori nutrizionali
                        GapH(20.dp)
                        ACard(radius = 32.dp, padding = 18.dp, glow = Color(0x8C10B981),
                            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFEDF1F5)))) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val frac = (r.total.kcal / sc.target.kcal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                                    Box(Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                                        Canvas(Modifier.fillMaxSize()) {
                                            val sw = 14.dp.toPx(); val inset = sw / 2 + 4.dp.toPx()
                                            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                                            val sz = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset)
                                            drawArc(Color(0xD9FFFFFF), 135f, 270f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                                            drawArc(Brush.linearGradient(listOf(Color(0xFFF28B7A), Color(0xFFF59E6C), Color(0xFFF8C58A)),
                                                androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, 0f)),
                                                135f, 270f * frac, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(r.total.kcal.r0(), style = T.H1.copy(fontSize = 32.sp, lineHeight = 34.sp))
                                            Text("kcal per te", style = T.Small.copy(fontSize = 11.5.sp, color = A.Muted))
                                        }
                                    }
                                    GapW(16.dp)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                        RecipeMacroBar("Proteine", r.total.protein, sc.target.protein, C.Protein)
                                        RecipeMacroBar("Carboidrati", r.total.carbs, sc.target.carbs, C.Carbs)
                                        RecipeMacroBar("Grassi", r.total.fat, sc.target.fat, C.Fat)
                                    }
                                }
                                GapH(14.dp)
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xB3FFFFFF)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text("Obiettivo del pasto: ${sc.target.kcal.r0()} kcal · P ${sc.target.protein.r0()} · C ${sc.target.carbs.r0()} · G ${sc.target.fat.r0()}",
                                        style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                                }
                            }
                        }

                        // Ingredienti
                        ASection("Ingredienti", top = 30.dp)
                        GapH(12.dp)
                        ACard(padding = 0.dp, elevation = 6.dp) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                r.items.forEachIndexed { i, it ->
                                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(12.dp).clip(CircleShape).background(ROLE_DOT[it.ing.role] ?: C.Green))
                                        GapW(12.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text(it.displayName().replaceFirstChar { c -> c.uppercase() }, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                                            val cat = ROLE_NAME[it.ing.role] ?: ""
                                            val extra = if (it.ing.role != "x" && kotlin.math.abs(it.grams - it.ing.g) >= 1) " · base ${formatAmount(it.food, it.ing.g, true)}" else ""
                                            Text(cat + extra, style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                                        }
                                        Text(it.amountLabel(), style = T.Num.copy(fontSize = 18.sp))
                                    }
                                    if (i < r.items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                                }
                            }
                        }

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

                        // Micronutrienti
                        ASection("Vitamine e minerali del piatto", top = 30.dp)
                        Text("Percentuale del riferimento giornaliero EFSA coperta da questo piatto.", style = T.Small.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(top = 4.dp))
                        GapH(12.dp)
                        ACard(padding = 16.dp, elevation = 6.dp) { DishMicros(r, s.profile.male) }
                        Text("Fonti dei dati: USDA FoodData Central. Totale da usare per il diario; le etichette dei prodotti possono differire leggermente.",
                            style = T.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, color = A.Muted), modifier = Modifier.padding(top = 14.dp))

                        // Dettaglio del calcolo (a richiesta)
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
            // Pulsanti sopra la foto
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp)) { BackCircle { Nav.back() } }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MEAL_ORDER.forEach { m ->
                        val sel = m == meal
                        Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(if (sel) C.Navy else Color(0xFFF6F1EA)).clickable { meal = m },
                            contentAlignment = Alignment.Center) { Text(MEAL_LABEL.getValue(m), style = T.Chip.copy(color = if (sel) Color.White else A.Ink), maxLines = 1) }
                    }
                }
                GapH(10.dp)
                val btn = RoundedCornerShape(28.dp)
                Row(
                    Modifier.fillMaxWidth().height(56.dp).aShadow(btn, 10.dp, Color(0x73F25E00)).clip(btn).background(C.OrangeBrush).clickable {
                        Store.eatSolved(sc.date, meal, r)
                        Store.consumeFromPantry(r)
                        Toast.makeText(ctx, "Aggiunto al diario", Toast.LENGTH_SHORT).show()
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
    }
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

/** Catalogo ricette (database + mie), usato nella scheda Cucina. */
@Composable
fun RecipeCatalog() {
    val s by Store.state.collectAsState()
    var meal by remember { mutableStateOf("tutte") }
    var q by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val energy = Store.energyFor(s, today)
    val list = Db.db.recipes.filter { (meal == "tutte" || it.meal == meal) && (q.isBlank() || it.name.contains(q.trim(), true)) }
    Column {
        JField(q, { q = it }, "Cerca ricetta")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("tutte" to "Tutte", "colazione" to "Colazione", "pranzo" to "Pranzo", "cena" to "Cena", "spuntino" to "Spuntini", "mie" to "Le mie")
                .forEach { (k, l) -> Pill(l, meal == k) { meal = k } }
        }
        Spacer(Modifier.height(12.dp))
        if (meal == "mie") {
            PrimaryButton("+ Nuova ricetta", Modifier.fillMaxWidth()) { Nav.go(Screen.RecipeEditor(null)) }
            if (s.recipes.isEmpty()) EmptyState("🧑‍🍳", "Nessuna ricetta tua", "Crea una ricetta: i valori si calcolano dagli ingredienti.")
            s.recipes.forEach { r ->
                Spacer(Modifier.height(10.dp))
                val t = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
                JCard(onClick = { Nav.go(Screen.UserRecipeDetail(r)) }) {
                    Text("${r.emoji}  ${r.name}", style = T.Title2)
                    Text("${t.kcal.r0()} kcal a porzione · ${r.minutes} min", style = T.Small)
                }
            }
            return@Column
        }
        list.forEach { tpl ->
            val slotIdx = s.dietPrefs.slots.indexOfFirst { it.meal == tpl.meal }.coerceAtLeast(0)
            val target = com.jarvis.food.engine.Planner.slotTargets(energy.macro, s.dietPrefs)[slotIdx]
            val solved = remember(tpl.id, target) { Solver.solve(tpl, Db.db, target) }
            val diaryMeal = s.dietPrefs.slots.getOrNull(slotIdx)?.diaryMeal ?: 1
            JCard(padding = PaddingValues(12.dp), onClick = { Nav.go(Screen.RecipeView(tpl.id, target, today, null, diaryMeal)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.size(84.dp), "🍽️", 16.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tpl.meal.replaceFirstChar { it.uppercase() } + " · ${tpl.minutes} min", style = T.Small)
                        Text(tpl.name, style = T.Dish.copy(fontSize = T.Title2.fontSize), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${solved.total.kcal.r0()} kcal per te", style = T.Label.copy(color = C.AccentDeep))
                        MacroDots(solved.total.protein, solved.total.carbs, solved.total.fat)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun UserRecipeDetailScreen(recipe: Recipe) {
    val s by Store.state.collectAsState()
    val r = s.recipes.firstOrNull { it.id == recipe.id } ?: recipe
    var portions by remember { mutableStateOf("1") }
    val defaultMeal = when (LocalTime.now().hour) { in 5..10 -> 0; in 11..15 -> 1; in 18..23 -> 2; else -> 3 }
    var meal by remember { mutableIntStateOf(defaultMeal) }
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
    val food = Nutrition.recipeAsFood(r)
    Column(Modifier.fillMaxSize()) {
        TopBar(r.name, onBack = { Nav.back() }) { TextButton(onClick = { Nav.go(Screen.RecipeEditor(r)) }) { Text("Modifica", color = C.AccentDeep) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(r.emoji, style = T.Big)
            Text("${r.servings} porzioni · ${r.minutes} min", style = T.Muted)
            Spacer(Modifier.height(12.dp))
            JCard {
                Text("${per.kcal.r0()} kcal a porzione", style = T.Num.copy(color = C.AccentDeep))
                Spacer(Modifier.height(8.dp))
                MacroTiles(per.protein, per.carbs, per.fat)
            }
            SectionTitle("Ingredienti")
            JCard { r.ingredients.forEach { i -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(i.food.name, style = T.Body, modifier = Modifier.weight(1f)); Text("${i.grams.r0()} ${if (i.food.liquid) "ml" else "g"}", style = T.Label) } } }
            if (r.steps.isNotEmpty()) {
                SectionTitle("Preparazione")
                r.steps.forEachIndexed { idx, st -> Text("${idx + 1}. $st", style = T.Read, modifier = Modifier.padding(vertical = 4.dp)) }
            }
            SectionTitle("Aggiungi al diario di oggi")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MEAL_NAMES.forEachIndexed { i, n -> Pill(n.take(8), meal == i) { meal = i } } }
            Spacer(Modifier.height(10.dp))
            JField(portions, { portions = it }, "Porzioni", number = true)
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Aggiungi a ${MEAL_NAMES[meal]}", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = (parseNum(portions) ?: 0.0) > 0) {
            Store.addEntry(LocalDate.now(), meal, food, (parseNum(portions) ?: 1.0) * (food.servingGrams ?: 100.0))
            Nav.back(); Nav.tab = 0; Nav.diaryDate = LocalDate.now()
        }
    }
}

@Composable
fun RecipeEditorScreen(recipe: Recipe?) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var emoji by remember { mutableStateOf(recipe?.emoji ?: "🍽️") }
    var servings by remember { mutableStateOf((recipe?.servings ?: 1).toString()) }
    var minutes by remember { mutableStateOf((recipe?.minutes ?: 15).toString()) }
    var steps by remember { mutableStateOf(recipe?.steps?.joinToString("\n") ?: "") }
    val ingredients = remember { mutableStateListOf<Ingredient>().apply { addAll(recipe?.ingredients ?: emptyList()) } }
    var picker by remember { mutableStateOf(false) }
    val rid = remember { recipe?.id ?: Store.newId() }
    val emojis = listOf("🍽️", "🥗", "🍝", "🍗", "🥩", "🐟", "🍳", "🥣", "🥞", "🍕", "🌯", "🥪", "🍲", "🍛", "🍰", "🥤")
    val r = Recipe(rid, name.trim(), servings.toIntOrNull()?.coerceAtLeast(1) ?: 1, ingredients.toList(),
        steps.lines().map { it.trim() }.filter { it.isNotEmpty() }, minutes.toIntOrNull() ?: 0, "", emoji)
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings)
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (recipe == null) "Nuova ricetta" else "Modifica ricetta", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JField(name, { name = it }, "Nome della ricetta *")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                emojis.forEach { e -> Text(e, style = T.Title, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (emoji == e) C.AccentSoft else C.Surface).clickable { emoji = e }.padding(8.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(servings, { servings = it.filter { c -> c.isDigit() } }, "Porzioni", Modifier.weight(1f), number = true)
                JField(minutes, { minutes = it.filter { c -> c.isDigit() } }, "Minuti", Modifier.weight(1f), number = true)
            }
            SectionTitle("Ingredienti", action = "+ Aggiungi") { picker = true }
            if (ingredients.isEmpty()) Text("Aggiungi almeno un ingrediente.", style = T.Muted)
            ingredients.forEachIndexed { idx, ing ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ing.food.name, style = T.Label, modifier = Modifier.weight(1f), maxLines = 2)
                    JField(ing.grams.r0(), { v -> parseNum(v)?.let { ingredients[idx] = ing.copy(grams = it) } }, "g", Modifier.width(90.dp), number = true)
                    Text("✕", style = T.Title2.copy(color = C.Faint), modifier = Modifier.clickable { ingredients.removeAt(idx) }.padding(10.dp))
                }
            }
            if (ingredients.isNotEmpty()) JCard { Text("A porzione: ${per.kcal.r0()} kcal", style = T.Title2.copy(color = C.AccentDeep)); MacroDots(per.protein, per.carbs, per.fat) }
            SectionTitle("Preparazione")
            JField(steps, { steps = it }, "Un passaggio per riga", singleLine = false, modifier = Modifier.heightIn(min = 120.dp))
        }
        Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipe != null && Store.current.recipes.any { it.id == recipe.id })
                GhostButton("Elimina", Modifier.weight(1f), color = C.Danger) { Store.deleteRecipe(recipe.id); Nav.back(); Nav.back() }
            PrimaryButton("Salva ricetta", Modifier.weight(2f), enabled = name.isNotBlank() && ingredients.isNotEmpty()) { Store.saveRecipe(r); Nav.back() }
        }
    }
    if (picker) IngredientPicker(onDismiss = { picker = false }) { food -> ingredients.add(Ingredient(food, food.servingGrams ?: 100.0)); picker = false }
}

@Composable
fun IngredientPicker(onDismiss: () -> Unit, onPick: (Food) -> Unit) {
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val local = Db.db.search(q, 25).map { it.toFood() }
    LaunchedEffect(q) {
        online = emptyList()
        if (q.trim().length < 3) return@LaunchedEffect
        delay(600); loading = true
        online = runCatching { OpenFoodFacts.search(q) }.getOrDefault(emptyList())
        loading = false
    }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = C.Surface,
        title = { Text("Aggiungi ingrediente", style = T.Title) },
        text = {
            Column {
                JField(q, { q = it }, "Cerca")
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    (local + online).distinctBy { it.id }.forEach { f ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(f) }.padding(vertical = 9.dp, horizontal = 4.dp)) {
                            Text(f.name, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${f.kcal.r0()} kcal/100 g" + if (f.brand.isNotBlank()) " · ${f.brand}" else "", style = T.Small)
                        }
                    }
                    if (loading) CircularProgressIndicator(Modifier.padding(8.dp).size(18.dp), color = C.Accent, strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi", color = C.Muted) } }
    )
}
