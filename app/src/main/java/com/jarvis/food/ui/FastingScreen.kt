package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.FastingPlans
import com.jarvis.food.data.Store
import com.jarvis.food.notify.Reminders
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val DHM = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", IT)

fun fmtTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(HM)
fun fmtDayTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DHM)

fun fmtDuration(ms: Long): String {
    val totalMin = (ms / 60_000).coerceAtLeast(0)
    val h = totalMin / 60; val m = totalMin % 60
    return "%d:%02d".format(h, m)
}

@Composable
fun FastingScreen() {
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val f = s.fasting
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(f.activeStart) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    var confirmStop by remember { mutableStateOf(false) }

    val active = f.activeStart != null
    val elapsed = if (active) now - f.activeStart!! else 0L
    val target = f.targetHours * 3_600_000L
    val progress = if (active) (elapsed.toFloat() / target) else 0f
    val hours = elapsed / 3_600_000.0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Digiuno intermittente", style = T.Display)
        Text(if (active) "Piano ${f.planId} in corso" else "Scegli un piano e inizia", style = T.Muted)
        Spacer(Modifier.height(14.dp))

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FastingPlans.plans.forEach { p ->
                val sel = f.planId == p.id
                Column(
                    Modifier.width(118.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) C.Fasting.copy(alpha = 0.16f) else C.Surface)
                        .border(1.dp, if (sel) C.Fasting else C.Line, RoundedCornerShape(18.dp))
                        .clickable {
                            Store.update { it.copy(fasting = it.fasting.copy(planId = p.id, targetHours = p.fastHours)) }
                            Reminders.rescheduleAll(ctx)
                        }
                        .padding(12.dp)
                ) {
                    Text(p.title, style = T.Title2.copy(color = if (sel) C.Fasting else C.Text))
                    Text(p.level, style = T.Small)
                }
            }
        }
        Text(FastingPlans.plans.firstOrNull { it.id == f.planId }?.desc ?: "", style = T.Small, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(250.dp)) {
                val st = 18.dp.toPx()
                val tl = Offset(st / 2, st / 2)
                val sz = Size(size.width - st, size.height - st)
                drawArc(C.SurfaceHi, -90f, 360f, false, tl, sz, style = Stroke(st))
                drawArc(
                    if (progress >= 1f) C.Accent else C.Fasting, -90f, 360f * progress.coerceAtMost(1f), false, tl, sz,
                    style = Stroke(st, cap = StrokeCap.Round)
                )
                // Tacche delle fasi
                FastingPlans.phases.drop(1).forEach { ph ->
                    if (ph.fromH < f.targetHours) {
                        val a = Math.toRadians(-90.0 + 360.0 * ph.fromH / f.targetHours)
                        val r = sz.width / 2
                        val c = Offset(size.width / 2, size.height / 2)
                        drawCircle(C.Bg, 4.dp.toPx(), Offset(c.x + (r * Math.cos(a)).toFloat(), c.y + (r * Math.sin(a)).toFloat()))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (active) {
                    Text(if (progress >= 1f) "Completato! 🎉" else "Trascorso", style = T.Muted)
                    Text(fmtDuration(elapsed), style = T.Big)
                    val left = target - elapsed
                    Text(if (left > 0) "mancano ${fmtDuration(left)}" else "+${fmtDuration(-left)} oltre l'obiettivo", style = T.Small)
                } else {
                    Text("Pronto", style = T.Muted)
                    Text("${f.targetHours} h", style = T.Big)
                    Text("di digiuno", style = T.Small)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (active) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Inizio", fmtTime(f.activeStart!!), C.Text)
                Spacer(Modifier.weight(1f))
                Stat("Fine prevista", fmtTime(f.activeStart + target), C.Fasting)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Inizio −30 min", Modifier.weight(1f)) {
                    Store.startFast(f.planId, f.targetHours, f.activeStart - 30 * 60_000L); Reminders.rescheduleAll(ctx)
                }
                GhostButton("Inizio +30 min", Modifier.weight(1f)) {
                    Store.startFast(f.planId, f.targetHours, minOf(System.currentTimeMillis(), f.activeStart + 30 * 60_000L)); Reminders.rescheduleAll(ctx)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!confirmStop) PrimaryButton("Termina digiuno", Modifier.fillMaxWidth(), color = C.Fasting) {
                if (progress >= 1f) { Store.stopFast(true); Reminders.rescheduleAll(ctx) } else confirmStop = true
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Continua", Modifier.weight(1f)) { confirmStop = false }
                GhostButton("Salva e termina", Modifier.weight(1f)) { Store.stopFast(true); confirmStop = false; Reminders.rescheduleAll(ctx) }
                GhostButton("Annulla", Modifier.weight(1f), color = C.Danger) { Store.stopFast(false); confirmStop = false; Reminders.rescheduleAll(ctx) }
            }

            val phase = FastingPlans.phaseAt(hours)
            SectionTitle("Fase attuale")
            JCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(phase.emoji, style = T.Title)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(phase.title, style = T.Title2)
                        Text(phase.desc, style = T.Muted)
                    }
                }
            }
        } else {
            PrimaryButton("Inizia digiuno ora", Modifier.fillMaxWidth(), color = C.Fasting) {
                Store.startFast(f.planId, f.targetHours); Reminders.rescheduleAll(ctx)
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Ho iniziato dopo cena (ieri alle 21:00)", Modifier.fillMaxWidth()) {
                val start = java.time.LocalDate.now().minusDays(1).atTime(21, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                Store.startFast(f.planId, f.targetHours, start); Reminders.rescheduleAll(ctx)
            }
        }

        SectionTitle("Le fasi del digiuno")
        FastingPlans.phases.forEach { ph ->
            val reached = active && hours >= ph.fromH
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (reached) C.Fasting.copy(alpha = 0.2f) else C.Surface),
                    contentAlignment = Alignment.Center
                ) { Text(ph.emoji) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${ph.title} · da ${ph.fromH.toInt()} h", style = T.Label.copy(color = if (reached) C.Fasting else C.Text))
                    Text(ph.desc, style = T.Small)
                }
            }
        }
        Text("I tempi delle fasi sono indicativi e variano da persona a persona. In gravidanza, allattamento, diabete o disturbi alimentari consulta il medico prima di digiunare.", style = T.Small, modifier = Modifier.padding(top = 8.dp))

        SectionTitle("Storico", action = "Vedi tutto") { Nav.go(Screen.FastingHistory) }
        val last7 = f.history.take(7)
        if (last7.isEmpty()) Text("Nessun digiuno completato ancora.", style = T.Muted)
        else {
            val ok = f.history.count { it.end - it.start >= it.targetHours * 3_600_000L }
            JCard {
                Text("$ok digiuni completati su ${f.history.size}", style = T.Label)
                Spacer(Modifier.height(10.dp))
                BarChart(
                    values = last7.reversed().map { (it.end - it.start) / 3_600_000.0 },
                    labels = last7.reversed().map { Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).dayOfMonth.toString() },
                    goal = f.targetHours.toDouble(), color = C.Fasting
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FastingHistoryScreen() {
    val s by Store.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Storico digiuni", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (s.fasting.history.isEmpty()) EmptyState("⏱️", "Ancora vuoto", "I digiuni terminati compariranno qui.")
            s.fasting.history.forEach { h ->
                val dur = h.end - h.start
                val ok = dur >= h.targetHours * 3_600_000L
                JCard(Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fmtDayTime(h.start), style = T.Label)
                            Text("Obiettivo ${h.targetHours} h", style = T.Small)
                        }
                        Text(fmtDuration(dur), style = T.Title2.copy(color = if (ok) C.Accent else C.Muted))
                    }
                }
            }
        }
    }
}
