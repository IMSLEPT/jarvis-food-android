package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DietStylesTest {
    private val db = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())
    private val date = LocalDate.of(2026,11,2)
    private val target = Macro(2200.0,120.0,260.0,75.0)

    @Test fun everyStyleIsAvailableAndIngredientRulesAreStrict() {
        assertTrue(DietStyles.options.size >= 16)
        for (style in listOf("vegetarian", "vegan", "pescatarian", "japanese", "paleo", "carnivore", "glutenfree", "lactosefree")) {
            for (meal in listOf("colazione", "pranzo", "cena", "spuntino")) {
                val candidates = Planner.candidates(db, meal, DietPrefs(styleId=style))
                assertTrue("$style / $meal without compatible recipes", candidates.isNotEmpty())
                candidates.forEach { assertTrue(DietStyles.allowed(db,it,style)) }
            }
        }
        Planner.candidates(db,"pranzo",DietPrefs(styleId="vegan")).forEach { recipe ->
            assertFalse(db.recipeGroups(recipe).any { it in setOf("carne_bianca","carne_rossa","salumi","uova","pesce","pesce_grasso") })
            assertTrue(recipe.ings.none { it.food == "miele" })
        }
        DietStyles.strictRecipes.forEach { assertEquals(it,db.recipe(it.id)) }
    }

    @Test fun incompatibleMacroStylesAreRejectedWithoutChangingAnyTarget() {
        val copy = target.copy()
        assertNotNull(DietStyles.targetProblem("keto",target))
        assertNotNull(DietStyles.targetProblem("carnivore",target))
        assertNotNull(DietStyles.targetProblem("lowcarb",target))
        assertNotNull(DietStyles.targetProblem("hypocaloric",target,2100.0))
        assertNull(DietStyles.targetProblem("hypocaloric",target,2500.0))
        val result = DietStyles.build(db,DietPrefs(),date,7,emptyList(),"keto",{target},{2500.0})
        assertTrue(result.days.isEmpty())
        assertNotNull(result.problem)
        assertEquals(copy,target)
    }

    @Test fun exclusionsNeverFallbackToIncompatibleFood() {
        val prefs = DietPrefs(styleId="japanese", maxMinutes=5)
        assertTrue(Planner.candidates(db,"colazione",prefs).isEmpty())
        val result = DietStyles.build(db,prefs,date,1,emptyList(),"japanese",{target},{2500.0})
        assertNotNull(result.problem)
        assertTrue(result.days.isEmpty())
    }

    @Test fun generatedVegetarianDayKeepsTargetsAndMatchesComputedMacros() {
        val result = DietStyles.build(db,DietPrefs(),date,1,emptyList(),"vegetarian",{target},{2500.0})
        assertNull(result.problem,result.problem)
        val day = result.days.single()
        assertEquals("vegetarian",day.styleId)
        val meals = Planner.solveDay(db,day,target,DietPrefs(styleId="vegetarian"))
        assertEquals(4,meals.size)
        assertTrue(meals.all { DietStyles.allowed(db,it.solved.template,"vegetarian") })
        val total = meals.fold(Macro.ZERO) { sum,m -> sum+m.solved.total }
        assertEquals(target.kcal,total.kcal,target.kcal*.10)
        assertEquals(target.protein,total.protein,target.protein*.15)
        assertEquals(target.carbs,total.carbs,target.carbs*.15)
        assertEquals(target.fat,total.fat,target.fat*.15)
    }
}
