package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
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
class TodayRefinementTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        check(com.jarvis.food.engine.RecipeCats.get(Db.db).isNotEmpty())
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day
        }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(300)
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/today-$profile").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
    private fun fill(): Float = compose.onNodeWithTag("water-bottle", useUnmergedTree = true)
        .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
    private fun bluePixels(): Int {
        val pixels = compose.onNodeWithTag("water-bottle", useUnmergedTree = true).captureToImage().toPixelMap()
        return (0 until pixels.height).sumOf { y -> (0 until pixels.width).count { x ->
            val c = pixels[x, y]
            c.blue > 0.6f && c.blue - c.red > 0.25f && c.green > 0.35f
        } }
    }
    private fun readable(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { l ->
            assertFalse("Missing text: ${l.layoutInput.text}", l.multiParagraph.didExceedMaxLines)
            assertTrue("Clipped height: ${l.layoutInput.text}", l.multiParagraph.height <= l.size.height + 1f)
            for (line in 0 until l.lineCount) {
                assertFalse("Ellipsis: ${l.layoutInput.text}", l.isLineEllipsized(line))
                assertTrue("Clipped width: ${l.layoutInput.text}", l.getLineRight(line) - l.getLineLeft(line) <= l.size.width + 1f)
            }
        }
    }

    @Test fun bottleMatchesSavedWaterInBothDirectionsAcrossDatesAndRecreation() {
        prepare()
        ui { Store.update { it.copy(goals = it.goals.copy(waterMl = 2000), glassMl = 250) } }
        val before = Store.current
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithTag("water-minus").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("water-bottle", useUnmergedTree = true).performScrollTo()
            val emptyBlue = bluePixels()
            assertEquals(0f, fill(), 0.001f)
            shot("water-empty")
            repeat(4) { compose.onNodeWithTag("water-plus").performScrollTo().performClick(); compose.waitForIdle() }
            assertEquals(1000, Store.current.water[day.toString()])
            assertEquals(0.5f, fill(), 0.001f)
            compose.onNodeWithTag("water-amount", useUnmergedTree = true).assertTextEquals("1,0 L")
            assertTrue("Bottle must visibly fill, not just change its semantics", bluePixels() > emptyBlue + 100)
            shot("water-half")
            scenario.recreate()
            compose.onNodeWithTag("water-plus").performScrollTo()
            assertEquals(0.5f, fill(), 0.001f)
            compose.onNodeWithTag("water-plus").performClick()
            compose.onNodeWithTag("water-amount", useUnmergedTree = true).assertTextEquals("1,25 L")
            compose.onNodeWithTag("water-minus").performClick()
            assertEquals(1000, Store.current.water[day.toString()])
            ui { Store.setWater(day, 2250) }
            compose.waitForIdle()
            assertEquals(1f, fill(), 0.001f)
            compose.onNodeWithTag("water-amount", useUnmergedTree = true).assertTextEquals("2,25 L")
            shot("water-goal-exceeded")
            ui { Nav.diaryDate = day.minusDays(1) }
            compose.onNodeWithTag("water-plus").performScrollTo().performClick()
            assertEquals(250, Store.current.water[day.minusDays(1).toString()])
            assertEquals(2250, Store.current.water[day.toString()])
            ui { Store.update { it.copy(glassMl = 300) } }
            compose.onNodeWithTag("water-plus").performClick()
            assertEquals(550, Store.current.water[day.minusDays(1).toString()])
            assertEquals(before.diary, Store.current.diary)
            assertEquals(before.plan, Store.current.plan)
            assertEquals(before.weights, Store.current.weights)
        }
    }

    @Test fun quickActionsVitaminsTipsAndActivityKeepExistingDestinations() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("home-quick-actions")
            val destinations = listOf(
                "Aggiungi" to Screen.AddFood(day, defaultMeal()),
                "Foto piatto" to Screen.PhotoAi(day, defaultMeal()),
                "Frigo" to Screen.PantryScan("foto"),
                "Digiuno" to Screen.Fasting
            )
            for ((label, screen) in destinations) {
                compose.onNodeWithTag("quick-icon-$label", useUnmergedTree = true).assertWidthIsEqualTo(46.dp)
                val button = compose.onNode(hasClickAction() and hasAnyDescendant(hasTestTag("quick-icon-$label")), useUnmergedTree = true)
                button.performScrollTo().performClick()
                assertEquals(screen, Nav.stack.last())
                ui { Nav.back() }
            }
            val before = Store.current
            compose.onNodeWithTag("home-vitamins").performScrollTo().performClick()
            assertEquals(Screen.Trend("vitamins"), Nav.stack.last())
            ui { Nav.back() }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Vitamina C", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("home-vitamins").performScrollTo()
            shot("vitamins-and-tips")
            for (name in listOf("Vitamina C", "Vitamina D", "Vitamina B12")) {
                readable(compose.onNodeWithText(name, useUnmergedTree = true).performScrollTo())
            }
            val tips = com.jarvis.food.engine.DailyFoodTips.build(Store.current, day, Db.db)
            compose.onNodeWithTag("daily-food-tip-${tips.first().id}").performScrollTo().performClick()
            compose.onNodeWithTag("daily-tip-source").assertExists()
            compose.onNodeWithText("Chiudi").performClick()
            compose.onNodeWithTag("everyday-food-guide").performScrollTo()
            shot("personal-ideas")
            compose.onNodeWithTag("home-activity").performScrollTo()
            shot("activity-empty")
            compose.onNodeWithText("Modifica passi").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("3285")
            compose.onNodeWithText("Salva").performScrollTo().performClick()
            assertEquals(3285, Store.stepsFor(Store.current, day))
            compose.onNodeWithTag("home-activity").performScrollTo()
            shot("activity-with-steps")
            ui { Store.update { s -> s.copy(energy = s.energy.copy(mode = 2)) } }
            compose.onNodeWithText("Smartwatch").performScrollTo().performClick()
            assertEquals(Screen.Watch, Nav.stack.last())
            assertEquals(before.diary, Store.current.diary)
            assertEquals(before.plan, Store.current.plan)
            assertEquals(before.weights, Store.current.weights)
        }
    }
}
