package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class VideoNovemberFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        ui { Store.update { AppState(onboarded = true) }; Nav.clear(); Nav.tab = 3 }
    }
    private fun shot(name: String) {
        compose.waitForIdle()
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/november-$profile").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }
        finally { bitmap.recycle() }
    }
    @Test fun profileHeaderAndChatHistoryKeepSavedConversations() {
        prepare()
        ui { Store.update { it.copy(chat = listOf(ChatMsg("first", "user", "Conversazione da conservare", 1000L))) } }
        ActivityScenario.launch(MainActivity::class.java).use {
            val profileBefore = compose.onNodeWithContentDescription("Apri profilo").fetchSemanticsNode().boundsInRoot
            ui { Nav.tab = 5 }
            val profileAfter = compose.onNodeWithContentDescription("Apri profilo").fetchSemanticsNode().boundsInRoot
            assertEquals(profileBefore.top, profileAfter.top, 1f)
            assertEquals(profileBefore.width, profileAfter.width, 1f)
            compose.onNodeWithText("Conversazione da conservare").assertExists()
            compose.onNodeWithContentDescription("Nuova chat").performClick()
            assertTrue(Store.current.chat.isEmpty())
            compose.onNodeWithContentDescription("Cronologia delle chat").performClick()
            compose.onNodeWithTag("chat-history-screen").assertIsDisplayed()
            compose.onNodeWithTag("chat-history-first").performClick()
            compose.onNodeWithText("Conversazione da conservare").assertExists()
            assertEquals("first", Store.current.chat.single().id)
            shot("chat-profile-and-history")
            compose.onNodeWithContentDescription("Apri profilo").performClick()
            compose.onNodeWithText("Profilo e obiettivo").assertExists()
        }
    }
    @Test fun chartBarsKeepTheirSemanticColoursWhenADayIsSelected() {
        prepare()
        val today = LocalDate.now()
        ui { Store.update { state -> state.copy(
            water = (0..6).associate { today.minusDays(it.toLong()).toString() to 1500 },
            steps = (0..6).associate { today.minusDays(it.toLong()).toString() to 5000 },
            diary = listOf(DiaryEntry("macro",today.toString(),0,Food("food","Test",kcal=200.0,protein=20.0,carbs=30.0,fat=5.0),100.0,0L))) } }
        ActivityScenario.launch(MainActivity::class.java).use {
            for (kind in listOf("water", "activity", "macro")) {
                ui { Nav.clear(); Nav.go(Screen.Trend(kind)) }
                compose.onNodeWithTag("trend-chart").performScrollTo().assertIsDisplayed()
                fun checkColour() {
                    val pixels = compose.onNodeWithTag("trend-chart").captureToImage().toPixelMap()
                    val expected = TrendPalette.ink(kind).toArgb()
                    var found = 0
                    for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
                        val pixel = pixels[x,y].toArgb()
                        // Water uses a gradient; accept its bright blue/cyan channel range.
                        if (kind == "water") {
                            val red = android.graphics.Color.red(pixel); val green = android.graphics.Color.green(pixel); val blue = android.graphics.Color.blue(pixel)
                            if (red < 80 && green > 120 && blue > 190) found++
                        } else if (pixel == expected) found++
                    }
                    assertTrue("$kind chart lost its own palette", found > 50)
                }
                checkColour()
                compose.onNodeWithTag("trend-day-2").performClick()
                checkColour()
                shot("trend-$kind-selected")
            }
        }
    }
    @Test fun nutrientsAndPhotoContentStayAboveAndroidButtons() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var safeBottom = 0
            scenario.onActivity { activity ->
                val bottom = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)!!
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
                safeBottom = activity.window.decorView.height - bottom
            }
            ui { Nav.go(Screen.DayNutrients(LocalDate.now())) }
            compose.onNodeWithTag("day-nutrients-meals").performScrollTo()
            compose.onNodeWithText("Spuntino serale").assertIsDisplayed()
            val bottom = compose.onNodeWithTag("day-nutrients-meals").fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("Nutrient card overlaps navigation", bottom < safeBottom - 8)
            shot("nutrient-footer")
            ui { Nav.clear(); Nav.go(Screen.PhotoAi(LocalDate.now(),0)) }
            compose.onNodeWithTag("photo-mode-offline").performScrollTo().assertIsDisplayed()
            assertTrue(compose.onNodeWithTag("photo-mode-offline").fetchSemanticsNode().boundsInRoot.bottom < safeBottom)
            shot("photo-footer")
        }
    }
}
