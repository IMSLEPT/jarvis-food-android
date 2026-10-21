package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Goals
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.data.WeightEntry
import java.time.LocalDate

@Composable
fun Onboarding() {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var male by remember { mutableStateOf(true) }
    var birth by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var activity by remember { mutableIntStateOf(1) }
    var goal by remember { mutableIntStateOf(0) }
    var rate by remember { mutableDoubleStateOf(0.5) }

    fun profile() = Profile(
        name = name.trim().ifBlank { "Tu" }, male = male,
        birthYear = birth.toIntOrNull() ?: 1995, heightCm = parseNum(height) ?: 175.0,
        startWeight = parseNum(weight) ?: 75.0, targetWeight = parseNum(target) ?: parseNum(weight) ?: 75.0,
        activity = activity, goal = goal, weeklyRate = rate
    )

    val canNext = when (step) {
        1 -> (birth.toIntOrNull() ?: 0) in 1920..(LocalDate.now().year - 14) && (parseNum(height) ?: 0.0) in 120.0..230.0
        2 -> (parseNum(weight) ?: 0.0) in 30.0..300.0 && (parseNum(target) ?: 0.0) in 30.0..300.0
        else -> true
    }

    AppBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(20.dp)
        ) {
            ProgressLine((step + 1) / 6f, C.Accent, Modifier.padding(top = 8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp)) {
                when (step) {
                    0 -> {
                        Text("🥗", style = T.Big)
                        Spacer(Modifier.height(12.dp))
                        Text("Benvenuto in\nJarvis Food", style = T.Display)
                        Spacer(Modifier.height(10.dp))
                        Text("Conta calorie e macro, digiuno intermittente, acqua, peso e AI per riconoscere i pasti dalle foto. Tutti i dati restano sul tuo telefono.", style = T.Muted)
                        Spacer(Modifier.height(24.dp))
                        JField(name, { name = it }, "Come ti chiami?")
                    }
                    1 -> {
                        Text("Parlami di te", style = T.Display)
                        Text("Servono per calcolare il tuo fabbisogno.", style = T.Muted)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill("Uomo", male) { male = true }
                            Pill("Donna", !male) { male = false }
                        }
                        Spacer(Modifier.height(14.dp))
                        JField(birth, { birth = it.filter { c -> c.isDigit() }.take(4) }, "Anno di nascita", number = true)
                        Spacer(Modifier.height(10.dp))
                        JField(height, { height = it }, "Altezza", number = true, suffix = "cm")
                    }
                    2 -> {
                        Text("Il tuo peso", style = T.Display)
                        Spacer(Modifier.height(20.dp))
                        JField(weight, { weight = it }, "Peso attuale", number = true, suffix = "kg")
                        Spacer(Modifier.height(10.dp))
                        JField(target, { target = it }, "Peso obiettivo", number = true, suffix = "kg")
                    }
                    3 -> {
                        Text("Quanto sei attivo?", style = T.Display)
                        Spacer(Modifier.height(16.dp))
                        Nutrition.ACTIVITY_LABELS.forEachIndexed { i, l ->
                            Choice(l, activity == i) { activity = i }
                        }
                    }
                    4 -> {
                        Text("Il tuo obiettivo", style = T.Display)
                        Spacer(Modifier.height(16.dp))
                        Choice("Dimagrire", goal == -1) { goal = -1 }
                        Choice("Mantenere il peso", goal == 0) { goal = 0 }
                        Choice("Aumentare massa muscolare", goal == 1) { goal = 1 }
                        if (goal != 0) {
                            Spacer(Modifier.height(16.dp))
                            Text("Ritmo: ${rate.r1()} kg a settimana", style = T.Title2)
                            Slider(
                                value = rate.toFloat(), onValueChange = { rate = (Math.round(it * 4) / 4.0).coerceAtLeast(0.25) },
                                valueRange = 0.25f..1f, steps = 2,
                                colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.SurfaceHi)
                            )
                            Text(
                                if (goal == -1) "Un ritmo di 0,25–0,5 kg a settimana è il più sostenibile."
                                else "Per aumentare massa con poco grasso, 0,25 kg a settimana è un buon punto di partenza.",
                                style = T.Small
                            )
                        }
                    }
                    else -> {
                        val p = profile()
                        val g = Nutrition.autoGoals(p, p.startWeight, Goals())
                        val (c, pr, f) = Nutrition.goalGrams(g)
                        Text("Il tuo piano", style = T.Display)
                        Spacer(Modifier.height(16.dp))
                        JCard {
                            Text("Obiettivo giornaliero", style = T.Muted)
                            Text("${g.kcal} kcal", style = T.Big.copy(color = C.Accent))
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Stat("Carboidrati", "${c.r0()} g", C.Carbs)
                                Stat("Proteine", "${pr.r0()} g", C.Protein)
                                Stat("Grassi", "${f.r0()} g", C.Fat)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Acqua: ${g.waterMl} ml al giorno", style = T.Label)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Metabolismo basale ${Nutrition.bmr(p, p.startWeight).r0()} kcal (Mifflin-St Jeor), fabbisogno ${Nutrition.tdee(p, p.startWeight).r0()} kcal. " +
                                    "Sono stime: potrai modificare tutto dal Profilo. Non sostituiscono il parere di un medico o di un nutrizionista.",
                            style = T.Small
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) GhostButton("Indietro", Modifier.weight(1f)) { step-- }
                PrimaryButton(if (step == 5) "Inizia" else "Avanti", Modifier.weight(2f), enabled = canNext) {
                    if (step < 5) step++ else {
                        Store.update { it.copy(dietPrefs = it.dietPrefs) }
                        val p = profile()
                        Store.update {
                            it.copy(
                                onboarded = true, profile = p,
                                goals = Nutrition.autoGoals(p, p.startWeight, it.goals),
                                weights = listOf(WeightEntry(LocalDate.now().toString(), p.startWeight))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) C.Accent.copy(alpha = 0.12f) else C.Surface)
            .border(1.dp, if (selected) C.Accent else C.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp)
    ) { Text(text, style = T.Label.copy(color = if (selected) C.Accent else C.Text)) }
}

@Composable
fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, style = T.Title2.copy(color = color))
        Text(label, style = T.Small)
    }
}
