package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.jarvis.food.data.*
import com.jarvis.food.engine.toFood
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun ReplaceFoodScreen(entryId: String, initialQuery: String = "", offlineOnly: Boolean = false) {
    val s by Store.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var barcodeDialog by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf("") }
    var barcodeLoading by remember { mutableStateOf(false) }
    var barcodeError by remember { mutableStateOf<String?>(null) }
    var missingCode by remember { mutableStateOf<String?>(null) }
    val entry = s.diary.firstOrNull { it.id == entryId }
    var query by remember { mutableStateOf(initialQuery) }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val local = remember(query, s.customFoods, s.favorites, s.recipes, s.diary) {
        val q = query.trim().lowercase()
        val personal = s.customFoods + s.favorites + s.recipes.map { Nutrition.recipeAsFood(it) } + Store.recentFoods(s).map { it.first }
        val words = q.split(" ").filter { it.isNotBlank() }
        (personal.filter { f -> words.all { (f.name + " " + f.brand).lowercase().contains(it) } } +
            (if (q.isBlank()) emptyList() else Db.db.search(q).map { it.toFood() }))
            .distinctBy { it.id }.filter { it.id != entry?.food?.id }.take(100)
    }
    fun preview(food: Food) {
        val current = Store.current.diary.firstOrNull { it.id == entryId } ?: return
        Nav.go(Screen.FoodDetail(food, LocalDate.parse(current.date), current.meal, current.grams, current, replacing = true))
    }
    fun lookup(raw: String) {
        val code = raw.trim()
        if (code.length !in 8..14 || code.any { !it.isDigit() }) { barcodeDialog = true; barcodeError = "Inserisci un codice numerico da 8 a 14 cifre."; return }
        barcodeError = null; missingCode = null
        val mine = (s.customFoods + s.favorites + s.diary.map { it.food }).firstOrNull { it.barcode == code }
        if (mine != null) { barcodeDialog = false; preview(mine); return }
        barcodeLoading = true
        scope.launch {
            try {
                val food = OpenFoodFacts.byBarcode(code)
                if (food != null) { barcodeDialog = false; preview(food) }
                else { missingCode = code; barcodeError = "Prodotto non trovato: puoi copiarne i valori dall'etichetta."; barcodeDialog = true }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { barcodeError = "Non riesco a cercare il prodotto online. Controlla la connessione e riprova."; barcodeDialog = true }
            finally { barcodeLoading = false }
        }
    }
    fun scan() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).enableAutoZoom().build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { result -> result.rawValue?.let { barcode = it; lookup(it) } }
            .addOnFailureListener { barcodeError = "Scanner non disponibile. Puoi inserire il codice a mano."; barcodeDialog = true }
    }
    LaunchedEffect(query, offlineOnly) {
        online = emptyList(); error = null; loading = false
        if (offlineOnly || query.trim().length < 3) return@LaunchedEffect
        delay(600); loading = true
        runCatching { OpenFoodFacts.search(query) }
            .onSuccess { online = it.filter { f -> f.id != entry?.food?.id } }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; error = "Ricerca online non disponibile. Puoi usare gli alimenti locali." }
        loading = false
    }
    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        TopBar("Sostituisci alimento", { Nav.back() })
        if (entry == null) {
            Text("Questa voce non è più nel diario.", style = T.Body, modifier = Modifier.padding(20.dp))
            return@Column
        }
        val foods = (local + online).distinctBy { it.id }
        LazyColumn(Modifier.weight(1f).testTag("replacement-results"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val previous = Nutrition.of(entry.food, entry.grams)
                SettingsCard(title = "Alimento da sostituire", warm = true) {
                    Text(entry.food.name, style = T.Title2.copy(fontSize = 19.sp, lineHeight = 25.sp))
                    Text("${MEAL_NAMES[entry.meal]} · ${amountText(entry.grams)} ${if (entry.food.liquid) "ml" else "g"} · ${previous.kcal.r0()} kcal", style = T.Small)
                }
            }
            item {
                JField(query, { query = it }, "Cerca un alimento", Modifier.fillMaxWidth().testTag("replace-food-search"))
            }
            item {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReplacementSource("Scansiona codice a barre", Ic.Barcode, false, Modifier.weight(1f).testTag("replace-barcode")) { scan() }
                    ReplacementSource("Fotografa con AI", Ic.Camera, true, Modifier.weight(1f).testTag("replace-photo")) {
                        Nav.go(Screen.PhotoAi(LocalDate.parse(entry.date), entry.meal, entry.id))
                    }
                }
                TextButton(onClick = { barcodeDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Inserisci codice a mano", color = C.AccentDeep) }
            }
            if (barcodeLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.Accent) }
            item {
                SettingsHeading(if (query.isBlank()) "Recenti e preferiti" else "Risultati della ricerca", "Seleziona il nuovo alimento: nel passaggio successivo controlli quantità e valori.")
            }
            items(foods, key = { it.id }) { food ->
                val shape = RoundedCornerShape(20.dp)
                Row(Modifier.fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Line, shape).testTag("replacement-${food.id}").clickable { preview(food) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(food.name, style = T.Label.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
                        if (food.brand.isNotBlank()) Text(food.brand, style = T.Small)
                        Spacer(Modifier.height(7.dp))
                        Text("P ${food.protein.r0()} · C ${food.carbs.r0()} · G ${food.fat.r0()} g", style = T.Small)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(food.kcal.r0(), style = T.Title2.copy(fontSize = 19.sp))
                        Text("kcal / 100 ${if (food.liquid) "ml" else "g"}", style = T.Small.copy(fontSize = 12.sp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(C.NavySoft), contentAlignment = Alignment.Center) { AIcon(Ic.Swap, C.Navy, 19.dp) }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.Accent) }
            if (error != null) item { Text(error!!, style = T.Small) }
            if (foods.isEmpty() && !loading) item { SettingsCard { Text(if (query.isBlank()) "Scrivi il nome, scansiona la confezione o fotografa l'alimento." else "Nessun risultato. Prova un altro nome o usa il codice della confezione.", style = T.Small) } }
        }
    }

    if (barcodeDialog) AlertDialog(onDismissRequest = { if (!barcodeLoading) barcodeDialog = false },
        title = { Text("Sostituisci da codice a barre", style = T.Title2) },
        text = { Column {
            JField(barcode, { barcode = it.filter { c -> c.isDigit() }.take(14); barcodeError = null; missingCode = null }, "Codice prodotto", number = true)
            barcodeError?.let { Text(it, style = T.Small.copy(color = C.Danger)) }
            if (barcodeLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.Accent)
            if (missingCode != null && entry != null) TextButton(onClick = {
                barcodeDialog = false
                Nav.go(Screen.CustomFood(LocalDate.parse(entry.date), entry.meal, barcode = missingCode, replacementEntryId = entry.id))
            }) { Text("Crea dai valori in etichetta", color = C.AccentDeep) }
        } }, confirmButton = { TextButton(enabled = !barcodeLoading, onClick = { lookup(barcode) }) { Text("Cerca", color = C.Navy) } },
        dismissButton = { TextButton(enabled = !barcodeLoading, onClick = { barcodeDialog = false }) { Text("Annulla", color = C.Muted) } }, containerColor = C.Surface)

}

@Composable
private fun ReplacementSource(title: String, icon: String, warm: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxHeight().heightIn(min = 112.dp).clip(shape).background(if (warm) C.AccentSoft else C.NavySoft)
        .border(1.dp, C.Line, shape).clickable(onClick = onClick).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AIcon(icon, if (warm) C.AccentDeep else C.Navy, 30.dp)
        Text(title, style = T.Label.copy(fontSize = 14.sp, lineHeight = 19.sp))
    }
}
