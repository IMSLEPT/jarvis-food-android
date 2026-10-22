package com.jarvis.food

import com.jarvis.food.net.OpenFoodFacts
import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeVariantsTest {
    @Test fun upcA_prova_anche_ean13() {
        assertEquals(listOf("012345678905", "0012345678905"), OpenFoodFacts.variants("012345678905"))
    }

    @Test fun ean13_con_zero_prova_anche_upc() {
        assertEquals(listOf("0012345678905", "012345678905"), OpenFoodFacts.variants(" 0012345678905 "))
    }

    @Test fun ean13_normale_resta_uguale() {
        assertEquals(listOf("8001620000001"), OpenFoodFacts.variants("8001620000001"))
    }

    @Test fun vuoto_o_non_numerico() {
        assertEquals(emptyList<String>(), OpenFoodFacts.variants("abc"))
    }
}
