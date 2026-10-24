package com.jarvis.food

import com.jarvis.food.ai.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class PhotoAnalysisControllerTest {
    private val offline = PhotoAnalysisResult.Offline(listOf(AiLabel("pasta", 0.8f)))

    @Test fun selectedOfflineNeverCallsOnlineAcrossNewPhotos() = runBlocking {
        var onlineCalls = 0; var offlineCalls = 0
        var savedMode: Boolean? = null
        val controller = PhotoAnalysisController<String>(this, true,
            analyzeOnline = { _, _, _ -> onlineCalls++; PhotoAnalysisResult.Online(emptyList(), "") },
            analyzeOffline = { offlineCalls++; offline }, persistMode = { savedMode = it })
        controller.selectMode(false)
        repeat(2) { number ->
            controller.loadPhoto { "photo-$number" }
            withTimeout(2000) { controller.state.first { !it.busy && it.photo == "photo-$number" && it.result != null } }
        }
        assertEquals(false, savedMode)
        assertEquals(0, onlineCalls)
        assertEquals(2, offlineCalls)
    }

    @Test fun switchingToOfflineCancelsOnlineAndRejectsItsLateResponse() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val releaseOld = CompletableDeferred<Unit>()
        val controller = PhotoAnalysisController<String>(this, true,
            analyzeOnline = { _, _, report ->
                started.complete(Unit)
                withContext(NonCancellable) { releaseOld.await() }
                report("late old status")
                PhotoAnalysisResult.Online(listOf(AiItem("old", 100.0, "pasta")), "old")
            }, analyzeOffline = { offline })
        controller.loadPhoto { "photo" }
        started.await()
        controller.selectMode(false)
        withTimeout(2000) { controller.state.first { !it.busy && it.result is PhotoAnalysisResult.Offline } }
        releaseOld.complete(Unit)
        yield()
        assertEquals(offline, controller.state.value.result)
        assertEquals("", controller.state.value.status)
        assertNull(controller.state.value.error)
    }

    @Test fun modeSelectedDuringDecodingIsUsedForThatPhoto() = runBlocking {
        val decoding = CompletableDeferred<Unit>(); val decoded = CompletableDeferred<String>()
        var onlineCalls = 0
        val controller = PhotoAnalysisController<String>(this, true,
            analyzeOnline = { _, _, _ -> onlineCalls++; error("Online must not run") },
            analyzeOffline = { offline })
        controller.loadPhoto { decoding.complete(Unit); decoded.await() }
        decoding.await()
        controller.selectMode(false)
        decoded.complete("photo")
        withTimeout(2000) { controller.state.first { !it.busy && it.result != null } }
        assertEquals(offline, controller.state.value.result)
        assertEquals(0, onlineCalls)
    }

    @Test fun newerPhotoWinsEvenWhenOldWorkIgnoresCancellation() = runBlocking {
        val oldStarted = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val controller = PhotoAnalysisController<String>(this, false,
            analyzeOnline = { _, _, _ -> error("Online must not run") },
            analyzeOffline = { photo ->
                if (photo == "old") { oldStarted.complete(Unit); withContext(NonCancellable) { release.await() } }
                PhotoAnalysisResult.Offline(listOf(AiLabel(photo, 0.9f)))
            })
        controller.loadPhoto { "old" }; oldStarted.await()
        controller.loadPhoto { "new" }
        withTimeout(2000) { controller.state.first { !it.busy && it.photo == "new" && it.result != null } }
        release.complete(Unit); yield()
        assertEquals("new", (controller.state.value.result as PhotoAnalysisResult.Offline).labels.single().label)
    }

    @Test fun errorDoesNotSilentlyChangeModeAndRetryCanRecover() = runBlocking {
        var attempts = 0; var offlineCalls = 0
        val controller = PhotoAnalysisController<String>(this, true,
            analyzeOnline = { _, _, _ -> attempts++; if (attempts == 1) error("Slow provider") else PhotoAnalysisResult.Online(listOf(AiItem("pasta", 80.0, "pasta")), "") },
            analyzeOffline = { offlineCalls++; offline })
        controller.loadPhoto { "photo" }
        withTimeout(2000) { controller.state.first { !it.busy && it.error != null } }
        assertTrue(controller.state.value.online)
        controller.retry()
        withTimeout(2000) { controller.state.first { !it.busy && it.result != null } }
        assertNull(controller.state.value.error)
        assertEquals(0, offlineCalls)
        assertEquals(2, attempts)
    }

    @Test fun stopClearsBusyWithoutReportingCancellationAsError() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val controller = PhotoAnalysisController<String>(this, false,
            analyzeOnline = { _, _, _ -> error("Online must not run") },
            analyzeOffline = { started.complete(Unit); awaitCancellation() })
        controller.loadPhoto { "photo" }; started.await()
        controller.stop(); yield()
        assertFalse(controller.state.value.busy)
        assertNull(controller.state.value.result)
        assertNull(controller.state.value.error)
    }
}
