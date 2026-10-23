package com.jarvis.food

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.appearance.Appearance
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.Screen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native Compose checks: real navigation and the two complete interfaces, sharing the same Store. */
@RunWith(AndroidJUnit4::class)
class AppearanceRegressionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        val ctx = ins.targetContext
        Store.init(ctx)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Test Aurora")) }
            Appearance.init(ctx)
            Appearance.selectClassic(false)
            Appearance.settingsOpen = false
            Nav.clear(); Nav.tab = 0; Nav.kitchenTab = 2
            com.jarvis.food.classicui.Nav.clear(); com.jarvis.food.classicui.Nav.tab = 0
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
            compose.onNodeWithText("Aspetto").assertExists()
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
        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.tab = 2; Nav.kitchenTab = 2 }
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

    @Test fun appearanceRollbackPersistsWithoutChangingNutritionData() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val before = Store.exportJson()
            ui { Nav.go(Screen.Profile) }
            compose.onNodeWithText("Aspetto").performScrollTo().performClick()
            compose.onNodeWithText("Aurora precedente").performClick()
            compose.onNodeWithText("Fatto").performClick()
            compose.waitForIdle()
            assertTrue(Appearance.classic)
            assertEquals(before, Store.exportJson())
            scenario.recreate()
            compose.waitForIdle()
            assertTrue(Appearance.classic)
            ui { com.jarvis.food.classicui.Nav.tab = 4 }
            compose.onNodeWithText("Aspetto").performScrollTo().performClick()
            compose.onNodeWithText("Aurora", substring = false).performClick()
            compose.onNodeWithText("Fatto").performClick()
            compose.waitForIdle()
            assertFalse(Appearance.classic)
            assertEquals(before, Store.exportJson())
        }
    }
}
