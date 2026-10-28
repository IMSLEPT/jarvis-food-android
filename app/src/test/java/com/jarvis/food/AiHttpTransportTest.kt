package com.jarvis.food

import com.jarvis.food.ai.AiHttpTransport
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.data.AiConfig
import com.jarvis.food.engine.FoodDb
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/** Real HTTP fixture using only APIs available in Android's unit-test classpath. */
private class PhotoHttpFixture(private val respond: (String) -> String) : Closeable {
    private val listener = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val workers = Executors.newCachedThreadPool()
    private val clients = ConcurrentLinkedQueue<Socket>()
    val url = "http://127.0.0.1:${listener.localPort}"

    init {
        workers.execute {
            while (!listener.isClosed) {
                val client = try { listener.accept() } catch (_: Exception) { break }
                clients.add(client)
                workers.execute {
                    try {
                        client.use { socket ->
                            socket.soTimeout = 5000
                            val input = socket.getInputStream().buffered()
                            val headers = ByteArrayOutputStream()
                            var ending = 0
                            while (ending != 0x0d0a0d0a) {
                                val next = input.read()
                                if (next < 0 || headers.size() >= 16_384) error("Incomplete HTTP request")
                                headers.write(next)
                                ending = (ending shl 8) or next
                            }
                            val length = Regex("(?im)^Content-Length: *([0-9]+)")
                                .find(headers.toString("US-ASCII"))?.groupValues?.get(1)?.toInt() ?: 0
                            require(length in 0..1_000_000)
                            val body = ByteArray(length)
                            var count = 0
                            while (count < length) {
                                val read = input.read(body, count, length - count)
                                if (read < 0) error("Incomplete HTTP body")
                                count += read
                            }
                            val answer = respond(String(body, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
                            socket.getOutputStream().use { output ->
                                output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${answer.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                                output.write(answer)
                                output.flush()
                            }
                        }
                    } catch (_: Exception) {
                        // Cancellation intentionally closes a connection before its response.
                    } finally {
                        clients.remove(client)
                        runCatching { client.close() }
                    }
                }
            }
        }
    }

    override fun close() {
        listener.close()
        clients.forEach { runCatching { it.close() } }
        workers.shutdownNow()
    }
}

class AiHttpTransportTest {
    @Test fun cancelledSocketDoesNotHoldTheCoroutineUntilReadTimeout() = runBlocking {
        val received = CountDownLatch(1); val release = CountDownLatch(1)
        PhotoHttpFixture {
            received.countDown()
            release.await(10, TimeUnit.SECONDS)
            "{}"
        }.use { server ->
            try {
                val request = launch(Dispatchers.Default) { AiHttpTransport.post("${server.url}/slow", "test", "{}") }
                assertTrue(withContext(Dispatchers.IO) { received.await(3, TimeUnit.SECONDS) })
                val elapsed = measureTimeMillis { request.cancelAndJoin() }
                assertTrue("Cancellation waited for the slow socket: $elapsed ms", elapsed < 2000)
            } finally { release.countDown() }
        }
    }

    @Test fun fastFallbackReturnsBeforeSlowPreferredModelCompletes() = runBlocking {
        val release = CountDownLatch(1)
        PhotoHttpFixture { body ->
            val request = JSONObject(body)
            if (request.getString("model") == "slow-test") release.await(12, TimeUnit.SECONDS)
            JSONObject().put("choices", JSONArray().put(JSONObject().put("message", JSONObject().put("content", "{\"items\":[]}")))).toString()
        }.use { server ->
            try {
                val config = AiConfig(baseUrl = "${server.url}/openrouter", model = "slow-test", apiKey = "test")
                val elapsed = measureTimeMillis {
                    assertTrue(FoodAi.pantryFromReceipt(config, FoodDb(emptyList(), emptyList()), "scontrino").isEmpty())
                }
                assertTrue("Winner waited for a losing request: $elapsed ms", elapsed < 9000)
            } finally { release.countDown() }
        }
    }
}
