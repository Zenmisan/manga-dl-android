package com.mangadl.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mangadl.android.ui.screens.browse.BrowseScreen
import com.mangadl.android.ui.screens.browse.SourceBrowseScreen
import com.mangadl.android.ui.screens.detail.MangaDetailScreen
import com.mangadl.android.ui.screens.downloads.DownloadsScreen
import com.mangadl.android.ui.screens.history.HistoryScreen
import com.mangadl.android.ui.screens.library.LibraryScreen
import com.mangadl.android.ui.screens.more.MoreScreen
import com.mangadl.android.ui.screens.reader.ReaderScreen
import com.mangadl.android.ui.screens.settings.SettingsGeneralScreen
import com.mangadl.android.ui.screens.settings.SettingsReaderScreen
import com.mangadl.android.ui.screens.settings.SettingsScreen
import com.mangadl.android.ui.screens.updates.UpdatesScreen
import com.mangadl.android.ui.theme.MangaDlColors

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val navTabs = listOf(
    NavTab("library", "Library", Icons.Default.LocalLibrary),
    NavTab("updates", "Updates", Icons.Default.Refresh),
    NavTab("history", "History", Icons.Default.History),
    NavTab("browse", "Browse", Icons.Default.Explore),
    NavTab("more", "More", Icons.Default.MoreHoriz),
)

private val bottomNavRoutes = navTabs.map { it.route }.toSet()

@Composable
fun MangaDlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute in bottomNavRoutes &&
        !currentRoute.startsWith("source/") &&
        !currentRoute.startsWith("detail/") &&
        !currentRoute.startsWith("reader/")

    Column(Modifier.fillMaxSize().background(MangaDlColors.Background)) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "library",
            ) {
                composable("library") {
                    LibraryScreen(
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        },
                        onResumeReading = { provider, mangaId, chapterId ->
                            navController.navigate("reader/$provider/$mangaId/$chapterId")
                        },
                    )
                }
                composable("updates") {
                    UpdatesScreen(
                        onChapterClick = { provider, mangaId, chapterId ->
                            navController.navigate("reader/$provider/$mangaId/$chapterId")
                        }
                    )
                }
                composable("history") {
                    HistoryScreen(
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        }
                    )
                }
                composable("browse") {
                    BrowseScreen(
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        },
                        onSourceClick = { provider ->
                            navController.navigate("source/$provider")
                        },
                    )
                }
                composable(
                    route = "source/{provider}",
                    arguments = listOf(navArgument("provider") { type = NavType.StringType }),
                ) { backStackEntry ->
                    SourceBrowseScreen(
                        provider = backStackEntry.arguments?.getString("provider") ?: "",
                        onMangaClick = { prov, mangaId ->
                            navController.navigate("detail/$prov/$mangaId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("more") {
                    MoreScreen(
                        onDownloads = { navController.navigate("downloads") },
                        onSettings = { navController.navigate("settings") },
                        onHistory = { navController.navigate("history") },
                    )
                }
                composable("downloads") {
                    DownloadsScreen(onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onGeneral = { navController.navigate("settings/general") },
                        onReader = { navController.navigate("settings/reader") },
                    )
                }
                composable("settings/general") {
                    SettingsGeneralScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/reader") {
                    SettingsReaderScreen(onBack = { navController.popBackStack() })
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
                        onReadChapter = { prov, mId, chId ->
                            navController.navigate("reader/$prov/$mId/$chId")
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

        if (showBottomBar) {
            MangaDlBottomNav(
                currentRoute = currentRoute,
                onTabClick = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo("library") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MangaDlBottomNav(currentRoute: String, onTabClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MangaDlColors.NavBg)
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = androidx.compose.ui.graphics.RectangleShape,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabClick(tab.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(width = 56.dp, height = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) MangaDlColors.Primary else MangaDlColors.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = tab.label,
                    color = if (selected) MangaDlColors.Primary else MangaDlColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
