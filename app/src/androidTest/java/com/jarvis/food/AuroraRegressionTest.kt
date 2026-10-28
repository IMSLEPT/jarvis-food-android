package com.jarvis.food

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.Screen
import com.jarvis.food.ui.EditorialPhotos
import android.graphics.BitmapFactory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native Compose checks: navigation, categories and the original plan actions. */
@RunWith(AndroidJUnit4::class)
class AuroraRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        val ctx = ins.targetContext
        Store.init(ctx)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Test Aurora")) }
            Nav.clear(); Nav.tab = 0; Nav.kitchenTab = 2
        }
    }

    @Test fun navigationAndProfileRemainReachable() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Oggi", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Piano", useUnmergedTree = true).performClick()
            compose.waitForIdle()
            assertEquals(1, Nav.tab)
            compose.onNodeWithContentDescription("Apri profilo").performClick()
            compose.onNodeWithText("Profilo e obiettivo").assertExists()
            ui { Nav.back() }
            compose.onNodeWithContentDescription("Famiglia", useUnmergedTree = true).performClick()
            compose.waitForIdle()
            assertTrue(Nav.stack.last() is Screen.Family)
            ui { Nav.clear() }
            compose.onNodeWithText("Jarvis", useUnmergedTree = true).performClick()
            compose.waitForIdle()
            assertEquals(5, Nav.tab)
        }
    }

    @Test fun originalCategoriesOpenNativeResults() {
        prepare()
        // Start directly in the catalogue: changing tab during the first measure can
        // re-enter Android 10's layout through Espresso's idle synchronization.
        ui { Nav.tab = 2; Nav.kitchenTab = 2 }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Mostra tutte").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Mostra tutte").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onAllNodesWithTag("recipe-category").assertCountEquals(56) // 55 original categories + all recipes
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo().performClick()
            compose.waitForIdle()
            val list = Nav.stack.last() as Screen.RecipeList
            assertEquals(setOf("vegana"), list.filter.diets)
            compose.onNodeWithText("Elenco ricette").assertExists()
        }
    }

    @Test fun compactPlanKeepsDiaryRecipeAlternativesAndShoppingActions() {
        prepare()
        val today = java.time.LocalDate.now()
        ui { Store.generatePlan(today.with(java.time.DayOfWeek.MONDAY), 7) }
        val first = Store.plannedMeals(Store.current, today).first()
        val originalDiary = Store.current.diary.size
        ActivityScenario.launch(MainActivity::class.java).use {
            // Home no longer duplicates the plan, even when a full plan exists.
            compose.onAllNodesWithText("Il piano di oggi").assertCountEquals(0)
            compose.onAllNodesWithText("Crea la tua dieta personalizzata").assertCountEquals(0)
            // Verify native Android decoding, including the process thumbnails.
            for (path in EditorialPhotos.paths) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ins.targetContext.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                assertTrue("Photo too small or unreadable: $path", options.outWidth >= 640 && options.outHeight >= 640)
            }
            ui { Nav.tab = 1; Nav.planDate = today }
            compose.onAllNodesWithText("Segna mangiato")[0].performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Store.current.diary.size > originalDiary)
            compose.onAllNodesWithText("Mangiato ✓")[0].performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(originalDiary, Store.current.diary.size)
            compose.onAllNodesWithText("Alternative")[0].performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Nav.stack.last() is Screen.Alternatives)
            ui { Nav.back() }
            compose.onAllNodesWithText(first.solved.template.name)[0].performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Nav.stack.last() is Screen.RecipeView)
            ui { Nav.back() }
            compose.onNodeWithText("Crea lista della spesa").performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(2, Nav.tab)
            assertEquals(1, Nav.kitchenTab)
            assertTrue(Store.current.shopping.isNotEmpty())
        }
    }

}
