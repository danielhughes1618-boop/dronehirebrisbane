package com.crewroster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crewroster.app.ui.theme.CrewRosterExtras

@Composable
fun WarningBanner(
    title: String,
    messages: List<String>,
    critical: Boolean,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) return
    val extras = CrewRosterExtras.colors
    val accent = if (critical) extras.statusCritical else extras.statusWarning
    val bg = if (critical) accent.copy(alpha = 0.12f) else accent.copy(alpha = 0.14f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = (if (critical) "⚠ " else "") + title,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.titleSmall.fontSize,
            color = if (critical) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
        )
        messages.forEach { message ->
            Text(
                text = "• $message",
                color = if (critical) MaterialTheme.colorScheme.onSurface else extras.text2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
