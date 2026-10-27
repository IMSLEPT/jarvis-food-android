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
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.*
import com.jarvis.food.engine.toFood
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun ReplaceFoodScreen(entryId: String) {
    val s by Store.state.collectAsState()
    val entry = s.diary.firstOrNull { it.id == entryId }
    var query by remember { mutableStateOf("") }
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
    LaunchedEffect(query) {
        online = emptyList(); error = null; loading = false
        if (query.trim().length < 3) return@LaunchedEffect
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
        Text("Al posto di ${entry.food.name}", style = T.Small, modifier = Modifier.padding(horizontal = 20.dp))
        Text("${MEAL_NAMES[entry.meal]} · scegli il nuovo alimento, poi controlla la quantità.", style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        JField(query, { query = it }, "Cerca un alimento", Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("replace-food-search"))
        Spacer(Modifier.height(12.dp))
        val foods = (local + online).distinctBy { it.id }
        LazyColumn(Modifier.weight(1f).testTag("replacement-results"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(foods, key = { it.id }) { food ->
                val shape = RoundedCornerShape(20.dp)
                Row(Modifier.fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Line, shape).testTag("replacement-${food.id}").clickable {
                    Nav.go(Screen.FoodDetail(food, LocalDate.parse(entry.date), entry.meal, entry.grams, entry, replacing = true))
                }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(food.name, style = T.Label)
                        Text("${food.kcal.r0()} kcal / 100 ${if (food.liquid) "ml" else "g"}" + if (food.brand.isNotBlank()) " · ${food.brand}" else "", style = T.Small)
                    }
                    Spacer(Modifier.width(12.dp)); AIcon(Ic.Swap, C.Navy, 24.dp)
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.Accent) }
            if (error != null) item { Text(error!!, style = T.Small) }
            if (foods.isEmpty() && !loading) item { Text(if (query.isBlank()) "Cerca il nome dell'alimento da usare." else "Nessun alimento trovato. Prova un altro nome.", style = T.Small) }
        }
    }
}
