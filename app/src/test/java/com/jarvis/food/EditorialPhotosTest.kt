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
            assertEquals("ai_illustration", record.getString("kind"))
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
    }

    @Test fun allViewsResolveTheSameReviewedOriginalAsset() {
        assertEquals(EditorialPhotos.asset("porridge_banana", null),
            EditorialPhotos.asset("other-view", "recipes/porridge_banana.webp"))
    }
}
