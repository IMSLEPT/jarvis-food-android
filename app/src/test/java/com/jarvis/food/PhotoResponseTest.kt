package com.jarvis.food

import com.jarvis.food.ai.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PhotoResponseTest {
    @Test fun nullableProviderContentIsRejectedInsteadOfDisplayedAsNull() {
        for (content in listOf("null", "\"null\"", "\"\"")) {
            val failure = runCatching { AiResponse.text("""{"choices":[{"message":{"content":$content}}]}""") }.exceptionOrNull()
            assertNotNull(failure)
            assertFalse(failure!!.message.orEmpty().contains("null"))
        }
    }
    @Test fun providersTextBlocksAndFencedOrArrayMealsAreAccepted() {
        assertEquals("ciao", AiResponse.text("""{"choices":[{"message":{"content":[{"type":"text","text":"ciao"}]}}]}"""))
        val item = """{"name":"Salmone","grams":150}"""
        assertEquals(1, AiResponse.meal("```json\n{\"items\":[$item]}\n```").getJSONArray("items").length())
        assertEquals(1, AiResponse.meal("[$item]").getJSONArray("items").length())
        assertEquals(0, AiResponse.meal("{\"items\":[]}").getJSONArray("items").length())
    }
    @Test fun fabricatedDefaultsCannotTurnMalformedResultsIntoFood() {
        for (value in listOf("null", "{}", "{\"items\":[null]}", "{\"items\":[{\"name\":\"null\",\"grams\":100}]}",
            "{\"items\":[{\"name\":\"Pizza\"}]}", "{\"items\":[{\"name\":\"Pizza\",\"grams\":-1}]}")) {
            assertTrue(value, runCatching { AiResponse.meal(value) }.isFailure)
        }
    }
    @Test fun weakOrAmbiguousOfflineScoresReturnNoInventedDish() {
        assertTrue(OfflineFoodLabels.select(listOf(AiLabel("quiche", .01f), AiLabel("waffle", .01f))).isEmpty())
        assertTrue(OfflineFoodLabels.select(listOf(AiLabel("pizza", .4f), AiLabel("pasta", .38f))).isEmpty())
        assertTrue(OfflineFoodLabels.select(listOf(AiLabel("unknown-label", .9f))).isEmpty())
        assertTrue(OfflineFoodLabels.select(listOf(AiLabel("pizza", Float.NaN))).isEmpty())
    }
    @Test fun usableOfflineLabelsAreReadableInItalian() {
        val labels = OfflineFoodLabels.select(listOf(AiLabel("grilled_salmon", .8f), AiLabel("pasta", .1f)))
        assertEquals(listOf("Salmone"), labels.map { it.label })
    }
}
