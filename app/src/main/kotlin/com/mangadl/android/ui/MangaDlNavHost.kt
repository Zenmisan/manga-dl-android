package com.mangadl.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.mangadl.android.ui.screens.auth.CreateAccountScreen
import com.mangadl.android.ui.screens.auth.ForgotPasswordScreen
import com.mangadl.android.ui.screens.auth.SignInScreen
import com.mangadl.android.ui.screens.auth.WelcomeScreen
import com.mangadl.android.ui.screens.browse.BrowseScreen
import com.mangadl.android.ui.screens.browse.SourceBrowseScreen
import com.mangadl.android.ui.screens.search.SearchScreen
import com.mangadl.android.ui.screens.detail.MangaDetailScreen
import com.mangadl.android.ui.screens.downloads.DownloadsScreen
import com.mangadl.android.ui.screens.help.HelpScreen
import com.mangadl.android.ui.screens.history.HistoryScreen
import com.mangadl.android.ui.screens.library.LibraryScreen
import com.mangadl.android.ui.screens.localfile.LocalFileDetailScreen
import com.mangadl.android.ui.screens.migrate.MigrateScreen
import com.mangadl.android.ui.screens.more.MoreScreen
import com.mangadl.android.ui.screens.notifications.NotificationsScreen
import com.mangadl.android.ui.screens.updates.UpdatesScreen
import com.mangadl.android.ui.screens.profile.ProfileScreen
import com.mangadl.android.ui.screens.reader.NovelReaderScreen
import com.mangadl.android.ui.screens.reader.ReaderScreen
import com.mangadl.android.ui.screens.settings.SettingsAccountScreen
import com.mangadl.android.ui.screens.settings.SettingsGeneralScreen
import com.mangadl.android.ui.screens.settings.SettingsLibraryScreen
import com.mangadl.android.ui.screens.settings.SettingsReaderScreen
import com.mangadl.android.ui.screens.settings.SettingsScreen
import com.mangadl.android.ui.screens.settings.SettingsSystemScreen
import com.mangadl.android.ui.screens.settings.SettingsTrackersScreen
import com.mangadl.android.ui.screens.statistics.StatisticsScreen
import com.mangadl.android.ui.screens.tracking.TrackingSheet
import com.mangadl.android.ui.components.rememberHapticClick
import com.mangadl.android.ui.theme.MangaDlColors

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val navTabs = listOf(
    NavTab("library", "Library", Icons.Default.LocalLibrary),
    NavTab("updates", "Updates", Icons.Default.Notifications),
    NavTab("history", "History", Icons.Default.History),
    NavTab("browse", "Browse", Icons.Default.Language),
    NavTab("more", "More", Icons.Default.MoreHoriz),
)

private val bottomNavRoutes = navTabs.map { it.route }.toSet()

@Composable
fun MangaDlNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute in bottomNavRoutes

    Column(Modifier.fillMaxSize().background(MangaDlColors.Background)) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "library",
            ) {
                // ── Auth ──────────────────────────────────────────────────
                composable("welcome") {
                    WelcomeScreen(
                        onSignIn = { navController.navigate("signin") },
                        onCreateAccount = { navController.navigate("create_account") },
                        onGuest = { navController.navigate("library") { popUpTo("welcome") { inclusive = true } } },
                    )
                }
                composable("signin") {
                    SignInScreen(
                        onSuccess = { navController.navigate("library") { popUpTo("welcome") { inclusive = true } } },
                        onForgotPassword = { navController.navigate("forgot_password") },
                        onCreateAccount = { navController.navigate("create_account") },
                        onBack = { navController.popBackStack() },
                        onGuest = { navController.navigate("library") { popUpTo("welcome") { inclusive = true } } },
                    )
                }
                composable("create_account") {
                    CreateAccountScreen(
                        onSuccess = { navController.navigate("library") { popUpTo("welcome") { inclusive = true } } },
                        onSignIn = { navController.navigate("signin") },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("forgot_password") {
                    ForgotPasswordScreen(
                        onBack = { navController.popBackStack() },
                        onSignIn = { navController.navigate("signin") },
                    )
                }

                // ── Main tabs ─────────────────────────────────────────────
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
                        },
                    )
                }
                composable("history") {
                    HistoryScreen(
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        },
                        onResumeReading = { provider, mangaId, chapterId ->
                            navController.navigate("reader/$provider/$mangaId/$chapterId")
                        },
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
                        onSearchClick = { navController.navigate("search") },
                    )
                }
                composable("search") {
                    SearchScreen(
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("more") {
                    MoreScreen(
                        onDownloads = { navController.navigate("downloads") },
                        onSettings = { navController.navigate("settings") },
                        onHistory = { navController.navigate("history") },
                        onStatistics = { navController.navigate("statistics") },
                        onHelp = { navController.navigate("help") },
                        onProfile = { navController.navigate("profile") },
                        onNotifications = { navController.navigate("notifications") },
                        onSignIn = { navController.navigate("welcome") },
                    )
                }

                // ── Browse / source ────────────────────────────────────────
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

                // ── Downloads ──────────────────────────────────────────────
                composable("downloads") {
                    DownloadsScreen(onBack = { navController.popBackStack() })
                }

                // ── Settings ───────────────────────────────────────────────
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onGeneral = { navController.navigate("settings/general") },
                        onReader = { navController.navigate("settings/reader") },
                        onAccount = { navController.navigate("settings/account") },
                        onLibrary = { navController.navigate("settings/library") },
                        onTrackers = { navController.navigate("settings/trackers") },
                        onSystem = { navController.navigate("settings/system") },
                    )
                }
                composable("settings/general") {
                    SettingsGeneralScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/reader") {
                    SettingsReaderScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/account") {
                    SettingsAccountScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = { navController.navigate("welcome") { popUpTo(0) } },
                    )
                }
                composable("settings/library") {
                    SettingsLibraryScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/trackers") {
                    SettingsTrackersScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/system") {
                    SettingsSystemScreen(onBack = { navController.popBackStack() })
                }

                // ── Profile / stats ────────────────────────────────────────
                composable("profile") {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable("statistics") {
                    StatisticsScreen(onBack = { navController.popBackStack() })
                }
                composable("notifications") {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onMangaClick = { provider, mangaId ->
                            navController.navigate("detail/$provider/$mangaId")
                        },
                    )
                }
                composable("help") {
                    HelpScreen(onBack = { navController.popBackStack() })
                }
                composable("migrate") {
                    MigrateScreen(
                        onBack = { navController.popBackStack() },
                        onComplete = { navController.popBackStack() },
                    )
                }

                // ── Manga detail ───────────────────────────────────────────
                composable(
                    route = "detail/{provider}/{mangaId}",
                    arguments = listOf(
                        navArgument("provider") { type = NavType.StringType },
                        navArgument("mangaId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val prov = backStackEntry.arguments?.getString("provider") ?: ""
                    val mId = backStackEntry.arguments?.getString("mangaId") ?: ""
                    MangaDetailScreen(
                        provider = prov,
                        mangaId = mId,
                        onReadChapter = { p, m, chId ->
                            navController.navigate("reader/$p/$m/$chId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Reader ─────────────────────────────────────────────────
                composable(
                    route = "reader/{provider}/{mangaId}/{chapterId}",
                    arguments = listOf(
                        navArgument("provider") { type = NavType.StringType },
                        navArgument("mangaId") { type = NavType.StringType },
                        navArgument("chapterId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    ReaderScreen(
                        provider = backStackEntry.arguments?.getString("provider") ?: "",
                        mangaId = backStackEntry.arguments?.getString("mangaId") ?: "",
                        chapterId = backStackEntry.arguments?.getString("chapterId") ?: "",
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Novel reader ───────────────────────────────────────────
                composable(
                    route = "novel_reader/{provider}/{novelId}/{chapterId}",
                    arguments = listOf(
                        navArgument("provider") { type = NavType.StringType },
                        navArgument("novelId") { type = NavType.StringType },
                        navArgument("chapterId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    NovelReaderScreen(
                        provider = backStackEntry.arguments?.getString("provider") ?: "",
                        novelId = backStackEntry.arguments?.getString("novelId") ?: "",
                        chapterId = backStackEntry.arguments?.getString("chapterId") ?: "",
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Local file detail ──────────────────────────────────────
                composable(
                    route = "local_file/{fileUri}",
                    arguments = listOf(navArgument("fileUri") { type = NavType.StringType }),
                ) { backStackEntry ->
                    LocalFileDetailScreen(
                        fileUri = backStackEntry.arguments?.getString("fileUri") ?: "",
                        onBack = { navController.popBackStack() },
                        onReadChapter = { chapterId ->
                            navController.navigate("reader/local/${backStackEntry.arguments?.getString("fileUri") ?: ""}/$chapterId")
                        },
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
    val haptic = rememberHapticClick()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MangaDlColors.NavBg),
    ) {
        // Top border line
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x1AFFFFFF))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { haptic(); onTabClick(tab.route) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MangaDlColors.Primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            ),
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
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
