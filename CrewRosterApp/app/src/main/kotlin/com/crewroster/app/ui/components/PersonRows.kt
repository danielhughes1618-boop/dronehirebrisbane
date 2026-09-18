package com.crewroster.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.core.Person
import com.crewroster.core.Role
import com.crewroster.core.Vehicle

@Composable
fun AbsenceRow(person: Person, isAbsent: Boolean, onToggle: () -> Unit) {
    val extras = CrewRosterExtras.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (person.role) {
                Role.TL -> RoleBadge("TL", MaterialTheme.colorScheme.primary)
                Role.ATL -> RoleBadge("ATL", MaterialTheme.colorScheme.secondary)
                Role.BO -> {}
            }
            Text(person.name, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (isAbsent) "Absent" else "Present",
                color = if (isAbsent) extras.statusCritical else extras.statusGood,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = !isAbsent,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = extras.statusGood)
            )
        }
    }
}

@Composable
fun PinRow(
    person: Person,
    vehicles: List<Vehicle>,
    pinnedVehicleId: String?,
    pinnedAsDriver: Boolean,
    onCarPinChange: (String?) -> Unit,
    onDriverPinChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (person.role) {
                Role.TL -> RoleBadge("TL", MaterialTheme.colorScheme.primary)
                Role.ATL -> RoleBadge("ATL", MaterialTheme.colorScheme.secondary)
                Role.BO -> {}
            }
            Text(person.name, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val carOptions = listOf<Pair<String?, String>>(null to "No pin") + vehicles.map { it.id to it.name }
            SimpleDropdown(options = carOptions, selectedKey = pinnedVehicleId, onSelect = onCarPinChange)
            if (person.role == Role.BO) {
                val driverOptions = listOf<Pair<String?, String>>(
                    null to "Not pinned as driver",
                    "driver" to "Pin as driver of that car"
                )
                SimpleDropdown(
                    options = driverOptions,
                    selectedKey = if (pinnedAsDriver) "driver" else null,
                    onSelect = { onDriverPinChange(it == "driver") },
                    enabled = pinnedVehicleId != null
                )
            }
        }
    }
}
