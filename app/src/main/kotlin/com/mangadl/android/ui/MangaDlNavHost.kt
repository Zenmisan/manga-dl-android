package com.mangadl.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mangadl.android.ui.screens.detail.MangaDetailScreen
import com.mangadl.android.ui.screens.downloads.DownloadsScreen
import com.mangadl.android.ui.screens.library.LibraryScreen
import com.mangadl.android.ui.screens.reader.ReaderScreen
import com.mangadl.android.ui.screens.search.SearchScreen
import com.mangadl.android.ui.screens.settings.SettingsScreen
import com.mangadl.android.ui.screens.sources.SourcesScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Library : Screen("library", "Library", Icons.Default.Bookmarks)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Sources : Screen("sources", "Sources", Icons.Default.Explore)
    data object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val bottomNavScreens = listOf(
    Screen.Library,
    Screen.Search,
    Screen.Sources,
    Screen.Downloads,
    Screen.Settings,
)

private val fullscreenRoutes = setOf("reader/", "detail/")

@Composable
fun MangaDlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = fullscreenRoutes.none { currentRoute.startsWith(it) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavScreens.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelMedium) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    onMangaClick = { provider, mangaId ->
                        navController.navigate("detail/$provider/$mangaId")
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onMangaClick = { provider, mangaId ->
                        navController.navigate("detail/$provider/$mangaId")
                    }
                )
            }
            composable(Screen.Sources.route) {
                SourcesScreen()
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "detail/{provider}/{mangaId}",
                arguments = listOf(
                    navArgument("provider") { type = NavType.StringType },
                    navArgument("mangaId") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                MangaDetailScreen(
                    provider = backStackEntry.arguments?.getString("provider") ?: "",
                    mangaId = backStackEntry.arguments?.getString("mangaId") ?: "",
                    onReadChapter = { provider, mangaId, chapterId ->
                        navController.navigate("reader/$provider/$mangaId/$chapterId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "reader/{provider}/{mangaId}/{chapterId}",
                arguments = listOf(
                    navArgument("provider") { type = NavType.StringType },
                    navArgument("mangaId") { type = NavType.StringType },
                    navArgument("chapterId") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                ReaderScreen(
                    provider = backStackEntry.arguments?.getString("provider") ?: "",
                    mangaId = backStackEntry.arguments?.getString("mangaId") ?: "",
                    chapterId = backStackEntry.arguments?.getString("chapterId") ?: "",
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
