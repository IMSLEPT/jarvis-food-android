package com.jarvis.food

import com.jarvis.food.family.AggItem
import com.jarvis.food.family.FCheck
import com.jarvis.food.family.FEntry
import com.jarvis.food.family.FLine
import com.jarvis.food.family.FMeal
import com.jarvis.food.family.FMember
import com.jarvis.food.family.FamilyAgg
import com.jarvis.food.family.FamilyBackend
import com.jarvis.food.family.FamilyCook
import com.jarvis.food.family.FamilyCrypto
import com.jarvis.food.family.FamilyDoc
import com.jarvis.food.family.FamilySync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Famiglia: somma delle quantità, fusione delle modifiche, cifratura, "cucinare insieme" e giro di sincronizzazione. */
class FamilyTest {
    private val ale = FMember("a", "Alessandro", 1)
    private val ali = FMember("b", "Alessandra", 1)

    private fun plan(member: String, name: String, ts: Long, vararg lines: FLine, meals: List<FMeal> = emptyList()) =
        FEntry("plan:$member", member, "plan", "Piano di $name", lines.toList(), meals, false, ts, member, name)

    private fun agg(d: FamilyDoc) = FamilyAgg.aggregate(d, { it }, { "Altro" })

    @Test fun pasta120Piu80Fa200() {
        val d = FamilyDoc(members = listOf(ale, ali), entries = listOf(
            plan("a", "Alessandro", 10, FLine("pasta", "Pasta", 120.0), FLine("pomodoro", "Pomodoro", 200.0)),
            plan("b", "Alessandra", 11, FLine("pasta", "Pasta", 80.0))))
        val items = agg(d)
        assertEquals(2, items.size)
        val pasta = items.first { it.foodId == "pasta" }
        assertEquals(200.0, pasta.grams, 1e-9)
        assertEquals(2, pasta.parts.size)
        assertEquals(setOf("Alessandro", "Alessandra"), pasta.parts.map { it.memberName }.toSet())
    }

    @Test fun nessunDuplicatoTraPianoEProdottiManuali() {
        val manual = FEntry("m1", "a", "manual", "Pasta", listOf(FLine("pasta", "Pasta", 500.0)), emptyList(), false, 20, "a", "Alessandro")
        val recipe = FEntry("r1", "b", "recipe", "Carbonara", listOf(FLine("pasta", "Pasta", 160.0), FLine("uova", "Uova", 100.0)), emptyList(), false, 21, "b", "Alessandra")
        val d = FamilyDoc(members = listOf(ale, ali), entries = listOf(plan("a", "Alessandro", 10, FLine("pasta", "Pasta", 120.0)), manual, recipe))
        val items = agg(d)
        assertEquals(1, items.count { it.foodId == "pasta" })
        assertEquals(780.0, items.first { it.foodId == "pasta" }.grams, 1e-9)
        assertEquals(listOf("m1", "r1"), items.first { it.foodId == "pasta" }.manualEntryIds)
    }

    @Test fun prodottiLiberiConStessoNomeSiUniscono() {
        val e1 = FEntry("m1", "a", "manual", "Detersivo", listOf(FLine(null, "Detersivo", 0.0, "1")), emptyList(), false, 1, "a", "A")
        val e2 = FEntry("m2", "b", "manual", " detersivo ", listOf(FLine(null, " detersivo ", 0.0, "2")), emptyList(), false, 2, "b", "B")
        val items = agg(FamilyDoc(members = listOf(ale, ali), entries = listOf(e1, e2)))
        assertEquals(1, items.size)
        assertEquals(listOf("1", "2"), items[0].qtyTexts)
    }

    @Test fun cancellazioneEMembriRimossiNonContano() {
        val e = plan("b", "Alessandra", 5, FLine("pasta", "Pasta", 80.0))
        val d = FamilyDoc(members = listOf(ale, ali.copy(removed = true, ts = 9)), entries = listOf(e, plan("a", "Alessandro", 5, FLine("pasta", "Pasta", 120.0))))
        assertEquals(120.0, agg(d).first().grams, 1e-9)
        val del = e.copy(deleted = true, ts = 9)
        val d2 = FamilyDoc(members = listOf(ale, ali), entries = listOf(del))
        assertTrue(agg(d2).isEmpty())
    }

    @Test fun fusioneVinceLUltimaModificaEDeterministica() {
        val a = FamilyDoc(members = listOf(ale), entries = listOf(plan("a", "Alessandro", 10, FLine("pasta", "Pasta", 100.0))))
        val b = FamilyDoc(members = listOf(ali), entries = listOf(plan("a", "Alessandro", 20, FLine("pasta", "Pasta", 150.0)),
            FEntry("x", "b", "manual", "Latte", listOf(FLine("latte", "Latte", 1000.0)), emptyList(), false, 5, "b", "B")))
        val ab = a.merge(b); val ba = b.merge(a)
        assertEquals(ab, ba)
        assertEquals(150.0, ab.entries.first { it.id == "plan:a" }.lines[0].grams, 1e-9)
        assertEquals(2, ab.members.size)
        assertEquals(ab, ab.merge(a).merge(b))   // idempotente
    }

    @Test fun lapideCancellaAncheSuLAltroTelefono() {
        val e = FEntry("m1", "a", "manual", "Pane", listOf(FLine("pane", "Pane", 500.0)), emptyList(), false, 5, "a", "A")
        val phoneB = FamilyDoc(members = listOf(ale, ali), entries = listOf(e))
        val phoneA = FamilyDoc(members = listOf(ale, ali), entries = listOf(e.copy(deleted = true, ts = 9)))
        assertTrue(agg(phoneB.merge(phoneA)).isEmpty())
        assertTrue(agg(phoneA.merge(phoneB)).isEmpty())
    }

    @Test fun spuntaVisibileAglAltriEPianoCambiatoRiapre() {
        val d0 = FamilyDoc(members = listOf(ale, ali), entries = listOf(plan("a", "Alessandro", 10, FLine("pasta", "Pasta", 120.0))))
        val item = agg(d0).first()
        val checked = d0.copy(checks = listOf(FCheck(item.key, true, "b", "Alessandra", 30, item.grams)))
        val other = d0.merge(checked)
        val i1 = agg(other).first()
        assertTrue(i1.checked); assertEquals(0.0, i1.missingAfterCheck(), 1e-9)
        // il piano cambia dopo l'acquisto: servono 80 g in più
        val changed = other.merge(FamilyDoc(entries = listOf(plan("a", "Alessandro", 40, FLine("pasta", "Pasta", 200.0)))))
        val i2 = agg(changed).first()
        assertTrue(i2.checked); assertEquals(80.0, i2.missingAfterCheck(), 1e-9)
    }

    @Test fun confezioni() {
        assertEquals(2, FamilyAgg.packs(501.0, 500.0))
        assertEquals(1, FamilyAgg.packs(500.0, 500.0))
        assertEquals(1, FamilyAgg.packs(20.0, 500.0))
        assertNull(FamilyAgg.packs(200.0, null))
        assertNull(FamilyAgg.packs(0.0, 500.0))
    }

    @Test fun cifraturaAndataERitorno() {
        val key = FamilyCrypto.newKey()
        val d = FamilyDoc(members = listOf(ale), entries = listOf(plan("a", "Alessandro", 1, FLine("pasta", "Pasta", 120.0))))
        val raw = FamilySync.wrap(key, d)
        assertFalse("il testo cifrato non deve contenere dati in chiaro", raw.contains("Alessandro") || raw.contains("pasta"))
        assertEquals(d, FamilySync.unwrap(key, raw))
        assertNull("chiave sbagliata", FamilySync.unwrap(FamilyCrypto.newKey(), raw))
        assertNull("dati alterati", FamilySync.unwrap(key, raw.replace("\"d\":\"", "\"d\":\"A")))
        assertNotEquals(FamilySync.wrap(key, d), FamilySync.wrap(key, d))   // IV diverso a ogni cifratura
    }

    @Test fun codiceInvito() {
        val c = FamilyCrypto.Code("abc-123_xyz", FamilyCrypto.newKey())
        val t = c.text()
        val p = FamilyCrypto.parseCode("  Ti invito: \n" + t.chunked(10).joinToString(" ") + " grazie")
        assertNotNull(p); assertEquals(c.id, p!!.id); assertTrue(c.key.contentEquals(p.key))
        assertNull(FamilyCrypto.parseCode("JF1.abc"))
        assertNull(FamilyCrypto.parseCode("ciao"))
    }

    @Test fun cucinareInsieme() {
        fun meal(date: String, slot: String, g: Double) = FMeal(date, slot, "carbonara", "Carbonara", listOf(FLine("pasta", "Pasta", g), FLine("uova", "Uova", 100.0)))
        val d = FamilyDoc(members = listOf(ale, ali), entries = listOf(
            plan("a", "Alessandro", 1, meals = listOf(meal("2030-01-02", "Pranzo", 120.0), FMeal("2030-01-03", "Cena", "pizza", "Pizza", emptyList()))),
            plan("b", "Alessandra", 1, meals = listOf(meal("2030-01-02", "Cena", 80.0)))))
        val r = FamilyCook.together(d, "2030-01-01")
        assertEquals(1, r.size)
        val c = r[0]
        assertEquals("2030-01-02", c.date)
        assertEquals(200.0, c.total.first { it.foodId == "pasta" }.grams, 1e-9)
        assertEquals(200.0, c.total.first { it.foodId == "uova" }.grams, 1e-9)
        assertEquals(120.0, c.perMember["a"]!!.first { it.foodId == "pasta" }.grams, 1e-9)
        assertEquals(80.0, c.perMember["b"]!!.first { it.foodId == "pasta" }.grams, 1e-9)
        assertTrue(FamilyCook.together(d, "2030-01-03").isEmpty())   // giorni passati ignorati
    }

    /** Servizio finto in memoria: permette di simulare due telefoni e una scrittura concorrente. */
    private class MemBackend : FamilyBackend {
        var data: String? = null
        var beforePut: (() -> Unit)? = null
        override fun create(payload: String): String { data = payload; return "mem" }
        override fun get(id: String) = data
        override fun put(id: String, payload: String) { beforePut?.also { beforePut = null }?.invoke(); data = payload }
    }

    @Test fun dueTelefoniSiAllineanoEConcorrenzaNonPerdeModifiche() {
        val key = FamilyCrypto.newKey(); val code = FamilyCrypto.Code("mem", key)
        val be = MemBackend()
        var a = FamilyDoc(members = listOf(ale), entries = listOf(plan("a", "Alessandro", 10, FLine("pasta", "Pasta", 120.0))))
        be.create(FamilySync.wrap(key, a))
        var b = FamilyDoc(members = listOf(ali), entries = listOf(plan("b", "Alessandra", 11, FLine("pasta", "Pasta", 80.0))))
        b = FamilySync.round(be, code, b).doc
        a = FamilySync.round(be, code, a).doc
        assertEquals(200.0, agg(a).first().grams, 1e-9)
        // B spunta e A aggiunge un prodotto nello stesso istante (scrittura concorrente simulata)
        val item = agg(b).first()
        b = b.copy(checks = listOf(FCheck(item.key, true, "b", "Alessandra", 50, item.grams)))
        a = a.copy(entries = a.entries + FEntry("m", "a", "manual", "Latte", listOf(FLine("latte", "Latte", 1000.0)), emptyList(), false, 60, "a", "Alessandro"))
        be.beforePut = { FamilySync.round(be, code, a) }   // A scrive mentre B sta per scrivere
        b = FamilySync.round(be, code, b).doc
        a = FamilySync.round(be, code, a).doc
        b = FamilySync.round(be, code, b).doc
        assertEquals(a, b)
        assertTrue(agg(a).first { it.foodId == "pasta" }.checked)
        assertTrue(agg(a).any { it.foodId == "latte" })
    }

    @Test fun famigliaCancellataSulServizio() {
        val key = FamilyCrypto.newKey(); val be = MemBackend()
        assertEquals(FamilySync.Kind.GONE, FamilySync.round(be, FamilyCrypto.Code("mem", key), FamilyDoc()).kind)
        be.data = "{\"v\":1,\"d\":\"AAAA\"}"
        assertEquals(FamilySync.Kind.UNREADABLE, FamilySync.round(be, FamilyCrypto.Code("mem", key), FamilyDoc()).kind)
    }
}
