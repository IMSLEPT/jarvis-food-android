package com.jarvis.food

import android.graphics.BitmapFactory
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Store
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PhotoAiFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()

    @Test fun offlineChoiceSurvivesReopeningWithAConfiguredOnlineKey() {
        val context = ins.targetContext
        Store.init(context)
        val preferences = context.getSharedPreferences("photo_ai", android.content.Context.MODE_PRIVATE)
        preferences.edit().putBoolean("online", true).commit()
        ins.runOnMainSync {
            Store.update { AppState(onboarded = true).let { it.copy(ai = it.ai.copy(apiKey = "test-key-no-network")) } }
            Nav.clear(); Nav.go(Screen.PhotoAi(LocalDate.now(), 0))
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.onNodeWithTag("photo-mode-online").assertIsSelected()
                compose.onNodeWithTag("photo-mode-offline").performClick().assertIsSelected()
                ins.runOnMainSync { Nav.back() }
                compose.waitForIdle()
                ins.runOnMainSync { Nav.go(Screen.PhotoAi(LocalDate.now(), 0)) }
                compose.onNodeWithTag("photo-mode-offline").assertIsSelected()
                compose.onNodeWithTag("photo-mode-online").assertIsNotSelected()
                assertFalse(preferences.getBoolean("online", true))
            }
        } finally {
            preferences.edit().clear().commit()
            ins.runOnMainSync { Store.update { AppState(onboarded = true) }; Nav.clear() }
        }
    }

    @Test fun bundledClassifierActuallyRecognizesAnImageLocally() = runBlocking {
        val context = ins.targetContext
        assertTrue("Offline food model must be packaged in the APK", FoodAi.offlineAvailable(context))
        val bitmap = context.assets.open("photography/pasta_salmone_zucchine.webp").use { BitmapFactory.decodeStream(it) }
        try {
            val started = System.nanoTime()
            val labels = FoodAi.analyzeOffline(context, bitmap)
            println("Offline photo analysis: ${(System.nanoTime() - started) / 1_000_000} ms; labels=${labels.map { it.label }}")
            assertTrue("Classifier returned no food labels", labels.isNotEmpty())
            assertTrue(labels.all { it.label.isNotBlank() && it.score.isFinite() && it.score in 0f..1f })
        } finally { bitmap.recycle() }
    }
}
