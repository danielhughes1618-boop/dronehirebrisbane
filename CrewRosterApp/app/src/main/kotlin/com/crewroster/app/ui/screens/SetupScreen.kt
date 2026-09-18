package com.crewroster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.components.ConfirmDialog
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.core.AppData
import com.crewroster.core.Person
import com.crewroster.core.Role
import com.crewroster.core.Vehicle
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(
    appData: AppData,
    onPersonNameChange: (String, String) -> Unit,
    onVehicleNameChange: (String, String) -> Unit,
    onExportCsv: () -> Unit,
    onWipeAll: () -> Unit
) {
    val extras = CrewRosterExtras.colors
    var wipeDialogOpen by remember { mutableStateOf(false) }

    val leaders = appData.people.filter { it.role == Role.TL || it.role == Role.ATL }
    val bos = appData.people.filter { it.role == Role.BO }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Setup", fontWeight = FontWeight.ExtraBold, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            Text("Names save automatically", color = extras.text2, modifier = Modifier.padding(bottom = 12.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Leadership")
                    leaders.forEach { person -> PersonNameField(person, onPersonNameChange) }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Build Officers")
                    bos.forEach { person -> PersonNameField(person, onPersonNameChange) }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Vehicles")
                    appData.vehicles.forEach { vehicle -> VehicleNameField(vehicle, onVehicleNameChange) }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Data")
                    Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("Export CSV")
                    }
                    Button(
                        onClick = { wipeDialogOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = extras.statusCritical)
                    ) {
                        Text("Wipe all data")
                    }
                }
            }
        }
    }

    if (wipeDialogOpen) {
        ConfirmDialog(
            title = "Wipe all data?",
            body = "This permanently deletes all names, vehicles, history and driving tallies from this device. This cannot be undone.",
            confirmLabel = "Wipe everything",
            danger = true,
            onConfirm = {
                onWipeAll()
                wipeDialogOpen = false
            },
            onDismiss = { wipeDialogOpen = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val extras = CrewRosterExtras.colors
    Text(
        text.uppercase(),
        color = extras.muted,
        fontWeight = FontWeight.Bold,
        fontSize = MaterialTheme.typography.titleSmall.fontSize
    )
}

/**
 * Autosaves 600ms after typing stops, rather than requiring an explicit Save button -
 * mirrors the "names save automatically" behavior from the original prototype.
 */
@Composable
private fun PersonNameField(person: Person, onChange: (String, String) -> Unit) {
    var value by remember(person.id, person.name) { mutableStateOf(person.name) }
    val label = when (person.role) {
        Role.TL -> "Team Leader"
        Role.ATL -> "Assistant Team Leader"
        Role.BO -> "Build Officer"
    }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    LaunchedEffect(value) {
        delay(600)
        if (value != person.name) onChange(person.id, value)
    }
}

@Composable
private fun VehicleNameField(vehicle: Vehicle, onChange: (String, String) -> Unit) {
    var value by remember(vehicle.id, vehicle.name) { mutableStateOf(vehicle.name) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text("Vehicle") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    LaunchedEffect(value) {
        delay(600)
        if (value != vehicle.name) onChange(vehicle.id, value)
    }
}
