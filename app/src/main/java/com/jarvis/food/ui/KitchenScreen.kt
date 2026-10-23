package com.jarvis.food.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.ai.Ocr
import com.jarvis.food.ai.PantryGuess
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.PantryItem
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Kitchen
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.Shopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun KitchenScreen() {
    val s by Store.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp)) {
        Text("Cucina", style = T.H1)
        Text("Dispensa, lista della spesa e ricette", style = T.Body.copy(fontSize = 14.sp, color = A.Muted), modifier = Modifier.padding(top = 4.dp))
        val seg = RoundedCornerShape(28.dp)
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(seg).background(Color(0xE6FFFFFF)).border(1.dp, A.Hair, seg).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Dispensa", "Spesa", "Ricette").forEachIndexed { i, l ->
                val sel = Nav.kitchenTab == i
                val n = when (i) { 0 -> s.pantry.size; 1 -> s.shopping.count { !it.checked }; else -> 0 }
                Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (sel) C.Navy else Color.Transparent).clickable { Nav.kitchenTab = i },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(l, style = T.BtnS.copy(color = if (sel) Color.White else A.Ink), maxLines = 1)
                    if (n > 0) { GapW(6.dp); Text("$n", style = T.BtnS.copy(color = if (sel) Color(0xFF9DB4FF) else A.Muted)) }
                }
            }
        }
        GapH(14.dp)
        when (Nav.kitchenTab) {
            0 -> PantryTab(s)
            1 -> ShoppingTab(s)
            else -> RecipesHome()
        }
    }
}

private fun shopDot(shop: String): Color = when {
    shop.startsWith("Frutta e verdura") -> C.Green
    shop.startsWith("Pane") -> C.Gold
    shop.startsWith("Carne") -> C.Protein
    shop.startsWith("Latticini") -> C.Blue
    shop.startsWith("Dispensa") -> C.Fasting
    shop.startsWith("Legumi") -> Color(0xFF84CC16)
    shop.startsWith("Frutta secca") -> Color(0xFFA16207)
    shop.startsWith("Condimenti") -> Color(0xFFF97316)
    shop.startsWith("Bevande") -> C.Water
    else -> Color(0xFFB8B4CC)
}

/** "Cosa cucino oggi": scelta del pasto, ricetta nuova con l'AI e idee dal ricettario con ciò che c'è in dispensa. */
@Composable
private fun CookTodayBlock(s: AppState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var meal by remember { mutableStateOf(when (defaultMeal()) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; else -> "spuntino" }) }
    var busy by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val energy = Store.energyFor(s, today)
    val slotIdx = s.dietPrefs.slots.indexOfFirst { it.meal == meal }.coerceAtLeast(0)
    val target = Planner.slotTargets(energy.macro, s.dietPrefs)[slotIdx]
    val diaryMeal = s.dietPrefs.slots.getOrNull(slotIdx)?.diaryMeal ?: 1
    // Le ricette possibili si calcolano in background: la scheda non si blocca mentre scorre il ricettario.
    val ideas by produceState(emptyList<com.jarvis.food.engine.PantrySuggestion>(), s.pantry, meal, target, s.dietPrefs) {
        val pantry = s.pantry; val prefs = s.dietPrefs
        value = withContext(Dispatchers.Default) { Kitchen.suggest(Db.db, pantry, meal, { target }, { Planner.allowed(Db.db, it, prefs) }) }
    }

    ASection("Cosa cucino oggi", top = 26.dp)
    GapH(12.dp)
    val cs = RoundedCornerShape(30.dp)
    Column(Modifier.fillMaxWidth().aShadow(cs, 10.dp, Color(0x66E88C60)).clip(cs)
        .background(Brush.linearGradient(listOf(Color(0xFFFFEBDD), Color(0xFFFFE4E8)))).padding(16.dp)) {
        Text("Ricette dal ricettario con ciò che hai in dispensa, calcolate sul tuo obiettivo.", style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp, color = A.Muted))
        Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pranzo" to "Pranzo", "cena" to "Cena", "colazione" to "Colazione", "spuntino" to "Spuntino").forEach { (k, l) ->
                val sel = meal == k
                Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (sel) C.Navy else Color.White).clickable { meal = k }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(l, style = T.Chip.copy(color = if (sel) Color.White else A.Ink), maxLines = 1)
                }
            }
        }
        GapH(14.dp)
        val bs = RoundedCornerShape(28.dp)
        val enabled = !busy && s.pantry.any { it.foodId != null }
        Row(Modifier.fillMaxWidth().height(56.dp).clip(bs).background(if (enabled) C.Navy else Color(0x6617286B)).clickable(enabled = enabled) {
            if (s.ai.apiKey.isBlank()) { Nav.go(Screen.AiSettings); return@clickable }
            busy = true
            scope.launch {
                runCatching { FoodAi.inventRecipe(s.ai, Db.db, s.pantry.mapNotNull { it.foodId }, meal, target, s.dietPrefs.exclude) }
                    .onSuccess { tpl -> Nav.go(Screen.RecipeView(tpl.id, target, today, null, diaryMeal, adHoc = tpl)) }
                    .onFailure { Toast.makeText(ctx, it.message ?: "Errore", Toast.LENGTH_LONG).show() }
                busy = false
            }
        }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            AIcon(Ic.Spark, Color(0xFFFFB820), 22.dp, 2f); GapW(10.dp)
            Text(if (busy) "Sto pensando…" else "Ricetta nuova con l'AI", style = T.BtnM.copy(color = Color.White))
        }
    }
    GapH(14.dp)
    if (s.pantry.isEmpty()) {
        Text("Aggiungi qualcosa alla dispensa per vedere le ricette possibili.", style = T.Small)
    }
    ideas.take(4).forEach { sg ->
        val rs = RoundedCornerShape(30.dp)
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).aShadow(rs, 8.dp, Color(0x661D1B3A)).clip(rs).background(Color.White).border(1.dp, A.Hair, rs)
            .clickable { Nav.go(Screen.RecipeView(sg.solved.template.id, target, today, null, diaryMeal)) }.padding(14.dp)) {
            Row {
                RecipeImage(sg.solved.template.id, sg.solved.template.image, s.recipePhotos[sg.solved.template.id], Modifier.size(width = 100.dp, height = 100.dp), "🍽️", 20.dp)
                GapW(14.dp)
                Column(Modifier.weight(1f)) {
                    Text(sg.solved.template.name, style = T.H2.copy(fontSize = 17.sp, lineHeight = 21.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${sg.solved.total.kcal.r0()} kcal · hai il ${(sg.coverage * 100).r0()}% degli ingredienti", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 4.dp))
                    ABar(sg.coverage.toFloat(), Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF10B981))), Color(0xFFD9F6EA), 6.dp, Modifier.padding(top = 8.dp))
                }
            }
            if (sg.missing.isNotEmpty()) {
                Text("Manca: " + sg.missing.joinToString { it.name.lowercase() }, style = T.Small.copy(fontSize = 12.5.sp, color = Color(0xFF8F2A14)), modifier = Modifier.padding(top = 10.dp), maxLines = 2)
                Text("+ Aggiungi i mancanti alla spesa", style = T.Link.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 8.dp).clickable {
                    sg.missing.forEach { f -> Store.update { st -> st.copy(shopping = st.shopping + com.jarvis.food.data.ShoppingItem(Store.newId(), f.id, f.name,
                        packs = 1, packGrams = f.pack ?: 100.0, shop = f.shop)) } }
                    Toast.makeText(ctx, "Aggiunti alla lista della spesa", Toast.LENGTH_SHORT).show()
                }.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun PantryTab(s: AppState) {
    var name by remember { mutableStateOf("") }
    ScanTile(Ic.Fridge, "Fotografa frigo o dispensa", "Riconosco gli alimenti dalla foto e li aggiungo", Brush.linearGradient(listOf(Color(0xFFC9F4E2), Color(0xFFE3F8EF))), A.GreenInk, Color(0x6610B981)) { Nav.go(Screen.PantryScan("foto")) }
    GapH(12.dp)
    ScanTile(Ic.Receipt, "Fotografa lo scontrino", "Leggo i prodotti acquistati e li metto in dispensa", Brush.linearGradient(listOf(Color(0xFFFFE3B0), Color(0xFFFFF2DA))), Color(0xFFB45309), Color(0x66F59E0B)) { Nav.go(Screen.PantryScan("scontrino")) }

    CookTodayBlock(s)

    ASection("In dispensa", top = 18.dp)
    GapH(12.dp)
    // Aggiunta a mano
    val fs = RoundedCornerShape(30.dp)
    Row(Modifier.fillMaxWidth().height(56.dp).clip(fs).background(Color.White).border(1.dp, Color(0x141D1B3A), fs).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (name.isEmpty()) Text("Aggiungi a mano (es. uova, spinaci)", style = T.Body.copy(fontSize = 15.sp, color = Color(0xFF9A98B0)))
            androidx.compose.foundation.text.BasicTextField(name, { name = it }, singleLine = true, textStyle = T.Body.copy(fontSize = 15.sp, color = A.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Accent), modifier = Modifier.fillMaxWidth())
        }
        PlusButton("Aggiungi", 44.dp) {
            if (name.isNotBlank()) {
                val f = Db.db.match(name) ?: Db.db.search(name, 1).firstOrNull()
                Store.addPantry(listOf(PantryItem(Store.newId(), f?.id, f?.name ?: name.trim(), null)))
                name = ""
            }
        }
    }
    GapH(12.dp)
    if (s.pantry.isEmpty()) EmptyState("🧺", "Dispensa vuota", "Fotografa il frigo, la dispensa o lo scontrino: Jarvis riconosce i prodotti e ti propone ricette con ciò che hai.")
    val byShop = s.pantry.groupBy { it.foodId?.let { id -> Db.db.food(id)?.shop } ?: "Altro" }
    Shopping.SHOP_ORDER.filter { byShop.containsKey(it) }.forEach { shop ->
        val cardShape = RoundedCornerShape(28.dp)
        Column(Modifier.padding(bottom = 12.dp).fillMaxWidth().aShadow(cardShape, 8.dp, Color(0x661D1B3A)).clip(cardShape).background(Color.White).border(1.dp, A.Hair, cardShape).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(shopDot(shop)))
                GapW(10.dp)
                Text(shop.uppercase(), style = T.Eyebrow.copy(fontSize = 12.5.sp, letterSpacing = 0.6.sp, color = A.Muted))
            }
            byShop[shop]!!.forEachIndexed { i, p ->
                var edit by remember(p.id) { mutableStateOf(false) }
                var g by remember(p.id) { mutableStateOf(p.grams?.toInt()?.toString() ?: "") }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { edit = !edit }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                        Text((p.grams?.let { "${it.toInt()} ${if (Db.db.food(p.foodId ?: "")?.liquid == true) "ml" else "g"}" } ?: "quantità non indicata") +
                                (if (p.foodId == null) " · non nel database" else ""), style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF6F1EA)).clickable { Store.removePantry(p.id) }, contentAlignment = Alignment.Center) {
                        AIcon(Ic.Close, A.Ink, 18.dp, 2.4f)
                    }
                }
                if (edit) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    JField(g, { g = it.filter { c -> c.isDigit() } }, "Quantità", Modifier.weight(1f), number = true, suffix = "g")
                    Spacer(Modifier.width(8.dp))
                    PrimaryButton("Salva") { Store.updatePantry(p.copy(grams = g.toDoubleOrNull())); edit = false }
                }
            }
        }
    }
    if (s.pantry.isNotEmpty()) Text("Svuota dispensa", style = T.Link.copy(fontSize = 13.sp), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { Store.clearPantry() }.padding(vertical = 12.dp))
}

/** Scheda larga e comoda da toccare: icona grande a sinistra, titolo e spiegazione, freccia a destra. */
@Composable
private fun ScanTile(icon: String, title: String, subtitle: String, bg: Brush, ink: Color, glow: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(Modifier.fillMaxWidth().aShadow(shape, 10.dp, glow).clip(shape).background(bg).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(22.dp)).background(Color.White), contentAlignment = Alignment.Center) { AIcon(icon, ink, 30.dp, 2f) }
        GapW(16.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = A.Ink))
            GapH(2.dp)
            Text(subtitle, style = T.Small.copy(fontSize = 13.sp, lineHeight = 18.sp, color = A.Ink.copy(alpha = 0.65f)))
        }
        GapW(10.dp)
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { AIcon(Ic.Right, ink, 18.dp, 2.4f) }
    }
}

@Composable
private fun ShoppingTab(s: AppState) {
    val ctx = LocalContext.current
    var manual by remember { mutableStateOf("") }
    val range = s.shoppingRange.split("|").takeIf { it.size == 2 }
    Text(range?.let { "Per i pasti dal ${LocalDate.parse(it[0]).dayOfMonth}/${LocalDate.parse(it[0]).monthValue} per ${it[1]} giorni, già tolto ciò che hai in dispensa" }
        ?: "Genera la lista dai pasti del tuo piano: quantità calcolate e arrotondate alle confezioni.",
        style = T.Body.copy(fontSize = 13.5.sp, lineHeight = 20.sp, color = A.Muted), modifier = Modifier.padding(horizontal = 4.dp))
    GapH(12.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(3, 7).forEach { d ->
            APill("Prossimi $d giorni", false, Modifier.padding(vertical = 2.dp), height = 48.dp, hPad = 22.dp) {
                val today = LocalDate.now()
                if ((0 until d).any { Store.dayPlan(Store.current, today.plusDays(it.toLong())) == null }) {
                    Store.generatePlan(today.with(DayOfWeek.MONDAY), 14)
                }
                Store.buildShopping(today, d)
                Toast.makeText(ctx, "Lista aggiornata", Toast.LENGTH_SHORT).show()
            }
        }
        if (s.shopping.any { it.checked }) AInk("Spuntati → dispensa", height = 44.dp, bg = A.GreenInk) { Store.moveCheckedToPantry() }
    }
    GapH(12.dp)
    val fs = RoundedCornerShape(30.dp)
    Row(Modifier.fillMaxWidth().height(56.dp).clip(fs).background(Color.White).border(1.dp, Color(0x141D1B3A), fs).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (manual.isEmpty()) Text("Aggiungi un articolo", style = T.Body.copy(fontSize = 15.sp, color = Color(0xFF9A98B0)))
            androidx.compose.foundation.text.BasicTextField(manual, { manual = it }, singleLine = true, textStyle = T.Body.copy(fontSize = 15.sp, color = A.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Accent), modifier = Modifier.fillMaxWidth())
        }
        PlusButton("Aggiungi", 44.dp) { if (manual.isNotBlank()) { Store.addShoppingManual(manual); manual = "" } }
    }
    GapH(12.dp)
    if (s.shopping.isEmpty()) EmptyState("🛒", "Lista vuota", "Crea il piano nella scheda Piano, poi genera la lista per i prossimi giorni.")
    val groups = s.shopping.groupBy { it.shop }
    Shopping.SHOP_ORDER.filter { groups.containsKey(it) }.forEach { shop ->
        val cardShape = RoundedCornerShape(28.dp)
        Column(Modifier.padding(bottom = 12.dp).fillMaxWidth().aShadow(cardShape, 8.dp, Color(0x661D1B3A)).clip(cardShape).background(Color.White).border(1.dp, A.Hair, cardShape).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(shopDot(shop)))
                GapW(10.dp)
                Text(shop.uppercase(), style = T.Eyebrow.copy(fontSize = 12.5.sp, letterSpacing = 0.6.sp, color = A.Muted))
            }
            groups[shop]!!.forEach { it ->
                val f = it.foodId?.let { id -> Db.db.food(id) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { Store.toggleShopping(it.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(9.dp)).background(if (it.checked) A.Ink else Color.Transparent)
                            .border(2.dp, if (it.checked) A.Ink else Color(0xFFC9C5D8), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) { if (it.checked) AIcon(Ic.Check, Color.White, 16.dp, 3.2f) }
                    GapW(14.dp)
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (it.checked) Color(0xFFA6A4BC) else A.Ink,
                            textDecoration = if (it.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null))
                        val d = Shopping.describe(it, f)
                        if (d.isNotBlank()) Text(d + if (it.note.isNotBlank() && !it.manual) " · ${it.note}" else "", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF6F1EA)).clickable { Store.removeShopping(it.id) }, contentAlignment = Alignment.Center) {
                        AIcon(Ic.Close, A.Ink, 18.dp, 2.4f)
                    }
                }
            }
        }
    }
}

/** Foto di frigo/dispensa o scontrino -> prodotti riconosciuti, da confermare prima di salvarli. */
@Composable
fun PantryScanScreen(mode: String) {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val found = remember { mutableStateListOf<Pair<PantryGuess, Boolean>>() }
    val photoFile = remember { File(ctx.cacheDir, "foto").apply { mkdirs() }.let { File(it, "dispensa.jpg") } }
    val photoUri = remember { FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", photoFile) }
    val receipt = mode == "scontrino"

    fun analyze(b: Bitmap) {
        busy = true; error = null; info = null; found.clear()
        scope.launch {
            try {
                val list: List<PantryGuess> = if (receipt) {
                    val text = Ocr.read(b)
                    if (text.isBlank()) throw RuntimeException("Non riesco a leggere il testo: prova con più luce e lo scontrino ben disteso.")
                    if (s.ai.apiKey.isNotBlank()) runCatching { FoodAi.pantryFromReceipt(s.ai, Db.db, text) }.getOrElse {
                        info = "AI non disponibile (${it.message}): uso il riconoscimento offline."; Ocr.parseReceiptOffline(Db.db, text)
                    } else { info = "Riconoscimento offline dal testo dello scontrino. Con la chiave AI gratuita è più preciso."; Ocr.parseReceiptOffline(Db.db, text) }
                } else {
                    if (s.ai.apiKey.isNotBlank()) FoodAi.pantryFromPhoto(s.ai, Db.db, b)
                    else {
                        info = "Senza chiave AI leggo solo le scritte sulle confezioni (offline)."
                        Ocr.parseReceiptOffline(Db.db, Ocr.read(b))
                    }
                }
                found.addAll(list.map { it to (it.foodId != null) })
                if (list.isEmpty()) error = "Nessun alimento riconosciuto."
            } catch (e: Exception) { error = e.message }
            busy = false
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) scope.launch { bitmap = withContext(Dispatchers.IO) { loadBitmap(ctx, photoUri, 2000) }; bitmap?.let { analyze(it) } }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { bitmap = withContext(Dispatchers.IO) { loadBitmap(ctx, uri, 2000) }; bitmap?.let { analyze(it) } }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (receipt) "Scontrino della spesa" else "Frigo e dispensa", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val b = bitmap
            if (b == null) JCard {
                Text(if (receipt) "🧾" else "🧊", style = T.Big)
                Text(if (receipt) "Fotografa lo scontrino" else "Fotografa frigorifero o dispensa", style = T.Title)
                Text(if (receipt) "Il testo viene letto sul telefono (senza internet); l'AI abbina ogni riga a un alimento e alla quantità acquistata."
                else "L'AI riconosce i prodotti visibili e stima le quantità. Potrai correggere tutto prima di salvare.", style = T.Read.copy(color = C.Muted))
            } else Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Scatta foto", Modifier.weight(1f)) { camera.launch(photoUri) }
                GhostButton("Galleria", Modifier.weight(1f)) { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            }
            if (busy) Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), color = C.Accent, strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Riconoscimento in corso…", style = T.Muted)
            }
            info?.let { Text(it, style = T.Small, modifier = Modifier.padding(top = 10.dp)) }
            error?.let { Text(it, style = T.Label.copy(color = C.Danger), modifier = Modifier.padding(top = 10.dp)) }
            if (found.isNotEmpty()) {
                SectionTitle("Prodotti riconosciuti (${found.count { it.second }})")
                JCard(padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    found.forEachIndexed { i, (g, sel) ->
                        val f = g.foodId?.let { Db.db.food(it) }
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(sel, { v -> found[i] = g to v }, colors = CheckboxDefaults.colors(checkedColor = C.Green, checkmarkColor = C.OnAccent))
                            Column(Modifier.weight(1f)) {
                                Text(f?.name ?: g.name, style = T.Label)
                                Text((if (f == null) "non nel database · " else if (!f.name.equals(g.name, true)) "da \"${g.name}\" · " else "") +
                                        (g.grams?.let { "${it.toInt()} ${if (f?.liquid == true) "ml" else "g"}" } ?: "quantità da indicare"), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (i < found.lastIndex) HorizontalDivider(color = C.Line)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (found.any { it.second }) PrimaryButton("Aggiungi ${found.count { it.second }} prodotti alla dispensa", Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), color = C.Accent) {
            Store.addPantry(found.filter { it.second }.map { (g, _) ->
                PantryItem(Store.newId(), g.foodId, g.foodId?.let { Db.db.food(it)?.name } ?: g.name, g.grams, source = if (receipt) "scontrino" else "foto")
            })
            Toast.makeText(ctx, "Dispensa aggiornata", Toast.LENGTH_SHORT).show()
            Nav.back(); Nav.tab = 2; Nav.kitchenTab = 0
        }
    }
}

/** Ricette realizzabili con la dispensa, calcolate sull'obiettivo del pasto; più ricetta inventata dall'AI. */
@Composable
fun PantryIdeasScreen(initialMeal: String?) {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var meal by remember { mutableStateOf(initialMeal ?: when (defaultMeal()) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; else -> "spuntino" }) }
    var busy by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val energy = Store.energyFor(s, today)
    val slotIdx = s.dietPrefs.slots.indexOfFirst { it.meal == meal }.coerceAtLeast(0)
    val target = Planner.slotTargets(energy.macro, s.dietPrefs)[slotIdx]
    val diaryMeal = s.dietPrefs.slots.getOrNull(slotIdx)?.diaryMeal ?: 1
    val ideasN by produceState<List<com.jarvis.food.engine.PantrySuggestion>?>(null, s.pantry, meal, target, s.dietPrefs) {
        val pantry = s.pantry; val prefs = s.dietPrefs
        value = withContext(Dispatchers.Default) { Kitchen.suggest(Db.db, pantry, meal, { target }, { Planner.allowed(Db.db, it, prefs) }) }
    }
    val ideas = ideasN.orEmpty()
    Column(Modifier.fillMaxSize()) {
        TopBar("Cosa cucino?", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("colazione" to "Colazione", "pranzo" to "Pranzo", "spuntino" to "Spuntino", "cena" to "Cena").forEach { (k, l) -> Pill(l, meal == k) { meal = k } }
            }
            Spacer(Modifier.height(8.dp))
            Text("Obiettivo ${meal}: ${target.kcal.r0()} kcal · P ${target.protein.r0()} · C ${target.carbs.r0()} · G ${target.fat.r0()} g", style = T.Small)
            Spacer(Modifier.height(12.dp))
            JCard(color = C.AccentSoft) {
                Text("✨ Ricetta nuova con l'AI", style = T.Title2)
                Text("L'AI propone un piatto con i tuoi ingredienti; grammature e valori li calcola Jarvis dal database.", style = T.Muted)
                Spacer(Modifier.height(10.dp))
                PrimaryButton(if (busy) "Sto pensando…" else "Inventa una ricetta", Modifier.fillMaxWidth(), enabled = !busy && s.pantry.any { it.foodId != null }, color = C.Accent) {
                    if (s.ai.apiKey.isBlank()) { Nav.go(Screen.AiSettings); return@PrimaryButton }
                    busy = true
                    scope.launch {
                        runCatching { FoodAi.inventRecipe(s.ai, Db.db, s.pantry.mapNotNull { it.foodId }, meal, target, s.dietPrefs.exclude) }
                            .onSuccess { tpl -> Nav.go(Screen.RecipeView(tpl.id, target, today, null, diaryMeal, adHoc = tpl)) }
                            .onFailure { Toast.makeText(ctx, it.message ?: "Errore", Toast.LENGTH_LONG).show() }
                        busy = false
                    }
                }
            }
            SectionTitle("Dal ricettario, con ciò che hai")
            if (ideasN != null && ideas.isEmpty()) EmptyState("🥕", "Nessuna corrispondenza", "Aggiungi altri prodotti alla dispensa o prova un altro pasto.")
            ideas.forEach { sg ->
                JCard(padding = PaddingValues(12.dp), onClick = { Nav.go(Screen.RecipeView(sg.solved.template.id, target, today, null, diaryMeal)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RecipeImage(sg.solved.template.id, sg.solved.template.image, s.recipePhotos[sg.solved.template.id], Modifier.size(80.dp), "🍽️", 14.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sg.solved.template.name, style = T.Title2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${sg.solved.total.kcal.r0()} kcal · hai ${(sg.coverage * 100).r0()}% degli ingredienti", style = T.Small)
                            if (sg.missing.isNotEmpty()) Text("Manca: " + sg.missing.joinToString { it.name.lowercase() }, style = T.Small.copy(color = C.AccentDeep), maxLines = 2)
                        }
                    }
                    if (sg.missing.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("+ Aggiungi i mancanti alla spesa", style = T.Label.copy(color = C.AccentDeep), modifier = Modifier.clickable {
                            sg.missing.forEach { f -> Store.update { st -> st.copy(shopping = st.shopping + com.jarvis.food.data.ShoppingItem(Store.newId(), f.id, f.name,
                                packs = 1, packGrams = f.pack ?: 100.0, shop = f.shop)) } }
                            Toast.makeText(ctx, "Aggiunti alla lista della spesa", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

