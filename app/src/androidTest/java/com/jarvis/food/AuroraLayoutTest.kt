package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Profile
import com.jarvis.food.data.Store
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Executed on real emulator configurations, including Android system font scaling. */
@RunWith(AndroidJUnit4::class)
class AuroraLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private val profile get() = InstrumentationRegistry.getArguments().getString("captureName") ?: "default"

    private fun shot(name: String) {
        compose.waitForIdle()
        ins.waitForIdleSync()
        // Compose's test clock can finish before SurfaceFlinger presents the new frame.
        android.os.SystemClock.sleep(500)
        val bitmap = ins.uiAutomation.takeScreenshot()
        val dir = File(ins.targetContext.filesDir, "shots/layout-$profile").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun readable(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Text layout missing", results.isNotEmpty())
        results.forEach { result ->
            // The semantics paragraph can retain the parent's maximum width even when Text
            // measures to its content width. Compare actual line widths, not the paragraph box.
            // Allow only single-pixel rounding, never ellipsis or a missing line.
            val paragraph = result.multiParagraph
            val details = "${result.layoutInput.text.text}: box=${result.size}, paragraph=${paragraph.width}x${paragraph.height}, lines=${result.lineCount}"
            assertFalse("Ellipsized text: $details", (0 until result.lineCount).any { result.isLineEllipsized(it) })
            assertFalse("Missing text lines: $details", paragraph.didExceedMaxLines)
            val widestLine = (0 until result.lineCount).maxOfOrNull { result.getLineRight(it) - result.getLineLeft(it) } ?: 0f
            assertTrue("Clipped text width ($widestLine): $details", widestLine <= result.size.width + 1f)
            assertTrue("Clipped text height: $details", paragraph.height <= result.size.height + 1f)
        }
    }

    // Measure the visible drawing, so enlarging only a button or empty canvas cannot pass.
    private fun visibleIcon(tag: String, tint: Color, minimumInkWidthDp: Float) {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
        node.assertIsDisplayed()
        val bitmap = node.captureToImage()
        val pixels = bitmap.toPixelMap()
        var left = bitmap.width
        var right = -1
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val c = pixels[x, y]
            if (kotlin.math.abs(c.red - tint.red) < 0.04f && kotlin.math.abs(c.green - tint.green) < 0.04f && kotlin.math.abs(c.blue - tint.blue) < 0.04f) {
                left = minOf(left, x); right = maxOf(right, x)
            }
        }
        val density = ins.targetContext.resources.displayMetrics.density
        assertTrue("Visible icon too small: $tag, ${(right - left + 1) / density} dp", right >= left && (right - left + 1) / density >= minimumInkWidthDp)
    }

    private fun chatHeaderHasAccountControlsAboveActions() {
        val family = compose.onNodeWithContentDescription("Famiglia").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val profile = compose.onNodeWithContentDescription("Apri profilo").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val fresh = compose.onNodeWithContentDescription("Nuova chat").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val engine = compose.onNodeWithContentDescription("Motore della chat").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Chat actions overlap account controls", minOf(fresh.top, engine.top) >= maxOf(family.bottom, profile.bottom))
        assertTrue("New chat is not below Family", kotlin.math.abs(fresh.center.x - family.center.x) <= 1f)
        assertTrue("Engine is not below Profile", kotlin.math.abs(engine.center.x - profile.center.x) <= 1f)
    }

    @Test fun barsGreetingsAndLongActionsRemainVisible() {
        Store.init(ins.targetContext)
        // Geometry tests use the real catalogue, precomputed before opening the activity.
        // The separate navigation regression covers the asynchronous category-loading path.
        check(com.jarvis.food.engine.RecipeCats.get(com.jarvis.food.data.Db.db).isNotEmpty())
        val day = LocalDate.now()
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro")) }
            Store.generatePlan(day.with(DayOfWeek.MONDAY), 7)
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day; Nav.kitchenTab = 2
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            var navigationBottom = 0
            var windowHeight = 0
            scenario.onActivity { activity ->
                navigationBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                windowHeight = activity.window.decorView.height
            }
            compose.waitForIdle()
            shot("home-before-check")
            for (label in listOf("Foto piatto", "Frigo", "Digiuno")) visibleIcon("quick-icon-$label", C.Navy, 22f)
            shot("home-enlarged-icons")
            compose.onNodeWithContentDescription("Famiglia").assertWidthIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performClick()
            compose.waitForIdle()
            assertEquals(Screen.Family, Nav.stack.last())
            ui { Nav.back() }
            compose.onNodeWithContentDescription("Apri profilo").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performClick()
            compose.waitForIdle()
            assertEquals(Screen.Profile, Nav.stack.last())
            ui { Nav.back() }
            ui { Nav.clear(); Nav.tab = 2; Nav.kitchenTab = 0 }
            compose.waitForIdle()
            visibleIcon("scan-icon-Fotografa frigo o dispensa", C.Navy, 24f)
            visibleIcon("scan-icon-Fotografa lo scontrino", C.Accent, 24f)
            shot("pantry-enlarged-icons")
            ui { Nav.clear(); Nav.tab = 0; Nav.kitchenTab = 2 }
            compose.waitForIdle()
            readable(compose.onNodeWithTag("home-greeting"))
            val greeting = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("home-greeting").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(greeting) }
            assertTrue("Greeting must be readable at normal size", greeting.single().layoutInput.style.fontSize.value >= 16f)
            for ((tab, label) in listOf(0 to "Oggi", 1 to "Piano", 2 to "Cucina", 3 to "Progressi", 5 to "Jarvis")) {
                compose.onNodeWithTag("nav-$tab").performClick().assertIsSelected()
                compose.waitForIdle()
                val bar = compose.onNodeWithTag("bottom-bar").fetchSemanticsNode().boundsInRoot
                assertTrue("Bar overlaps Android navigation", bar.bottom <= windowHeight - navigationBottom + 1f)
                val text = compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("bottom-bar")), useUnmergedTree = true)
                readable(text)
                val navText = mutableListOf<TextLayoutResult>()
                text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(navText) }
                assertEquals("Navigation label must stay on one line", 1, navText.single().lineCount)
                compose.onNodeWithTag("nav-$tab").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
                if (tab != 5) {
                    val scroll = compose.onNodeWithTag("main-scroll").fetchSemanticsNode().boundsInRoot
                    assertTrue("Content overlaps bottom bar", scroll.bottom <= bar.top + 1f)
                }
                shot("tab-$tab")
            }
            // A synthetic non-empty key enables the local input UI. No message is sent.
            ui { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1, apiKey = "layout-test-no-network")) } }
            chatHeaderHasAccountControlsAboveActions()
            shot("chat-empty-account-controls")
            compose.onNodeWithContentDescription("Famiglia").performClick()
            compose.waitForIdle()
            assertEquals(Screen.Family, Nav.stack.last())
            ui { Nav.back() }
            compose.onNodeWithContentDescription("Apri profilo").performClick()
            compose.waitForIdle()
            assertEquals(Screen.Profile, Nav.stack.last())
            ui { Nav.back() }
            val answer = "Per mantenere il tuo peso attuale, il piano di oggi è bilanciato con 275 g di carboidrati, 114 g di proteine e 64 g di grassi. Se preferisci un approccio diverso, posso cambiare le macro."
            ui { Store.update { it.copy(chat = listOf(
                com.jarvis.food.data.ChatMsg("layout-question", "user", "Che macro mi conviene?", System.currentTimeMillis() - 1000),
                com.jarvis.food.data.ChatMsg("layout-answer", "assistant", answer, System.currentTimeMillis(), suggestions = listOf("Sì, applica", "No"))
            )) } }
            chatHeaderHasAccountControlsAboveActions()
            compose.onNodeWithContentDescription("Nuova chat").assertWidthIsAtLeast(androidx.compose.ui.unit.Dp(48f))
            compose.onNodeWithContentDescription("Motore della chat").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
            compose.onNodeWithText(answer, useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText(answer, useUnmergedTree = true))
            compose.onNodeWithText("Sì, applica", useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText("Sì, applica", useUnmergedTree = true))
            readable(compose.onNodeWithText("No", useUnmergedTree = true))
            val responseBottom = compose.onNodeWithText(answer, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom
            val suggestionTop = compose.onNodeWithTag("chat-suggestions").fetchSemanticsNode().boundsInRoot.top
            assertTrue("Quick replies overlap their response", suggestionTop >= responseBottom)
            compose.onNodeWithContentDescription("Invia messaggio", useUnmergedTree = true).assertIsNotEnabled()
            shot("chat-conversation")
            compose.onNodeWithContentDescription("Motore della chat").performClick()
            compose.onNodeWithText("Motore di Jarvis").assertIsDisplayed()
            compose.onNodeWithContentDescription("Chiudi impostazioni motore").performClick()
            compose.onNodeWithContentDescription("Nuova chat").performClick()
            compose.waitForIdle()
            assertTrue("New chat did not clear the previous conversation", Store.current.chat.isEmpty())
            compose.onNodeWithTag("chat-input").performClick().performTextInput("Prova layout")
            compose.onNodeWithTag("chat-input").assertIsFocused()
            compose.waitForIdle()
            // Request the software keyboard explicitly: instrumentation text injection
            // alone does not guarantee an IME opens on every emulator cold start.
            scenario.onActivity { activity ->
                ViewCompat.getWindowInsetsController(activity.window.decorView)?.show(WindowInsetsCompat.Type.ime())
                val inputMethod = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                @Suppress("DEPRECATION")
                inputMethod.showSoftInput(activity.currentFocus ?: activity.window.decorView, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
            }
            var imeBottom = 0
            try { compose.waitUntil(20_000) {
                scenario.onActivity { activity ->
                    imeBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                }
                imeBottom > 0 && compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().isEmpty()
            } } catch (failure: Throwable) {
                println("IME diagnostic: bottom=$imeBottom, bottomBars=${compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().size}, tab=${Nav.tab}")
                scenario.onActivity { activity ->
                    val inputMethod = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    println("IME diagnostic: windowFocus=${activity.hasWindowFocus()}, viewFocus=${activity.currentFocus}, active=${inputMethod.isActive}, accepting=${inputMethod.isAcceptingText}")
                }
                shot("chat-keyboard-failure")
                throw failure
            }
            android.os.SystemClock.sleep(500)
            compose.waitForIdle()
            scenario.onActivity { activity ->
                imeBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            compose.onNodeWithTag("bottom-bar").assertDoesNotExist()
            val input = compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot
            assertTrue("Chat input overlaps keyboard", input.bottom <= windowHeight - imeBottom + 1f)
            shot("chat-keyboard")
            ui { Store.update { it.copy(chat = listOf(
                com.jarvis.food.data.ChatMsg("keyboard-question", "user", "Che macro mi conviene?", System.currentTimeMillis() - 1000),
                com.jarvis.food.data.ChatMsg("keyboard-answer", "assistant", answer, System.currentTimeMillis(), suggestions = listOf("Sì, applica", "No"))
            )) } }
            compose.waitForIdle()
            compose.onNodeWithTag("chat-input").assertIsFocused()
            compose.onNodeWithContentDescription("Invia messaggio", useUnmergedTree = true).assertIsEnabled()
            val activeInput = compose.onNodeWithTag("chat-input").fetchSemanticsNode().boundsInRoot
            assertTrue("Active conversation input overlaps keyboard", activeInput.bottom <= windowHeight - imeBottom + 1f)
            compose.onNodeWithTag("bottom-bar").assertDoesNotExist()
            shot("chat-conversation-keyboard")
            ins.uiAutomation.executeShellCommand("input keyevent 4").close()
            compose.waitUntil(20_000) {
                scenario.onActivity { activity ->
                    imeBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                }
                imeBottom == 0 && compose.onAllNodesWithTag("bottom-bar").fetchSemanticsNodes().isNotEmpty()
            }
            ui { Store.update { it.copy(chat = emptyList(), ai = it.ai.copy(apiKey = "")) } }
            ui { Nav.tab = 0 }
            compose.onNodeWithContentDescription("Apri profilo").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("bottom-bar").assertDoesNotExist()
            readable(compose.onNodeWithText("Mantenere", useUnmergedTree = true))
            readable(compose.onNodeWithText("Calorie al giorno", useUnmergedTree = true))
            shot("profile")
            ui { Nav.clear() }
            ui { Nav.tab = 1 }
            compose.onNodeWithText("Crea lista della spesa").performScrollTo()
            readable(compose.onNodeWithText("Crea lista della spesa", useUnmergedTree = true))
            shot("plan-bottom")
            val first = Store.plannedMeals(Store.current, day).first()
            ui { Nav.go(Screen.RecipeView(first.solved.template.id, first.solved.target, day, first.slotIndex, first.diaryMeal)) }
            compose.waitForIdle()
            var content = compose.onNodeWithTag("recipe-scroll").fetchSemanticsNode().boundsInRoot
            var footer = compose.onNodeWithTag("recipe-footer").fetchSemanticsNode().boundsInRoot
            assertTrue("Recipe footer covers content", content.bottom <= footer.top + 1f)
            readable(compose.onNodeWithText("Segna mangiato", useUnmergedTree = true))
            shot("recipe-planned")
            ui { Nav.clear(); Nav.go(Screen.RecipeView(first.solved.template.id, first.solved.target, day, diaryMeal = 4, original = true)) }
            compose.waitForIdle()
            content = compose.onNodeWithTag("recipe-scroll").fetchSemanticsNode().boundsInRoot
            footer = compose.onNodeWithTag("recipe-footer").fetchSemanticsNode().boundsInRoot
            assertTrue("Recipe footer covers content with long action", content.bottom <= footer.top + 1f)
            readable(compose.onNodeWithText("＋ Aggiungi a ${MEAL_LABEL.getValue(4).lowercase()}", useUnmergedTree = true))
            shot("recipe-long-action")
            ui { Nav.clear(); Nav.go(Screen.RecipeView("porridge_banana", first.solved.target, day, diaryMeal = 0, original = true)) }
            compose.onNodeWithTag("recipe-step-0").performScrollTo()
            compose.onNodeWithTag("recipe-step-photo-0", useUnmergedTree = true).assertIsDisplayed()
            val photoTop = compose.onNodeWithTag("recipe-step-photo-0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
            val toolbarBottom = compose.onNodeWithTag("recipe-toolbar").fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("Recipe toolbar covers the preparation photo", photoTop >= toolbarBottom)
            compose.onNodeWithText("Ricetta", useUnmergedTree = true).assertExists()
            readable(compose.onNodeWithText("Passaggio 1", useUnmergedTree = true))
            readable(compose.onNode(hasText("in un pentolino antiaderente", substring = true), useUnmergedTree = true))
            shot("recipe-photography")
            compose.onNodeWithTag("recipe-step-0").performClick()
            compose.onNodeWithText("✓ Passaggio 1", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("recipe-step-3").performScrollTo()
            compose.onNodeWithTag("recipe-step-photo-3", useUnmergedTree = true).assertIsDisplayed()
            shot("recipe-photography-last")
            ui { Nav.clear(); Nav.tab = 2; Nav.kitchenTab = 2 }
            // Recipe groups are loaded on a background dispatcher after navigation settles.
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Mostra tutte").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Mostra tutte").performScrollTo().performClick()
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText("Vegana", useUnmergedTree = true))
            shot("categories")

            // The approved profile must keep every destination readable at both font scales.
            ui { Nav.clear(); Nav.go(Screen.Profile) }
            compose.onNodeWithContentDescription("Modifica profilo").assertWidthIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performClick()
            compose.waitForIdle()
            assertTrue("Profile edit action changed destination", Nav.stack.last() is Screen.ProfileEdit)
            ui { Nav.back() }
            compose.waitForIdle()
            readable(compose.onNodeWithText("Dati e obiettivi", useUnmergedTree = true).performScrollTo())
            shot("profile-overview")
            for (key in listOf("profile", "weight", "measurements", "goals", "diet", "chat", "watch", "ai", "fasting", "reminders", "foods", "backup", "about")) {
                val tag = "profile-item-$key"
                val card = compose.onNodeWithTag(tag).performScrollTo()
                card.assertHasClickAction().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
                val texts = compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                assertTrue("Missing title or description for $key", texts.fetchSemanticsNodes().size >= 2)
                for (index in texts.fetchSemanticsNodes().indices) readable(texts[index])
            }
            shot("profile-tools")
            val profileScroll = compose.onNodeWithTag("profile-scroll").fetchSemanticsNode().boundsInRoot
            assertTrue("Profile content overlaps Android navigation", profileScroll.bottom <= windowHeight - navigationBottom + 1f)
            val profileRoutes = listOf(
                "profile" to Screen.ProfileEdit, "weight" to Screen.Weight,
                "measurements" to Screen.Measurements, "goals" to Screen.Goals,
                "diet" to Screen.DietSettings, "chat" to Screen.Chat,
                "watch" to Screen.Watch, "ai" to Screen.AiSettings,
                "fasting" to Screen.Fasting, "reminders" to Screen.RemindersSet,
                "foods" to Screen.MyFoods, "backup" to Screen.Backup, "about" to Screen.About
            )
            for ((key, destination) in profileRoutes) {
                compose.onNodeWithTag("profile-item-$key").performScrollTo().performClick()
                compose.waitForIdle()
                assertEquals("Wrong profile destination: $key", destination, Nav.stack.last())
                ui { Nav.back() }
                compose.waitForIdle()
            }
        }
    }

    @Test fun smallTextAndActionLabelsMeetContrast() {
        // Preserve the explicitly approved orange and white labels without recolouring the app.
        assertEquals(Color.White, C.OnOrange)
        assertEquals(C.Accent, C.ButtonOrange)
        val pairs = listOf(
            A.ProteinInk to A.ProteinSoft, A.CarbInk to A.CarbSoft,
            A.FatInk to A.FatSoft, A.GreenInk to A.GreenSoft,
            A.TonalInk to A.Tonal, A.FastInk to A.FastSoft,
            C.Navy to C.Bg, C.Muted to C.Surface
        )
        val icon = androidx.core.content.ContextCompat.getDrawable(ins.targetContext, R.drawable.ic_notification)!!
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 24, 24)
        icon.draw(android.graphics.Canvas(bitmap))
        val pixels = IntArray(24 * 24)
        bitmap.getPixels(pixels, 0, 24, 0, 0, 24, 24)
        assertTrue("Notification mask is empty or an opaque square", pixels.count { android.graphics.Color.alpha(it) > 0 } in 100..450)
        assertEquals("Notification background must be transparent", 0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
        assertTrue("Notification mask must be monochrome white", pixels.filter { android.graphics.Color.alpha(it) > 0 }.all { (it and 0x00FFFFFF) == 0x00FFFFFF })
        bitmap.recycle()
        for ((foreground, background) in pairs) {
            val ratio = ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb())
            assertTrue("Insufficient text contrast: $ratio", ratio >= 4.5)
        }
    }
}
