package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Food
import com.jarvis.food.data.MEAL_NAMES
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import java.time.LocalDate

@Composable
fun FoodDetailScreen(food: Food, date: LocalDate, meal: Int, initialGrams: Double?, entry: DiaryEntry?) {
    val s by Store.state.collectAsState()
    val hasServing = food.servingGrams != null && food.servingName != null
    val unit = if (food.liquid) "ml" else "g"
    // Modalità quantità: 0 = grammi, 1 = porzioni
    var mode by remember { mutableIntStateOf(if (hasServing && initialGrams == null) 1 else 0) }
    var amount by remember {
        mutableStateOf(
            when {
                initialGrams != null -> initialGrams.r0()
                hasServing -> "1"
                else -> "100"
            }
        )
    }
    var mealSel by remember { mutableIntStateOf(entry?.meal ?: meal) }
    val grams = (parseNum(amount) ?: 0.0) * if (mode == 1) (food.servingGrams ?: 100.0) else 1.0
    val t = Nutrition.of(food, grams)
    val isFav = s.favorites.any { it.id == food.id }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (entry != null) "Modifica" else "Dettaglio", onBack = { Nav.back() }) {
            if (food.source == "custom") IconButton(onClick = { Nav.go(Screen.CustomFood(null, meal, edit = food)) }) {
                Icon(Icons.Filled.Edit, "Modifica alimento", tint = C.Muted)
            }
            IconButton(onClick = { Store.toggleFavorite(food) }) {
                Icon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Preferito", tint = if (isFav) C.Protein else C.Muted)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(food.name, style = T.Display)
            val sub = listOfNotNull(
                food.brand.takeIf { it.isNotBlank() },
                when (food.source) {
                    "off" -> "Open Food Facts"; "ai" -> "Stima AI"; "custom" -> "Creato da te"
                    "recipe" -> "Ricetta calcolata"; "quick" -> "Aggiunta rapida"
                    "db" -> "USDA FoodData Central" + (food.fdc?.let { " · FDC $it" } ?: ""); else -> "Valori medi"
                },
                food.barcode?.let { "EAN $it" }
            ).joinToString(" · ")
            Text(sub, style = T.Muted)
            Spacer(Modifier.height(16.dp))

            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacroDonut(t.carbs, t.protein, t.fat)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${t.kcal.r0()} kcal", style = T.Big.copy(color = C.Accent))
                        Text("per ${grams.r0()} $unit", style = T.Muted)
                        Spacer(Modifier.height(6.dp))
                        MacroDots(t.protein, t.carbs, t.fat)
                    }
                }
            }

            SectionTitle("Quantità")
            if (hasServing) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${food.servingName}", mode == 1) {
                    if (mode != 1) { amount = ((parseNum(amount) ?: 0.0) / (food.servingGrams ?: 100.0)).r1().replace(",0", ""); mode = 1 }
                }
                Pill(unit, mode == 0) {
                    if (mode != 0) { amount = grams.r0(); mode = 0 }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("−") {
                    val step = if (mode == 1) 0.5 else 10.0
                    amount = ((parseNum(amount) ?: 0.0) - step).coerceAtLeast(0.0).let { if (mode == 1) it.r1().replace(",0", "") else it.r0() }
                }
                Spacer(Modifier.width(8.dp))
                JField(amount, { amount = it }, if (mode == 1) "Porzioni" else "Quantità", Modifier.weight(1f), number = true,
                    suffix = if (mode == 1) "× ${food.servingGrams?.r0()} $unit" else unit)
                Spacer(Modifier.width(8.dp))
                GhostButton("+") {
                    val step = if (mode == 1) 0.5 else 10.0
                    amount = ((parseNum(amount) ?: 0.0) + step).let { if (mode == 1) it.r1().replace(",0", "") else it.r0() }
                }
            }

            SectionTitle("Pasto")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MEAL_NAMES.forEachIndexed { i, n -> Pill(n.take(8), mealSel == i) { mealSel = i } }
            }

            SectionTitle("Valori nutrizionali")
            JCard {
                NutRow("Energia", "${t.kcal.r0()} kcal", bold = true)
                NutRow("Carboidrati", "${t.carbs.r1()} g", bold = true)
                food.sugar?.let { NutRow("   di cui zuccheri", "${t.sugar.r1()} g") }
                food.fiber?.let { NutRow("Fibre", "${t.fiber.r1()} g") }
                NutRow("Proteine", "${t.protein.r1()} g", bold = true)
                NutRow("Grassi", "${t.fat.r1()} g", bold = true)
                food.satFat?.let { NutRow("   di cui saturi", "${t.satFat.r1()} g") }
                food.salt?.let { NutRow("Sale", "${t.salt.r1()} g") }
                Spacer(Modifier.height(6.dp))
                Text("Per 100 $unit: ${food.kcal.r0()} kcal · C ${food.carbs.r1()} · P ${food.protein.r1()} · G ${food.fat.r1()}", style = T.Small)
            }
            if (food.micro.size >= 8) {
                SectionTitle("Vitamine e minerali")
                JCard {
                    com.jarvis.food.engine.Micros.defs.filter { it.kind != "LIM" && it.key != "fiber" }.forEach { d ->
                        val v = (food.micro[d.key] ?: 0.0) * grams / 100
                        val ref = if (s.profile.male) d.men else d.women
                        if (!d.perMJ) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name, style = T.Body, modifier = Modifier.width(110.dp))
                            ProgressLine((v / ref).toFloat(), C.Green, Modifier.weight(1f), 6.dp)
                            Text("  " + com.jarvis.food.engine.Micros.fmt(v, d.unit), style = T.Small, modifier = Modifier.width(80.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (entry != null) {
                GhostButton("Elimina", Modifier.weight(1f), color = C.Danger) { Store.removeEntry(entry.id); Nav.back() }
                PrimaryButton("Salva", Modifier.weight(2f), enabled = grams > 0) {
                    Store.updateEntry(entry.id, grams, mealSel); Nav.back()
                }
            } else {
                PrimaryButton("Aggiungi a ${MEAL_NAMES[mealSel]}", Modifier.fillMaxWidth(), enabled = grams > 0) {
                    Store.addEntry(date, mealSel, food, grams)
                    Nav.backTo { it !is Screen.AddFood && it !is Screen.FoodDetail && it !is Screen.PhotoAi && it !is Screen.CustomFood }
                }
            }
        }
    }
}

@Composable
private fun NutRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = if (bold) T.Label else T.Muted, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) T.Label else T.Muted)
    }
    HorizontalDivider(color = C.Line)
}

