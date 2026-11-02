package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.FoodAmounts
import com.jarvis.food.ui.fastingClock
import com.jarvis.food.ui.fastingElapsed
import org.junit.Assert.*
import org.junit.Test

class VideoCorrectionsTest {
    private fun food(name: String, brand: String = "", liquid: Boolean = false) = Food("test", name, brand, 387.0, 14.0, 60.0, 7.4, liquid = liquid)

    @Test fun broadPlantCategoryDoesNotTurnSolidsIntoDrinks() {
        for (name in listOf("Fette biscottate integrali", "Gallette di mais", "Riso", "Pane integrale", "Mandorle")) {
            assertFalse(name, FoodAmounts.liquid(name, listOf("en:plant-based-foods-and-beverages"), "315 g"))
        }
        assertTrue(FoodAmounts.liquid("Latte di riso", listOf("en:plant-based-foods-and-beverages", "en:plant-milks"), "1 l"))
        assertTrue(FoodAmounts.liquid("Acqua minerale", quantity = "500ml"))
        assertFalse(FoodAmounts.liquid("Latte in polvere", quantity = "400 g"))
        assertFalse(FoodAmounts.liquid("Cioccolato al latte", quantity = "100 g"))
    }

    @Test fun exactProductGetsVerifiedSliceButOtherBrandsNeverInheritIt() {
        val exact = FoodAmounts.normalize(food("Fette biscottate integrali", "Mulino Bianco", true))
        assertFalse(exact.liquid)
        assertEquals(8.8, exact.servingGrams!!, 0.0001)
        assertEquals("4 fette", FoodAmounts.pieces(exact, 4))
        assertEquals("https://www.mulinobianco.it/le-integrali", exact.portionUrl)
        assertNull(FoodAmounts.normalize(food("Fette biscottate integrali", "Altra marca")).servingGrams)
        assertNull(FoodAmounts.normalize(food("Mini fette biscottate integrali", "Mulino Bianco")).servingGrams)
        assertEquals(387.0, exact.kcal, 0.0)
    }

    @Test fun multiPieceLabelDividesItsOwnPortionAndManualWeightWins() {
        val f = food("Gallette di riso").copy(servingName = "porzione (4 gallette (28 g))", servingGrams = 28.0)
        val portion = FoodAmounts.normalize(f)
        assertEquals(7.0, portion.servingGrams!!, 0.0)
        assertEquals("galletta", portion.servingName)
        val manual = food("Fette biscottate integrali", "Mulino Bianco").copy(servingName = "fetta", servingGrams = 9.0, portionSource = "Inserito da te")
        assertEquals(9.0, FoodAmounts.normalize(manual).servingGrams!!, 0.0)
        assertNull(FoodAmounts.normalize(food("Pane artigianale")).servingGrams)
    }

    @Test fun upgradeOnlyRepairsMetadataAndKeepsAllDiaryFacts() {
        val old = food("Fette biscottate integrali", "Mulino Bianco", true)
        val entry = DiaryEntry("entry", "2026-11-02", 0, old, 60.0, 12345L, "2026-11-02:0")
        val state = AppState(diary = listOf(entry), favorites = listOf(old), customFoods = listOf(old))
        val fixed = FoodAmounts.migrate(state)
        assertEquals(entry.copy(food = fixed.diary.single().food), fixed.diary.single())
        assertFalse(fixed.diary.single().food.liquid)
        assertEquals(Nutrition.of(old, 60.0), Nutrition.of(fixed.diary.single().food, 60.0))
        assertFalse(fixed.favorites.single().liquid)
        assertEquals(fixed, FoodAmounts.migrate(fixed))
    }

    @Test fun fastingClockMovesInSecondsAndSurvivesReopening() {
        assertEquals("00:00:00", fastingClock(fastingElapsed(1000, 1000)))
        assertEquals("00:00:01", fastingClock(fastingElapsed(2000, 1000)))
        assertEquals("16:03:07", fastingClock(fastingElapsed(1000 + 16 * 3600000L + 187000, 1000)))
        assertEquals(0L, fastingElapsed(900, 1000))
        assertEquals(0L, fastingElapsed(900, null))
    }
}
