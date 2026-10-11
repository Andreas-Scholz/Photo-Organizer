package io.github.andreasscholz.photoorganizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.andreasscholz.photoorganizer.ui.folders.FolderSelectionScreen
import io.github.andreasscholz.photoorganizer.ui.home.HomeScreen
import io.github.andreasscholz.photoorganizer.ui.review.ReviewScreen
import io.github.andreasscholz.photoorganizer.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onStartReview = { startItemId -> navController.navigate(ReviewRoute(startItemId)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ReviewRoute> {
            ReviewScreen(onClose = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenFolderSelection = { navController.navigate(FolderSelectionRoute) },
            )
        }
        composable<FolderSelectionRoute> {
            FolderSelectionScreen(onBack = { navController.popBackStack() })
        }
    }
}
