package com.homm3.livewallpaper.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.homm3.livewallpaper.android.data.MapsViewModel
import com.homm3.livewallpaper.android.ui.OnboardingViewModel
import com.homm3.livewallpaper.android.ui.SettingsViewModel

object Destinations {
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val MAPS = "maps"
    const val MAP_NAME_KEY = "map_name"
}

class NavigationActions(navController: NavHostController) {
    val settings: () -> Unit = { navController.navigate(Destinations.SETTINGS) }
    val about: () -> Unit = { navController.navigate(Destinations.ABOUT) }
    val maps: () -> Unit = { navController.navigate(Destinations.MAPS) }
    val mapByName: (String) -> Unit =
        { mapName -> navController.navigate("${Destinations.MAPS}/$mapName") }
    val navigateUp: () -> Unit = { navController.navigateUp() }
}

@Composable
fun NavigationHost(
    mapViewModel: MapsViewModel,
    settingsViewModel: SettingsViewModel,
    onboardingViewModel: OnboardingViewModel,
    onSetWallpaperClick: () -> Unit,
) {
    val navController = rememberNavController()
    val actions = remember(navController) { NavigationActions(navController) }
    val startDestination = when (onboardingViewModel.isGameAssetsAvailable()) {
        true -> Destinations.SETTINGS
        false -> Destinations.ONBOARDING
    }

    NavHost(navController, startDestination) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                actions
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onSetWallpaperClick = onSetWallpaperClick,
                actions
            )
        }

        composable(Destinations.ABOUT) {
            AboutScreen(actions)
        }

        composable(Destinations.MAPS) {
            MapsScreen(viewModel = mapViewModel, actions)
        }

        composable(
            "${Destinations.MAPS}/{${Destinations.MAP_NAME_KEY}}",
            arguments = listOf(
                navArgument(Destinations.MAP_NAME_KEY) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)

            MapsInfoScreen(
                mapName = arguments.getString(Destinations.MAP_NAME_KEY, ""),
                actions
            )
        }
    }
}