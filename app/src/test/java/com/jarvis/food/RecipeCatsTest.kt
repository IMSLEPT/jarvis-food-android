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

    @Test fun categoriePerTempoSonoCumulativeERealistiche() {
        val tempo = RecipeCats.get(db).first { it.id == "tempo" }
        assertEquals(listOf(10, 15, 20, 30, 45, 60), tempo.cats.map { it.filter.maxMinutes })
        val counts = tempo.cats.map { it.count }
        assertEquals("le fasce devono essere cumulative (entro 20 comprende entro 10)", counts, counts.sorted())
        for (c in tempo.cats) assertEquals(db.recipes.count { it.minutes <= c.filter.maxMinutes!! }, c.count)
        // ricette con lunghe attese non possono comparire tra quelle rapide
        val lunghe = db.recipes.filter { it.id == "overnight_oats" || it.id == "chia_pudding_mango" }
        assertTrue(lunghe.isNotEmpty() && lunghe.all { it.minutes > 60 })
    }

    @Test fun iMinutiNonSottostimanoLeAttesaScrittePassaggi() {
        val dur = Regex("(\\d+)\\s*(?:-|–|/)?\\s*(\\d+)?\\s*(minut\\w*|min\\b)", RegexOption.IGNORE_CASE)
        val par = Regex("\\b(mentre|nel frattempo|intanto|contemporaneamente)\\b", RegexOption.IGNORE_CASE)
        val sotto = db.recipes.filter { r ->
            val tot = r.steps.filterNot { par.containsMatchIn(it) }.sumOf { st -> dur.findAll(st).sumOf { m -> maxOf(m.groupValues[1].toInt(), m.groupValues[2].toIntOrNull() ?: 0) } }
            r.minutes < tot
        }
        assertTrue("ricette con tempo inferiore alla somma dei passaggi: ${sotto.map { it.id }.take(10)}", sotto.isEmpty())
    }
}
