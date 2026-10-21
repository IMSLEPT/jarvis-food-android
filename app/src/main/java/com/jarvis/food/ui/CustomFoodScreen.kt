package com.jarvis.food.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Food
import com.jarvis.food.data.Store
import java.time.LocalDate

@Composable
fun CustomFoodScreen(date: LocalDate?, meal: Int, barcode: String?, edit: Food?) {
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var brand by remember { mutableStateOf(edit?.brand ?: "") }
    var kcal by remember { mutableStateOf(edit?.kcal?.r1() ?: "") }
    var carbs by remember { mutableStateOf(edit?.carbs?.r1() ?: "") }
    var sugar by remember { mutableStateOf(edit?.sugar?.r1() ?: "") }
    var fiber by remember { mutableStateOf(edit?.fiber?.r1() ?: "") }
    var protein by remember { mutableStateOf(edit?.protein?.r1() ?: "") }
    var fat by remember { mutableStateOf(edit?.fat?.r1() ?: "") }
    var sat by remember { mutableStateOf(edit?.satFat?.r1() ?: "") }
    var salt by remember { mutableStateOf(edit?.salt?.r1() ?: "") }
    var servName by remember { mutableStateOf(edit?.servingName ?: "") }
    var servG by remember { mutableStateOf(edit?.servingGrams?.r0() ?: "") }
    var liquid by remember { mutableStateOf(edit?.liquid ?: false) }
    val code = edit?.barcode ?: barcode

    val valid = name.isNotBlank() && parseNum(kcal) != null

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (edit != null) "Modifica alimento" else "Nuovo alimento", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (code != null) Text("Codice a barre: $code", style = T.Muted)
            Text("Copia i valori dall'etichetta, riferiti a 100 ${if (liquid) "ml" else "g"}.", style = T.Small)
            JField(name, { name = it }, "Nome *")
            JField(brand, { brand = it }, "Marca")
            SwitchRow("È una bevanda (ml)", checked = liquid, onChange = { liquid = it })
            JField(kcal, { kcal = it }, "Energia *", number = true, suffix = "kcal")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(carbs, { carbs = it }, "Carboidrati", Modifier.weight(1f), number = true, suffix = "g")
                JField(sugar, { sugar = it }, "di cui zuccheri", Modifier.weight(1f), number = true, suffix = "g")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(protein, { protein = it }, "Proteine", Modifier.weight(1f), number = true, suffix = "g")
                JField(fiber, { fiber = it }, "Fibre", Modifier.weight(1f), number = true, suffix = "g")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(fat, { fat = it }, "Grassi", Modifier.weight(1f), number = true, suffix = "g")
                JField(sat, { sat = it }, "di cui saturi", Modifier.weight(1f), number = true, suffix = "g")
            }
            JField(salt, { salt = it }, "Sale", number = true, suffix = "g")
            Text("Porzione (facoltativa)", style = T.Title2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JField(servName, { servName = it }, "Nome (es. barretta)", Modifier.weight(1.4f))
                JField(servG, { servG = it }, "Peso", Modifier.weight(1f), number = true, suffix = if (liquid) "ml" else "g")
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (edit != null) GhostButton("Elimina", Modifier.weight(1f), color = C.Danger) {
                Store.deleteCustomFood(edit.id); Nav.back(); Nav.back()
            }
            PrimaryButton(if (date != null) "Salva e aggiungi" else "Salva", Modifier.weight(2f), enabled = valid) {
                val food = Food(
                    id = edit?.id ?: "custom:${Store.newId()}",
                    name = name.trim(), brand = brand.trim(),
                    kcal = parseNum(kcal) ?: 0.0, carbs = parseNum(carbs) ?: 0.0, protein = parseNum(protein) ?: 0.0,
                    fat = parseNum(fat) ?: 0.0, sugar = parseNum(sugar), fiber = parseNum(fiber), satFat = parseNum(sat),
                    salt = parseNum(salt),
                    servingName = servName.trim().ifBlank { null }?.takeIf { parseNum(servG) != null },
                    servingGrams = parseNum(servG)?.takeIf { servName.isNotBlank() },
                    barcode = code, source = "custom", liquid = liquid, category = "I miei"
                )
                Store.saveCustomFood(food)
                Nav.back()
                if (edit != null) Nav.back()
                if (date != null && edit == null) Nav.go(Screen.FoodDetail(food, date, meal))
            }
        }
    }
}

@Composable
fun MyFoodsScreen() {
    val s by Store.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("I miei alimenti", onBack = { Nav.back() })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item { PrimaryButton("+ Crea alimento", Modifier.fillMaxWidth()) { Nav.go(Screen.CustomFood(null, 0)) } }
            if (s.customFoods.isEmpty()) item { EmptyState("🧑‍🍳", "Nessun alimento", "Crea alimenti personalizzati o salva i prodotti non trovati dal barcode.") }
            items(s.customFoods, key = { it.id }) { f ->
                JCard(Modifier.padding(top = 8.dp), onClick = { Nav.go(Screen.CustomFood(null, 0, edit = f)) }) {
                    Text(f.name, style = T.Label)
                    Text("${f.kcal.r0()} kcal / 100 ${if (f.liquid) "ml" else "g"}" + if (f.brand.isNotBlank()) " · ${f.brand}" else "", style = T.Small)
                }
            }
        }
    }
}
