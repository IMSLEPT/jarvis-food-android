package com.jarvis.food.ai

import com.jarvis.food.data.Food
import com.jarvis.food.engine.FoodDb

/** Ricetta letta da un testo (incollato o riconosciuto da una foto) o proposta dall'AI, pronta da rivedere nell'editor. */
data class ImportedRecipe(
    val name: String = "",
    val servings: Int = 1,
    val minutes: Int = 0,
    /** Ingredienti riconosciuti nel database, con i grammi TOTALI della ricetta (per [servings] porzioni). */
    val ingredients: List<Pair<Food, Double>> = emptyList(),
    val steps: List<String> = emptyList(),
    val description: String = "",
    val meal: String = "",
    val cuisine: String = "",
    val difficulty: Int = 0,
    /** Righe di ingredienti che non corrispondono a nessun alimento del database: non entrano nel calcolo. */
    val unmatched: List<String> = emptyList(),
    /** Ingredienti riconosciuti ma senza quantità leggibile (usata una quantità standard da controllare). */
    val checkQty: List<String> = emptyList()
)

/**
 * Lettura di una ricetta da testo SENZA AI (funziona offline): trova titolo, porzioni, tempo, ingredienti con quantità
 * (g, kg, ml, l, cucchiai, pezzi...) e passaggi. È un'euristica: le quantità non leggibili vengono segnalate, non inventate.
 */
object RecipeImport {
    private const val NUM = "(\\d+(?:[.,]\\d+)?|\\d+\\s*/\\s*\\d+|½|¼|¾)"
    private val QTY = Regex("$NUM\\s*(kg|chili|chilo|g|gr|grammi|grammo|l|lt|litri|litro|dl|cl|ml|cucchiaini|cucchiaino|cucchiai|cucchiaio|tazze|tazza|bicchieri|bicchiere|pizzico|pizzichi)\\b", RegexOption.IGNORE_CASE)
    private val COUNT = Regex("^\\s*[-•*]?\\s*$NUM\\s+(?!min|ore|ora)(\\p{L}+)", RegexOption.IGNORE_CASE)
    private val ING_HEAD = Regex("^\\s*(ingredienti|ingredients)\\b.*", RegexOption.IGNORE_CASE)
    private val STEP_HEAD = Regex("^\\s*(preparazione|procedimento|istruzioni|esecuzione|come si prepara|metodo|directions|method|instructions)\\b.*", RegexOption.IGNORE_CASE)
    private val STEP_START = Regex("^\\s*(\\d{1,2}\\s*[.)]|[-•*]|passo\\s*\\d+|step\\s*\\d+)\\s*", RegexOption.IGNORE_CASE)
    private val SERV1 = Regex("(\\d{1,2})\\s*(?:persone|porzioni|pers\\b|dosi)", RegexOption.IGNORE_CASE)
    private val SERV2 = Regex("(?:porzioni|dosi\\s+per|per)\\s*:?\\s*(\\d{1,2})\\b(?!\\s*(?:min|ore|g\\b|kg|ml|gr))", RegexOption.IGNORE_CASE)
    private val TIME = Regex("(\\d{1,3})\\s*(?:min\\b|minuti)", RegexOption.IGNORE_CASE)
    private val TIME_H = Regex("(\\d{1,2})\\s*(?:h\\b|ore\\b|ora\\b)", RegexOption.IGNORE_CASE)
    private val TIME_LINE = Regex("^\\s*(tempo|preparazione|cottura|prep|cook|totale)\\b[^\\n]*\\d", RegexOption.IGNORE_CASE)

    private fun num(s: String): Double? {
        val t = s.trim()
        if (t == "½") return 0.5
        if (t == "¼") return 0.25
        if (t == "¾") return 0.75
        if ("/" in t) {
            val p = t.split("/")
            val a = p[0].trim().toDoubleOrNull() ?: return null
            val b = p[1].trim().toDoubleOrNull() ?: return null
            return if (b == 0.0) null else a / b
        }
        return t.replace(",", ".").toDoubleOrNull()
    }

    private fun grams(v: Double, unit: String, f: com.jarvis.food.engine.DbFood): Double = when (unit.lowercase()) {
        "kg", "chili", "chilo" -> v * 1000
        "l", "lt", "litri", "litro" -> v * 1000
        "dl" -> v * 100
        "cl" -> v * 10
        "cucchiaio", "cucchiai" -> v * (if (f.liquid || f.group == "olio") 10 else 12)
        "cucchiaino", "cucchiaini" -> v * 5
        "tazza", "tazze" -> v * 200
        "bicchiere", "bicchieri" -> v * 200
        "pizzico", "pizzichi" -> v * 1
        else -> v
    }

    private fun clean(line: String) = line.replace(' ', ' ').replace(Regex("\\s+"), " ").trim()

    /** Ingrediente di una riga: (alimento, grammi o null se non leggibili) oppure null se l'alimento non è nel database. */
    private fun ingredient(db: FoodDb, line: String): Triple<com.jarvis.food.engine.DbFood, Double?, Boolean>? {
        val f = db.match(line) ?: return null
        QTY.find(line)?.let { m ->
            val v = num(m.groupValues[1]) ?: return@let
            return Triple(f, grams(v, m.groupValues[2], f), true)
        }
        COUNT.find(line)?.let { m ->
            val v = num(m.groupValues[1]) ?: return@let
            val pg = f.pieceGrams
            if (pg != null) return Triple(f, v * pg, true)
        }
        if (Regex("\\bq\\.?\\s?b\\b\\.?|quanto basta|a piacere", RegexOption.IGNORE_CASE).containsMatchIn(line)) return Triple(f, 5.0, true)
        return Triple(f, null, false)
    }

    fun parse(db: FoodDb, text: String): ImportedRecipe {
        val lines = text.lines().map { clean(it) }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ImportedRecipe()
        var section = 0 // 0 nessuna, 1 ingredienti, 2 passaggi
        val ings = LinkedHashMap<String, Pair<Food, Double>>()
        val unmatched = ArrayList<String>(); val check = ArrayList<String>()
        val steps = ArrayList<String>(); val buf = StringBuilder()
        fun flush() { if (buf.isNotBlank()) steps += buf.toString().trim(); buf.setLength(0) }
        var name = ""
        var minutes = 0
        for (l in lines) {
            when {
                ING_HEAD.matches(l) -> { section = 1; continue }
                STEP_HEAD.matches(l) -> { flush(); section = 2; continue }
            }
            if (TIME_LINE.containsMatchIn(l) && section != 2) {
                minutes += TIME.findAll(l).sumOf { it.groupValues[1].toInt() } + TIME_H.findAll(l).sumOf { it.groupValues[1].toInt() * 60 }
                continue
            }
            if (name.isEmpty() && section == 0 && l.length <= 80 && l.any { it.isLetter() } && !TIME_LINE.containsMatchIn(l)) { name = l.trimEnd(':', '.'); continue }
            val looksIngredient = section == 1 || (section == 0 && l.length <= 70 && (QTY.containsMatchIn(l) || COUNT.containsMatchIn(l)))
            if (looksIngredient) {
                val r = ingredient(db, l)
                if (r == null) { unmatched += l.replace(STEP_START, "").trim(); continue }
                val (f, g, ok) = r
                val food = f.toFood()
                if (!ok) check += f.name
                val add = g ?: (f.pieceGrams ?: 100.0)
                val prev = ings[f.id]
                ings[f.id] = food to ((prev?.second ?: 0.0) + add)
                continue
            }
            // passaggio: unisce le righe spezzate dall'OCR finché non inizia un nuovo punto elenco o la frase non è finita
            val startsNew = STEP_START.containsMatchIn(l)
            if (buf.isNotEmpty() && (startsNew || buf.last() in ".!?")) flush()
            if (buf.isNotEmpty()) buf.append(' ')
            buf.append(l.replace(STEP_START, ""))
            if (section == 0) section = 2
        }
        flush()
        val servings = lines.take(8).firstNotNullOfOrNull { SERV1.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: lines.take(8).firstNotNullOfOrNull { SERV2.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 1
        if (minutes == 0) minutes = 30
        return ImportedRecipe(
            name = name.ifBlank { "Ricetta importata" }, servings = servings.coerceIn(1, 30), minutes = minutes.coerceIn(1, 600),
            ingredients = ings.values.map { it.first to Math.round(it.second * 10) / 10.0 }, steps = steps,
            unmatched = unmatched, checkQty = check
        )
    }
}
