package com.crewroster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crewroster.app.ui.CrewRosterViewModel
import com.crewroster.app.ui.UiEvent
import com.crewroster.app.ui.components.BottomNavBar
import com.crewroster.app.ui.nav.Screen
import com.crewroster.app.ui.screens.HistoryScreen
import com.crewroster.app.ui.screens.HomeScreen
import com.crewroster.app.ui.screens.ResultScreen
import com.crewroster.app.ui.screens.SetupScreen
import com.crewroster.app.ui.screens.TallyScreen
import com.crewroster.app.ui.theme.CrewRosterTheme
import com.crewroster.app.util.shareCsv

private const val RESULT_ROUTE = "result"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrewRosterTheme {
                CrewRosterApp()
            }
        }
    }
}

@Composable
fun CrewRosterApp(viewModel: CrewRosterViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Toast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is UiEvent.CopyToClipboard -> {
                    copyToClipboard(context, event.text)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ExportCsv -> shareCsv(context, event.csv, event.fileName)
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute) { screen ->
                viewModel.clearSelection()
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    appData = uiState.appData,
                    onToggleAbsent = viewModel::toggleAbsent,
                    onCarPinChange = viewModel::setPinCar,
                    onDriverPinChange = viewModel::setPinDriver,
                    onGenerate = {
                        viewModel.generate()
                        navController.navigate(RESULT_ROUTE) { launchSingleTop = true }
                    }
                )
            }
            composable(RESULT_ROUTE) {
                val draft = uiState.appData.draft
                val cars = draft?.cars
                if (draft != null && cars != null) {
                    ResultScreen(
                        appData = uiState.appData,
                        draft = draft,
                        ruleViolations = uiState.ruleViolations,
                        selectedPersonId = uiState.selectedPersonId,
                        driverPickerCarIndex = uiState.driverPickerCarIndex,
                        onPersonTap = viewModel::onPersonTap,
                        onOpenDriverPicker = viewModel::openDriverPicker,
                        onCloseDriverPicker = viewModel::closeDriverPicker,
                        onSetDriver = viewModel::setDriver,
                        onRegenerate = viewModel::regenerate,
                        onConfirm = viewModel::confirm,
                        onCopyAsText = { viewModel.copyDayAsText(draft.date, cars) }
                    )
                }
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    appData = uiState.appData,
                    onEditDay = { date ->
                        viewModel.editDay(date)
                        navController.navigate(RESULT_ROUTE) { launchSingleTop = true }
                    },
                    onDeleteDay = viewModel::deleteDay,
                    onCopyDay = viewModel::copyDayAsText
                )
            }
            composable(Screen.Tally.route) {
                TallyScreen(
                    rows = uiState.tally,
                    onExportCsv = viewModel::exportCsv
                )
            }
            composable(Screen.Setup.route) {
                SetupScreen(
                    appData = uiState.appData,
                    onPersonNameChange = viewModel::updatePersonName,
                    onVehicleNameChange = viewModel::updateVehicleName,
                    onExportCsv = viewModel::exportCsv,
                    onWipeAll = viewModel::wipeAllData
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Crew Roster", text))
}
