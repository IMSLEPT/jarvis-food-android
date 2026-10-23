package com.jarvis.food.family

import android.content.Context
import com.jarvis.food.data.Db
import com.jarvis.food.data.FamilyLink
import com.jarvis.food.data.Store
import com.jarvis.food.engine.Shopping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * Famiglia e spesa condivisa. Ogni telefono tiene una copia completa del documento (funziona anche offline) e la sincronizza
 * con le copie degli altri tramite un archivio cifrato: si legge, si uniscono le modifiche (vince la più recente per ogni voce),
 * si riscrive e si controlla che le proprie modifiche siano rimaste. La dieta e i dati personali non entrano mai nel documento.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
object FamilyStore {
    data class Status(val syncing: Boolean = false, val lastOk: Long = 0, val error: String? = null, val notice: String? = null)

    private val json get() = FamilySync.json
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var file: File? = null
    private var kickJob: Job? = null

    var backend: FamilyBackend = JsonBlobBackend()

    private val _doc = MutableStateFlow<FamilyDoc?>(null)
    val doc: StateFlow<FamilyDoc?> = _doc.asStateFlow()
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    /** true mentre la schermata Famiglia è aperta: si sincronizza più spesso. */
    @Volatile var screenOpen = false

    val link: FamilyLink? get() = Store.current.family

    fun init(ctx: Context) {
        if (file != null) return
        file = File(ctx.filesDir, "family_doc.json")
        file?.takeIf { it.exists() }?.let { f -> runCatching { _doc.value = json.decodeFromString(FamilyDoc.serializer(), f.readText()) } }
        if (link == null) { _doc.value = null; file?.delete() }
        // Se cambia il piano (o i giorni scelti) la lista comune si aggiorna da sola, senza toccare i prodotti aggiunti a mano.
        scope.launch {
            Store.state.map { Triple(it.plan, it.dietPrefs, it.family?.span) }.distinctUntilChanged().debounce(2000).collect {
                if (link != null) runCatching { publishPlan() }
            }
        }
    }

    private fun save(d: FamilyDoc) { runCatching { file?.writeText(json.encodeToString(FamilyDoc.serializer(), d)) } }
    // ------------------------------------------------------------------ Collegamento

    /** Crea una nuova famiglia. Restituisce il codice d'invito oppure lancia un'eccezione con il motivo. */
    suspend fun create(name: String): String = withContext(Dispatchers.IO) {
        val me = Store.newId(); val now = System.currentTimeMillis()
        val key = FamilyCrypto.newKey()
        val doc = FamilyDoc(members = listOf(FMember(me, name.trim(), now)))
        val id = backend.create(FamilySync.wrap(key, doc))
        val code = FamilyCrypto.Code(id, key).text()
        Store.update { it.copy(family = FamilyLink(code, me, name.trim(), 3, now)) }
        _doc.value = doc; save(doc)
        publishPlan(); kick()
        code
    }

    /** Entra in una famiglia con il codice ricevuto. */
    suspend fun join(codeText: String, name: String): Unit = withContext(Dispatchers.IO) {
        val code = FamilyCrypto.parseCode(codeText) ?: throw IllegalArgumentException("Il codice non è valido: copialo per intero.")
        val raw = backend.get(code.id) ?: throw IllegalStateException("Questa famiglia non esiste più.")
        val remote = FamilySync.unwrap(code.key, raw) ?: throw IllegalArgumentException("Il codice non corrisponde a nessuna famiglia (o è incompleto).")
        val me = Store.newId(); val now = System.currentTimeMillis()
        val doc = remote.merge(FamilyDoc(members = listOf(FMember(me, name.trim(), now))))
        backend.put(code.id, FamilySync.wrap(code.key, doc))
        Store.update { it.copy(family = FamilyLink(code.text(), me, name.trim(), 3, now)) }
        _doc.value = doc; save(doc)
        publishPlan(); kick()
    }

    /** Esce dalla famiglia: le proprie voci spariscono dagli altri telefoni, i dati personali restano qui. */
    suspend fun leave() {
        val l = link ?: return
        runCatching { removeMemberNow(l.memberId) }
        runCatching { sync() }
        unlink()
    }

    private fun unlink() {
        Store.update { it.copy(family = null) }
        _doc.value = null; file?.delete()
        _status.value = Status()
    }

    fun removeMember(id: String) = removeMemberNow(id)

    private fun removeMemberNow(id: String) {
        val now = System.currentTimeMillis()
        edit { d -> d.copy(members = d.members.map { if (it.id == id) it.copy(removed = true, ts = now) else it },
            entries = d.entries.map { if (it.member == id && !it.deleted) it.copy(deleted = true, ts = now) else it }) }
    }

    // ------------------------------------------------------------------ Modifiche locali (poi sincronizzate)

    private fun me() = link?.memberId ?: ""
    private fun myName() = link?.name ?: ""

    private fun edit(block: (FamilyDoc) -> FamilyDoc) {
        _doc.update { block(it ?: FamilyDoc()) }
        _doc.value?.let { save(it) }
        kick()
    }

    private fun putEntry(e: FamilyEntryBuilder) = edit { d -> d.copy(entries = d.entries.filterNot { it.id == e.id } + e.build()) }

    private class FamilyEntryBuilder(val id: String, val kind: String, val label: String, val lines: List<FLine>, val meals: List<FMeal> = emptyList(), val member: String, val by: String, val byName: String) {
        fun build() = FEntry(id, member, kind, label, lines, meals, false, System.currentTimeMillis(), by, byName)
    }

    /** Aggiunge un prodotto scritto a mano. Se il nome corrisponde a un alimento e la quantità è leggibile, si somma agli ingredienti dei piani. */
    fun addManual(name: String, qty: String) {
        if (link == null || name.isBlank()) return
        edit { d -> d.copy(entries = d.entries + FEntry(Store.newId(), me(), "manual", name.trim(), listOf(lineFromText(name.trim(), qty.trim())), emptyList(), false, System.currentTimeMillis(), me(), myName())) }
    }

    /** Aggiunge gli ingredienti di una ricetta alla spesa di tutti. */
    fun addRecipe(label: String, items: List<Pair<String, Double>>) {
        if (link == null || items.isEmpty()) return
        val lines = items.groupBy({ it.first }, { it.second }).map { (id, g) -> FLine(id, Db.db.food(id)?.name ?: id, Math.round(g.sum() * 10) / 10.0) }
        edit { d -> d.copy(entries = d.entries + FEntry(Store.newId(), me(), "recipe", label, lines, emptyList(), false, System.currentTimeMillis(), me(), myName())) }
    }

    fun removeEntry(id: String) = edit { d -> d.copy(entries = d.entries.map { if (it.id == id) it.copy(deleted = true, ts = System.currentTimeMillis()) else it }) }

    fun setChecked(item: AggItem, checked: Boolean) = edit { d ->
        d.copy(checks = d.checks.filterNot { it.key == item.key } + FCheck(item.key, checked, me(), myName(), System.currentTimeMillis(), if (checked) item.grams else 0.0))
    }

    fun setSpan(days: Int) {
        Store.update { it.copy(family = it.family?.copy(span = days)) }
        scope.launch { runCatching { publishPlan() } }
    }

    /** Ricalcola dal piano personale gli ingredienti e i pasti dei giorni scelti e li aggiorna nel documento (solo se è cambiato qualcosa). */
    fun publishPlan() {
        val l = link ?: return
        val s = Store.current
        val today = LocalDate.now()
        val meals = ArrayList<FMeal>()
        val sum = LinkedHashMap<String, Double>()
        for (d in 0 until l.span.coerceIn(1, 14)) {
            val date = today.plusDays(d.toLong())
            for (pm in Store.plannedMeals(s, date)) {
                val ings = pm.solved.items.filter { it.food.id !in setOf("sale", "pepe") && it.grams > 0 }
                    .map { FLine(it.food.id, it.food.name, Math.round(it.grams * 10) / 10.0) }
                meals += FMeal(date.toString(), pm.slotName, pm.solved.template.id, pm.solved.template.name, ings)
                for (i in ings) sum[i.foodId!!] = (sum[i.foodId] ?: 0.0) + i.grams
            }
        }
        val lines = sum.map { (id, g) -> FLine(id, Db.db.food(id)?.name ?: id, Math.round(g * 10) / 10.0) }
        val id = "plan:${l.memberId}"
        val cur = _doc.value?.entries?.firstOrNull { it.id == id }
        if (cur != null && !cur.deleted && cur.lines == lines && cur.meals == meals) return
        if (cur == null && lines.isEmpty()) return
        putEntry(FamilyEntryBuilder(id, "plan", "Piano di ${l.name}", lines, meals, l.memberId, l.memberId, l.name))
    }

    private fun lineFromText(name: String, qty: String): FLine {
        val f = Db.db.match(name)
        if (qty.isBlank()) return if (f != null) FLine(f.id, f.name, 0.0) else FLine(null, name)
        val m = Regex("(\\d+(?:[.,]\\d+)?)\\s*(kg|g|gr|l|lt|ml|cl|dl)?", RegexOption.IGNORE_CASE).find(qty)
        val n = m?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        if (f != null && m != null && n != null) {
            val grams: Double? = when (m.groupValues[2].lowercase()) {
                "kg", "l", "lt" -> n * 1000
                "dl" -> n * 100
                "cl" -> n * 10
                "g", "gr", "ml" -> n
                else -> f.pieceGrams?.let { n * it }      // "3" = 3 pezzi, solo se l'alimento ha un peso a pezzo
            }
            return if (grams != null) FLine(f.id, f.name, grams) else FLine(f.id, f.name, 0.0, qty)
        }
        return if (f != null) FLine(f.id, f.name, 0.0, qty) else FLine(null, name, 0.0, qty)
    }

    // ------------------------------------------------------------------ Sincronizzazione

    /** Pianifica una sincronizzazione subito dopo una modifica (raggruppa più modifiche ravvicinate). */
    fun kick() {
        kickJob?.cancel()
        kickJob = scope.launch { delay(350); runCatching { sync() } }
    }

    /** Un giro completo: legge, unisce, riscrive se serve e verifica. */
    suspend fun sync(): Unit = withContext(Dispatchers.IO) {
        val l = link ?: return@withContext
        val code = FamilyCrypto.parseCode(l.code) ?: return@withContext
        mutex.withLock { syncLocked(l, code) }
    }

    private fun syncLocked(l: FamilyLink, code: FamilyCrypto.Code) {
        _status.update { it.copy(syncing = true) }
        try {
            val out = FamilySync.round(backend, code, _doc.value ?: FamilyDoc())
            when (out.kind) {
                FamilySync.Kind.GONE -> { _status.update { it.copy(syncing = false, error = "La famiglia non è più disponibile sul servizio. I dati restano su questo telefono.") }; return }
                FamilySync.Kind.UNREADABLE -> { _status.update { it.copy(syncing = false, error = "Impossibile leggere i dati condivisi (codice non valido).") }; return }
                else -> {}
            }
            _doc.update { cur -> (cur ?: FamilyDoc()).merge(out.doc) }
            _doc.value?.let { save(it) }
            if (out.kind == FamilySync.Kind.OK) {
                Store.update { it.copy(family = it.family?.copy(lastSync = System.currentTimeMillis())) }
                _status.update { it.copy(syncing = false, lastOk = System.currentTimeMillis(), error = null) }
                // se un altro familiare mi ha rimosso, esco
                val d = _doc.value
                if (d != null && d.members.any { it.id == l.memberId && it.removed }) { unlink(); _status.value = Status(notice = "Sei stato rimosso dalla famiglia.") }
            } else {
                _status.update { it.copy(syncing = false, error = "Troppi aggiornamenti contemporanei: riprovo tra poco.") }
            }
        } catch (e: Exception) {
            _status.update { it.copy(syncing = false, error = "Senza connessione: le modifiche partiranno appena torna la rete.") }
        }
    }

    /** Un passo del ciclo automatico: controlla se il piano è cambiato e sincronizza. */
    suspend fun tick() { if (link == null) return; publishPlan(); sync() }

    /** Ciclo in primo piano: ogni pochi secondi se la schermata Famiglia è aperta, altrimenti più di rado. */
    suspend fun loop() {
        while (true) {
            if (link != null) runCatching { tick() }
            delay(if (screenOpen) 4_000 else 25_000)
        }
    }

    /** Righe della lista comune, già aggregate e raggruppate per reparto. */
    fun aggregate(d: FamilyDoc): List<AggItem> = FamilyAgg.aggregate(d, { Db.db.food(it)?.name }, { id -> id?.let { Db.db.food(it)?.shop } ?: "Altro" })
        .sortedWith(compareBy({ Shopping.SHOP_ORDER.indexOf(it.shop).let { i -> if (i < 0) 99 else i } }, { it.name }))
}
