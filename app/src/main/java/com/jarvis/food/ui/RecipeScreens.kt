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
            MealChips(meal) { meal = it }
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
