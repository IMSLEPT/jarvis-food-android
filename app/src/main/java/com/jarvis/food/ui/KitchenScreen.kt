package com.jarvis.food.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jarvis.food.R
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun KitchenScreen() {
    val s by Store.state.collectAsState()
    val scroll = rememberScrollState()
    LaunchedEffect(Nav.kitchenTab) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
        val order = listOf(2, 0, 1)
        NavyBanner("Cucina", listOf("Ricette", "Dispensa", "Spesa"), order.indexOf(Nav.kitchenTab),
            onSelect = { Nav.kitchenTab = order[it] }, query = "", onQuery = {}, hint = "",
            labelTags = order.map { "kitchen-tab-label-$it" }, icons = listOf(RIc.Fork, RIc.Basket, RIc.Cart), logo = { ChefHatLogo(RD.BannerLogo) }, searchContent = {
                if (Nav.kitchenTab == 2) RecipeSearchBar()
                else Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.65f))
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        AIcon(if (Nav.kitchenTab == 0) Ic.Fridge else Ic.Cart, Color.White, 20.dp)
                        GapW(8.dp)
                        Text(if (Nav.kitchenTab == 0) "La tua dispensa" else "La tua lista della spesa",
                            style = T.Body.copy(lineHeight = 18.sp, color = Color.White))
                    }
                }
            })
        Column(Modifier.weight(1f).fillMaxWidth().background(C.Bg).testTag("main-scroll").verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            if (Nav.kitchenTab != 2) GapH(12.dp)
            when (Nav.kitchenTab) {
                0 -> PantryTab(s)
                1 -> ShoppingTab(s)
                else -> RecipesHome()
            }
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

private fun shoppingCategoryIcon(shop: String): String = when (shop) {
    "Frutta e verdura" -> RIc.Leaf
    "Pane, pasta e cereali" -> DashboardIcon.Wheat
    "Carne e pesce" -> "M3 12c4-5 9-6 14-2l4-3v10l-4-3c-5 4-10 3-14-2zM8 11h.1"
    "Latticini e uova" -> "M8 3h8v4l3 4v9H5v-9l3-4zM8 7h8M5 11h14M9 15h6"
    "Legumi e tofu" -> "M17 4c4 1 5 6 3 10-2 4-6 7-11 6-4-1-6-4-4-7 1-2 4-2 6-4 2-2 2-6 6-5zM7 16c4 2 9-2 10-6"
    "Frutta secca e semi" -> "M5 18c-3-3-1-7 3-11 3-3 7-4 10-3 1 3 0 7-3 11-4 4-7 6-10 3zM7 16l8-9"
    "Dispensa" -> "M8 4h8v3l2 3v9a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-9l2-3zM8 7h8M6 13h12"
    "Condimenti" -> "M10 3h4v5l3 3v8a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-8l3-3zM7 13h10"
    "Bevande" -> Ic.Drop
    else -> Ic.Cart
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

    Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        PeachTile(36.dp) { ChefHatLogo(23.dp) }
        GapW(8.dp)
        Text("Cosa cucino oggi", style = T.Title2)
    }
    val cs = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().refShadow(cs, 4.dp).clip(cs)
        .background(Color.White).border(1.dp, A.Hair, cs).padding(14.dp)) {
        Text("Ricette dal ricettario con ciò che hai in dispensa, calcolate sul tuo obiettivo.", style = T.Body.copy(fontSize = 14.sp, lineHeight = 21.sp, color = A.Muted))
        val compact = LocalDensity.current.fontScale <= 1.15f && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 350
        Row(Modifier.padding(top = 12.dp).fillMaxWidth().then(if (compact) Modifier else Modifier.horizontalScroll(rememberScrollState())),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("pranzo" to "Pranzo", "cena" to "Cena", "colazione" to "Colazione", "spuntino" to "Spuntino").forEach { (k, l) ->
                val sel = meal == k
                val chip = RoundedCornerShape(24.dp)
                Box((if (compact) Modifier.weight(if (l.length > 6) 1.18f else 0.82f) else Modifier).heightIn(min = 48.dp)
                    .clip(chip).background(if (sel) C.Navy else Color.White)
                    .border(if (sel) 1.5.dp else 1.dp, if (sel) RD.OrangeLine else A.Hair, chip)
                    .selectable(selected = sel, role = Role.Tab) { meal = k }
                    .padding(horizontal = if (compact) 4.dp else 12.dp), contentAlignment = Alignment.Center) {
                    Text(l, style = T.Chip.copy(fontSize = 12.sp, color = if (sel) Color.White else A.Ink), textAlign = TextAlign.Center)
                }
            }
        }
        GapH(14.dp)
        val bs = RoundedCornerShape(28.dp)
        val enabled = !busy && s.pantry.any { it.foodId != null }
        Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(bs)
            .background(if (enabled) RD.OrangeBrush else RD.PeachBrush).clickable(enabled = enabled, role = Role.Button) {
            if (s.ai.apiKey.isBlank()) { Nav.go(Screen.AiSettings); return@clickable }
            busy = true
            scope.launch {
                runCatching { FoodAi.inventRecipe(s.ai, Db.db, s.pantry.mapNotNull { it.foodId }, meal, target, s.dietPrefs.exclude) }
                    .onSuccess { tpl -> Nav.go(Screen.RecipeView(tpl.id, target, today, null, diaryMeal, adHoc = tpl)) }
                    .onFailure { Toast.makeText(ctx, it.message ?: "Errore", Toast.LENGTH_LONG).show() }
                busy = false
            }
        }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val actionInk = if (enabled) Color.White else C.AccentDeep
            AIcon(Ic.Spark, actionInk, 22.dp, 2f)
            GapW(10.dp)
            Text(if (busy) "Sto pensando…" else "Ricetta nuova con l'AI", style = T.BtnM.copy(color = actionInk),
                modifier = Modifier.weight(1f))
            AIcon(Ic.Right, actionInk, 16.dp)
        }
        if (!enabled && !busy) Text("Aggiungi alla dispensa un ingrediente riconosciuto per creare una ricetta.", style = T.Small, modifier = Modifier.padding(top = 8.dp))
    }
    GapH(14.dp)
    if (s.pantry.isEmpty()) {
        val hintShape = RoundedCornerShape(18.dp)
        Row(Modifier.fillMaxWidth().clip(hintShape).background(C.Surface).border(1.dp, A.Hair, hintShape)
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AIcon("M12 7v.2M12 10v6" + Ic.circ(12.0, 12.0, 9.0), C.Navy, 17.dp)
            GapW(8.dp)
            Text("Aggiungi qualcosa alla dispensa per vedere le ricette possibili.", style = T.Small)
        }
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
                Text("Manca: " + sg.missing.joinToString { it.name.lowercase() }, style = T.Small.copy(fontSize = 12.5.sp, color = C.AccentDeep), modifier = Modifier.padding(top = 10.dp), maxLines = 2)
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
    ScanTile(Ic.Fridge, "Fotografa frigo o dispensa", "Riconosco gli alimenti dalla foto e li aggiungo", Brush.verticalGradient(listOf(Color(0xFFE2EFFB), Color(0xFFEAF4FE))), C.Navy) { Nav.go(Screen.PantryScan("foto")) }
    GapH(12.dp)
    ScanTile(Ic.Receipt, "Fotografa lo scontrino", "Leggo i prodotti acquistati e li metto in dispensa", RD.PeachBrush, C.AccentDeep) { Nav.go(Screen.PantryScan("scontrino")) }

    CookTodayBlock(s)

    ASection("In dispensa", top = 18.dp)
    GapH(12.dp)
    // Aggiunta a mano
    val fs = RoundedCornerShape(30.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(fs).background(Color.White).border(1.dp, Color(0x141D1B3A), fs).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (name.isEmpty()) Text("Aggiungi a mano (es. uova, spinaci)", style = T.Body.copy(fontSize = 15.sp, color = C.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.foundation.text.BasicTextField(name, { name = it }, singleLine = true, textStyle = T.Body.copy(fontSize = 15.sp, color = A.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Accent), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Ingrediente da aggiungere alla dispensa" })
        }
        PlusButton("Aggiungi", 48.dp) {
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
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { edit = !edit }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                        Text((p.grams?.let { "${it.toInt()} ${if (Db.db.food(p.foodId ?: "")?.liquid == true) "ml" else "g"}" } ?: "quantità non indicata") +
                                (if (p.foodId == null) " · non nel database" else ""), style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).background(C.Surface).clickable { Store.removePantry(p.id) }, contentAlignment = Alignment.Center) {
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
private fun ScanTile(icon: String, title: String, subtitle: String, bg: Brush, ink: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(Modifier.fillMaxWidth().refShadow(shape, 4.dp).clip(shape).background(Color.White)
        .border(1.dp, C.Line, shape).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        PeachTile(44.dp, brush = bg) { AIcon(icon, ink, 27.dp, 2f, modifier = Modifier.testTag("scan-icon-$title")) }
        GapW(12.dp)
        OrangeDivider(36.dp)
        GapW(12.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label.copy(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = RD.Ink))
            GapH(2.dp)
            Text(subtitle, style = T.Small.copy(fontSize = 13.sp, lineHeight = 18.sp, color = C.Muted))
        }
        GapW(6.dp)
        AIcon(Ic.Right, RD.Ink, 16.dp, 2.4f)
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(3, 7).forEach { d ->
            val selected = range?.get(1)?.toIntOrNull() == d
            val shape = RoundedCornerShape(24.dp)
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                .background(if (selected) RD.Peach else Color.White)
                .border(1.dp, if (selected) RD.OrangeLine else C.Line, shape)
                .clickable(role = Role.Button) {
                val today = LocalDate.now()
                if ((0 until d).any { Store.dayPlan(Store.current, today.plusDays(it.toLong())) == null }) {
                    Store.generatePlan(today.with(DayOfWeek.MONDAY), 14)
                }
                Store.buildShopping(today, d)
                Toast.makeText(ctx, "Lista aggiornata", Toast.LENGTH_SHORT).show()
            }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                AIcon(Ic.Calendar, if (selected) C.AccentDeep else RD.Navy, 19.dp)
                GapW(6.dp)
                Text("Prossimi $d giorni", style = T.Chip.copy(color = RD.Ink), textAlign = TextAlign.Center)
            }
        }
    }
    if (s.shopping.any { it.checked }) {
        GapH(8.dp)
        AInk("Spuntati → dispensa", height = 48.dp, bg = A.GreenInk) { Store.moveCheckedToPantry() }
    }
    GapH(12.dp)
    val fs = RoundedCornerShape(30.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(fs).background(Color.White).border(1.dp, Color(0x141D1B3A), fs).padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (manual.isEmpty()) Text("Aggiungi un articolo", style = T.Body.copy(fontSize = 15.sp, color = C.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.foundation.text.BasicTextField(manual, { manual = it }, singleLine = true, textStyle = T.Body.copy(fontSize = 15.sp, color = A.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Accent), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Articolo da aggiungere alla lista della spesa" })
        }
        PlusButton("Aggiungi", 48.dp) { if (manual.isNotBlank()) { Store.addShoppingManual(manual); manual = "" } }
    }
    GapH(12.dp)
    if (s.shopping.isEmpty()) EmptyState("🛒", "Lista vuota", "Crea il piano nella scheda Piano, poi genera la lista per i prossimi giorni.")
    val groups = s.shopping.groupBy { it.shop }
    Shopping.SHOP_ORDER.filter { groups.containsKey(it) }.forEach { shop ->
        val cardShape = RoundedCornerShape(28.dp)
        Column(Modifier.padding(bottom = 12.dp).fillMaxWidth().aShadow(cardShape, 8.dp, Color(0x661D1B3A)).clip(cardShape).background(Color.White).border(1.dp, A.Hair, cardShape).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                AIcon(shoppingCategoryIcon(shop), shopDot(shop), 20.dp, 2.2f)
                GapW(10.dp)
                Text(shop.uppercase(), style = T.Eyebrow.copy(fontSize = 13.sp, letterSpacing = 0.4.sp, color = RD.Ink))
            }
            groups[shop]!!.forEach { it ->
                val f = it.foodId?.let { id -> Db.db.food(id) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { Store.toggleShopping(it.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (it.checked) A.Ink else Color.Transparent)
                            .border(2.dp, if (it.checked) A.Ink else RD.GreyLine, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) { if (it.checked) AIcon(Ic.Check, Color.White, 16.dp, 3.2f) }
                    GapW(14.dp)
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (it.checked) C.Faint else A.Ink,
                            textDecoration = if (it.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null))
                        val d = Shopping.describe(it, f)
                        if (d.isNotBlank()) Text(d + if (it.note.isNotBlank() && !it.manual) " · ${it.note}" else "", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { Store.removeShopping(it.id) }, contentAlignment = Alignment.Center) {
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

    fun analyze(uri: android.net.Uri) {
        if (busy) return
        busy = true
        error = null
        info = null
        found.clear()
        scope.launch {
            try {
                val b = withContext(Dispatchers.IO) { loadBitmap(ctx, uri, 2000) }
                    ?: throw IllegalArgumentException("Immagine illeggibile. Prova un'altra foto.")
                bitmap = b
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Impossibile analizzare la foto."
            } finally {
                busy = false
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) analyze(photoUri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) analyze(uri)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (receipt) "Scontrino della spesa" else "Frigo e dispensa", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val b = bitmap
            if (b == null) {
                val shape = RoundedCornerShape(22.dp)
                Column(Modifier.fillMaxWidth().refShadow(shape, 5.dp).clip(shape)
                    .background(Brush.linearGradient(listOf(Color.White, Color.White, RD.Peach)))
                    .border(1.dp, A.Hair, shape).padding(16.dp)) {
                    Image(painterResource(if (receipt) R.drawable.approved_receipt else R.drawable.approved_fridge),
                        null, Modifier.size(width = 111.dp, height = 88.dp), contentScale = ContentScale.Fit)
                    GapH(10.dp)
                    Text(if (receipt) "Fotografa lo scontrino" else "Fotografa frigorifero o dispensa",
                        style = T.Title2.copy(color = RD.Ink))
                    GapH(5.dp)
                    Text(if (receipt) "Il testo viene letto sul telefono (senza internet); l'AI abbina ogni riga a un alimento e alla quantità acquistata."
                    else "L'AI riconosce i prodotti visibili e stima le quantità. Potrai correggere tutto prima di salvare.",
                        style = T.Read.copy(color = C.Muted))
                }
            } else Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RefPrimaryButton("Scatta foto", Modifier.weight(1f), divider = false, enabled = !busy) {
                    runCatching { camera.launch(photoUri) }.onFailure { error = "Fotocamera non disponibile." }
                }
                RefOutlineButton("Galleria", Modifier.weight(1f), divider = false, height = 52.dp, enabled = !busy) {
                    runCatching { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .onFailure { error = "Impossibile aprire la galleria." }
                }
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

