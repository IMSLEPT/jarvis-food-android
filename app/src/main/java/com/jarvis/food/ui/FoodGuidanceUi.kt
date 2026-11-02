package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun FoodGuidanceCard(food: Food, grams: Double, s: AppState, date: LocalDate, entry: DiaryEntry?, meal: Int) {
    val advice = remember(food, grams, s.diary, date) { FoodGuidance.advice(food, grams, s.diary, date) }
    var details by remember { mutableStateOf(false) }
    var compare by remember { mutableStateOf(false) }
    var query by remember(food.id) { mutableStateOf(food.name) }
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
        Text(advice.title, style = T.Label.copy(color = if (advice.caution) C.AccentDeep else C.Navy))
        GapH(8.dp)
        advice.signals.forEach { signal ->
            val color = when (signal.level) { FoodGuidance.Level.HIGH -> C.Danger; FoodGuidance.Level.MEDIUM -> C.AccentDeep; FoodGuidance.Level.LOW -> A.GreenInk; else -> C.Muted }
            val label = when (signal.level) { FoodGuidance.Level.HIGH -> "alto"; FoodGuidance.Level.MEDIUM -> "medio"; FoodGuidance.Level.LOW -> "basso"; else -> "non noto" }
            Text("● ${signal.label}: ${signal.per100?.let { "${it.r1()} g/100 ${if (food.liquid) "ml" else "g"}" } ?: "dato mancante"} · $label", style = T.Small.copy(color = color))
        }
        GapH(8.dp)
        if(food.liquid && advice.signals.any { it.per100 != null && it.level == FoodGuidance.Level.UNKNOWN }) Text("I liquidi che non sono bevande (es. olio e salse) non ricevono il colore delle bevande: servirebbe una base di peso comparabile.",style=T.Small)
        TextButton(onClick = { details = !details }, modifier = Modifier.testTag("food-guidance-details")) { Text(if (details) "Chiudi consigli" else "Quantità, frequenza e alternative", color = C.Navy) }
        if (details) {
            Text(advice.explanation, style = T.Small)
            GapH(6.dp)
            Text(advice.portion, style = T.Small)
            GapH(6.dp); Text(advice.frequency, style = T.Small)
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
            Text("Confronta ingredienti ed etichette prima di preferire una marca. Non deduco NOVA o qualità dai nomi commerciali.", style = T.Small)
            TextButton(onClick = { compare = true }) { Text("Confronta prodotti e marche", color = C.Navy) }
            TextButton(onClick = { runCatching { uri.openUri(FoodGuidance.LABEL_SOURCE) } }) { Text("Criteri dei colori · NHS", color = C.Navy) }
            TextButton(onClick = { runCatching { uri.openUri(FoodGuidance.FREQUENCY_SOURCE) } }) { Text("Frequenze alimentari · CREA", color = C.Navy) }
            if (food.liquid) TextButton(onClick = { runCatching { uri.openUri(FoodGuidance.DRINK_SOURCE) } }) { Text("Soglie per le bevande · Governo scozzese", color = C.Navy) }
            TextButton(onClick = { runCatching { uri.openUri(FoodGuidance.VARIETY_SOURCE) } }) { Text("Porzioni e varietà · CREA", color = C.Navy) }
        }
    }
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

@Composable
fun EverydayFoodGuide() {
    var expanded by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    JCard(Modifier.testTag("everyday-food-guide")) {
        Text("Idee pratiche per mangiare vario", style = T.Label)
        Text("Puoi iniziare subito, anche con poco storico. Sono esempi per adulti sani: adatta porzioni, allergie e preferenze.", style = T.Small)
        GapH(8.dp)
        Text("Esempio onnivoro: alterna legumi, pesce, uova e carni fresche. Per il pesce fresco: 2–4 porzioni da 150 g a settimana; per la carne rossa: 1 da 100 g (CREA).", style = T.Small)
        Text("Adatta questi riferimenti alla tua alimentazione e al fabbisogno: non sono una garanzia di copertura delle vitamine.", style = T.Small)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Riduci" else "Mostra la guida", color = C.Navy) }
        if (expanded) {
            Text("• Alterna le fonti proteiche: legumi, pesce, uova e carni fresche; limita le carni lavorate. Esempi di porzione: legumi cotti 150 g, pesce fresco 150 g.", style = T.Small)
            GapH(8.dp)
            Text("• Pesce, uova e latticini apportano B12. Il pesce grasso può apportare vitamina D: mangiare pesce non garantisce di coprire da solo il fabbisogno. In una dieta vegana la B12 richiede una fonte affidabile concordata con un professionista.", style = T.Small)
            GapH(8.dp)
            Text("• Inserisci frutta e verdura variate ogni giorno e alterna cereali integrali. Frutta a guscio al naturale: riferimento 30 g, contando l'energia nel piano.", style = T.Small)
            GapH(8.dp)
            Text("• Per colazione alterna, per esempio, pane e ricotta, yogurt e avena, oppure pane e crema 100% frutta a guscio. Scegli quantità coerenti con i tuoi obiettivi.", style = T.Small)
            TextButton(onClick = { runCatching { uri.openUri("https://sapermangiare.an.crea.gov.it/507/pesce.html") } }) { Text("Frequenze del pesce · CREA", color = C.Navy) }
            TextButton(onClick = { runCatching { uri.openUri("https://sapermangiare.an.crea.gov.it/506/carne.html") } }) { Text("Frequenze della carne · CREA", color = C.Navy) }
            TextButton(onClick = { runCatching { uri.openUri(FoodGuidance.VARIETY_SOURCE) } }) { Text("Leggi le linee guida CREA", color = C.Navy) }
        }
    }
}
