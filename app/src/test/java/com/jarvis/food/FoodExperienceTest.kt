package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class FoodExperienceTest {
    private val day=LocalDate.of(2026,11,2)
    private fun food()=Food("example","Alimento",kcal=400.0,protein=20.0,carbs=40.0,fat=15.0)
    private fun db()=FoodDb.parse(File("src/main/assets/foods.json").readText(),File("src/main/assets/recipes.json").readText())

    @Test fun missingFactsDoNotBecomeGreenOrZeroAdditives() {
        val review=FoodReview.assess(food())
        assertEquals(FoodReview.Tone.UNKNOWN,review.tone)
        assertNull(review.grade)
        assertTrue(review.positive.none { it.title in setOf("Zuccheri","Saturi","Sale") })
        val facts=OpenFoodFacts.parseFacts(JSONObject("{\"additives_n\":0,\"labels_tags\":[],\"nutriscore_grade\":\"unknown\"}"))
        assertNull(facts.additivesCount)
        assertNull(facts.organic)
        assertNull(facts.nutriScore)
    }
    @Test fun productMetadataIsParsedSeparatelyAndSurvivesOlderModels() {
        val p=JSONObject("""{"code":"test-review","product_name":"Cereali","nutriments":{"energy-kcal_100g":378,"proteins_100g":9.8,"carbohydrates_100g":65,"fat_100g":8},"nutriscore_grade":"a","nutriscore_version":"2023","ingredients_text_it":"Avena, cacao","additives_n":0,"labels_tags":["en:eu-organic"],"nova_group":1} """)
        val food=OpenFoodFacts.parse(p,null)!!
        assertEquals(378.0,food.kcal,0.0)
        assertEquals("a",food.productFacts!!.nutriScore)
        assertEquals(0,food.productFacts!!.additivesCount)
        assertEquals(true,food.productFacts!!.organic)
        assertEquals(food,Json.decodeFromString<Food>(Json.encodeToString(food)))
        assertNull(Json.decodeFromString<Food>("""{"id":"old","name":"Vecchio","kcal":100.0,"protein":2.0,"carbs":15.0,"fat":3.0}""").productFacts)
    }
    @Test fun nutrientStrengthsUseEnergyForProteinAndDocumentedFibreThresholds() {
        val f=food().copy(protein=20.0,fiber=6.0,sugar=3.0,satFat=1.0,salt=.1)
        val r=FoodReview.assess(f)
        assertTrue(r.positive.any { it.title=="Ricco di proteine" })
        assertTrue(r.positive.any { it.title=="Ricco di fibre" })
        assertFalse(FoodReview.assess(f.copy(protein=11.0,fiber=1.0)).positive.any { it.title.contains("proteine") || it.title.contains("fibre") })
        val liquid=f.copy(liquid=true,kcal=400.0,fiber=3.0)
        assertFalse(FoodReview.assess(liquid).positive.any { it.title.contains("fibre") })
    }
    @Test fun additivesAndOrganicAreFactsWithoutAnInventedHealthPenalty() {
        val f=food().copy(sugar=3.0,satFat=1.0,salt=.1)
        val without=FoodReview.assess(f)
        val with=FoodReview.assess(f.copy(productFacts=ProductFacts(additivesCount=2,organic=true)))
        assertEquals(without.tone,with.tone)
        assertTrue(with.facts.any { it.title=="Additivi" && it.value.contains("2 dichiarati") })
        assertTrue(with.facts.any { it.title=="Biologico" })
        assertTrue(with.attention.isEmpty())
    }
    @Test fun chocolateAndPureAlmondsHaveContextualSummaries() {
        val chocolate=food().copy(name="Cioccolato al latte",sugar=59.0,satFat=19.0,salt=.2)
        val bad=FoodReview.assess(chocolate)
        assertEquals(FoodReview.Tone.LIMIT,bad.tone)
        assertEquals(setOf("Zuccheri","Saturi"),bad.attention.map { it.title }.toSet())
        val almonds=food().copy(name="100% crema di mandorle",kcal=625.0,protein=21.0,sugar=3.0,satFat=5.2,salt=.01)
        assertEquals(FoodReview.Tone.CAUTION,FoodReview.assess(almonds).tone)
        assertEquals("Conta la porzione",FoodReview.assess(almonds).title)
        assertEquals("Nutri-Score E",FoodReview.assess(chocolate.copy(productFacts=ProductFacts(nutriScore="e"))).title)
        assertEquals(FoodReview.Tone.GOOD,FoodReview.assess(chocolate.copy(productFacts=ProductFacts(nutriScore="a"))).tone)
    }
    @Test fun genericTipsAreThreeStableWithinADayAndNewTheNextDay() {
        val db=db();val state=AppState()
        val today=DailyFoodTips.build(state,day,db)
        assertEquals(3,today.size)
        assertTrue(today.all { !it.personal && it.text.length<=110 })
        assertEquals(today,DailyFoodTips.build(state,day,db))
        val tomorrow=DailyFoodTips.build(state,day.plusDays(1),db)
        assertTrue(today.map { it.id }.intersect(tomorrow.map { it.id }.toSet()).isEmpty())
        (today+tomorrow).forEach { assertTrue(NutritionEvidence.source(it.source).url.startsWith("https://")) }
    }
    @Test fun dailyFoodSuggestionsRespectDietAndExclusions() {
        val db=db()
        val state=AppState(dietPrefs=DietPrefs(styleId="vegan",exclude=listOf("frutta_secca","glutine","lattosio")))
        for(offset in 0..20) {
            val tips=DailyFoodTips.build(state,day.plusDays(offset.toLong()),db)
            assertEquals(3,tips.size)
            assertTrue(tips.none { "noci" in it.foodIds || "greco0" in it.foodIds || "avena" in it.foodIds })
        }
    }
    @Test fun anIncompleteDiaryDoesNotTurnAGeneralTipIntoADeficiencyWarning() {
        val review=NutritionReview(7,1,emptyList(),0,emptyList(),listOf(NutritionNotice("fiber","Fibre","","","","","fiber",true)),emptyList(),0,0,1,1,true)
        assertTrue(DailyFoodTips.build(AppState(),day,db(),review).all { !it.personal })
    }
    @Test fun jointBalanceImprovesTheDayAndPreservesFixedMealGrams() {
        fun f(id:String,k:Double,p:Double,c:Double,f:Double)=DbFood(id,id,"Altro","base",fdc=0,usda="",kcal=k,protein=p,carbs=c,fat=f)
        val carb=f("joint-carb",400.0,0.0,100.0,0.0);val protein=f("joint-protein",400.0,100.0,0.0,0.0);val fat=f("joint-fat",900.0,0.0,0.0,100.0)
        val ings=listOf(TemplateIngredient(carb.id,100.0,"c"),TemplateIngredient(protein.id,100.0,"p"),TemplateIngredient(fat.id,30.0,"f"))
        val recipe=RecipeTemplate("joint","Pasto","pranzo",10,ings,emptyList())
        val foods=listOf(carb,protein,fat)
        val adjustable=PlannedMeal(0,"Pranzo",1,SolvedRecipe(recipe,ings.mapIndexed { i,ing -> SolvedIngredient(ing,foods[i],ing.g) },Macro.ZERO))
        val fixed=PlannedMeal(1,"Fisso",2,SolvedRecipe(recipe,listOf(SolvedIngredient(ings[0],carb,50.0)),Macro.ZERO))
        val target=Macro(1200.0,50.0,150.0,44.444)
        val result=Solver.balanceDay(listOf(adjustable,fixed),target,setOf(0))
        assertEquals(fixed,result[1])
        val total=result.fold(Macro.ZERO) { a,m -> a+m.solved.total }
        assertEquals(1200.0,total.kcal,50.0)
        assertEquals(50.0,total.protein,5.0)
        assertEquals(44.444,total.fat,5.0)
        assertTrue(result[0].solved.items.all { it.grams in 0.0..it.ing.g*2.5 })
    }
}
