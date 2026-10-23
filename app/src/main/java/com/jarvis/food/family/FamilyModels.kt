package com.jarvis.food.family

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.max

/** Una riga della spesa: un alimento del database (con grammi) oppure un prodotto libero scritto a mano. */
@Serializable
data class FLine(val foodId: String? = null, val name: String, val grams: Double = 0.0, val qty: String = "")

/** Un pasto programmato di un familiare: serve a proporre di cucinare insieme. Contiene solo ingredienti e quantità, mai calorie o macro. */
@Serializable
data class FMeal(val date: String, val slot: String, val recipeId: String, val recipeName: String, val ings: List<FLine>)

/**
 * Un contributo alla spesa comune (ogni voce vince per "ultima modifica", [ts]):
 * - "plan": gli ingredienti dei pasti del piano di UN familiare (si ricalcola da solo quando il piano cambia);
 * - "recipe": gli ingredienti di una ricetta aggiunta con "Aggiungi alla spesa";
 * - "manual": un prodotto scritto a mano.
 */
@Serializable
data class FEntry(
    val id: String, val member: String, val kind: String, val label: String,
    val lines: List<FLine> = emptyList(), val meals: List<FMeal> = emptyList(),
    val deleted: Boolean = false, val ts: Long, val by: String, val byName: String = ""
)

@Serializable
data class FMember(val id: String, val name: String, val ts: Long, val removed: Boolean = false)

/** Spunta "comprato" di un prodotto aggregato ([key] = alimento o nome normalizzato). */
@Serializable
data class FCheck(val key: String, val checked: Boolean, val by: String, val byName: String = "", val ts: Long, val boughtGrams: Double = 0.0)

@Serializable
data class FamilyDoc(
    val v: Int = 1,
    val members: List<FMember> = emptyList(),
    val entries: List<FEntry> = emptyList(),
    val checks: List<FCheck> = emptyList()
) {
    /** Unione di due copie del documento: per ogni elemento vince la modifica più recente (a parità, l'id maggiore: risultato uguale su tutti i dispositivi). */
    fun merge(o: FamilyDoc): FamilyDoc = FamilyDoc(
        max(v, o.v),
        mergeBy(members + o.members, { it.id }, { it.ts }),
        mergeBy(entries + o.entries, { it.id }, { it.ts }),
        mergeBy(checks + o.checks, { it.key }, { it.ts })
    )

    /** Toglie le voci cancellate da più di 30 giorni (le lapidi servono a far sparire la voce anche sugli altri telefoni). */
    fun pruned(now: Long): FamilyDoc = copy(entries = entries.filterNot { it.deleted && now - it.ts > 30L * 86_400_000 })

    fun activeMembers() = members.filterNot { it.removed }
    fun name(id: String) = members.firstOrNull { it.id == id }?.name ?: "—"

    private fun <T, K> mergeBy(all: List<T>, key: (T) -> K, ts: (T) -> Long): List<T> {
        val m = LinkedHashMap<K, T>()
        for (x in all) {
            val cur = m[key(x)]
            if (cur == null || ts(x) > ts(cur) || (ts(x) == ts(cur) && x.toString() > cur.toString())) m[key(x)] = x
        }
        return m.entries.sortedBy { it.key.toString() }.map { it.value }
    }
}

/** Voce della lista comune dopo aver sommato i contributi di tutti. */
data class AggItem(
    val key: String, val foodId: String?, val name: String, val grams: Double, val qtyTexts: List<String>,
    val shop: String, val parts: List<Part>, val check: FCheck?, val manualEntryIds: List<String>
) {
    data class Part(val memberId: String, val memberName: String, val grams: Double, val source: String, val qty: String = "")
    val checked get() = check?.checked == true
    /** Spuntato ma poi è servito altro (es. il piano è cambiato): resta da prendere la differenza. */
    fun missingAfterCheck(): Double = if (checked && foodId != null) max(0.0, grams - (check?.boughtGrams ?: grams)) else 0.0
}

object FamilyAgg {
    fun norm(s: String) = s.lowercase().trim().replace(Regex("\\s+"), " ")

    /**
     * Somma gli ingredienti di tutti i contributi attivi: stessa chiave (alimento del database o nome identico) = una sola riga.
     * Esempio: 120 g di pasta nel piano di uno e 80 g in quello dell'altra diventano 200 g, con il dettaglio di chi li usa.
     */
    fun aggregate(doc: FamilyDoc, foodName: (String) -> String?, shopOf: (String?) -> String): List<AggItem> {
        val active = doc.members.filterNot { it.removed }.map { it.id }.toSet()
        data class Acc(var foodId: String?, var name: String, var grams: Double = 0.0, val qty: MutableList<String> = mutableListOf(),
                       val parts: MutableList<AggItem.Part> = mutableListOf(), val ids: MutableList<String> = mutableListOf())
        val acc = LinkedHashMap<String, Acc>()
        for (e in doc.entries) {
            if (e.deleted || e.member !in active) continue
            for (l in e.lines) {
                val key = l.foodId?.let { "f:$it" } ?: "t:${norm(l.name)}"
                val a = acc.getOrPut(key) { Acc(l.foodId, l.foodId?.let(foodName) ?: l.name) }
                a.grams += l.grams
                if (l.qty.isNotBlank()) a.qty += l.qty
                a.parts += AggItem.Part(e.member, doc.name(e.member), l.grams, e.kind, l.qty)
                if (e.kind != "plan") a.ids += e.id
            }
        }
        val checks = doc.checks.associateBy { it.key }
        return acc.map { (k, a) ->
            AggItem(k, a.foodId, a.name, a.grams, a.qty.distinct(), shopOf(a.foodId), a.parts.toList(), checks[k], a.ids.distinct())
        }
    }

    /** Quanti pezzi/confezioni servono: "2 × 500 g". Se non c'è una confezione tipica restano i grammi. */
    fun packs(grams: Double, pack: Double?): Int? = if (pack == null || pack <= 0 || grams <= 0) null else max(1, ceil(grams / pack - 1e-9).toInt())
}

/** Due o più familiari che mangiano la stessa ricetta nello stesso giorno: si può cucinare una volta sola. */
data class CookTogether(
    val date: String, val recipeId: String, val recipeName: String,
    val members: List<Pair<String, String>>,             // (id, nome)
    val slots: Map<String, String>,                      // memberId -> pasto
    val total: List<FLine>,                              // quantità totali da usare
    val perMember: Map<String, List<FLine>>              // porzione di ciascuno (con la sua dieta)
)

object FamilyCook {
    fun together(doc: FamilyDoc, today: String): List<CookTogether> {
        val active = doc.activeMembers().associateBy { it.id }
        // (data, ricetta) -> membro -> pasto
        val by = LinkedHashMap<Pair<String, String>, MutableMap<String, FMeal>>()
        for (e in doc.entries) {
            if (e.deleted || e.kind != "plan" || e.member !in active) continue
            for (m in e.meals) if (m.date >= today) by.getOrPut(m.date to m.recipeId) { LinkedHashMap() }[e.member] = m
        }
        return by.filter { it.value.size >= 2 }.map { (k, per) ->
            val total = LinkedHashMap<String, FLine>()
            for ((_, m) in per) for (l in m.ings) {
                val key = l.foodId ?: norm(l.name)
                total[key] = total[key]?.let { it.copy(grams = it.grams + l.grams) } ?: l
            }
            val first = per.values.first()
            CookTogether(k.first, k.second, first.recipeName, per.keys.map { it to (active[it]?.name ?: "—") },
                per.mapValues { it.value.slot }, total.values.toList(), per.mapValues { it.value.ings })
        }.sortedBy { it.date }
    }
    private fun norm(s: String) = FamilyAgg.norm(s)
}
