package com.jarvis.food

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.EnergySettings
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.PantryItem
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.data.WeightEntry
import com.jarvis.food.data.WorkoutSession
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Avvia l'app con dati dimostrativi, apre tutte le schermate principali e salva uno screenshot di ciascuna. */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val perm: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val ins = InstrumentationRegistry.getInstrumentation()
    private val dir by lazy { File(ins.targetContext.filesDir, "shots").apply { deleteRecursively(); mkdirs() } }

    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)

    private fun shot(name: String, scrolls: Int = 0) {
        compose.waitForIdle(); Thread.sleep(1200); compose.waitForIdle()
        save(name)
        repeat(scrolls) { i ->
            compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = bottom * 0.2f, durationMillis = 400) }
            compose.waitForIdle(); Thread.sleep(700)
            save("${name}_${i + 2}")
        }
    }

    private fun save(name: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun screenshots() {
        val ctx = ins.targetContext
        Store.init(ctx)
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        Store.update {
            AppState(onboarded = true,
                profile = Profile("Alessandro", true, 1997, 178.0, 80.0, 78.0, 2, 1, 0.25),
                weights = (0..20 step 4).map { WeightEntry(today.minusDays((20 - it).toLong()).toString(), 80.4 - it * 0.03) },
                energy = EnergySettings(mode = 2, watchConnected = true, lastSync = System.currentTimeMillis()),
                health = mapOf(today.toString() to HealthDay(today.toString(), 9850, 540.0, 2650.0,
                    listOf(WorkoutSession("Allenamento con i pesi", 70, System.currentTimeMillis() - 7_200_000, System.currentTimeMillis() - 3_600_000, 310.0)),
                    58.0, 72.0, 432, System.currentTimeMillis())),
                water = mapOf(today.toString() to 1250))
        }
        Store.generatePlan(monday, 14)
        // Ultimi giorni: pasti del piano "mangiati" per avere diario e micronutrienti.
        for (d in 1..4) {
            val date = today.minusDays(d.toLong())
            if (Store.dayPlan(Store.current, date) == null) Store.generatePlan(date.with(DayOfWeek.MONDAY), 7)
            Store.plannedMeals(Store.current, date).forEach { Store.eatPlanned(date, it) }
        }
        Store.plannedMeals(Store.current, today).take(2).forEach { Store.eatPlanned(today, it) }
        Store.addPantry(listOf("uova" to 300.0, "spinaci" to 400.0, "basmati" to 1000.0, "pasta" to 500.0, "pomodori" to 500.0,
            "greco0" to 500.0, "banana" to 600.0, "latte_sl" to 1000.0).map { (id, g) -> PantryItem(Store.newId(), id, Db.db.food(id)!!.name, g, source = "foto") })
        Store.buildShopping(today, 3)
        Store.saveNow()

        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.clear(); Nav.tab = 0 }; shot("01_oggi", 3)
            ui { Nav.tab = 1; Nav.planDate = today }; shot("02_piano", 3)
            val m = Store.plannedMeals(Store.current, today).first()
            ui { Nav.go(Screen.RecipeView(m.solved.template.id, m.solved.target, today, m.slotIndex, m.diaryMeal)) }; shot("03_ricetta", 4)
            ui { Nav.clear(); Nav.go(Screen.Alternatives(today, 1)) }; shot("04_alternative")
            ui { Nav.clear(); Nav.tab = 2; Nav.kitchenTab = 0 }; shot("05_dispensa", 1)
            ui { Nav.kitchenTab = 1 }; shot("06_spesa", 1)
            ui { Nav.kitchenTab = 2 }; shot("07_ricette")
            ui { Nav.go(Screen.PantryIdeas("cena")) }; shot("08_cosa_cucino")
            ui { Nav.clear(); Nav.tab = 3 }; shot("09_progressi", 2)
            ui { Nav.go(Screen.Micronutrients) }; shot("10_micronutrienti", 2)
            ui { Nav.clear(); Nav.tab = 4 }; shot("11_profilo", 1)
            ui { Nav.go(Screen.Watch) }; shot("12_smartwatch", 1)
            ui { Nav.clear(); Nav.go(Screen.EnergyInfo) }; shot("13_calcolo")
            ui { Nav.clear(); Nav.go(Screen.Fasting) }; shot("14_digiuno")
            ui { Nav.clear(); Nav.go(Screen.DietSettings) }; shot("15_preferenze")
            ui { Nav.clear(); Nav.go(Screen.AddFood(today, 1, "yogurt")) }; shot("16_cerca")
            ui { Nav.clear(); Nav.tab = 0 }
        }
    }
}
