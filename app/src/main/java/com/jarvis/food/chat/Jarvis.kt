package com.jarvis.food.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.AppState
import com.jarvis.food.data.ChatMsg
import com.jarvis.food.data.PendingCall
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

    /** L'utente conferma le eliminazioni proposte nel messaggio: solo ora vengono eseguite. */
    fun confirm(msgId: String) {
        val msg = Store.current.chat.firstOrNull { it.id == msgId } ?: return
        if (msg.pending.isEmpty() || busy) return
        val before = Store.current
        val chips = msg.pending.map { p ->
            val r = Actions.execute(ActionCall(p.name, runCatching { org.json.JSONObject(p.args) }.getOrDefault(org.json.JSONObject())), confirmed = true)
            (if (r.ok) "✓ " else "⚠ ") + r.text
        }
        Store.update { cur -> cur.copy(chat = cur.chat.map { if (it.id == msgId) it.copy(pending = emptyList(), actions = it.actions + chips) else it }) }
        if (chips.any { it.startsWith("✓") }) { undoFor = msgId; undoState = before }
        Store.saveNow()
    }

    fun cancelPending(msgId: String) {
        Store.update { cur -> cur.copy(chat = cur.chat.map { if (it.id == msgId) it.copy(pending = emptyList(), actions = it.actions + "↩ Eliminazione annullata: non ho toccato nulla") else it }) }
        Store.saveNow()
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

    private fun addMsg(role: String, text: String, actions: List<String> = emptyList(), quick: List<String> = emptyList(), pending: List<PendingCall> = emptyList()): ChatMsg {
        val m = ChatMsg(Store.newId(), role, text, System.currentTimeMillis(), actions, quick, pending)
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
            val pend = mutableListOf<PendingCall>()
            var finalText = ""
            var quick = emptyList<String>()
            var snapshot: AppState? = null
            try {
                var feedback: String? = null
                for (attempt in 0..1) {
                    val s = Store.current
                    val cfg = s.ai
                    val system = Actions.systemPrompt(s, s.chat.filter { it.role == "user" }.takeLast(2).joinToString(" ") { it.text } + " " + (feedback ?: ""))
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
                    val calls = parsed.actions.take(8)
                    val results = withContext(Dispatchers.Main) { calls.map { Actions.execute(it) } }
                    calls.zip(results).forEach { (c, r) -> if (r.confirm != null) pend += PendingCall(c.name, c.args.toString(), r.confirm) }
                    val real = results.filter { it.confirm == null }
                    chips += real.filter { it.ok }.map { "✓ ${it.text}" }
                    chips += real.filter { !it.ok && !it.retry }.map { "⚠ ${it.text}" }
                    finalText = parsed.text
                    // Non far credere di aver fatto ciò che l'app non ha eseguito.
                    if (calls.isEmpty() && Actions.claimsDone(finalText)) {
                        finalText = "Non ho eseguito nessuna modifica nell'app. Riscrivimi la richiesta con i dettagli (alimento, quantità, pasto) e la faccio."
                    }
                    // Se ha già eseguito qualcosa, una richiesta di conferma ("Sì, applica" / "No") non ha più senso.
                    quick = if (results.any { it.ok }) parsed.quick.filterNot { q -> q.lowercase().let { it.startsWith("sì") || it.startsWith("si,") || it == "si" || it.startsWith("no") } } else parsed.quick
                    val failed = real.filter { !it.ok && it.retry }
                    if (failed.isNotEmpty() && attempt == 0) {
                        feedback = (real.filter { it.ok }.map { "riuscita: ${it.text}" } + failed.map { "NON riuscita: ${it.text}" }).joinToString("; ")
                        status = "Controllo di nuovo…"
                        continue
                    }
                    if (failed.isNotEmpty()) chips += failed.map { "⚠ ${it.text}" }
                    break
                }
                val pendUnique = pend.distinctBy { it.name + it.args }
                val anyOk = chips.any { it.startsWith("✓") }; val warns = chips.filter { it.startsWith("⚠") }
                if (pendUnique.isNotEmpty()) finalText = "Prima di eliminare ho bisogno della tua conferma. Non ho ancora toccato nulla."
                else if (!anyOk && warns.isNotEmpty()) finalText = "Non sono riuscito a completare l'azione: " + warns.joinToString(" ") { it.removePrefix("⚠").trim() }
                else if (anyOk && warns.isNotEmpty()) finalText += "\n\nAttenzione: una parte non è andata a buon fine (vedi ⚠)."
                if (finalText.isBlank()) finalText = if (chips.any { it.startsWith("✓") }) "Fatto." else "Non ho capito bene: puoi riformulare?"
                val msg = addMsg("assistant", finalText, chips, quick, pendUnique)
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
