package com.homm3.livewallpaper.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homm3.livewallpaper.android.data.MapsViewModel
import com.homm3.livewallpaper.android.data.ParsingViewModel
import com.homm3.livewallpaper.android.data.SettingsViewModel

object Destinations {
    const val PARSING = "parsing"
    const val SETTINGS = "settings"
    const val MAPS = "maps"
    const val PHONE_LIMITATION = "phone_limitation"
}

class NavigationActions(navController: NavHostController) {
    val settings: () -> Unit = { navController.navigate(Destinations.SETTINGS) }
    val maps: () -> Unit = { navController.navigate(Destinations.MAPS) }
    val navigateUp: () -> Unit = { navController.navigateUp() }
    val phoneLimitations: () -> Unit =
        { navController.navigate(Destinations.PHONE_LIMITATION) }
}

@Composable
fun NavigationHost(
    mapViewModel: MapsViewModel,
    settingsViewModel: SettingsViewModel,
    parsingViewModel: ParsingViewModel,
    onSetWallpaperClick: () -> Unit,
) {
    val navController = rememberNavController()
    val actions = remember(navController) { NavigationActions(navController) }
    val startDestination = when (parsingViewModel.isGameAssetsAvailable()) {
        true -> Destinations.SETTINGS
        false -> Destinations.PARSING
    }

    NavHost(navController, startDestination) {
        composable(Destinations.PARSING) {
            ParsingScreen(viewModel = parsingViewModel, actions)
        }

        composable(Destinations.PHONE_LIMITATION) {
            PhoneLimitations(actions)
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onSetWallpaperClick = onSetWallpaperClick,
                actions
            )
        }

        composable(Destinations.MAPS) {
            MapsScreen(viewModel = mapViewModel)
        }
    }
}