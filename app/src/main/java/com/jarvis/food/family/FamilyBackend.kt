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

/**
 * textdb.dev: archivio di testo gratuito e senza account. L'identificativo lo genera il telefono (128 bit casuali, quindi non
 * indovinabile) e il contenuto è comunque cifrato. Una chiave mai scritta restituisce testo vuoto: lo trattiamo come "non esiste".
 */
class TextDbBackend(private val base: String = "https://textdb.dev/api/data/") : FamilyBackend {

    private fun open(id: String, method: String): HttpURLConnection = (URL(base + id).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 10_000; readTimeout = 20_000
        setRequestProperty("Accept", "*/*"); setRequestProperty("User-Agent", "JarvisFood")
        // text/plain: con il tipo predefinito dei form il servizio aggiunge un "=" in fondo al contenuto.
        setRequestProperty("Content-Type", "text/plain")
    }

    override fun create(payload: String): String {
        val id = java.util.UUID.randomUUID().toString().replace("-", "")
        put(id, payload)
        return id
    }

    override fun get(id: String): String? {
        val c = open(id, "GET")
        return when (val code = c.responseCode) {
            200 -> c.inputStream.bufferedReader().use { it.readText() }.takeIf { it.isNotBlank() }
            404 -> null
            else -> throw java.io.IOException("Lettura non riuscita (HTTP $code)")
        }
    }

    override fun put(id: String, payload: String) {
        val c = open(id, "POST"); c.doOutput = true
        c.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        if (code !in 200..204) throw java.io.IOException("Salvataggio non riuscito (HTTP $code)")
        c.inputStream.close()
    }
}
