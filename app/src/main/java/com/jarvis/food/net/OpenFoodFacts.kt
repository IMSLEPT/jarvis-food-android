package com.jarvis.food.net

import com.jarvis.food.data.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Open Food Facts: database aperto e gratuito di prodotti confezionati (dati ODbL). */
object OpenFoodFacts {
    private const val UA = "JarvisFood/1.0 (Android; uso personale)"
    private const val FIELDS =
        "code,product_name,product_name_it,product_name_en,generic_name_it,generic_name,brands,nutriments," +
            "nutrition_data_per,serving_quantity,serving_size,quantity,categories_tags"

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", "application/json")
        c.connectTimeout = 12000
        c.readTimeout = 15000
        try {
            val code = c.responseCode
            if (code == 404) return "{\"status\":0}"
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Varianti dello stesso codice: UPC-A (12 cifre) e EAN-13 con zero iniziale sono lo stesso prodotto. */
    internal fun variants(raw: String): List<String> {
        val c = raw.trim().filter { it.isDigit() }
        if (c.isEmpty()) return emptyList()
        val out = linkedSetOf(c)
        if (c.length == 12) out += "0$c"
        if (c.length == 13 && c.startsWith("0")) out += c.drop(1)
        if (c.length == 8) out += c.padStart(13, '0')
        return out.toList()
    }

    suspend fun byBarcode(code: String): Food? = withContext(Dispatchers.IO) {
        for (v in variants(code)) {
            val txt = get("https://world.openfoodfacts.org/api/v2/product/$v.json?fields=$FIELDS")
            val root = JSONObject(txt)
            if (root.optInt("status", 0) != 1) continue
            val food = parse(root.optJSONObject("product") ?: continue, v)
            if (food != null) return@withContext food
        }
        null
    }

    suspend fun search(query: String): List<Food> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        // 1) Motore di ricerca nuovo (veloce); 2) API classica come riserva.
        val first = runCatching {
            val txt = get("https://search.openfoodfacts.org/search?q=$q&page_size=25&langs=it,en&fields=$FIELDS")
            val hits = JSONObject(txt).optJSONArray("hits")
            (0 until (hits?.length() ?: 0)).mapNotNull { parse(hits!!.getJSONObject(it), null) }
        }.getOrNull()
        if (!first.isNullOrEmpty()) return@withContext first
        val txt = get(
            "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$q&search_simple=1&action=process" +
                    "&json=1&page_size=25&fields=$FIELDS"
        )
        val arr = JSONObject(txt).optJSONArray("products")
        (0 until (arr?.length() ?: 0)).mapNotNull { parse(arr!!.getJSONObject(it), null) }
    }

    private fun JSONObject.num(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toDouble()
            is String -> v.replace(",", ".").trim().toDoubleOrNull()
            else -> null
        }
    }

    private fun tags(p: JSONObject): List<String> {
        val a = p.optJSONArray("categories_tags") ?: return emptyList()
        return (0 until a.length()).map { a.optString(it) }
    }

    /** Un liquido (acqua, bevande): le quantità si contano in ml. */
    private fun isLiquid(p: JSONObject): Boolean {
        val t = tags(p)
        if (t.any { it.contains("beverage") || it.endsWith("waters") || it.contains("drinks") || it.contains("juices") || it.endsWith(":milks") }) return true
        val qty = p.optString("quantity", "").lowercase().replace(" ", "")
        return Regex("\\d(ml|cl|dl|l)\\b").containsMatchIn(qty) || Regex("\\d(ml|cl|dl|l)$").containsMatchIn(qty)
    }

    private fun isWater(p: JSONObject, name: String): Boolean {
        val t = tags(p)
        val byTag = t.any { it.endsWith(":waters") || it.endsWith(":spring-waters") || it.endsWith(":mineral-waters") ||
            it.endsWith(":natural-mineral-waters") || it.endsWith(":still-waters") || it.endsWith(":sparkling-waters") || it.endsWith(":carbonated-waters") }
        val byName = Regex("\\b(acqua|water|eau|agua)\\b", RegexOption.IGNORE_CASE).containsMatchIn(name) &&
            t.none { it.contains("flavoured") || it.contains("soda") || it.contains("juice") }
        return byTag || byName
    }

    /**
     * Valore per 100 g/ml esattamente come sull'etichetta: se la tabella è "per porzione" parto dal valore della porzione
     * (non arrotondato) invece del `_100g` ricalcolato da Open Food Facts; altrimenti uso `_100g` o il valore grezzo.
     */
    private fun JSONObject.per100(n: JSONObject, key: String, perServing: Boolean, serving: Double?): Double? {
        if (perServing && serving != null) n.num("${key}_serving")?.let { return it / serving * 100.0 }
        n.num("${key}_100g")?.let { return it }
        if (!perServing) {
            val v = n.num("${key}_value")
            val unit = n.optString("${key}_unit", "g").lowercase()
            if (v != null) return when (unit) { "mg" -> v / 1000.0; "µg", "mcg", "ug" -> v / 1_000_000.0; else -> v }
        }
        return null
    }

    private fun parse(p: JSONObject, fallbackCode: String?): Food? {
        val n = p.optJSONObject("nutriments") ?: JSONObject()
        val brandsRaw = p.opt("brands")
        val brand = when (brandsRaw) {
            is String -> brandsRaw.split(",").first().trim()
            is org.json.JSONArray -> if (brandsRaw.length() > 0) brandsRaw.optString(0) else ""
            else -> ""
        }
        val name = listOf("product_name_it", "product_name", "generic_name_it", "product_name_en", "generic_name")
            .map { p.optString(it, "").trim() }.firstOrNull { it.isNotEmpty() }
            ?: brand.takeIf { it.isNotBlank() } ?: return null
        val code = p.optString("code", fallbackCode ?: "").ifBlank { fallbackCode ?: name.hashCode().toString() }
        val serving = p.num("serving_quantity")?.takeIf { it > 0 && it < 2000 }
        val perServing = p.optString("nutrition_data_per", "100g") == "serving" && serving != null

        val protein = p.per100(n, "proteins", perServing, serving)
        val carbs = p.per100(n, "carbohydrates", perServing, serving)
        val fat = p.per100(n, "fat", perServing, serving)
        val water = isWater(p, name)

        // Energia: kcal dell'etichetta; se manca, kJ/4,184; se mancano entrambe, dai macro (Atwater); l'acqua senza dati vale 0.
        var kcal: Double? = null
        if (perServing && serving != null) n.num("energy-kcal_serving")?.let { kcal = it / serving * 100.0 }
        if (kcal == null) kcal = n.num("energy-kcal_100g")
        if (kcal == null && !perServing) kcal = n.num("energy-kcal_value")?.takeIf { n.optString("energy-kcal_unit", "kcal").lowercase() == "kcal" }
        if (kcal == null) {
            val kj = if (perServing && serving != null) n.num("energy-kj_serving")?.let { it / serving * 100.0 } else null
            kcal = (kj ?: n.num("energy-kj_100g") ?: n.num("energy_100g"))?.let { it / 4.184 }
        }
        if (kcal == null && (protein != null || carbs != null || fat != null)) {
            kcal = (protein ?: 0.0) * 4 + (carbs ?: 0.0) * 4 + (fat ?: 0.0) * 9
        }
        if (kcal == null) {
            if (water) kcal = 0.0 else return null
        }

        val sodium = p.per100(n, "sodium", perServing, serving)
        val salt = p.per100(n, "salt", perServing, serving) ?: sodium?.let { it * 2.5 }
        return Food(
            id = "off:$code",
            name = name,
            brand = brand,
            kcal = kcal!!,
            protein = protein ?: 0.0,
            carbs = carbs ?: 0.0,
            fat = fat ?: 0.0,
            fiber = p.per100(n, "fiber", perServing, serving),
            sugar = p.per100(n, "sugars", perServing, serving),
            satFat = p.per100(n, "saturated-fat", perServing, serving),
            salt = salt,
            servingName = if (serving != null) "porzione (${p.optString("serving_size", "").ifBlank { "${serving.toInt()} g" }})" else null,
            servingGrams = serving,
            barcode = code,
            source = "off",
            liquid = isLiquid(p) || water,
            category = if (water) "Acqua" else "Prodotto"
        )
    }
}
