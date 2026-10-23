package com.jarvis.food

import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** TEMPORANEO: comportamento di textdb.dev da CI (sovrascrittura, dimensioni, velocità, concorrenza). Usa solo chiavi casuali. */
class FamilyProbeTest {
    private class R(val code: Int, val body: String)

    private fun call(method: String, url: String, body: String? = null, headers: Map<String, String> = emptyMap()): R = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 10_000; c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", "JarvisFood"); c.setRequestProperty("Accept", "*/*")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode
        val stream = if (code in 200..399) c.inputStream else c.errorStream
        R(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
    } catch (e: Exception) { R(-1, e.javaClass.simpleName + ": " + e.message) }

    private fun show(tag: String, r: R) = println("[probe] $tag -> ${r.code} body=${r.body.take(120).replace('\n', ' ')}")
    private fun key() = "jfprobe" + UUID.randomUUID().toString().replace("-", "")

    @Test fun probe() {
        val base = "https://textdb.dev/api/data/"
        val T = mapOf("Content-Type" to "text/plain")
        val k = key()
        show("GET inesistente", call("GET", base + k))
        show("POST text/plain", call("POST", base + k, "{\"v\":1,\"d\":\"AAAA\"}", T))
        show("GET", call("GET", base + k))
        show("POST di nuovo (sovrascrive?)", call("POST", base + k, "{\"v\":2,\"d\":\"BBBB\"}", T))
        show("GET dopo secondo POST", call("GET", base + k))
        show("PUT", call("PUT", base + k, "{\"v\":3,\"d\":\"CCCC\"}", T))
        show("GET dopo PUT", call("GET", base + k))
        for (kb in listOf(8, 32, 128, 512)) {
            val big = "{\"v\":1,\"d\":\"" + "A".repeat(kb * 1024) + "\"}"
            val kk = key()
            val w = call("POST", base + kk, big, T)
            val g = call("GET", base + kk)
            println("[probe] $kb KB -> POST ${w.code}, GET ${g.code}, letti ${g.body.length} caratteri (attesi ${big.length})")
        }
        val t0 = System.currentTimeMillis(); val codes = mutableListOf<Int>()
        repeat(40) { codes.add(call("GET", base + k).code) }
        println("[probe] 40 GET in ${System.currentTimeMillis() - t0} ms, codici ${codes.groupBy { it }.mapValues { it.value.size }}")
        val ths = (1..2).map { n -> Thread { repeat(5) { call("POST", base + k, "{\"w\":$n}", T) } } }
        ths.forEach { it.start() }; ths.forEach { it.join() }
        show("dopo scritture simultanee", call("GET", base + k))
    }
}
