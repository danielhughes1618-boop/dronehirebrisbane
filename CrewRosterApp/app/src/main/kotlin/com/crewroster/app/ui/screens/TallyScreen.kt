package com.crewroster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crewroster.app.ui.theme.CrewRosterExtras
import com.crewroster.app.util.formatDateShort
import com.crewroster.core.TallyRow

@Composable
fun TallyScreen(
    rows: List<TallyRow>,
    onExportCsv: () -> Unit
) {
    val extras = CrewRosterExtras.colors
    val maxDrives = (rows.maxOfOrNull { it.drives } ?: 0).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Driving Tally", fontWeight = FontWeight.ExtraBold, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            Text(
                "Fewest drives first - the imbalance you should fix",
                color = extras.text2,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (rows.isEmpty()) {
            item {
                Text(
                    "No drives recorded yet.",
                    color = extras.muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                )
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        rows.forEachIndexed { index, row ->
                            TallyRowItem(row = row, maxDrives = maxDrives)
                            if (index != rows.lastIndex) HorizontalDivider(color = extras.grid)
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                Text("Export history as CSV")
            }
        }
    }
}

@Composable
private fun TallyRowItem(row: TallyRow, maxDrives: Int) {
    val extras = CrewRosterExtras.colors
    val fraction = if (row.drives > 0) (row.drives.toFloat() / maxDrives.toFloat()).coerceIn(0.03f, 1f) else 0f
    val lastDate = row.lastDate
    val metaText = if (lastDate != null) {
        "Last drove ${formatDateShort(lastDate)} - ${row.daysSince} day${if (row.daysSince == 1) "" else "s"} ago"
    } else {
        "Never driven yet"
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.name, fontWeight = FontWeight.Bold)
            Text(metaText, color = extras.muted, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(extras.sequentialTrack, RoundedCornerShape(8.dp))
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(extras.sequential, RoundedCornerShape(8.dp))
                    )
                }
            }
            Text(
                "${row.drives} drive${if (row.drives == 1) "" else "s"}",
                fontWeight = FontWeight.Bold,
                color = extras.text2,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
