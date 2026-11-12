package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Equal native touch targets and restrained rectangular geometry. */
@Composable
fun SquareAction(label: String, modifier: Modifier = Modifier, tag: String = "", isSelected: Boolean = false,
    subtitle: String? = null, minHeight: Dp = 56.dp, enabled: Boolean = true, primary: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(modifier.heightIn(min = minHeight).testTag(tag).clip(shape)
        .background(if (primary) { if (enabled) C.Royal else C.Royal.copy(alpha = .45f) } else if (isSelected) C.AccentSoft else C.Surface)
        .border(1.dp, if (isSelected) C.AccentDeep else C.Royal.copy(alpha = .24f), shape)
        .semantics { selected = isSelected }.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        Column(if (subtitle != null || isSelected) Modifier.weight(1f) else Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = T.Label.copy(fontSize = 14.sp, lineHeight = 20.sp, color = if (primary) Color.White else C.Navy), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (subtitle != null) Text(subtitle, style = T.Small.copy(fontSize = 11.sp, lineHeight = 15.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        if (isSelected) { GapW(8.dp); AIcon(Ic.Check, C.AccentDeep, 20.dp) }
    }
}
