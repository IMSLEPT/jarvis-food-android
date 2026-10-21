package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Food
import com.jarvis.food.data.FoodBase
import com.jarvis.food.data.Ingredient
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

private val CATS = listOf("Tutte", "Colazione", "Pranzo", "Cena", "Spuntino", "Le mie")

@Composable
fun RecipesScreen() {
    val s by Store.state.collectAsState()
    var cat by remember { mutableStateOf("Tutte") }
    var q by remember { mutableStateOf("") }
    val all = s.recipes + FoodBase.recipes
    val list = all.filter {
        (cat == "Tutte" || (cat == "Le mie" && !it.builtin) || it.category == cat) &&
                (q.isBlank() || it.name.contains(q.trim(), ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ricette", style = T.Display)
                Text("${all.size} ricette · valori calcolati dagli ingredienti", style = T.Muted)
            }
            PrimaryButton("+ Nuova") { Nav.go(Screen.RecipeEditor(null)) }
        }
        Spacer(Modifier.height(12.dp))
        JField(q, { q = it }, "Cerca ricetta")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATS.forEach { c -> Pill(c, cat == c) { cat = c } }
        }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) EmptyState("🍳", "Nessuna ricetta", "Crea la tua prima ricetta con il pulsante + Nuova.")
        list.forEach { r ->
            RecipeCard(r)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecipeCard(r: Recipe) {
    val t = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
    JCard(onClick = { Nav.go(Screen.RecipeDetail(r)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.SurfaceHi), contentAlignment = Alignment.Center) {
                Text(r.emoji, style = T.Title)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = T.Title2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${t.kcal.r0()} kcal a porzione · ${r.minutes} min" + if (!r.builtin) " · mia" else "", style = T.Small)
                Spacer(Modifier.height(4.dp))
                MacroDots(t.protein, t.carbs, t.fat)
            }
        }
    }
}

@Composable
fun RecipeDetailScreen(recipe: Recipe) {
    val s by Store.state.collectAsState()
    val r = s.recipes.firstOrNull { it.id == recipe.id } ?: recipe
    var portions by remember { mutableStateOf("1") }
    val defaultMeal = when (LocalTime.now().hour) { in 5..10 -> 0; in 11..15 -> 1; in 18..23 -> 2; else -> 3 }
    var meal by remember { mutableIntStateOf(defaultMeal) }
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
    val food = Nutrition.recipeAsFood(r)

    Column(Modifier.fillMaxSize()) {
        TopBar(r.name, onBack = { Nav.back() }) {
            if (!r.builtin) TextButton(onClick = { Nav.go(Screen.RecipeEditor(r)) }) { Text("Modifica", color = C.Accent) }
            else TextButton(onClick = {
                Nav.go(Screen.RecipeEditor(r.copy(id = Store.newId(), builtin = false, name = r.name + " (mia)")))
            }) { Text("Duplica", color = C.Accent) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(r.emoji, style = T.Big)
            Text(r.name, style = T.Display)
            Text("${r.servings} porzioni · ${r.minutes} min · ${r.category}", style = T.Muted)
            Spacer(Modifier.height(14.dp))
            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacroDonut(per.carbs, per.protein, per.fat)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${per.kcal.r0()} kcal", style = T.Big.copy(color = C.Accent))
                        Text("a porzione (${(Nutrition.recipeGrams(r) / r.servings).r0()} g)", style = T.Muted)
                        Spacer(Modifier.height(4.dp))
                        MacroDots(per.protein, per.carbs, per.fat)
                    }
                }
            }
            SectionTitle("Ingredienti")
            JCard {
                r.ingredients.forEach { i ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(i.food.name, style = T.Body, modifier = Modifier.weight(1f))
                        Text("${i.grams.r0()} ${if (i.food.liquid) "ml" else "g"}", style = T.Label)
                    }
                }
            }
            if (r.steps.isNotEmpty()) {
                SectionTitle("Preparazione")
                r.steps.forEachIndexed { idx, st ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(50)).background(C.Accent), contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", style = T.Label.copy(color = C.OnAccent))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(st, style = T.Body)
                    }
                }
            }
            SectionTitle("Aggiungi al diario di oggi")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MEAL_NAMES.forEachIndexed { i, n -> Pill(n.take(8), meal == i) { meal = i } }
            }
            Spacer(Modifier.height(10.dp))
            JField(portions, { portions = it }, "Porzioni", number = true)
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Aggiungi a ${MEAL_NAMES[meal]}", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            enabled = (parseNum(portions) ?: 0.0) > 0) {
            Store.addEntry(LocalDate.now(), meal, food, (parseNum(portions) ?: 1.0) * (food.servingGrams ?: 100.0))
            Nav.back()
            Nav.tab = 0
            Nav.diaryDate = LocalDate.now()
        }
    }
}

@Composable
fun RecipeEditorScreen(recipe: Recipe?) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var emoji by remember { mutableStateOf(recipe?.emoji ?: "🍽️") }
    var servings by remember { mutableStateOf((recipe?.servings ?: 1).toString()) }
    var minutes by remember { mutableStateOf((recipe?.minutes ?: 15).toString()) }
    var category by remember { mutableStateOf(recipe?.category?.ifBlank { "Pranzo" } ?: "Pranzo") }
    var steps by remember { mutableStateOf(recipe?.steps?.joinToString("\n") ?: "") }
    val ingredients = remember { mutableStateListOf<Ingredient>().apply { addAll(recipe?.ingredients ?: emptyList()) } }
    var picker by remember { mutableStateOf(false) }
    val emojis = listOf("🍽️", "🥗", "🍝", "🍗", "🥩", "🐟", "🍳", "🥣", "🥞", "🍕", "🌯", "🥪", "🍲", "🍛", "🍰", "🥤")

    val rid = remember { recipe?.id ?: Store.newId() }
    val r = Recipe(
        id = rid, name = name.trim(), servings = servings.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        ingredients = ingredients.toList(), steps = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
        minutes = minutes.toIntOrNull() ?: 0, category = category, emoji = emoji, builtin = false
    )
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings)

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (recipe == null) "Nuova ricetta" else "Modifica ricetta", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JField(name, { name = it }, "Nome della ricetta *")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                emojis.forEach { e ->
                    Text(e, style = T.Title, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (emoji == e) C.Accent.copy(alpha = 0.25f) else C.Surface).clickable { emoji = e }.padding(8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(servings, { servings = it.filter { c -> c.isDigit() } }, "Porzioni", Modifier.weight(1f), number = true)
                JField(minutes, { minutes = it.filter { c -> c.isDigit() } }, "Minuti", Modifier.weight(1f), number = true)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Colazione", "Pranzo", "Cena", "Spuntino").forEach { c -> Pill(c, category == c) { category = c } }
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
            if (ingredients.isNotEmpty()) JCard {
                Text("A porzione: ${per.kcal.r0()} kcal", style = T.Title2.copy(color = C.Accent))
                MacroDots(per.protein, per.carbs, per.fat)
            }
            SectionTitle("Preparazione")
            JField(steps, { steps = it }, "Un passaggio per riga", singleLine = false, modifier = Modifier.heightIn(min = 120.dp))
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipe != null && !recipe.builtin && Store.current.recipes.any { it.id == recipe.id })
                GhostButton("Elimina", Modifier.weight(1f), color = C.Danger) { Store.deleteRecipe(recipe.id); Nav.back(); Nav.back() }
            PrimaryButton("Salva ricetta", Modifier.weight(2f), enabled = name.isNotBlank() && ingredients.isNotEmpty()) {
                Store.saveRecipe(r); Nav.back()
            }
        }
    }

    if (picker) IngredientPicker(onDismiss = { picker = false }) { food ->
        ingredients.add(Ingredient(food, food.servingGrams ?: 100.0)); picker = false
    }
}

@Composable
fun IngredientPicker(onDismiss: () -> Unit, onPick: (Food) -> Unit) {
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val s = Store.current
    val local = FoodBase.search(q, s.customFoods + s.favorites)
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
                    (local.take(25) + online).distinctBy { it.id }.forEach { f ->
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
