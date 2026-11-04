package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

/** Real geometry and keyboard checks at the device sizes/font scales configured in CI. */
@RunWith(AndroidJUnit4::class)
class PremiumVideoLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = LocalDate.now(); Nav.planDate = LocalDate.now()
        }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val config = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val file = File(ins.targetContext.filesDir, "shots/premium-$config/$name.png").apply { parentFile!!.mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
    private fun readable(node: SemanticsNodeInteraction, minimumSp: Float) {
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Text must expose layout", layouts.isNotEmpty())
        layouts.forEach { text ->
            assertTrue("Text shrunk below $minimumSp sp: ${text.layoutInput.text}", text.layoutInput.style.fontSize.value >= minimumSp)
            assertFalse("Missing lines: ${text.layoutInput.text}", text.multiParagraph.didExceedMaxLines)
            assertTrue("Clipped height: ${text.layoutInput.text}", text.multiParagraph.height <= text.size.height + 1f)
            for (line in 0 until text.lineCount) {
                assertFalse("Ellipsized: ${text.layoutInput.text}", text.isLineEllipsized(line))
                assertTrue("Clipped width: ${text.layoutInput.text}", text.getLineRight(line) - text.getLineLeft(line) <= text.size.width + 1f)
            }
        }
    }
    @Test fun fiveTabsKeepAccessibleNavigationAndHeaderInsets() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle(); ins.waitForIdleSync()
            var safeBottom = 0
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                safeBottom = decor.height - (ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0)
            }
            val home = compose.onNodeWithTag("top-header-frame").fetchSemanticsNode().boundsInRoot
            val calendar = compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot
            for (tab in listOf(0, 1, 2, 3, 5)) {
                compose.onNodeWithTag("nav-$tab").performClick().assertIsSelected().assertHeightIsAtLeast(48.dp)
                val frame = compose.onNodeWithTag("top-header-frame").fetchSemanticsNode().boundsInRoot
                assertEquals(home.left, frame.left, 1f); assertEquals(home.right, frame.right, 1f)
                assertEquals(home.top, frame.top, 1f)
                val bar = compose.onNodeWithTag("bottom-bar").fetchSemanticsNode().boundsInRoot
                assertTrue("Navigation overlaps Android controls", bar.bottom <= safeBottom + 1f)
                readable(compose.onNodeWithTag("nav-label-$tab", useUnmergedTree = true), 12f)
                if (tab == 1) assertEquals(calendar, compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot)
                if (tab in listOf(2, 3)) {
                    val title = if (tab == 2) "Cucina" else "Progressi"
                    val count = if (tab == 2) 3 else 4
                    for (index in 0 until count) {
                        compose.onNodeWithTag("banner-tab-$title-$index").performScrollTo().performClick().assertIsSelected()
                        val labelTag = if (tab == 2) "kitchen-tab-label-${listOf(2, 0, 1)[index]}" else "banner-label-$title-$index"
                        readable(compose.onNodeWithTag(labelTag, useUnmergedTree = true), 12f)
                    }
                    compose.onNodeWithTag("banner-tab-$title-0").performScrollTo().performClick()
                }
                shot("tab-$tab")
            }
        }
    }
    @Test fun dietPeriodChoicesKeepWholeWordsAndReadableType() {
        prepare()
        ui { Store.generatePlan(LocalDate.now().with(java.time.DayOfWeek.MONDAY), 7); Nav.tab = 1 }
        ActivityScenario.launch(MainActivity::class.java).use {
            if (compose.onAllNodesWithTag("plan-diet-style").fetchSemanticsNodes().isEmpty())
                compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick()
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            for (period in listOf(1, 7, 14, 30)) {
                val label = compose.onNodeWithTag("diet-period-$period-label", useUnmergedTree = true).performScrollTo()
                readable(label, 14f)
                val layouts = mutableListOf<TextLayoutResult>()
                label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                layouts.forEach { layout ->
                    assertTrue("Period label becomes a narrow column", layout.lineCount <= 2)
                    val text = layout.layoutInput.text.text
                    for (line in 0 until layout.lineCount - 1) {
                        val end = layout.getLineEnd(line, visibleEnd = true)
                        val next = layout.getLineStart(line + 1)
                        val splitsWord = end == next && text.getOrNull(end - 1)?.isLetterOrDigit() == true && text.getOrNull(next)?.isLetterOrDigit() == true
                        assertFalse("Period word split across lines: $text", splitsWord)
                    }
                }
            }
            shot("diet-period-choices")
            compose.onNodeWithText("Annulla").performClick()
        }
    }
    @Test fun chatComposerRemainsAboveKeyboardWithoutSending() {
        prepare()
        ui { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1, apiKey = "layout-test-no-network")) }; Nav.tab = 5 }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithTag("chat-input").performClick().performTextInput("Prova layout senza invio")
            var safeBottom = 0
            var keyboard = 0
            compose.waitUntil(20_000) {
                scenario.onActivity { activity ->
                    val decor = activity.window.decorView
                    keyboard = ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                    safeBottom = decor.height - keyboard
                }
                keyboard > 0 && compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().isEmpty()
            }
            val input = compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot
            assertTrue("Chat input overlaps keyboard", input.bottom <= safeBottom + 1f)
            compose.onNodeWithContentDescription("Invia messaggio").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            assertTrue("Layout check must never send a message", Store.current.chat.isEmpty())
            shot("chat-keyboard")
            ins.uiAutomation.executeShellCommand("input keyevent 4").close()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().isNotEmpty() }
            ui { Store.update { it.copy(ai = it.ai.copy(apiKey = "")) } }
        }
    }
}
