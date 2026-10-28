package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared hierarchy for the editable destinations of Profile, without changing app-wide components. */
@Composable
fun SettingsHeading(title: String, subtitle: String? = null, warm: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp).width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if (warm) C.Accent else C.Navy))
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(title, style = T.Title2.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold))
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = T.Small.copy(lineHeight = 18.sp), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun SettingsCard(title: String? = null, subtitle: String? = null, warm: Boolean = false,
                 modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(C.Surface).border(1.dp, C.Line, shape)) {
        if (title != null) Column(Modifier.fillMaxWidth().background(if (warm) C.AccentSoft else C.NavySoft).padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(title, style = T.Label.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold))
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = T.Small.copy(lineHeight = 18.sp), modifier = Modifier.padding(top = 3.dp))
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun SettingsIntro(title: String, description: String, warm: Boolean = false) {
    SettingsCard(title = title, warm = warm) { Text(description, style = T.Small.copy(lineHeight = 19.sp)) }
}

@Composable
fun SettingsChoice(title: String, description: String? = null, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(if (selected) C.NavySoft else C.Surface)
        .border(if (selected) 2.dp else 1.dp, if (selected) C.Navy else C.Line, shape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label.copy(fontSize = 15.sp, lineHeight = 21.sp))
            if (description != null) Text(description, style = T.Small.copy(lineHeight = 18.sp), modifier = Modifier.padding(top = 4.dp))
        }
        Box(Modifier.padding(start = 12.dp).size(22.dp).clip(RoundedCornerShape(11.dp))
            .border(2.dp, if (selected) C.Navy else C.Line, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(C.Navy))
        }
    }
}

@Composable
fun SettingsCounter(label: String, value: String, description: String? = null, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = T.Label)
        if (description != null) Text(description, style = T.Small)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Bg).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onMinus, modifier = Modifier.semantics { contentDescription = "Riduci $label" }) { AIcon("M6 12h12", C.Navy, 22.dp) }
            Text(value, style = T.Title2.copy(fontSize = 21.sp), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            androidx.compose.material3.IconButton(onClick = onPlus, modifier = Modifier.semantics { contentDescription = "Aumenta $label" }) { AIcon(Ic.Plus, C.Navy, 22.dp) }
        }
    }
}

@Composable
fun SettingsSegment(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(20.dp)).background(C.Surface)
        .border(1.dp, C.Line, RoundedCornerShape(20.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, label ->
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                .background(if (index == selected) C.Navy else C.Surface).selectable(selected = index == selected, role = Role.RadioButton) { onPick(index) }.padding(horizontal = 10.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = T.Label.copy(fontSize = 14.sp, lineHeight = 19.sp, color = if (index == selected) C.Surface else C.Navy), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
