package com.jarvis.food.ai

import org.json.JSONArray
import org.json.JSONObject

/** Handles nullable content, text parts, fenced JSON and providers returning a bare item array. */
internal object AiResponse {
    fun text(envelope: String): String {
        val message = JSONObject(envelope).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            ?: throw IllegalArgumentException("L’AI non ha restituito una risposta utilizzabile. Riprova.")
        val content = message.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> (0 until content.length()).mapNotNull { i ->
                content.optJSONObject(i)?.takeIf { it.optString("type") == "text" }?.optString("text")
            }.joinToString("\n")
            else -> ""
        }.trim()
        if (text.isBlank() || text.equals("null", true)) throw IllegalArgumentException("L’AI non ha restituito dati leggibili. Riprova con una foto più chiara o cerca gli alimenti.")
        return text
    }

    fun meal(answer: String): JSONObject {
        val clean = answer.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = try {
            if (clean.startsWith("[")) JSONObject().put("items", JSONArray(clean))
            else {
                val start = clean.indexOf('{'); val end = clean.lastIndexOf('}')
                require(start >= 0 && end > start)
                JSONObject(clean.substring(start, end + 1))
            }
        } catch (_: Exception) { throw IllegalArgumentException("L’AI ha restituito dati incompleti. Riprova o cerca gli alimenti.") }
        val items = parsed.optJSONArray("items") ?: throw IllegalArgumentException("L’AI non ha restituito alimenti leggibili. Riprova.")
        require(items.length() <= 30) { "Risposta AI troppo estesa. Fotografa un piatto alla volta." }
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: throw IllegalArgumentException("Dati dell’alimento incompleti. Riprova.")
            val name = item.optString("name").trim()
            val grams = item.optDouble("grams", Double.NaN)
            require(name.isNotBlank() && !name.equals("null", true) && name.length <= 160 && grams.isFinite() && grams > 0 && grams <= 1500) {
                "L’AI non ha stimato correttamente gli alimenti e le quantità. Riprova o inseriscili manualmente."
            }
            for (key in listOf("kcal", "protein", "carbs", "fat")) {
                if (item.has(key)) {
                    val value = item.optDouble(key, Double.NaN)
                    val maximum = if (key == "kcal") grams * 9.5 else grams
                    require(value.isFinite() && value >= 0 && value <= maximum) { "L’AI ha restituito valori nutrizionali incoerenti. Riprova o inserisci gli alimenti manualmente." }
                }
            }
        }
        return parsed
    }
}
