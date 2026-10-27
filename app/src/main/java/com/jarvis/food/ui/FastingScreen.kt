package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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

private val FAST_ICON = listOf(Ic.MealBreakfast, Ic.Steps, Ic.Bolt, Ic.Flame, Ic.Drop)
private val FAST_TINT = listOf(C.AccentSoft, C.NavySoft, C.AccentSoft, C.NavySoft, C.AccentSoft)
private val FAST_INK = listOf(C.AccentDeep, C.Navy, C.AccentDeep, C.Navy, C.AccentDeep)
private val Violet = C.Navy

private fun hm(ms: Long): String { val t = (ms / 60_000).coerceAtLeast(0); return if (t >= 60) "${t / 60} h ${t % 60} min" else "$t min" }

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
    val phases = FastingPlans.phases
    val curIdx = if (active) phases.indexOfLast { hours >= it.fromH } else -1

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackCircle { Nav.back() }
            GapW(12.dp)
            Text("Digiuno intermittente", style = T.H1.copy(fontSize = 27.sp, lineHeight = 31.sp), modifier = Modifier.weight(1f))
        }

        // Piani
        Row(Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FastingPlans.plans.forEach { p ->
                val sel = f.planId == p.id
                val shape = RoundedCornerShape(22.dp)
                Box(Modifier.width(68.dp).height(44.dp).clip(shape).background(if (sel) C.Navy else Color.White).border(1.dp, if (sel) C.Navy else A.Hair, shape)
                    .clickable { Store.update { it.copy(fasting = it.fasting.copy(planId = p.id, targetHours = p.fastHours)) }; Reminders.rescheduleAll(ctx) },
                    contentAlignment = Alignment.Center) { Text(p.title.removePrefix("OMAD ").removeSuffix(" ore").let { if (p.id == "36") "36 h" else it }, style = T.BtnS.copy(color = if (sel) Color.White else A.Ink), maxLines = 1) }
            }
        }
        Text(FastingPlans.plans.firstOrNull { it.id == f.planId }?.desc ?: "", style = T.Small.copy(color = A.Muted), modifier = Modifier.padding(top = 8.dp, start = 4.dp))

        // Timer
        val cardShape = RoundedCornerShape(36.dp)
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().aShadow(cardShape, 14.dp, Color(0xA68B5CF6)).clip(cardShape)
            .background(C.Surface).border(1.dp, Color(0xE6FFFFFF), cardShape)
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp)) {
            Box(Modifier.size(260.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val st = 22.dp.toPx()
                    val tl = Offset(st / 2 + 2.dp.toPx(), st / 2 + 2.dp.toPx())
                    val sz = Size(size.width - st - 4.dp.toPx(), size.height - st - 4.dp.toPx())
                    drawArc(C.SurfaceHi, -90f, 360f, false, tl, sz, style = Stroke(st))
                    if (progress > 0f) drawArc(
                        C.NavyBrush,
                        -90f, 360f * progress.coerceAtMost(1f), false, tl, sz, style = Stroke(st, cap = StrokeCap.Round)
                    )
                    // Tacche delle fasi
                    phases.drop(1).forEach { ph ->
                        if (ph.fromH < f.targetHours) {
                            val a = Math.toRadians(-90.0 + 360.0 * ph.fromH / f.targetHours)
                            val r = sz.width / 2
                            val c = Offset(size.width / 2, size.height / 2)
                            val p = Offset(c.x + (r * Math.cos(a)).toFloat(), c.y + (r * Math.sin(a)).toFloat())
                            drawCircle(Color.White, 6.5.dp.toPx(), p)
                            drawCircle(if (active && hours >= ph.fromH) C.Accent else C.Line, 4.5.dp.toPx(), p)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val left = target - elapsed
                    Text(if (!active) "Pronto a partire" else if (left > 0) "Mancano ${hm(left)}" else "Obiettivo raggiunto 🎉", style = T.Small.copy(fontSize = 13.sp, color = C.Muted, fontWeight = FontWeight.Medium))
                    Text(if (active) fmtDuration(elapsed) else "${f.targetHours}:00", style = T.H1.copy(fontSize = 54.sp, lineHeight = 56.sp))
                    Text(if (active) "di digiuno su ${f.targetHours} h" else "ore di digiuno", style = T.Small.copy(fontSize = 13.sp, color = C.Muted, fontWeight = FontWeight.Medium))
                }
            }
            if (active) {
                val ph = curIdx.coerceAtLeast(0)
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xCCFFFFFF)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TonalIcon(FAST_ICON[ph % 5], FAST_INK[ph % 5], FAST_TINT[ph % 5], 40.dp, 14.dp, 22.dp)
                    GapW(10.dp)
                    Column {
                        Text("Fase attuale", style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                        Text(phases[ph].title, style = T.Body.copy(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Inizio −30 min" to -30L, "Inizio +30 min" to 30L).forEach { (l, d) ->
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xD9FFFFFF)).clickable {
                            Store.startFast(f.planId, f.targetHours, minOf(System.currentTimeMillis(), f.activeStart!! + d * 60_000L)); Reminders.rescheduleAll(ctx)
                        }, contentAlignment = Alignment.Center) { Text(l, style = T.BtnS.copy(fontSize = 13.5.sp)) }
                    }
                }
                if (!confirmStop) Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(C.Navy).clickable {
                    if (progress >= 1f) { Store.stopFast(true); Reminders.rescheduleAll(ctx) } else confirmStop = true
                }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Stop, Color.White, 18.dp, fill = true); GapW(10.dp)
                    Text("Termina digiuno", style = T.BtnL.copy(color = Color.White))
                } else Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Continua", A.Ink, 0), Triple("Salva e termina", C.Navy, 1), Triple("Annulla", C.Danger, 2)).forEach { (l, col, k) ->
                        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(if (k == 1) C.Navy else Color(0xD9FFFFFF)).clickable {
                            when (k) { 0 -> confirmStop = false; 1 -> { Store.stopFast(true); confirmStop = false; Reminders.rescheduleAll(ctx) }; else -> { Store.stopFast(false); confirmStop = false; Reminders.rescheduleAll(ctx) } }
                        }, contentAlignment = Alignment.Center) { Text(l, style = T.BtnS.copy(fontSize = 13.sp, color = if (k == 1) Color.White else col), maxLines = 1) }
                    }
                }
                val st = java.time.Instant.ofEpochMilli(f.activeStart!!).atZone(ZoneId.systemDefault())
                val dayWord = when (java.time.temporal.ChronoUnit.DAYS.between(st.toLocalDate(), java.time.LocalDate.now()).toInt()) { 0 -> "oggi"; 1 -> "ieri"; else -> st.format(DateTimeFormatter.ofPattern("d MMM", IT)) }
                Text("Iniziato $dayWord alle ${fmtTime(f.activeStart!!)} · puoi ancora regolare l'inizio", style = T.Small.copy(fontSize = 12.5.sp, color = C.Muted),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            } else {
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(C.Navy).clickable { Store.startFast(f.planId, f.targetHours); Reminders.rescheduleAll(ctx) },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Bolt, C.Accent, 20.dp, 2f); GapW(10.dp)
                    Text("Inizia digiuno ora", style = T.BtnL.copy(color = Color.White))
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xD9FFFFFF)).clickable {
                    val start = java.time.LocalDate.now().minusDays(1).atTime(21, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    Store.startFast(f.planId, f.targetHours, start); Reminders.rescheduleAll(ctx)
                }, contentAlignment = Alignment.Center) { Text("Ho iniziato dopo cena (ieri alle 21:00)", style = T.BtnS.copy(fontSize = 13.5.sp)) }
            }
        }

        // Fasi
        ASection("Le fasi del digiuno", top = 28.dp)
        GapH(14.dp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            phases.forEachIndexed { i, ph ->
                val past = active && i < curIdx
                val cur = active && i == curIdx
                val status = when { cur -> "In corso"; past -> "Superata"; active -> "Tra ${((ph.fromH - hours) * 10).roundToInt() / 10.0}".replace(".", ",") + " h"; else -> "" }
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(44.dp).let { if (cur) it.border(3.dp, C.AccentDeep, RoundedCornerShape(16.dp)) else it }, contentAlignment = Alignment.Center) {
                        TonalIcon(FAST_ICON[i % 5], FAST_INK[i % 5], FAST_TINT[i % 5], 44.dp, 16.dp, 22.dp)
                    }
                    GapW(12.dp)
                    val cs = RoundedCornerShape(24.dp)
                    Column(Modifier.weight(1f).let { if (cur) it.aShadow(cs, 6.dp, Color(0x661D1B3A)) else it }.clip(cs).background(if (cur) Color.White else Color(0xB3F6F8FB)).border(1.dp, A.Hair, cs).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ph.title, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            Text(status, style = T.Chip.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (cur) Violet else A.Muted))
                        }
                        Text("da ${ph.fromH.toInt()} h", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
                        Text(ph.desc, style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp), modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        Text("I tempi delle fasi sono indicativi e variano da persona a persona. In gravidanza, allattamento, diabete o disturbi alimentari consulta il medico prima di digiunare.",
            style = T.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, color = A.Muted), modifier = Modifier.padding(top = 14.dp))

        // Storico
        Row(Modifier.padding(top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Storico", style = T.H2, modifier = Modifier.weight(1f))
            Text("Vedi tutti", style = T.Link.copy(color = C.AccentDeep), modifier = Modifier.clickable { Nav.go(Screen.FastingHistory) }.padding(vertical = 8.dp))
        }
        val last7 = f.history.take(7).reversed()
        val hs = RoundedCornerShape(30.dp)
        Column(Modifier.fillMaxWidth().aShadow(hs, 10.dp, Color(0x668B5CF6)).clip(hs).background(Color.White).border(1.dp, A.Hair, hs).padding(16.dp)) {
            if (last7.isEmpty()) Text("Nessun digiuno completato ancora.", style = T.Muted)
            else {
                val maxH = maxOf(f.targetHours.toDouble(), last7.maxOf { (it.end - it.start) / 3_600_000.0 }) * 1.05
                Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    last7.forEach { h ->
                        val done = h.end - h.start >= h.targetHours * 3_600_000L
                        val frac = (((h.end - h.start) / 3_600_000.0) / maxH).toFloat().coerceIn(0.05f, 1f)
                        Box(Modifier.weight(1f).fillMaxHeight(frac).clip(RoundedCornerShape(12.dp))
                            .background(if (done) C.NavyBrush else Brush.verticalGradient(listOf(C.NavySoft, C.NavySoft))))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    last7.forEach { h ->
                        Text(Instant.ofEpochMilli(h.start).atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, IT).uppercase(),
                            style = T.Chip.copy(fontSize = 11.5.sp, color = A.Muted), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                val ok = f.history.count { it.end - it.start >= it.targetHours * 3_600_000L }
                val longest = f.history.maxOf { it.end - it.start }
                Text("$ok digiuni completati su ${f.history.size}", style = T.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 12.dp))
                Text("Obiettivo ${f.targetHours} h · il più lungo: ${hm(longest)}", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted))
            }
        }
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
