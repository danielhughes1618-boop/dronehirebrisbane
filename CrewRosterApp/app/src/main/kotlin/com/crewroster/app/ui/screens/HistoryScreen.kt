package com.crewroster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.crewroster.app.ui.components.ConfirmDialog
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.app.util.formatDateLong
import com.crewroster.app.util.formatDateShort
import com.crewroster.core.AppData
import com.crewroster.core.HistoryDay
import com.crewroster.core.Role

@Composable
fun HistoryScreen(
    appData: AppData,
    onEditDay: (String) -> Unit,
    onDeleteDay: (String) -> Unit,
    onCopyDay: (String, List<com.crewroster.core.CarAllocation>) -> Unit
) {
    val extras = CrewRosterExtras.colors
    val days = appData.history.sortedByDescending { it.date }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("History", fontWeight = FontWeight.ExtraBold, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            Text(
                "${days.size} confirmed day${if (days.size == 1) "" else "s"}",
                color = extras.text2,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (days.isEmpty()) {
            item {
                Text(
                    "No confirmed days yet.\nGenerate and confirm a roster to see it here.",
                    color = extras.muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                )
            }
        }

        items(days, key = { it.date }) { day ->
            HistoryDayCard(
                day = day,
                appData = appData,
                onEdit = { onEditDay(day.date) },
                onDelete = { pendingDelete = day.date },
                onCopy = { onCopyDay(day.date, day.cars) }
            )
        }
    }

    pendingDelete?.let { date ->
        ConfirmDialog(
            title = "Delete this day?",
            body = "This removes the confirmed roster for ${formatDateShort(date)} and its drives from the tally. This can't be undone.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                onDeleteDay(date)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun HistoryDayCard(
    day: HistoryDay,
    appData: AppData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val extras = CrewRosterExtras.colors
    val personById = appData.people.associateBy { it.id }
    val vehicleById = appData.vehicles.associateBy { it.id }
    val carColors = listOf(MaterialTheme.colorScheme.primary, extras.car2, extras.car3)

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(formatDateLong(day.date), fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            day.cars.forEachIndexed { index, car ->
                val vehicleName = vehicleById[car.vehicleId]?.name ?: "Car"
                val driverName = car.driverId?.let { personById[it]?.name } ?: "no driver"
                val crewNames = car.crewIds.mapNotNull { pid ->
                    val p = personById[pid] ?: return@mapNotNull null
                    val tag = when (p.role) {
                        Role.TL -> " (TL)"
                        Role.ATL -> " (ATL)"
                        Role.BO -> ""
                    }
                    p.name + tag
                }
                Row(modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(carColors[index % carColors.size], RoundedCornerShape(2.dp))
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text("$vehicleName - 🚗 $driverName", fontWeight = FontWeight.Bold)
                        Text(if (crewNames.isEmpty()) "no crew" else crewNames.joinToString(", "), color = extras.text2)
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = extras.statusCritical) }
                TextButton(onClick = onCopy) { Text("Copy") }
            }
        }
    }
}
