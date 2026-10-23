package com.jarvis.food

import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.Practical
import com.jarvis.food.engine.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** "Riadatta ricetta": porzioni pratiche (unità intere), giudizio onesto, contorni e alternative. */
class PracticalTest {
    private val db: FoodDb = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())

    private fun baseMacro(id: String): Macro = db.recipe(id)!!.let { t -> t.ings.fold(Macro.ZERO) { a, i -> a + Macro.of(db.food(i.food)!!, i.g) } }

    @Test fun leUnitaNonSiDividonoInFrazioniScomode() {
        var checked = 0
        for (r in db.recipes) {
            val base = baseMacro(r.id)
            for (k in doubleArrayOf(0.85, 1.3)) {
                val s = Solver.solve(r, db, base * k, practical = true)
                for (it in s.items) {
                    val unit = Practical.unitStep(it.food) ?: continue
                    if (it.ing.role == "x") continue
                    val n = it.grams / unit
                    assertTrue("${r.id}: ${it.food.id} ${it.grams} g non è un multiplo di $unit g", abs(n - Math.round(n)) < 1e-6)
                    assertTrue("${r.id}: ${it.food.id} sotto una unità", it.grams >= unit - 1e-6)
                    checked++
                }
            }
        }
        assertTrue("nessun ingrediente a unità controllato", checked > 500)
    }

    @Test fun laPiadinaResta_interaEGliAltriIngredientiSiRiequilibrano() {
        val r = db.recipes.first { t -> t.ings.any { it.food == "piadina" && it.role != "x" } }
        val piad = db.food("piadina")!!
        val base = baseMacro(r.id)
        val s = Solver.solve(r, db, base * 1.25, practical = true)
        val g = s.items.first { it.food.id == "piadina" }.grams
        assertEquals("mai frazioni di piadina", 0.0, g % piad.pieceGrams!!, 1e-6)
        val a = Practical.adapt(db, r, base * 1.25, withAlternatives = false)
        assertTrue(a.practical.items.first { it.food.id == "piadina" }.grams % piad.pieceGrams!! < 1e-6)
    }

    @Test fun obiettivoAssurdoVieneRiconosciutoENonForzato() {
        val insalata = db.recipes.first { RecipeMetaKcal(it) < 300 }
        val a = Practical.adapt(db, insalata, Macro(1500.0, 120.0, 150.0, 50.0), allowed = { true })
        assertEquals(Practical.Level.POOR, a.level)
        assertTrue("manca la spiegazione", a.headline.isNotBlank() && a.notes.isNotEmpty())
        // le quantità non superano mai le porzioni massime realistiche
        for (i in a.practical.items) assertTrue("${i.food.id} ${i.grams} g", i.grams <= Solver.maxServing(i.food) + 0.01)
    }

    @Test fun ilContornoMigliora_eNonSforaIlPasto() {
        var withSide = 0
        for (r in db.recipes.take(600)) {
            val base = baseMacro(r.id)
            for (k in doubleArrayOf(1.5, 1.8, 2.1, 2.4, 2.8, 3.2)) {
                val target = base * k
                val a = Practical.adapt(db, r, target, withAlternatives = false)
                val sd = a.side ?: continue
                withSide++
                assertTrue("${r.id}: il contorno peggiora", Solver.fitScore(sd.after) < Solver.fitScore(a.practical))
                assertTrue("${r.id}: contorno sfora l'obiettivo", Practical.kcalErrPct(sd.after) <= 7.0 + 1e-9)
                assertEquals("${r.id}: kcal del contorno incoerenti", a.practical.total.kcal + sd.macro.kcal, sd.after.total.kcal, 1e-6)
            }
        }
        assertTrue("nessun contorno proposto", withSide > 0)
    }

    @Test fun leAlternativeSonoDavveroMigliori() {
        val r = db.recipes.first { it.meal == "cena" }
        val t = Macro(1100.0, 90.0, 100.0, 40.0)
        val a = Practical.adapt(db, r, t)
        val here = Solver.fitScore(a.side?.after ?: a.practical)
        for (alt in a.alternatives) {
            assertEquals(Practical.Level.GOOD, Practical.level(alt.solved))
            assertTrue(Solver.fitScore(alt.solved) < here)
            assertEquals(r.meal, alt.solved.template.meal)
            assertTrue(alt.solved.template.id != r.id)
        }
    }

    @Test fun descrizioniDelleQuantita() {
        assertEquals("1 piadina (110 g)", Practical.describe(db.food("piadina")!!, 110.0))
        assertEquals("2 piadina (220 g)", Practical.describe(db.food("piadina")!!, 220.0))
        assertEquals("½ mozzarella (63 g)", Practical.describe(db.food("mozzarella")!!, 62.5))
        assertEquals("2 uova", Practical.describe(db.food("uova")!!, 100.0))
        assertEquals("80 g", Practical.describe(db.food("pasta")!!, 80.0))
    }

    @Test fun iContorniEsistonoNelDatabase() {
        for (id in listOf("banana", "mela", "arancia", "kiwi", "pera", "greco0", "yogurt", "pane", "pane_int", "noci", "mandorle", "parmigiano", "bresaola", "fesa_tacchino", "mozzarella_light", "fette_bisc", "gallette"))
            assertTrue("$id manca", db.food(id) != null)
    }

    private fun RecipeMetaKcal(t: com.jarvis.food.engine.RecipeTemplate) = com.jarvis.food.engine.RecipeMeta.info(db, t).kcal
}
