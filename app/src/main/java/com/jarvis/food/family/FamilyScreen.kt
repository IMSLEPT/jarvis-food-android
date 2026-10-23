package com.jarvis.food.family

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.Db
import com.jarvis.food.data.Store
import com.jarvis.food.engine.formatAmount
import com.jarvis.food.ui.A
import com.jarvis.food.ui.ACard
import com.jarvis.food.ui.AIcon
import com.jarvis.food.ui.AInk
import com.jarvis.food.ui.APill
import com.jarvis.food.ui.APrimary
import com.jarvis.food.ui.C
import com.jarvis.food.ui.GapH
import com.jarvis.food.ui.GapW
import com.jarvis.food.ui.Ic
import com.jarvis.food.ui.JField
import com.jarvis.food.ui.Nav
import com.jarvis.food.ui.T
import com.jarvis.food.ui.TopBar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================================================
// Famiglia: invito, membri, spesa condivisa e "cucinare insieme".
// ============================================================================================

/** Scheda aperta nella schermata Famiglia (fuori dalla composizione per poterla impostare nei test). */
object FamilyUi { var tab by mutableIntStateOf(0) }

@Composable
fun FamilyScreen() {
    val s by Store.state.collectAsState()
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        FamilyStore.screenOpen = true
        onDispose { FamilyStore.screenOpen = false }
    }
    LaunchedEffect(s.family != null) { if (s.family != null) runCatching { FamilyStore.tick() } }

    Column(Modifier.fillMaxSize()) {
        TopBar("Famiglia", onBack = { Nav.back() })
        if (s.family == null) FamilyIntro() else FamilyLinked(ctx)
    }
}

// ------------------------------------------------------------------ Non collegato

@Composable
private fun FamilyIntro() {
    val s by Store.state.collectAsState()
    val st by FamilyStore.status.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(s.profile.name) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
        st.notice?.let {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(A.FatSoft).padding(14.dp)) { Text(it, style = T.Body.copy(color = A.FatInk)) }
            GapH(12.dp)
        }
        ACard(radius = 28.dp, padding = 18.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(18.dp)).background(A.Dawn), contentAlignment = Alignment.Center) { AIcon(Ic.Family, Color.White, 26.dp, 2.1f) }
                    GapW(12.dp)
                    Text("Una sola spesa per tutti", style = T.Title2.copy(fontSize = 19.sp, lineHeight = 24.sp), modifier = Modifier.weight(1f))
                }
                GapH(12.dp)
                Text("Collega i telefoni dei tuoi familiari: ognuno tiene la propria dieta e i propri obiettivi, ma la lista della spesa è unica, si aggiorna da sola e somma le quantità (120 g + 80 g di pasta = 200 g).",
                    style = T.Body.copy(color = A.Muted))
            }
        }
        GapH(14.dp)
        JField(name, { name = it }, "Il tuo nome nella famiglia")
        GapH(14.dp)
        APrimary(if (busy) "Attendi…" else "Crea una famiglia", Modifier.fillMaxWidth(), enabled = !busy && name.isNotBlank()) {
            busy = true; error = null
            scope.launch {
                try { FamilyStore.create(name) } catch (e: Exception) { error = "Non riesco a creare la famiglia: ${e.message ?: "controlla la connessione"}." }
                busy = false
            }
        }
        GapH(22.dp)
        Text("Hai ricevuto un invito?", style = T.H2)
        GapH(8.dp)
        JField(code, { code = it; error = null }, "Codice d'invito (JF1.…)", singleLine = false)
        GapH(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AInk("Incolla", bg = A.Tonal, fg = A.TonalInk) { code = readClip(ctx) }
            AInk(if (busy) "Attendi…" else "Entra", Modifier.weight(1f)) {
                if (busy) return@AInk
                busy = true; error = null
                scope.launch {
                    try { FamilyStore.join(code, name) } catch (e: Exception) { error = e.message ?: "Non riesco a collegarmi: controlla la connessione." }
                    busy = false
                }
            }
        }
        error?.let { GapH(10.dp); Text(it, style = T.Body.copy(color = C.Danger)) }
        GapH(18.dp)
        PrivacyCard()
        GapH(30.dp)
    }
}

@Composable
private fun PrivacyCard() {
    ACard(radius = 24.dp, padding = 16.dp, color = A.Peach) {
        Column {
            Text("Cosa si condivide e cosa no", style = T.Title2)
            GapH(8.dp)
            Text("Condiviso: il tuo nome, gli ingredienti (con le quantità) dei pasti dei giorni scelti, i prodotti aggiunti a mano e le spunte \"comprato\".", style = T.Body.copy(color = A.Muted))
            GapH(6.dp)
            Text("Mai condiviso: peso, misure, calorie e macro, diario, obiettivi, dieta. Nessuno può modificare il tuo piano.", style = T.Body.copy(color = A.Muted))
            GapH(6.dp)
            Text("I dati viaggiano cifrati: il servizio che li conserva non può leggerli. Solo chi ha il codice d'invito può farlo.", style = T.Small)
        }
    }
}

// ------------------------------------------------------------------ Collegato

@Composable
private fun FamilyLinked(ctx: Context) {
    val s by Store.state.collectAsState()
    val doc by FamilyStore.doc.collectAsState()
    val st by FamilyStore.status.collectAsState()
    val link = s.family ?: return
    val tab = FamilyUi.tab
    val d = doc ?: FamilyDoc()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
        // stato sincronizzazione
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ok = st.error == null
            Box(Modifier.size(9.dp).clip(CircleShape).background(if (st.syncing) C.Gold else if (ok) C.Green else C.Danger))
            GapW(8.dp)
            Text(
                when {
                    st.error != null -> st.error!!
                    st.syncing -> "Sincronizzazione…"
                    st.lastOk > 0 -> "Aggiornato ${ago(st.lastOk)}"
                    else -> "Collegamento in corso…"
                }, style = T.Small, modifier = Modifier.weight(1f)
            )
        }
        GapH(12.dp)
        MembersStrip(d, link.memberId)
        GapH(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            APill("Spesa", tab == 0, Modifier.weight(1f), hPad = 8.dp) { FamilyUi.tab = 0 }
            APill("Insieme", tab == 1, Modifier.weight(1f), hPad = 8.dp) { FamilyUi.tab = 1 }
            APill("Membri", tab == 2, Modifier.weight(1f), hPad = 8.dp) { FamilyUi.tab = 2 }
        }
        GapH(16.dp)
        when (tab) {
            0 -> ShopTab(d, link)
            1 -> TogetherTab(d, link)
            else -> MembersTab(d, link, ctx)
        }
        GapH(36.dp)
    }
}

@Composable
private fun MembersStrip(d: FamilyDoc, me: String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        d.activeMembers().forEachIndexed { i, m ->
            val (bg, ink) = MEMBER_COLORS[i % MEMBER_COLORS.size]
            Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(20.dp)).padding(start = 4.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Text(m.name.take(1).uppercase(), style = T.BtnS.copy(color = ink)) }
                GapW(8.dp)
                Text(if (m.id == me) "${m.name} (tu)" else m.name, style = T.BtnS, maxLines = 1)
            }
        }
    }
}

private val MEMBER_COLORS = listOf(A.ProteinSoft to A.ProteinInk, A.GreenSoft to A.GreenInk, A.CarbSoft to A.CarbInk, A.FatSoft to A.FatInk, A.FastSoft to A.FastInk, A.Tonal to A.TonalInk)

// ------------------------------------------------------------------ Spesa

@Composable
private fun ShopTab(d: FamilyDoc, link: com.jarvis.food.data.FamilyLink) {
    val items = remember(d) { FamilyStore.aggregate(d) }
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    fun open(i: AggItem) = !i.checked || i.missingAfterCheck() > 0.5
    val toBuy = items.filter { open(it) }
    val done = items.filterNot { open(it) }

    // giorni inclusi
    Text("Giorni dei piani inclusi", style = T.Eyebrow)
    GapH(8.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        APill("Oggi", link.span == 1, Modifier.weight(1f), hPad = 8.dp) { FamilyStore.setSpan(1) }
        APill("3 giorni", link.span == 3, Modifier.weight(1f), hPad = 8.dp) { FamilyStore.setSpan(3) }
        APill("Settimana", link.span == 7, Modifier.weight(1f), hPad = 8.dp) { FamilyStore.setSpan(7) }
    }
    GapH(16.dp)

    ACard(radius = 24.dp, padding = 14.dp) {
        Column {
            Text("Aggiungi un prodotto", style = T.Title2)
            GapH(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                JField(name, { name = it }, "Prodotto", Modifier.weight(1f))
                JField(qty, { qty = it }, "Quantità", Modifier.width(112.dp))
            }
            GapH(10.dp)
            APrimary("Aggiungi alla lista", Modifier.fillMaxWidth(), height = 48.dp, enabled = name.isNotBlank()) {
                FamilyStore.addManual(name, qty); name = ""; qty = ""
            }
            GapH(4.dp)
            Text("Esempi di quantità: 500 g, 2 kg, 1 l, 6 (pezzi).", style = T.Small)
        }
    }
    GapH(18.dp)

    if (items.isEmpty()) {
        Text("La lista è vuota. Genera il piano dei pasti, usa \"Spesa famiglia\" in una ricetta oppure aggiungi un prodotto qui sopra.", style = T.Muted)
        return
    }

    Text("Da comprare (${toBuy.size})", style = T.H2)
    GapH(8.dp)
    var lastShop = ""
    for (it in toBuy) {
        if (it.shop != lastShop) { lastShop = it.shop; Text(it.shop.uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) }
        ShopRow(it, d, link)
        GapH(8.dp)
    }
    if (toBuy.isEmpty()) Text("Hai preso tutto.", style = T.Muted)
    if (done.isNotEmpty()) {
        GapH(18.dp)
        Text("Già comprati (${done.size})", style = T.H2)
        GapH(8.dp)
        for (it in done) { ShopRow(it, d, link); GapH(8.dp) }
    }
}

@Composable
private fun ShopRow(item: AggItem, d: FamilyDoc, link: com.jarvis.food.data.FamilyLink) {
    val missing = item.missingAfterCheck()
    val partlyBought = item.checked && missing > 0.5
    val done = item.checked && !partlyBought
    val f = item.foodId?.let { Db.db.food(it) }
    val shown = if (partlyBought) missing else item.grams
    val qtyText = when {
        f != null && shown > 0.5 -> fmtQty(f.id, shown, f.liquid)
        item.qtyTexts.isNotEmpty() -> item.qtyTexts.joinToString(" + ")
        else -> ""
    }
    val pack = f?.let { ff -> if (shown > 0.5) FamilyAgg.packs(shown, ff.pack)?.let { n -> if (ff.pack != null && n >= 1 && ff.id != "uova") "≈ $n × ${fmtQty(ff.id, ff.pack, ff.liquid)}" else null } else null }
    val sub = buildString {
        val manual = item.parts.filter { it.source != "plan" }
        val plan = item.parts.filter { it.source == "plan" }
        if (plan.isNotEmpty()) {
            val per = plan.groupBy { it.memberName }.map { (n, ps) -> "$n ${fmtQty(f?.id ?: "", ps.sumOf { it.grams }, f?.liquid == true)}" }
            if (plan.map { it.memberName }.distinct().size > 1 || manual.isNotEmpty()) append(per.joinToString(" · "))
            else append("Dal piano di ${plan.first().memberName}")
        }
        if (manual.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            val who = manual.map { it.memberName }.distinct().joinToString(", ")
            append("aggiunto da $who")
        }
        if (done) { val c = item.check; if (c != null) { if (isNotEmpty()) append(" · "); append("preso da ${c.byName.ifBlank { d.name(c.by) }}") } }
        if (partlyBought) { if (isNotEmpty()) append(" · "); append("il piano è cambiato: mancano ancora") }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (done) Color(0xFFF1F3F6) else Color.White).border(1.dp, A.Hair, RoundedCornerShape(20.dp))
            .clickable { FamilyStore.setChecked(item, !done) }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (done) C.Green else Color.White).border(2.dp, if (done) C.Green else Color(0xFFC9CCDA), CircleShape), contentAlignment = Alignment.Center) {
            if (done) AIcon(Ic.Check, Color.White, 16.dp, 3f)
        }
        GapW(12.dp)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = T.Label.copy(color = if (done) C.Faint else C.Text), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) Text(sub, style = T.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        GapW(8.dp)
        Column(horizontalAlignment = Alignment.End) {
            if (qtyText.isNotBlank()) Text(qtyText, style = T.BtnS.copy(color = if (done) C.Faint else A.Ink), maxLines = 1)
            if (pack != null && !done) Text(pack, style = T.Small, maxLines = 1)
        }
        if (item.manualEntryIds.isNotEmpty()) {
            GapW(6.dp)
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { item.manualEntryIds.forEach { FamilyStore.removeEntry(it) } }, contentAlignment = Alignment.Center) {
                AIcon(Ic.Close, C.Faint, 16.dp, 2.4f)
            }
        }
    }
}

// ------------------------------------------------------------------ Cucinare insieme

@Composable
private fun TogetherTab(d: FamilyDoc, link: com.jarvis.food.data.FamilyLink) {
    val today = LocalDate.now().toString()
    val list = remember(d) { FamilyCook.together(d, today) }
    if (list.isEmpty()) {
        ACard(radius = 24.dp, padding = 16.dp) {
            Text("Quando due o più familiari hanno nel piano la stessa ricetta nello stesso giorno, qui trovi le quantità totali per cucinarla una volta sola e la porzione di ciascuno. Al momento non ci sono ricette in comune nei giorni scelti.", style = T.Body.copy(color = A.Muted))
        }
        return
    }
    for (c in list) {
        ACard(radius = 26.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(dayLabel(c.date), style = T.Eyebrow)
                Text(c.recipeName, style = T.Title2.copy(fontSize = 19.sp, lineHeight = 24.sp))
                GapH(2.dp)
                Text("Insieme: " + c.members.joinToString(", ") { (id, n) -> "$n (${c.slots[id] ?: ""})" }, style = T.Muted)
                GapH(12.dp)
                Text("Quantità totali da usare", style = T.Label)
                GapH(4.dp)
                Text(c.total.joinToString("\n") { "• ${it.name}: ${lineQty(it)}" }, style = T.Body)
                GapH(12.dp)
                Text("Porzioni individuali", style = T.Label)
                for ((id, name) in c.members) {
                    GapH(6.dp)
                    Text(name, style = T.BtnS.copy(color = A.TonalInk))
                    Text((c.perMember[id] ?: emptyList()).joinToString(" · ") { "${it.name.lowercase()} ${lineQty(it)}" }, style = T.Muted)
                }
                GapH(8.dp)
                Text("Le porzioni sono calcolate sulla dieta di ciascuno: cucina tutto insieme e dividi secondo queste quantità.", style = T.Small)
            }
        }
        GapH(12.dp)
    }
}

// ------------------------------------------------------------------ Membri

@Composable
private fun MembersTab(d: FamilyDoc, link: com.jarvis.food.data.FamilyLink, ctx: Context) {
    val scope = rememberCoroutineScope()
    var removing by remember { mutableStateOf<FMember?>(null) }
    var leaving by remember { mutableStateOf(false) }

    ACard(radius = 24.dp, padding = 16.dp) {
        Column {
            Text("Invita un familiare", style = T.Title2)
            GapH(6.dp)
            Text("Manda questo codice: chi lo inserisce in Famiglia entra nella lista condivisa. Chi ha il codice può leggere e modificare la lista, quindi condividilo solo con le persone di fiducia.", style = T.Muted)
            GapH(10.dp)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(A.Peach).border(1.dp, A.Hair, RoundedCornerShape(16.dp)).padding(12.dp)) {
                Text(link.code, style = T.Small.copy(color = A.Ink, fontSize = 11.5.sp, lineHeight = 16.sp))
            }
            GapH(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AInk("Condividi", Modifier.weight(1f)) {
                    val text = "Ti invito nella spesa di famiglia su Jarvis Food. Apri Famiglia dalla Home, scegli \"Entra\" e incolla questo codice:\n\n${link.code}"
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                    ctx.startActivity(Intent.createChooser(send, "Invita in famiglia").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                AInk("Copia", bg = A.Tonal, fg = A.TonalInk) {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Codice famiglia", link.code))
                    Toast.makeText(ctx, "Codice copiato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    GapH(16.dp)
    Text("Membri (${d.activeMembers().size})", style = T.H2)
    GapH(8.dp)
    d.activeMembers().forEachIndexed { i, m ->
        val (bg, ink) = MEMBER_COLORS[i % MEMBER_COLORS.size]
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp, A.Hair, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Text(m.name.take(1).uppercase(), style = T.Num.copy(color = ink, fontSize = 18.sp)) }
            GapW(12.dp)
            Column(Modifier.weight(1f)) {
                Text(if (m.id == link.memberId) "${m.name} (tu)" else m.name, style = T.Label)
                Text(if (m.id == link.memberId) "Questo telefono" else "Dieta e dati personali non visibili", style = T.Small)
            }
            if (m.id != link.memberId) Text("Rimuovi", style = T.Link, modifier = Modifier.clickable { removing = m }.padding(8.dp))
        }
        GapH(8.dp)
    }
    GapH(10.dp)
    PrivacyCard()
    GapH(14.dp)
    AInk("Esci dalla famiglia", Modifier.fillMaxWidth(), bg = C.DangerSoft, fg = C.Danger) { leaving = true }

    removing?.let { m ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Rimuovere ${m.name}?") },
            text = { Text("Sparirà dalla famiglia e i suoi prodotti dalla lista. Non vedrà più gli aggiornamenti, ma chi ha il codice d'invito potrebbe ancora leggerne il contenuto: per escluderlo davvero crea una nuova famiglia.") },
            confirmButton = { TextButton(onClick = { FamilyStore.removeMember(m.id); removing = null }) { Text("Rimuovi") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Annulla") } })
    }
    if (leaving) {
        AlertDialog(onDismissRequest = { leaving = false }, title = { Text("Uscire dalla famiglia?") },
            text = { Text("Questo telefono smette di condividere la spesa e i tuoi prodotti spariscono dalla lista degli altri. La tua dieta e i tuoi dati restano qui.") },
            confirmButton = { TextButton(onClick = { leaving = false; scope.launch { FamilyStore.leave() } }) { Text("Esci") } },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text("Annulla") } })
    }
}

// ------------------------------------------------------------------ Utilità

private fun readClip(ctx: Context): String {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
}

private fun ago(t: Long): String {
    val s = (System.currentTimeMillis() - t) / 1000
    return when { s < 10 -> "ora"; s < 60 -> "$s s fa"; s < 3600 -> "${s / 60} min fa"; else -> "${s / 3600} h fa" }
}

private fun dayLabel(date: String): String {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    val today = LocalDate.now()
    return when (d) {
        today -> "OGGI"
        today.plusDays(1) -> "DOMANI"
        else -> (d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN) + " " + d.dayOfMonth).uppercase()
    }
}

/** "120 g", "1,2 kg", "3 uova": usa l'unità giusta per liquidi e uova. */
internal fun fmtQty(foodId: String, grams: Double, liquid: Boolean): String {
    val f = if (foodId.isNotBlank()) Db.db.food(foodId) else null
    if (f != null && grams < 1000) return formatAmount(f, grams)
    if (grams >= 1000) {
        val v = grams / 1000
        val t = String.format(Locale.ITALY, "%.1f", v).removeSuffix(",0")
        return t + if (liquid) " l" else " kg"
    }
    return grams.roundToInt().toString() + if (liquid) " ml" else " g"
}

private fun lineQty(l: FLine): String =
    if (l.grams > 0.5) fmtQty(l.foodId ?: "", l.grams, l.foodId?.let { Db.db.food(it)?.liquid } == true) else l.qty
