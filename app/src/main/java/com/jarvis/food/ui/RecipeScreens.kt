package com.jarvis.food.ui

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.ai.ImportedRecipe
import com.jarvis.food.ai.Ocr
import com.jarvis.food.ai.RecipeImport
import com.jarvis.food.engine.RecipeMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

private fun shareText(r: Recipe, per: com.jarvis.food.data.Totals): String = buildString {
    appendLine(r.name)
    appendLine("${r.servings} porzioni · ${r.minutes} min · ${per.kcal.r0()} kcal a porzione (P ${per.protein.r0()} g · C ${per.carbs.r0()} g · G ${per.fat.r0()} g)")
    if (r.description.isNotBlank()) { appendLine(); appendLine(r.description) }
    appendLine(); appendLine("INGREDIENTI")
    r.ingredients.forEach { appendLine("- ${it.food.name}: ${it.grams.r0()} ${if (it.food.liquid) "ml" else "g"}") }
    if (r.steps.isNotEmpty()) { appendLine(); appendLine("PREPARAZIONE"); r.steps.forEachIndexed { i, st -> appendLine("${i + 1}. $st") } }
    appendLine(); append("Creata con Jarvis Food")
}

@Composable
fun UserRecipeDetailScreen(recipe: Recipe) {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val r = s.recipes.firstOrNull { it.id == recipe.id } ?: recipe
    var portions by remember { mutableStateOf("1") }
    var people by remember(r.servings) { mutableIntStateOf(r.servings.coerceAtLeast(1)) }
    val defaultMeal = when (LocalTime.now().hour) { in 5..10 -> 0; in 11..15 -> 1; in 18..23 -> 2; else -> 3 }
    var meal by remember { mutableIntStateOf(defaultMeal) }
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
    val food = Nutrition.recipeAsFood(r)
    val k = people.toDouble() / r.servings.coerceAtLeast(1)
    Column(Modifier.fillMaxSize()) {
        TopBar(r.name, onBack = { Nav.back() }) { TextButton(onClick = { Nav.go(Screen.RecipeEditor(r)) }) { Text("Modifica", color = C.AccentDeep) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (r.photo != null) {
                RecipeImage(r.id, null, r.photo, Modifier.fillMaxWidth().height(220.dp), r.emoji, 24.dp)
            } else {
                Text(r.emoji, style = T.Big)
            }
            GapH(10.dp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag("${r.servings} porzioni", C.Muted); if (r.minutes > 0) Tag("${r.minutes} min", C.Muted)
                if (r.meal.isNotBlank()) Tag(RecipeMeta.label(RecipeMeta.MEALS, r.meal), C.AccentDeep)
                if (r.cuisine.isNotBlank()) Tag(RecipeMeta.label(RecipeMeta.CUISINES, r.cuisine), C.AccentDeep)
                if (r.difficulty in 1..3) Tag(RecipeMeta.DIFFICULTY[r.difficulty], C.Muted)
            }
            if (r.description.isNotBlank()) { GapH(10.dp); Text(r.description, style = T.Read) }
            Spacer(Modifier.height(12.dp))
            JCard {
                Text("${per.kcal.r0()} kcal a porzione", style = T.Num.copy(color = C.AccentDeep))
                Spacer(Modifier.height(8.dp))
                MacroTiles(per.protein, per.carbs, per.fat)
                if (r.ingredients.isEmpty()) Text("Nessun ingrediente.", style = T.Small)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Duplica", Modifier.weight(1f)) {
                    val id = Store.newId()
                    val copy = r.copy(id = id, name = r.name + " (copia)", photo = RecipePhoto.copy(ctx, r.photo, id))
                    Store.saveRecipe(copy); Toast.makeText(ctx, "Ricetta duplicata", Toast.LENGTH_SHORT).show()
                    Nav.back(); Nav.go(Screen.UserRecipeDetail(copy))
                }
                GhostButton("Condividi", Modifier.weight(1f)) {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, r.name); putExtra(Intent.EXTRA_TEXT, shareText(r, per)) }
                    ctx.startActivity(Intent.createChooser(send, "Condividi la ricetta").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                GhostButton("Spesa", Modifier.weight(1f)) {
                    val n = Store.addUserRecipeToShopping(r, k, s.pantry.mapNotNull { it.foodId }.toSet())
                    Toast.makeText(ctx, if (n > 0) "Aggiunti $n ingredienti alla lista della spesa" else "Hai già tutto in dispensa", Toast.LENGTH_SHORT).show()
                }
            }
            SectionTitle("Ingredienti")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Per", style = T.Label)
                Text("−", style = T.Title2.copy(color = C.AccentDeep), modifier = Modifier.clickable { if (people > 1) people-- }.padding(horizontal = 14.dp, vertical = 6.dp))
                Text("$people", style = T.Title2)
                Text("+", style = T.Title2.copy(color = C.AccentDeep), modifier = Modifier.clickable { if (people < 30) people++ }.padding(horizontal = 14.dp, vertical = 6.dp))
                Text(if (people == 1) "persona" else "persone", style = T.Label)
            }
            GapH(6.dp)
            JCard { r.ingredients.forEach { i -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(i.food.name, style = T.Body, modifier = Modifier.weight(1f)); Text("${(i.grams * k).r0()} ${if (i.food.liquid) "ml" else "g"}", style = T.Label) } } }
            if (r.steps.isNotEmpty()) {
                SectionTitle("Preparazione")
                r.steps.forEachIndexed { idx, st -> Text("${idx + 1}. $st", style = T.Read, modifier = Modifier.padding(vertical = 4.dp)) }
            }
            SectionTitle("Aggiungi al diario di oggi")
            MealChips(meal) { meal = it }
            Spacer(Modifier.height(10.dp))
            JField(portions, { portions = it }, "Porzioni mangiate", number = true)
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Aggiungi a ${MEAL_NAMES[meal]}", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = (parseNum(portions) ?: 0.0) > 0) {
            Store.addEntry(LocalDate.now(), meal, food, (parseNum(portions) ?: 1.0) * (food.servingGrams ?: 100.0))
            Nav.back(); Nav.tab = 0; Nav.diaryDate = LocalDate.now()
        }
    }
}

private class EditorSnapshot(
    val name: String, val servings: String, val minutes: String, val steps: String, val ingredients: List<Ingredient>,
    val description: String, val meal: String, val cuisine: String, val difficulty: Int
)

@Composable
fun RecipeEditorScreen(recipe: Recipe?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by Store.state.collectAsState()
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var emoji by remember { mutableStateOf(recipe?.emoji ?: "🍽️") }
    var servings by remember { mutableStateOf((recipe?.servings ?: 1).toString()) }
    var minutes by remember { mutableStateOf((recipe?.minutes ?: 15).toString()) }
    var steps by remember { mutableStateOf(recipe?.steps?.joinToString("\n") ?: "") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var meal by remember { mutableStateOf(recipe?.meal ?: "") }
    var cuisine by remember { mutableStateOf(recipe?.cuisine ?: "") }
    var difficulty by remember { mutableIntStateOf(recipe?.difficulty ?: 0) }
    var photo by remember { mutableStateOf(recipe?.photo) }
    val ingredients = remember { mutableStateListOf<Ingredient>().apply { addAll(recipe?.ingredients ?: emptyList()) } }
    var picker by remember { mutableStateOf(false) }
    val rid = remember { recipe?.id ?: Store.newId() }
    val emojis = listOf("🍽️", "🥗", "🍝", "🍗", "🥩", "🐟", "🍳", "🥣", "🥞", "🍕", "🌯", "🥪", "🍲", "🍛", "🍰", "🥤")

    // importazione e creazione con AI
    var dialog by remember { mutableIntStateOf(0) } // 1 incolla testo, 2 da foto, 3 crea con AI
    var pasted by remember { mutableStateOf("") }
    var idea by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf<EditorSnapshot?>(null) }
    var pickFor by remember { mutableIntStateOf(0) } // 0 foto di copertina, 1 lettura ricetta da foto
    val hasKey = s.ai.apiKey.isNotBlank()

    fun applyImport(imp: ImportedRecipe, prefix: String) {
        undo = EditorSnapshot(name, servings, minutes, steps, ingredients.toList(), description, meal, cuisine, difficulty)
        name = imp.name; servings = imp.servings.toString(); minutes = imp.minutes.toString()
        ingredients.clear(); ingredients.addAll(imp.ingredients.map { (f, g) -> Ingredient(f, g) })
        steps = imp.steps.joinToString("\n")
        if (imp.description.isNotBlank()) description = imp.description
        if (imp.meal.isNotBlank()) meal = imp.meal
        if (imp.cuisine.isNotBlank()) cuisine = imp.cuisine
        if (imp.difficulty > 0) difficulty = imp.difficulty
        report = buildString {
            if (prefix.isNotBlank()) appendLine(prefix)
            append("Importati ${imp.ingredients.size} ingredienti e ${imp.steps.size} passaggi per ${imp.servings} porzioni. Controlla nome, quantità e passaggi prima di salvare.")
            if (imp.unmatched.isNotEmpty()) append("\nNon presenti nel database (NON contati nelle calorie, aggiungili a mano con + Aggiungi): " + imp.unmatched.take(8).joinToString("; ") + ".")
            if (imp.checkQty.isNotEmpty()) append("\nQuantità non leggibile, ho messo una quantità standard da controllare: " + imp.checkQty.take(8).joinToString(", ") + ".")
        }
    }

    fun importText(text: String) {
        if (text.isBlank()) return
        busy = "Leggo la ricetta…"; report = null
        scope.launch {
            val off = withContext(Dispatchers.Default) { RecipeImport.parse(Db.db, text) }
            var note = ""
            val imp = if (hasKey) runCatching { FoodAi.parseRecipe(s.ai, Db.db, text) }
                .getOrElse { e -> note = "L'AI non ha risposto (${FoodAi.friendlyError(e)}): ho letto il testo direttamente sul telefono, in modo meno preciso."; off } else off
            busy = ""
            if (imp.ingredients.isEmpty() && imp.steps.isEmpty()) report = "Non ho riconosciuto né ingredienti né passaggi. Prova a incollare il testo con le righe \"Ingredienti\" e \"Preparazione\"."
            else applyImport(imp, note)
        }
    }

    fun importPhoto(uri: Uri) {
        busy = "Leggo il testo dalla foto…"; report = null
        scope.launch {
            val text = runCatching {
                val bmp = withContext(Dispatchers.IO) { loadBitmap(ctx, uri, 2400) } ?: throw RuntimeException("Impossibile leggere la foto.")
                Ocr.read(bmp)
            }.getOrElse { e -> busy = ""; report = e.message ?: "Lettura della foto non riuscita."; return@launch }
            busy = ""
            if (text.isBlank()) report = "Nella foto non ho trovato testo leggibile. Riprova con più luce e inquadrando tutta la ricetta."
            else importText(text)
        }
    }

    fun createWithAi() {
        val q = idea.trim(); if (q.isEmpty()) return
        busy = "Scrivo la ricetta con l'AI…"; report = null
        scope.launch {
            runCatching { FoodAi.createRecipe(s.ai, Db.db, q, s.dietPrefs.exclude) }
                .onSuccess { applyImport(it, "Ricetta creata dall'AI: le calorie sono calcolate dal database sugli ingredienti riconosciuti, ma ricetta e quantità sono una proposta da verificare.") }
                .onFailure { report = FoodAi.friendlyError(it) }
            busy = ""
        }
    }

    val cameraFile = remember { File(ctx.cacheDir, "foto").apply { mkdirs() }.let { File(it, "ricetta.jpg") } }
    val cameraUri = remember { FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", cameraFile) }
    fun handle(uri: Uri) {
        if (pickFor == 1) importPhoto(uri)
        else scope.launch {
            val path = withContext(Dispatchers.IO) { RecipePhoto.save(ctx, uri, rid) }
            if (path != null) photo = path else Toast.makeText(ctx, "Impossibile leggere la foto", Toast.LENGTH_SHORT).show()
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) handle(cameraUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) handle(uri) }
    fun pickGallery(purpose: Int) { pickFor = purpose; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    fun pickCamera(purpose: Int) { pickFor = purpose; camera.launch(cameraUri) }

    val r = Recipe(rid, name.trim(), servings.toIntOrNull()?.coerceAtLeast(1) ?: 1, ingredients.toList(),
        steps.lines().map { it.trim() }.filter { it.isNotEmpty() }, minutes.toIntOrNull() ?: 0, "", emoji,
        description = description.trim(), photo = photo, cuisine = cuisine, difficulty = difficulty, meal = meal)
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings)
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (recipe == null) "Nuova ricetta" else "Modifica ricetta", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // foto di copertina
            if (photo != null) {
                RecipeImage(rid, null, photo, Modifier.fillMaxWidth().height(190.dp), emoji, 24.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Scatta", Modifier.weight(1f)) { pickCamera(0) }
                    GhostButton("Galleria", Modifier.weight(1f)) { pickGallery(0) }
                    GhostButton("Rimuovi", Modifier.weight(1f), color = C.Danger) { RecipePhoto.delete(photo); photo = null }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(C.SurfaceAlt), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, style = T.Big)
                        Text("Aggiungi una foto del piatto", style = T.Small)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Scatta una foto", Modifier.weight(1f)) { pickCamera(0) }
                    GhostButton("Dalla galleria", Modifier.weight(1f)) { pickGallery(0) }
                }
            }
            JField(name, { name = it }, "Nome della ricetta *")
            JField(description, { description = it }, "Descrizione (facoltativa)", singleLine = false, modifier = Modifier.heightIn(min = 72.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                emojis.forEach { e -> Text(e, style = T.Title, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(C.Surface).border(2.dp, if (emoji == e) C.Navy else Color.Transparent, RoundedCornerShape(10.dp)).clickable { emoji = e }.padding(8.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(servings, { servings = it.filter { c -> c.isDigit() } }, "Porzioni", Modifier.weight(1f), number = true)
                JField(minutes, { minutes = it.filter { c -> c.isDigit() } }, "Minuti", Modifier.weight(1f), number = true)
            }
            Text("Pasto", style = T.Label)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RecipeMeta.MEALS.forEach { (k, l) -> Pill(l, meal == k) { meal = if (meal == k) "" else k } }
            }
            Text("Cucina", style = T.Label)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RecipeMeta.CUISINES.forEach { (k, l) -> Pill(l, cuisine == k) { cuisine = if (cuisine == k) "" else k } }
            }
            Text("Difficoltà", style = T.Label)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..3).forEach { d -> Pill(RecipeMeta.DIFFICULTY[d], difficulty == d) { difficulty = if (difficulty == d) 0 else d } }
            }

            // importazione
            SectionTitle("Importa o crea in un attimo")
            JCard {
                Text("Hai già la ricetta scritta o in foto? Incolla il testo o fotografala, oppure fai scrivere una ricetta all'AI: calcolo le calorie dal database.", style = T.Muted)
                GapH(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Incolla", Modifier.weight(1f)) { dialog = 1 }
                    GhostButton("Da foto", Modifier.weight(1f)) { dialog = 2 }
                    GhostButton("Con AI", Modifier.weight(1f)) { dialog = 3 }
                }
                if (busy.isNotEmpty()) {
                    GapH(10.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = C.Accent, strokeWidth = 2.dp); GapW(10.dp); Text(busy, style = T.Label)
                    }
                }
                report?.let { msg ->
                    GapH(10.dp); Text(msg, style = T.Small.copy(color = C.Text))
                    undo?.let { u ->
                        TextButton(onClick = {
                            name = u.name; servings = u.servings; minutes = u.minutes; steps = u.steps; description = u.description
                            meal = u.meal; cuisine = u.cuisine; difficulty = u.difficulty
                            ingredients.clear(); ingredients.addAll(u.ingredients); undo = null; report = null
                        }) { Text("Annulla importazione", color = C.AccentDeep) }
                    }
                }
            }

            SectionTitle("Ingredienti", action = "+ Aggiungi") { picker = true }
            if (ingredients.isEmpty()) Text("Aggiungi almeno un ingrediente.", style = T.Muted)
            ingredients.forEachIndexed { idx, ing ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ing.food.name, style = T.Label, modifier = Modifier.weight(1f), maxLines = 2)
                    JField(ing.grams.r0(), { v -> parseNum(v)?.let { ingredients[idx] = ing.copy(grams = it) } }, if (ing.food.liquid) "ml" else "g", Modifier.width(90.dp), number = true)
                    Text("✕", style = T.Title2.copy(color = C.Faint), modifier = Modifier.clickable { ingredients.removeAt(idx) }.padding(10.dp))
                }
            }
            if (ingredients.isNotEmpty()) JCard { Text("A porzione: ${per.kcal.r0()} kcal", style = T.Title2.copy(color = C.AccentDeep)); MacroDots(per.protein, per.carbs, per.fat) }
            SectionTitle("Preparazione")
            JField(steps, { steps = it }, "Un passaggio per riga", singleLine = false, modifier = Modifier.heightIn(min = 120.dp))
        }
        Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipe != null && Store.current.recipes.any { it.id == recipe.id })
                GhostButton("Elimina", Modifier.weight(1f), color = C.Danger) { RecipePhoto.delete(recipe.photo); Store.deleteRecipe(recipe.id); Nav.back(); Nav.back() }
            PrimaryButton("Salva ricetta", Modifier.weight(2f), enabled = name.isNotBlank() && ingredients.isNotEmpty() && busy.isEmpty()) { Store.saveRecipe(r); Nav.back() }
        }
    }
    if (picker) IngredientPicker(onDismiss = { picker = false }) { food -> ingredients.add(Ingredient(food, food.servingGrams ?: 100.0)); picker = false }

    when (dialog) {
        1 -> AlertDialog(
            onDismissRequest = { dialog = 0 }, containerColor = C.Surface,
            title = { Text("Incolla la ricetta", style = T.Title) },
            text = {
                Column {
                    Text("Incolla titolo, ingredienti e preparazione.", style = T.Muted)
                    GapH(8.dp)
                    JField(pasted, { pasted = it }, "Testo della ricetta", singleLine = false, modifier = Modifier.heightIn(min = 180.dp, max = 320.dp))
                }
            },
            confirmButton = { TextButton(enabled = pasted.isNotBlank(), onClick = { dialog = 0; importText(pasted); pasted = "" }) { Text("Importa", color = C.AccentDeep) } },
            dismissButton = { TextButton(onClick = { dialog = 0 }) { Text("Annulla", color = C.Muted) } }
        )
        2 -> AlertDialog(
            onDismissRequest = { dialog = 0 }, containerColor = C.Surface,
            title = { Text("Ricetta da foto", style = T.Title) },
            text = { Text("Fotografa una pagina di libro, un biglietto o uno schermo con la ricetta: leggo il testo sul telefono (anche senza internet) e lo trasformo in ricetta." + if (hasKey) "" else " Senza la chiave gratuita dell'AI la struttura viene riconosciuta in modo più semplice: controlla il risultato.", style = T.Body) },
            confirmButton = { TextButton(onClick = { dialog = 0; pickCamera(1) }) { Text("Scatta", color = C.AccentDeep) } },
            dismissButton = { Row { TextButton(onClick = { dialog = 0; pickGallery(1) }) { Text("Galleria", color = C.AccentDeep) }; TextButton(onClick = { dialog = 0 }) { Text("Annulla", color = C.Muted) } } }
        )
        3 -> AlertDialog(
            onDismissRequest = { dialog = 0 }, containerColor = C.Surface,
            title = { Text("Crea con l'AI", style = T.Title) },
            text = {
                Column {
                    if (!hasKey) Text("Per creare ricette con l'AI serve la chiave gratuita: Profilo → Intelligenza artificiale.", style = T.Body.copy(color = C.Danger))
                    else Text("Descrivi la ricetta che vuoi, ad esempio \"lasagne vegetariane per 4\" o \"colazione proteica veloce\".", style = T.Muted)
                    GapH(8.dp)
                    JField(idea, { idea = it }, "Che ricetta vuoi?", singleLine = false, modifier = Modifier.heightIn(min = 90.dp))
                }
            },
            confirmButton = { TextButton(enabled = hasKey && idea.isNotBlank(), onClick = { dialog = 0; createWithAi() }) { Text("Crea", color = C.AccentDeep) } },
            dismissButton = { TextButton(onClick = { dialog = 0 }) { Text("Annulla", color = C.Muted) } }
        )
    }
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
