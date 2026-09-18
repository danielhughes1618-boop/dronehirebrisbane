package com.crewroster.core

import java.time.LocalDate
import kotlin.random.Random

object AllocationEngine {

    fun driverStats(history: List<HistoryDay>, personId: String): DriverStats {
        var drives = 0
        var lastDate: String? = null
        for (day in history) {
            for (car in day.cars) {
                if (car.driverId == personId) {
                    drives++
                    if (lastDate == null || day.date > lastDate!!) lastDate = day.date
                }
            }
        }
        return DriverStats(drives, lastDate)
    }

    private fun seatSizes(n: Int, k: Int, random: Random): IntArray {
        val base = n / k
        val rem = n % k
        val sizes = IntArray(k) { base }
        val idxs = (0 until k).shuffled(random)
        for (i in 0 until rem) sizes[idxs[i]] += 1
        return sizes
    }

    private fun currentCount(assign: Map<String, Int>, idx: Int): Int =
        assign.values.count { it == idx }

    private fun pickCarIndex(k: Int, assign: Map<String, Int>, sizes: IntArray, avoidIdx: Int?, random: Random): Int {
        var candidates = (0 until k).toList()
        if (avoidIdx != null) {
            val withoutAvoid = candidates.filter { it != avoidIdx }
            if (withoutAvoid.isNotEmpty()) candidates = withoutAvoid
        }
        val scored = candidates.map { it to (sizes[it] - currentCount(assign, it)) }
        val maxSlack = scored.maxOf { it.second }
        val best = scored.filter { it.second == maxSlack }.shuffled(random)
        return best[0].first
    }

    private fun isExactRepeatOfPrevious(cars: List<CarAllocation>, prevDay: HistoryDay?): Boolean {
        if (prevDay == null) return false
        val todaySets = cars.map { it.crewIds.toSet() }
        val prevSets = prevDay.cars.map { it.crewIds.toSet() }
        return todaySets.any { ts -> prevSets.any { ps -> ts == ps } }
    }

    private data class DriverCandidate(val bo: Person, val stats: DriverStats, val droveLastDay: Boolean)

    private val candidateComparator = Comparator<DriverCandidate> { a, b ->
        if (a.droveLastDay != b.droveLastDay) return@Comparator if (a.droveLastDay) 1 else -1
        if (a.stats.drives != b.stats.drives) return@Comparator a.stats.drives - b.stats.drives
        val at = a.stats.lastDate?.let { LocalDate.parse(it).toEpochDay() } ?: Long.MIN_VALUE
        val bt = b.stats.lastDate?.let { LocalDate.parse(it).toEpochDay() } ?: Long.MIN_VALUE
        at.compareTo(bt)
    }

    private fun <T> weightedPick(sorted: List<T>, random: Random): T {
        val kk = minOf(3, sorted.size)
        val weights = listOf(3, 2, 1).take(kk)
        val total = weights.sum()
        var r = random.nextDouble() * total
        for (i in 0 until kk) {
            if (r < weights[i]) return sorted[i]
            r -= weights[i]
        }
        return sorted[0]
    }

    /**
     * "Yesterday" means the most recently CONFIRMED day in history, not literal
     * calendar-yesterday - this keeps rotation fairness working sensibly across gaps
     * (weekends, days the app wasn't used) rather than penalizing based on the calendar.
     */
    fun generate(
        people: List<Person>,
        vehicles: List<Vehicle>,
        absentIds: Set<String>,
        pins: Map<String, PinInfo>,
        history: List<HistoryDay>,
        random: Random = Random.Default
    ): GenerationResult {
        val k = vehicles.size
        val present = people.filter { it.id !in absentIds }
        val presentBOs = present.filter { it.role == Role.BO }

        if (presentBOs.size < 3) {
            return GenerationResult(
                cars = null,
                error = "Only ${presentBOs.size} Build Officer${if (presentBOs.size == 1) "" else "s"} present today — " +
                    "at least 3 are needed to crew $k cars with a licensed driver each. No roster generated."
            )
        }

        val tl = present.firstOrNull { it.role == Role.TL }
        val atl = present.firstOrNull { it.role == Role.ATL }
        val personById = people.associateBy { it.id }
        val vehicleIndexById = vehicles.withIndex().associate { (i, v) -> v.id to i }
        val presentIds = present.map { it.id }.toSet()

        val sizes = seatSizes(present.size, k, random)
        val lockedAssign = mutableMapOf<String, Int>()
        val pinnedToCar = Array(k) { mutableListOf<String>() }
        val pinnedDriverForCar = arrayOfNulls<String>(k)

        for ((personId, pin) in pins) {
            if (personId !in presentIds) continue
            val idx = pin.vehicleId?.let { vehicleIndexById[it] } ?: continue
            pinnedToCar[idx].add(personId)
            lockedAssign[personId] = idx
            if (pin.asDriver) {
                val person = personById[personId]
                if (person != null && person.role == Role.BO) pinnedDriverForCar[idx] = personId
            }
        }

        for (i in 0 until k) {
            if (pinnedToCar[i].size > sizes[i]) sizes[i] = pinnedToCar[i].size
        }

        val warnings = mutableListOf<GenerationWarning>()

        var tlCar = tl?.let { lockedAssign[it.id] }
        var atlCar = atl?.let { lockedAssign[it.id] }
        if (tl != null && tlCar == null) {
            tlCar = pickCarIndex(k, lockedAssign, sizes, atlCar, random)
            lockedAssign[tl.id] = tlCar
        }
        if (atl != null && atlCar == null) {
            atlCar = pickCarIndex(k, lockedAssign, sizes, tlCar, random)
            lockedAssign[atl.id] = atlCar
        }
        if (tl != null && atl != null && lockedAssign[tl.id] == lockedAssign[atl.id]) {
            warnings.add(
                GenerationWarning(
                    WarningLevel.INFO,
                    "Team Leader and Assistant Team Leader ended up in the same car because of a pin."
                )
            )
        }

        val prevDay = history.maxByOrNull { it.date }
        val droveLastDay = prevDay?.cars?.mapNotNull { it.driverId }?.toSet() ?: emptySet()

        val driverAssign = mutableMapOf<Int, String>()
        val usedDrivers = mutableSetOf<String>()
        for (i in 0 until k) {
            val pinned = pinnedDriverForCar[i]
            if (pinned != null) {
                driverAssign[i] = pinned
                usedDrivers.add(pinned)
                lockedAssign[pinned] = i
            }
        }
        val remainingCarIdx = (0 until k).filter { it !in driverAssign }.shuffled(random)
        for (i in remainingCarIdx) {
            val candidates = presentBOs.filter { bo ->
                bo.id !in usedDrivers && (lockedAssign[bo.id] == null || lockedAssign[bo.id] == i)
            }.map { bo -> DriverCandidate(bo, driverStats(history, bo.id), bo.id in droveLastDay) }

            if (candidates.isEmpty()) {
                warnings.add(GenerationWarning(WarningLevel.ERROR, "No eligible Build Officer left to drive ${vehicles[i].name}."))
                continue
            }
            val sorted = candidates.sortedWith(candidateComparator)
            val chosen = weightedPick(sorted, random).bo
            driverAssign[i] = chosen.id
            usedDrivers.add(chosen.id)
            lockedAssign[chosen.id] = i
        }

        val freeCount = present.count { lockedAssign[it.id] == null }
        var attempts = 0
        val maxAttempts = 25
        var cars: List<CarAllocation>
        do {
            val trial = lockedAssign.toMutableMap()
            val remaining = present.filter { trial[it.id] == null }.shuffled(random)
            for (p in remaining) {
                trial[p.id] = pickCarIndex(k, trial, sizes, null, random)
            }
            cars = vehicles.mapIndexed { i, v ->
                val crewIds = trial.filterValues { it == i }.keys.toList()
                CarAllocation(v.id, driverAssign[i], crewIds)
            }
            attempts++
        } while (attempts < maxAttempts && freeCount > 0 && isExactRepeatOfPrevious(cars, prevDay))

        if (freeCount > 0 && attempts >= maxAttempts && isExactRepeatOfPrevious(cars, prevDay)) {
            warnings.add(
                GenerationWarning(
                    WarningLevel.INFO,
                    "Could not avoid repeating one of yesterday's exact crews given the current pins."
                )
            )
        }

        return GenerationResult(cars = cars, warnings = warnings)
    }

    fun validate(cars: List<CarAllocation>, people: List<Person>, vehicles: List<Vehicle>): List<RuleViolation> {
        val violations = mutableListOf<RuleViolation>()
        val personById = people.associateBy { it.id }
        val tl = people.firstOrNull { it.role == Role.TL }
        val atl = people.firstOrNull { it.role == Role.ATL }

        if (tl != null && atl != null) {
            val tlCarIndex = cars.indexOfFirst { tl.id in it.crewIds }
            val atlCarIndex = cars.indexOfFirst { atl.id in it.crewIds }
            if (tlCarIndex != -1 && tlCarIndex == atlCarIndex) {
                violations.add(RuleViolation("tl-atl-together", message = "${tl.name} and ${atl.name} are in the same car."))
            }
        }

        cars.forEachIndexed { i, c ->
            val vname = vehicles.getOrNull(i)?.name ?: "Car ${i + 1}"
            if (c.driverId == null) {
                violations.add(RuleViolation("no-driver", i, "$vname has no driver."))
            } else {
                val driver = personById[c.driverId]
                if (driver == null || driver.role != Role.BO) {
                    val roleDesc = when (driver?.role) {
                        Role.TL -> "the Team Leader"
                        Role.ATL -> "the Assistant Team Leader"
                        else -> "unknown"
                    }
                    violations.add(
                        RuleViolation(
                            "invalid-driver",
                            i,
                            "The driver of $vname is ${driver?.name ?: "unknown"}, who is $roleDesc — drivers must be Build Officers."
                        )
                    )
                }
            }
        }
        return violations
    }

    fun tally(people: List<Person>, history: List<HistoryDay>, today: String): List<TallyRow> {
        val bos = people.filter { it.role == Role.BO }
        val todayEpoch = LocalDate.parse(today).toEpochDay()
        val rows = bos.map { p ->
            val stats = driverStats(history, p.id)
            val daysSince = stats.lastDate?.let { (todayEpoch - LocalDate.parse(it).toEpochDay()).toInt() }
            TallyRow(p.id, p.name, stats.drives, stats.lastDate, daysSince)
        }
        return rows.sortedWith(compareBy<TallyRow> { it.drives }.thenByDescending { it.daysSince ?: Int.MAX_VALUE })
    }
}
