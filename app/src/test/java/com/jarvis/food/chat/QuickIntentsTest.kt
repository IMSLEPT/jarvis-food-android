package com.jarvis.food.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickIntentsTest {
    private fun first(t: String) = QuickIntents.parse(t)?.calls?.firstOrNull()

    @Test fun water() {
        assertEquals(500, first("Ho bevuto 500 ml d'acqua")?.args?.getInt("ml"))
        assertEquals(1000, first("bevo un litro d'acqua")?.args?.getInt("ml"))
        assertEquals(500, first("aggiungi 2 bicchieri d'acqua")?.args?.getInt("ml"))
    }

    @Test fun food() {
        val c = first("Ho mangiato 150 g di pollo a pranzo")!!
        assertEquals("log_food", c.name); assertEquals("pollo", c.args.getString("food"))
        assertEquals(150.0, c.args.getDouble("grams"), 0.0); assertEquals("pranzo", c.args.getString("meal"))
    }

    @Test fun weightAndFast() {
        assertEquals(79.4, first("oggi peso 79,4")!!.args.getDouble("kg"), 0.0)
        assertEquals("16:8", first("avvia il digiuno")!!.args.getString("plan"))
        assertEquals("stop_fast", first("termina il digiuno")!!.name)
    }

    @Test fun leavesAmbiguousToModel() {
        assertNull(first("Ho mangiato 100 g di pasta e 50 g di pane"))
        assertNull(first("Ieri ho bevuto 500 ml d'acqua"))
        assertNull(first("Quanta acqua devo bere?"))
        assertNull(first("Pesavo 80, oggi sono 79,4"))
    }
}
