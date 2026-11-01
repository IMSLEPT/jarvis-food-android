package com.jarvis.food

import com.jarvis.food.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class LegacyBackupCompatibilityTest {
    private fun withLegacy(state: AppState, legacy: JsonElement): String {
        val obj=Store.json.encodeToJsonElement(AppState.serializer(),state).jsonObject
        return JsonObject(obj + ("family" to legacy)).toString()
    }
    @Test fun removingFamilyKeepsProfileDiaryWeightsAndShoppingFromExistingBackups() {
        val state=AppState(onboarded=true,profile=Profile(name="Backup test"),weights=listOf(WeightEntry("2026-11-01",71.0)), shopping=listOf(ShoppingItem("keep-shop",name="Latte",manual=true)),
            diary=listOf(DiaryEntry("one","2026-11-01",1,Food("f","Test",kcal=100.0,protein=10.0,carbs=10.0,fat=1.0),150.0,0L)))
        val restored=Store.json.decodeFromString(AppState.serializer(),withLegacy(state,buildJsonObject {
            put("code","legacy-unused");put("memberId","old-member");put("name","Previous link");put("span",3)
        }))
        assertEquals(state,restored)
        assertFalse(Store.json.encodeToString(AppState.serializer(),restored).contains("\"family\""))
    }
    @Test fun obsoleteFamilyPayloadsCannotBreakOtherwiseValidPersonalData() {
        val state=AppState(profile=Profile(name="Keep personal data"),weights=listOf(WeightEntry("2026-11-01",70.0)))
        for (legacy in listOf(JsonNull,JsonPrimitive("obsolete"),buildJsonObject { put("unknownShape",true) })) {
            assertEquals(state,Store.json.decodeFromString(AppState.serializer(),withLegacy(state,legacy)))
        }
    }
}
