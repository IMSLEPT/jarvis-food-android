package com.jarvis.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Banner navy delle schede Cucina, Progressi e Jarvis (immagini 9-12): logo, titolo, cerchio profilo,
 * linguette con pillola arancio e ricerca bianca. Le schede di comando (Jarvis) usano lo stesso aspetto.
 */
@Composable
fun NavyBanner(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    query: String, onQuery: (String) -> Unit, hint: String, icons: List<String> = emptyList(),
    descriptions: List<String> = labels, accounts: Boolean = true, onSubmit: () -> Unit = {}, onBack: (() -> Unit)? = null,
    labelTags: List<String> = emptyList(), searchContent: (@Composable () -> Unit)? = null,
    actions: Boolean = false, supportingText: String? = null, count: String? = null,
    tileIcon: String? = null, subtitle: String? = null, mascot: (@Composable () -> Unit)? = null) {
    val large = RD.large()
    RefBannerFrame(true) {
        val heading: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tileIcon != null) { GlowIcon(tileIcon, Tone.Orange.first, Tone.Orange.second, 32.dp, 11.dp, glyph = 18.dp, duo = false); GapW(10.dp) }
                Text(title, style = T.Title.copy(fontSize = 28.sp, lineHeight = 32.sp, color = Color.White), maxLines = 1)
            }
        }
        if (mascot != null && !large) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) { BannerBackTile(onBack); GapW(10.dp) }
                        if (accounts) HeaderProfileTile(42.dp)
                        if (tileIcon != null) { if (accounts || onBack != null) GapW(10.dp); GlowIcon(tileIcon, Tone.Orange.first, Tone.Orange.second, 42.dp, 14.dp, glyph = 22.dp, duo = false) }
                    }
                    GapH(8.dp)
                    Text(title, style = T.Title.copy(fontSize = 28.sp, lineHeight = 32.sp, color = Color.White), maxLines = 1)
                    if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp, bottom = 4.dp), style = T.Body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFB9C8EC)), maxLines = 3)
                }
                mascot()
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) { BannerBackTile(onBack); GapW(10.dp) }
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    heading()
                    if (subtitle != null) Text(subtitle, style = T.Body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFB9C8EC)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (accounts) HeaderProfileTile(42.dp)
            }
        }
        GapH(10.dp)
        RefBannerTabs(labels, icons, selected, onSelect, actions, title, descriptions, labelTags)
        if (mascot != null && !large) GapH(10.dp) // stessa altezza del banner di Oggi
        if (searchContent != null || supportingText != null || hint.isNotEmpty() || query.isNotEmpty()) {
            GapH(10.dp)
            Box(Modifier.fillMaxWidth().heightIn(min = if (large) 52.dp else 44.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    searchContent != null -> searchContent()
                    supportingText != null && query.isEmpty() && hint.isEmpty() -> Text(supportingText, style = T.Body.copy(lineHeight = 18.sp, color = Color(0xFFE5EBFA)), modifier = Modifier.padding(horizontal = 8.dp))
                    else -> RefSearchShell(count = count) {
                        BasicTextField(query, onQuery, Modifier.weight(1f).testTag("banner-search-$title").semantics { contentDescription = hint },
                            textStyle = T.Body.copy(fontSize = 16.sp, color = RD.Ink), cursorBrush = SolidColor(RD.Ink), singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (query.isEmpty()) Text(hint, style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp, color = Color(0xFF59627A)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    inner()
                                }
                            })
                    }
                }
            }
        }
    }
}

/** Tasto indietro in vetro, stessa misura e raggio delle tessere profilo/calendario. */
@Composable
private fun BannerBackTile(onBack: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Box(Modifier.size(42.dp).clip(shape).background(Color(0x24FFFFFF)).semantics { contentDescription = "Indietro" }
        .clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { AIcon(RIc.Left, Color.White, 22.dp, 2.5f) }
}
