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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.NowSuggestion
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.RecipeCats
import com.jarvis.food.engine.RecipeFilter
import com.jarvis.food.engine.RecipeHit
import com.jarvis.food.engine.RecipeInfo
import com.jarvis.food.engine.RecipeMeta
import com.jarvis.food.engine.RecipeSearch
import com.jarvis.food.engine.RecipeTemplate
import com.jarvis.food.engine.WhatNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max

// ---------------------------------------------------------------- Utilità condivise

internal fun recEmoji(meal: String) = when (meal) { "colazione" -> "☕"; "pranzo" -> "🍝"; "cena" -> "🍽️"; "dessert" -> "🍰"; else -> "🍎" }

internal fun pantryIds(s: AppState): Set<String> = s.pantry.mapNotNull { it.foodId?.removePrefix("db:") }.toSet()

/** Obiettivo del pasto e pasto del diario in cui di solito si mangia questa ricetta. */
internal fun slotTargetFor(s: AppState, t: RecipeTemplate): Pair<Macro, Int> {
    val key = if (t.meal == "dessert") "spuntino" else t.meal
    val slots = s.dietPrefs.slots
    val idx = slots.indexOfFirst { it.meal == key }.takeIf { it >= 0 } ?: 0
    val target = Planner.slotTargets(Store.energyFor(s, LocalDate.now()).macro, s.dietPrefs)[idx]
    return target to (slots.getOrNull(idx)?.diaryMeal ?: 1)
}

/** Calorie e macro che restano nella giornata (mai negativi). */
internal fun dayRemaining(s: AppState, date: LocalDate): Macro {
    val e = Store.energyFor(s, date).macro
    val eaten = Nutrition.sum(Store.entriesFor(s, date))
    return Macro(max(0.0, e.kcal - eaten.kcal), max(0.0, e.protein - eaten.protein), max(0.0, e.carbs - eaten.carbs), max(0.0, e.fat - eaten.fat))
}

internal data class NowCtx(val remaining: Macro, val target: Macro, val plannedUsed: Boolean, val plannedOthers: Int)

/**
 * Obiettivo per il prossimo pasto: i macro rimasti divisi tra questo pasto e quelli che mancano ancora
 * (secondo la ripartizione scelta). Con [usePlan] si sottraggono invece i pasti già pianificati e non mangiati.
 */
internal fun nowContext(s: AppState, key: String, usePlan: Boolean): NowCtx {
    val today = LocalDate.now()
    val rem = dayRemaining(s, today)
    val diaryMeal = WhatNow.diaryMealForKey(key)
    val slots = s.dietPrefs.slots
    val rank = { m: Int -> MEAL_ORDER.indexOf(m) }
    val eatenMeals = Store.entriesFor(s, today).map { it.meal }.toSet()
    val cur = slots.firstOrNull { it.meal == key } ?: slots.first()
    val left = slots.filter { rank(it.diaryMeal) > rank(diaryMeal) && it.diaryMeal !in eatenMeals }
    val denom = (cur.share + left.sumOf { it.share }).coerceAtLeast(1)
    var t = rem * (cur.share.toDouble() / denom)
    var used = false
    var others = 0
    if (usePlan) {
        val dp = Store.dayPlan(s, today)
        val pending = Store.plannedMeals(s, today).filter { pm ->
            pm.diaryMeal != diaryMeal && dp?.slots?.firstOrNull { it.slot == pm.slotIndex }?.eatenEntryIds?.isEmpty() != false
        }
        others = pending.size
        if (pending.isNotEmpty()) {
            val t2 = rem - pending.fold(Macro.ZERO) { a, pm -> a + pm.solved.total }
            if (t2.kcal >= 0.25 * rem.kcal && t2.kcal >= 100) { t = t2; used = true }
        }
    }
    t = Macro(max(t.kcal, 120.0), max(t.protein, 5.0), max(t.carbs, 8.0), max(t.fat, 3.0))
    return NowCtx(rem, t, used, others)
}

private fun diffText(v: Double, unit: String = ""): String = (if (v >= 0) "+" else "−") + abs(v).r0() + unit

// ---------------------------------------------------------------- Schede ricetta

/** Riga di schede: con 1–2 elementi occupano tutta la larghezza (niente spazio vuoto), con più elementi scorrono in orizzontale. */
@Composable
private fun <T> TileStrip(items: List<T>, content: @Composable (T, Modifier) -> Unit) {
    if (items.size <= 2) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { content(it, Modifier.weight(1f)) }
            if (items.size == 1) Box(Modifier.weight(1f))
        }
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { content(it, Modifier.width(TILE_W)) }
        }
    }
}

private val TILE_W = 166.dp

/** Riga "kcal · minuti" con icone piccole: stessa altezza in tutte le schede. */
@Composable
private fun TileMeta(kcal: String, minutes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AIcon(Ic.Flame, C.Accent, 13.dp, 2f); GapW(3.dp)
        Text(kcal, style = T.Small.copy(fontSize = 12.sp, color = A.Ink.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold), maxLines = 1)
        GapW(10.dp)
        AIcon(Ic.Clock, A.Muted, 13.dp, 2f); GapW(3.dp)
        Text("$minutes min", style = T.Small.copy(fontSize = 12.sp, color = A.Ink.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun RecipeTile(tpl: RecipeTemplate, info: RecipeInfo, s: AppState, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.aShadow(shape, 6.dp, Color(0x4D1D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape).clickable(onClick = onClick)) {
        Box {
            RecipeImage(tpl.id, tpl.image, s.recipePhotos[tpl.id], Modifier.fillMaxWidth().aspectRatio(1.4f), recEmoji(tpl.meal), 0.dp)
            if (tpl.id in s.recipeFavs) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(Color(0xEBFFFFFF)), contentAlignment = Alignment.Center) {
                AIcon(Ic.Heart, C.Protein, 15.dp, 2f, fill = true)
            }
            if (badge != null) Box(Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xEBFFFFFF)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                Text(badge, style = T.Small.copy(fontWeight = FontWeight.SemiBold, color = A.Ink, fontSize = 11.sp), maxLines = 1)
            }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Text(tpl.name, style = T.Label.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            GapH(6.dp)
            TileMeta("${info.kcal.r0()} kcal", tpl.minutes)
        }
    }
}

@Composable
private fun UserRecipeTile(r: Recipe, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
    Column(modifier.aShadow(shape, 6.dp, Color(0x4D1D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape).clickable(onClick = onClick)) {
        RecipeImage(r.id, null, r.photo, Modifier.fillMaxWidth().aspectRatio(1.4f), r.emoji, 0.dp)
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Text(r.name, style = T.Label.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            GapH(6.dp)
            TileMeta("${per.kcal.r0()} kcal", r.minutes)
        }
    }
}

@Composable
private fun RecipeRow(hit: RecipeHit, s: AppState, onClick: () -> Unit) {
    val t = hit.tpl; val i = hit.info
    val shape = RoundedCornerShape(26.dp)
    Row(Modifier.fillMaxWidth().aShadow(shape, 6.dp, Color(0x661D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box {
            RecipeImage(t.id, t.image, s.recipePhotos[t.id], Modifier.size(88.dp), recEmoji(t.meal), 18.dp)
            if (t.id in s.recipeFavs) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Color(0xEBFFFFFF)), contentAlignment = Alignment.Center) {
                AIcon(Ic.Heart, C.Protein, 12.dp, 2f, fill = true)
            }
        }
        GapW(12.dp)
        Column(Modifier.weight(1f)) {
            Text(RecipeMeta.label(RecipeMeta.MEALS, t.meal) + " · ${t.minutes} min · " + RecipeMeta.DIFFICULTY.getOrElse(t.difficulty) { "" }, style = T.Small, maxLines = 1)
            Text(t.name, style = T.Label.copy(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            GapH(3.dp)
            Text("${i.kcal.r0()} kcal · P ${i.protein.r0()} · C ${i.carbs.r0()} · G ${i.fat.r0()}", style = T.Small.copy(color = A.Ink), maxLines = 1)
            if (s.pantry.isNotEmpty() && hit.fridge > 0) Text("In dispensa: ${(hit.fridge * 100).toInt()}% degli ingredienti", style = T.Small.copy(color = A.GreenInk), maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- Home Ricette

private fun openRecipe(s: AppState, t: RecipeTemplate) {
    val (target, dm) = slotTargetFor(s, t)
    Nav.go(Screen.RecipeView(t.id, target, LocalDate.now(), null, dm, original = true))
}

/** Colori pastello delle tessere (sfondo, macchia decorativa): ogni tessera ha il suo, come su Yazio. */
private val CAT_BASE = listOf(
    Color(0xFFDDEEFA) to Color(0xFFC4E0F4), Color(0xFFE6E0F8) to Color(0xFFD3C9F1), Color(0xFFDAF4E5) to Color(0xFFBFEAD3), Color(0xFFFFE5D3) to Color(0xFFFFD1B3),
    Color(0xFFFFF0C4) to Color(0xFFFFE19B), Color(0xFFFFDFE8) to Color(0xFFFFC8D8), Color(0xFFD6F2F2) to Color(0xFFB8E5E5), Color(0xFFF3E8D8) to Color(0xFFE8D6BA)
)
/** Stesse tinte di prima, ma più tenui (meno sature): sfondo schiarito del 38%, macchia molto vicina allo sfondo, così le scritte restano ben leggibili. */
private val CAT_COLORS = CAT_BASE.map { (bg, blob) ->
    val soft = androidx.compose.ui.graphics.lerp(bg, Color.White, 0.38f)
    soft to androidx.compose.ui.graphics.lerp(androidx.compose.ui.graphics.lerp(blob, Color.White, 0.38f), soft, 0.4f)
}
private val CAT_OFFSET = mapOf("pasto" to 0, "kcal" to 1, "obiettivo" to 3, "dieta" to 5, "metodo" to 2, "cucina" to 6, "tempo" to 4)

/** Tessera di categoria in stile Yazio: sfondo pieno colorato con macchie decorative, emoji grande, etichetta in grassetto e numero di ricette. */
@Composable
private fun CatTile(emoji: String, label: String, count: Int, colorIdx: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val (bg, blob) = CAT_COLORS[Math.floorMod(colorIdx, CAT_COLORS.size)]
    Column(modifier.height(132.dp).aShadow(shape, 5.dp, Color(0x331D1B3A)).clip(shape).background(bg)
        .drawBehind {
            drawCircle(blob, radius = size.width * 0.62f, center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 1.02f))
            drawCircle(blob.copy(alpha = 0.55f), radius = size.width * 0.28f, center = androidx.compose.ui.geometry.Offset(size.width * 0.02f, size.height * 0.08f))
        }
        .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(emoji, fontSize = 38.sp, lineHeight = 44.sp)
        GapH(8.dp)
        Text(label, style = T.BtnS.copy(color = A.Ink, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("$count ricette", style = T.Small.copy(color = A.Ink.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** Gruppo di categorie: scorre in orizzontale (tessere larghe 118 dp). */
@Composable
private fun CatGroupRow(g: com.jarvis.food.engine.RecipeCatGroup) {
    val off = CAT_OFFSET[g.id] ?: 0
    Text(g.title, style = T.Label.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        g.cats.forEachIndexed { i, c -> CatTile(c.emoji, c.label, c.count, off + i, Modifier.width(118.dp)) { Nav.go(Screen.RecipeList(c.filter, c.listTitle)) } }
    }
}

/** Fasce di calorie: griglia fissa 4 × 2, sempre tutta visibile (es. "Sotto 100 kcal" si trova subito). */
@Composable
private fun CatKcalGrid(g: com.jarvis.food.engine.RecipeCatGroup, cols: Int = 4) {
    val off = CAT_OFFSET[g.id] ?: 0
    Text(g.title, style = T.Label.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
    g.cats.chunked(cols).forEachIndexed { row, items ->
        if (row > 0) GapH(10.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { i, c -> CatTile(c.emoji, c.label, c.count, off + row * cols + i, Modifier.weight(1f)) { Nav.go(Screen.RecipeList(c.filter, c.listTitle)) } }
            repeat(cols - items.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun CatGroupView(g: com.jarvis.food.engine.RecipeCatGroup) { when (g.id) { "kcal" -> CatKcalGrid(g, 4); "tempo" -> CatKcalGrid(g, 3); else -> CatGroupRow(g) } }

/** Segnaposto mentre le categorie si calcolano in background (di solito sono già pronte all'apertura). */
@Composable
private fun CatPlaceholder() {
    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = A.Muted, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) }
}

/** Dati della home calcolati fuori dal thread principale. */
internal class HomeData(val ctx: NowCtx, val recommended: List<Pair<RecipeTemplate, RecipeInfo>>, val discover: List<RecipeTemplate>)

/** Scheda "Ricette": home con sezioni orizzontali, ricetta del giorno, categorie reali e "Cosa mangio adesso?". */
@Composable
fun RecipesHome() {
    val s by Store.state.collectAsState()
    val db = Db.db
    val today = LocalDate.now()
    val disliked = s.dietPrefs.disliked.toSet()
    val nextKey = remember { WhatNow.keyForDiaryMeal(mealForHour(LocalTime.now().hour)) }
    // Tutto ciò che scorre le 2.500 ricette si calcola in background: la scheda si apre subito e le sezioni compaiono appena pronte.
    val home by produceState<HomeData?>(null, s.diary, s.plan, s.dietPrefs, s.energy, s.goals, s.profile, today) {
        val st = s
        value = withContext(Dispatchers.Default) {
            val c = nowContext(st, nextKey, true)
            val rec = db.recipes.filter { it.meal in WhatNow.mealsFor(nextKey) && Planner.allowed(db, it, st.dietPrefs) }
                .map { it to RecipeMeta.info(db, it) }
                .sortedBy { (_, i) -> abs(i.kcal - c.target.kcal) / max(c.target.kcal, 80.0) + max(0.0, c.target.protein * 0.8 - i.protein) / max(c.target.protein, 10.0) * 0.5 }
                .take(10)
            val pool = db.recipes.filter { it.image != null && it.id !in disliked && Planner.allowed(db, it, st.dietPrefs.copy(maxMinutes = 90)) }
            val disc = pool.sortedBy { Math.floorMod((it.id.hashCode() * 31) xor today.toEpochDay().toInt(), 100003) }.take(10)
            HomeData(c, rec, disc)
        }
    }
    val catGroups by produceState(RecipeCats.peek(db), db) { if (value == null) value = withContext(Dispatchers.Default) { RecipeCats.get(db) } }
    val ctx = home?.ctx

    // Ricerca
    val shape = RoundedCornerShape(26.dp)
    Row(Modifier.fillMaxWidth().height(56.dp).aShadow(shape, 6.dp, Color(0x4D1D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape)
        .clickable { Nav.go(Screen.RecipeList(RecipeFilter(), "Tutte le ricette")) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        AIcon(Ic.Search, A.Muted, 20.dp, 2.2f); GapW(10.dp)
        Text("Cerca ricette o ingredienti", style = T.Body.copy(color = A.Muted), modifier = Modifier.weight(1f))
        Text("${db.recipes.size}", style = T.Chip.copy(color = A.Muted))
    }

    // Cosa mangio adesso
    GapH(12.dp)
    ACard(brush = Brush.linearGradient(listOf(Color(0xFF13225F), Color(0xFF1B3382), Color(0xFF2B49A8))), glow = Color(0x8013225F), bordered = false, radius = 28.dp,
        onClick = { Nav.go(Screen.WhatNow) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cosa mangio adesso?", style = T.H2.copy(color = Color.White, fontSize = 20.sp))
                GapH(4.dp)
                Text(if (ctx == null) "Ti propongo piatti su misura per i tuoi obiettivi di oggi."
                else if (ctx.remaining.kcal >= 80) "Ti restano ${ctx.remaining.kcal.r0()} kcal · P ${ctx.remaining.protein.r0()} · C ${ctx.remaining.carbs.r0()} · G ${ctx.remaining.fat.r0()}. Ti propongo piatti su misura."
                else "Hai già raggiunto l'obiettivo di oggi: ti propongo qualcosa di leggero.", style = T.Muted.copy(color = Color(0xFFD7E0FF), fontSize = 13.sp))
            }
            GapW(12.dp)
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) { AIcon(Ic.Spark, Color.White, 24.dp, 2f) }
        }
    }

    // Categorie principali (pasto e calorie): subito in alto, come su Yazio
    if (catGroups == null) CatPlaceholder()
    else catGroups?.filter { it.id in TOP_CATS }?.forEach { CatGroupView(it) }

    // Ricetta del giorno
    val daily = remember(today, s.dietPrefs.exclude, disliked) { RecipeSearch.recipeOfTheDay(db, today, s.dietPrefs.exclude, disliked) }
    if (daily != null) {
        val di = RecipeMeta.info(db, daily)
        ASection("Ricetta del giorno", top = 22.dp)
        GapH(10.dp)
        val big = RoundedCornerShape(30.dp)
        Box(Modifier.fillMaxWidth().aspectRatio(1.75f).aShadow(big, 10.dp, Color(0x661D1B3A)).clip(big).clickable { openRecipe(s, daily) }) {
            RecipeImage(daily.id, daily.image, s.recipePhotos[daily.id], Modifier.fillMaxSize(), recEmoji(daily.meal), 0.dp)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color(0xDB18142F))))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(daily.name, style = T.H2.copy(color = Color.White, fontSize = 21.sp, lineHeight = 25.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${di.kcal.r0()} kcal · P ${di.protein.r0()} · ${daily.minutes} min", style = T.Small.copy(color = Color(0xFFE8E6F5)))
            }
        }
    }

    // Consigliate per te: vicine all'obiettivo del prossimo pasto
    val recommended = home?.recommended.orEmpty()
    if (recommended.isNotEmpty()) {
        ASection("Consigliate per te", top = 22.dp, action = "Vedi tutte") { Nav.go(Screen.WhatNow) }
        Text("Per il prossimo pasto (${RecipeMeta.label(RecipeMeta.MEALS, nextKey).lowercase()}), vicine ai ${ctx?.target?.kcal?.r0() ?: ""} kcal che ti restano.", style = T.Small, modifier = Modifier.padding(bottom = 10.dp))
        TileStrip(recommended) { (t, i), m -> RecipeTile(t, i, s, m) { openRecipe(s, t) } }
    }

    // Più aperte da te (dati reali: nessuna finta classifica di popolarità)
    val most = remember(s.recipeViews) { s.recipeViews.entries.sortedByDescending { it.value }.mapNotNull { e -> db.recipe(e.key)?.let { it to e.value } }.take(8) }
    if (most.size >= 2) {
        ASection("Le più aperte da te", top = 22.dp)
        GapH(10.dp)
        TileStrip(most) { (t, n), m -> RecipeTile(t, RecipeMeta.info(db, t), s, m, badge = "$n aperture") { openRecipe(s, t) } }
    }

    // Da provare oggi: scelta che ruota ogni giorno tra le ricette con foto
    val discover = home?.discover.orEmpty()
    if (discover.isNotEmpty()) {
        ASection("Da provare oggi", top = 22.dp)
        GapH(10.dp)
        TileStrip(discover) { t, m -> RecipeTile(t, RecipeMeta.info(db, t), s, m) { openRecipe(s, t) } }
    }

    // Recenti
    val recent = remember(s.recipeRecent) { s.recipeRecent.mapNotNull { db.recipe(it) }.take(10) }
    if (recent.isNotEmpty()) {
        ASection("Viste di recente", top = 22.dp)
        GapH(10.dp)
        TileStrip(recent) { t, m -> RecipeTile(t, RecipeMeta.info(db, t), s, m) { openRecipe(s, t) } }
    }

    // Preferite
    val favs = remember(s.recipeFavs) { s.recipeFavs.mapNotNull { db.recipe(it) } }
    ASection("Preferite", top = 22.dp, action = if (favs.isNotEmpty()) "Vedi tutte" else null) { Nav.go(Screen.RecipeList(RecipeFilter(onlyFavs = true), "Ricette preferite")) }
    if (favs.isEmpty()) {
        ACard(color = A.Tonal, radius = 24.dp, bordered = false, elevation = 0.dp) {
            Text("Tocca il cuore su una ricetta per ritrovarla qui.", style = T.Muted)
        }
    } else {
        GapH(10.dp)
        TileStrip(favs.take(12)) { t, m -> RecipeTile(t, RecipeMeta.info(db, t), s, m) { openRecipe(s, t) } }
    }

    // Le mie ricette
    ASection("Le mie ricette", top = 22.dp, action = if (s.recipes.isNotEmpty()) "Vedi tutte" else null) { Nav.go(Screen.RecipeList(RecipeFilter(), "Le mie ricette", mine = true)) }
    if (s.recipes.isEmpty()) {
        ACard(color = A.Tonal, radius = 24.dp, bordered = false, elevation = 0.dp, onClick = { Nav.go(Screen.RecipeEditor(null)) }) {
            Column {
                Text("Crea la tua prima ricetta", style = T.Title2)
                Text("Con foto, quantità esatte e valori calcolati da soli. Puoi anche importarla da un testo o farla generare all'AI.", style = T.Muted)
            }
        }
    } else {
        GapH(10.dp)
        TileStrip(s.recipes.take(12)) { r, m -> UserRecipeTile(r, m) { Nav.go(Screen.UserRecipeDetail(r)) } }
    }
    GapH(10.dp)
    APrimary("+ Nuova ricetta", Modifier.fillMaxWidth(), height = 54.dp) { Nav.go(Screen.RecipeEditor(null)) }

    // Altre categorie (obiettivo, dieta, metodo, cucina)
    catGroups?.let { gs ->
        ASection("Altre categorie", top = 26.dp, action = "Tutte") { Nav.go(Screen.RecipeList(RecipeFilter(), "Tutte le ricette")) }
        gs.filter { it.id !in TOP_CATS }.forEach { CatGroupView(it) }
    }
    GapH(8.dp)
}

private val TOP_CATS = setOf("pasto", "tempo", "kcal")

// ---------------------------------------------------------------- Elenco con filtri

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Text(title, style = T.Label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    content()
}

@Composable
private fun MultiPills(options: List<Pair<String, String>>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (k, l) -> Pill(l, k in selected) { onChange(if (k in selected) selected - k else selected + k) } }
    }
}

@Composable
private fun <T> SinglePills(options: List<Pair<T?, String>>, selected: T?, onSelect: (T?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, l) -> Pill(l, selected == v) { onSelect(if (selected == v) null else v) } }
    }
}

@Composable
private fun FilterPanel(f: RecipeFilter, onChange: (RecipeFilter) -> Unit) {
    ACard(radius = 28.dp, padding = 16.dp, elevation = 6.dp) {
        Column {
            Text("Filtri combinabili", style = T.H2.copy(fontSize = 19.sp))
            Text("Ogni filtro restringe i risultati: puoi combinarli (es. alto contenuto proteico + senza lattosio + sotto i 20 minuti).", style = T.Small)
            FilterGroup("Pasto") { MultiPills(RecipeMeta.MEALS, f.meals) { onChange(f.copy(meals = it)) } }
            FilterGroup("Calorie a porzione") {
                SinglePills(RecipeMeta.KCAL_BUCKETS.map { (k, r) ->
                    (r.first.toInt() to r.second.toInt().takeIf { it < 99999 }) to (if (k.startsWith("<")) "< ${k.drop(1)}" else if (k.startsWith(">")) "> ${k.drop(1)}" else k.replace("-", "–"))
                }, if (f.kcalMin != null || f.kcalMax != null) ((f.kcalMin ?: 0) to f.kcalMax) else null) { v ->
                    onChange(if (v == null) f.copy(kcalMin = null, kcalMax = null) else f.copy(kcalMin = v.first.takeIf { it > 0 }, kcalMax = v.second))
                }
            }
            FilterGroup("Obiettivo nutrizionale") { MultiPills(RecipeMeta.GOALS, f.goals) { onChange(f.copy(goals = it)) } }
            FilterGroup("Tipo di dieta") { MultiPills(RecipeMeta.DIETS, f.diets) { onChange(f.copy(diets = it)) } }
            FilterGroup("Metodo") { MultiPills(RecipeMeta.METHODS, f.methods) { onChange(f.copy(methods = it)) } }
            FilterGroup("Cucina") { MultiPills(RecipeMeta.CUISINES, f.cuisines) { onChange(f.copy(cuisines = it)) } }
            FilterGroup("Tempo massimo") { SinglePills(listOf(10, 15, 20, 30, 45, 60).map { it to "$it min" }, f.maxMinutes) { onChange(f.copy(maxMinutes = it)) } }
            FilterGroup("Difficoltà massima") { SinglePills(listOf(1 to "Facile", 2 to "Media", 3 to "Impegnativa"), f.maxDifficulty) { onChange(f.copy(maxDifficulty = it)) } }
            FilterGroup("Proteine minime a porzione") { SinglePills(listOf(15, 20, 30, 40).map { it to "≥ $it g" }, f.proteinMin) { onChange(f.copy(proteinMin = it)) } }
            FilterGroup("Carboidrati massimi") { SinglePills(listOf(20, 40, 60).map { it to "≤ $it g" }, f.carbsMax) { onChange(f.copy(carbsMax = it)) } }
            FilterGroup("Grassi massimi") { SinglePills(listOf(10, 20, 30).map { it to "≤ $it g" }, f.fatMax) { onChange(f.copy(fatMax = it)) } }
            FilterGroup("Escludi allergeni") { MultiPills(RecipeMeta.ALLERGENS, f.excludeAllergens) { onChange(f.copy(excludeAllergens = it)) } }
            FilterGroup("Ingredienti da usare") { JField(f.ingredients, { onChange(f.copy(ingredients = it)) }, "Es. pollo, riso, zucchine") }
            FilterGroup("Ordina per") { SinglePills(listOf("rilevanza" to "Rilevanza", "kcal" to "Calorie", "proteine" to "Proteine", "tempo" to "Tempo"), f.sort) { onChange(f.copy(sort = it ?: "rilevanza")) } }
            GapH(14.dp)
            AInk("Azzera tutti i filtri", Modifier.fillMaxWidth(), height = 46.dp, bg = Color(0xFFF6F1EA), fg = A.Ink) { onChange(RecipeFilter(query = f.query)) }
        }
    }
}

@Composable
fun RecipeListScreen(sc: Screen.RecipeList) {
    val s by Store.state.collectAsState()
    val db = Db.db
    var f by remember { mutableStateOf(sc.filter) }
    var panel by remember { mutableStateOf(false) }
    var respect by remember { mutableStateOf(true) }
    val pantry = remember(s.pantry) { pantryIds(s) }
    val excl = s.dietPrefs.exclude
    val hits = remember(f, respect, s.pantry, s.recipeFavs, s.dietPrefs) {
        if (sc.mine) emptyList() else RecipeSearch.search(db, f, pantry, s.recipeFavs.toSet(), s.dietPrefs.disliked.toSet())
            .filter { !respect || excl.isEmpty() || db.recipeTags(it.tpl).none { t -> t in excl } }
    }
    val mine = remember(f.query, s.recipes) { s.recipes.filter { f.query.isBlank() || it.name.contains(f.query.trim(), true) } }

    Column(Modifier.fillMaxSize()) {
        TopBar(sc.title, onBack = { Nav.back() }) {
            if (!sc.mine) TextButton(onClick = { panel = !panel }) {
                Text(if (f.activeCount > 0) "Filtri (${f.activeCount})" else "Filtri", color = C.AccentDeep, style = T.BtnS)
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { JField(f.query, { f = f.copy(query = it) }, if (sc.mine) "Cerca tra le mie ricette" else "Cerca per nome o ingrediente") }
            if (!sc.mine) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Con ciò che ho", f.fridge) { f = f.copy(fridge = !f.fridge) }
                        Pill("Preferite", f.onlyFavs) { f = f.copy(onlyFavs = !f.onlyFavs) }
                        if (excl.isNotEmpty()) Pill("Rispetta le mie esclusioni", respect) { respect = !respect }
                        RecipeMeta.MEALS.take(4).forEach { (k, l) -> Pill(l, k in f.meals) { f = f.copy(meals = if (k in f.meals) f.meals - k else f.meals + k) } }
                    }
                }
                if (panel) item { FilterPanel(f) { f = it } }
                item {
                    Text(if (hits.size == 1) "1 ricetta" else "${hits.size} ricette", style = T.Muted)
                    if (f.fridge && s.pantry.isEmpty()) Text("La dispensa è vuota: fotografa il frigo e Jarvis riconosce i prodotti.", style = T.Small.copy(color = A.TonalInk))
                }
                if (hits.isEmpty()) item { EmptyState("🔎", "Nessuna ricetta con questi filtri", "Prova a toglierne qualcuno: ogni filtro restringe i risultati.") }
                items(hits, key = { it.tpl.id }) { h -> RecipeRow(h, s) { openRecipe(s, h.tpl) } }
            } else {
                item { APrimary("+ Nuova ricetta", Modifier.fillMaxWidth(), height = 52.dp) { Nav.go(Screen.RecipeEditor(null)) } }
                if (mine.isEmpty()) item { EmptyState("🧑‍🍳", "Nessuna ricetta tua", "Crea una ricetta: i valori si calcolano dagli ingredienti.") }
                items(mine, key = { it.id }) { r ->
                    val per = Nutrition.recipeTotals(r).scaled(1.0 / r.servings.coerceAtLeast(1))
                    Row(Modifier.fillMaxWidth().aShadow(RoundedCornerShape(26.dp), 6.dp, Color(0x661D1B3A)).clip(RoundedCornerShape(26.dp)).background(Color.White)
                        .clickable { Nav.go(Screen.UserRecipeDetail(r)) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RecipeImage(r.id, null, r.photo, Modifier.size(88.dp), r.emoji, 18.dp)
                        GapW(12.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${r.servings} porzioni · ${r.minutes} min", style = T.Small)
                            Text(r.name, style = T.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${per.kcal.r0()} kcal a porzione · P ${per.protein.r0()} · C ${per.carbs.r0()} · G ${per.fat.r0()}", style = T.Small.copy(color = A.Ink))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Cosa mangio adesso?

@Composable
fun WhatNowScreen() {
    val s by Store.state.collectAsState()
    val ctxA = LocalContext.current
    val autoKey = remember { WhatNow.keyForDiaryMeal(mealForHour(LocalTime.now().hour)) }
    var keyOv by remember { mutableStateOf<String?>(null) }
    var maxMin by remember { mutableStateOf(999) }
    var fridge by remember { mutableStateOf(false) }
    var usePlan by remember { mutableStateOf(true) }
    val key = keyOv ?: autoKey
    val nc = remember(s.diary, s.plan, s.dietPrefs, s.energy, s.goals, s.profile, key, usePlan) { nowContext(s, key, usePlan) }
    val pantry = remember(s.pantry) { pantryIds(s) }
    val sugg by produceState<List<NowSuggestion>?>(null, nc.target, key, maxMin, fridge, s.dietPrefs, pantry) {
        value = null
        value = withContext(Dispatchers.Default) { WhatNow.suggest(Db.db, key, nc.target, maxMin, s.dietPrefs, pantry, fridge) }
    }
    val diaryMeal = WhatNow.diaryMealForKey(key)

    Column(Modifier.fillMaxSize()) {
        TopBar("Cosa mangio adesso?", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ACard(brush = Brush.linearGradient(listOf(Color(0xFF13225F), Color(0xFF1B3382))), bordered = false, glow = Color(0x8013225F), radius = 28.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Ti restano oggi", style = T.Small.copy(color = Color(0xFFB9C6F5)))
                    Text("${nc.remaining.kcal.r0()} kcal", style = T.H1.copy(color = Color.White, fontSize = 30.sp))
                    Text("P ${nc.remaining.protein.r0()} g · C ${nc.remaining.carbs.r0()} g · G ${nc.remaining.fat.r0()} g", style = T.Muted.copy(color = Color(0xFFD7E0FF)))
                    GapH(10.dp)
                    Text("Obiettivo per ${RecipeMeta.label(RecipeMeta.MEALS, key).lowercase()}: ${nc.target.kcal.r0()} kcal · P ${nc.target.protein.r0()} · C ${nc.target.carbs.r0()} · G ${nc.target.fat.r0()}" +
                        if (nc.plannedUsed) " (tolti i ${nc.plannedOthers} pasti già pianificati)" else "", style = T.Small.copy(color = Color(0xFFD7E0FF)))
                    if (nc.remaining.kcal < 80) Text("Hai già raggiunto l'obiettivo di oggi: propongo porzioni leggere.", style = T.Small.copy(color = Color(0xFFFFD9A0)), modifier = Modifier.padding(top = 6.dp))
                }
            }
            GapH(14.dp)
            Text("Pasto", style = T.Label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Ora (auto)", keyOv == null) { keyOv = null }
                listOf("colazione" to "Colazione", "pranzo" to "Pranzo", "cena" to "Cena", "spuntino" to "Spuntino").forEach { (k, l) -> Pill(l, keyOv == k) { keyOv = k } }
            }
            Text("Tempo a disposizione", style = T.Label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(999 to "Qualsiasi", 15 to "≤ 15 min", 30 to "≤ 30 min", 45 to "≤ 45 min").forEach { (m, l) -> Pill(l, maxMin == m) { maxMin = m } }
            }
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Solo con ciò che ho", fridge) { fridge = !fridge }
                Pill("Considera i pasti pianificati", usePlan) { usePlan = !usePlan }
            }
            if (s.dietPrefs.exclude.isNotEmpty() || s.dietPrefs.disliked.isNotEmpty())
                Text("Escluse le tue intolleranze e le ricette che hai scartato.", style = T.Small, modifier = Modifier.padding(top = 8.dp))

            GapH(16.dp)
            val list = sugg
            if (list == null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = C.Accent, strokeWidth = 2.dp); GapW(12.dp); Text("Calcolo le quantità giuste per te…", style = T.Muted)
                }
            } else if (list.isEmpty()) {
                EmptyState("🤔", "Nessuna ricetta adatta", if (fridge) "Con gli ingredienti in dispensa non trovo piatti adatti: togli il filtro dispensa o aggiungi prodotti." else "Allenta il tempo a disposizione o cambia pasto.")
            } else {
                list.forEachIndexed { idx, sg ->
                    SuggestionCard(sg, s, idx == 0) {
                        Store.eatSolved(LocalDate.now(), diaryMeal, sg.solved); Store.consumeFromPantry(sg.solved)
                        Toast.makeText(ctxA, "Aggiunto al diario", Toast.LENGTH_SHORT).show()
                    }
                    GapH(12.dp)
                }
                Text("Le quantità sono ricalcolate sui tuoi macro rimasti (calorie, proteine, carboidrati e grassi calcolati da USDA FoodData Central). La compatibilità misura quanto il piatto adattato si avvicina all'obiettivo senza stravolgere la ricetta.", style = T.Small)
            }
            GapH(30.dp)
        }
    }
}

@Composable
private fun SuggestionCard(sg: NowSuggestion, s: AppState, best: Boolean, onAdd: () -> Unit) {
    val t = sg.solved.template
    val a = sg.solved.total
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().aShadow(shape, 10.dp, Color(0x661D1B3A)).clip(shape).background(Color.White).border(1.dp, A.Hair, shape)) {
        Box(Modifier.clickable { Nav.go(Screen.RecipeView(t.id, sg.solved.target, LocalDate.now(), null, WhatNow.diaryMealForKey(t.meal.let { if (it == "dessert") "spuntino" else it }))) }) {
            RecipeImage(t.id, t.image, s.recipePhotos[t.id], Modifier.fillMaxWidth().height(150.dp), recEmoji(t.meal), 0.dp)
            Box(Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(16.dp)).background(if (sg.compat >= 85) C.Green else if (sg.compat >= 70) C.Gold else Color(0xFF8C8AA6)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("${sg.compat}% compatibile" + if (best) " · migliore" else "", style = T.Chip.copy(color = Color.White), maxLines = 1)
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(t.name, style = T.H2.copy(fontSize = 19.sp, lineHeight = 23.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${t.minutes} min · ${RecipeMeta.DIFFICULTY.getOrElse(t.difficulty) { "" }}" + if (s.pantry.isNotEmpty()) " · in dispensa ${(sg.fridge * 100).toInt()}%" else "", style = T.Small)
            GapH(10.dp)
            Text("${a.kcal.r0()} kcal", style = T.Num.copy(color = C.AccentDeep, fontSize = 22.sp))
            GapH(6.dp)
            MacroChips(a.protein, a.carbs, a.fat)
            GapH(10.dp)
            Text(sg.solved.items.filter { it.ing.role != "x" }.take(4).joinToString(" · ") { "${it.displayName()} ${it.amountLabel()}" }, style = T.Small.copy(color = A.Ink), maxLines = 3)
            GapH(8.dp)
            Text("Scarto dall'obiettivo: ${diffText(sg.dKcal, " kcal")} · P ${diffText(sg.dProtein)} · C ${diffText(sg.dCarbs)} · G ${diffText(sg.dFat)}" +
                if (abs(sg.scale - 1.0) >= 0.15) " · quantità ${if (sg.scale > 1) "aumentate" else "ridotte"} del ${(abs(sg.scale - 1) * 100).toInt()}% rispetto alla ricetta base" else "", style = T.Small)
            GapH(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AInk("Ricetta", Modifier.weight(1f), height = 50.dp, bg = Color(0xFFF6F1EA), fg = A.Ink) {
                    Nav.go(Screen.RecipeView(t.id, sg.solved.target, LocalDate.now(), null, WhatNow.diaryMealForKey(t.meal.let { if (it == "dessert") "spuntino" else it })))
                }
                APrimary("Aggiungi al diario", Modifier.weight(1.4f), height = 50.dp, onClick = onAdd)
            }
        }
    }
}
