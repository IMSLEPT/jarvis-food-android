package com.jarvis.food.family

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Il documento condiviso viene cifrato sul telefono (AES-128-GCM) prima di uscire: il servizio che lo conserva vede solo
 * dati illeggibili. La chiave sta solo nel codice d'invito, che si scambia tra familiari.
 */
object FamilyCrypto {
    private val rnd = SecureRandom()
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun newKey(): ByteArray = ByteArray(16).also { rnd.nextBytes(it) }

    fun encrypt(key: ByteArray, plain: String): String {
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return enc.encodeToString(iv + ct)
    }

    /** Null se la chiave non corrisponde o i dati sono alterati. */
    fun decrypt(key: ByteArray, data: String): String? = runCatching {
        val all = dec.decode(data)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, all.copyOfRange(0, 12)))
        String(c.doFinal(all, 12, all.size - 12), Charsets.UTF_8)
    }.getOrNull()

    /** Codice d'invito: identificativo del documento + chiave. Esempio: JF1.<id>.<chiave> */
    data class Code(val id: String, val key: ByteArray) {
        fun text() = "JF1." + enc.encodeToString(id.toByteArray()) + "." + enc.encodeToString(key)
    }

    /** Trova il codice anche dentro un messaggio (spazi, a capo e testo attorno vengono ignorati): la chiave è sempre di 22 caratteri. */
    fun parseCode(text: String): Code? = runCatching {
        val t = text.replace(Regex("\\s+"), "")
        val m = Regex("JF1\\.([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]{22})").find(t) ?: return null
        val id = String(dec.decode(m.groupValues[1]), Charsets.UTF_8)
        val key = dec.decode(m.groupValues[2])
        if (id.isBlank() || key.size != 16) null else Code(id, key)
    }.getOrNull()
}
