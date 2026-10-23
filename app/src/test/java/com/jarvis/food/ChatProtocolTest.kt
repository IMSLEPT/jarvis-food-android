package com.jarvis.food

import com.jarvis.food.chat.Actions
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test fun mealsUseTheSixDiarySlots() {
        assertEquals(0, Actions.parseMeal("colazione")); assertEquals(1, Actions.parseMeal("a pranzo")); assertEquals(2, Actions.parseMeal("cena"))
        assertEquals(3, Actions.parseMeal("merenda")); assertEquals(4, Actions.parseMeal("spuntino mattutino")); assertEquals(5, Actions.parseMeal("spuntino serale"))
        assertEquals(4, Actions.parseMeal("spuntino", hour = 10)); assertEquals(3, Actions.parseMeal("spuntino", hour = 16)); assertEquals(5, Actions.parseMeal("spuntino", hour = 21))
        assertNull(Actions.parseMeal("boh")); assertNull(Actions.parseMeal(null))
        assertEquals(0, Actions.mealIndex(null, hour = 8)); assertEquals(1, Actions.mealIndex("", hour = 13))
    }

    @Test fun flagsTextThatClaimsUnexecutedActions() {
        assertTrue(Actions.claimsDone("Registrato.")); assertTrue(Actions.claimsDone("Ho aggiunto il pollo al pranzo."))
        assertTrue(Actions.claimsDone("Ok, fatto!")); assertTrue(Actions.claimsDone("Ho eliminato la cena di ieri"))
        assertFalse(Actions.claimsDone("Il tuo obiettivo di oggi è 2500 kcal e te ne restano 1300."))
        assertFalse(Actions.claimsDone("Vuoi che registri il pranzo?")); assertFalse(Actions.claimsDone("Quanta pasta hai mangiato?"))
    }

    @Test fun actionDocsFollowTheRequest() {
        assertTrue(Actions.actionDocs("elimina il pollo di pranzo").contains("delete_entry"))
        assertFalse(Actions.actionDocs("ho mangiato 150 g di pollo a pranzo").contains("delete_entry"))
        assertTrue(Actions.actionDocs("cerca una cena vegetariana sotto 500 kcal").contains("find_recipes"))
        assertTrue(Actions.actionDocs("metti le uova nella spesa").contains("add_shopping"))
        assertTrue(Actions.actionDocs("avvia il digiuno 16:8").contains("start_fast"))
        // dopo un errore su un'azione non documentata, il modello rivede la documentazione giusta
        assertTrue(Actions.actionDocs("NON riuscita: Azione sconosciuta: create_recipe").contains("create_recipe"))
    }

    @Test fun deletionsAlwaysNeedConfirmation() {
        listOf("delete_entry", "clear_meal", "delete_recipe", "delete_weight", "delete_activity", "remove_pantry", "remove_shopping")
            .forEach { assertTrue(it, it in Actions.DESTRUCTIVE) }
    }
}
