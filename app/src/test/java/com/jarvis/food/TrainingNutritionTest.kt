package com.jarvis.food

import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class TrainingNutritionTest {
    private val monday = LocalDate.of(2026, 10, 26)
    private val on = DailyTargets(2600, 160.0, 330.0, 70.0)
    private val off = DailyTargets(2100, 150.0, 210.0, 60.0)
    private val schedule = TrainingNutrition(true, on, off, listOf(1, 3, 5))
    private fun base() = Energy.forDay(Profile(), Goals(kcal = 2000, auto = false), EnergySettings(), 75.0, null, emptyList(), 0)
    private val food = Food("a", "Pasta", kcal = 371.0, protein = 13.0, carbs = 71.0, fat = 1.5)
    private val other = Food("b", "Merluzzo", kcal = 100.0, protein = 20.0, carbs = 0.0, fat = 2.0)

    @Test fun weeklyCalendarAndDateOverridesResolveAcrossWeeks() {
        assertEquals(on, schedule.target(monday))
        assertEquals(off, schedule.target(monday.plusDays(1)))
        assertEquals(on, schedule.target(monday.plusDays(7)))
        val exception = schedule.withOverride(monday, false)
        assertEquals(off, exception.target(monday))
        assertEquals(on, exception.target(monday.plusDays(7)))
        assertEquals(on, exception.withOverride(monday, null).target(monday))
    }

    @Test fun offToOnExceptionDoesNotAlterTheRecurringWeek() {
        val tuesday = monday.plusDays(1)
        val changed = schedule.withOverride(tuesday, true)
        assertEquals(on, changed.target(tuesday))
        assertEquals(off, changed.target(tuesday.plusWeeks(1)))
        assertEquals(schedule.trainingDays, changed.trainingDays)
    }

    @Test fun disabledAndInvalidConfigurationsPreserveLegacyCalculationExactly() {
        val base = base()
        assertSame(base, TrainingNutrition().apply(base, monday))
        assertSame(base, schedule.copy(enabled = false).apply(base, monday))
        assertSame(base, schedule.copy(on = null).apply(base, monday))
        assertSame(base, schedule.copy(off = off.copy(protein = Double.NaN)).apply(base, monday))
    }

    @Test fun configuredTargetsAreExactAndDoNotDoubleCountTraining() {
        val active = base().copy(extraKcal = 400.0, targetKcal = 2400.0, activeRaw = 700.0, activeKcal = 500.0)
        val result = schedule.apply(active, monday)
        assertEquals(2600.0, result.targetKcal, 0.0)
        assertEquals(160.0, result.protein, 0.0)
        assertEquals(330.0, result.carbs, 0.0)
        assertEquals(70.0, result.fat, 0.0)
        assertEquals(0.0, result.extraKcal, 0.0)
        assertEquals(700.0, result.activeRaw, 0.0)
        assertEquals(2100.0, schedule.apply(active, monday.plusDays(1)).targetKcal, 0.0)
    }

    @Test fun oldBackupsDecodeWithFeatureDisabledAndNoDataLoss() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val old = """{"onboarded":true,"goals":{"kcal":2131,"auto":false},"diary":[],"water":{"2026-10-26":750}}"""
        val state = json.decodeFromString(AppState.serializer(), old)
        assertFalse(state.trainingNutrition.active)
        assertEquals(2131, state.goals.kcal)
        assertEquals(750, state.water[monday.toString()])
    }

    @Test fun newBackupsRoundTripScheduleTargetsAndOverrides() {
        val json = Json { encodeDefaults = true }
        val original = AppState(trainingNutrition = schedule.withOverride(monday, false))
        val restored = json.decodeFromString(AppState.serializer(), json.encodeToString(AppState.serializer(), original))
        assertEquals(original, restored)
        assertEquals(off, restored.trainingNutrition.target(monday))
    }

    @Test fun replacementPreservesIdentityDateMealOrderTimeAndPlanReference() {
        val first = DiaryEntry("entry1", monday.toString(), 1, food, 200.0, 123L, "2026-10-26#1")
        val second = first.copy(id = "entry2", meal = 2, grams = 75.0)
        val result = DiaryEdits.replace(listOf(first, second), first.id, other, 150.0)
        assertEquals(2, result.size)
        assertEquals(first.copy(food = other, grams = 150.0), result[0])
        assertEquals(second, result[1])
        assertEquals(150.0 + Nutrition.of(food, 75.0).kcal, Nutrition.sum(result).kcal, 0.001)
        assertEquals(30.0, Nutrition.of(result[0].food, result[0].grams).protein, 0.001)
        assertEquals(food, first.food) // selection/preview did not mutate the original
    }

    @Test fun replacementCannotResurrectAnEntryRemovedWhileEditing() {
        assertTrue(DiaryEdits.replace(emptyList(), "missing", other, 100.0).isEmpty())
    }

    @Test fun replacementRejectsNonFiniteAndNonPositiveQuantities() {
        for (grams in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { DiaryEdits.replace(emptyList(), "x", other, grams) }.isFailure)
        }
    }

    @Test fun weeklyPlanUsesEachDatesTargetsAndStillSolvesAgainstThem() {
        val db = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())
        val used = mutableListOf<LocalDate>()
        fun macro(d: LocalDate): Macro { val t = schedule.target(d)!!; return Macro(t.kcal.toDouble(), t.protein, t.carbs, t.fat) }
        val days = Planner.generate(db, DietPrefs(), monday, 7, macro(monday), dayTargetForDate = { d -> used += d; macro(d) })
        assertEquals((0L..6L).map { monday.plusDays(it) }, used)
        assertEquals(7, days.size)
        val onMeals = Planner.solveDay(db, days[0], macro(monday), DietPrefs())
        val offMeals = Planner.solveDay(db, days[1], macro(monday.plusDays(1)), DietPrefs())
        assertTrue(onMeals.isNotEmpty() && offMeals.isNotEmpty())
        assertTrue(onMeals.sumOf { it.solved.total.kcal } > offMeals.sumOf { it.solved.total.kcal })
    }
}
