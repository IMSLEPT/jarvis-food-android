package com.jarvis.food.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.jarvis.food.data.Db
import com.jarvis.food.data.Food
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AddFoodScreen(date: LocalDate, meal: Int, initialQuery: String = "", offlineOnly: Boolean = false) {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mealSel by remember { mutableIntStateOf(meal) }
    var query by remember { mutableStateOf(initialQuery) }
    var tab by remember { mutableIntStateOf(0) }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var quickAdd by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    // Alimenti aggiunti in questa schermata: id alimento -> id della voce nel diario (per poterla togliere).
    val added = remember { mutableStateMapOf<String, String>() }
    val addedKcal = s.diary.filter { it.id in added.values }.sumOf { Nutrition.of(it.food, it.grams).kcal }

    val local = remember(query, s.customFoods, s.favorites) {
        val q = query.trim().lowercase()
        val mine = if (q.isEmpty()) emptyList() else (s.customFoods + s.favorites + s.recipes.map { Nutrition.recipeAsFood(it) })
            .filter { q.split(" ").all { t -> (it.name + " " + it.brand).lowercase().contains(t) } }
        (mine + Db.db.search(query).map { it.toFood() }).distinctBy { it.id }
    }

    // Ricerca online con attesa di 600 ms dopo l'ultima digitazione.
    LaunchedEffect(query, offlineOnly) {
        online = emptyList(); error = null; loading = false
        if (offlineOnly || query.trim().length < 3) return@LaunchedEffect
        delay(600)
        loading = true
        runCatching { OpenFoodFacts.search(query) }
            .onSuccess { list -> online = list.distinctBy { it.id }.filter { o -> local.none { it.id == o.id } } }
            .onFailure { error = "Ricerca online non riuscita (${it.message}). I risultati locali restano disponibili." }
        loading = false
    }

    fun scan() {
        val opts = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(ctx, opts).startScan()
            .addOnSuccessListener { code ->
                val value = code.rawValue ?: return@addOnSuccessListener
                // Prima cerco tra gli alimenti creati da me, poi su Open Food Facts.
                val mine = (s.customFoods + s.favorites).firstOrNull { it.barcode == value }
                if (mine != null) { Nav.go(Screen.FoodDetail(mine, date, mealSel)); return@addOnSuccessListener }
                scanning = true
                scope.launch {
                    val food = runCatching { OpenFoodFacts.byBarcode(value) }.getOrElse {
                        Toast.makeText(ctx, "Connessione non disponibile: ${it.message}", Toast.LENGTH_LONG).show(); null
                    }
                    scanning = false
                    if (food != null) Nav.go(Screen.FoodDetail(food, date, mealSel))
                    else {
                        Toast.makeText(ctx, "Prodotto non trovato: crealo tu, lo ricorderò.", Toast.LENGTH_LONG).show()
                        Nav.go(Screen.CustomFood(date, mealSel, barcode = value))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(ctx, "Scanner non disponibile: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Aggiunge (o toglie, se già aggiunto) l'alimento senza lasciare la schermata. */
    fun toggle(food: Food, grams: Double) {
        val id = added[food.id]
        if (id != null) { Store.removeEntry(id); added.remove(food.id) }
        else added[food.id] = Store.addEntry(date, mealSel, food, grams)
    }

    val mealName = MEAL_LABEL.getValue(mealSel)
    val freq = Store.frequentFoods(s, mealSel)

    // Reserve the footer's measured height, including system insets and larger text.
    // The last action must stay separated from the fixed confirmation bar.
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp)) {
            item {
                Row(Modifier.statusBarsPadding().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackCircle { Nav.back() }
                    GapW(12.dp)
                    Column {
                        Text("Aggiungi a", style = T.Small.copy(fontSize = 13.sp, color = A.Muted))
                        Text(mealName, style = T.H1.copy(fontSize = 28.sp, lineHeight = 32.sp))
                    }
                }
                MealChips(mealSel, Modifier.padding(top = 16.dp)) { mealSel = it }
                if (offlineOnly) Column(Modifier.padding(top = 14.dp).testTag("food-search-offline")) {
                    Text("Solo database sul telefono", style = T.Label.copy(color = A.GreenInk))
                    Text("Cerca l'alimento in italiano e conferma il peso. Per un piatto composto aggiungi anche gli altri ingredienti e i condimenti.", style = T.Small)
                }
                // Ricerca
                val sb = RoundedCornerShape(30.dp)
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(60.dp).aShadow(sb, 10.dp, Color(0x801D1B3A)).clip(sb).background(Color.White).border(1.dp, Color(0x141D1B3A), sb)
                    .padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Search, A.Muted, 22.dp, 2.2f)
                    GapW(8.dp)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Cerca alimenti o prodotti", style = T.Body.copy(fontSize = 16.sp, color = C.Muted))
                        BasicTextField(query, { query = it }, singleLine = true, textStyle = T.Body.copy(fontSize = 16.sp, color = A.Ink),
                            cursorBrush = SolidColor(C.Accent), modifier = Modifier.fillMaxWidth())
                    }
                    if (!offlineOnly) Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(C.AccentSoft, C.AccentSoft))).clickable { scan() }, contentAlignment = Alignment.Center) {
                        AIcon(Ic.Barcode, A.Ink, 22.dp, 2.2f)
                    }
                }
                if (scanning) Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = C.Accent, strokeWidth = 2.dp)
                    GapW(10.dp); Text("Cerco il prodotto…", style = T.Muted)
                }
                if (query.isBlank() && freq.isNotEmpty()) {
                    Text("Spesso a ${mealName.lowercase()}", style = T.Chip.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(top = 20.dp))
                    Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        freq.forEach { (f, g) ->
                            val on = f.id in added
                            val (tint, ink) = tileColors(f.name)
                            val cs = RoundedCornerShape(22.dp)
                            Row(Modifier.height(44.dp).aShadow(cs, 5.dp, Color(0x801D1B3A)).clip(cs).background(if (on) C.Surface else Color.White).clickable { toggle(f, g) }
                                .padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(30.dp).clip(CircleShape).background(C.cardSurface(tint)), contentAlignment = Alignment.Center) {
                                    Text(f.name.trim().take(1).uppercase(), style = T.H1.copy(fontSize = 14.sp, lineHeight = 16.sp, color = ink))
                                }
                                GapW(8.dp)
                                Text(f.name, style = T.Chip.copy(fontSize = 13.5.sp), maxLines = 1)
                            }
                        }
                    }
                }
                if (query.isBlank()) {
                    val ts = RoundedCornerShape(28.dp)
                    Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(ts).background(Color(0xE6FFFFFF)).border(1.dp, A.Hair, ts).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Recenti", "Preferiti", "I miei", "Ricette").forEachIndexed { i, l ->
                            val sel = tab == i
                            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(if (sel) C.Navy else Color.Transparent).clickable { tab = i }, contentAlignment = Alignment.Center) {
                                Text(l, style = T.Chip.copy(color = if (sel) Color.White else A.Ink), maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Elenco alimenti
            val list: List<Pair<Food, Double?>> = when {
                query.isNotBlank() -> local.take(40).map { it to null }
                tab == 0 -> (freq + Store.recentFoods(s)).distinctBy { it.first.id }.map { it.first to it.second }
                tab == 1 -> s.favorites.map { it to null }
                tab == 2 -> s.customFoods.map { it to null }
                else -> s.recipes.map { Nutrition.recipeAsFood(it) to null }
            }
            item {
                Row(Modifier.padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Alimenti", style = T.H2.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
                    Text("${list.size} risultati", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                }
            }
            if (list.isEmpty()) item {
                val (e, t, d) = when {
                    query.isNotBlank() -> Triple("🔎", "Nessun alimento trovato", "Prova con un altro nome o crea il tuo alimento.")
                    tab == 1 -> Triple("⭐", "Nessun preferito", "Tocca la stella nel dettaglio di un alimento per salvarlo qui.")
                    tab == 2 -> Triple("🧑‍🍳", "Nessun alimento creato", "Crea i tuoi alimenti o i prodotti non trovati con il barcode.")
                    tab == 3 -> Triple("📖", "Nessuna ricetta tua", "Crea una ricetta dalla scheda Cucina.")
                    else -> Triple("🔎", "Inizia a cercare", "Scrivi un alimento, scansiona un codice a barre o scatta una foto del piatto.")
                }
                EmptyState(e, t, d)
            }
            items(list, key = { "a" + tab + it.first.id }) { (f, g) ->
                FoodRow(f, g, date, mealSel, added = f.id in added) { grams -> toggle(f, grams) }
                GapH(8.dp)
            }
            if (tab == 3 && query.isBlank()) {
                val slotIdx = s.dietPrefs.slots.indexOfFirst { it.diaryMeal == mealSel }.coerceAtLeast(0)
                val target = com.jarvis.food.engine.Planner.slotTargets(Store.energyFor(s, date).macro, s.dietPrefs)[slotIdx]
                val mealKey = s.dietPrefs.slots.getOrNull(slotIdx)?.meal ?: "pranzo"
                items(Db.db.recipes.filter { it.meal == mealKey }, key = { "db" + it.id }) { tpl ->
                    val rs = RoundedCornerShape(26.dp)
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).aShadow(rs, 6.dp, Color(0x801D1B3A)).clip(rs).background(Color.White).border(1.dp, A.Hair, rs)
                        .clickable { Nav.go(Screen.RecipeView(tpl.id, target, date, null, mealSel)) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.size(52.dp), "🍽️", 19.dp)
                        GapW(12.dp)
                        Column(Modifier.weight(1f)) {
                            Text(tpl.name, style = T.Label.copy(fontSize = 15.sp), maxLines = 2)
                            Text("Ricetta calcolata sul tuo obiettivo", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        }
                    }
                }
            }

            // Prodotti confezionati
            if (!offlineOnly) item {
                Row(Modifier.padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Prodotti confezionati", style = T.H2.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
                    Text("Open Food Facts", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    if (loading) { GapW(8.dp); CircularProgressIndicator(Modifier.size(14.dp), color = C.Accent, strokeWidth = 2.dp) }
                }
                if (query.trim().length < 3) {
                    val ps = RoundedCornerShape(28.dp)
                    Column(Modifier.fillMaxWidth().clip(ps).background(C.Surface).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Color.White), contentAlignment = Alignment.Center) { AIcon(Ic.Barcode, C.Navy, 22.dp, 2.2f) }
                            GapW(12.dp)
                            Text("Scrivi almeno 3 lettere o scansiona il codice a barre: cerco il prodotto online, poi puoi correggere i valori.", style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF4B5B6E)))
                        }
                        GapH(12.dp)
                        Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).clickable { Nav.go(Screen.CustomFood(date, mealSel)) },
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            AIcon(Ic.Plus, A.Ink, 18.dp, 2.4f); GapW(8.dp)
                            Text("Crea un alimento tuo", style = T.BtnS.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    error?.let { Text(it, style = T.Small.copy(color = C.Danger)) }
                    if (!loading && online.isEmpty() && error == null) Text("Nessun prodotto trovato online.", style = T.Small)
                }
            }
            if (!offlineOnly && query.trim().length >= 3) items(online, key = { "o" + it.id }) { f ->
                FoodRow(f, null, date, mealSel, added = f.id in added) { grams -> toggle(f, grams) }
                GapH(8.dp)
            }

            // Aggiunta rapida
            item {
                val qs = RoundedCornerShape(28.dp)
                Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(qs).background(C.Surface).clickable { quickAdd = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aggiunta rapida", style = T.H2.copy(fontSize = 18.sp))
                        Text("Scrivi solo le calorie, senza cercare l'alimento.", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    }
                    GapW(10.dp)
                    Row(Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("kcal", style = T.Num.copy(fontSize = 17.sp))
                    }
                }
            }
        }

        // Barra "alimenti aggiunti"
        val bs = RoundedCornerShape(30.dp)
        Row(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 14.dp).fillMaxWidth()
            .aShadow(bs, 14.dp, Color(0x8C17286B)).clip(bs).background(C.NavyBrush).padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(when (added.size) { 0 -> "Nessun alimento aggiunto"; 1 -> "1 alimento aggiunto"; else -> "${added.size} alimenti aggiunti" }, style = T.Small.copy(fontSize = 12.5.sp, color = Color(0xCCFFFFFF)))
                Text("${kcalFmt(addedKcal)} kcal", style = T.H1.copy(fontSize = 22.sp, lineHeight = 26.sp, color = Color.White))
            }
            val fs = RoundedCornerShape(26.dp)
            Row(Modifier.height(52.dp).aShadow(fs, 8.dp, Color(0x73F25E00)).clip(fs).background(C.OrangeBrush).clickable { Nav.back() }.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Fatto", style = T.BtnM.copy(color = C.OnOrange)); GapW(6.dp); AIcon(Ic.Check, C.OnOrange, 18.dp, 2.6f)
            }
        }
    }

    if (quickAdd) QuickAddDialog(onDismiss = { quickAdd = false }) { food ->
        added[food.id] = Store.addEntry(date, mealSel, food, 100.0)
        quickAdd = false
    }
}

/** Riga alimento del design: tessera con iniziale, nome, "g · kcal/100 g", kcal e pulsante +/✓. */
@Composable
fun FoodRow(food: Food, grams: Double?, date: LocalDate, meal: Int, emoji: String? = null, added: Boolean = false, onAdd: ((Double) -> Unit)? = null) {
    val g = grams ?: food.servingGrams ?: 100.0
    val unit = if (food.liquid) "ml" else "g"
    val t = Nutrition.of(food, g)
    val shape = RoundedCornerShape(26.dp)
    val (tint, ink) = tileColors(food.name)
    Row(
        Modifier.fillMaxWidth().aShadow(shape, 6.dp, Color(0x801D1B3A)).clip(shape).background(if (added) C.Surface else Color.White)
            .border(1.dp, A.Hair, shape).clickable { Nav.go(Screen.FoodDetail(food, date, meal, grams)) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) Box(Modifier.size(52.dp).clip(RoundedCornerShape(19.dp)).background(C.cardSurface(tint)), contentAlignment = Alignment.Center) { Text(emoji, style = T.Title2) }
        else InitialTile(food.name, tint, ink, 52.dp, 19.dp)
        GapW(12.dp)
        Column(Modifier.weight(1f)) {
            Text(food.name, style = T.Label.copy(fontSize = 15.sp, lineHeight = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            val per = Nutrition.of(food, 100.0).kcal
            Text("${g.r0()} $unit · ${per.r0()} kcal / 100 $unit" + if (food.brand.isNotBlank()) " · ${food.brand}" else "",
                style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GapW(8.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(t.kcal.r0(), style = T.Num.copy(fontSize = 17.sp, lineHeight = 20.sp))
            Text("kcal", style = T.Small.copy(fontSize = 11.5.sp, color = A.Muted))
        }
        GapW(10.dp)
        Box(Modifier.size(48.dp).clip(CircleShape).background(if (added) C.Green else C.Navy)
            .clickable { if (onAdd != null) onAdd(g) else { Store.addEntry(date, meal, food, g); Nav.back() } }, contentAlignment = Alignment.Center) {
            if (added) AIcon(Ic.Check, Color.White, 22.dp, 2.8f) else AIcon(Ic.Plus, Color.White, 22.dp, 2.6f)
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
            }) { Text("Aggiungi", color = C.AccentDeep) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = C.Muted) } }
    )
}
