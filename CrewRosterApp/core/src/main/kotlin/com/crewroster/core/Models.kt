package com.crewroster.core

import kotlinx.serialization.Serializable

@Serializable
enum class Role { TL, ATL, BO }

@Serializable
data class Person(
    val id: String,
    val name: String,
    val role: Role
)

@Serializable
data class Vehicle(
    val id: String,
    val name: String
)

@Serializable
data class CarAllocation(
    val vehicleId: String,
    val driverId: String?,
    val crewIds: List<String>
)

@Serializable
data class HistoryDay(
    val date: String,
    val cars: List<CarAllocation>,
    val confirmedAt: String
)

@Serializable
data class PinInfo(
    val vehicleId: String? = null,
    val asDriver: Boolean = false
)

@Serializable
enum class WarningLevel { INFO, ERROR }

@Serializable
data class GenerationWarning(
    val level: WarningLevel,
    val message: String
)

data class RuleViolation(
    val rule: String,
    val carIndex: Int? = null,
    val message: String
)

data class GenerationResult(
    val cars: List<CarAllocation>?,
    val warnings: List<GenerationWarning> = emptyList(),
    val error: String? = null
)

data class DriverStats(
    val drives: Int,
    val lastDate: String?
)

data class TallyRow(
    val personId: String,
    val name: String,
    val drives: Int,
    val lastDate: String?,
    val daysSince: Int?
)

/**
 * Ephemeral, not-yet-confirmed state for "today" - mirrors what used to live in
 * localStorage's draft object in the web prototype. Cleared/replaced whenever the
 * calendar date rolls over to a new day.
 */
@Serializable
data class DraftState(
    val date: String,
    val absentIds: Set<String> = emptySet(),
    val pins: Map<String, PinInfo> = emptyMap(),
    val cars: List<CarAllocation>? = null,
    val warnings: List<GenerationWarning> = emptyList(),
    val savedAt: String? = null,
    val dirty: Boolean = false
)

@Serializable
data class AppData(
    val people: List<Person>,
    val vehicles: List<Vehicle>,
    val history: List<HistoryDay> = emptyList(),
    val draft: DraftState? = null
)

fun defaultPeople(): List<Person> {
    val people = mutableListOf(
        Person("tl", "Team Leader", Role.TL),
        Person("atl", "Assistant Team Leader", Role.ATL)
    )
    for (i in 1..10) people.add(Person("bo$i", "Build Officer $i", Role.BO))
    return people
}

fun defaultVehicles(): List<Vehicle> = listOf(
    Vehicle("v1", "Car 1"),
    Vehicle("v2", "Car 2"),
    Vehicle("v3", "Car 3")
)

fun defaultAppData(): AppData = AppData(people = defaultPeople(), vehicles = defaultVehicles())
