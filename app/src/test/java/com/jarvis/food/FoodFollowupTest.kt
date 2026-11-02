package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FoodFollowupTest {
    private val date=LocalDate.of(2026,11,2)
    private fun food(name:String="Alimento")=Food("test",name,kcal=250.0,protein=15.0,carbs=25.0,fat=10.0)
    @Test fun missingNutrientsStayUnknownAndDoNotBecomeGreen() {
        val a=FoodGuidance.advice(food(),100.0,emptyList(),date)
        assertTrue(a.signals.all { it.level==FoodGuidance.Level.UNKNOWN })
        assertEquals("Valutazione parziale",a.title)
    }
    @Test fun pureAlmondCreamDoesNotGetArbitraryWeeklyLimitOrJunkLabel() {
        val a=FoodGuidance.advice(food("100% crema di mandorle tostate").copy(sugar=3.0,satFat=4.0,salt=.01),15.0,emptyList(),date)
        assertFalse(a.caution)
        assertTrue(a.frequency.contains("Non c'è un limite generale"))
        assertTrue(a.explanation.contains("tostatura"))
    }
    @Test fun processingAdviceRequiresKnownCategoryRatherThanProductNameAlone() {
        val dessert=food("Salame di cioccolato").copy(source="recipe",nutrientDataVersion=1)
        assertFalse(FoodGuidance.advice(dessert,30.0,emptyList(),date).caution)
        val meat=food("Prosciutto").copy(group="salumi")
        assertTrue(FoodGuidance.advice(meat,30.0,emptyList(),date).caution)
        assertEquals("Carne lavorata · da limitare",FoodGuidance.advice(meat,30.0,emptyList(),date).title)
    }
    @Test fun labelBoundariesAndDrinkThresholdsAreExplicit() {
        val a=food().copy(sugar=22.5,satFat=5.01,salt=1.5)
        assertEquals(listOf(FoodGuidance.Level.MEDIUM,FoodGuidance.Level.HIGH,FoodGuidance.Level.MEDIUM),FoodGuidance.signals(a).map { it.level })
        assertEquals(FoodGuidance.Level.HIGH,FoodGuidance.signals(a.copy(liquid=true,sugar=11.3))[0].level)
        assertEquals(FoodGuidance.Level.HIGH,FoodGuidance.signals(a.copy(liquid=true,salt=.76))[2].level)
        assertEquals(FoodGuidance.Level.UNKNOWN,FoodGuidance.signals(a.copy(unknownNutrients=listOf("sugar")))[0].level)
        assertTrue(FoodGuidance.signals(a.copy(name="Olio di oliva",liquid=true,group="olio")).all { it.level==FoodGuidance.Level.UNKNOWN })
    }
    @Test fun habitCountIgnoresFutureAndOldEntriesAndCountsDays() {
        val f=food()
        val entries=listOf(date,date,date.minusDays(6),date.minusDays(7),date.plusDays(1)).mapIndexed { i,d->DiaryEntry("$i",d.toString(),0,f,50.0,0) }
        assertTrue(FoodGuidance.advice(f,50.0,entries,date).frequency.startsWith("Registrato in 2 giorni"))
    }
    @Test fun manualMealSurvivesSerializationAndOlderSlotsKeepDefaults() {
        val recipe=Recipe("manual:test","Mio pasto",1,listOf(Ingredient(food(),123.5)))
        val slot=PlanSlot(0,recipe.id,manualRecipe=recipe)
        assertEquals(slot,Json.decodeFromString<PlanSlot>(Json.encodeToString(slot)))
        assertNull(Json.decodeFromString<PlanSlot>("{\"slot\":0,\"recipeId\":\"old\"}").manualRecipe)
    }
    @Test fun manualQuantitiesAndUnknownMicrosArePreservedWhenSolvingDay() {
        val recipe=Recipe("manual:test","Mio pasto",1,listOf(Ingredient(food(),123.5)))
        val prefs=DietPrefs(slots=listOf(MealSlotDef("Pranzo","pranzo",100,1)))
        val meals=Planner.solveDay(FoodDb(emptyList(),emptyList()),DayPlan(date.toString(),listOf(PlanSlot(0,recipe.id,manualRecipe=recipe))),Macro(700.0,40.0,80.0,25.0),prefs)
        assertEquals(1,meals.size)
        assertEquals(123.5,meals.single().solved.items.single().grams,0.0)
        assertEquals(308.75,meals.single().solved.total.kcal,.001)
        assertNull(NutritionScience.per100(meals.single().solved.toFood(), "vitD"))
    }
    @Test fun missingMacrosPreventQuantityOptimization() {
        val items=listOf(Ingredient(food().copy(unknownNutrients=listOf("protein")),100.0))
        assertFalse(ManualMeals.complete(items))
        assertEquals(items,ManualMeals.adjust(items,Macro(600.0,40.0,80.0,20.0)))
    }
    @Test fun quantitySuggestionNeverWorsensFitAndStaysBounded() {
        val items=listOf(Ingredient(food(),100.0),Ingredient(food("Altro").copy(id="other",protein=30.0,carbs=2.0,fat=1.0),50.0))
        val target=Macro(600.0,50.0,50.0,15.0)
        val adjusted=ManualMeals.adjust(items,target)
        assertTrue(ManualMeals.error(ManualMeals.total(adjusted),target)<=ManualMeals.error(ManualMeals.total(items),target))
        adjusted.forEachIndexed { i,it->assertTrue(it.grams in items[i].grams*.5..items[i].grams*2) }
        assertEquals(100.0,items[0].grams,0.0)
    }
}
