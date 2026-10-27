package com.jarvis.food.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Store
import com.jarvis.food.health.HealthSync
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val shape34 = RoundedCornerShape(34.dp)

    Column(Modifier.fillMaxSize()) {
        TopBar("Smartwatch e attività", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
            // Connessione
            Column(
                Modifier.padding(top = 6.dp).fillMaxWidth().aShadow(shape34, 14.dp, Color(0x803B82F6)).clip(shape34)
                    .background(C.Surface).border(1.dp, Color(0xE6FFFFFF), shape34).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(C.Navy), contentAlignment = Alignment.Center) {
                        AIcon(WIc.Watch, Color(0xFFFFB820), 32.dp, 1.9f)
                    }
                    GapW(16.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Amazfit Balance", style = T.H1.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) Color(0xFF10B981) else Color(0xFFF59E0B)))
                            GapW(6.dp)
                            Text(
                                when { ok -> "Collegato tramite Health Connect"; status == 0 -> "Da autorizzare in Health Connect"; status == 1 -> "Health Connect da installare"; else -> "Health Connect non disponibile" },
                                style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (ok) Color(0xFF0F6B4D) else Color(0xFF8A5A00))
                            )
                        }
                    }
                }
                Text(buildAnnotatedString {
                    append("Ultimo aggiornamento: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = A.Ink)) { append(if (s.energy.lastSync > 0) fmtDayTime(s.energy.lastSync) else "mai") }
                }, style = T.Body.copy(fontSize = 13.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 16.dp))
                val btnShape = RoundedCornerShape(27.dp)
                Row(
                    Modifier.padding(top = 10.dp).fillMaxWidth().height(54.dp).aShadow(btnShape, 8.dp, Color(0xB31D1B3A)).clip(btnShape).background(C.Navy)
                        .clickable(enabled = !busy) {
                            when {
                                status == 1 -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }
                                status != 0 -> {}
                                ok -> sync()
                                else -> permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
                            }
                        },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFFFFB820), strokeWidth = 2.4.dp)
                    else AIcon(WIc.Refresh, Color(0xFFFFB820), 20.dp, 2.4f)
                    GapW(10.dp)
                    Text(
                        when { busy -> "Sincronizzo…"; status == 1 -> "Apri Play Store"; status != 0 -> "Non disponibile"; ok -> "Sincronizza ora"; else -> "Collega Health Connect" },
                        style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White), maxLines = 1
                    )
                }
                if (ok) {
                    if (HealthSync.BACKGROUND !in granted) Text("Lettura in background non autorizzata: i dati si aggiornano quando apri l'app.", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(top = 10.dp))
                    Text("Gestisci permessi", style = T.Link, modifier = Modifier.padding(top = 4.dp).clickable { permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND) }.padding(vertical = 8.dp))
                }
                msg?.let { Text(it, style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 6.dp)) }
            }

            // Oggi dal watch
            GHead("Oggi dal watch")
            if (today != null) {
                val tiles = listOf(
                    WTile("Passi", today.steps.thousands(), "", WIc.Steps, A.Tonal, A.TonalInk, Color(0x99E88C60)),
                    WTile("Kcal attive", today.activeKcal?.let { kcalFmt(it) } ?: "—", "kcal", Ic.Flame, A.FatSoft, A.FatInk, Color(0x99F59E0B)),
                    WTile("Allenamenti", today.sessions.size.toString(), "oggi", WIc.Dumbbell, A.FastSoft, A.FastInk, Color(0x998B5CF6)),
                    WTile("FC a riposo", today.restingHr?.r0() ?: "—", "bpm", Ic.Heart, A.ProteinSoft, A.ProteinInk, Color(0x99F43F6E)),
                    WTile("Sonno", today.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", "", WIc.Moon, A.CarbSoft, A.CarbInk, Color(0x993B82F6)),
                    WTile("Kcal totali", today.totalKcal?.let { kcalFmt(it) } ?: "—", "kcal", Ic.Bolt, A.GreenSoft, A.GreenInk, Color(0x9910B981))
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiles.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { pair.forEach { WatchTile(it, Modifier.weight(1f)) } }
                    }
                }
                if (today.activeKcal == null) Text("Il watch non ha condiviso le calorie attive: le stimo da passi e allenamenti.", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, top = 10.dp))
            } else {
                ACard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 16.dp, elevation = 8.dp) {
                    Text(if (ok) "Ancora nessun dato di oggi: tocca \"Sincronizza ora\"." else "Collega Health Connect per vedere qui passi, calorie attive, allenamenti, battito e sonno.", style = T.Body.copy(fontSize = 13.5.sp, color = Color(0xFF4B4A6A)))
                }
            }

            // Come contare l'attività
            GHead("Come contare l'attività")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WatchMode(0, s.energy.mode, "Livello fisso", "Usa solo il livello di attività del profilo. Il watch non cambia l'obiettivo.", WIc.Sliders, A.CarbSoft, A.CarbInk) { Store.setEnergy(s.energy.copy(mode = 0)) }
                WatchMode(1, s.energy.mode, "Attività a mano", "Aggiungi tu gli allenamenti nel diario; l'obiettivo cresce di conseguenza.", WIc.Pencil, A.FatSoft, A.FatInk) { Store.setEnergy(s.energy.copy(mode = 1)) }
                WatchMode(2, s.energy.mode, "Dal watch", "Conta in automatico una parte delle calorie attive misurate dal watch.", WIc.Watch, A.GreenSoft, A.GreenInk) {
                    if (granted.any { it in HealthSync.PERMISSIONS }) Store.setEnergy(s.energy.copy(mode = 2, watchConnected = true))
                    else permLauncher.launch(HealthSync.PERMISSIONS + HealthSync.BACKGROUND)
                }
            }

            // Percentuale
            ACard(Modifier.padding(top = 16.dp).fillMaxWidth().alpha(if (s.energy.mode == 2) 1f else 0.4f), radius = 30.dp, padding = 20.dp, elevation = 8.dp) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Calorie attive conteggiate", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                        Text("${s.energy.eatBackPct}%", style = T.H1.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.sp, color = Color(0xFFD9411E)))
                    }
                    val pcts = listOf(0, 25, 50, 75, 100)
                    ASegment(pcts.map { "$it%" }, pcts.indexOf(s.energy.eatBackPct), modifier = Modifier.padding(top = 12.dp)) { i -> Store.setEnergy(Store.current.energy.copy(eatBackPct = pcts[i])) }
                    Column(
                        Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                            .background(C.Surface).padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        val lab = T.Body.copy(fontSize = 13.5.sp, color = Color(0xFF4B4A6A))
                        val num = T.Body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = A.Ink)
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Obiettivo di oggi", style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                            Text("${kcalFmt(e.targetKcal)} kcal", style = T.H1.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.sp))
                        }
                    }
                    Text("I dispositivi da polso tendono a sovrastimare le calorie dell'attività: conteggiarne una parte è prudente. Se il peso scende più del previsto aumenta la percentuale, se sale diminuiscila.",
                        style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(top = 12.dp))
                }
            }

            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 16.dp, elevation = 8.dp) {
                SwitchRow("Importa il peso da Health Connect", "Ultima pesata degli ultimi 30 giorni", s.energy.importWeight) { Store.setEnergy(s.energy.copy(importWeight = it)) }
            }
            ACard(Modifier.padding(top = 12.dp).fillMaxWidth(), radius = 30.dp, padding = 0.dp, elevation = 8.dp, onClick = { Nav.go(Screen.EnergyInfo) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TonalIcon(Ic.Flame, A.TonalInk, A.Tonal, box = 46.dp, radius = 17.dp, size = 23.dp)
                    GapW(14.dp)
                    Text("Come calcolo l'obiettivo di oggi", style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                    AIcon(Ic.Right, Color(0xFF8A87A3), 18.dp, 2.4f)
                }
            }

            // Come arrivano i dati
            GHead("Come arrivano i dati")
            ACard(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, elevation = 8.dp) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        ChainStep("Amazfit", WIc.Watch, A.FastSoft, A.FastInk, Modifier.weight(1f))
                        ChainArrow()
                        ChainStep("Zepp", WIc.Phone, A.CarbSoft, A.CarbInk, Modifier.weight(1f))
                        ChainArrow()
                        ChainStep("Health Connect", Ic.Heart, A.GreenSoft, A.GreenInk, Modifier.weight(1f))
                        ChainArrow()
                        ChainStep("Jarvis Food", Ic.Bowl, null, Color.White, Modifier.weight(1f))
                    }
                    Text("Il watch sincronizza con Zepp, Zepp scrive su Health Connect e Jarvis Food legge da lì, solo sul tuo telefono. Se i passi non si aggiornano, apri prima Zepp e lascia che sincronizzi.",
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 14.dp))
                    Text("Una volta sola, nell'app Zepp: Profilo → Collegamento account di terze parti (3rd Party Account Linking) → Health Connect → consenti tutte le categorie.",
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 8.dp))
                    AInk("Apri Zepp", Modifier.padding(top = 14.dp).fillMaxWidth(), height = 50.dp, bg = C.NavySoft, fg = C.Navy) {
                        val i = ctx.packageManager.getLaunchIntentForPackage("com.huami.watch.hmwatchmanager")
                        runCatching { ctx.startActivity(i ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.huami.watch.hmwatchmanager"))) }
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
    val Phone = Ic.rr(7.0, 3.0, 10.0, 18.0, 3.0) + "M11 18h2"
}

private class WTile(val label: String, val value: String, val unit: String, val icon: String, val bg: Color, val ink: Color, val glow: Color)

@Composable
private fun WatchTile(t: WTile, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Column(modifier.aShadow(shape, 8.dp, t.glow).clip(shape).background(C.cardSurface(t.bg)).border(1.dp, Color(0xE6FFFFFF), shape).padding(16.dp)) {
        TonalIcon(t.icon, t.ink, Color(0xD9FFFFFF), box = 40.dp, radius = 15.dp, size = 21.dp)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
            Text(t.value, style = T.H1.copy(fontSize = 26.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp), maxLines = 1)
            if (t.unit.isNotEmpty()) Text(t.unit, style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = A.Muted), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        Text(t.label, style = T.Body.copy(fontSize = 12.5.sp, color = Color(0xFF4B4A6A)), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WatchMode(id: Int, current: Int, name: String, desc: String, icon: String, tint: Color, ink: Color, onClick: () -> Unit) {
    val sel = id == current
    AChoice(sel, Modifier.fillMaxWidth(), minHeight = 78.dp, radius = 28.dp, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TonalIcon(icon, ink, tint, box = 46.dp, radius = 17.dp, size = 23.dp)
            GapW(14.dp)
            Column(Modifier.weight(1f)) {
                Text(name, style = T.Body.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                Text(desc, style = T.Small.copy(fontSize = 12.5.sp, lineHeight = 18.sp, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
            }
            GapW(14.dp)
            Box(Modifier.size(24.dp).clip(CircleShape).background(if (sel) A.Ink else Color.Transparent).border(2.dp, if (sel) A.Ink else Color(0xFFC9C5D8), CircleShape), contentAlignment = Alignment.Center) {
                if (sel) AIcon(Ic.Check, Color.White, 14.dp, 3.4f)
            }
        }
    }
}

@Composable
private fun ChainStep(label: String, icon: String, bg: Color?, ink: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(18.dp)).background(bg?.let { C.cardSurface(it) } ?: Color.Transparent).then(if (bg == null) Modifier.background(A.Dawn) else Modifier), contentAlignment = Alignment.Center) {
            AIcon(icon, ink, 24.dp, if (bg == null) 2.2f else 2f)
        }
        Text(label, style = T.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = A.Ink, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ChainArrow() {
    Box(Modifier.padding(top = 15.dp)) { AIcon(Ic.Right, Color(0xFFB7B3C9), 18.dp, 2.4f) }
}
