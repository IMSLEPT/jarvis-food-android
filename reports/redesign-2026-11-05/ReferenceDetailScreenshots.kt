package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.ui.Nav
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ReferenceDetailScreenshots {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()

    @Test fun capturesVitaminAndCaloriePanels() {
        val day = LocalDate.now()
        Store.init(ins.targetContext)
        ins.runOnMainSync {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }
            val food = requireNotNull(Db.db.food("pasta")).toFood()
            Store.addEntry(day.minusDays(2), 1, food, 300.0)
            Store.addEntry(day.minusDays(3), 1, food, 250.0)
            Nav.clear()
            Nav.tab = 0
            Nav.diaryDate = day
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Vitamina C", useUnmergedTree = true).performScrollTo()
            compose.onNodeWithText("Vitamina B12", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            shot("04-vitamine")
            compose.onNodeWithTag("nav-3").performClick()
            compose.onNodeWithTag("banner-tab-Progressi-1").performScrollTo().performClick()
            compose.onNodeWithText("Trattino: nessun pasto registrato. I giorni senza dati non sono zero calorie.",
                useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            shot("24c-progressi-calorie")
        }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        ins.waitForIdleSync()
        android.os.SystemClock.sleep(650)
        val image = ins.uiAutomation.takeScreenshot()
        val destination = File(ins.targetContext.filesDir, "$name.png")
        try {
            destination.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            image.recycle()
        }
    }
}
