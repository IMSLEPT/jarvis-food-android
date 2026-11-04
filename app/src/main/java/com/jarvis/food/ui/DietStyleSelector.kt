package com.jarvis.food.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Store
import com.jarvis.food.engine.DietStyles
import com.jarvis.food.data.Db
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DietStyleDialog(s: AppState, date: LocalDate, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(Store.prefsFor(s, date).styleId) }
    var days by remember { mutableIntStateOf(1) }
    var maxMinutes by remember { mutableIntStateOf(s.dietPrefs.maxMinutes.coerceAtLeast(5)) }
    var chooseTime by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val compactOptions = LocalConfiguration.current.screenWidthDp / LocalDensity.current.fontScale < 400
    val energy = Store.energyFor(s, date)
    val targetProblem = (0 until days).firstNotNullOfOrNull { offset ->
        val e = Store.energyFor(s, date.plusDays(offset.toLong()))
        DietStyles.targetProblem(selected, e.macro, e.expectedTdee)
    }
    val availability = remember(selected, maxMinutes, s.dietPrefs) {
        DietStyles.availability(Db.db, s.dietPrefs.copy(maxMinutes = maxMinutes), selected)
    }
    val problem = targetProblem ?: availability.message(DietStyles.get(selected).title, maxMinutes)
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(horizontal = 14.dp, vertical = 24.dp).widthIn(max = 560.dp).fillMaxWidth()
            .height((LocalConfiguration.current.screenHeightDp * .86f).dp), shape = RoundedCornerShape(30.dp), color = C.Surface, shadowElevation = 8.dp) {
            Column(Modifier.padding(18.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("diet-style-dialog"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Immagine 2: intestazione con ciotola su tessera pesca e separatore arancio.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PeachTile(62.dp, radius = 18.dp) { DuoIcon(RIc.BowlSteam, "", 36.dp, stroke = 2.3f, ink = RD.Orange) }
                    GapW(14.dp); OrangeDivider(50.dp); GapW(14.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Alimentazione del piano", style = T.Title.copy(fontSize = 23.sp, lineHeight = 28.sp, color = RD.Ink))
                        Text("Dal ${fullDay(date).lowercase()}", style = T.Body.copy(fontSize = 16.sp, lineHeight = 21.sp, color = RD.Grey))
                    }
                }
                GapH(2.dp)
                SectionTitle(RIc.Clock, "Per quanto tempo?")
                listOf(1 to "Giorno", 7 to "7 giorni", 14 to "14 giorni", 30 to "30 giorni").chunked(if (compactOptions) 1 else 2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (n, label) ->
                            RefOption(label, Modifier.weight(1f), tag = "diet-period-$n", isSelected = days == n, selectedStyle = OptionStyle.Navy, enabled = !busy, minHeight = 58.dp,
                                icon = { PeriodCalendarIcon() }) { days = n; error = null }
                        }
                    }
                }
                RefOption("Tempo per pasto: $maxMinutes min", Modifier.fillMaxWidth(), tag = "diet-time-toggle", enabled = !busy, minHeight = 58.dp,
                    icon = { Box(Modifier.size(30.dp)) { AIcon(RIc.Stopwatch, RD.Ink, 30.dp, 2.1f); AIcon(RIc.StopwatchHand, RD.Orange, 30.dp, 2.4f); AIcon(RIc.StopwatchTicks, RD.Ink, 30.dp, 1.4f) } },
                    trailing = { ChevronCircle(40.dp, up = chooseTime) }) { chooseTime = !chooseTime }
                if (chooseTime) {
                    (listOf(5, 10, 15, 20, 25, 30, 40, 60) + maxMinutes).distinct().sorted().chunked(if (compactOptions) 2 else 3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { n -> RefOption("$n min", Modifier.weight(1f), tag = "diet-time-$n", isSelected = maxMinutes == n, selectedStyle = OptionStyle.Navy,
                                enabled = !busy, minHeight = 50.dp, centered = true, trailing = {}) { maxMinutes = n; error = null } }
                        }
                    }
                    Text("Il tempo scelto viene salvato solo con la conferma e vale anche per i prossimi piani.", style = T.Small)
                }
                SectionTitle(RIc.Leaf, "Scegli l’alimentazione", leaf = true)
                DietStyles.options.forEach { option ->
                    RefOption(option.title, Modifier.fillMaxWidth(), tag = "diet-style-${option.id}", isSelected = selected == option.id,
                        subtitle = if (selected == option.id) option.description else null, enabled = !busy, minHeight = 60.dp,
                        icon = { DietIcon(option.id) }) { selected = option.id; error = null }
                    if (selected == option.id && problem != null) {
                        Text(problem, style = T.Small.copy(color = C.Danger))
                        if (targetProblem == null && availability.requiredMinutes != null) {
                            RefOption("Usa almeno ${availability.requiredMinutes} min per pasto", Modifier.fillMaxWidth(), tag = "diet-style-use-required-time", enabled = !busy, centered = true, trailing = {}) {
                                maxMinutes = maxOf(maxMinutes, availability.requiredMinutes); error = null
                            }
                        }
                    }
                    if (selected == option.id && selected == "healthy") {
                        Text("È una selezione più mirata degli ingredienti rispetto a Bilanciata e varia. Non garantisce da sola la copertura di tutti i nutrienti né una salute ottimale.", style = T.Small)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RefOption("OMS", Modifier.weight(1f), centered = true, minHeight = 48.dp, trailing = {}) { runCatching { uri.openUri("https://www.who.int/news-room/fact-sheets/detail/healthy-diet") } }
                            RefOption("CREA", Modifier.weight(1f), centered = true, minHeight = 48.dp, trailing = {}) { runCatching { uri.openUri("https://sapermangiare.an.crea.gov.it/483/linee-guida.html") } }
                        }
                    }
                }
                Column {
                    RefOption(if (details) "Nascondi obiettivi e dettagli" else "Obiettivi e dettagli", Modifier.fillMaxWidth(), centered = true, minHeight = 50.dp, trailing = {}) { details = !details }
                    if (details) {
                        GapH(8.dp)
                        Text("Target invariati", style = T.Label)
                        Text("${energy.targetKcal.r0()} kcal · P ${energy.protein.r0()} g · C ${energy.carbs.r0()} g · G ${energy.fat.r0()} g", style = T.Small)
                        Text("Ogni giorno mantiene i propri target, compresi i giorni ON/OFF. Tolleranze del piano: kcal ±10%, macro ±15%. Se non si possono rispettare, il piano resta invariato.", style = T.Small)
                    }
                    GapH(8.dp)
                    Text("Il diario e i pasti già registrati si conservano. La conferma aggiorna soltanto il periodo scelto e ricorda lo stile per i prossimi piani.", style = T.Small)
                    (error ?: problem)?.let { GapH(8.dp); Text(it, Modifier.testTag("diet-style-error"), style = T.Small.copy(color = C.Danger)) }
                }
            }
                    GapH(12.dp)
                    RefPrimaryButton(if (busy) "Calcolo…" else "Conferma e aggiorna", Modifier.fillMaxWidth(), icon = RIc.Refresh, tag = "diet-style-confirm", enabled = !busy && problem == null) {
                        busy = true; error = null
                        scope.launch {
                            val result = try { Store.regenerateStyle(date, days, selected, maxMinutes) }
                            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; "Calcolo non riuscito. Il piano resta invariato: riprova." }
                            finally { busy = false }
                            if (result == null) onDismiss() else error = result
                        }
                    }
                    GapH(10.dp)
                    RefOutlineButton("Annulla", Modifier.fillMaxWidth(), icon = RIc.Close, enabled = !busy, onClick = onDismiss)
                }
            }
        }
    }

@Composable
private fun SectionTitle(icon: String, text: String, leaf: Boolean = false) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leaf) Box(Modifier.size(28.dp)) { AIcon(icon, RD.Orange, 28.dp, 2.4f) } else AIcon(icon, RD.Orange, 28.dp, 2.2f)
        GapW(12.dp)
        Text(text, style = T.Title.copy(fontSize = 20.sp, lineHeight = 25.sp, color = RD.Ink))
    }
}

/** Calendario navy con griglia arancio (immagine 2). */
@Composable
private fun PeriodCalendarIcon() {
    Box(Modifier.size(30.dp)) {
        AIcon(RIc.Calendar, RD.Ink, 30.dp, 2.1f)
        AIcon(Ic.rr(3.5, 5.0, 17.0, 5.0, 2.5), RD.Orange, 30.dp, fill = true)
        AIcon(RIc.CalendarDots, RD.Orange, 30.dp, fill = true)
    }
}

/** Icona bicolore per ogni alimentazione, nello stile delle prime cinque dell'immagine 2. */
@Composable
fun DietIcon(id: String, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val green = RD.Green
    Box(Modifier.size(size)) {
        when (id) {
            "balanced" -> { AIcon("M3.5 12.5h17a8.5 8.5 0 0 1-17 0z", RD.Ink, size, 2.1f); AIcon("M12 11c-3-.4-4.6-2.6-4.4-5.4 2.8.1 4.5 2.2 4.4 5.4zM12 11c3-.4 4.6-2.6 4.4-5.4-2.8.1-4.5 2.2-4.4 5.4z", RD.Orange, size, fill = true) }
            "healthy" -> { AIcon("M5 19c0-8.5 5-13.5 14.5-14 .4 9.6-4.6 14.6-14.5 14z", Color(0xFFCFE3C9), size, fill = true); AIcon(RIc.Leaf, RD.Ink, size, 2f) }
            "mediterranean" -> { AIcon("M4 20c4-4 8-9 15-15M9 14.5c-2.5-1-3.5-3.4-3-5.5 2.2.4 3.6 2.6 3 5.5zM14 9.5c-.5-2.6.8-4.8 3-5.3.4 2.2-.8 4.6-3 5.3z", RD.Ink, size, 2f); AIcon(Ic.circ(9.0, 18.0, 2.4) + Ic.circ(15.5, 15.0, 2.4), RD.Orange, size, fill = true) }
            "omnivore" -> { AIcon("M14.5 3.5a6 6 0 0 1 6 6c0 3.6-3.6 6.6-7.2 5.6L9 19.4", RD.Ink, size, 2f); AIcon("M14.5 3.5a6 6 0 0 1 6 6c0 3.6-3.6 6.6-7.2 5.6-1.5-.4-2.8-1.6-3.3-3.1-1-3.7 1.6-8.5 4.5-8.5z", RD.Orange, size, fill = true); AIcon(Ic.circ(6.5, 20.0, 1.8) + Ic.circ(4.0, 17.5, 1.8), RD.Ink, size, 2f) }
            "vegetarian" -> { AIcon("M12 21v-9M12 12c0-4-3-6.5-7.5-6.5 0 4 3 6.5 7.5 6.5zM12 12c0-4 3-6.5 7.5-6.5 0 4-3 6.5-7.5 6.5z", RD.Ink, size, 2f); AIcon("M12 12c0-4 3-6.5 7.5-6.5 0 4-3 6.5-7.5 6.5z", Color(0xFFCFE3C9), size, fill = true) }
            "vegan" -> { AIcon("M4.5 5l7.5 15 7.5-15", RD.Ink, size, 2.2f); AIcon("M14 7c.3-2.7 2.4-4.2 5-4-.2 2.6-2.3 4.1-5 4z", green, size, fill = true) }
            "pescatarian" -> { AIcon("M3 12c3-4.5 9-6 14-2l4-3v10l-4-3c-5 4-11 2.5-14-2z", RD.Ink, size, 2f); AIcon(Ic.circ(8.0, 11.0, 1.2), RD.Orange, size, fill = true) }
            "japanese" -> { AIcon("M4 13h16a8 8 0 0 1-16 0z", RD.Ink, size, 2f); AIcon("M8 10L19 3M11 10l10-5", RD.Orange, size, 2.2f) }
            "keto" -> { AIcon("M12 3c3.8 0 6.5 4.3 6.5 9.5S15.8 21 12 21s-6.5-3.3-6.5-8.5S8.2 3 12 3z", RD.Ink, size, 2f); AIcon(Ic.circ(12.0, 14.0, 3.0), RD.Orange, size, fill = true) }
            "lowcarb" -> { AIcon("M5 10a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v9H5zM8 10v9", RD.Ink, size, 2f); AIcon("M4 20L20 4", RD.Orange, size, 2.4f) }
            "paleo" -> { AIcon("M7 8.5a2.5 2.5 0 1 1 2-4 2.5 2.5 0 1 1 3.5 3l6 6a2.5 2.5 0 1 1 1.5 4 2.5 2.5 0 1 1-4 1.5l-6-6a2.5 2.5 0 0 1-3-4.5z", RD.Ink, size, 2f); AIcon(Ic.circ(12.0, 12.0, 1.4), RD.Orange, size, fill = true) }
            "dash" -> { AIcon(RIc.Heart, RD.Ink, size, 2f); AIcon("M5 12h3l2-3 3 6 2-3h4", RD.Orange, size, 2.2f) }
            "hypocaloric" -> { AIcon(RIc.Flame, RD.Ink, size, 2f); AIcon("M12 12v6M9.5 15.5L12 18l2.5-2.5", RD.Orange, size, 2.2f) }
            "highprotein" -> { AIcon(RIc.Dumbbell, RD.Ink, size, 2f); AIcon(RIc.DumbbellBar, RD.Orange, size, 2.6f) }
            "carnivore" -> { AIcon("M5 9c2-4 9-5 13-2 3 2.3 2.4 7-1 9-4 2.4-10 2.6-12-1-.8-1.6-.8-3.8 0-6z", RD.Ink, size, 2f); AIcon(Ic.circ(14.5, 11.5, 2.2), RD.Orange, size, fill = true) }
            "glutenfree" -> { AIcon("M12 21V8M12 12c-2.5-.5-4-2.4-4-4.6 2.5.3 4 2.2 4 4.6zM12 12c2.5-.5 4-2.4 4-4.6-2.5.3-4 2.2-4 4.6zM12 16.5c-2.5-.5-4-2.4-4-4.6 2.5.3 4 2.2 4 4.6zM12 16.5c2.5-.5 4-2.4 4-4.6-2.5.3-4 2.2-4 4.6zM12 7.5V3", RD.Ink, size, 1.8f); AIcon("M4 20L20 4", RD.Orange, size, 2.4f) }
            "lactosefree" -> { AIcon("M8 3h8M8.5 3v3L6 9.5V21h12V9.5L15.5 6V3M6 13h12", RD.Ink, size, 2f); AIcon("M4 20L20 4", RD.Orange, size, 2.4f) }
            else -> { AIcon(RIc.Bowl, RD.Ink, size, 2f); AIcon(RIc.Steam, RD.Orange, size, 2f) }
        }
    }
}
