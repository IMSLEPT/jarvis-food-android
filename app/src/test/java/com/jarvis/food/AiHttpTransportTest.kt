package com.jarvis.food

import com.jarvis.food.ai.AiHttpTransport
import com.jarvis.food.ai.FoodAi
import com.jarvis.food.data.AiConfig
import com.jarvis.food.engine.FoodDb
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class AiHttpTransportTest {
    @Test fun cancelledSocketDoesNotHoldTheCoroutineUntilReadTimeout() = runBlocking {
        val received = CountDownLatch(1); val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/slow") { exchange ->
            exchange.requestBody.use { it.readBytes() }; received.countDown()
            release.await(10, TimeUnit.SECONDS); exchange.close()
        }
        server.start()
        try {
            val request = launch(Dispatchers.Default) { AiHttpTransport.post("http://127.0.0.1:${server.address.port}/slow", "test", "{}") }
            assertTrue(withContext(Dispatchers.IO) { received.await(3, TimeUnit.SECONDS) })
            val elapsed = measureTimeMillis { request.cancelAndJoin() }
            assertTrue("Cancellation waited for the slow socket: $elapsed ms", elapsed < 2000)
        } finally { release.countDown(); server.stop(0) }
    }

    @Test fun fastFallbackReturnsBeforeSlowPreferredModelCompletes() = runBlocking {
        val release = CountDownLatch(1)
        val workers = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = workers
        server.createContext("/openrouter/chat/completions") { exchange ->
            try {
                val request = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
                if (request.getString("model") == "slow-test") release.await(12, TimeUnit.SECONDS)
                val answer = JSONObject().put("choices", JSONArray().put(JSONObject().put("message", JSONObject().put("content", "{\"items\":[]}")))).toString().toByteArray()
                exchange.sendResponseHeaders(200, answer.size.toLong()); exchange.responseBody.use { it.write(answer) }
            } catch (_: Exception) { exchange.close() }
        }
        server.start()
        try {
            val config = AiConfig(baseUrl = "http://127.0.0.1:${server.address.port}/openrouter", model = "slow-test", apiKey = "test")
            val elapsed = measureTimeMillis {
                assertTrue(FoodAi.pantryFromReceipt(config, FoodDb(emptyList(), emptyList()), "scontrino").isEmpty())
            }
            assertTrue("Winner waited for a losing request: $elapsed ms", elapsed < 9000)
        } finally { release.countDown(); server.stop(0); workers.shutdownNow() }
    }
}
