package com.crewroster.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class AllocationEngineTest {

    private fun mkPeople(): List<Person> {
        val people = mutableListOf(
            Person("tl", "Team Leader", Role.TL),
            Person("atl", "Assistant Team Leader", Role.ATL)
        )
        for (i in 1..10) people.add(Person("bo$i", "Build Officer $i", Role.BO))
        return people
    }

    private fun mkVehicles(): List<Vehicle> = listOf(
        Vehicle("v1", "Car 1"),
        Vehicle("v2", "Car 2"),
        Vehicle("v3", "Car 3")
    )

    private fun dateFor(dayIndex: Int, start: LocalDate = LocalDate.of(2020, 1, 1)): String =
        start.plusDays(dayIndex.toLong()).toString()

    private fun checkInvariants(
        result: GenerationResult,
        presentIds: List<String>,
        people: List<Person>,
        vehicles: List<Vehicle>,
        label: String,
        allowSizeSlack: Boolean = false
    ): List<CarAllocation> {
        assertNull("$label: unexpected error ${result.error}", result.error)
        val cars = result.cars!!
        val allCrew = cars.flatMap { it.crewIds }
        assertEquals("$label: duplicate crew assignment", allCrew.size, allCrew.toSet().size)
        assertEquals("$label: crew count mismatch", presentIds.size, allCrew.size)
        presentIds.forEach { id -> assertTrue("$label: present person $id missing", allCrew.contains(id)) }

        val sizes = cars.map { it.crewIds.size }
        if (!allowSizeSlack) {
            assertTrue("$label: car sizes too uneven: $sizes", (sizes.max() - sizes.min()) <= 1)
        }

        val personById = people.associateBy { it.id }
        val tl = people.firstOrNull { it.role == Role.TL }
        val atl = people.firstOrNull { it.role == Role.ATL }
        if (tl != null && atl != null && presentIds.contains(tl.id) && presentIds.contains(atl.id)) {
            val tlCarIdx = cars.indexOfFirst { tl.id in it.crewIds }
            val atlCarIdx = cars.indexOfFirst { atl.id in it.crewIds }
            if (tlCarIdx == atlCarIdx) {
                assertTrue(
                    "$label: TL/ATL in same car with no warning",
                    result.warnings.any { it.message.contains("same car") }
                )
            }
        }

        cars.forEachIndexed { i, c ->
            if (c.crewIds.isEmpty()) return@forEachIndexed
            assertNotNull("$label: car $i has no driver (crew ${c.crewIds.size})", c.driverId)
            val driver = personById[c.driverId]
            assertTrue("$label: car $i driver is not a BO: ${driver?.role}", driver != null && driver.role == Role.BO)
            assertTrue("$label: car $i driver not listed in crew", c.crewIds.contains(c.driverId))
        }

        val violations = AllocationEngine.validate(cars, people, vehicles)
        assertTrue("$label: validateCars found violations on freshly generated roster: $violations", violations.isEmpty())
        return cars
    }

    @Test
    fun `fewer than 3 BOs present blocks generation`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        for (n in 0..2) {
            val absentBoIds = people.filter { it.role == Role.BO }.drop(n).map { it.id }.toSet()
            val result = AllocationEngine.generate(people, vehicles, absentBoIds, emptyMap(), emptyList())
            assertNotNull("expected error when only $n BOs present", result.error)
        }
    }

    @Test
    fun `exactly 3 BOs present succeeds`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentBoIds = people.filter { it.role == Role.BO }.drop(3).map { it.id }.toSet()
        val result = AllocationEngine.generate(people, vehicles, absentBoIds, emptyMap(), emptyList())
        assertNull(result.error)
    }

    @Test
    fun `full attendance splits evenly and satisfies all rules`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val presentIds = people.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, emptySet(), emptyMap(), emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "full-attendance-12")
        assertEquals(listOf(4, 4, 4), cars.map { it.crewIds.size }.sortedDescending())
    }

    @Test
    fun `11 present splits 4-4-3`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentIds = setOf("bo1")
        val presentIds = people.filter { it.id !in absentIds }.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "present-11")
        assertEquals(listOf(4, 4, 3), cars.map { it.crewIds.size }.sortedDescending())
    }

    @Test
    fun `10 present splits 4-3-3`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentIds = setOf("bo1", "bo2")
        val presentIds = people.filter { it.id !in absentIds }.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "present-10")
        assertEquals(listOf(4, 3, 3), cars.map { it.crewIds.size }.sortedDescending())
    }

    @Test
    fun `TL absent still generates fine`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentIds = setOf("tl")
        val presentIds = people.filter { it.id !in absentIds }.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList())
        checkInvariants(result, presentIds, people, vehicles, "tl-absent")
    }

    @Test
    fun `TL and ATL both absent still generates fine`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentIds = setOf("tl", "atl")
        val presentIds = people.filter { it.id !in absentIds }.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList())
        checkInvariants(result, presentIds, people, vehicles, "tl-atl-absent")
    }

    @Test
    fun `exactly 3 BOs present each drives their own car`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val absentIds = people.filter { it.role != Role.BO || it.id !in setOf("bo1", "bo2", "bo3") }.map { it.id }.toSet()
        val presentIds = people.filter { it.id !in absentIds }.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "exactly-3-bos", allowSizeSlack = true)
        cars.forEach { assertEquals(1, it.crewIds.size) }
    }

    @Test
    fun `pinned driver is honored`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val pins = mapOf("bo5" to PinInfo(vehicleId = "v2", asDriver = true))
        val presentIds = people.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, emptySet(), pins, emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "pin-driver")
        val v2 = cars.first { it.vehicleId == "v2" }
        assertEquals("bo5", v2.driverId)
        assertTrue(v2.crewIds.contains("bo5"))
    }

    @Test
    fun `pinning TL and ATL to the same car warns but is honored`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val pins = mapOf("tl" to PinInfo(vehicleId = "v1"), "atl" to PinInfo(vehicleId = "v1"))
        val result = AllocationEngine.generate(people, vehicles, emptySet(), pins, emptyList())
        assertNull(result.error)
        val v1 = result.cars!!.first { it.vehicleId == "v1" }
        assertTrue(v1.crewIds.contains("tl") && v1.crewIds.contains("atl"))
        assertTrue(result.warnings.any { it.message.contains("same car") })
    }

    @Test
    fun `pinned crew member is placed in the pinned car`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val pins = mapOf("bo1" to PinInfo(vehicleId = "v3"))
        val presentIds = people.map { it.id }
        val result = AllocationEngine.generate(people, vehicles, emptySet(), pins, emptyList())
        val cars = checkInvariants(result, presentIds, people, vehicles, "pin-crew-only")
        val v3 = cars.first { it.vehicleId == "v3" }
        assertTrue(v3.crewIds.contains("bo1"))
    }

    @Test
    fun `driver rotation converges to near-perfect fairness over long run`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val random = Random(42)
        var history = listOf<HistoryDay>()
        val drivesCount = mutableMapOf<String, Int>()
        people.filter { it.role == Role.BO }.forEach { drivesCount[it.id] = 0 }
        val days = 1000
        for (d in 0 until days) {
            val result = AllocationEngine.generate(people, vehicles, emptySet(), emptyMap(), history, random)
            assertNull("unexpected error on day $d", result.error)
            result.cars!!.forEach { c -> c.driverId?.let { drivesCount[it] = (drivesCount[it] ?: 0) + 1 } }
            history = history + HistoryDay(dateFor(d), result.cars, dateFor(d))
        }
        val counts = drivesCount.values
        val spread = counts.max() - counts.min()
        assertTrue("driver rotation too unfair after $days days: spread=$spread, counts=$drivesCount", spread <= 10)
    }

    @Test
    fun `drove-yesterday repeats are rare when alternatives exist`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val random = Random(7)
        var history = listOf<HistoryDay>()
        var repeatCount = 0
        val days = 100
        for (d in 0 until days) {
            val result = AllocationEngine.generate(people, vehicles, emptySet(), emptyMap(), history, random)
            val prevDay = history.maxByOrNull { it.date }
            if (prevDay != null) {
                val prevDrivers = prevDay.cars.mapNotNull { it.driverId }.toSet()
                result.cars!!.mapNotNull { it.driverId }.forEach { if (it in prevDrivers) repeatCount++ }
            }
            history = history + HistoryDay(dateFor(d), result.cars!!, dateFor(d))
        }
        assertTrue("drove-yesterday repeats too frequent: $repeatCount/$days", repeatCount <= 5)
    }

    @Test
    fun `exact crew repeats are rare when alternatives exist`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val random = Random(99)
        var history = listOf<HistoryDay>()
        var exactRepeatCount = 0
        val days = 60
        for (d in 0 until days) {
            val result = AllocationEngine.generate(people, vehicles, emptySet(), emptyMap(), history, random)
            val prevDay = history.maxByOrNull { it.date }
            if (prevDay != null) {
                val prevSets = prevDay.cars.map { it.crewIds.toSet() }
                val todaySets = result.cars!!.map { it.crewIds.toSet() }
                if (todaySets.any { ts -> prevSets.any { ps -> ts == ps } }) exactRepeatCount++
            }
            history = history + HistoryDay(dateFor(d), result.cars!!, dateFor(d))
        }
        assertTrue("exact crew repeats too frequent: $exactRepeatCount/$days", exactRepeatCount <= 5)
    }

    @Test
    fun `random attendance fuzzing always satisfies invariants`() {
        val people = mkPeople()
        val vehicles = mkVehicles()
        val random = Random(1234)
        repeat(500) { trial ->
            val absentIds = people.filter { random.nextDouble() < 0.25 }.map { it.id }.toSet()
            val presentIds = people.filter { it.id !in absentIds }.map { it.id }
            val presentBoCount = presentIds.count { id -> people.first { it.id == id }.role == Role.BO }
            val result = AllocationEngine.generate(people, vehicles, absentIds, emptyMap(), emptyList(), random)
            if (presentBoCount < 3) {
                assertNotNull("trial $trial: expected error with only $presentBoCount BOs present", result.error)
            } else {
                checkInvariants(result, presentIds, people, vehicles, "fuzz-trial-$trial", allowSizeSlack = true)
            }
        }
    }

    @Test
    fun `tally sorts fewest drives first and computes days since`() {
        val people = mkPeople()
        val history = listOf(
            HistoryDay(
                "2024-01-01",
                listOf(
                    CarAllocation("v1", "bo1", listOf("bo1", "bo2")),
                    CarAllocation("v2", "bo3", listOf("bo3", "bo4")),
                    CarAllocation("v3", null, listOf("bo5"))
                ),
                "2024-01-01T08:00:00Z"
            )
        )
        val rows = AllocationEngine.tally(people, history, "2024-01-05")
        assertEquals(10, rows.size)
        assertTrue("fewest-drives-first ordering: $rows", rows.take(8).all { it.drives == 0 })
        assertTrue("drivers should sort after never-driven: $rows", rows.drop(8).all { it.drives == 1 })
        val bo1Row = rows.first { it.personId == "bo1" }
        assertEquals(1, bo1Row.drives)
        assertEquals(4, bo1Row.daysSince)
        assertEquals("2024-01-01", bo1Row.lastDate)
    }
}
