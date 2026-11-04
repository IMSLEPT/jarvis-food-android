package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createEmptyComposeRule
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
import java.time.DayOfWeek

/** Identical fixtures captured before and after; settings are opened without saving or external actions. */
@RunWith(AndroidJUnit4::class)
class PremiumWholeAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val capture get() = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(500)
        val file = File(ins.targetContext.filesDir, "shots/whole-$capture/$name.png").apply { parentFile!!.mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } finally { bitmap.recycle() }
    }
    @Test fun wholeAppMainTabsAndProfileDestinations() {
        Store.init(ins.targetContext)
        val day = LocalDate.now()
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), trainingNutrition = TrainingNutrition(enabled = true, automatic = true)) }
            Store.generatePlan(day.with(DayOfWeek.MONDAY), 7)
            Store.setSteps(day, 3285)
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle(); ins.waitForIdleSync()
            var safeBottom = 0
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                safeBottom = decor.height - (ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0)
            }
            for (tab in listOf(0, 1, 2, 3, 5)) {
                compose.onNodeWithTag("nav-$tab").performClick().assertIsSelected().assertHeightIsAtLeast(48.dp)
                compose.onNodeWithTag("header-profile").assertHeightIsAtLeast(48.dp)
                if (!capture.contains("before") && ins.targetContext.resources.configuration.fontScale <= 1.2f) {
                    val density = ins.targetContext.resources.displayMetrics.density
                    val bounds = compose.onNodeWithTag("top-header-frame").fetchSemanticsNode().boundsInRoot
                    assertTrue("Header should leave room for content", bounds.height / density <= 180f * ins.targetContext.resources.configuration.fontScale)
                }
                shot("main-$tab")
            }
            val destinations = listOf(
                "profile" to Screen.ProfileEdit, "weight" to Screen.Weight,
                "measurements" to Screen.Measurements, "goals" to Screen.Goals,
                "training-goals" to Screen.TrainingGoals, "diet" to Screen.DietSettings,
                "watch" to Screen.Watch, "ai" to Screen.AiSettings,
                "fasting" to Screen.Fasting, "reminders" to Screen.RemindersSet,
                "foods" to Screen.MyFoods, "backup" to Screen.Backup, "about" to Screen.About
            )
            compose.onNodeWithTag("header-profile").performClick()
            shot("profile-top")
            for ((key, route) in destinations) {
                compose.onNodeWithTag("profile-item-$key").performScrollTo().performClick()
                compose.waitForIdle()
                assertEquals("Profile route $key", route, Nav.stack.last())
                shot("settings-$key")
                val scrolls = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
                if (scrolls.fetchSemanticsNodes().isNotEmpty()) {
                    scrolls[0].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
                    shot("settings-$key-bottom")
                }
                if (!capture.contains("before")) {
                    compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
                        val bounds = node.boundsInRoot
                        if (bounds.width > 0f && bounds.height > 0f)
                            assertTrue("$key: interactive content overlaps Android navigation area: $bounds / $safeBottom", bounds.bottom <= safeBottom + 1f)
                    }
                }
                ui { Nav.back() }; compose.waitForIdle()
                assertEquals(Screen.Profile, Nav.stack.last())
            }
            compose.onNodeWithTag("profile-item-about").performScrollTo()
            shot("profile-tools")
        }
    }
    @Test fun longNamesKeepTodayAndPlanCalendarsAligned() {
        Store.init(ins.targetContext)
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro Maria Calamai", detailed = true)) }
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = LocalDate.now(); Nav.planDate = LocalDate.now()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val first = compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot
            if (!capture.contains("before")) {
                compose.onNodeWithTag("nav-1").performClick(); compose.waitForIdle()
                val second = compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot
                assertEquals("Long names must not move the shared calendar", first.top, second.top, 1f)
                assertEquals(first.bottom, second.bottom, 1f)
            }
            shot("long-name-header")
        }
    }

}
