package com.jarvis.food

import com.jarvis.food.family.FEntry
import com.jarvis.food.family.FLine
import com.jarvis.food.family.FMember
import com.jarvis.food.family.FamilyAgg
import com.jarvis.food.family.FamilyBackend
import com.jarvis.food.family.FamilyCrypto
import com.jarvis.food.family.FamilyDoc
import com.jarvis.food.family.FamilySync
import com.jarvis.food.family.TextDbBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Prova REALE del servizio di sincronizzazione (rete vera, dal server di CI): due "telefoni" creano una famiglia,
 * si scambiano modifiche e si allineano. Se il servizio è irraggiungibile il test si salta e lo scrive.
 */
class FamilyLiveTest {
    private val ale = FMember("a", "Alessandro", 1)
    private val ali = FMember("b", "Alessandra", 1)

    private fun reachable(be: FamilyBackend, payload: String): String? = try {
        be.create(payload)
    } catch (e: Exception) { println("[famiglia] SERVIZIO NON RAGGIUNGIBILE: ${e.javaClass.simpleName} ${e.message}"); null }

    @Test fun duePhoneVeriSullaRete() {
        val be: FamilyBackend = TextDbBackend()
        val key = FamilyCrypto.newKey()
        val t0 = System.currentTimeMillis()
        val a0 = FamilyDoc(members = listOf(ale), entries = listOf(
            FEntry("plan:a", "a", "plan", "Piano", listOf(FLine("pasta", "Pasta", 120.0)), emptyList(), false, 10, "a", "Alessandro")))
        val id = reachable(be, FamilySync.wrap(key, a0))
        assumeTrue("servizio non raggiungibile da questo ambiente", id != null)
        println("[famiglia] creazione ok in ${System.currentTimeMillis() - t0} ms, id=${id!!.length} caratteri")
        val code = FamilyCrypto.Code(id, key)

        // lettura da "un altro telefono" con il codice
        val parsed = FamilyCrypto.parseCode(code.text())!!
        val raw = be.get(parsed.id)!!
        println("[famiglia] il servizio conserva solo dati cifrati: ${!raw.contains("Alessandro") && !raw.contains("pasta")}")
        assertTrue(!raw.contains("Alessandro") && !raw.contains("pasta"))

        // B entra e aggiunge la sua pasta, A e B si allineano
        var b = FamilyDoc(members = listOf(ali), entries = listOf(
            FEntry("plan:b", "b", "plan", "Piano", listOf(FLine("pasta", "Pasta", 80.0)), emptyList(), false, 11, "b", "Alessandra")))
        val t1 = System.currentTimeMillis()
        b = FamilySync.round(be, parsed, b).doc
        var a = FamilySync.round(be, code, a0).doc
        println("[famiglia] due giri di sincronizzazione in ${System.currentTimeMillis() - t1} ms")
        val pasta = FamilyAgg.aggregate(a, { it }, { "Altro" }).first { it.foodId == "pasta" }
        println("[famiglia] pasta sul telefono A = ${pasta.grams} g (atteso 200)")
        assertEquals(200.0, pasta.grams, 1e-9)

        // modifiche quasi simultanee da entrambi
        a = a.copy(entries = a.entries + FEntry("m1", "a", "manual", "Latte", listOf(FLine("latte", "Latte", 1000.0)), emptyList(), false, 100, "a", "Alessandro"))
        b = b.copy(entries = b.entries + FEntry("m2", "b", "manual", "Pane", listOf(FLine("pane", "Pane", 500.0)), emptyList(), false, 101, "b", "Alessandra"))
        val threads = listOf(Thread { a = FamilySync.round(be, code, a).doc }, Thread { b = FamilySync.round(be, code, b).doc })
        threads.forEach { it.start() }; threads.forEach { it.join() }
        // qualche giro in più, come fa l'app ogni pochi secondi
        repeat(2) { a = FamilySync.round(be, code, a).doc; b = FamilySync.round(be, code, b).doc }
        val names = { d: FamilyDoc -> FamilyAgg.aggregate(d, { it }, { "Altro" }).map { it.name }.sorted() }
        println("[famiglia] dopo scritture simultanee A vede ${names(a)} e B vede ${names(b)}")
        assertEquals(a, b)
        assertEquals(listOf("latte", "pane", "pasta"), names(a).map { it.lowercase() })
    }
}
