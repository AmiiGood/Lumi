package io.github.amiigood.lumi.lumi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.amiigood.lumi.lumi.ui.detail.CollectionDetailScreen
import io.github.amiigood.lumi.lumi.ui.library.LibraryScreen
import io.github.amiigood.lumi.lumi.ui.onboarding.OnboardingScreen
import io.github.amiigood.lumi.lumi.ui.detail.ItemDetailScreen
import io.github.amiigood.lumi.lumi.ui.reader.ReaderScreen
import io.github.amiigood.lumi.lumi.ui.settings.SettingsScreen
import java.net.URLEncoder

@Composable
fun LumiNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(LumiDestination.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(LumiDestination.Library.route) {
                        popUpTo(LumiDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(LumiDestination.Library.route) {
            LibraryScreen(
                onCollectionClick = { name ->
                    val encoded = URLEncoder.encode(name, "UTF-8")
                    navController.navigate("collection/$encoded")
                },
                onItemClick = { id ->
                    navController.navigate(LumiDestination.ItemDetail.build(id))
                },
                onSettingsClick = {
                    navController.navigate(LumiDestination.Settings.route)
                }
            )
        }

        composable(LumiDestination.ItemDetail.route) {
            ItemDetailScreen(
                onBack = { navController.popBackStack() },
                onRead = { id ->
                    navController.navigate(LumiDestination.Reader.build(id))
                }
            )
        }

        composable(LumiDestination.CollectionDetail.route) {
            CollectionDetailScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { id ->
                    navController.navigate(LumiDestination.ItemDetail.build(id))
                }
            )
        }

        composable(LumiDestination.Reader.route) {
            ReaderScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(LumiDestination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}