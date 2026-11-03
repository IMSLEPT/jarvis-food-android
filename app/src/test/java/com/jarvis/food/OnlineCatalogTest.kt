package com.jarvis.food

import com.jarvis.food.ui.OnlineRecipeImages
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OnlineCatalogTest {
    private fun entry(id: String = "test_recipe", url: String = "${OnlineRecipeImages.ORIGIN}/images/test.a1.webp", sha: String = "a".repeat(64)) = JSONObject().put("recipeId",id).put("url",url).put("sha256",sha)
    private fun catalog(vararg rows: JSONObject) = JSONObject().put("schemaVersion",1).put("images",JSONArray(rows.toList())).toString()
    @Test fun validCatalog() { assertEquals("test_recipe",OnlineRecipeImages.parseCatalog(catalog(entry())).values.single().id) }
    @Test fun rejectWrongHostsAndUnsafeUrls() {
        listOf("http://jarvis-food-images.justale-c.chatgpt.site/images/a.webp", "https://evil.example/images/a.webp", "${OnlineRecipeImages.ORIGIN}/images/../../x.webp", "${OnlineRecipeImages.ORIGIN}/images/a.webp?token=x", "${OnlineRecipeImages.ORIGIN}:443/images/a.webp").forEach {
            assertThrows(IllegalArgumentException::class.java) { OnlineRecipeImages.parseCatalog(catalog(entry(url=it))) }
        }
    }
    @Test fun rejectDuplicateIdsAndBadHashes() {
        assertThrows(IllegalArgumentException::class.java) { OnlineRecipeImages.parseCatalog(catalog(entry(),entry())) }
        assertThrows(IllegalArgumentException::class.java) { OnlineRecipeImages.parseCatalog(catalog(entry(sha="bad"))) }
        assertThrows(IllegalArgumentException::class.java) { OnlineRecipeImages.parseCatalog(catalog(entry(id="../../oops"))) }
    }
}
