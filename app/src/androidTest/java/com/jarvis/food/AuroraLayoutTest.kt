package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.graphics.ColorUtils
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
import java.time.DayOfWeek
import java.time.LocalDate

/** Executed on real emulator configurations, including Android system font scaling. */
@RunWith(AndroidJUnit4::class)
class AuroraLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private val profile get() = InstrumentationRegistry.getArguments().getString("captureName") ?: "default"

    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = ins.uiAutomation.takeScreenshot()
        val dir = File(ins.targetContext.filesDir, "shots/layout-$profile").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun readable(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Text layout missing", results.isNotEmpty())
        results.forEach { result ->
            // Compose exposes fractional paragraph dimensions but rounds the measured box to pixels.
            // Allow only that single-pixel rounding, never ellipsis or a missing line.
            val paragraph = result.multiParagraph
            val details = "${result.layoutInput.text.text}: box=${result.size}, paragraph=${paragraph.width}x${paragraph.height}, lines=${result.lineCount}"
            assertFalse("Ellipsized text: $details", (0 until result.lineCount).any { result.isLineEllipsized(it) })
            assertTrue("Clipped text width: $details", paragraph.width <= result.size.width + 1f)
            assertTrue("Clipped text height: $details", paragraph.height <= result.size.height + 1f)
        }
    }

    @Test fun barsGreetingsAndLongActionsRemainVisible() {
        Store.init(ins.targetContext)
        val day = LocalDate.now()
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro")) }
            Store.generatePlan(day.with(DayOfWeek.MONDAY), 7)
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day; Nav.kitchenTab = 2
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            var navigationBottom = 0
            var windowHeight = 0
            scenario.onActivity { activity ->
                navigationBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                windowHeight = activity.window.decorView.height
            }
            compose.waitForIdle()
            shot("home-before-check")
            readable(compose.onNodeWithTag("home-greeting"))
            val greeting = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("home-greeting").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(greeting) }
            assertTrue("Greeting must be readable at normal size", greeting.single().layoutInput.style.fontSize.value >= 16f)
            for ((tab, label) in listOf(0 to "Oggi", 1 to "Piano", 2 to "Cucina", 3 to "Progressi", 5 to "Jarvis")) {
                compose.onNodeWithTag("nav-$tab").performClick().assertIsSelected()
                compose.waitForIdle()
                val bar = compose.onNodeWithTag("bottom-bar").fetchSemanticsNode().boundsInRoot
                assertTrue("Bar overlaps Android navigation", bar.bottom <= windowHeight - navigationBottom + 1f)
                val text = compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("bottom-bar")), useUnmergedTree = true)
                readable(text)
                compose.onNodeWithTag("nav-$tab").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
                if (tab != 5) {
                    val scroll = compose.onNodeWithTag("main-scroll").fetchSemanticsNode().boundsInRoot
                    assertTrue("Content overlaps bottom bar", scroll.bottom <= bar.top + 1f)
                }
                shot("tab-$tab")
            }
            // A synthetic non-empty key enables the local input UI. No message is sent.
            ui { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1, apiKey = "layout-test-no-network")) } }
            compose.onNodeWithTag("chat-input").performClick().performTextInput("Prova layout")
            var imeBottom = 0
            compose.waitUntil(20_000) {
                scenario.onActivity { activity ->
                    imeBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                }
                imeBottom > 0
            }
            compose.waitForIdle()
            compose.onNodeWithTag("bottom-bar").assertDoesNotExist()
            val input = compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot
            assertTrue("Chat input overlaps keyboard", input.bottom <= windowHeight - imeBottom + 1f)
            shot("chat-keyboard")
            ins.uiAutomation.executeShellCommand("input keyevent 4").close()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().isNotEmpty() }
            ui { Store.update { it.copy(ai = it.ai.copy(apiKey = "")) } }
            ui { Nav.tab = 0 }
            compose.onNodeWithContentDescription("Apri profilo").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("bottom-bar").assertDoesNotExist()
            shot("profile")
            ui { Nav.clear() }
            ui { Nav.tab = 1 }
            compose.onNodeWithText("Crea lista della spesa").performScrollTo()
            readable(compose.onNodeWithText("Crea lista della spesa", useUnmergedTree = true))
            shot("plan-bottom")
            val first = Store.plannedMeals(Store.current, day).first()
            ui { Nav.go(Screen.RecipeView(first.solved.template.id, first.solved.target, day, first.slotIndex, first.diaryMeal)) }
            compose.waitForIdle()
            var content = compose.onNodeWithTag("recipe-scroll").fetchSemanticsNode().boundsInRoot
            var footer = compose.onNodeWithTag("recipe-footer").fetchSemanticsNode().boundsInRoot
            assertTrue("Recipe footer covers content", content.bottom <= footer.top + 1f)
            readable(compose.onNodeWithText("Segna mangiato", useUnmergedTree = true))
            shot("recipe-planned")
            ui { Nav.clear(); Nav.go(Screen.RecipeView(first.solved.template.id, first.solved.target, day, diaryMeal = 4, original = true)) }
            compose.waitForIdle()
            content = compose.onNodeWithTag("recipe-scroll").fetchSemanticsNode().boundsInRoot
            footer = compose.onNodeWithTag("recipe-footer").fetchSemanticsNode().boundsInRoot
            assertTrue("Recipe footer covers content with long action", content.bottom <= footer.top + 1f)
            readable(compose.onNodeWithText("＋ Aggiungi a ${MEAL_LABEL.getValue(4).lowercase()}", useUnmergedTree = true))
            shot("recipe-long-action")
            ui { Nav.clear(); Nav.tab = 2 }
            compose.onNodeWithText("Mostra tutte").performScrollTo().performClick()
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText("Vegana", useUnmergedTree = true))
            shot("categories")
        }
    }

    @Test fun smallTextAndWhiteOrangeLabelsMeetContrast() {
        val pairs = listOf(
            Color.White to C.ButtonOrange, Color.White to C.ButtonOrangeEnd,
            A.ProteinInk to A.ProteinSoft, A.CarbInk to A.CarbSoft,
            A.FatInk to A.FatSoft, A.GreenInk to A.GreenSoft,
            A.TonalInk to A.Tonal, A.FastInk to A.FastSoft,
            C.Navy to C.Bg, C.Muted to C.Surface
        )
        for ((foreground, background) in pairs) {
            val ratio = ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb())
            assertTrue("Insufficient text contrast: $ratio", ratio >= 4.5)
        }
    }
}
