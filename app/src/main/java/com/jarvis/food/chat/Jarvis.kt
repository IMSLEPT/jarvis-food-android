package com.jarvis.food.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.AppState
import com.jarvis.food.data.ChatMsg
import com.jarvis.food.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Il chatbot Jarvis: costruisce il contesto, interroga il modello (sul telefono o nel cloud), esegue le azioni richieste
 * nell'app e salva la conversazione. Vive fuori dalle schermate: la risposta continua anche se esci dalla chat.
 */
object Jarvis {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    var busy by mutableStateOf(false)
        private set
    /** Testo della risposta mentre il modello la scrive (streaming sul telefono). */
    var partial by mutableStateOf("")
        private set
    var status by mutableStateOf("")
        private set

    /** Stato dell'app prima delle azioni dell'ultimo messaggio, per poterle annullare. */
    private var undoFor: String? = null
    private var undoState: AppState? = null

    fun canUndo(msgId: String) = undoFor == msgId && undoState != null

    fun undo(msgId: String) {
        val snap = undoState ?: return
        if (undoFor != msgId) return
        Store.update { cur ->
            snap.copy(chat = cur.chat.map { if (it.id == msgId) it.copy(actions = it.actions + "↩ Annullato") else it })
        }
        Store.saveNow()
        undoFor = null; undoState = null
    }

    fun ready(ctx: Context, cfg: AiConfig): Boolean =
        if (cfg.chatEngine == 0) ModelManager.isReady(ctx, LocalModel.of(cfg.localModel)) else cfg.apiKey.isNotBlank()

    fun clear() {
        job?.cancel(); busy = false; partial = ""; status = ""
        undoFor = null; undoState = null
        Store.update { it.copy(chat = emptyList()) }
        Store.saveNow()
    }

    fun stop() { job?.cancel(); busy = false; partial = ""; status = "" }

    private fun addMsg(role: String, text: String, actions: List<String> = emptyList(), quick: List<String> = emptyList()): ChatMsg {
        val m = ChatMsg(Store.newId(), role, text, System.currentTimeMillis(), actions, quick)
        Store.update { it.copy(chat = (it.chat + m).takeLast(80)) }
        return m
    }

    private fun clip(t: String, n: Int) = if (t.length <= n) t else t.take(n) + "…"

    /** Cronologia da passare al modello: ultimi scambi, alternati utente/assistente, con l'esito delle azioni. */
    private fun history(chat: List<ChatMsg>, feedback: String?): List<Turn> {
        val turns = mutableListOf<Turn>()
        chat.takeLast(10).forEach { m ->
            val role = if (m.role == "user") "user" else "assistant"
            var text = clip(m.text, 700)
            if (role == "assistant" && m.actions.isNotEmpty()) text += "\n[Azioni eseguite: ${m.actions.joinToString("; ")}]"
            if (turns.isNotEmpty() && turns.last().role == role) turns[turns.lastIndex] = Turn(role, turns.last().text + "\n" + text)
            else turns += Turn(role, text)
        }
        while (turns.isNotEmpty() && turns.first().role != "user") turns.removeAt(0)
        if (feedback != null && turns.isNotEmpty() && turns.last().role == "user")
            turns[turns.lastIndex] = Turn("user", turns.last().text + "\n\n[Esito delle azioni appena tentate: $feedback. Correggi o chiedi all'utente quello che manca.]")
        return turns
    }

    fun send(ctx: Context, text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        val app = ctx.applicationContext
        addMsg("user", t)
        busy = true; partial = ""; status = "Jarvis sta pensando…"
        job = scope.launch {
            val chips = mutableListOf<String>()
            var finalText = ""
            var quick = emptyList<String>()
            var snapshot: AppState? = null
            try {
                var feedback: String? = null
                for (attempt in 0..1) {
                    val s = Store.current
                    val cfg = s.ai
                    val system = Actions.systemPrompt(s)
                    val hist = history(s.chat, feedback)
                    partial = ""
                    val raw = if (cfg.chatEngine == 0) {
                        status = if (LocalLlm.loaded) "Jarvis sta scrivendo…" else "Carico il modello (la prima volta ci vuole un po')…"
                        LocalLlm.reply(app, cfg, system, hist) { acc -> status = "Jarvis sta scrivendo…"; partial = Actions.visible(acc) }
                    } else {
                        status = "Jarvis sta scrivendo…"
                        CloudLlm.reply(cfg, system, hist)
                    }
                    val parsed = Actions.parse(raw)
                    if (snapshot == null && parsed.actions.isNotEmpty()) snapshot = Store.current
                    val results = withContext(Dispatchers.Main) { parsed.actions.map { Actions.execute(it) } }
                    chips += results.filter { it.ok }.map { "✓ ${it.text}" }
                    chips += results.filter { !it.ok && !it.retry }.map { "⚠ ${it.text}" }
                    finalText = parsed.text
                    quick = parsed.quick
                    val failed = results.filter { !it.ok && it.retry }
                    if (failed.isNotEmpty() && attempt == 0) {
                        feedback = (results.filter { it.ok }.map { "riuscita: ${it.text}" } + failed.map { "NON riuscita: ${it.text}" }).joinToString("; ")
                        status = "Controllo di nuovo…"
                        continue
                    }
                    if (failed.isNotEmpty()) chips += failed.map { "⚠ ${it.text}" }
                    break
                }
                if (finalText.isBlank()) finalText = if (chips.any { it.startsWith("✓") }) "Fatto." else "Non ho capito bene: puoi riformulare?"
                val msg = addMsg("assistant", finalText, chips, quick)
                if (snapshot != null && chips.any { it.startsWith("✓") }) { undoFor = msg.id; undoState = snapshot }
                Store.saveNow()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                addMsg("assistant", "Non riesco a rispondere: ${e.message ?: e.javaClass.simpleName}", chips)
                Store.saveNow()
            } finally {
                busy = false; partial = ""; status = ""
            }
        }
    }
}
