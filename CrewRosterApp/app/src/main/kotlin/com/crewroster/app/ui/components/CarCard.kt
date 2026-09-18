package com.crewroster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crewroster.core.Role
import com.crewroster.app.ui.theme.CrewRosterExtras

data class CrewChipData(
    val personId: String,
    val name: String,
    val role: Role,
    val isDriver: Boolean,
    val isSelected: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarCard(
    vehicleName: String,
    accentColor: Color,
    driverName: String?,
    crew: List<CrewChipData>,
    onDriverClick: () -> Unit,
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = CrewRosterExtras.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, extras.grid, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(vehicleName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${crew.size} aboard", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDriverClick)
                .background(extras.surface2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DRIVER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = extras.muted)
                Text(
                    driverName ?: "No driver assigned",
                    fontWeight = FontWeight.Bold,
                    color = if (driverName == null) extras.statusCritical else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(Icons.Filled.Edit, contentDescription = "Change driver", tint = extras.muted, modifier = Modifier.size(18.dp))
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            crew.forEach { person ->
                CrewChip(person = person, onClick = { onPersonClick(person.personId) })
            }
        }
    }
}

@Composable
private fun CrewChip(person: CrewChipData, onClick: () -> Unit) {
    val extras = CrewRosterExtras.colors
    val borderColor = if (person.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val background = when {
        person.isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        person.isDriver -> MaterialTheme.colorScheme.surface
        else -> extras.surface2
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(2.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (person.role) {
            Role.TL -> RoleBadge("TL", MaterialTheme.colorScheme.primary)
            Role.ATL -> RoleBadge("ATL", MaterialTheme.colorScheme.secondary)
            Role.BO -> {}
        }
        if (person.isDriver) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = "Driver", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Text(person.name, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoleBadge(label: String, color: Color) {
    Text(
        label,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}
