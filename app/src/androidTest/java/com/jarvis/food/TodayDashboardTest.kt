package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
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
class TodayDashboardTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), goals = Goals(auto = false, kcal = 2150)) }
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day
        }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync()
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/dashboard-$profile").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } finally { bitmap.recycle() }
    }
    private fun verifyValues() {
        compose.waitForIdle()
        val s = Store.current
        val totals = Nutrition.sum(Store.entriesFor(s, Nav.diaryDate))
        val energy = Store.energyFor(s, Nav.diaryDate)
        compose.onNodeWithTag("calories-remaining", true).assertTextEquals(kcalFmt(kotlin.math.abs(energy.targetKcal - totals.kcal)))
        compose.onNodeWithTag("calories-Mangiate", true).assertTextEquals("${kcalFmt(totals.kcal)} kcal")
        compose.onNodeWithTag("calories-Obiettivo", true).assertTextEquals("${kcalFmt(energy.targetKcal)} kcal")
        compose.onNodeWithTag("calories-Attività", true).assertTextEquals("${kcalFmt(Store.burnedFor(s, Nav.diaryDate).toDouble())} kcal")
        listOf(Triple("Proteine", totals.protein, energy.protein), Triple("Carboidrati", totals.carbs, energy.carbs), Triple("Grassi", totals.fat, energy.fat)).forEach { (label, value, goal) ->
            compose.onNodeWithTag("macro-value-$label", true).performScrollTo().assertTextEquals("${value.r0()} g / ${goal.r0()} g")
            val range = compose.onNodeWithTag("macro-ring-$label", true).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
            assertEquals(TodayProgress.ratio(value, goal).coerceIn(0f, 1f), range.current, 0.001f)
        }
    }
    private fun readable(tag: String) {
        val node = compose.onNodeWithTag(tag, true).performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { result ->
            assertFalse("Missing text ${result.layoutInput.text}", result.multiParagraph.didExceedMaxLines)
            assertTrue("Clipped height ${result.layoutInput.text}: ${result.multiParagraph.height} > ${result.size.height}", result.multiParagraph.height <= result.size.height + 1f)
            for (line in 0 until result.lineCount) {
                assertFalse("Ellipsis ${result.layoutInput.text}", result.isLineEllipsized(line))
                assertTrue("Clipped width ${result.layoutInput.text}", result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1f)
            }
        }
    }
    @Test fun dataChangesDatesOverflowAndRecreationStayLive() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            verifyValues()
            ui { Store.addEntry(day, 0, Db.db.food("pasta")!!.toFood(), 85.0) }
            verifyValues()
            ui { Store.update { it.copy(goals = it.goals.copy(kcal = 2350)) }; Store.setSteps(day, 2000) }
            verifyValues()
            compose.onNodeWithTag("main-scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -10000f) }
            shot("home")
            for (tag in listOf("calories-remaining", "calories-Mangiate", "calories-Obiettivo", "calories-Attività", "macro-value-Proteine", "macro-value-Carboidrati", "macro-value-Grassi")) readable(tag)
            val before = Store.current
            scenario.recreate(); verifyValues()
            ui { Store.addEntry(day, 1, Db.db.food("pasta")!!.toFood(), 750.0) }
            verifyValues()
            compose.onNodeWithTag("today-calories").performScrollTo(); shot("over-target")
            ui { Nav.diaryDate = day.minusDays(1) }; verifyValues()
            assertEquals(before.plan, Store.current.plan)
            assertEquals(before.weights, Store.current.weights)
        }
    }
    @Test fun calendarAndFiveDestinationsKeepTheirActions() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("week-next").performClick(); compose.waitForIdle()
            assertEquals(day.plusWeeks(1), Nav.diaryDate)
            compose.onNodeWithTag("week-previous").performClick(); compose.waitForIdle()
            assertEquals(day, Nav.diaryDate)
            compose.onNodeWithTag("week-day-0").performClick(); compose.waitForIdle()
            assertEquals(day.minusDays(2), Nav.diaryDate)
            compose.onNodeWithTag("today-calories").performScrollTo().performClick()
            assertEquals(Screen.DayNutrients(day.minusDays(2)), Nav.stack.last())
            ui { Nav.back() }
            for (tab in listOf(1, 2, 3, 5, 0)) {
                compose.onNodeWithTag("nav-$tab").performClick(); compose.waitForIdle()
                assertEquals(tab, Nav.tab)
                compose.onNodeWithTag("nav-$tab").assertIsSelected()
            }
            shot("navigation")
        }
    }
}
