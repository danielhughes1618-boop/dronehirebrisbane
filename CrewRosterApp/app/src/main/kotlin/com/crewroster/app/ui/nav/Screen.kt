package com.crewroster.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    History("history", "History", Icons.Filled.DateRange),
    Tally("tally", "Tally", Icons.Outlined.BarChart),
    Setup("setup", "Setup", Icons.Filled.Settings)
}
