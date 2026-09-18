package com.crewroster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.components.AbsenceRow
import com.crewroster.app.ui.components.PinRow
import com.crewroster.app.ui.components.WarningBanner
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.app.util.formatDateLong
import com.crewroster.core.AppData
import com.crewroster.core.Role

@Composable
fun HomeScreen(
    appData: AppData,
    onToggleAbsent: (String) -> Unit,
    onCarPinChange: (String, String?) -> Unit,
    onDriverPinChange: (String, Boolean) -> Unit,
    onGenerate: () -> Unit
) {
    val draft = appData.draft
    val absentIds = draft?.absentIds ?: emptySet()
    val present = appData.people.filterNot { it.id in absentIds }
    val presentBoCount = present.count { it.role == Role.BO }
    val canGenerate = presentBoCount >= 3
    val alreadyToday = draft != null && appData.history.any { it.date == draft.date }
    var pinsExpanded by remember { mutableStateOf(false) }
    val extras = CrewRosterExtras.colors

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Crew Roster",
                fontWeight = FontWeight.ExtraBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )
            Text(
                draft?.let { formatDateLong(it.date) } ?: "",
                color = extras.text2,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (alreadyToday) {
            item {
                WarningBanner(
                    title = "Today already has a saved roster",
                    messages = listOf("Generating again will let you update it - remember to Confirm to save your changes."),
                    critical = false,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "WHO'S IN TODAY",
                        color = extras.muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    appData.people.forEachIndexed { index, person ->
                        AbsenceRow(
                            person = person,
                            isAbsent = person.id in absentIds,
                            onToggle = { onToggleAbsent(person.id) }
                        )
                        if (index != appData.people.lastIndex) HorizontalDivider(color = extras.grid)
                    }
                }
            }
        }

        item {
            TextButton(onClick = { pinsExpanded = !pinsExpanded }) {
                Text(if (pinsExpanded) "Pins (optional) - hide ▾" else "Pins (optional) - set before generating ▸")
            }
        }

        if (pinsExpanded) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (present.isEmpty()) {
                            Text("No one present to pin yet.", color = extras.text2)
                        } else {
                            present.forEachIndexed { index, person ->
                                val pin = draft?.pins?.get(person.id)
                                PinRow(
                                    person = person,
                                    vehicles = appData.vehicles,
                                    pinnedVehicleId = pin?.vehicleId,
                                    pinnedAsDriver = pin?.asDriver ?: false,
                                    onCarPinChange = { onCarPinChange(person.id, it) },
                                    onDriverPinChange = { onDriverPinChange(person.id, it) }
                                )
                                if (index != present.lastIndex) HorizontalDivider(color = extras.grid)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                if (canGenerate) {
                    "${present.size} present · ${appData.vehicles.size} cars · ready to generate."
                } else {
                    "Only $presentBoCount of 10 Build Officers present - need at least 3 to generate a roster."
                },
                color = if (canGenerate) extras.text2 else extras.statusCritical,
                fontWeight = if (canGenerate) FontWeight.Normal else FontWeight.Bold,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        item {
            Button(
                onClick = onGenerate,
                enabled = canGenerate,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                Text("Generate Roster", fontWeight = FontWeight.Bold)
            }
        }
    }
}
