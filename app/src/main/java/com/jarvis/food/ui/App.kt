package com.jarvis.food.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Food
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.RecipeTemplate
import java.time.LocalDate

/** Schermate secondarie aperte sopra le schede principali. */
sealed class Screen {
    data class AddFood(val date: LocalDate, val meal: Int, val query: String = "") : Screen()
    data class FoodDetail(val food: Food, val date: LocalDate, val meal: Int, val grams: Double? = null, val entry: DiaryEntry? = null) : Screen()
    data class CustomFood(val date: LocalDate?, val meal: Int, val barcode: String? = null, val edit: Food? = null) : Screen()
    data class PhotoAi(val date: LocalDate, val meal: Int) : Screen()
    /** Ricetta del database (o creata dall'AI) calcolata su un obiettivo. [slot] valorizzato se viene dal piano. */
    data class RecipeView(val recipeId: String, val target: Macro, val date: LocalDate, val slot: Int? = null,
                          val diaryMeal: Int = 1, val adHoc: RecipeTemplate? = null) : Screen()
    data class Alternatives(val date: LocalDate, val slot: Int) : Screen()
    data class UserRecipeDetail(val recipe: Recipe) : Screen()
    data class RecipeEditor(val recipe: Recipe?) : Screen()
    data class Activity(val date: LocalDate) : Screen()
    data class DayNutrients(val date: LocalDate) : Screen()
    data class PantryScan(val mode: String) : Screen()
    data class PantryIdeas(val meal: String?) : Screen()
    data object Goals : Screen()
    data object ProfileEdit : Screen()
    data object RemindersSet : Screen()
    data object AiSettings : Screen()
    data object Backup : Screen()
    data object Weight : Screen()
    data object Measurements : Screen()
    data object Fasting : Screen()
    data object FastingHistory : Screen()
    data object MyFoods : Screen()
    data object About : Screen()
    data object DietSettings : Screen()
    data object Watch : Screen()
    data object Micronutrients : Screen()
    data object EnergyInfo : Screen()
    /** Modifica delle risposte del questionario: [only] = una sola sezione, null = tutto. */
    data class Questionnaire(val only: QStep? = null) : Screen()
    data object Chat : Screen()
}

/** Navigazione semplice a pila, senza librerie esterne. */
object Nav {
    val stack = mutableStateListOf<Screen>()
    var tab by mutableIntStateOf(0)
    var diaryDate by mutableStateOf(LocalDate.now())
    var planDate by mutableStateOf(LocalDate.now())
    var kitchenTab by mutableIntStateOf(0)
    fun go(s: Screen) { stack.add(s) }
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun backTo(predicate: (Screen) -> Boolean) { while (stack.isNotEmpty() && !predicate(stack.last())) stack.removeAt(stack.lastIndex) }
    fun clear() { stack.clear() }
}

@Composable
fun App() {
    val state by Store.state.collectAsState()
    if (!state.onboarded) { Onboarding(); return }
    BackHandler(enabled = Nav.stack.isNotEmpty()) { Nav.back() }
    BackHandler(enabled = Nav.stack.isEmpty() && Nav.tab != 0) { Nav.tab = 0 }
    AppBackground {
        val top = Nav.stack.lastOrNull()
        AnimatedContent(targetState = top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { screen ->
            if (screen == null) Tabs() else ScreenHost(screen)
        }
    }
}

@Composable
private fun ScreenHost(s: Screen) {
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        when (s) {
            is Screen.AddFood -> AddFoodScreen(s.date, s.meal, s.query)
            is Screen.FoodDetail -> FoodDetailScreen(s.food, s.date, s.meal, s.grams, s.entry)
            is Screen.CustomFood -> CustomFoodScreen(s.date, s.meal, s.barcode, s.edit)
            is Screen.PhotoAi -> PhotoAiScreen(s.date, s.meal)
            is Screen.RecipeView -> RecipeViewScreen(s)
            is Screen.Alternatives -> AlternativesScreen(s.date, s.slot)
            is Screen.UserRecipeDetail -> UserRecipeDetailScreen(s.recipe)
            is Screen.RecipeEditor -> RecipeEditorScreen(s.recipe)
            is Screen.Activity -> ActivityScreen(s.date)
            is Screen.DayNutrients -> DayNutrientsScreen(s.date)
            is Screen.PantryScan -> PantryScanScreen(s.mode)
            is Screen.PantryIdeas -> PantryIdeasScreen(s.meal)
            Screen.Goals -> GoalsScreen()
            Screen.ProfileEdit -> ProfileEditScreen()
            Screen.RemindersSet -> RemindersScreen()
            Screen.AiSettings -> AiSettingsScreen()
            Screen.Backup -> BackupScreen()
            Screen.Weight -> WeightScreen()
            Screen.Measurements -> MeasurementsScreen()
            Screen.Fasting -> FastingScreen()
            Screen.FastingHistory -> FastingHistoryScreen()
            Screen.MyFoods -> MyFoodsScreen()
            Screen.About -> AboutScreen()
            Screen.DietSettings -> DietSettingsScreen()
            Screen.Watch -> WatchScreen()
            Screen.Micronutrients -> MicronutrientsScreen()
            Screen.EnergyInfo -> EnergyInfoScreen()
            is Screen.Questionnaire -> QuestionnaireScreen(s.only)
            Screen.Chat -> ChatScreen()
        }
    }
}

@Composable
private fun Tabs() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (Nav.tab) {
                0 -> TodayScreen()
                1 -> PlanScreen()
                2 -> KitchenScreen()
                3 -> ProgressScreen()
                else -> ProfileScreen()
            }
            ChatFab(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 14.dp))
        }
        BottomBar()
    }
}

@Composable
private fun BottomBar() {
    val items = listOf(NavIcon.Today to "Oggi", NavIcon.Plan to "Piano", NavIcon.Kitchen to "Cucina",
        NavIcon.Progress to "Progressi", NavIcon.Profile to "Profilo")
    Box(Modifier.fillMaxWidth().background(C.Bg).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(C.Surface)
                .border(1.dp, C.Line, RoundedCornerShape(28.dp)).padding(vertical = 8.dp)
        ) {
            items.forEachIndexed { i, (icon, label) ->
                val sel = Nav.tab == i
                Column(
                    Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Nav.tab = i },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (sel) C.AccentSoft else C.Surface)
                        .padding(horizontal = 16.dp, vertical = 5.dp)) { NavGlyph(icon, if (sel) C.AccentDeep else C.Muted) }
                    Spacer(Modifier.height(3.dp))
                    Text(label, style = T.Small.copy(color = if (sel) C.AccentDeep else C.Muted))
                }
            }
        }
    }
}
