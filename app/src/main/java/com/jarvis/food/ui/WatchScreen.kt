package com.jarvis.food.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Store
import com.jarvis.food.health.HealthSync
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(HealthSync.status(ctx)) }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun sync() {
        busy = true
        scope.launch {
            msg = runCatching { HealthSync.sync(ctx) }.getOrElse { "Errore: ${it.message}" }
            granted = runCatching { HealthSync.granted(ctx) }.getOrDefault(emptySet())
            busy = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(HealthSync.permissionContract()) { res ->
        granted = res
        if (res.any { it in HealthSync.PERMISSIONS }) {
            Store.setEnergy(Store.current.energy.copy(mode = 2, watchConnected = true))
            HealthSync.schedule(ctx)
            sync()
        } else msg = "Permessi non concessi: puoi riprovare quando vuoi."
    }
    LaunchedEffect(Unit) {
        status = HealthSync.status(ctx)
        granted = runCatching { HealthSync.granted(ctx) }.getOrDefault(emptySet())
    }

    val e = Store.energyFor(s, LocalDate.now())
    val today = s.health[LocalDate.now().toString()]
    val ok = status == 0 && granted.any { it in HealthSync.PERMISSIONS }
    val connectionShape = RoundedCornerShape(PremiumScale.cardRadius)

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(RD.NavyBrush).drawBehind {
                drawCircle(Color(0x220E4AA0), size.width * .62f, Offset(size.width, -size.width * .18f))
            }.statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackCircle { Nav.back() }
            GapW(12.dp)
            Text("Smartwatch e attività", style = T.Title.copy(fontSize = 21.sp, lineHeight = 27.sp, color = Color.White),
                modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).testTag("watch-vertical-scroll").verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp)) {
            // Connessione
            Column(
                Modifier.padding(top = 6.dp).fillMaxWidth().refShadow(connectionShape, 4.dp).clip(connectionShape)
                    .background(Color.White).border(1.dp, A.Hair, connectionShape).padding(PremiumScale.inset)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(C.Navy), contentAlignment = Alignment.Center) {
                        AIcon(WIc.Watch, Color(0xFFFFB820), 26.dp, 1.9f)
                    }
                    GapW(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Amazfit Balance", style = T.H1.copy(fontSize = PremiumScale.title, lineHeight = PremiumScale.titleLine, letterSpacing = 0.sp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) Color(0xFF10B981) else Color(0xFFF59E0B)))
                            GapW(6.dp)
                            Text(
                                when { ok -> "Collegato tramite Health Connect"; status == 0 -> "Da autorizzare in Health Connect"; status == 1 -> "Health Connect da installare"; else -> "Health Connect non disponibile" },
                                style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (ok) Color(0xFF0F6B4D) else Color(0xFF8A5A00))
                            )
                        }
                        Text(buildAnnotatedString {
                            append("Ultimo aggiornamento: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = RD.Ink)) {
                                append(if (s.energy.lastSync > 0) fmtDayTime(s.energy.lastSync) else "mai")
                            }
                        }, style = T.Body.copy(fontSize = 12.sp, lineHeight = 16.sp, color = RD.Grey),
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }
                val btnShape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = PremiumScale.actionHeight).clip(btnShape).background(RD.NavyBrush)
                        .clickable(enabled = !busy && status != 2, role = Role.Button) {
                            when {
                                status == 1 -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }
                                    .onFailure { msg = "Impossibile aprire il Play Store su questo dispositivo." }
                                status != 0 -> {}
                                ok -> sync()
                                else -> permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
                            }
                        },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFFFFB820), strokeWidth = 2.4.dp)
                    else AIcon(WIc.Refresh, C.Accent, 20.dp, 2.4f)
                    GapW(10.dp)
                    Text(
                        when { busy -> "Sincronizzo…"; status == 1 -> "Apri Play Store"; status != 0 -> "Non disponibile"; ok -> "Sincronizza ora"; else -> "Collega Health Connect" },
                        style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
                if (ok) {
                    if (HealthSync.BACKGROUND !in granted) Text("Lettura in background non autorizzata: i dati si aggiornano quando apri l'app.", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) {
                        permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Text("Gestisci permessi", style = T.Link, modifier = Modifier.weight(1f))
                        AIcon(Ic.Right, RD.OrangeText, 16.dp, 2.2f)
                    }
                }
                msg?.let { Text(it, style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 6.dp)) }
            }

            // Come contare l'attività
            WatchHeading("Fonte delle calorie attive", "Scegli una modalità di conteggio.", warm = true)
            ACard(radius = 22.dp, padding = 5.dp, elevation = 4.dp) {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    WatchMode(0, s.energy.mode, "Livello fisso", "Usa solo il livello di attività del profilo. Il watch non cambia l'obiettivo.", WIc.Sliders, A.CarbSoft, A.CarbInk) {
                        Store.setEnergy(Store.current.energy.copy(mode = 0))
                    }
                    WatchModeDivider(s.energy.mode != 0 && s.energy.mode != 1)
                    WatchMode(1, s.energy.mode, "Attività a mano", "Aggiungi tu gli allenamenti nel diario; l'obiettivo cresce di conseguenza.", WIc.Pencil, A.FatSoft, A.FatInk) {
                        Store.setEnergy(Store.current.energy.copy(mode = 1))
                    }
                    WatchModeDivider(s.energy.mode != 1 && s.energy.mode != 2)
                    WatchMode(2, s.energy.mode, "Dal watch", "Conta in automatico una parte delle calorie attive misurate dal watch.", WIc.Watch, A.GreenSoft, A.GreenInk) {
                        if (granted.any { it in HealthSync.PERMISSIONS }) Store.setEnergy(Store.current.energy.copy(mode = 2, watchConnected = true))
                        else if (status == 0) permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
                        else msg = "Health Connect non è disponibile. Controlla la connessione nella scheda Amazfit Balance."
                    }
                }
            }

            // Percentuale
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth().testTag("watch-calories"), radius = PremiumScale.cardRadius, padding = PremiumScale.inset, elevation = 4.dp) {
                Column {
                    val percentage = s.energy.eatBackPct.coerceIn(0, 100)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WatchBadge(Ic.Flame, RD.OrangeText, RD.Peach, box = 32.dp)
                        GapW(7.dp)
                        Text("Calorie attive conteggiate", style = T.Label.copy(color = RD.Ink), modifier = Modifier.weight(1f))
                        Text("$percentage%", Modifier.testTag("watch-activity-percentage")
                            .clip(CircleShape).background(RD.Peach).padding(horizontal = 10.dp, vertical = 5.dp),
                            style = T.Title2.copy(fontSize = PremiumScale.label, lineHeight = PremiumScale.labelLine, color = RD.OrangeText))
                    }
                    Slider(
                        value = percentage.toFloat(), valueRange = 0f..100f,
                        onValueChange = { value ->
                            val current = Store.current.energy
                            val newPercentage = value.roundToInt()
                            if (newPercentage != current.eatBackPct) Store.setEnergy(current.copy(eatBackPct = newPercentage))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("watch-activity-slider"),
                        thumb = {
                            Box(Modifier.size(19.dp).background(Color.White, CircleShape)
                                .border(2.dp, DashboardOrange, CircleShape))
                        },
                        track = { slider ->
                            SliderDefaults.Track(sliderState = slider, colors = SliderDefaults.colors(
                                activeTrackColor = DashboardOrange, inactiveTrackColor = Color(0xFFDCE1EA)),
                                thumbTrackGapSize = 0.dp, drawStopIndicator = null)
                        }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(0, 25, 50, 75, 100).forEach { Text("$it%", style = T.Small.copy(color = RD.Ink)) }
                    }
                    Column(
                        Modifier.padding(top = 10.dp).fillMaxWidth()
                    ) {
                        val lab = T.Body.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = RD.Grey)
                        val num = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = A.Ink)
                        Row { Text("Obiettivo base", style = lab, modifier = Modifier.weight(1f)); Text("${kcalFmt(e.baseKcal)} kcal", style = num) }
                        Row(Modifier.padding(top = 6.dp)) {
                            Text(when (s.energy.mode) {
                                2 -> if (e.activeRaw > 0) "Attività dal watch (${s.energy.eatBackPct}% di ${kcalFmt(e.activeRaw)})" else "Attività dal watch"
                                1 -> "Attività inserite a mano"
                                else -> "Livello di attività fisso"
                            }, style = lab, modifier = Modifier.weight(1f))
                            Text("+ ${kcalFmt(e.extraKcal)} kcal", style = num)
                        }
                        Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(Color(0x1A1D1B3A)))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(RD.Peach)
                            .padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Obiettivo di oggi", style = T.Label.copy(color = RD.Ink), modifier = Modifier.weight(1f))
                            Text("${kcalFmt(e.targetKcal)} kcal", Modifier.testTag("watch-calorie-target"),
                                style = T.Title2.copy(fontSize = PremiumScale.title, lineHeight = PremiumScale.titleLine, color = RD.OrangeText))
                        }
                    }
                    Text("I dispositivi da polso tendono a sovrastimare le calorie dell'attività: conteggiarne una parte è prudente. Se il peso scende più del previsto aumenta la percentuale, se sale diminuiscila.",
                        style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(top = 12.dp))
                }
            }

            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = PremiumScale.cardRadius, padding = PremiumScale.inset, elevation = 4.dp) {
                SwitchRow("Importa il peso da Health Connect", "Ultima pesata degli ultimi 30 giorni", s.energy.importWeight) { Store.setEnergy(Store.current.energy.copy(importWeight = it)) }
            }
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = PremiumScale.cardRadius, padding = 0.dp, elevation = 4.dp, onClick = { Nav.go(Screen.EnergyInfo) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = PremiumScale.inset, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    WatchBadge(Ic.Flame, RD.OrangeText, RD.Peach)
                    GapW(10.dp)
                    Text("Come calcolo l'obiettivo di oggi", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                    AIcon(Ic.Right, Color(0xFF8A87A3), 18.dp, 2.4f)
                }
            }

            // Oggi dal watch
            WatchHeading("Oggi dal watch", "I dati dell’ultima sincronizzazione disponibile.")
            if (s.trainingNutrition.active) {
                ACard(radius = PremiumScale.cardRadius, padding = PremiumScale.inset, elevation = 4.dp,
                    onClick = { Nav.go(Screen.TrainingGoals) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WatchBadge(WIc.Dumbbell, RD.OrangeText, RD.Peach)
                        GapW(PremiumScale.gap)
                        Column(Modifier.weight(1f)) {
                            Text("Obiettivi ON/OFF attivi", style = T.Label.copy(fontSize = PremiumScale.label,
                                lineHeight = PremiumScale.labelLine, color = RD.Ink))
                            Text("Passi e attività vengono registrati. Il budget rimane quello ON/OFF che hai impostato.",
                                style = T.Small.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine))
                        }
                        GapW(PremiumScale.gap)
                        AIcon(Ic.Right, RD.Ink, 16.dp)
                    }
                }
                GapH(8.dp)
            }
            if (today != null) {
                val tiles = listOf(
                    WTile("Passi", today.steps.thousands(), "", WIc.Steps, A.Tonal, A.TonalInk),
                    WTile("Kcal attive", today.activeKcal?.let { kcalFmt(it) } ?: "—", "kcal", Ic.Flame, A.FatSoft, A.FatInk),
                    WTile("Allenamenti", today.sessions.size.toString(), "oggi", WIc.Dumbbell, A.FastSoft, A.FastInk),
                    WTile("FC a riposo", today.restingHr?.r0() ?: "—", "bpm", Ic.Heart, A.ProteinSoft, A.ProteinInk),
                    WTile("Sonno", today.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", "", WIc.Moon, A.CarbSoft, A.CarbInk),
                    WTile("Kcal totali", today.totalKcal?.let { kcalFmt(it) } ?: "—", "kcal", Ic.Bolt, A.GreenSoft, A.GreenInk)
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth / LocalDensity.current.fontScale < 245.dp) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(PremiumScale.gap)) {
                        tiles.chunked(columns).forEach { pair ->
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PremiumScale.gap)) {
                                pair.forEach { WatchTile(it, Modifier.weight(1f).fillMaxHeight()) }
                            }
                        }
                    }
                }
                if (today.activeKcal == null) Text("Il watch non ha condiviso le calorie attive: le stimo da passi e allenamenti.", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, top = 10.dp))
            } else {
                ACard(Modifier.fillMaxWidth(), radius = PremiumScale.cardRadius, padding = PremiumScale.inset, elevation = 4.dp) {
                    Text(if (ok) "Ancora nessun dato di oggi: tocca \"Sincronizza ora\"." else "Collega Health Connect per vedere qui passi, calorie attive, allenamenti, battito e sonno.", style = T.Body.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = RD.Grey))
                }
            }

            // Come arrivano i dati
            WatchHeading("Collegamento del dispositivo", "Amazfit → Zepp → Health Connect → Jarvis Food", warm = true)
            ACard(Modifier.fillMaxWidth().testTag("watch-device-card"), radius = PremiumScale.cardRadius, padding = PremiumScale.inset, elevation = 4.dp) {
                Column {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val labels = listOf("Amazfit", "Zepp", "Health Connect", "Jarvis Food")
                        val density = LocalDensity.current
                        val measurer = rememberTextMeasurer()
                        val labelStyle = T.Body.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold)
                        val widths = labels.map { label ->
                            val word = label.split(" ").maxOf { word -> with(density) { measurer.measure(word, style = labelStyle).size.width.toDp() } }
                            maxOf(48.dp, word + 2.dp)
                        }
                        val needed = widths.fold(42.dp) { sum, width -> sum + width }
                        val extra = (maxWidth - needed).coerceAtLeast(0.dp) / labels.size
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Top) {
                            ChainStep(labels[0], WIc.Watch, C.NavySoft, C.Navy, Modifier.width(widths[0] + extra))
                            ChainArrow()
                            ChainStep(labels[1], WIc.Phone, A.CarbSoft, A.CarbInk, Modifier.width(widths[1] + extra))
                            ChainArrow()
                            ChainStep(labels[2], WIc.Health, A.GreenSoft, A.GreenInk, Modifier.width(widths[2] + extra))
                            ChainArrow()
                            ChainStep(labels[3], Ic.Bowl, null, Color.White, Modifier.width(widths[3] + extra))
                        }
                    }
                    Text("Il watch sincronizza con Zepp, Zepp scrive su Health Connect e Jarvis Food legge da lì, solo sul tuo telefono. Se i passi non si aggiornano, apri prima Zepp e lascia che sincronizzi.",
                        style = T.Body.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = RD.Grey), modifier = Modifier.padding(top = 14.dp))
                    Text("Una volta sola, nell'app Zepp: Profilo → Collegamento account di terze parti (3rd Party Account Linking) → Health Connect → consenti tutte le categorie.",
                        style = T.Body.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = RD.Grey), modifier = Modifier.padding(top = 8.dp))
                    val zeppShape = RoundedCornerShape(16.dp)
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = PremiumScale.actionHeight)
                        .testTag("watch-open-zepp").clip(zeppShape).background(Color(0xFFFFE5CA))
                        .border(1.dp, Color(0xFFF2CCA8), zeppShape).clickable(role = Role.Button) {
                            val i = ctx.packageManager.getLaunchIntentForPackage("com.huami.watch.hmwatchmanager")
                            runCatching { ctx.startActivity(i ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.huami.watch.hmwatchmanager"))) }
                                .onFailure { android.widget.Toast.makeText(ctx, "Impossibile aprire Zepp o il Play Store.", android.widget.Toast.LENGTH_LONG).show() }
                        }.padding(horizontal = PremiumScale.inset, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        AIcon(WIc.Phone, RD.OrangeText, PremiumScale.icon)
                        GapW(PremiumScale.gap)
                        Text("Apri Zepp", style = T.Label.copy(fontSize = PremiumScale.label, color = RD.Ink))
                        GapW(PremiumScale.gap)
                        AIcon(Ic.Right, RD.OrangeText, 16.dp)
                    }
                }
            }

        }
    }
}

private object WIc {
    val Watch = Ic.rr(7.0, 6.0, 10.0, 12.0, 3.0) + "M9 6l1-3h4l1 3M9 18l1 3h4l1-3M12 10v2l1.5 1"
    val Refresh = "M20 12a8 8 0 1 1-2.3-5.6M20 4v5h-5"
    val Steps = "M8 4c2 0 3 1.5 3 4s-1 4-3 4-3-1.5-3-4 1-4 3-4zM16 12c2 0 3 1.5 3 4s-1 4-3 4-3-1.5-3-4 1-4 3-4z"
    val Dumbbell = "M3 10v4M6 8v8M18 8v8M21 10v4M6 12h12"
    val Moon = "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"
    val Sliders = "M4 8h16M4 16h16M9 5v6M15 13v6"
    val Pencil = "M5 19l3.5-.8L19 7.7 16.3 5 5.8 15.5zM14.5 6.8l2.7 2.7"
    val Health = Ic.Heart + "M12 8v6M9 11h6"
    val Phone = Ic.rr(7.0, 3.0, 10.0, 18.0, 3.0) + "M11 18h2"
}

private class WTile(val label: String, val value: String, val unit: String, val icon: String, val bg: Color, val ink: Color)

@Composable
private fun WatchHeading(title: String, subtitle: String, warm: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp).width(3.dp).height(20.dp).clip(CircleShape)
            .background(if (warm) RD.Orange else RD.Navy))
        GapW(PremiumScale.gap)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title2.copy(fontSize = PremiumScale.title, lineHeight = PremiumScale.titleLine))
            Text(subtitle, style = T.Small.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine))
        }
    }
}

@Composable
private fun WatchBadge(icon: String, ink: Color, surface: Color, box: Dp = PremiumScale.iconTile) {
    Box(Modifier.size(box).clip(RoundedCornerShape(12.dp)).background(surface), contentAlignment = Alignment.Center) {
        AIcon(icon, ink, PremiumScale.icon, 2.1f)
    }
}

@Composable
private fun WatchTile(t: WTile, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.heightIn(min = 66.dp).refShadow(shape, 3.dp).clip(shape).background(Color.White)
        .border(1.dp, A.Hair, shape).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        WatchBadge(t.icon, t.ink, t.bg, box = 32.dp)
        GapW(PremiumScale.gap)
        Column(Modifier.weight(1f)) {
            Text(buildAnnotatedString {
                append(t.value)
                if (t.unit.isNotEmpty()) withStyle(SpanStyle(fontSize = PremiumScale.body, color = RD.Grey, fontWeight = FontWeight.Medium)) {
                    append(" ${t.unit}")
                }
            }, style = T.Title2.copy(fontSize = PremiumScale.value, lineHeight = PremiumScale.valueLine))
            Text(t.label, style = T.Small.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = RD.Grey))
        }
    }
}

@Composable
private fun WatchMode(id: Int, current: Int, name: String, desc: String, icon: String, tint: Color, ink: Color, onClick: () -> Unit) {
    val sel = id == current
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
        .border(if (sel) 1.5.dp else 0.dp, if (sel) RD.Navy else Color.Transparent, shape)
        .selectable(sel, role = Role.RadioButton, onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        WatchBadge(icon, ink, tint)
        GapW(10.dp)
        Column(Modifier.weight(1f)) {
            Text(name, style = T.Body.copy(fontSize = PremiumScale.label, lineHeight = PremiumScale.labelLine, fontWeight = FontWeight.SemiBold, color = A.Ink))
            Text(desc, style = T.Small.copy(fontSize = PremiumScale.body, lineHeight = PremiumScale.bodyLine, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
        }
        GapW(10.dp)
        Box(Modifier.size(20.dp).clip(CircleShape).background(if (sel) A.Ink else Color.Transparent).border(2.dp, if (sel) A.Ink else Color(0xFFC9C5D8), CircleShape), contentAlignment = Alignment.Center) {
            if (sel) AIcon(Ic.Check, Color.White, 14.dp, 3.4f)
        }
    }
}

@Composable
private fun WatchModeDivider(visible: Boolean) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp)
        .background(if (visible) RD.GreyLine else Color.Transparent))
}

@Composable
private fun ChainStep(label: String, icon: String, bg: Color?, ink: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(bg ?: Color.Transparent).then(if (bg == null) Modifier.background(A.Dawn) else Modifier), contentAlignment = Alignment.Center) {
            AIcon(icon, ink, PremiumScale.icon, if (bg == null) 2.2f else 2f)
        }
        Text(label, style = T.Body.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold, color = A.Ink, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("watch-chain-$label"))
    }
}

@Composable
private fun ChainArrow() {
    Box(Modifier.padding(top = 12.dp)) { AIcon(Ic.Right, Color(0xFFB7B3C9), 14.dp, 2.4f) }
}
