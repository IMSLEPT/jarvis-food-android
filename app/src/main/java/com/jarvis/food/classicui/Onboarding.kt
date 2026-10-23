package com.jarvis.food.classicui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.Profile

/** Prima configurazione: il questionario completo, poi si parte. */
@Composable
fun Onboarding() {
    val d = remember { QDraft(Profile(name = ""), true, 75.0) }
    val steps = remember { QStep.entries.toList() }
    AppBackground {
        QuestionnaireFlow(d, steps, "Inizia", onBack = null) { applyDraft(d) }
    }
}

@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    AChoice(selected, Modifier.fillMaxWidth().padding(vertical = 4.dp), minHeight = 58.dp, radius = 24.dp, onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text, style = T.Body.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = A.Ink), modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 12.dp))
            Box(
                Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (selected) A.Ink else androidx.compose.ui.graphics.Color.Transparent)
                    .border(2.dp, if (selected) A.Ink else androidx.compose.ui.graphics.Color(0xFFC9C5D8), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { if (selected) AIcon(Ic.Check, androidx.compose.ui.graphics.Color.White, 14.dp, 3.4f) }
        }
    }
}

@Composable
fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, labelColor: androidx.compose.ui.graphics.Color = C.Muted) {
    Column {
        Text(value, style = T.Title2.copy(color = color))
        Text(label, style = T.Small.copy(color = labelColor))
    }
}
