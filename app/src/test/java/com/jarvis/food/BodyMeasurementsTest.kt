package com.jarvis.food

import com.jarvis.food.data.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BodyMeasurementsTest {
    private val day = LocalDate.of(2026, 10, 27)
    @Test fun updatesOneDateAndPartWithoutDiscardingHistory() {
        val older = Measurement(day.minusDays(7).toString(), "vita", 84.0)
        val arm = Measurement(day.toString(), "braccio", 32.0)
        val current = Measurement(day.toString(), "vita", 82.0)
        val saved = BodyMeasurements.save(listOf(current, arm, older), day, "vita", 81.5)
        assertEquals(3, saved.size)
        assertTrue(saved.contains(older)); assertTrue(saved.contains(arm))
        assertEquals(81.5, saved.single { it.type == "vita" && it.date == day.toString() }.value, 0.0)
        assertEquals(older, saved.first())
    }
    @Test fun retainsAllSevenKindsInExistingBackupFormat() {
        var history = emptyList<Measurement>()
        BodyMeasurements.guides.forEach { history = BodyMeasurements.save(history, day, it.key, if (it.unit == "%") 18.5 else 70.0) }
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val original = AppState(measurements = history)
        val restored = json.decodeFromString(AppState.serializer(), json.encodeToString(AppState.serializer(), original))
        assertEquals(history, restored.measurements)
        val legacy = json.decodeFromString(AppState.serializer(), """{"measurements":[{"date":"2025-01-01","type":"vita","value":83.0}]}""")
        assertEquals(83.0, legacy.measurements.single().value, 0.0)
        assertEquals("%", BodyMeasurements.guide("grasso").unit)
    }
    @Test fun invalidValuesCannotCorruptTheHistory() {
        for (key in BodyMeasurements.guides.map { it.key }) {
            for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 1000.0)) {
                assertFalse(BodyMeasurements.valid(key, value))
                assertTrue(runCatching { BodyMeasurements.save(emptyList(), day, key, value) }.isFailure)
            }
        }
        assertFalse(BodyMeasurements.valid("unknown", 80.0))
        assertFalse(BodyMeasurements.valid("grasso", 80.0))
        assertTrue(BodyMeasurements.valid("vita", 80.0))
    }
}
