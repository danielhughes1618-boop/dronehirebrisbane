package com.crewroster.app.ui

import com.crewroster.core.AppData
import com.crewroster.core.RuleViolation
import com.crewroster.core.TallyRow
import com.crewroster.core.defaultAppData

data class UiState(
    val appData: AppData = defaultAppData(),
    val loading: Boolean = true,
    val selectedPersonId: String? = null,
    val driverPickerCarIndex: Int? = null
) {
    val ruleViolations: List<RuleViolation>
        get() {
            val cars = appData.draft?.cars ?: return emptyList()
            return com.crewroster.core.AllocationEngine.validate(cars, appData.people, appData.vehicles)
        }

    val tally: List<TallyRow>
        get() = com.crewroster.core.AllocationEngine.tally(
            appData.people,
            appData.history,
            com.crewroster.app.util.todayIso()
        )
}

sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
    data class CopyToClipboard(val text: String) : UiEvent
    data class ExportCsv(val csv: String, val fileName: String) : UiEvent
}
