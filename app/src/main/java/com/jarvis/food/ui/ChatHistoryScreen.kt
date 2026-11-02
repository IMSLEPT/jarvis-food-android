package com.jarvis.food.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jarvis.food.chat.ChatHistory
import com.jarvis.food.chat.Jarvis
import com.jarvis.food.data.Store
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatHistoryScreen() {
    val state by Store.state.collectAsState()
    val conversations = remember(state.chat, state.chatHistory, state.activeChatId) { ChatHistory.conversations(state) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", IT) }
    Column(Modifier.fillMaxSize().navigationBarsPadding().testTag("chat-history-screen")) {
        TopBar("Cronologia chat", onBack = { Nav.back() })
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Tocca una conversazione per riprenderla.", style = T.Small) }
            if (conversations.isEmpty()) item { EmptyState("💬", "Nessuna conversazione salvata", "Le chat restano qui anche quando ne inizi una nuova.") }
            items(conversations, key = { it.id }) { chat ->
                SquareAction(chat.title, Modifier.fillMaxWidth(), tag = "chat-history-${chat.id}",
                    subtitle = Instant.ofEpochMilli(chat.updatedAt).atZone(ZoneId.systemDefault()).format(dateFormat)) {
                    Jarvis.openConversation(chat.id)
                    Nav.back()
                }
            }
        }
    }
}
