package com.jarvis.food.ui

import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Ricetta completa: foto, valori, ingredienti calcolati, preparazione passo passo, calcolo dettagliato e fonti. */
@Composable
fun RecipeViewScreen(sc: Screen.RecipeView) {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val tpl = sc.adHoc ?: Db.db.recipe(sc.recipeId)
    if (tpl == null) { Column { TopBar("Ricetta", onBack = { Nav.back() }); EmptyState("🤔", "Ricetta non trovata", "") }; return }
    val r = remember(tpl, sc.target) { Solver.solve(tpl, Db.db, sc.target) }
    val checked = remember { mutableStateListOf<String>() }
    val slot = sc.slot?.let { sl -> Store.dayPlan(s, sc.date)?.slots?.firstOrNull { it.slot == sl } }
    val eaten = slot?.eatenEntryIds?.isNotEmpty() == true

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box {
                RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.fillMaxWidth().height(300.dp), "🍽️", 0.dp)
                Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent))))
                Box(Modifier.statusBarsPadding().padding(10.dp).size(44.dp).clip(CircleShape).background(C.Surface).clickable { Nav.back() },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = C.Text) }
                tpl.credit?.takeIf { it.author.isNotBlank() }?.let { cr ->
                    Text(cr.author.take(40), style = T.Small.copy(color = Color.White),
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(50))
                            .background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(tpl.name, style = T.DishBig)
                Spacer(Modifier.height(6.dp))
                Text("Ricetta per 1 persona, calcolata sul tuo obiettivo del pasto. Pesi degli ingredienti a crudo.", style = T.Read.copy(color = C.Muted))

                SectionTitle("Valori nutrizionali")
                JCard {
                    Row {
                        Column(Modifier.weight(1f)) { Text("Obiettivo del pasto", style = T.Small); Text("${sc.target.kcal.r0()} kcal", style = T.Num) }
                        Column(horizontalAlignment = Alignment.End) { Text("Ricetta calcolata", style = T.Small); Text("${r.total.kcal.r0()} kcal", style = T.Num.copy(color = C.AccentDeep)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    MacroTiles(r.total.protein, r.total.carbs, r.total.fat)
                    Spacer(Modifier.height(8.dp))
                    Text("Tempo: ${tpl.minutes} minuti · obiettivo P ${sc.target.protein.r0()} · C ${sc.target.carbs.r0()} · G ${sc.target.fat.r0()} g", style = T.Small)
                }

                SectionTitle("Ingredienti calcolati")
                JCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    r.items.forEachIndexed { i, it ->
                        val done = it.food.id in checked
                        Row(Modifier.fillMaxWidth().clickable { if (done) checked.remove(it.food.id) else checked.add(it.food.id) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(if (done) C.Green else C.Surface).border(1.5.dp, if (done) C.Green else C.Faint, CircleShape),
                                contentAlignment = Alignment.Center) { if (done) Icon(Icons.Filled.Check, null, tint = C.OnAccent, modifier = Modifier.size(15.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.displayName().replaceFirstChar { c -> c.uppercase() }, style = T.Label)
                                if (it.ing.role != "x" && kotlin.math.abs(it.grams - it.ing.g) >= 1)
                                    Text("ricetta base ${formatAmount(it.food, it.ing.g, true)}", style = T.Small)
                                else if (it.ing.role == "x") Text("quantità fissa", style = T.Small)
                            }
                            Text(it.amountLabel(), style = T.Title2)
                        }
                        if (i < r.items.lastIndex) HorizontalDivider(color = C.Line)
                    }
                }

                SectionTitle("Preparazione passo per passo")
                r.steps().forEachIndexed { i, st ->
                    val img = tpl.stepImages.getOrNull(i)
                    Row(Modifier.padding(vertical = 9.dp)) {
                        if (img != null) {
                            Box {
                                RecipeImage("${tpl.id}_s$i", img, null, Modifier.size(width = 112.dp, height = 112.dp), "🍳", 16.dp)
                                Box(Modifier.padding(6.dp).size(26.dp).clip(CircleShape).background(C.Surface), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = T.Label.copy(color = C.AccentDeep))
                                }
                            }
                        } else {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(C.AccentSoft), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = T.Title2.copy(color = C.AccentDeep))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Passaggio ${i + 1}", style = T.Title2)
                            Spacer(Modifier.height(2.dp))
                            Text(st, style = T.Read)
                        }
                    }
                }
                if (tpl.tip.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    JCard(color = C.GreenSoft) { Text("💡 ${tpl.tip}", style = T.Read) }
                }

                SectionTitle("Calcolo nutrizionale dettagliato")
                JCard(padding = PaddingValues(12.dp)) {
                    Row { listOf("Ingrediente" to 2f, "kcal" to 1f, "P" to 0.8f, "C" to 0.8f, "G" to 0.8f).forEach { (h, w) ->
                        Text(h, style = T.Small, modifier = Modifier.weight(w)) } }
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
                Text("Totale da usare per il diario. Le etichette dei prodotti possono differire leggermente.", style = T.Small.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(top = 6.dp))

                SectionTitle("Vitamine e minerali del piatto")
                JCard { DishMicros(r, s.profile.male) }

                SectionTitle("Fonti dei dati")
                r.items.filter { it.ing.role != "x" }.forEach {
                    Text("• ${it.food.name}: USDA FDC ${it.food.fdc} (${it.food.usda})" + (it.food.note?.let { n -> ". $n" } ?: ""), style = T.Small)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth().background(C.Bg).navigationBarsPadding().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (sc.slot != null && sc.adHoc == null) {
                GhostButton("Trova alternativa", Modifier.weight(1f)) { Nav.go(Screen.Alternatives(sc.date, sc.slot)) }
                PrimaryButton(if (eaten) "✓ Mangiato" else "Segna mangiato", Modifier.weight(1f), color = if (eaten) C.Green else C.Ink) {
                    val pm = Store.plannedMeals(Store.current, sc.date).firstOrNull { it.slotIndex == sc.slot }
                    if (eaten) Store.uneatPlanned(sc.date, sc.slot)
                    else if (pm != null) { Store.eatPlanned(sc.date, pm); Store.consumeFromPantry(pm.solved) }
                }
            } else {
                PrimaryButton("Aggiungi a ${MEAL_NAMES[sc.diaryMeal]}", Modifier.fillMaxWidth()) {
                    Store.eatSolved(sc.date, sc.diaryMeal, r)
                    Store.consumeFromPantry(r)
                    Toast.makeText(ctx, "Aggiunto al diario", Toast.LENGTH_SHORT).show()
                    Nav.back()
                }
            }
        }
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
