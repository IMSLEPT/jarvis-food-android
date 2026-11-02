package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DietSelectionFollowupTest {
    private val db = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())
    private val date = LocalDate.of(2026,11,2)
    private val fiveMeals = listOf(MealSlotDef("Colazione","colazione",25,0), MealSlotDef("Spuntino mattutino","spuntino",10,4),
        MealSlotDef("Pranzo","pranzo",30,1), MealSlotDef("Merenda","spuntino",10,3), MealSlotDef("Cena","cena",25,2))
    private val usual = Macro(2200.0,120.0,260.0,75.0)

    @Test fun japaneseTimeConflictHasAnActionableMinimumRatherThanAnExclusionError() {
        val prefs = DietPrefs(slots=fiveMeals,maxMinutes=15)
        val available = DietStyles.availability(db,prefs,"japanese")
        assertFalse(available.available)
        assertEquals(25,available.requiredMinutes)
        assertTrue(available.missingMeals.containsAll(listOf("Pranzo","Cena")))
        assertTrue(available.message("Giapponese",15)!!.contains("25 minuti"))
        assertTrue(DietStyles.availability(db,prefs.copy(maxMinutes=25),"japanese").available)
        assertEquals(15,prefs.maxMinutes)
    }

    @Test fun extraTimeDoesNotPretendToFixExcludedIngredients() {
        val prefs = DietPrefs(slots=fiveMeals,maxMinutes=15,exclude=listOf("soia"))
        val available = DietStyles.availability(db,prefs,"japanese")
        assertFalse(available.available)
        assertNull(available.requiredMinutes)
        assertTrue(available.message("Giapponese",15)!!.contains("esclusioni"))
        assertTrue(Planner.candidates(db,"colazione",prefs.copy(styleId="japanese",maxMinutes=1000)).isEmpty())
        assertEquals(listOf("soia"),prefs.exclude)
    }

    @Test fun healthyIsDistinctFromBalancedWithoutBanningPureNuts() {
        assertEquals("Salutare",DietStyles.get("healthy").title)
        val dessert = RecipeTemplate("health-test-sweet","Pane e miele","colazione",5,
            listOf(TemplateIngredient("pane_int",70.0,"c"),TemplateIngredient("miele",15.0,"f")),listOf("Servi"))
        assertTrue(DietStyles.allowed(db,dessert,"balanced"))
        assertFalse(DietStyles.allowed(db,dessert,"healthy"))
        val nuts = RecipeTemplate("health-test-nuts","Noci","spuntino",5,listOf(TemplateIngredient("noci",30.0,"f")),listOf("Pesa"))
        assertTrue(DietStyles.allowed(db,nuts,"healthy"))
        for(meal in listOf("colazione","pranzo","cena","spuntino")) {
            val candidates=Planner.candidates(db,meal,DietPrefs(styleId="healthy"))
            assertTrue(meal,candidates.isNotEmpty())
            assertTrue(candidates.all { r -> db.recipeGroups(r).none { it in setOf("salumi","dolci","alcol") } })
        }
    }

    @Test fun everySelectableStyleBuildsWithCompatibleTargetsAndFiveMeals() {
        val failures=mutableListOf<String>()
        for(style in DietStyles.options) {
            val target=when(style.id) {
                "keto" -> Macro(2000.0,110.0,30.0,160.0)
                "carnivore" -> Macro(2000.0,140.0,15.0,153.333)
                "lowcarb" -> Macro(2000.0,120.0,100.0,124.444)
                "highprotein" -> Macro(2200.0,140.0,240.0,75.556)
                "hypocaloric" -> Macro(2000.0,120.0,240.0,62.222)
                else -> usual
            }
            val prefs=DietPrefs(slots=fiveMeals,maxMinutes=60)
            val result=DietStyles.build(db,prefs,date,1,emptyList(),style.id,{target},{2500.0})
            println("STYLE_AUDIT ${style.id}: ${result.problem ?: "OK"}")
            if(result.problem!=null) failures += "${style.id}: ${result.problem}"
            else {
                val plan=result.days.single()
                assertEquals(style.id,plan.styleId)
                assertEquals(5,plan.slots.size)
                assertTrue(plan.slots.all { DietStyles.allowed(db,db.recipe(it.recipeId)!!,style.id) })
                val meals=Planner.solveDay(db,plan,target,prefs.copy(styleId=style.id))
                assertEquals(target.kcal,meals.sumOf { it.solved.total.kcal },target.kcal*.10)
            }
        }
        assertTrue(failures.joinToString("\n"),failures.isEmpty())
    }

    @Test fun japaneseAndHealthySupportWholePeriodsWithoutChangingTargets() {
        for(style in listOf("japanese","healthy")) for(count in listOf(7,30)) {
            val prefs=DietPrefs(slots=fiveMeals,maxMinutes=40)
            val result=DietStyles.build(db,prefs,date,count,emptyList(),style,{usual},{2500.0})
            assertNull("$style / $count: ${result.problem}",result.problem)
            assertEquals(count,result.days.size)
            assertTrue(result.days.all { it.slots.size==5 && it.styleId==style })
            assertEquals(Macro(2200.0,120.0,260.0,75.0),usual)
        }
    }
}
