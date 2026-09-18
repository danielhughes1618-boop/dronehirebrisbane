package com.crewroster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.theme.CrewRosterExtras

/**
 * A single-select dropdown built from a plain clickable row + DropdownMenu, rather than
 * ExposedDropdownMenuBox - simpler surface area to get right without a compiler on hand.
 */
@Composable
fun SimpleDropdown(
    options: List<Pair<String?, String>>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val extras = CrewRosterExtras.colors
    val selectedLabel = options.firstOrNull { it.first == selectedKey }?.second ?: options.first().second

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(extras.surface2, RoundedCornerShape(10.dp))
            .border(1.dp, extras.grid, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { expanded = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(selectedLabel)
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = extras.muted)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (key, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    expanded = false
                    onSelect(key)
                }
            )
        }
    }
}
