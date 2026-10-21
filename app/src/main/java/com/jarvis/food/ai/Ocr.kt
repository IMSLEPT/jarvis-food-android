package com.jarvis.food.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jarvis.food.engine.FoodDb
import kotlinx.coroutines.tasks.await

/** Lettura del testo sul telefono (ML Kit, modello incluso nell'app: funziona senza internet). */
object Ocr {
    suspend fun read(bitmap: Bitmap): String {
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            return client.process(InputImage.fromBitmap(bitmap, 0)).await().text
        } finally {
            client.close()
        }
    }

    private val QTY = Regex("(\\d+(?:[.,]\\d+)?)\\s*(kg|g|gr|grammi|l|lt|ml|cl)\\b", RegexOption.IGNORE_CASE)
    private val PIECES = Regex("\\b(?:x|conf\\.?\\s*)(\\d{1,2})\\b", RegexOption.IGNORE_CASE)

    /** Riconoscimento senza AI: confronta ogni riga dello scontrino con i nomi e sinonimi del database. */
    fun parseReceiptOffline(db: FoodDb, text: String): List<PantryGuess> {
        val out = LinkedHashMap<String, PantryGuess>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.length < 3 || line.count { it.isLetter() } < 3) continue
            val f = db.match(line) ?: continue
            var grams: Double? = QTY.find(line)?.let { m ->
                val v = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@let null
                when (m.groupValues[2].lowercase()) {
                    "kg", "l", "lt" -> v * 1000
                    "cl" -> v * 10
                    else -> v
                }
            }
            if (grams == null && f.id == "uova") grams = (PIECES.find(line)?.groupValues?.get(1)?.toDoubleOrNull() ?: 6.0) * (f.pieceGrams ?: 50.0)
            if (grams == null) grams = f.pack
            val prev = out[f.id]
            out[f.id] = PantryGuess(f.name, f.id, (prev?.grams ?: 0.0) + (grams ?: 0.0))
        }
        return out.values.toList()
    }
}
