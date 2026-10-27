package com.jarvis.food

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import org.json.JSONArray
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import android.webkit.WebView
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProfileFormsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val today = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare(s: AppState) {
        Store.init(ins.targetContext)
        ui { Store.update { s }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = today; Nav.planDate = today }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); SystemClock.sleep(450)
        val capture = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/profile-$capture").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun web(v: View): WebView? {
        if (v is WebView && v.tag == "body-model-webview") return v
        if (v is ViewGroup) for (i in 0 until v.childCount) web(v.getChildAt(i))?.let { return it }
        return null
    }
    private fun js(scenario: ActivityScenario<MainActivity>, code: String): String {
        val result = AtomicReference<String>("missing"); val latch = CountDownLatch(1)
        scenario.onActivity { a ->
            val w = web(a.window.decorView)
            if (w == null) latch.countDown() else w.evaluateJavascript(code) { value -> result.set(value); latch.countDown() }
        }
        assertTrue("WebView callback", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }
    private fun hideIme(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { a ->
            val input = a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(a.window.decorView.windowToken, 0); a.currentFocus?.clearFocus()
        }
        compose.waitUntil(5000) {
            var visible = false
            scenario.onActivity { a -> visible = androidx.core.view.ViewCompat.getRootWindowInsets(a.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true }
            !visible
        }
        compose.waitForIdle()
    }
    @Test fun bodyModelRendersPicksZonesRotatesAndPreservesMeasurementHistory() {
        val old = Measurement(today.minusDays(7).toString(), "vita", 84.0)
        val arm = Measurement(today.toString(), "braccio", 32.0)
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), measurements = listOf(old, arm)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ui { Nav.go(Screen.Measurements) }
            try { compose.waitUntil(20000) { js(scenario, "Boolean(window.bodyState && window.bodyState.ready)") == "true" } }
            catch (failure: Throwable) { throw AssertionError("3D renderer: " + js(scenario, "JSON.stringify(window.bodyState)"), failure) }
            assertEquals("true", js(scenario, "window.bodyState.pixel[3]>0 && window.bodyState.glError===0"))
            assertEquals("true", js(scenario, "window.bodyState.triangles > 1000 && !window.bodyState.error"))
            shot("body-3d-waist")
            // Native choice updates the actual renderer, then a rendered-surface pick returns through the JS bridge.
            compose.onNodeWithText("Braccio", substring = false).performScrollTo().performClick()
            compose.waitUntil(5000) { js(scenario, "window.bodyState.selected") == "\"braccio\"" }
            compose.onNodeWithTag("body-model-3d").performScrollTo()
            js(scenario, "window.rotateTo(0)")
            val point = JSONArray(js(scenario, "[window.bodyPoint('collo')[0],window.bodyPoint('collo')[1],document.getElementById('model').clientWidth]"))
            val bounds = compose.onNodeWithTag("body-model-3d").fetchSemanticsNode().boundsInRoot
            val scale = bounds.width / point.getDouble(2)
            compose.onNodeWithTag("body-model-3d").performTouchInput {
                click(Offset((point.getDouble(0)*scale).toFloat(), (point.getDouble(1)*scale).toFloat()))
            }
            try { compose.waitUntil(5000) { js(scenario, "window.bodyState.selected") == "\"collo\"" } }
            catch (failure: Throwable) { throw AssertionError("3D touch: " + js(scenario, "JSON.stringify(window.bodyState)"), failure) }
            compose.waitForIdle()
            compose.onNodeWithText("Dove misurare · Collo").performScrollTo().assertIsDisplayed()
            assertEquals("true", js(scenario, "window.rotateTo(Math.PI);Math.abs(window.bodyState.angle-Math.PI)<0.01"))
            compose.onNodeWithText("Vita", substring = false).performScrollTo().performClick()
            js(scenario, "window.rotateTo(0)")
            compose.onNodeWithTag("measurement-value").performScrollTo().performTextReplacement("82,5")
            hideIme(scenario)
            compose.onNodeWithText("Salva misura").performClick()
            compose.waitUntil(5000) { Store.current.measurements.size == 3 }
            assertTrue(Store.current.measurements.contains(old)); assertTrue(Store.current.measurements.contains(arm))
            compose.onNodeWithTag("measurement-history-$today").performScrollTo().assertIsDisplayed()
            shot("measurements-history")
            compose.onNodeWithContentDescription("Modifica misura del $today").performClick()
            compose.onNodeWithTag("measurement-value").performScrollTo().performTextReplacement("81,5")
            hideIme(scenario)
            compose.onNodeWithText("Aggiorna misura").performClick()
            compose.waitUntil(5000) { Store.current.measurements.any { it.type == "vita" && it.date == today.toString() && it.value == 81.5 } }
            assertEquals(3, Store.current.measurements.size)
            val backup = Store.exportJson(); assertTrue(Store.importJson(backup).isSuccess)
            assertEquals(3, Store.current.measurements.size)
            compose.onNodeWithText("Massa grassa", substring = false).performScrollTo().performClick()
            compose.onNodeWithTag("measurement-value").performScrollTo().assertTextContains("Massa grassa (%)")
            assertEquals("\"grasso\"", js(scenario, "window.bodyState.selected"))
        }
    }
    @Test fun organizedGoalsKeepEditingAndSaveEveryValue() {
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), goals = Goals(kcal = 2131, auto = false)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ui { Nav.go(Screen.Goals) }
            compose.onNodeWithTag("goals-summary").assertIsDisplayed()
            shot("goals-overview")
            compose.onNodeWithTag("goals-calories").performScrollTo().performTextReplacement("2300")
            hideIme(scenario)
            compose.onNodeWithTag("goal-pace-0").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText("Quota di grassi").performScrollTo()
            shot("goals-macros")
            compose.onNodeWithContentDescription("Aumenta Obiettivo acqua").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Aumenta Passi al giorno").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Aumenta Bicchiere").performScrollTo().performClick()
            shot("goals-daily-habits")
            val before = Store.current
            compose.onNodeWithText("Salva obiettivi").performClick()
            compose.waitForIdle()
            assertEquals(2300, Store.current.goals.kcal)
            assertEquals(1, Store.current.profile.goal)
            assertEquals(before.goals.waterMl + 250, Store.current.goals.waterMl)
            assertEquals(before.goals.steps + 500, Store.current.goals.steps)
            assertEquals(before.glassMl + 50, Store.current.glassMl)
            assertEquals(100, Store.current.goals.carbsPct + Store.current.goals.proteinPct + Store.current.goals.fatPct)
            ui { Nav.go(Screen.ProfileEdit) }; shot("profile-details")
            ui { Nav.back(); Nav.go(Screen.Questionnaire(QStep.About)) }; shot("profile-questionnaire")
            ui { Nav.back(); Nav.go(Screen.Weight) }; shot("profile-weight")
            ui { Nav.back(); Nav.go(Screen.RemindersSet) }; shot("profile-reminders")
            ui { Nav.back(); Nav.go(Screen.Backup) }; shot("profile-backup")
        }
    }
}
