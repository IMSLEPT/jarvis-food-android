package com.jarvis.food.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Blocking HTTP lives outside child coroutines so cancellation never waits for a socket timeout. */
internal object AiHttpTransport {
    private val workers = Executors.newFixedThreadPool(4) { work ->
        Thread(work, "food-ai-http").apply { isDaemon = true }
    }
    private val cleanup = Executors.newCachedThreadPool { work ->
        Thread(work, "food-ai-disconnect").apply { isDaemon = true }
    }

    suspend fun post(url: String, apiKey: String, body: String, readTimeoutMs: Int = 25_000): Pair<Int, String> =
        suspendCancellableCoroutine { continuation ->
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 8_000
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            connection.setRequestProperty("X-Title", "Jarvis Food")
            val pending = workers.submit {
                try {
                    if (!continuation.isActive) return@submit
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (continuation.isActive) continuation.resume(code to response)
                } catch (failure: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(failure)
                } finally {
                    connection.disconnect()
                }
            }
            continuation.invokeOnCancellation {
                // Disconnect immediately when the winner is found or the user changes mode.
                pending.cancel(true)
                cleanup.execute { runCatching { connection.disconnect() } }
            }
        }
}
