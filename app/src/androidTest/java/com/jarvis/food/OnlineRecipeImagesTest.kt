package com.jarvis.food

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.food.ui.EditorialPhotos
import com.jarvis.food.ui.OnlineRecipeImages
import com.jarvis.food.ui.RecipeImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OnlineRecipeImagesTest {
    @get:Rule val compose = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()
    @Test fun allTwentyDecodeAndDiskWorksOffline() = runBlocking {
        File(ctx.cacheDir,"online-recipe-images-v1").deleteRecursively()
        OnlineRecipeImages.resetForTest()
        val catalog = OnlineRecipeImages.catalog(ctx)
        assertEquals(20,catalog.size)
        catalog.keys.forEach { id ->
            assertNull("Must not replace a local cover: $id", EditorialPhotos.asset(id,null))
            val bitmap = OnlineRecipeImages.load(ctx,id)
            assertNotNull("Cannot decode $id on Android",bitmap)
            assertTrue(bitmap!!.width in 1..960)
        }
        assertEquals("One catalog and 20 files",21,OnlineRecipeImages.downloads.get())
        val downloaded = OnlineRecipeImages.downloads.get()
        OnlineRecipeImages.clearMemoryForTest(); OnlineRecipeImages.offlineForTest = true
        catalog.keys.forEach { assertNotNull(OnlineRecipeImages.load(ctx,it)) }
        assertEquals(downloaded,OnlineRecipeImages.downloads.get())
        assertNull(OnlineRecipeImages.load(ctx,"unknown_recipe"))
        OnlineRecipeImages.offlineForTest = false
    }
    @Test fun localPhotoTakesPrecedenceAndOnlineIsRendered() {
        OnlineRecipeImages.resetForTest()
        compose.setContent { RecipeImage("spiedini_pollo_couscous",null,null,Modifier.size(160.dp).testTag("photo")) }
        compose.waitUntil(25000) { OnlineRecipeImages.hasMemoryForTest() }
        val pixels=compose.onNodeWithTag("photo").captureToImage().toPixelMap()
        val colors=(1..8).flatMap { x -> (1..8).map { y -> pixels[x*pixels.width/10,y*pixels.height/10].value } }.toSet()
        assertTrue("Photo is rendered rather than a flat placeholder", colors.size>15)
    }
    @Test fun diskLimitEvictsOldestFiles() {
        val folder=File(ctx.cacheDir,"image-cache-limit-test").apply { mkdirs() }
        try {
            repeat(4) { i -> File(folder,"$i.webp").apply { writeBytes(ByteArray(30));setLastModified(1000L+i) } }
            OnlineRecipeImages.trim(folder,65)
            assertTrue(folder.listFiles()!!.sumOf { it.length() }<=65)
            assertFalse(File(folder,"0.webp").exists());assertTrue(File(folder,"3.webp").exists())
        } finally { folder.deleteRecursively() }
    }
    @Test fun bundledImageNeverDownloads() {
        OnlineRecipeImages.resetForTest()
        compose.setContent { RecipeImage("porridge_banana","recipes/porridge_banana.webp",null,Modifier.size(160.dp).testTag("local")) }
        compose.onNodeWithTag("local").assertExists()
        assertEquals(0,OnlineRecipeImages.downloads.get())
    }
}
