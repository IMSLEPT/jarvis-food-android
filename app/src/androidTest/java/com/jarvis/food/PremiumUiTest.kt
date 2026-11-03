package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
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

/** Behavioural checks for the approved refinement, using existing callbacks and data. */
@RunWith(AndroidJUnit4::class)
class PremiumUiTest {
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
        compose.waitForIdle(); ins.waitForIdleSync()
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/premium-$profile").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }

    @Test fun oneComposerRetainsDraftWhenTogglingModelWithoutChangingData() {
        prepare()
        ui { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1, apiKey = "layout-test-no-network")) }; Nav.tab = 5 }
        val before = Store.current
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
            compose.onNodeWithContentDescription("Nuova chat").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            compose.onNodeWithTag("chat-input").performTextReplacement("Pasta per stasera")
            scenario.onActivity { activity ->
                val keyboard = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                keyboard.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Motore della chat").performClick()
            compose.onNodeWithContentDescription("Chiudi impostazioni motore").performClick()
            compose.onNodeWithTag("chat-input").assertTextContains("Pasta per stasera")
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
            shot("jarvis-single-composer")
            compose.onNodeWithContentDescription("Cronologia delle chat").performClick()
            assertEquals(Screen.ChatHistory, Nav.stack.last())
            assertEquals(before.diary, Store.current.diary)
            assertEquals(before.weights, Store.current.weights)
            assertEquals(before.plan, Store.current.plan)
        }
    }

    @Test fun progressExplainsPartialLogsAndKeepsMeasurementsReadOnly() {
        prepare()
        val food = Db.db.food("pasta")!!.toFood()
        ui {
            Store.update { it.copy(diary = listOf(DiaryEntry("premium-log", day.toString(), 1, food, 90.0, 0L))) }
            Nav.tab = 3
        }
        val before = Store.current
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("progress-calorie-coverage", useUnmergedTree = true).performScrollTo().assertTextContains("1 giorno registrato su 7")
            compose.onNodeWithTag("progress-weight-summary").performScrollTo().assertTextContains("Ultimo peso disponibile", substring = true)
            shot("progress-context")
            compose.onNodeWithTag("banner-tab-Progressi-1").performClick()
            compose.onNodeWithTag("weight-empty", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("progress-register-weight").performScrollTo().assertHasClickAction()
            assertEquals(before.weights, Store.current.weights)
            assertEquals(before.diary, Store.current.diary)
            compose.onNodeWithTag("progress-register-weight").performClick()
            assertEquals(Screen.Weight, Nav.stack.last())
            assertEquals(before.weights, Store.current.weights)
            assertEquals(before.diary, Store.current.diary)
        }
    }
}
