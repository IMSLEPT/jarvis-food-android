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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Flat reference colour and shared geometry, with accessible wrapped tabs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavyBanner(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    query: String, onQuery: (String) -> Unit, hint: String, icons: List<String> = emptyList(),
    descriptions: List<String> = labels, accounts: Boolean = true, onSubmit: () -> Unit = {}, onBack: (() -> Unit)? = null,
    labelTags: List<String> = emptyList(), searchContent: (@Composable () -> Unit)? = null) {
    val large = LocalDensity.current.fontScale > 1.5f
    val medium = LocalDensity.current.fontScale > 1.2f
    ScreenHeaderFrame(true) {
        Row(Modifier.fillMaxWidth().height(HeaderLayout.titleHeight()), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) Box(Modifier.size(48.dp).semantics { contentDescription = "Indietro" }.clickable(onClick = onBack), contentAlignment = Alignment.Center) { AIcon(Ic.Left, Color.White, 24.dp) }
            Text(title, style = T.Title.copy(fontSize = if (large) 23.sp else 30.sp, lineHeight = if (large) 28.sp else 34.sp, color = Color.White), modifier = Modifier.weight(1f))
            if (accounts) HeaderAccountActions()
        }
        FlowRow(Modifier.fillMaxWidth().height(HeaderLayout.tabsHeight()), maxItemsInEachRow = if (large) 2 else labels.size,
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            labels.forEachIndexed { i, label ->
                Column(Modifier.weight(1f).height(if (large) 72.dp else HeaderLayout.tabsHeight())
                    .semantics { contentDescription = descriptions[i] }.testTag("banner-tab-$title-$i")
                    .selectable(selected == i, role = Role.Tab, onClick = { onSelect(i) }), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) { icons.getOrNull(i)?.let { AIcon(it, Color.White, 22.dp) } }
                    Text(label, modifier = Modifier.testTag(labelTags.getOrElse(i) { "banner-label-$title-$i" }),
                        style = T.Body.copy(fontSize = if (large) 12.sp else if (medium) 13.sp else 14.sp, lineHeight = if (large) 16.sp else 19.sp, color = Color.White,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal), textAlign = TextAlign.Center)
                    GapH(4.dp)
                    Box(Modifier.fillMaxWidth(.85f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == selected) C.Accent else Color.Transparent))
                }
            }
            if (large && labels.size % 2 != 0) Spacer(Modifier.weight(1f).height(72.dp))
        }
        GapH(16.dp)
        Box(Modifier.fillMaxWidth().height(HeaderLayout.searchHeight()), contentAlignment = Alignment.Center) {
            if (searchContent != null) searchContent() else BasicTextField(query, onQuery, Modifier.fillMaxWidth().fillMaxHeight().testTag("banner-search-$title")
                .clip(RoundedCornerShape(25.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = T.Body.copy(fontSize = 14.sp, color = C.Navy), cursorBrush = SolidColor(C.Navy), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner -> Row(verticalAlignment = Alignment.CenterVertically) {
                    AIcon(Ic.Search, C.Muted, 20.dp); GapW(8.dp)
                    Box(Modifier.weight(1f)) { if (query.isEmpty()) Text(hint, style = T.Small.copy(fontSize = 14.sp), maxLines = if (large) 2 else 1, overflow = TextOverflow.Ellipsis); inner() }
                } })
        }
    }
}
