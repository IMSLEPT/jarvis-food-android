package com.jarvis.food.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhotoAnalysisResult {
    data class Online(val items: List<AiItem>, val note: String) : PhotoAnalysisResult
    data class Offline(val labels: List<AiLabel>) : PhotoAnalysisResult
}

data class PhotoAnalysisState<T>(
    val online: Boolean,
    val photo: T? = null,
    val busy: Boolean = false,
    val analyzed: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val result: PhotoAnalysisResult? = null
)

/** One explicit mode and one current request; an old photo can never overwrite a new one. */
class PhotoAnalysisController<T>(
    private val scope: CoroutineScope,
    initialOnline: Boolean,
    private val analyzeOnline: suspend (T, String, (String) -> Unit) -> PhotoAnalysisResult.Online,
    private val analyzeOffline: suspend (T) -> PhotoAnalysisResult.Offline,
    private val persistMode: (Boolean) -> Unit = {},
    private val describeError: (Throwable) -> String = { it.message ?: "Analisi non riuscita. Riprova." }
) {
    private val mutableState = MutableStateFlow(PhotoAnalysisState<T>(initialOnline))
    val state = mutableState.asStateFlow()
    private var task: Job? = null
    private var generation = 0L

    fun selectMode(online: Boolean, hint: String = "", retry: Boolean = false) {
        val changed = state.value.online != online
        mutableState.value = state.value.copy(online = online)
        persistMode(online)
        // While an image is loading, use this selection as soon as decoding finishes.
        if (state.value.photo != null && (changed || retry)) start(hint)
    }

    fun loadPhoto(hint: String = "", load: suspend () -> T?) {
        launchRequest { id ->
            mutableState.value = state.value.copy(photo = null, busy = true, analyzed = false,
                status = "Preparo la foto…", error = null, result = null)
            val photo = load() ?: throw IllegalArgumentException("Impossibile leggere l'immagine. Scegli un'altra foto.")
            currentCoroutineContext().ensureActive()
            if (id != generation) return@launchRequest
            mutableState.value = state.value.copy(photo = photo)
            analyze(id, photo, hint)
        }
    }

    fun retry(hint: String = "") = start(hint)

    fun stop() {
        generation++
        task?.cancel()
        task = null
        mutableState.value = state.value.copy(busy = false, status = "", error = null)
    }

    private fun start(hint: String) {
        val photo = state.value.photo ?: return
        launchRequest { id -> analyze(id, photo, hint) }
    }

    private fun launchRequest(block: suspend (Long) -> Unit) {
        generation++
        val id = generation
        task?.cancel()
        task = scope.launch {
            try {
                block(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (id == generation) mutableState.value = state.value.copy(error = describeError(failure))
            } finally {
                if (id == generation) mutableState.value = state.value.copy(busy = false)
            }
        }
    }

    private suspend fun analyze(id: Long, photo: T, hint: String) {
        val online = state.value.online
        mutableState.value = state.value.copy(busy = true, analyzed = true, result = null, error = null,
            status = if (online) "Preparo la foto…" else "Analisi sul telefono…")
        val result = if (online) analyzeOnline(photo, hint) { message ->
            if (id == generation) mutableState.value = state.value.copy(status = message)
        } else analyzeOffline(photo)
        currentCoroutineContext().ensureActive()
        if (id != generation) return
        val emptyError = when (result) {
            is PhotoAnalysisResult.Online -> if (result.items.isEmpty()) result.note.ifBlank { "Nessun alimento riconosciuto. Prova un'altra foto." } else null
            is PhotoAnalysisResult.Offline -> if (result.labels.isEmpty()) "Nessun piatto riconosciuto. Prova una foto più nitida." else null
        }
        mutableState.value = state.value.copy(result = result, error = emptyError, status = "")
    }
}
