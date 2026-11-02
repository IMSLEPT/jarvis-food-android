package com.jarvis.food

import com.jarvis.food.chat.ChatHistory
import com.jarvis.food.data.*
import org.junit.Assert.*
import org.junit.Test

class ChatHistoryTest {
    private val first = ChatMsg("m1", "user", "Una ricetta per cena", 1000)
    private val second = ChatMsg("m2", "user", "Pranzo per domani", 2000)
    @Test fun legacyChatSurvivesNewChatAndBackupRoundTrip() {
        val legacy = Store.json.decodeFromString(AppState.serializer(), """{"chat":[{"id":"m1","role":"user","text":"Una ricetta per cena","time":1000}]}""")
        val fresh = ChatHistory.newChat(legacy, "new")
        assertTrue(fresh.chat.isEmpty())
        assertEquals(listOf(first), fresh.chatHistory.single().messages)
        val backup = Store.json.decodeFromString(AppState.serializer(), Store.json.encodeToString(AppState.serializer(), fresh))
        assertEquals(legacy.chat, ChatHistory.open(backup, "m1").chat)
    }
    @Test fun switchingBackAndForthKeepsBothConversationsWithoutDuplicates() {
        val fresh = ChatHistory.newChat(AppState(chat = listOf(first)), "new").copy(chat = listOf(second))
        val reopened = ChatHistory.open(fresh, "m1")
        assertEquals(listOf(first), reopened.chat)
        val again = ChatHistory.open(reopened, "new")
        assertEquals(listOf(second), again.chat)
        assertEquals(listOf("new", "m1"), ChatHistory.conversations(again).map { it.id })
        assertEquals(again, ChatHistory.open(again, "missing"))
    }
    @Test fun archivedActionsDoNotResumeAnOldDeleteConfirmation() {
        val old = first.copy(pending = listOf(PendingCall("delete", "{}", "Elimina")))
        val fresh = ChatHistory.newChat(AppState(chat = listOf(old)), "new")
        assertTrue(ChatHistory.open(fresh, "m1").chat.single().pending.isEmpty())
        assertEquals(1, ChatHistory.newChat(fresh, "another").chatHistory.size)
    }
}
