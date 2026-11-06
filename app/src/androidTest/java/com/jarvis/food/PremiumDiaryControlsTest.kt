package com.jarvis.food

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Food
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.Screen
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PremiumDiaryControlsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun diaryDeleteUpdatesNutritionAndShortcutsOpenTheirOwnFlows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val day = LocalDate.now()
        val food = Food("premium-diary-oats", "Avena prova", kcal = 400.0, protein = 10.0, carbs = 60.0, fat = 10.0)
        Store.init(instrumentation.targetContext)
        instrumentation.runOnMainSync {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }
            Store.addEntry(day, 0, food, 50.0)
            Nav.clear()
            Nav.tab = 0
            Nav.diaryDate = day
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            fun openMeal() {
                if (compose.onAllNodesWithTag("diary-actions-0").fetchSemanticsNodes().isEmpty()) {
                    compose.onNodeWithText("Colazione").performScrollTo().performClick()
                }
                compose.onNodeWithTag("diary-actions-0").performScrollTo()
            }
            openMeal()
            assertEquals(200.0, Nutrition.sum(Store.entriesFor(Store.current, day)).kcal, .01)
            compose.onNodeWithContentDescription("Elimina Avena prova").performScrollTo().performClick()
            assertEquals(0.0, Nutrition.sum(Store.entriesFor(Store.current, day)).kcal, .01)
            compose.onNodeWithText("Avena prova").assertDoesNotExist()
            compose.onNode(hasText("Foto AI") and hasClickAction()).performScrollTo().performClick()
            assertEquals(Screen.PhotoAi(day, 0), Nav.stack.last())
            instrumentation.runOnMainSync { Nav.back() }
            compose.waitForIdle()
            openMeal()
            compose.onNodeWithTag("meal-barcode-0").performScrollTo().performClick()
            assertTrue((Nav.stack.lastOrNull() as? Screen.AddFood)?.scanOnOpen == true)
        }
    }
}
