package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class NutritionScienceTest {
    private val today = LocalDate.of(2026,11,1)
    private fun food(sugar: Double? = 30.0, free: Double? = null, fiber: Double? = 4.0, salt: Double? = 0.2, sat: Double? = 1.0) =
        Food("f", "Yogurt", kcal=200.0, protein=10.0, carbs=30.0, fat=4.0, sugar=sugar, freeSugar=free, fiber=fiber, salt=salt, satFat=sat,
            micro=mapOf("vitD" to 2.0, "vitC" to 10.0, "vitB12" to 1.0))
    private fun state(f: Food, days: Int = 7, grams: Double = 100.0) = AppState(onboarded=true, goals=Goals(kcal=2000, auto=false),
        diary=(1..days).flatMap { day -> (0..2).map { meal -> DiaryEntry("$day-$meal",today.minusDays(day.toLong()).toString(),meal,f,grams,0L) } },
        nutritionCompleteDays=(1..days).map { today.minusDays(it.toLong()).toString() })
    @Test fun totalSugarNeverTriggersFreeSugarWarning() {
        val r=NutritionScience.analyze(state(food(sugar=80.0)),today)
        assertFalse(r.notices.any { it.id=="free-sugar" }); assertNull(r.nutrient("freeSugar").average)
        assertTrue(r.notices.any { it.id=="missing-sugars" })
    }
    @Test fun freeSugarUsesRecordedEnergyAndCorrectUnits() {
        val r=NutritionScience.analyze(state(food(free=10.0)),today)
        assertEquals(30.0,r.nutrient("freeSugar").average!!,0.001)
        assertEquals(600.0,r.nutrient("kcal").average!!,0.001)
        assertTrue(r.notices.any { it.id=="free-sugar" && it.attention })
    }
    @Test fun sodiumSaltConversionIsNotDoubleCounted() {
        val f=food(salt=1.0).copy(micro=mapOf("sodium" to 400.0))
        val r=NutritionScience.analyze(state(f),today)
        assertEquals(1200.0,r.nutrient("sodium").average!!,0.001)
        assertEquals(3.0,r.nutrient("salt").average!!,0.001)
    }
    @Test fun partialDaysDoNotCreateLowNutrientWarnings() {
        val s=state(food(),days=7).copy(nutritionCompleteDays=emptyList())
        val r=NutritionScience.analyze(s,today)
        assertTrue(r.usableDays.isEmpty()); assertNull(r.nutrient("fiber").average)
        assertFalse(r.notices.any { it.attention || it.id=="fiber" })
    }
    @Test fun missingNutrientIsNotZeroAndHasNoPercentage() {
        val s=state(food(fiber=null)).copy(diary=state(food(fiber=null)).diary.mapIndexed { i,e -> if(i==0)e.copy(food=food(fiber=0.0)) else e })
        val r=NutritionScience.analyze(s,today)
        assertEquals(1,r.nutrient("fiber").knownEntries); assertNull(r.nutrient("fiber").percent)
        assertFalse(r.notices.any { it.id=="fiber" })
    }
    @Test fun datesOutsideWindowAndFutureAreExcluded() {
        val s=state(food(),days=30)
        assertEquals(6,NutritionScience.analyze(s,today,7).usableDays.size)
        assertEquals(13,NutritionScience.analyze(s,today,14).usableDays.size)
        assertEquals(29,NutritionScience.analyze(s,today,30).usableDays.size)
        val future=s.diary.first().copy(id="future",date=today.plusDays(1).toString())
        assertEquals(NutritionScience.analyze(s,today).recordedDays,NutritionScience.analyze(s.copy(diary=s.diary+future),today).recordedDays)
    }
    @Test fun repetitionIsDescriptiveAndNeverHarmfulByItself() {
        val r=NutritionScience.analyze(state(food(free=0.0),days=6),today)
        assertEquals(6,r.repeatedBreakfastDays)
        assertFalse(r.notices.first { it.id=="repetition" }.attention)
    }
    @Test fun minimumObservationsAndSpecialProfilesDisableWarnings() {
        assertFalse(NutritionScience.analyze(state(food(free=70.0),2),today).notices.any { it.attention })
        assertFalse(NutritionScience.analyze(state(food(free=70.0)).copy(profile=Profile(special=1)),today).notices.any { it.attention })
        assertFalse(NutritionScience.analyze(state(food(free=70.0)).copy(profile=Profile(birthYear=2015)),today).applicable)
    }
    @Test fun operationalCompletenessUsesDateSpecificTargetsAndThreeMeals() {
        val s=state(food(),days=3,grams=300.0).copy(nutritionCompleteDays=emptyList())
        assertEquals(3,NutritionScience.analyze(s,today,targetForDate={2000.0}).usableDays.size)
        assertEquals(0,NutritionScience.analyze(s,today,targetForDate={3000.0}).usableDays.size)
    }
    @Test fun sourceThresholdsAndPortionsHaveCorrectMeaning() {
        assertEquals(0.10,NutritionEvidence.FREE_SUGAR_ENERGY_FRACTION,0.0)
        assertEquals(0.10,NutritionEvidence.SATURATED_ENERGY_FRACTION,0.0)
        assertEquals(2000.0,NutritionEvidence.SODIUM_MG,0.0)
        assertEquals(25.0,NutritionEvidence.FIBER_G,0.0)
        assertEquals(15.0,NutritionEvidence.reference("vitD",Profile())!!,0.0)
        assertEquals(95.0,NutritionEvidence.reference("vitC",Profile(male=false))!!,0.0)
        assertEquals(4.0,NutritionEvidence.reference("vitB12",Profile())!!,0.0)
        assertNull(NutritionEvidence.reference("sugar",Profile()))
        assertEquals(125.0,NutritionScience.standardPortion(food())!!,0.0)
        assertTrue(NutritionEvidence.sources.all { it.url.startsWith("https://") && it.year<=2026 })
    }
    @Test fun foodArtworkHasDistinctFishAndSafeFallback() {
        assertEquals(0,FoodArtCatalog.resolve("Trota iridea").cell)
        assertEquals(1,FoodArtCatalog.resolve("Salmone").cell)
        assertEquals(2,FoodArtCatalog.resolve("Sgombro").cell)
        assertEquals(3,FoodArtCatalog.resolve("Sardine").cell)
        assertEquals(6,FoodArtCatalog.resolve("Banana").cell)
        assertEquals(7,FoodArtCatalog.resolve("Yogurt greco").cell)
        assertNull(FoodArtCatalog.resolve("Melanzane").cell)
        assertNull(FoodArtCatalog.resolve("Alimento sconosciuto").cell)
    }
    @Test fun oldBackupsDecodeWithoutInventingAddedSugar() {
        val old="""{"id":"old","name":"Yogurt","kcal":100.0,"protein":5.0,"carbs":10.0,"fat":3.0,"sugar":8.0}"""
        val f=Json.decodeFromString<Food>(old)
        assertNull(f.addedSugar); assertNull(f.freeSugar); assertTrue(f.unknownNutrients.isEmpty())
    }
    @Test fun invalidDatesAndUnknownCompositeNutrientsAreSafe() {
        val s=state(food()).copy(diary=state(food()).diary+DiaryEntry("bad","wrong",0,food(),100.0,0))
        assertEquals(6,NutritionScience.analyze(s,today).usableDays.size)
        assertNull(NutritionScience.per100(food().copy(unknownNutrients=listOf("vitD")),"vitD"))
    }
    @Test fun simpleFoodSugarRulesDistinguishJuiceAndIntactFruit() {
        val db=FoodDb.parse(java.io.File("src/main/assets/foods.json").readText(),java.io.File("src/main/assets/recipes.json").readText())
        assertEquals(0.0,db.food("banana")!!.toFood().freeSugar!!,0.0)
        assertEquals(0.0,db.food("greco0")!!.toFood().addedSugar!!,0.0)
        val juice=db.food("succo_arancia")!!
        assertEquals(juice.micro.getValue("sugar"),juice.toFood().freeSugar!!,0.0)
        assertNull(juice.toFood().addedSugar)
        assertNotNull(juice.toFood().sugarDataNote)
    }
    @Test fun oldCompositeRecordsDoNotClaimCompleteMicronutrientData() {
        val old=food().copy(source="recipe",micro=mapOf("vitD" to 0.0))
        assertNull(NutritionScience.per100(old,"vitD"))
        assertEquals(200.0,NutritionScience.per100(old,"kcal")!!,0.0)
    }

}
