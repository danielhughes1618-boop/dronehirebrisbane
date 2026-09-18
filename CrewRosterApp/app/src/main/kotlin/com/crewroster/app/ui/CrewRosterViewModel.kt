package com.crewroster.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crewroster.app.data.AppDataStore
import com.crewroster.app.util.formatDateShort
import com.crewroster.app.util.todayIso
import com.crewroster.core.AllocationEngine
import com.crewroster.core.AppData
import com.crewroster.core.CarAllocation
import com.crewroster.core.DraftState
import com.crewroster.core.HistoryDay
import com.crewroster.core.PinInfo
import com.crewroster.core.Role
import com.crewroster.core.Vehicle
import com.crewroster.core.defaultAppData
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrewRosterViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppDataStore(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        // Load once at startup; from then on in-memory _uiState is the single source of
        // truth for this session (DataStore is a write-only persistence sink here, not a
        // live sync source) - this app has exactly one writer, so re-collecting every
        // emission back into _uiState only created a feedback loop where our own writes
        // (e.g. editDay's historical draft, or a wipe) got read back and reapplied,
        // sometimes stomping fresher in-memory state that was written after them.
        viewModelScope.launch {
            val initial = store.data.first()
            val normalized = ensureTodayDraft(initial)
            _uiState.update { it.copy(appData = normalized, loading = false) }
            if (normalized != initial) persist(normalized)
        }
    }

    private fun ensureTodayDraft(data: AppData): AppData {
        val today = todayIso()
        return if (data.draft == null || data.draft.date != today) {
            data.copy(draft = DraftState(date = today))
        } else data
    }

    private fun persist(data: AppData) {
        viewModelScope.launch { store.save(data) }
    }

    private fun updateData(transform: (AppData) -> AppData) {
        val current = ensureTodayDraft(_uiState.value.appData)
        val updated = transform(current)
        _uiState.update { it.copy(appData = updated) }
        persist(updated)
    }

    private fun updateDraft(transform: (DraftState) -> DraftState) {
        updateData { data ->
            val draft = data.draft ?: DraftState(date = todayIso())
            data.copy(draft = transform(draft))
        }
    }

    private fun emit(event: UiEvent) {
        _events.tryEmit(event)
    }

    // ---- Home screen actions ----

    fun toggleAbsent(personId: String) {
        updateDraft { draft ->
            val isCurrentlyAbsent = personId in draft.absentIds
            val newAbsentIds = if (isCurrentlyAbsent) draft.absentIds - personId else draft.absentIds + personId
            val newPins = if (!isCurrentlyAbsent) draft.pins - personId else draft.pins
            draft.copy(absentIds = newAbsentIds, pins = newPins)
        }
    }

    fun setPinCar(personId: String, vehicleId: String?) {
        updateDraft { draft ->
            if (vehicleId == null) {
                draft.copy(pins = draft.pins - personId)
            } else {
                val existing = draft.pins[personId] ?: PinInfo()
                draft.copy(pins = draft.pins + (personId to existing.copy(vehicleId = vehicleId)))
            }
        }
    }

    fun setPinDriver(personId: String, asDriver: Boolean) {
        updateDraft { draft ->
            val existing = draft.pins[personId] ?: PinInfo()
            draft.copy(pins = draft.pins + (personId to existing.copy(asDriver = asDriver)))
        }
    }

    fun generate() {
        val data = ensureTodayDraft(_uiState.value.appData)
        val draft = data.draft ?: return
        val result = AllocationEngine.generate(data.people, data.vehicles, draft.absentIds, draft.pins, data.history)
        if (result.error != null) {
            emit(UiEvent.Toast(result.error))
            return
        }
        updateDraft { it.copy(cars = result.cars, warnings = result.warnings, dirty = true) }
        _uiState.update { it.copy(selectedPersonId = null, driverPickerCarIndex = null) }
    }

    fun regenerate() = generate()

    /**
     * Clears any in-progress swap-selection or open driver picker. Called on every bottom-nav
     * tap (mirrors the JS reference, which resets both on every nav click) so a pending
     * selection from a previous visit to Result can never carry over and silently complete an
     * unintended swap on a different day.
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedPersonId = null, driverPickerCarIndex = null) }
    }

    // ---- Result screen actions ----

    fun onPersonTap(personId: String) {
        val selected = _uiState.value.selectedPersonId
        when {
            selected == null -> _uiState.update { it.copy(selectedPersonId = personId) }
            selected == personId -> _uiState.update { it.copy(selectedPersonId = null) }
            else -> {
                swapPeople(selected, personId)
                _uiState.update { it.copy(selectedPersonId = null) }
            }
        }
    }

    private fun swapPeople(idA: String, idB: String) {
        updateDraft { draft ->
            val cars = draft.cars ?: return@updateDraft draft
            val carAIndex = cars.indexOfFirst { idA in it.crewIds }
            val carBIndex = cars.indexOfFirst { idB in it.crewIds }
            if (carAIndex == -1 || carBIndex == -1 || carAIndex == carBIndex) return@updateDraft draft

            val newCars = cars.toMutableList()
            val carA = newCars[carAIndex]
            val carB = newCars[carBIndex]
            newCars[carAIndex] = carA.copy(
                crewIds = carA.crewIds - idA + idB,
                driverId = if (carA.driverId == idA) null else carA.driverId
            )
            newCars[carBIndex] = carB.copy(
                crewIds = carB.crewIds - idB + idA,
                driverId = if (carB.driverId == idB) null else carB.driverId
            )
            draft.copy(cars = newCars, dirty = true)
        }
    }

    fun openDriverPicker(carIndex: Int) {
        _uiState.update { it.copy(driverPickerCarIndex = carIndex, selectedPersonId = null) }
    }

    fun closeDriverPicker() {
        _uiState.update { it.copy(driverPickerCarIndex = null) }
    }

    fun setDriver(carIndex: Int, personId: String) {
        updateDraft { draft ->
            val cars = draft.cars ?: return@updateDraft draft
            if (carIndex !in cars.indices) return@updateDraft draft
            val newCars = cars.toMutableList()
            newCars[carIndex] = newCars[carIndex].copy(driverId = personId)
            draft.copy(cars = newCars, dirty = true)
        }
        _uiState.update { it.copy(driverPickerCarIndex = null) }
    }

    fun confirm() {
        val data = ensureTodayDraft(_uiState.value.appData)
        val draft = data.draft ?: return
        val cars = draft.cars ?: return
        val confirmedAt = Instant.now().toString()
        val entry = HistoryDay(date = draft.date, cars = cars, confirmedAt = confirmedAt)
        val newHistory = data.history.filterNot { it.date == draft.date } + entry
        val newDraft = draft.copy(pins = emptyMap(), savedAt = confirmedAt, dirty = false)
        updateData { it.copy(history = newHistory, draft = newDraft) }
        emit(UiEvent.Toast("Saved to history for ${formatDateShort(draft.date)}"))
    }

    fun copyDayAsText(date: String, cars: List<CarAllocation>) {
        val data = _uiState.value.appData
        val text = buildDayText(date, cars, data.people, data.vehicles)
        emit(UiEvent.CopyToClipboard(text))
    }

    private fun buildDayText(date: String, cars: List<CarAllocation>, people: List<com.crewroster.core.Person>, vehicles: List<Vehicle>): String {
        val personById = people.associateBy { it.id }
        val vehicleById = vehicles.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Crew Roster — ").append(formatDateShort(date)).append("\n\n")
        cars.forEach { car ->
            val vehicleName = vehicleById[car.vehicleId]?.name ?: "Car"
            val driverName = car.driverId?.let { personById[it]?.name } ?: "— none —"
            sb.append(vehicleName).append("\n")
            sb.append("Driver: ").append(driverName).append("\n")
            val crewNames = car.crewIds.mapNotNull { pid ->
                val p = personById[pid] ?: return@mapNotNull null
                val tag = when (p.role) {
                    Role.TL -> " (TL)"
                    Role.ATL -> " (ATL)"
                    Role.BO -> ""
                }
                p.name + tag
            }
            sb.append("Crew: ").append(if (crewNames.isEmpty()) "none" else crewNames.joinToString(", ")).append("\n\n")
        }
        return sb.toString().trim()
    }

    // ---- History screen actions ----

    fun deleteDay(date: String) {
        updateData { data -> data.copy(history = data.history.filterNot { it.date == date }) }
        emit(UiEvent.Toast("Deleted ${formatDateShort(date)}"))
    }

    fun editDay(date: String) {
        val data = _uiState.value.appData
        val day = data.history.firstOrNull { it.date == date } ?: return
        val presentIds = day.cars.flatMap { it.crewIds }.toSet()
        val absentIds = data.people.filterNot { it.id in presentIds }.map { it.id }.toSet()
        val newDraft = DraftState(
            date = day.date,
            absentIds = absentIds,
            pins = emptyMap(),
            cars = day.cars,
            warnings = emptyList(),
            savedAt = day.confirmedAt,
            dirty = false
        )
        updateData { it.copy(draft = newDraft) }
        _uiState.update { it.copy(selectedPersonId = null, driverPickerCarIndex = null) }
    }

    // ---- Tally / CSV ----

    fun exportCsv() {
        val data = _uiState.value.appData
        val rows = mutableListOf(listOf("Date", "Vehicle", "Driver", "Crew", "Team Leader In Car", "Assistant Team Leader In Car"))
        val personById = data.people.associateBy { it.id }
        val vehicleById = data.vehicles.associateBy { it.id }
        data.history.sortedBy { it.date }.forEach { day ->
            day.cars.forEach { car ->
                val vehicleName = vehicleById[car.vehicleId]?.name ?: car.vehicleId
                val driverName = car.driverId?.let { personById[it]?.name } ?: ""
                val crewNames = car.crewIds.mapNotNull { personById[it]?.name }
                val tlIn = car.crewIds.any { personById[it]?.role == Role.TL }
                val atlIn = car.crewIds.any { personById[it]?.role == Role.ATL }
                rows.add(
                    listOf(
                        day.date,
                        vehicleName,
                        driverName,
                        crewNames.joinToString("; "),
                        if (tlIn) "Y" else "N",
                        if (atlIn) "Y" else "N"
                    )
                )
            }
        }
        val csv = rows.joinToString("\r\n") { row -> row.joinToString(",") { csvEscape(it) } }
        emit(UiEvent.ExportCsv(csv, "crew-roster-history-${todayIso()}.csv"))
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value

    // ---- Setup screen actions ----

    fun updatePersonName(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateData { data ->
            data.copy(people = data.people.map { if (it.id == id) it.copy(name = trimmed) else it })
        }
    }

    fun updateVehicleName(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateData { data ->
            data.copy(vehicles = data.vehicles.map { if (it.id == id) it.copy(name = trimmed) else it })
        }
    }

    fun wipeAllData() {
        val fresh = defaultAppData()
        _uiState.update { UiState(appData = ensureTodayDraft(fresh), loading = false) }
        persist(_uiState.value.appData)
        emit(UiEvent.Toast("All data wiped"))
    }
}
