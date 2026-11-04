package com.jarvis.food.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    actions: Boolean = false, supportingText: String? = null, logo: (@Composable () -> Unit)? = null, count: String? = null) {
    val large = RD.large()
    RefBannerFrame(true) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (large) 64.dp else 56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) Box(Modifier.size(48.dp).semantics { contentDescription = "Indietro" }.clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center) { AIcon(RIc.Left, Color.White, 26.dp, 2.6f) }
            if (logo != null) { Box(Modifier.padding(start = 6.dp).size(RD.BannerLogo + 4.dp), contentAlignment = Alignment.Center) { logo() }; GapW(12.dp) } else GapW(8.dp)
            Text(title, style = T.Title.copy(fontSize = RD.BannerTitle, lineHeight = 36.sp, color = Color.White), modifier = Modifier.weight(1f).padding(end = 8.dp))
            if (accounts) RefProfileButton(RD.headerButton())
        }
        GapH(4.dp)
        RefBannerTabs(labels, icons, selected, onSelect, actions, title, descriptions, labelTags)
        GapH(8.dp)
        Box(Modifier.fillMaxWidth().heightIn(min = if (large) 56.dp else 48.dp), contentAlignment = Alignment.CenterStart) {
            when {
                searchContent != null -> searchContent()
                supportingText != null && query.isEmpty() && hint.isEmpty() -> Text(supportingText, style = T.Body.copy(lineHeight = 18.sp, color = Color(0xFFE5EBFA)), modifier = Modifier.padding(horizontal = 8.dp))
                else -> RefSearchShell(count = count) {
                    BasicTextField(query, onQuery, Modifier.weight(1f).testTag("banner-search-$title").semantics { contentDescription = hint },
                        textStyle = T.Body.copy(fontSize = 17.sp, color = RD.Ink), cursorBrush = SolidColor(RD.Ink), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                if (query.isEmpty()) Text(hint, style = T.Body.copy(fontSize = 17.sp, lineHeight = 22.sp, color = Color(0xFF59627A)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                inner()
                            }
                        })
                }
            }
        }
        GapH(4.dp)
    }
}
