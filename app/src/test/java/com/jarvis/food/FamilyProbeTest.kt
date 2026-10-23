package com.jarvis.food

import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/** TEMPORANEO: prova quali servizi di archiviazione anonima rispondono da CI (crea, legge, aggiorna). */
class FamilyProbeTest {
    private class R(val code: Int, val body: String, val loc: String?)

    private fun call(method: String, url: String, body: String? = null, headers: Map<String, String> = emptyMap(), ua: String = "JarvisFood"): R = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 10_000; c.readTimeout = 15_000; c.instanceFollowRedirects = false
        c.setRequestProperty("User-Agent", ua); c.setRequestProperty("Accept", "*/*")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode
        val stream = if (code in 200..399) c.inputStream else c.errorStream
        R(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty(), c.getHeaderField("Location"))
    } catch (e: Exception) { R(-1, e.javaClass.simpleName + ": " + e.message, null) }

    private fun show(tag: String, r: R) = println("[probe] $tag -> ${r.code} loc=${r.loc} body=${r.body.take(160).replace('\n', ' ')}")
    private val J = mapOf("Content-Type" to "application/json")
    private val browser = "Mozilla/5.0 (Linux; Android 14; Pixel 9) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36"

    @Test fun probe() {
        val payload = "{\"v\":1,\"d\":\"AAAA\"}"
        // jsonblob con varie intestazioni
        show("jsonblob UA=JarvisFood", call("POST", "https://jsonblob.com/api/jsonBlob", payload, J))
        show("jsonblob UA=browser", call("POST", "https://jsonblob.com/api/jsonBlob", payload, J, browser))
        show("jsonblob UA=browser+origin", call("POST", "https://jsonblob.com/api/jsonBlob", payload, J + mapOf("Origin" to "https://jsonblob.com", "Referer" to "https://jsonblob.com/"), browser))
        // restful-api.dev
        val r1 = call("POST", "https://api.restful-api.dev/objects", "{\"name\":\"jf\",\"data\":$payload}", J)
        show("restful-api.dev POST", r1)
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(r1.body)?.groupValues?.get(1)?.let { id ->
            show("restful-api.dev GET", call("GET", "https://api.restful-api.dev/objects/$id"))
            show("restful-api.dev PUT", call("PUT", "https://api.restful-api.dev/objects/$id", "{\"name\":\"jf\",\"data\":{\"v\":2}}", J))
        }
        // textdb.dev
        val key = "jf" + System.nanoTime()
        show("textdb POST", call("POST", "https://textdb.dev/api/data/$key", payload))
        show("textdb GET", call("GET", "https://textdb.dev/api/data/$key"))
        show("textdb PATCH", call("PATCH", "https://textdb.dev/api/data/$key", "{\"v\":2}"))
        show("textdb GET2", call("GET", "https://textdb.dev/api/data/$key"))
        // extendsclass json storage
        val r2 = call("POST", "https://json.extendsclass.com/bin", payload, J)
        show("extendsclass POST", r2)
        // keyvalue.xyz
        val r3 = call("POST", "https://api.keyvalue.xyz/new/jf")
        show("keyvalue new", r3)
        // kvdb.io
        show("kvdb new", call("POST", "https://kvdb.io", "email=probe@example.com", mapOf("Content-Type" to "application/x-www-form-urlencoded")))
        // myjson.online
        show("myjson.online POST", call("POST", "https://api.myjson.online/v1/records", "{\"data\":$payload}", J))
        // npoint
        show("npoint POST", call("POST", "https://api.npoint.io/", payload, J))
        // ntfy.sh
        val topic = "jf-probe-" + System.nanoTime()
        show("ntfy publish", call("POST", "https://ntfy.sh/$topic", payload))
        show("ntfy poll", call("GET", "https://ntfy.sh/$topic/json?poll=1&since=all"))
        // pantry-like / jsonstorage
        show("jsonstorage POST", call("POST", "https://api.jsonstorage.net/v1/json", payload, J))
        show("jsonkeeper POST", call("POST", "https://www.jsonkeeper.com/api/b", payload, J))
        // domini raggiungibili in generale
        show("GET https://example.com", call("GET", "https://example.com"))
    }
}
