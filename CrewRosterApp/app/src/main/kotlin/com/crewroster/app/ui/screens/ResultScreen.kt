package com.crewroster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.components.CarCard
import com.crewroster.app.ui.components.CrewChipData
import com.crewroster.app.ui.components.WarningBanner
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.app.util.formatDateLong
import com.crewroster.core.AppData
import com.crewroster.core.DraftState
import com.crewroster.core.RuleViolation
import com.crewroster.core.WarningLevel

private val CAR_COLORS_INDEX = 3

@Composable
fun ResultScreen(
    appData: AppData,
    draft: DraftState,
    ruleViolations: List<RuleViolation>,
    selectedPersonId: String?,
    driverPickerCarIndex: Int?,
    onPersonTap: (String) -> Unit,
    onOpenDriverPicker: (Int) -> Unit,
    onCloseDriverPicker: () -> Unit,
    onSetDriver: (Int, String) -> Unit,
    onRegenerate: () -> Unit,
    onConfirm: () -> Unit,
    onCopyAsText: () -> Unit
) {
    val extras = CrewRosterExtras.colors
    val cars = draft.cars ?: emptyList()
    val personById = appData.people.associateBy { it.id }
    val vehicleById = appData.vehicles.associateBy { it.id }

    val errorMessages = draft.warnings.filter { it.level == WarningLevel.ERROR }.map { it.message } +
        ruleViolations.map { it.message }
    val infoMessages = draft.warnings.filter { it.level == WarningLevel.INFO }.map { it.message }

    val carColors = listOf(
        MaterialTheme.colorScheme.primary,
        extras.car2,
        extras.car3
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Crew Roster",
                fontWeight = FontWeight.ExtraBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )
            Text(formatDateLong(draft.date), color = extras.text2, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (errorMessages.isNotEmpty()) {
            item {
                WarningBanner(
                    title = "Rule broken",
                    messages = errorMessages,
                    critical = true,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
        if (infoMessages.isNotEmpty()) {
            item {
                WarningBanner(
                    title = "Note",
                    messages = infoMessages,
                    critical = false,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        itemsIndexed(cars) { index, car ->
            val vehicle = vehicleById[car.vehicleId]
            val driver = car.driverId?.let { personById[it] }
            val crewChips = car.crewIds.mapNotNull { pid ->
                val p = personById[pid] ?: return@mapNotNull null
                CrewChipData(
                    personId = p.id,
                    name = p.name,
                    role = p.role,
                    isDriver = car.driverId == p.id,
                    isSelected = selectedPersonId == p.id
                )
            }
            CarCard(
                vehicleName = vehicle?.name ?: "Car",
                accentColor = carColors[index % CAR_COLORS_INDEX],
                driverName = driver?.name,
                crew = crewChips,
                onDriverClick = { onOpenDriverPicker(index) },
                onPersonClick = onPersonTap,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        item {
            if (draft.savedAt != null && !draft.dirty) {
                Text("✓ Saved to history", color = extras.statusGood, modifier = Modifier.padding(bottom = 10.dp))
            } else if (draft.savedAt != null) {
                Text("Unsaved changes - Confirm to update history.", color = extras.text2, modifier = Modifier.padding(bottom = 10.dp))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f)) {
                    Text("Regenerate")
                }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            OutlinedButton(onClick = onCopyAsText, modifier = Modifier.fillMaxWidth()) {
                Text("Copy as text")
            }
        }
    }

    if (driverPickerCarIndex != null && driverPickerCarIndex in cars.indices) {
        val car = cars[driverPickerCarIndex]
        val vehicle = vehicleById[car.vehicleId]
        AlertDialog(
            onDismissRequest = onCloseDriverPicker,
            title = { Text("Set driver - ${vehicle?.name ?: "Car"}") },
            text = {
                Column {
                    Text(
                        "Choose who drives this car. Picking a Team Leader or Assistant Team Leader will break the driver rule and show a warning.",
                        color = extras.text2,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (car.crewIds.isEmpty()) {
                        Text("No one is in this car yet - swap someone in first.", color = extras.text2)
                    } else {
                        car.crewIds.forEach { pid ->
                            val p = personById[pid] ?: return@forEach
                            val current = if (car.driverId == pid) " (current)" else ""
                            val roleSuffix = if (p.role.name != "BO") " - ${p.role.name}" else ""
                            TextButton(onClick = { onSetDriver(driverPickerCarIndex, pid) }) {
                                Text(p.name + roleSuffix + current)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseDriverPicker) { Text("Cancel") }
            }
        )
    }
}
