package com.jarvis.food.ui

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
    SettingsChoice(text, selected = selected, modifier = Modifier.padding(vertical = 4.dp), onClick = onClick)
}

@Composable
fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, labelColor: androidx.compose.ui.graphics.Color = C.Muted) {
    Column {
        Text(value, style = T.Title2.copy(color = color))
        Text(label, style = T.Small.copy(color = labelColor))
    }
}
