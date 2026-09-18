package com.crewroster.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `AppData round-trips through JSON including pins, sets, and nullable fields`() {
        val data = defaultAppData().copy(
            history = listOf(
                HistoryDay(
                    "2026-01-01",
                    listOf(
                        CarAllocation("v1", "bo1", listOf("bo1", "bo2", "tl")),
                        CarAllocation("v2", null, emptyList())
                    ),
                    "2026-01-01T08:00:00Z"
                )
            ),
            draft = DraftState(
                date = "2026-01-02",
                absentIds = setOf("bo3", "bo4"),
                pins = mapOf("bo5" to PinInfo(vehicleId = "v2", asDriver = true), "tl" to PinInfo(vehicleId = "v1")),
                cars = listOf(CarAllocation("v1", "bo5", listOf("bo5", "tl"))),
                warnings = listOf(GenerationWarning(WarningLevel.INFO, "test warning")),
                savedAt = null,
                dirty = true
            )
        )
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<AppData>(encoded)
        assertEquals(data, decoded)
    }
}
