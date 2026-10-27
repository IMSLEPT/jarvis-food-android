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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Food
import com.jarvis.food.data.Recipe
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.RecipeTemplate
import java.time.LocalDate

/** Schermate secondarie aperte sopra le schede principali. */
sealed class Screen {
    data class AddFood(val date: LocalDate, val meal: Int, val query: String = "", val offlineOnly: Boolean = false) : Screen()
    data class FoodDetail(val food: Food, val date: LocalDate, val meal: Int, val grams: Double? = null, val entry: DiaryEntry? = null) : Screen()
    data class CustomFood(val date: LocalDate?, val meal: Int, val barcode: String? = null, val edit: Food? = null) : Screen()
    data class PhotoAi(val date: LocalDate, val meal: Int) : Screen()
    /** Ricetta del database (o creata dall'AI) calcolata su un obiettivo. [slot] valorizzato se viene dal piano. */
    data class RecipeView(val recipeId: String, val target: Macro, val date: LocalDate, val slot: Int? = null,
                          val diaryMeal: Int = 1, val adHoc: RecipeTemplate? = null,
                          /** true = si apre la ricetta base (grammature dell'autore); false = già adattata all'obiettivo. */
                          val original: Boolean = false) : Screen()
    data class RecipeList(val filter: com.jarvis.food.engine.RecipeFilter = com.jarvis.food.engine.RecipeFilter(), val title: String = "Ricette", val mine: Boolean = false) : Screen()
    data object WhatNow : Screen()
    data class Alternatives(val date: LocalDate, val slot: Int) : Screen()
    data class UserRecipeDetail(val recipe: Recipe) : Screen()
    data class RecipeEditor(val recipe: Recipe?) : Screen()
    data class Activity(val date: LocalDate) : Screen()
    data class DayNutrients(val date: LocalDate) : Screen()
    data class PantryScan(val mode: String) : Screen()
    data class PantryIdeas(val meal: String?) : Screen()
    data object Profile : Screen()
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
    data object Family : Screen()
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
    var kitchenTab by mutableIntStateOf(2)
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
            is Screen.AddFood -> AddFoodScreen(s.date, s.meal, s.query, s.offlineOnly)
            is Screen.FoodDetail -> FoodDetailScreen(s.food, s.date, s.meal, s.grams, s.entry)
            is Screen.CustomFood -> CustomFoodScreen(s.date, s.meal, s.barcode, s.edit)
            is Screen.PhotoAi -> PhotoAiScreen(s.date, s.meal)
            is Screen.RecipeView -> RecipeViewScreen(s)
            is Screen.RecipeList -> RecipeListScreen(s)
            Screen.WhatNow -> WhatNowScreen()
            is Screen.Alternatives -> AlternativesScreen(s.date, s.slot)
            is Screen.UserRecipeDetail -> UserRecipeDetailScreen(s.recipe)
            is Screen.RecipeEditor -> RecipeEditorScreen(s.recipe)
            is Screen.Activity -> ActivityScreen(s.date)
            is Screen.DayNutrients -> DayNutrientsScreen(s.date)
            is Screen.PantryScan -> PantryScanScreen(s.mode)
            is Screen.PantryIdeas -> PantryIdeasScreen(s.meal)
            Screen.Profile -> ProfileScreen()
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
            Screen.Family -> com.jarvis.food.family.FamilyScreen()
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
    val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showBottomBar = !(Nav.tab == 5 && imeUp)
    Column(Modifier.fillMaxSize()) {
        // BottomBar owns the navigation inset; children must not reserve it a second time.
        Box(Modifier.weight(1f).then(if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier)) {
            when (Nav.tab) {
                0 -> TodayScreen()
                1 -> PlanScreen()
                2 -> KitchenScreen()
                3 -> ProgressScreen()
                4 -> ProfileScreen()
                else -> ChatScreen()
            }
        }
        // Con la tastiera aperta nella chat la barra si nasconde: l'input resta subito sopra la tastiera.
        if (showBottomBar) BottomBar()
    }
}

@Composable
private fun BottomBar() {
    val items = listOf(Triple(0, Ic.Sun, "Oggi"), Triple(1, Ic.Calendar, "Piano"), Triple(2, Ic.Bowl, "Cucina"), Triple(3, Ic.Chart, "Progressi"), Triple(5, "", "Jarvis"))
    val shape = RoundedCornerShape(30.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().testTag("bottom-bar").selectableGroup().aShadow(shape, 10.dp, Color(0x261D1B3A)).clip(shape).background(Color.White).border(1.dp, C.Line, shape).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (tab, icon, label) ->
                val selected = Nav.tab == tab
                Column(Modifier.weight(1f).heightIn(min = 60.dp).testTag("nav-$tab").clip(RoundedCornerShape(22.dp))
                    .selectable(selected = selected, role = Role.Tab, onClick = { Nav.tab = tab }).padding(horizontal = 2.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (icon.isEmpty()) JarvisLogo(24.dp) else AIcon(icon, if (selected) C.Accent else A.Muted, 22.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = T.Small.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal, color = if (selected) C.Navy else A.Muted), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(28.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(if (selected) C.Accent else Color.Transparent))
                }
            }
        }
    }
}
