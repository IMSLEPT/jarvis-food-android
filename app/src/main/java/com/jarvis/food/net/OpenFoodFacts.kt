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
        "code,product_name,product_name_it,generic_name_it,brands,nutriments,serving_quantity,serving_size,quantity"

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", "application/json")
        c.connectTimeout = 12000
        c.readTimeout = 15000
        try {
            val code = c.responseCode
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    suspend fun byBarcode(code: String): Food? = withContext(Dispatchers.IO) {
        val txt = get("https://world.openfoodfacts.org/api/v2/product/${code.trim()}.json?fields=$FIELDS")
        val root = JSONObject(txt)
        if (root.optInt("status", 0) != 1) return@withContext null
        parse(root.optJSONObject("product") ?: return@withContext null, code)
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
        val v = opt(key)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.replace(",", ".").toDoubleOrNull()
            else -> null
        }
    }

    private fun parse(p: JSONObject, fallbackCode: String?): Food? {
        val n = p.optJSONObject("nutriments") ?: return null
        val kcal = n.num("energy-kcal_100g") ?: n.num("energy_100g")?.let { it / 4.184 } ?: return null
        val name = listOf("product_name_it", "product_name", "generic_name_it")
            .map { p.optString(it, "").trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        val brandsRaw = p.opt("brands")
        val brand = when (brandsRaw) {
            is String -> brandsRaw.split(",").first().trim()
            is org.json.JSONArray -> if (brandsRaw.length() > 0) brandsRaw.optString(0) else ""
            else -> ""
        }
        val code = p.optString("code", fallbackCode ?: "").ifBlank { fallbackCode ?: name.hashCode().toString() }
        val serving = p.num("serving_quantity")?.takeIf { it > 0 && it < 2000 }
        return Food(
            id = "off:$code",
            name = name,
            brand = brand,
            kcal = kcal,
            protein = n.num("proteins_100g") ?: 0.0,
            carbs = n.num("carbohydrates_100g") ?: 0.0,
            fat = n.num("fat_100g") ?: 0.0,
            fiber = n.num("fiber_100g"),
            sugar = n.num("sugars_100g"),
            satFat = n.num("saturated-fat_100g"),
            salt = n.num("salt_100g"),
            servingName = if (serving != null) "porzione (${p.optString("serving_size", "").ifBlank { "${serving.toInt()} g" }})" else null,
            servingGrams = serving,
            barcode = code,
            source = "off",
            category = "Prodotto"
        )
    }
}
