package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.AppState
import com.jarvis.food.data.Db
import com.jarvis.food.data.Store
import com.jarvis.food.engine.MicroAnalysis
import com.jarvis.food.engine.MicroStatus
import com.jarvis.food.engine.Micros
import java.time.LocalDate

private fun levelColor(st: MicroStatus): Color = when (st.level) {
    "ok" -> C.Green; "vicino" -> C.Gold; "alto" -> C.Danger; else -> C.Accent
}

@Composable
fun MicroSummaryCard(s: AppState) {
    val e = Store.energyFor(s, LocalDate.now())
    val rep = remember(s.diary.size, s.profile) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal) }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
    Row(
        Modifier.fillMaxWidth().aShadow(shape, 12.dp, Color(0x9910B981)).clip(shape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFDDF6EC), Color(0xFFE8F0FF))))
            .clickable { Nav.go(Screen.Micronutrients) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ok = rep.statuses.count { it.level == "ok" && it.def.kind != "LIM" }
        val tot = rep.statuses.count { it.def.kind != "LIM" }.coerceAtLeast(1)
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val sw = 8.dp.toPx(); val inset = sw / 2
                val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                drawArc(Color(0xE6FFFFFF), 0f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), sz, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                if (rep.loggedDays > 0) drawArc(Color(0xFF10B981), -90f, 360f * ok / tot, false, androidx.compose.ui.geometry.Offset(inset, inset), sz,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Text(if (rep.loggedDays > 0) "$ok/$tot" else "–", style = T.H1.copy(fontSize = 16.sp, lineHeight = 20.sp))
        }
        GapW(14.dp)
        Column(Modifier.weight(1f)) {
            Text("Analisi completa", style = T.Body.copy(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Text(if (rep.loggedDays == 0) "registra almeno un giorno completo per l'analisi" else "nutrienti al riferimento nella media degli ultimi ${rep.loggedDays} giorni registrati",
                style = T.Small.copy(fontSize = 12.5.sp, color = Color(0xFF4B5B6E)))
        }
        AIcon(Ic.Right, A.Ink, 20.dp, 2.4f)
    }
}

@Composable
private fun MicroRow(st: MicroStatus) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(st.def.name, style = T.Label, modifier = Modifier.width(112.dp))
        val frac = if (st.def.kind == "LIM") (st.pct / 150).toFloat() else (st.pct / 150).toFloat()
        ProgressLine(frac, levelColor(st), Modifier.weight(1f), 8.dp, marker = (100 / 150.0).toFloat())
        Text("  ${st.pct.r0()}%", style = T.Small.copy(color = C.Text), modifier = Modifier.width(52.dp))
    }
}

@Composable
fun MicronutrientsScreen() {
    val s by Store.state.collectAsState()
    var days by remember { mutableIntStateOf(7) }
    val e = Store.energyFor(s, LocalDate.now())
    val rep = remember(s.diary.size, s.profile, days) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal, LocalDate.now(), days) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Vitamine e minerali", onBack = { Nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30).forEach { d -> Pill("$d giorni", days == d) { days = d } }
            }
            Spacer(Modifier.height(10.dp))
            JCard(color = C.BlueSoft) {
                Text("Come leggere questi dati", style = T.Title2)
                Text("Confronto la media dei giorni registrati (almeno metà delle calorie inserite) con i valori di riferimento EFSA per adulti. " +
                        "Un apporto basso non è una carenza: solo esami del sangue valutati dal medico possono accertarla. " +
                        "La vitamina D dipende soprattutto dal sole, non solo dal cibo.", style = T.Read.copy(fontSize = T.Body.fontSize))
                Spacer(Modifier.height(6.dp))
                Text("Giorni analizzati: ${rep.loggedDays}. Calorie con dati completi di micronutrienti: ${(rep.coverage * 100).r0()}% " +
                        "(i prodotti confezionati spesso non li riportano).", style = T.Small)
            }
            if (rep.loggedDays == 0) { EmptyState("📊", "Ancora nessun giorno completo", "Registra i pasti di una giornata per vedere l'analisi."); return@Column }

            val low = rep.low
            if (low.isNotEmpty()) {
                SectionTitle("Da migliorare")
                low.forEach { st ->
                    JCard(Modifier.padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(C.AccentSoft), contentAlignment = Alignment.Center) {
                                Text("${st.pct.r0()}%", style = T.Label.copy(color = C.AccentDeep))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(st.def.name, style = T.Title2)
                                Text("Media ${Micros.fmt(st.avg, st.def.unit)} su ${Micros.fmt(st.ref, st.def.unit)} (${if (st.def.kind == "PRI") "assunzione raccomandata" else "assunzione adeguata"}) · ${st.def.role}", style = T.Small)
                            }
                        }
                        val tips = MicroAnalysis.tips(Db.db, st, s.dietPrefs.exclude)
                        if (tips.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Cosa aggiungere", style = T.Label)
                            tips.forEach { t ->
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Text("• ${t.grams.r0()} ${if (t.food.liquid) "ml" else "g"} di ${t.food.name.lowercase()}", style = T.Body, modifier = Modifier.weight(1f))
                                    Text("+${Micros.fmt(t.amount, st.def.unit)} (${t.pctOfRef.r0()}%)", style = T.Small.copy(color = C.Green))
                                }
                            }
                        }
                    }
                }
            }
            if (rep.high.isNotEmpty()) {
                SectionTitle("Da limitare")
                rep.high.forEach { st ->
                    JCard(Modifier.padding(bottom = 10.dp), color = C.DangerSoft) {
                        Text("${st.def.name}: ${Micros.fmt(st.avg, st.def.unit)} al giorno", style = T.Title2)
                        Text("Oltre il limite di ${Micros.fmt(st.ref, st.def.unit)} · ${st.def.role}", style = T.Small)
                    }
                }
            }

            SectionTitle("Da quanto tempo non mangi…")
            JCard {
                rep.groups.forEachIndexed { i, g ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.label, style = T.Label)
                            Text(g.tip, style = T.Small)
                        }
                        val d = g.daysSince
                        Tag(when { d == null -> "mai"; d == 0 -> "oggi"; d == 1 -> "ieri"; else -> "$d giorni" },
                            when { d == null || d >= 7 -> C.AccentDeep; d >= 4 -> C.Gold; else -> C.Green })
                    }
                    if (i < rep.groups.lastIndex) HorizontalDivider(color = C.Line)
                }
            }

            SectionTitle("Tutti i nutrienti")
            JCard {
                rep.statuses.forEach { MicroRow(it) }
                Spacer(Modifier.height(6.dp))
                Text("La linea verticale indica il 100% del riferimento. Iodio non disponibile nel database USDA.", style = T.Small)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
