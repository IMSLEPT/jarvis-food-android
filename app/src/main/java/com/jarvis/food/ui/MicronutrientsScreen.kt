package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.Surface, C.Surface)))
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

private val VIT_KEYS = setOf("vitA", "vitC", "vitD", "vitE", "vitK", "thiamin", "riboflavin", "niacin", "vitB6", "folate", "vitB12")
private val MARKER = Color(0xFF17286B)

private fun microFill(pct: Double): Color = if (pct >= 90) Color(0xFF10B981) else if (pct >= 50) Color(0xFFF59E0B) else Color(0xFFF43F6E)
private fun microInk(pct: Double): Color = if (pct >= 90) Color(0xFF047857) else if (pct >= 50) Color(0xFFB45309) else Color(0xFFC41F4C)

/** Barra con la linea verticale del 100% (a 2/3 della lunghezza: la scala arriva al 150%). */
@Composable
private fun NBar(pct: Double, h: Dp, fill: Brush, track: Color, markerExtra: Dp = 8.dp) {
    Canvas(Modifier.fillMaxWidth().height(h + markerExtra)) {
        val hp = h.toPx()
        val top = (size.height - hp) / 2
        val r = CornerRadius(hp / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, hp), r)
        val w = size.width * (minOf(pct, 150.0) / 150.0).toFloat()
        if (w > 0f) drawRoundRect(fill, Offset(0f, top), Size(maxOf(w, hp), hp), r)
        val mx = size.width * (100f / 150f)
        drawRoundRect(MARKER, Offset(mx - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height), CornerRadius(1.dp.toPx()))
    }
}

@Composable
private fun MCard(modifier: Modifier = Modifier, brush: Brush? = null, glow: Color = Color(0x731D1B3A), content: @Composable () -> Unit) {
    ACard(modifier.fillMaxWidth(), radius = 30.dp, padding = 0.dp, elevation = 8.dp, glow = glow, brush = brush) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

@Composable
fun MicronutrientsScreen() {
    val s by Store.state.collectAsState()
    var days by remember { mutableIntStateOf(7) }
    var tab by remember { mutableIntStateOf(0) }
    val e = Store.energyFor(s, LocalDate.now())
    val rep = remember(s.diary.size, s.profile, days) { MicroAnalysis.analyze(s.diary, s.profile, e.baseKcal, LocalDate.now(), days) }
    val heroShape = RoundedCornerShape(34.dp)
    Column(Modifier.fillMaxSize()) {
        TopBar("Vitamine e minerali", onBack = { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
            val ranges = listOf(7, 14, 30)
            ASegment(ranges.map { "$it giorni" }, ranges.indexOf(days), modifier = Modifier.padding(top = 4.dp)) { days = ranges[it] }
            if (rep.loggedDays == 0) {
                Box(Modifier.padding(top = 12.dp)) { EmptyState("📊", "Ancora nessun giorno completo", "Registra i pasti di una giornata per vedere l'analisi.") }
                return@Column
            }
            val reach = rep.statuses.filter { it.def.kind != "LIM" }
            val ok = reach.count { it.pct >= 90 }
            val tot = reach.size.coerceAtLeast(1)

            // Hero
            Row(
                Modifier.padding(top = 14.dp).fillMaxWidth().aShadow(heroShape, 14.dp, Color(0x8C10B981)).clip(heroShape)
                    .background(C.Surface).border(1.dp, Color(0xE6FFFFFF), heroShape).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 12.dp.toPx()
                        val sz = Size(size.width - sw, size.height - sw)
                        drawArc(Color(0xE6FFFFFF), 0f, 360f, false, Offset(sw / 2, sw / 2), sz, style = Stroke(sw))
                        drawArc(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF5CD6EA))), -90f, 360f * ok / tot, false, Offset(sw / 2, sw / 2), sz, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$ok", style = T.H1.copy(fontSize = 30.sp, lineHeight = 32.sp))
                        Text("/$tot", style = T.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = A.Muted), modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                GapW(18.dp)
                Column(Modifier.weight(1f)) {
                    Text("$ok su $tot nutrienti al riferimento", style = T.H1.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp))
                    Text("Media degli ultimi ${rep.loggedDays} giorni registrati, sul riferimento EFSA per adulti. Il 100% è la linea sulla barra.",
                        style = T.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF3F5A52)), modifier = Modifier.padding(top = 6.dp))
                }
            }

            // Da migliorare
            val low = reach.filter { it.level != "ok" && it.pct < 90 }.sortedBy { it.pct }.take(5)
            if (low.isNotEmpty()) {
                GHead("Da migliorare")
                MCard {
                    low.forEachIndexed { i, st ->
                        Column(Modifier.padding(top = 14.dp, bottom = 10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(st.def.name, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                                Text("${st.pct.r0()}%", style = T.H1.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp, color = microInk(st.pct)))
                            }
                            Spacer(Modifier.height(4.dp))
                            NBar(st.pct, 12.dp, SolidColor(microFill(st.pct)), Color(0xFFEBEFF3))
                        }
                        if (i < low.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                    }
                }

                // Cosa aggiungere
                val ideas = low.take(3).mapNotNull { st -> MicroAnalysis.tips(Db.db, st, s.dietPrefs.exclude, 1).firstOrNull()?.let { st to it } }
                if (ideas.isNotEmpty()) {
                    GHead("Cosa aggiungere")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val tints = listOf(A.Tonal to A.TonalInk, A.CarbSoft to A.CarbInk, A.FatSoft to A.FatInk)
                        ideas.forEachIndexed { i, (st, t) ->
                            val inList = s.shopping.firstOrNull { it.name.equals(t.food.name, ignoreCase = true) }
                            ACard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 14.dp, elevation = 8.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TonalIcon(Ic.Bowl, tints[i % 3].second, tints[i % 3].first, box = 52.dp, radius = 19.dp, size = 26.dp)
                                    GapW(14.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text("${t.grams.r0()} ${if (t.food.liquid) "ml" else "g"} di ${t.food.name.lowercase()}", style = T.Body.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                                        Text("Per ${st.def.name.lowercase()}: +${Micros.fmt(t.amount, st.def.unit)} (${t.pctOfRef.r0()}%)", style = T.Small.copy(fontSize = 12.5.sp, color = A.Muted), modifier = Modifier.padding(top = 2.dp))
                                    }
                                    GapW(10.dp)
                                    Box(
                                        Modifier.size(48.dp).clip(CircleShape).background(if (inList != null) Color(0xFF10B981) else A.Ink)
                                            .clickable { if (inList != null) Store.removeShopping(inList.id) else Store.addShoppingManual(t.food.name) },
                                        contentAlignment = Alignment.Center
                                    ) { AIcon(if (inList != null) Ic.Check else "M12 5v14M5 12h14", if (inList != null) Color.White else C.Accent, 22.dp, 2.6f) }
                                }
                            }
                        }
                    }
                    Text("Il + aggiunge l'alimento alla lista della spesa.", style = T.Small.copy(fontSize = 12.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                }
            }

            // Da limitare
            if (rep.high.isNotEmpty()) {
                GHead("Da limitare")
                MCard(glow = C.Navy) {
                    rep.high.forEachIndexed { i, st ->
                        Column(Modifier.padding(top = 14.dp, bottom = 10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(st.def.name, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
                                Text("${st.pct.r0()}% del limite", style = T.H1.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp, color = Color(0xFFB42318)))
                            }
                            Text("${Micros.fmt(st.avg, st.def.unit)} al giorno · limite ${Micros.fmt(st.ref, st.def.unit)}", style = T.Small.copy(fontSize = 12.sp, color = A.Muted))
                            Spacer(Modifier.height(4.dp))
                            NBar(st.pct, 12.dp, Brush.horizontalGradient(listOf(Color(0xFFFF8A2B), Color(0xFFE5484D))), Color(0xFFFFE0D3))
                        }
                        if (i < rep.high.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x24E88C60)))
                    }
                }
            }

            // Da quanto tempo non mangi
            GHead("Da quanto tempo non mangi…", "Gruppi di alimenti che non compaiono nel diario da un po'.")
            MCard {
                rep.groups.forEachIndexed { i, g ->
                    val d = g.daysSince
                    val shown = d ?: 10
                    val col = if (shown >= 7) Color(0xFFF43F6E) else if (shown >= 4) Color(0xFFF59E0B) else Color(0xFF10B981)
                    val ink = if (shown >= 7) Color(0xFFC41F4C) else if (shown >= 4) Color(0xFFB45309) else Color(0xFF047857)
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.label, style = T.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = A.Ink))
                            ABar((shown / 10f).coerceIn(0f, 1f), SolidColor(col), Color(0xFFEBEFF3), 8.dp, Modifier.padding(top = 8.dp))
                        }
                        GapW(14.dp)
                        Column(Modifier.width(64.dp), horizontalAlignment = Alignment.End) {
                            Text(if (d == null) "mai" else "$d", style = T.H1.copy(fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = 0.sp, color = ink))
                            Text(if (d == 1) "giorno" else "giorni", style = T.Small.copy(fontSize = 11.5.sp, color = A.Muted))
                        }
                    }
                    if (i < rep.groups.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                }
            }

            // Tutti i nutrienti
            GHead("Tutti i nutrienti")
            ASegment(listOf("Vitamine", "Minerali"), tab) { tab = it }
            val list = if (tab == 0) rep.statuses.filter { it.def.key in VIT_KEYS } else rep.statuses.filter { it.def.key !in VIT_KEYS }
            Box(Modifier.padding(top = 12.dp)) {
                MCard {
                    list.forEachIndexed { i, st ->
                        val lim = st.def.kind == "LIM"
                        val c = if (lim) (if (st.level == "alto") Color(0xFFF43F6E) else Color(0xFF10B981)) else microFill(st.pct)
                        val ink = if (lim) (if (st.level == "alto") Color(0xFFC41F4C) else Color(0xFF047857)) else microInk(st.pct)
                        Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(st.def.name, style = T.Body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = A.Ink), modifier = Modifier.weight(1f))
                                Text("${st.pct.r0()}%", style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                            }
                            Spacer(Modifier.height(5.dp))
                            NBar(st.pct, 8.dp, SolidColor(c), Color(0xFFEBEFF3), markerExtra = 6.dp)
                        }
                        if (i < list.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEBEFF3)))
                    }
                }
            }
            Text("Riferimenti EFSA per adulti. Un apporto basso non è una carenza: solo gli esami del sangue valutati dal medico possono accertarla; la vitamina D dipende soprattutto dal sole. " +
                "Giorni analizzati: ${rep.loggedDays}; dati completi di micronutrienti sul ${(rep.coverage * 100).r0()}% delle calorie (i prodotti confezionati spesso non li riportano). Iodio non disponibile nel database USDA.",
                style = T.Small.copy(fontSize = 12.sp, lineHeight = 18.sp, color = A.Muted), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
        }
    }
}
