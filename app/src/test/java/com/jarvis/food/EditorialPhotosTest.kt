package com.jarvis.food

import com.jarvis.food.ui.EditorialPhotos
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Each shipped image is tied to its exact unchanged recipe/step, not a similar dish. */
class EditorialPhotosTest {
    private val root = sequenceOf(File("."), File("..")).first { File(it, "tools/photography/approved.json").isFile }
    private val approved = JSONObject(File(root, "tools/photography/approved.json").readText())
    private val assets = File(root, "app/src/main/assets")
    private val recipes = JSONObject(File(assets, "recipes.json").readText()).getJSONArray("recipes")

    @Test fun approvedAssetsExistAndMatchReviewedBytes() {
        val keys = approved.keys().asSequence().toSet()
        assertEquals(keys, EditorialPhotos.keys)
        for (key in keys) {
            val record = approved.getJSONObject(key)
            val path = record.getString("asset")
            assertTrue(path.startsWith("photography/"))
            assertFalse(path.contains(".."))
            assertEquals(path, EditorialPhotos.asset(key, "legacy.webp"))
            val bytes = File(assets, path).readBytes()
            assertTrue(bytes.size > 10_000)
            assertEquals("RIFF", String(bytes.copyOfRange(0, 4)))
            assertEquals("WEBP", String(bytes.copyOfRange(8, 12)))
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(record.getString("sha256"), hash)
            when (record.getString("kind")) {
                "ai_illustration" -> assertEquals("Immagine illustrativa · AI", EditorialPhotos.label(key, null))
                "licensed_photo" -> {
                    val source = record.getJSONObject("source")
                    for (field in listOf("provider", "author", "pageUrl", "originalUrl", "license", "licenseUrl", "verifiedAt", "changes")) {
                        assertTrue("Missing $field: $key", source.getString(field).isNotBlank())
                    }
                    assertEquals("Foto: ${source.getString("author")} · ${source.getString("provider")}", EditorialPhotos.label(key, null))
                    if (record.has("sharedFrom")) {
                        val original = approved.getJSONObject(record.getString("sharedFrom"))
                        assertEquals(original.getString("kind"), record.getString("kind"))
                        assertEquals(original.getJSONObject("source").toString(), source.toString())
                    }
                }
                else -> fail("Unknown image origin: $key")
            }
        }
    }

    @Test fun eachNewDishHasItsCompleteMatchingProcedure() {
        val byId = (0 until recipes.length()).associate { recipes.getJSONObject(it).let { r -> r.getString("id") to r } }
        val heroes = EditorialPhotos.keys.filter { it in byId }
        assertTrue(heroes.isNotEmpty())
        for (id in heroes) {
            val recipe = byId.getValue(id)
            assertEquals(recipe.getString("name"), approved.getJSONObject(id).getString("subject"))
            val steps = recipe.getJSONArray("steps")
            for (index in 0 until steps.length()) {
                val key = "${id}_s$index"
                assertTrue("Missing photo: $key", EditorialPhotos.contains(key))
                val record = approved.getJSONObject(key)
                assertEquals(id, record.getString("recipeId"))
                assertEquals(index, record.getInt("stepIndex"))
                assertEquals(steps.getString(index), record.getString("subject"))
                assertNotNull(EditorialPhotos.step(id, index, null))
            }
        }
    }

    @Test fun uncoveredRecipesKeepTheirOriginalAsset() {
        assertEquals("recipes/original.webp", EditorialPhotos.asset("unreviewed", "recipes/original.webp"))
        assertEquals("recipes/step.webp", EditorialPhotos.step("unreviewed", 0, "recipes/step.webp"))
        assertNull(EditorialPhotos.asset("unreviewed", null))
        assertFalse(EditorialPhotos.contains("unreviewed"))
        assertNull(EditorialPhotos.label("unreviewed", null))
        assertNull(EditorialPhotos.label("unreviewed", "recipes/original.webp"))
    }

    @Test fun allViewsResolveTheSameReviewedOriginalAsset() {
        assertEquals(EditorialPhotos.asset("porridge_banana", null),
            EditorialPhotos.asset("other-view", "recipes/porridge_banana.webp"))
    }
}
