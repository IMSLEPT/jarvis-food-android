package com.jarvis.food.family

import kotlinx.serialization.json.Json

/**
 * Un giro di sincronizzazione, senza dipendenze da Android (si prova anche sulla JVM): legge la copia comune, la unisce
 * alla propria, la riscrive se serve e controlla che le proprie modifiche siano rimaste.
 */
object FamilySync {
    enum class Kind { OK, UNREADABLE, BUSY }
    class Outcome(val doc: FamilyDoc, val kind: Kind)

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun wrap(key: ByteArray, d: FamilyDoc): String =
        "{\"v\":1,\"d\":\"" + FamilyCrypto.encrypt(key, json.encodeToString(FamilyDoc.serializer(), d)) + "\"}"

    fun unwrap(key: ByteArray, raw: String): FamilyDoc? {
        val d = Regex("\"d\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"").find(raw)?.groupValues?.get(1) ?: return null
        val plain = FamilyCrypto.decrypt(key, d) ?: return null
        return runCatching { json.decodeFromString(FamilyDoc.serializer(), plain) }.getOrNull()
    }

    /** Eccezione di rete se il servizio non risponde: chi chiama la gestisce (le modifiche restano sul telefono). */
    fun round(backend: FamilyBackend, code: FamilyCrypto.Code, start: FamilyDoc, now: Long = System.currentTimeMillis()): Outcome {
        var local = start
        repeat(4) {
            // Documento sparito dal servizio (scaduto o cancellato): ogni telefono ha la copia completa, quindi lo si ricrea da questa.
            val raw = backend.get(code.id)
            val remote = if (raw == null) FamilyDoc() else (unwrap(code.key, raw) ?: return Outcome(local, Kind.UNREADABLE))
            val merged = local.merge(remote).pruned(now)
            if (merged != remote) {
                backend.put(code.id, wrap(code.key, merged))
                val chk = backend.get(code.id)?.let { unwrap(code.key, it) }
                if (chk != null && merged.merge(chk) != chk) { local = merged.merge(chk); return@repeat }   // qualcuno ha scritto nel frattempo: si riprova
            }
            return Outcome(merged, Kind.OK)
        }
        return Outcome(local, Kind.BUSY)
    }
}
