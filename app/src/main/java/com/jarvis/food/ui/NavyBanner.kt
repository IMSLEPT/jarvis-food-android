package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Matching navy panels; controls wrap at large accessibility font sizes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavyBanner(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    query: String, onQuery: (String) -> Unit, hint: String, icons: List<String> = emptyList(),
    descriptions: List<String> = labels, accounts: Boolean = true, onSubmit: () -> Unit = {}, onBack: (() -> Unit)? = null) {
    val large = LocalDensity.current.fontScale > 1.5f
    Column(Modifier.fillMaxWidth().testTag("navy-banner").background(C.Bg).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
        .background(C.NavyBrush).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(Modifier.size(48.dp).semantics { contentDescription = "Indietro" }.clickable(onClick = onBack), contentAlignment = Alignment.Center) { AIcon(Ic.Left, Color.White, 24.dp) }
            }
            Text(title, style = T.Title.copy(fontSize = 30.sp, lineHeight = 36.sp, color = Color.White), modifier = Modifier.weight(1f))
            if (accounts) HeaderAccountActions()
        }
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (large) 2 else labels.size,
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { i, label ->
                Column(Modifier.weight(1f).heightIn(min = if (icons.isEmpty()) 48.dp else 66.dp)
                    .semantics { contentDescription = descriptions[i] }.testTag("banner-tab-$title-$i")
                    .selectable(selected == i, role = Role.Tab, onClick = { onSelect(i) }).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    icons.getOrNull(i)?.let { AIcon(it, Color.White, 24.dp); GapH(5.dp) }
                    Text(label, style = T.Body.copy(fontSize = 14.sp, lineHeight = 19.sp, color = Color.White,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal), textAlign = TextAlign.Center)
                    GapH(7.dp)
                    Box(Modifier.fillMaxWidth(.85f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == selected) C.Accent else Color.Transparent))
                }
            }
        }
        GapH(8.dp)
        BasicTextField(query, onQuery, Modifier.fillMaxWidth().testTag("banner-search-$title").heightIn(min = 44.dp)
            .clip(RoundedCornerShape(25.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = T.Body.copy(fontSize = 14.sp, color = C.Navy), cursorBrush = SolidColor(C.Navy), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { inner -> Row(verticalAlignment = Alignment.CenterVertically) {
                AIcon(Ic.Search, C.Muted, 20.dp); GapW(8.dp)
                Box(Modifier.weight(1f)) { if (query.isEmpty()) Text(hint, style = T.Small.copy(fontSize = 14.sp)); inner() }
            } })
    }
}
