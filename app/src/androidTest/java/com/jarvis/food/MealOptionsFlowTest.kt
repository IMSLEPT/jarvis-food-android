package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MealOptionsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val today = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare(s: AppState) {
        Store.init(ins.targetContext)
        ui { Store.update { s }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = today; Nav.planDate = today }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val capture = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/options-$capture").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun sixAndThreeMealsConfigureBothDiaryAndPlan() {
        prepare(AppState(onboarded = true, profile = Profile(detailed = true)))
        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.go(Screen.DietSettings) }
            compose.onNodeWithTag("meal-enabled-4").performScrollTo().performClick()
            compose.onNodeWithTag("meal-enabled-5").performScrollTo().performClick()
            shot("six-meals")
            compose.onNodeWithText("Salva e ricalcola il piano").performClick()
            compose.waitForIdle()
            assertEquals(MealSchedule.order, MealSchedule.selected(Store.current.dietPrefs))
            assertEquals(6, Store.dayPlan(Store.current, today)!!.slots.size)
            assertEquals(6, Store.plannedMeals(Store.current, today).size)
            ui { Nav.clear(); Nav.tab = 1 }
            compose.onNodeWithTag("plan-meal-header-0").performScrollTo().assertIsDisplayed()
            shot("plan-separated-meals")
            compose.onNodeWithTag("plan-recipe-0").performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Nav.stack.last() is Screen.RecipeView)
            ui { Nav.back() }
            compose.onNodeWithTag("plan-alternatives-0").performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Nav.stack.last() is Screen.Alternatives)
            ui { Nav.back() }
            compose.onNodeWithTag("plan-eaten-0").performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(1, Store.current.diary.size)
            assertEquals(0, Store.current.diary.single().meal)
            compose.onNodeWithTag("plan-eaten-0").performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Store.current.diary.isEmpty())
            ui { Nav.clear(); Nav.go(Screen.DietSettings) }
            for (id in listOf(4, 3, 5)) compose.onNodeWithTag("meal-enabled-$id").performScrollTo().performClick()
            compose.onNodeWithText("Salva e ricalcola il piano").performClick()
            compose.waitForIdle()
            assertEquals(listOf(0, 1, 2), MealSchedule.selected(Store.current.dietPrefs))
            assertEquals(3, Store.plannedMeals(Store.current, today).size)
            assertEquals(listOf(0, 1, 2), MealSchedule.visible(Store.current, today.toString()))
        }
    }
    @Test fun automaticTargetsSaveAndResolveDayOverridesAndWeightChanges() {
        val profile = Profile(detailed = true, heightCm = 173.0, startWeight = 71.0)
        prepare(AppState(onboarded = true, profile = profile, trainingNutrition = TrainingNutrition(trainingDays = listOf(today.dayOfWeek.value))))
        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.go(Screen.TrainingGoals) }
            compose.onNodeWithTag("training-enabled").performClick()
            compose.onNodeWithText("Automatico Jarvis").performScrollTo().performClick()
            compose.onNodeWithTag("auto-training-minutes").performScrollTo().performTextReplacement("60")
            compose.onNodeWithText("Mostra come viene calcolato").performScrollTo().performClick()
            shot("automatic-calculation")
            compose.onNodeWithText("Salva giorni ON/OFF").performClick()
            compose.waitForIdle()
            assertTrue(Store.current.trainingNutrition.automatic)
            val before = Store.energyFor(Store.current, today)
            ui { Store.setDayType(today, false) }
            val off = Store.energyFor(Store.current, today)
            assertTrue(before.targetKcal > off.targetKcal)
            assertEquals(0.0, off.extraKcal, 0.0)
            ui { Store.setDayType(today, true); Store.update { state -> state.copy(weights = listOf(WeightEntry(today.toString(), 80.0))) } }
            assertTrue(Store.energyFor(Store.current, today).targetKcal > before.targetKcal)
        }
    }
    @Test fun barcodeAndPhotoReplacementNeverAddAnExtraEntry() {
        val old = Food("old", "Alimento originale", kcal = 200.0, protein = 10.0, carbs = 20.0, fat = 5.0)
        val product = old.copy(id = "new", name = "Prodotto barcode", barcode = "8001234567890", kcal = 100.0)
        val entry = DiaryEntry("replace-barcode", today.toString(), 1, old, 150.0, 42L)
        prepare(AppState(onboarded = true, profile = Profile(detailed = true), diary = listOf(entry), customFoods = listOf(product)))
        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.go(Screen.ReplaceFood(entry.id)) }
            compose.onNodeWithTag("replacement-results").performScrollToNode(hasTestTag("replace-photo"))
            compose.onNodeWithTag("replace-photo").performClick()
            compose.onNodeWithText("Fotografa il nuovo alimento").assertIsDisplayed()
            assertEquals(listOf(entry), Store.current.diary)
            ui { Nav.back() }
            compose.onNodeWithTag("replacement-results").performScrollToNode(hasText("Inserisci codice a mano"))
            compose.onNodeWithText("Inserisci codice a mano").performClick()
            compose.onNodeWithText("Codice prodotto").performTextInput("8001234567890")
            compose.onNodeWithText("Cerca").performClick()
            compose.waitForIdle()
            assertEquals(listOf(entry), Store.current.diary)
            shot("barcode-preview")
            compose.onNode(hasText("Sostituisci") and hasClickAction()).performClick()
            compose.waitForIdle()
            assertEquals(listOf(entry.copy(food = product)), Store.current.diary)
            assertTrue(Nav.stack.isEmpty())
        }
    }
}
