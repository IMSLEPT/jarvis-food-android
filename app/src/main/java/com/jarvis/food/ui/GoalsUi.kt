package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pulsante tondo 44 px con "−" o "+" (stepper del design Obiettivi). */
@Composable
fun AStepBtn(plus: Boolean, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(C.cardSurface(bg)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        AIcon(if (plus) "M12 6v12M6 12h12" else "M6 12h12", ink, 20.dp, 2.8f)
    }
}

/** Scheda selezionabile: bordo navy 2 px e fondo bianco se attiva, fondo bianco trasparente altrimenti. */
@Composable
fun AChoice(selected: Boolean, modifier: Modifier = Modifier, minHeight: Dp = 64.dp, radius: Dp = 24.dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.heightIn(min = minHeight).clip(shape).background(if (selected) Color.White else Color(0xB3FFFFFF))
            .border(2.dp, if (selected) A.Ink else A.Hair, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

/** Selettore a segmenti (pista grigia 5 px, voce attiva navy), in riga o in colonna. */
@Composable
fun ASegment(options: List<String>, selected: Int, vertical: Boolean = false, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val track = RoundedCornerShape(26.dp)
    @Composable
    fun item(i: Int, m: Modifier) {
        val sel = i == selected
        Box(m.height(44.dp).clip(RoundedCornerShape(21.dp)).background(if (sel) C.Navy else Color.Transparent).clickable { onPick(i) }, contentAlignment = Alignment.Center) {
            Text(options[i], style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else A.Ink), maxLines = 1, textAlign = TextAlign.Center)
        }
    }
    if (vertical) Column(modifier.fillMaxWidth().clip(track).background(C.Surface).padding(5.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.indices.forEach { item(it, Modifier.fillMaxWidth()) }
    } else Row(modifier.fillMaxWidth().clip(track).background(C.Surface).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.indices.forEach { item(it, Modifier.weight(1f)) }
    }
}

/** Chip di esclusione alimentare (44 px, bordo 2 px; attiva = navy). */
@Composable
fun AChip(label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.heightIn(min = 44.dp).clip(shape).background(if (on) C.Navy else Color.White)
            .border(2.dp, if (on) A.Ink else Color(0x1F1D1B3A), shape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = T.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else A.Ink), maxLines = 1) }
}

/** Titolo di sezione dell'Obiettivi (Bricolage 22, margini 28/12). */
@Composable
fun GHead(text: String, sub: String? = null) {
    Text(text, style = T.H2, modifier = Modifier.padding(top = 28.dp, bottom = if (sub == null) 12.dp else 4.dp))
    if (sub != null) Text(sub, style = T.Body.copy(fontSize = 13.sp, color = A.Muted), modifier = Modifier.padding(bottom = 12.dp))
}

/** Pulsante "Salva" fisso in fondo (arancione, 56 px). */
@Composable
fun GSave(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    // Keep the entire touch target above Android's buttons or gesture handle.
    // imePadding on the form already consumes this inset when the keyboard is open.
    APrimary(text, Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), height = 56.dp, enabled = enabled, onClick = onClick)
}
