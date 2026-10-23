package com.jarvis.food

import com.jarvis.food.ai.RecipeImport
import com.jarvis.food.engine.FoodDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Lettura offline di una ricetta da testo: titolo, porzioni, tempo, ingredienti con quantità e passaggi. */
class RecipeImportTest {
    private val db: FoodDb = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())

    private val text = """
        Pasta al pomodoro
        Per 2 persone
        Tempo: 20 minuti
        Ingredienti
        - 200 g di spaghetti
        - 300 g passata di pomodoro
        - 2 cucchiai di olio extravergine
        - basilico q.b.
        - 1 uovo
        - 3 foglie di unobtainium magico
        Preparazione
        1. Cuoci la pasta in acqua salata per 10 minuti.
        2. Scalda la passata con l'olio
        e il basilico.
        3. Unisci e servi.
    """.trimIndent()

    @Test fun parsesStructuredRecipe() {
        val r = RecipeImport.parse(db, text)
        assertEquals("Pasta al pomodoro", r.name)
        assertEquals(2, r.servings)
        assertEquals(20, r.minutes)
        val g = r.ingredients.associate { it.first.id to it.second }
        assertEquals(200.0, g["db:pasta"] ?: -1.0, 0.01)
        assertEquals(300.0, g["db:passata"] ?: -1.0, 0.01)
        assertEquals(20.0, g["db:olio"] ?: -1.0, 0.01)   // 2 cucchiai = 20 g
        assertEquals(50.0, g["db:uova"] ?: -1.0, 0.01)   // 1 uovo = peso del pezzo
        assertTrue("basilico", g.containsKey("db:basilico"))
        assertTrue("l'ingrediente sconosciuto va segnalato, non inventato", r.unmatched.any { it.contains("unobtainium") })
        assertEquals(3, r.steps.size)
        assertTrue("le righe spezzate dall'OCR vanno unite", r.steps[1].contains("olio e il basilico"))
    }

    @Test fun emptyTextGivesEmptyRecipe() {
        val r = RecipeImport.parse(db, "   \n  ")
        assertTrue(r.ingredients.isEmpty() && r.steps.isEmpty())
    }

    @Test fun recipeWithoutHeadingsStillFindsIngredientsAndSteps() {
        val r = RecipeImport.parse(db, "Insalata di riso\n150 g riso\n100 g tonno\nCuoci il riso e mescola con il tonno quando è freddo.")
        assertTrue(r.ingredients.size >= 2)
        assertTrue(r.steps.isNotEmpty())
    }
}
