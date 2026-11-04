package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun FoodGuidanceCard(food: Food, grams: Double, s: AppState, date: LocalDate, entry: DiaryEntry?, meal: Int) {
    val advice = remember(food, grams, s.diary, date) { FoodGuidance.advice(food, grams, s.diary, date) }
    var details by remember { mutableStateOf(false) }
    var alternativesOpen by remember { mutableStateOf(false) }
    var criteria by remember { mutableStateOf(false) }
    val facts = food.productFacts
    val review = remember(food) { FoodReview.assess(food) }
    var compare by remember { mutableStateOf(false) }
    var query by remember(food.id) { mutableStateOf(advice.alternativeQuery) }
    var products by remember(food.id) { mutableStateOf<List<Food>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val alternatives = remember(advice.alternativeQuery, s.dietPrefs.exclude, food.id) {
        Db.db.search(advice.alternativeQuery).map { it.toFood() }.filter { it.id != food.id && it.tags.none { t -> t in s.dietPrefs.exclude } }.take(2)
    }
    GapH(12.dp)
    JCard(Modifier.testTag("food-guidance")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("●", style = T.Title2.copy(color=reviewColor(review.tone)))
            GapW(8.dp)
            Column(Modifier.weight(1f)) {
                Text(review.title,Modifier.testTag("food-review-rating"),style=T.Label.copy(color=reviewColor(review.tone)))
                Text(if(review.grade!=null) "Profilo nutrizionale · Open Food Facts" else "Sintesi dei dati noti dell’etichetta",style=T.Small)
            }
        }
        GapH(8.dp)
        if(review.positive.isNotEmpty()) {
            Text("Punti positivi",style=T.Label.copy(color=A.GreenInk))
            review.positive.take(3).forEach { ReviewPoint(it) }
        }
        val considerations=review.attention + review.facts.filter { it.tone==FoodReview.Tone.CAUTION }
        if(considerations.isNotEmpty()) {
            GapH(6.dp); Text("Da considerare",style=T.Label.copy(color=C.AccentDeep))
            considerations.take(3).forEach { ReviewPoint(it) }
        }
        review.facts.filter { it.title in setOf("Additivi","Biologico") }.forEach { ReviewPoint(it.copy(value=it.value.substringBefore(" · "))) }
        if(review.missing.isNotEmpty()) Text("Dati parziali · ${review.missing.joinToString()}",style=T.Small.copy(color=C.Muted))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={ alternativesOpen=!alternativesOpen },modifier=Modifier.testTag("food-guidance-alternatives")) { Text(if(alternativesOpen) "Chiudi alternative" else "Alternative",color=C.Navy) }
            TextButton(onClick={ criteria=true },modifier=Modifier.testTag("food-review-criteria")) { Text("Criteri e ingredienti",color=C.Navy) }
        }
        TextButton(onClick = { details = !details }, modifier = Modifier.testTag("food-guidance-details")) { Text(if (details) "Chiudi consigli" else "Quantità e frequenza", color = C.Navy) }
        if (details) {
            Text(advice.title,style=T.Label)
            Text(advice.explanation, style = T.Small)
            GapH(6.dp)
            Text(advice.portion, style = T.Small)
            GapH(6.dp); Text(advice.frequency, style = T.Small)
        }
        if(alternativesOpen || details) {
            GapH(8.dp); Text("Alternative da valutare", style = T.Label)
            alternatives.forEach { candidate ->
                val portion = NutritionScience.standardPortion(candidate) ?: 100.0
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    FoodIllustration(candidate.name, candidate.group, 40.dp); GapW(8.dp)
                    Column(Modifier.weight(1f)) {
                        Text(candidate.name, style = T.Label)
                        Text("Riferimento ${portion.r0()} ${if (candidate.liquid) "ml" else "g"} · non è una sostituzione isocalorica", style = T.Small)
                        TextButton(onClick = {
                            if (entry != null) Nav.go(Screen.FoodDetail(candidate, date, entry.meal, portion, entry, replacing = true))
                            else Nav.go(Screen.FoodDetail(candidate, date, meal, portion))
                        }) { Text("Valuta alternativa", color = C.Navy) }
                    }
                }
            }
            Text("Confronta ingredienti, quantità e allergeni prima di sostituire.", style = T.Small)
            TextButton(onClick = { compare = true }) { Text("Confronta prodotti e marche", color = C.Navy) }
        }
    }
    if(criteria) AlertDialog(onDismissRequest={ criteria=false },title={ Text("Come leggere il profilo",style=T.Title2) },text={
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Il Nutri-Score A–E, quando presente, proviene da Open Food Facts${facts?.nutriScoreVersion?.let { " (versione $it)" } ?: ""}. In sua assenza il colore riassume solo zuccheri, saturi e sale noti; non è un voto complessivo alla salute dell’alimento.",style=T.Small)
            GapH(8.dp)
            Text("Senza Nutri-Score: due valori elevati indicano rosso; uno indica arancione. Se non emergono valori elevati ma manca uno dei tre dati, il colore è grigio. Le carni lavorate ricevono un invito a limitarle. Una crema 100% frutta a guscio richiede attenzione alla porzione, senza diventare automaticamente cibo spazzatura.",style=T.Small)
            Text("Proteine e fibre seguono le soglie UE. Biologico e numero degli additivi sono informazioni distinte, senza un premio o una penalità inventati. Indice glicemico: non disponibile; non si ricava dai soli zuccheri.",style=T.Small)
            GapH(8.dp)
            (review.positive+review.attention+review.facts).distinct().forEach { ReviewPoint(it) }
            GapH(8.dp);Text("Ingredienti",style=T.Label)
            Text(facts?.ingredients ?: "Non disponibili: verifica la confezione.",style=T.Small)
            facts?.additives?.takeIf { it.isNotEmpty() }?.let { Text("Additivi dichiarati: ${it.joinToString { code -> code.substringAfter(':').uppercase() }}",style=T.Small) }
            facts?.allergens?.takeIf { it.isNotEmpty() }?.let { Text("Allergeni dichiarati: ${it.joinToString()}",style=T.Small) }
            facts?.traces?.takeIf { it.isNotEmpty() }?.let { Text("Possibili tracce: ${it.joinToString()}",style=T.Small) }
            if(review.missing.isNotEmpty()) Text("Dati mancanti: ${review.missing.joinToString()}",style=T.Small)
            Text("I dati online possono essere incompleti o superati: controlla l’etichetta del prodotto.",style=T.Small)
            TextButton(onClick={ runCatching { uri.openUri(FoodGuidance.LABEL_SOURCE) } }) { Text("Colori dei nutrienti · NHS") }
            TextButton(onClick={ runCatching { uri.openUri(FoodReview.CLAIMS_SOURCE) } }) { Text("Proteine e fibre · UE") }
            TextButton(onClick={ runCatching { uri.openUri(FoodReview.NUTRISCORE_SOURCE) } }) { Text("Nutri-Score · metodo") }
            TextButton(onClick={ runCatching { uri.openUri(FoodGuidance.VARIETY_SOURCE) } }) { Text("Porzioni e varietà · CREA") }
        }
    },confirmButton={ TextButton(onClick={ criteria=false }) { Text("Chiudi") } },containerColor=C.Surface)
    if (compare) AlertDialog(onDismissRequest = { compare = false }, title = { Text("Confronta le etichette", style = T.Title2) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Stessa unità: per 100 ${if (food.liquid) "ml" else "g"}. Un valore più basso non prova che il prodotto sia migliore in assoluto. Controlla anche ingredienti e allergeni sulla confezione.", style = T.Small)
            JField(query, { query = it }, "Alimento o marca")
            TextButton(enabled = !busy && query.trim().length >= 3, onClick = {
                busy = true; error = null; products = emptyList()
                scope.launch {
                    try { products = OpenFoodFacts.search(query).filter { it.liquid == food.liquid && it.id != food.id }.take(8); if (products.isEmpty()) error = "Nessun prodotto confrontabile trovato." }
                    catch (_: Exception) { error = "Ricerca non disponibile. Puoi controllare l'etichetta del prodotto." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Cerco…" else "Cerca e confronta", color = C.Navy) }
            Text("Attuale: ${food.name}", style = T.Label)
            Text(FoodGuidance.signals(food).joinToString(" · ") { "${it.label} ${it.per100?.r1() ?: "?"} g" }, style = T.Small)
            error?.let { Text(it, style = T.Small) }
            products.forEach { product ->
                GapH(10.dp)
                Text("${product.brand} · ${product.name}", style = T.Label)
                Text(FoodGuidance.signals(product).joinToString(" · ") { "${it.label} ${it.per100?.r1() ?: "?"} g" }, style = T.Small)
                Text("Open Food Facts · dati da verificare sulla confezione", style = T.Small)
                // Open the normal editor; selecting a comparison never writes to diary.
                TextButton(onClick = {
                    compare = false
                    if (entry != null) Nav.go(Screen.FoodDetail(product, date, entry.meal, grams, entry, replacing = true))
                    else Nav.go(Screen.FoodDetail(product, date, meal, grams))
                }) { Text("Apri e controlla quantità", color = C.Navy) }
            }
        }
    }, confirmButton = { TextButton(onClick = { compare = false }) { Text("Chiudi") } }, containerColor = C.Surface)
}

private fun reviewColor(tone: FoodReview.Tone): Color = when(tone) {
    FoodReview.Tone.GOOD -> A.GreenInk
    FoodReview.Tone.CAUTION -> C.AccentDeep
    FoodReview.Tone.LIMIT -> C.Danger
    FoodReview.Tone.UNKNOWN -> C.Muted
}

@Composable
private fun ReviewPoint(point: FoodReview.Point) {
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.Top) {
        Text("●",style=T.Small.copy(color=reviewColor(point.tone)))
        GapW(7.dp)
        Column(Modifier.weight(1f)) {
            Text(point.title,style=T.Label)
            Text(point.value,style=T.Small.copy(color=C.Muted))
        }
    }
}

private fun tipIcon(id: String): Pair<String, String> = when {
    "protein" in id -> RIc.Dumbbell to RIc.DumbbellBar
    "sugar" in id || "sodium" in id || "salumi" in id || "label" in id || "saturated" in id -> (Ic.circ(10.0, 10.0, 6.0)) to "M14.5 14.5l5.5 5.5"
    "portion" in id || "quantity" in id || "over" in id || "under" in id -> RIc.Bowl to RIc.Steam
    "veg" in id || "legume" in id || "fresh" in id || "fiber" in id -> RIc.Leaf to ""
    "fruit" in id -> RIc.Apple to RIc.AppleLeaf
    "water" in id -> Ic.Drop to ""
    "repeat" in id || "variety" in id -> RIc.Swap to ""
    "breakfast" in id -> RIc.Cup to RIc.CupSteam
    "sweets" in id -> RIc.Muffin to RIc.MuffinTop
    else -> Ic.Spark to ""
}

@Composable
private fun TipIcon(id: String, size: androidx.compose.ui.unit.Dp = 30.dp) {
    val (navy, accent) = tipIcon(id)
    DuoIcon(navy, accent, size, accentFill = accent == RIc.AppleLeaf || accent == RIc.MuffinTop, stroke = 2.1f)
}

/** Consigli per oggi: analisi del diario (giorno, 7 e 30 giorni) più idee generali a rotazione. */
@Composable
fun EverydayFoodGuide(s: AppState, review: NutritionReview? = null) {
    val date=Nav.today
    val ctx=remember(s.profile,s.goals,s.energy,s.trainingNutrition,s.weights) {
        DietCoach.Context({ Store.energyFor(s,it).targetKcal }, { Store.energyFor(s,it).protein }, s.goals.waterMl)
    }
    val general=remember(s.diary,s.dietPrefs,s.plan,date) { DailyFoodTips.build(s,date,Db.db,null) }
    val personal=remember(s.diary,s.water,s.dietPrefs,review,date,ctx) {
        (DietCoach.analyze(s,date,1,ctx) + DietCoach.analyze(s,date,7,ctx,review) + DietCoach.analyze(s,date,30,ctx)).distinctBy { it.id }
    }
    val all=(personal+general).distinctBy { it.id }
    val shown=all.take(3)
    var selected by remember { mutableStateOf<DailyFoodTips.Tip?>(null) }
    var full by remember { mutableStateOf(false) }
    val uri=LocalUriHandler.current
    ACard(Modifier.fillMaxWidth().testTag("everyday-food-guide"), radius = 24.dp, elevation = 4.dp, glow = Color(0x2009194D)) {
        ReferenceInsightBody(TodayArt.Calendar) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Per te oggi", style = T.Title.copy(fontSize = 19.sp, lineHeight = 24.sp, color = RD.Ink), modifier = Modifier.weight(1f))
                Row(Modifier.heightIn(min = 40.dp).testTag("daily-tips-all").clip(RoundedCornerShape(10.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { full = true }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${shown.size}/${all.size}", style = T.Title2.copy(fontSize = 15.sp, color = RD.Grey)); GapW(4.dp)
                    AIcon(RIc.Right, RD.Ink, 16.dp, 2.4f)
                }
            }
            Text(if (personal.isNotEmpty()) "${shown.size} idee dalla tua dieta" else "${shown.size} idee del giorno", style = T.Body.copy(fontSize = 15.sp, lineHeight = 20.sp, color = RD.Grey))
            shown.forEach { tip ->
                GapH(8.dp)
                TipRow(tip) { selected = tip }
            }
        }
    }
    if (full) DietCoachDialog(s, date, ctx, review, onOpen = { full = false; selected = it }) { full = false }
    selected?.let { tip ->
        val source=NutritionEvidence.source(tip.source)
        AlertDialog(onDismissRequest={ selected=null },title={ Text(tip.title,style=T.Title2) },text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(tip.text,style=T.Body)
                GapH(8.dp)
                Text(if(tip.personal) "Suggerimento calcolato dal tuo diario; non è una diagnosi."
                    else "Idea generale per adulti: adatta quantità, preferenze e condizioni personali. I consigli ruotano ogni giorno.",style=T.Small)
                TextButton(onClick={ runCatching { uri.openUri(source.url) } },modifier=Modifier.testTag("daily-tip-source")) { Text("${source.title} ↗",style=T.Small) }
                if(tip.personal) TextButton(onClick={ selected=null;Nav.go(Screen.Micronutrients) }) { Text("Dati del diario e alternative") }
            }
        },confirmButton={ TextButton(onClick={ selected=null }) { Text("Chiudi") } },containerColor=C.Surface)
    }
}

@Composable
private fun TipRow(tip: DailyFoodTips.Tip, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("daily-food-tip-${tip.id}").clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFEEF2F8))
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TipIcon(tip.id)
        GapW(12.dp)
        Text(tip.title, style = T.Title2.copy(color = RD.Ink, fontSize = 15.sp, lineHeight = 20.sp), modifier = Modifier.weight(1f))
        GapW(4.dp)
        AIcon(RIc.Right, RD.Ink, 16.dp, 2.4f)
    }
}

/** Tutti i consigli con periodo selezionabile e, se configurata, l'analisi scritta dall'AI di Jarvis. */
@Composable
private fun DietCoachDialog(s: AppState, date: LocalDate, ctx: DietCoach.Context, review: NutritionReview?, onOpen: (DailyFoodTips.Tip) -> Unit, onClose: () -> Unit) {
    var days by remember { mutableIntStateOf(7) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tips = remember(days, s.diary, s.water, review) { DietCoach.analyze(s, date, days, ctx, if (days == 7) review else null) }
    val scope = rememberCoroutineScope()
    var ai by remember(days) { mutableStateOf(DietCoachAi.cached(date, days)) }
    var busy by remember(days) { mutableStateOf(false) }
    val ready = remember(s.ai) { DietCoachAi.ready(context, s) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(30.dp), color = C.Surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * .85f).dp)
                .verticalScroll(rememberScrollState()).padding(20.dp).testTag("diet-coach-dialog")) {
                Text("La tua dieta", style = T.Title.copy(fontSize = 23.sp, color = RD.Ink))
                Text("Analisi del diario ${DietCoach.windowLabel(days)}", style = T.Small)
                GapH(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Oggi", 7 to "7 giorni", 30 to "30 giorni").forEach { (n, label) ->
                        RefOption(label, Modifier.weight(1f), isSelected = days == n, selectedStyle = OptionStyle.Navy, tag = "diet-coach-$n", minHeight = 48.dp, centered = true, labelSize = 14.sp,
                            trailing = {}) { days = n }
                    }
                }
                GapH(12.dp)
                if (tips.isEmpty()) Text(if (s.diary.isEmpty()) "Registra i pasti per ricevere consigli personali." else "Nessuna abitudine da correggere ${DietCoach.windowLabel(days)}: continua così.", style = T.Body)
                tips.forEach { tip -> TipRow(tip) { onOpen(tip) }; GapH(8.dp) }
                GapH(8.dp)
                if (ready) {
                    RefPrimaryButton(if (busy) "Jarvis sta analizzando…" else "Analisi di Jarvis AI", Modifier.fillMaxWidth(), icon = Ic.Spark, tag = "diet-coach-ai", enabled = !busy && s.diary.isNotEmpty()) {
                        busy = true
                        scope.launch {
                            ai = DietCoachAi.ask(context, s, date, days, DietCoach.summary(s, date, days, ctx, tips))
                            busy = false
                        }
                    }
                } else Text("Per un commento scritto dall’AI, attiva il modello di Jarvis in Profilo → Intelligenza artificiale.", style = T.Small)
                ai?.let { GapH(10.dp); Text(it, Modifier.testTag("diet-coach-ai-text"), style = T.Body) }
                GapH(14.dp)
                RefOutlineButton("Chiudi", Modifier.fillMaxWidth(), icon = RIc.Close, onClick = onClose)
            }
        }
    }
}
