package com.jarvis.food.chat

import com.jarvis.food.data.AppState
import com.jarvis.food.data.ChatConversation

/** Pure state transitions: old backups with one conversation migrate on their first switch. */
object ChatHistory {
    fun conversations(state: AppState): List<ChatConversation> {
        if (state.chat.isEmpty()) return state.chatHistory.sortedByDescending { it.updatedAt }
        val id = state.activeChatId.ifBlank { state.chat.first().id }
        val current = ChatConversation(id, state.chat.map { it.copy(pending = emptyList()) })
        return (state.chatHistory.filterNot { it.id == id } + current).sortedByDescending { it.updatedAt }
    }
    fun newChat(state: AppState, id: String): AppState = state.copy(
        chatHistory = conversations(state), chat = emptyList(), activeChatId = id)

    fun open(state: AppState, id: String): AppState {
        val saved = conversations(state)
        val target = saved.firstOrNull { it.id == id } ?: return state
        return state.copy(chatHistory = saved, chat = target.messages, activeChatId = id)
    }
}
