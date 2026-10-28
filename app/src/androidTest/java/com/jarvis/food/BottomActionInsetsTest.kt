package com.jarvis.food

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/** Real system insets, including the three-button navigation shown in the bug report. */
@RunWith(AndroidJUnit4::class)
class BottomActionInsetsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)

    @Test fun bottomActionsRemainAboveSystemNavigation() {
        Store.init(ins.targetContext)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro")) }
            Nav.clear(); Nav.tab = 0
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            val capture = InstrumentationRegistry.getArguments().getString("captureName") ?: "footer"
            val expectedMode = InstrumentationRegistry.getArguments().getString("navigationMode")?.toInt()
            if (expectedMode != null) {
                assertEquals("Requested navigation mode was not applied", expectedMode,
                    Settings.Secure.getInt(ins.targetContext.contentResolver, "navigation_mode", -1))
            }
            var windowHeight = 0
            var navigationBottom = 0
            scenario.onActivity { activity ->
                windowHeight = activity.window.decorView.height
                navigationBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            }
            assertTrue("A real navigation inset is required", navigationBottom > 0)

            val cases = listOf(
                Triple("goals", Screen.Goals, "Salva obiettivi"),
                Triple("diet", Screen.DietSettings, "Salva e ricalcola il piano"),
                Triple("profile-edit", Screen.ProfileEdit, "Salva nome"),
                Triple("custom-food", Screen.CustomFood(null, 0), "Salva"),
                Triple("recipe-editor", Screen.RecipeEditor(null), "Salva ricetta"),
                Triple("ai-settings", Screen.AiSettings, "Salva"),
                Triple("activity", Screen.Activity(LocalDate.now()), "Aggiungi · 0 kcal")
            )
            for ((name, screen, label) in cases) {
                ui { Nav.clear(); Nav.go(screen) }
                compose.waitForIdle()
                val button = compose.onNode(hasText(label) and hasClickAction())
                button.assertIsDisplayed().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
                val bounds = button.fetchSemanticsNode().boundsInRoot
                val safeBottom = windowHeight - navigationBottom
                assertTrue("$name button overlaps Android navigation: $bounds, safeBottom=$safeBottom",
                    bounds.bottom <= safeBottom + 1f)
                assertTrue("$name button is outside the visible window", bounds.top >= 0f && bounds.height >= 48f)
                val text = compose.onNodeWithText(label, useUnmergedTree = true)
                val results = mutableListOf<TextLayoutResult>()
                text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                assertTrue("$name label has no layout", results.isNotEmpty())
                results.forEach { result ->
                    assertFalse("$name label is clipped", result.multiParagraph.didExceedMaxLines)
                    assertTrue("$name label is clipped vertically", result.multiParagraph.height <= result.size.height + 1f)
                    assertFalse("$name label is ellipsized", (0 until result.lineCount).any { result.isLineEllipsized(it) })
                }
                if (name == "goals" || name == "diet") {
                    // Reproduce the screenshots: scroll the form to its last controls.
                    compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
                    compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
                    button.assertIsDisplayed()
                    val scroll = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
                    assertTrue("$name content extends behind its fixed button", scroll.bottom <= bounds.top + 1f)
                }
                ins.waitForIdleSync()
                android.os.SystemClock.sleep(500)
                val dir = File(ins.targetContext.filesDir, "shots/layout-$capture").apply { mkdirs() }
                File(dir, "footer-$name.png").outputStream().use {
                    ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
