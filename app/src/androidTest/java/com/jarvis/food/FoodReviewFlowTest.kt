package com.jarvis.food

import android.graphics.Bitmap
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

class FoodReviewFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val ins=InstrumentationRegistry.getInstrumentation()
    private val date=LocalDate.now()
    private fun ui(block:()->Unit)=ins.runOnMainSync(block)
    private fun prepare() { Store.init(ins.targetContext);ui { Store.update { AppState(onboarded=true) };Nav.clear();Nav.tab=0 } }
    private fun shot(name:String) {
        compose.waitForIdle();ins.waitForIdleSync();android.os.SystemClock.sleep(400)
        val profile=InstrumentationRegistry.getArguments().getString("captureName")?:"standard"
        val file=File(ins.targetContext.filesDir,"shots/food-review-$profile/$name.png").apply { parentFile!!.mkdirs() }
        val bitmap=ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() }
    }
    @Test fun ratingStrengthsConcernsAndAlternativesDoNotEditTheDiary() {
        prepare()
        val food=Food("off:review-fixture","Cioccolato al latte",kcal=536.0,protein=7.0,carbs=59.0,fat=30.0,sugar=59.0,satFat=19.0,salt=.2,fiber=3.0,source="off",
            productFacts=ProductFacts(nutriScore="e",nutriScoreVersion="2023",ingredients="Zucchero, latte, cacao 30%",additivesCount=2,additives=listOf("en:e322","en:e476")))
        val id=Store.addEntry(date,0,food,15.0)
        val before=Store.current.diary
        ui { Nav.go(Screen.FoodDetail(food,date,0,15.0,before.first { it.id==id })) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("food-guidance").performScrollTo()
            compose.onNodeWithTag("food-review-rating").assertTextContains("Nutri-Score E")
            compose.onNodeWithText("Punti positivi").assertExists()
            compose.onNodeWithText("Da considerare").assertExists()
            compose.onNodeWithText("Additivi").assertExists()
            shot("label-summary")
            compose.onNodeWithTag("food-guidance-alternatives").performScrollTo().performClick()
            compose.onNodeWithText("Confronta prodotti e marche").performScrollTo().assertIsDisplayed()
            assertEquals(before,Store.current.diary)
            compose.onNodeWithTag("food-review-criteria").performScrollTo().performClick()
            compose.onNodeWithText("Zucchero, latte, cacao 30%").performScrollTo().assertIsDisplayed()
            shot("ingredients-and-criteria")
            compose.onNodeWithText("Chiudi").performClick()
            assertEquals(before,Store.current.diary)
        }
    }
    @Test fun missingDataAndKnownFavourableGradeRemainDistinct() {
        prepare()
        val unknown=Food("custom:unknown","Etichetta parziale",kcal=100.0,protein=2.0,carbs=15.0,fat=3.0,source="custom")
        ui { Nav.go(Screen.FoodDetail(unknown,date,0)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("food-review-rating").performScrollTo().assertTextContains("Dati incompleti")
            compose.onAllNodesWithText("Additivi").assertCountEquals(0)
            shot("missing-data")
            val yogurt=Food("off:yogurt-fixture","Yogurt bianco",kcal=65.0,protein=11.0,carbs=4.0,fat=.2,sugar=4.0,satFat=.1,salt=.1,source="off",productFacts=ProductFacts(nutriScore="a"))
            ui { Nav.go(Screen.FoodDetail(yogurt,date,0)) }
            compose.onNodeWithTag("food-review-rating").performScrollTo().assertTextContains("Nutri-Score A")
            compose.onNodeWithText("Ricco di proteine").assertExists()
            shot("favourable-profile")
            assertTrue(Store.current.diary.isEmpty())
        }
    }
}
