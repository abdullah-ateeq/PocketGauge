package com.example.pocketgauge.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketgauge.ui.about.AboutScreen
import com.example.pocketgauge.ui.components.GaugeIcons
import com.example.pocketgauge.ui.history.HistoryScreen
import com.example.pocketgauge.ui.measure.MeasureScreen
import com.example.pocketgauge.ui.settings.SettingsScreen

object Routes {
    const val MEASURE = "measure"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination(Routes.MEASURE, "Measure", GaugeIcons.Ruler),
    Destination(Routes.HISTORY, "History", Icons.AutoMirrored.Outlined.List),
    Destination(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
    Destination(Routes.ABOUT, "About", Icons.Outlined.Info),
)

private fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun PocketGaugeApp(viewModel: PocketGaugeViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigateTopLevel(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MEASURE,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.MEASURE) { MeasureScreen(viewModel, snackbarHostState) }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onMeasureAgain = { navController.navigateTopLevel(Routes.MEASURE) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel, snackbarHostState) }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}
