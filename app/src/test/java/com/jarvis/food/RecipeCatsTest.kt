package com.jarvis.food

import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.RecipeCats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RecipeCatsTest {
    private val db: FoodDb = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())

    @Test fun categorieYazioConEmojiEConteggiReali() {
        val groups = RecipeCats.get(db)
        val ids = groups.map { it.id }
        assertTrue("manca per pasto", "pasto" in ids)
        assertTrue("manca per calorie", "kcal" in ids)
        for (g in groups) for (c in g.cats) {
            assertTrue("${g.id}/${c.label} senza emoji", c.emoji.isNotBlank())
            assertTrue("${g.id}/${c.label} vuota ma mostrata", c.count > 0)
            assertTrue("${c.label} troppo lunga per la tessera", c.label.length <= 26)
        }
    }

    @Test fun sotto100kcalSiTrovaSubito() {
        val kcal = RecipeCats.get(db).first { it.id == "kcal" }
        val first = kcal.cats.first()
        assertEquals("Sotto 100 kcal", first.label)
        assertTrue("nessuna ricetta sotto 100 kcal", first.count > 0)
        assertEquals("Oltre 700 kcal", kcal.cats.last().label)
    }

    @Test fun lefasceDiCalorieCopronoTutteLeRicette() {
        val kcal = RecipeCats.get(db).first { it.id == "kcal" }
        assertEquals(db.recipes.size, kcal.cats.sumOf { it.count })
    }

    @Test fun ilRisultatoResta_inMemoria() {
        RecipeCats.get(db)
        assertNotNull(RecipeCats.peek(db))
    }
}
