package com.jarvis.food.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.jarvis.food.data.Food
import com.jarvis.food.data.Db
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AddFoodScreen(date: LocalDate, meal: Int, initialQuery: String = "") {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var tab by remember { mutableIntStateOf(0) }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var quickAdd by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }

    val local = remember(query, s.customFoods, s.favorites) {
        val q = query.trim().lowercase()
        val mine = if (q.isEmpty()) emptyList() else (s.customFoods + s.favorites + s.recipes.map { Nutrition.recipeAsFood(it) })
            .filter { q.split(" ").all { t -> (it.name + " " + it.brand).lowercase().contains(t) } }
        (mine + Db.db.search(query).map { it.toFood() }).distinctBy { it.id }
    }

    // Ricerca online con attesa di 600 ms dopo l'ultima digitazione.
    LaunchedEffect(query) {
        online = emptyList(); error = null
        if (query.trim().length < 3) return@LaunchedEffect
        delay(600)
        loading = true
        runCatching { OpenFoodFacts.search(query) }
            .onSuccess { list -> online = list.distinctBy { it.id }.filter { o -> local.none { it.id == o.id } } }
            .onFailure { error = "Ricerca online non riuscita (${it.message}). I risultati locali restano disponibili." }
        loading = false
    }

    fun scan() {
        val opts = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(ctx, opts).startScan()
            .addOnSuccessListener { code ->
                val value = code.rawValue ?: return@addOnSuccessListener
                // Prima cerco tra gli alimenti creati da me, poi su Open Food Facts.
                val mine = (s.customFoods + s.favorites).firstOrNull { it.barcode == value }
                if (mine != null) { Nav.go(Screen.FoodDetail(mine, date, meal)); return@addOnSuccessListener }
                scanning = true
                scope.launch {
                    val food = runCatching { OpenFoodFacts.byBarcode(value) }.getOrElse {
                        Toast.makeText(ctx, "Connessione non disponibile: ${it.message}", Toast.LENGTH_LONG).show(); null
                    }
                    scanning = false
                    if (food != null) Nav.go(Screen.FoodDetail(food, date, meal))
                    else {
                        Toast.makeText(ctx, "Prodotto non trovato: crealo tu, lo ricorderò.", Toast.LENGTH_LONG).show()
                        Nav.go(Screen.CustomFood(date, meal, barcode = value))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(ctx, "Scanner non disponibile: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Aggiungi a ${MEAL_NAMES[meal]}", onBack = { Nav.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            JField(query, { query = it }, "Cerca un alimento o un prodotto")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("📷 Barcode") { scan() }
                ActionChip("✨ Foto AI") { Nav.go(Screen.PhotoAi(date, meal)) }
                ActionChip("⚡ Rapido") { quickAdd = true }
                ActionChip("➕ Crea alimento") { Nav.go(Screen.CustomFood(date, meal)) }
            }
            if (query.isBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Recenti", tab == 0) { tab = 0 }
                    Pill("Preferiti", tab == 1) { tab = 1 }
                    Pill("I miei", tab == 2) { tab = 2 }
                    Pill("Ricette", tab == 3) { tab = 3 }
                }
            }
        }
        if (scanning) Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = C.Accent, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp)); Text("Cerco il prodotto…", style = T.Muted)
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (query.isNotBlank()) {
                if (local.isNotEmpty()) {
                    item { Text("Alimenti", style = T.Small, modifier = Modifier.padding(bottom = 6.dp)) }
                    items(local.take(40), key = { "l" + it.id }) { FoodRow(it, null, date, meal) }
                }
                item {
                    Row(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Prodotti (Open Food Facts)", style = T.Small, modifier = Modifier.weight(1f))
                        if (loading) CircularProgressIndicator(Modifier.size(14.dp), color = C.Accent, strokeWidth = 2.dp)
                    }
                }
                error?.let { item { Text(it, style = T.Small.copy(color = C.Danger)) } }
                if (!loading && online.isEmpty() && query.trim().length >= 3 && error == null)
                    item { Text("Nessun prodotto trovato online.", style = T.Small) }
                items(online, key = { "o" + it.id }) { FoodRow(it, null, date, meal) }
            } else {
                when (tab) {
                    0 -> {
                        val freq = Store.frequentFoods(s, meal)
                        val recent = Store.recentFoods(s)
                        if (freq.isEmpty() && recent.isEmpty()) item {
                            EmptyState("🔎", "Inizia a cercare", "Scrivi un alimento, scansiona un codice a barre o scatta una foto del piatto.")
                        }
                        if (freq.isNotEmpty()) {
                            item { Text("Spesso a ${MEAL_NAMES[meal].lowercase()}", style = T.Small, modifier = Modifier.padding(bottom = 6.dp)) }
                            items(freq, key = { "f" + it.first.id }) { FoodRow(it.first, it.second, date, meal) }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                        if (recent.isNotEmpty()) {
                            item { Text("Recenti", style = T.Small, modifier = Modifier.padding(bottom = 6.dp)) }
                            items(recent, key = { "r" + it.first.id }) { FoodRow(it.first, it.second, date, meal) }
                        }
                    }
                    1 -> {
                        if (s.favorites.isEmpty()) item { EmptyState("⭐", "Nessun preferito", "Tocca la stella nel dettaglio di un alimento per salvarlo qui.") }
                        items(s.favorites, key = { "fav" + it.id }) { FoodRow(it, null, date, meal) }
                    }
                    2 -> {
                        if (s.customFoods.isEmpty()) item { EmptyState("🧑‍🍳", "Nessun alimento creato", "Crea i tuoi alimenti o i prodotti non trovati con il barcode.") }
                        items(s.customFoods, key = { "c" + it.id }) { FoodRow(it, null, date, meal) }
                    }
                    else -> {
                        items(s.recipes, key = { "rec" + it.id }) { r ->
                            FoodRow(Nutrition.recipeAsFood(r), null, date, meal, emoji = r.emoji)
                        }
                        val slotIdx = s.dietPrefs.slots.indexOfFirst { it.diaryMeal == meal }.coerceAtLeast(0)
                        val target = com.jarvis.food.engine.Planner.slotTargets(Store.energyFor(s, date).macro, s.dietPrefs)[slotIdx]
                        val mealKey = s.dietPrefs.slots.getOrNull(slotIdx)?.meal ?: "pranzo"
                        items(Db.db.recipes.filter { it.meal == mealKey }, key = { "db" + it.id }) { tpl ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface)
                                    .clickable { Nav.go(Screen.RecipeView(tpl.id, target, date, null, meal)) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.size(52.dp), "🍽️", 12.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tpl.name, style = T.Label, maxLines = 2)
                                    Text("Ricetta calcolata sul tuo obiettivo", style = T.Small)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (quickAdd) QuickAddDialog(onDismiss = { quickAdd = false }) { food ->
        Store.addEntry(date, meal, food, 100.0)
        quickAdd = false
        Nav.back()
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    Text(
        text, style = T.Label,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.SurfaceHi).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}

@Composable
fun FoodRow(food: Food, grams: Double?, date: LocalDate, meal: Int, emoji: String? = null) {
    val g = grams ?: food.servingGrams ?: 100.0
    val unit = if (food.liquid) "ml" else "g"
    val t = Nutrition.of(food, g)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface)
            .clickable { Nav.go(Screen.FoodDetail(food, date, meal, grams)) }
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) { Text(emoji, style = T.Title2); Spacer(Modifier.width(10.dp)) }
        Column(Modifier.weight(1f)) {
            Text(food.name, style = T.Label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val label = when {
                grams != null -> "${g.r0()} $unit"
                food.servingName != null && food.servingGrams != null -> "1 ${food.servingName} (${g.r0()} $unit)"
                else -> "100 $unit"
            }
            Text(
                "${t.kcal.r0()} kcal · $label" + if (food.brand.isNotBlank()) " · ${food.brand}" else "",
                style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        RoundIconButton(Icons.Filled.Add, "Aggiungi", bg = C.SurfaceHi, tint = C.Accent, size = 34.dp) {
            Store.addEntry(date, meal, food, g)
            Nav.back()
        }
    }
}

@Composable
fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (Food) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text("Aggiunta rapida", style = T.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(name, { name = it }, "Descrizione (facoltativa)")
                JField(kcal, { kcal = it }, "Calorie", number = true, suffix = "kcal")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JField(c, { c = it }, "Carb.", Modifier.weight(1f), number = true)
                    JField(p, { p = it }, "Prot.", Modifier.weight(1f), number = true)
                    JField(f, { f = it }, "Grassi", Modifier.weight(1f), number = true)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = (parseNum(kcal) ?: 0.0) > 0, onClick = {
                onAdd(
                    Food(
                        id = "quick:${Store.newId()}", name = name.ifBlank { "Aggiunta rapida" },
                        kcal = parseNum(kcal) ?: 0.0, protein = parseNum(p) ?: 0.0, carbs = parseNum(c) ?: 0.0,
                        fat = parseNum(f) ?: 0.0, source = "quick", servingName = "voce", servingGrams = 100.0
                    )
                )
            }) { Text("Aggiungi", color = C.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = C.Muted) } }
    )
}
