package com.jarvis.food

import com.jarvis.food.chat.Actions
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatProtocolTest {
    @Test fun parsesActionAndQuickReplies() {
        val raw = """Segno 150 g di pollo a pranzo.
<action>{"name":"log_food","args":{"food":"pollo","grams":150,"meal":"pranzo"}}</action>
<quick>Aggiungi contorno</quick><quick>Basta così</quick>"""
        val p = Actions.parse(raw)
        assertEquals("Segno 150 g di pollo a pranzo.", p.text)
        assertEquals(1, p.actions.size)
        assertEquals("log_food", p.actions[0].name)
        assertEquals(150, p.actions[0].args.getInt("grams"))
        assertEquals(listOf("Aggiungi contorno", "Basta così"), p.quick)
    }

    @Test fun toleratesCodeFencesAndMultipleActions() {
        val raw = "Fatto.\n<action>```json\n{\"name\":\"add_water\",\"args\":{\"ml\":500}}\n```</action><action>{\"name\":\"log_weight\",\"args\":{\"kg\":79.4}}</action>"
        val p = Actions.parse(raw)
        assertEquals(listOf("add_water", "log_weight"), p.actions.map { it.name })
        assertEquals("Fatto.", p.text)
    }

    @Test fun ignoresBrokenJsonAndHidesPartialBlocksWhileStreaming() {
        assertTrue(Actions.parse("Ok <action>{non json}</action>").actions.isEmpty())
        assertEquals("Segno", Actions.visible("Segno <action>{\"name\":\"log_f"))
        assertEquals("Quanta?", Actions.visible("Quanta?<quick>100 g</quick>"))
    }

    @Test fun macroAdviceFollowsProteinPerKg() {
        // Dimagrire a 80 kg con 2550 kcal: il Bilanciato dà solo 1,6 g/kg, serve il Proteico.
        val cut = Profile(male = true, birthYear = 1997, heightCm = 178.0, startWeight = 80.0, targetWeight = 76.0, goal = -1, weeklyRate = 0.5)
        val a = Nutrition.macroAdvice(cut, 80.0, 2550.0)
        assertEquals(1, a.best)
        assertTrue(a.perKg[0] < 1.7 && a.perKg[1] > 2.2)
        // Mantenimento con calorie alte: il Bilanciato basta.
        val keep = cut.copy(goal = 0, workouts = 3, detailed = true)
        assertEquals(0, Nutrition.macroAdvice(keep, 80.0, 3000.0).best)
        // Preferenza low carb senza allenamenti intensi: si rispetta.
        assertEquals(2, Nutrition.macroAdvice(keep.copy(dietStyle = 2, workouts = 2), 80.0, 3000.0).best)
    }

    @Test fun questionnaireMakesTdeeMoreSpecific() {
        val base = Profile(male = true, birthYear = 1997, heightCm = 178.0, startWeight = 80.0, detailed = true, job = 0, stepsLevel = 0, workouts = 0)
        val sedentary = Nutrition.tdee(base, 80.0)
        val active = Nutrition.tdee(base.copy(job = 2, stepsLevel = 3, workouts = 4, workoutMin = 60, workoutIntensity = 2), 80.0)
        assertTrue(active > sedentary * 1.25)
        // Katch-McArdle: con la massa grassa il basale cambia rispetto a Mifflin.
        assertTrue(Nutrition.bmr(base.copy(bodyFatPct = 15.0), 80.0) != Nutrition.bmr(base, 80.0))
    }
}
