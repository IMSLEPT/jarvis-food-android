package com.jarvis.food

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.io.File

@RunWith(AndroidJUnit4::class)
class NutritionAdvisorFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val ins=InstrumentationRegistry.getInstrumentation()
    private fun ui(block:()->Unit)=ins.runOnMainSync(block)
    private fun shot(name:String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val file=File(ins.targetContext.filesDir,"shots/nutrition/$name.png");file.parentFile!!.mkdirs()
        val b=ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { b.recycle() }
    }
    @Test fun offlineIllustrationsAndFallbackHaveValidAssets() {
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        ins.targetContext.assets.open(FoodArtCatalog.ASSET).use { BitmapFactory.decodeStream(it,null,options) }
        assertTrue(options.outWidth/6>=180 && options.outHeight/6>=180)
        Store.init(ins.targetContext)
        ui { Store.update { AppState(onboarded=true) }; Nav.clear(); Nav.go(Screen.FoodDetail(Food("unknown","Alimento non riconosciuto",kcal=100.0,protein=3.0,carbs=15.0,fat=3.0),LocalDate.now(),0)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("food-art-fallback").assertIsDisplayed()
            shot("fallback")
            ui { Nav.clear(); Nav.go(Screen.FoodDetail(Db.db.food("trota")!!.toFood(),LocalDate.now(),1)) }
            compose.waitUntil(10000) { compose.onAllNodesWithTag("food-art-0").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("food-art-0").assertIsDisplayed()
            shot("trout")
        }
    }
    @Test fun incompleteDiaryDoesNotInventDeficiencyAndSourcesOpenAsDetails() {
        Store.init(ins.targetContext)
        val date=LocalDate.now()
        val f=Food("custom:breakfast","Yogurt greco",kcal=200.0,protein=15.0,carbs=20.0,fat=6.0,sugar=15.0)
        val diary=(1..6).map { DiaryEntry("breakfast-$it",date.minusDays(it.toLong()).toString(),0,f,100.0,0) }
        ui { Store.update { AppState(onboarded=true,diary=diary) }; Nav.clear(); Nav.go(Screen.Micronutrients) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15000) { compose.onAllNodesWithTag("nutrition-coverage").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("nutrition-coverage",useUnmergedTree=true).assertTextContains("0/7 giorni",substring=true)
            compose.onAllNodesWithTag("notice-repetition").assertCountEquals(0)
            compose.onAllNodesWithText("Vitamina D").assertCountEquals(0)
            compose.onAllNodesWithText("Fonti alimentari da esplorare").assertCountEquals(0)
            compose.onNodeWithText("Nessun suggerimento da mostrare").assertExists()
            assertEquals(diary,Store.current.diary)
            shot("no-unnecessary-advice")
            compose.onNodeWithText("30 giorni").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("nutrition-coverage",useUnmergedTree=true).fetchSemanticsNodes().any { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.any { t -> t.text.contains("0/30 giorni") } } }
            shot("thirty-days")
        }
    }
    @Test fun planReplacementRequiresConfirmationAndPreservesDiary() {
        Store.init(ins.targetContext)
        val date=LocalDate.now()
        val f=Food("breakfast", "Yogurt greco",kcal=200.0,protein=15.0,carbs=20.0,fat=6.0,freeSugar=40.0,fiber=30.0,satFat=0.0,salt=0.0)
        val diary=(1..6).map { DiaryEntry("breakfast-$it",date.minusDays(it.toLong()).toString(),0,f,100.0,0) }
        ui {
            Store.update { AppState(onboarded=true,diary=diary,nutritionCompleteDays=diary.map { it.date }) }
            Store.generatePlan(date.with(java.time.DayOfWeek.MONDAY),14)
            Nav.clear(); Nav.go(Screen.Micronutrients)
        }
        val before=Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.recipeId
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15000) { compose.onAllNodesWithTag("notice-free-sugar").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("notice-free-sugar").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Confronta e sostituisci").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Confronta e sostituisci")[0].performScrollTo().performClick()
            compose.onNodeWithText("Annulla").performClick()
            assertEquals(before,Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.recipeId)
            assertEquals(diary,Store.current.diary)
            compose.onAllNodesWithText("Confronta e sostituisci")[0].performScrollTo().performClick()
            shot("replacement-confirmation")
            compose.onNodeWithText("Conferma sostituzione").performClick()
            compose.waitForIdle()
            assertNotEquals(before,Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.recipeId)
            assertEquals(diary,Store.current.diary)
        }
    }

}
