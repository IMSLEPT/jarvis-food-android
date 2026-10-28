package com.jarvis.food

import com.jarvis.food.data.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MealTrainingFeaturesTest {
    private val p = Profile(heightCm = 173.0, startWeight = 71.0, goal = 0, detailed = true, job = 1, stepsLevel = 1, workouts = 4)
    private val n = TrainingNutrition(enabled = true, trainingDays = listOf(1, 2, 4, 5), automatic = true, workout = TrainingLoad(45, 1, 1))
    private val food = Food("a", "Pasta", kcal = 370.0, protein = 13.0, carbs = 72.0, fat = 1.0)
    @Test fun autoOnOffPreservesWeeklyEnergyAndCountsOnlyNetExercise() {
        val e = AutomaticTraining.calculate(p, 71.0, n)!!
        assertEquals((5.0 - 1) * 71 * 45 / 60, e.sessionKcal, 0.0001)
        assertEquals(e.sessionKcal, (e.on.kcal - e.off.kcal).toDouble(), 10.0)
        assertEquals((e.on.kcal * 4 + e.off.kcal * 3) / 7.0, e.weeklyAverage, 0.0001)
        assertEquals(e.on.protein, e.off.protein, 0.0001)
        assertTrue(e.on.carbs > e.off.carbs)
    }
    @Test fun durationIntensityAndWeightChangeTheEstimate() {
        val base = AutomaticTraining.calculate(p, 71.0, n)!!
        val longer = AutomaticTraining.calculate(p, 71.0, n.copy(workout = TrainingLoad(90, 1, 1)))!!
        val harder = AutomaticTraining.calculate(p, 71.0, n.copy(workout = TrainingLoad(45, 1, 2)))!!
        assertTrue(longer.on.kcal > base.on.kcal)
        assertTrue(harder.sessionKcal > base.sessionKcal)
        assertTrue(AutomaticTraining.calculate(p, 80.0, n)!!.off.kcal > base.off.kcal)
    }
    @Test fun macrosAlwaysSumToExplicitCaloriesIncludingExtremeInputs() {
        for (weight in listOf(30.0, 71.0, 180.0, 300.0)) for (style in 0..2) {
            val e = AutomaticTraining.calculate(p.copy(dietStyle = style, goal = -1, weeklyRate = 2.0), weight, n)!!
            for (t in listOf(e.on, e.off)) {
                assertTrue(t.valid)
                assertEquals(t.kcal.toDouble(), t.macroKcal, 0.001)
            }
        }
    }
    @Test fun unsafeOrMissingInputsDoNotGenerateAutomaticTargets() {
        assertNull(AutomaticTraining.calculate(p, Double.NaN, n))
        assertNull(AutomaticTraining.calculate(p.copy(special = 1), 71.0, n))
        assertNull(AutomaticTraining.calculate(p.copy(special = 2), 71.0, n))
        assertNull(AutomaticTraining.calculate(p.copy(birthYear = LocalDate.now().year - 16), 71.0, n))
        assertNull(AutomaticTraining.calculate(p, 71.0, n.copy(workout = TrainingLoad(0))))
    }
    @Test fun scheduleFrequencyChangesWeeklyMeanAndGoalAdjustment() {
        val few = AutomaticTraining.calculate(p, 71.0, n.copy(trainingDays = listOf(1)))!!
        val many = AutomaticTraining.calculate(p, 71.0, n)!!
        assertTrue(many.weeklyAverage > few.weeklyAverage)
        assertTrue(few.proteinPerKg >= 1.6)
        assertEquals(few.on.protein, few.off.protein, 0.0001)
        assertEquals(4, AutomaticTraining.calculate(p, 71.0, n.copy(trainingDays = listOf(1, 1, 2, 4, 5, 9)))!!.sessions)
        assertTrue(AutomaticTraining.calculate(p.copy(goal = -1), 71.0, n)!!.dailyAdjustment < 0)
        assertTrue(AutomaticTraining.calculate(p.copy(goal = 1), 71.0, n)!!.dailyAdjustment > 0)
    }
    @Test fun presetsSelectExactlyThreeOrSixStableMeals() {
        val three = MealSchedule.withMeals(DietPrefs(), listOf(0, 1, 2))
        val six = MealSchedule.withMeals(three, MealSchedule.order)
        assertEquals(listOf(0, 1, 2), three.slots.map { it.diaryMeal })
        assertEquals(MealSchedule.order, six.slots.map { it.diaryMeal })
        assertTrue(MealSchedule.valid(six))
        assertEquals(1.0, com.jarvis.food.engine.Planner.slotTargets(com.jarvis.food.engine.Macro(1.0, 1.0, 1.0, 1.0), six).sumOf { it.kcal }, 0.00001)
    }
    @Test fun emptyScheduleCannotEraseTheWholeDay() {
        assertThrows(IllegalArgumentException::class.java) { MealSchedule.withMeals(DietPrefs(), emptyList()) }
        assertFalse(MealSchedule.valid(DietPrefs(slots = emptyList())))
    }
    @Test fun changingMealsRemapsPlansAndLinksWithoutDeletingAnyLoggedFood() {
        val date = "2026-10-27"
        val entries = listOf(DiaryEntry("lunch", date, 1, food, 100.0, 1L, "$date#1"), DiaryEntry("snack", date, 3, food, 20.0, 2L, "$date#2"))
        val state = AppState(diary = entries, plan = listOf(DayPlan(date, listOf(PlanSlot(1, "r", listOf("lunch")), PlanSlot(2, "snack-r", listOf("snack"))))))
        val six = MealSchedule.apply(state, MealSchedule.withMeals(state.dietPrefs, MealSchedule.order))
        assertEquals("$date#2", six.diary.first().planRef)
        assertEquals(listOf(2, 3), six.plan.single().slots.map { it.slot })
        assertEquals(listOf("lunch"), six.plan.single().slots.first().eatenEntryIds)
        val three = MealSchedule.apply(six, MealSchedule.withMeals(six.dietPrefs, listOf(0, 1, 2)))
        assertEquals(2, three.diary.size)
        assertNull(three.diary.last().planRef)
        assertTrue(3 in MealSchedule.visible(three, date))
        assertFalse(3 in MealSchedule.visible(three, "2026-10-28"))
    }
    @Test fun photographedDishReplacesOneEntryAndRetainsMicronutrients() {
        val a = food.copy(micro = mapOf("iron" to 2.0))
        val b = food.copy(id = "b", name = "Pesto", kcal = 500.0, micro = mapOf("iron" to 1.0))
        val (combined, grams) = ReplacementFoods.combine(listOf(a to 100.0, b to 20.0), "dish")
        assertEquals(120.0, grams, 0.0)
        assertEquals(470.0, Nutrition.of(combined, grams).kcal, 0.001)
        assertEquals(2.2, combined.micro.getValue("iron") * grams / 100, 0.001)
        val entry = DiaryEntry("existing", "2026-10-27", 1, a, 100.0, 100L)
        val result = DiaryEdits.replace(listOf(entry), entry.id, combined, grams)
        assertEquals(1, result.size)
        assertEquals(entry.id, result.single().id)
        assertEquals(1, result.single().meal)
    }
    @Test fun onePhotographedFoodKeepsBarcodeAndDatabaseIdentity() {
        assertEquals(food to 75.0, ReplacementFoods.combine(listOf(food to 75.0), "unused"))
        assertThrows(IllegalArgumentException::class.java) { ReplacementFoods.combine(emptyList(), "bad") }
    }
    @Test fun newConfigurationAndOldBackupsRemainCompatible() {
        val json = Json { encodeDefaults = true }
        val state = AppState(trainingNutrition = n, dietPrefs = MealSchedule.withMeals(DietPrefs(), MealSchedule.order))
        assertEquals(state, json.decodeFromString(AppState.serializer(), json.encodeToString(AppState.serializer(), state)))
        assertFalse(json.decodeFromString(AppState.serializer(), "{}").trainingNutrition.automatic)
    }
}
