package com.jarvis.food.family

import java.net.HttpURLConnection
import java.net.URL

/** Dove sta il documento cifrato della famiglia. Un'interfaccia sola, così il servizio si può cambiare senza toccare il resto. */
interface FamilyBackend {
    /** Crea un nuovo documento e ne restituisce l'identificativo. */
    fun create(payload: String): String
    /** Null se il documento non esiste più. Eccezione se manca la rete. */
    fun get(id: String): String?
    fun put(id: String, payload: String)
}

/** JSON Blob (jsonblob.com): archivio JSON pubblico e gratuito, senza account. I contenuti sono comunque cifrati. */
class JsonBlobBackend(private val base: String = "https://jsonblob.com/api/jsonBlob") : FamilyBackend {

    private fun open(url: String, method: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 10_000; readTimeout = 15_000
        setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json")
        setRequestProperty("User-Agent", "JarvisFood")
    }

    private fun send(c: HttpURLConnection, body: String) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }

    override fun create(payload: String): String {
        val c = open(base, "POST"); send(c, payload)
        val code = c.responseCode
        if (code !in 200..201) throw java.io.IOException("Creazione non riuscita (HTTP $code)")
        val loc = c.getHeaderField("Location") ?: c.getHeaderField("X-jsonblob") ?: throw java.io.IOException("Risposta senza identificativo")
        return loc.trimEnd('/').substringAfterLast('/')
    }

    override fun get(id: String): String? {
        val c = open("$base/$id", "GET")
        return when (val code = c.responseCode) {
            200 -> c.inputStream.bufferedReader().use { it.readText() }
            404 -> null
            else -> throw java.io.IOException("Lettura non riuscita (HTTP $code)")
        }
    }

    override fun put(id: String, payload: String) {
        val c = open("$base/$id", "PUT"); send(c, payload)
        val code = c.responseCode
        if (code !in 200..204) throw java.io.IOException("Salvataggio non riuscito (HTTP $code)")
    }
}
