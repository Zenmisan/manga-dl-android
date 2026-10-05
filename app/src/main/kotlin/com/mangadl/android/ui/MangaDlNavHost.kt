package com.mangadl.android.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.ui.ContinueItem
import com.mangadl.android.data.ui.UiTracker
import com.mangadl.android.data.ui.toHistoryItems
import com.mangadl.android.data.ui.toUiExtension
import com.mangadl.android.data.ui.toUiManga
import com.mangadl.android.data.ui.toUiSource
import com.mangadl.android.data.ui.toUpdateGroups
import com.mangadl.android.data.ui.toNotices
import com.mangadl.android.data.ui.toUiDownloadItem
import com.mangadl.android.ui.viewmodels.DownloadViewModel
import com.mangadl.android.ui.viewmodels.NotificationsViewModel
import com.mangadl.android.ui.viewmodels.HistoryViewModel
import com.mangadl.android.ui.viewmodels.LibraryViewModel
import com.mangadl.android.ui.viewmodels.SearchViewModel
import com.mangadl.android.ui.viewmodels.UpdatesViewModel
import com.mangadl.android.ui.components.MainTab
import com.mangadl.android.ui.components.MdBottomNav
import com.mangadl.android.ui.components.MdNavRail
import com.mangadl.android.ui.screens.auth.CreateAccountScreen
import com.mangadl.android.ui.screens.auth.ForgotPasswordScreen
import com.mangadl.android.ui.screens.auth.OnboardingScreen
import com.mangadl.android.ui.screens.auth.SignInScreen
import com.mangadl.android.ui.screens.auth.WelcomeScreen
import com.mangadl.android.ui.screens.browse.BrowseScreen
import com.mangadl.android.ui.screens.browse.BrowseSourceScreen
import com.mangadl.android.ui.screens.detail.MangaDetailScreen
import com.mangadl.android.ui.screens.downloads.DownloadsScreen
import com.mangadl.android.ui.screens.help.HelpScreen
import com.mangadl.android.ui.screens.history.HistoryScreen
import com.mangadl.android.ui.screens.library.LibraryScreen
import com.mangadl.android.ui.screens.localfile.LocalFileDetailScreen
import com.mangadl.android.ui.screens.migrate.MigrateScreen
import com.mangadl.android.ui.screens.more.MoreDestination
import com.mangadl.android.ui.screens.more.MoreScreen
import com.mangadl.android.ui.screens.notifications.NotificationsScreen
import com.mangadl.android.ui.screens.profile.ProfileScreen
import com.mangadl.android.ui.screens.reader.NovelReaderScreen
import com.mangadl.android.ui.screens.reader.ReaderScreen
import com.mangadl.android.ui.screens.search.GlobalSearchScreen
import com.mangadl.android.ui.screens.settings.AccountSettingsScreen
import com.mangadl.android.ui.screens.settings.GeneralSettingsScreen
import com.mangadl.android.ui.screens.settings.LibrarySettingsScreen
import com.mangadl.android.ui.screens.settings.ReaderSettingsScreen
import com.mangadl.android.ui.screens.settings.SettingsPage
import com.mangadl.android.ui.screens.settings.SettingsScreen
import com.mangadl.android.ui.screens.settings.SystemSettingsScreen
import com.mangadl.android.ui.screens.settings.TrackerSettingsScreen
import com.mangadl.android.ui.screens.statistics.StatsScreen
import com.mangadl.android.ui.screens.updates.UpdatesScreen
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.theme.MdTheme

object Routes {
    const val Welcome = "welcome"
    const val Onboarding = "onboarding"
    const val Login = "login"
    const val Register = "register"
    const val Forgot = "forgot"
    const val Main = "main"
    const val LibraryEmpty = "library-empty"
    const val Detail = "detail"
    const val Tracking = "tracking"
    const val Reader = "reader"
    const val NovelReader = "novel-reader"
    const val Local = "local"
    const val Search = "search"
    const val Source = "source"
    const val Migrate = "migrate"
    const val Notifications = "notifications"
    const val Downloads = "downloads"
    const val Stats = "stats"
    const val Profile = "profile"
    const val Help = "help"
    const val Settings = "settings"
    const val Account = "settings/account"
    const val General = "settings/general"
    const val ReaderSettings = "settings/reader"
    const val LibrarySettings = "settings/library"
    const val Trackers = "settings/trackers"
    const val System = "settings/system"
}

@Composable
fun MangaDlNavHost(onAccentChange: (Accent) -> Unit, startDestination: String = Routes.Welcome) {
    val nav = rememberNavController()
    val accent = Accent.entries.first { it.main == MdTheme.colors.accent }
    val back: () -> Unit = { nav.popBackStack() }
    val goHome: () -> Unit = {
        nav.navigate(Routes.Main) { popUpTo(0) { inclusive = true } }
    }

    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize().background(MdTheme.colors.bg),
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) },
    ) {
        composable(Routes.Welcome) {
            WelcomeScreen(onStartReading = { nav.navigate(Routes.Onboarding) }, onSignIn = { nav.navigate(Routes.Login) })
        }
        composable(Routes.Onboarding) {
            OnboardingScreen(onBack = back, onContinue = { nav.navigate(Routes.Register) }, onSkip = goHome)
        }
        composable(Routes.Login) {
            SignInScreen(
                onBack = back,
                onSignIn = goHome,
                onForgot = { nav.navigate(Routes.Forgot) },
                onUseWithoutAccount = goHome,
                onCreateAccount = { nav.navigate(Routes.Register) },
            )
        }
        composable(Routes.Register) {
            CreateAccountScreen(onBack = back, onCreate = goHome, onSignIn = { nav.navigate(Routes.Login) })
        }
        composable(Routes.Forgot) { ForgotPasswordScreen(onBack = back) }

        composable(Routes.Main) { MainTabs(nav) }
        composable(Routes.LibraryEmpty) { MainTabs(nav, emptyLibrary = true) }

        composable(Routes.Detail) {
            MangaDetailScreen(
                manga = com.mangadl.android.data.ui.Manga(id = "demo", title = "Demo Manga", cover = androidx.compose.ui.graphics.Color(0xFF3A1518), source = "MangaDex"),
                chapters = emptyList(),
                onBack = back,
                onResume = { nav.navigate(Routes.Reader) },
                onOpenChapter = { nav.navigate(Routes.Reader) },
            )
        }
        composable(Routes.Tracking) {
            MangaDetailScreen(
                manga = com.mangadl.android.data.ui.Manga(id = "demo", title = "Demo Manga", cover = androidx.compose.ui.graphics.Color(0xFF3A1518), source = "MangaDex"),
                chapters = emptyList(),
                onBack = back,
                onResume = { nav.navigate(Routes.Reader) },
                onOpenChapter = { nav.navigate(Routes.Reader) },
                initiallyTracking = true,
                trackers = emptyList<UiTracker>(),
            )
        }
        composable(Routes.Reader) {
            ReaderScreen(
                title = "Demo Manga",
                chapterLabel = "Ch. 1 · Chapter title",
                onClose = back,
                onOpenSettings = { nav.navigate(Routes.ReaderSettings) },
            )
        }
        composable(Routes.NovelReader) { NovelReaderScreen("[Novel title]", "Chapter 12 · [Chapter title]", onClose = back) }
        composable(Routes.Local) {
            LocalFileDetailScreen(onBack = back, onRead = { nav.navigate(Routes.Reader) })
        }

        composable(Routes.Search) {
            val searchVm: SearchViewModel = viewModel()
            val searchResults by searchVm.results.collectAsState()
            GlobalSearchScreen(
                onBack = back,
                onOpenManga = { nav.navigate(Routes.Detail) },
                searchResults = searchResults,
                onSearch = { searchVm.search(it) },
            )
        }
        composable(Routes.Source) {
            BrowseSourceScreen("MangaDex", emptyList(), onBack = back, onOpenManga = { nav.navigate(Routes.Detail) })
        }
        composable(Routes.Migrate) { MigrateScreen(onBack = back, onMigrate = back) }

        composable(Routes.Notifications) {
            val notifVm: NotificationsViewModel = viewModel()
            val notifLibrary by notifVm.library.collectAsState()
            val notices = remember(notifLibrary) { notifLibrary.toNotices() }
            NotificationsScreen(notices, onBack = back)
        }
        composable(Routes.Downloads) {
            val dlVm: DownloadViewModel = viewModel()
            val dlItems by dlVm.downloads.collectAsState()
            DownloadsScreen(dlItems.map { it.toUiDownloadItem() }, onBack = back)
        }
        composable(Routes.Stats) { StatsScreen(onBack = back) }
        composable(Routes.Profile) { ProfileScreen(onBack = back, onEditProfile = { nav.navigate(Routes.Account) }) }
        composable(Routes.Help) { HelpScreen(onBack = back) }

        composable(Routes.Settings) {
            SettingsScreen(onBack = back, onOpen = { page ->
                nav.navigate(
                    when (page) {
                        SettingsPage.Account -> Routes.Account
                        SettingsPage.General -> Routes.General
                        SettingsPage.Reader -> Routes.ReaderSettings
                        SettingsPage.Library -> Routes.LibrarySettings
                        SettingsPage.Trackers -> Routes.Trackers
                        SettingsPage.System -> Routes.System
                    },
                )
            })
        }
        composable(Routes.Account) {
            val goWelcome: () -> Unit = { nav.navigate(Routes.Welcome) { popUpTo(0) { inclusive = true } } }
            AccountSettingsScreen(onBack = back, onSignOut = goWelcome, onDeleteAccount = goWelcome)
        }
        composable(Routes.General) { GeneralSettingsScreen(onBack = back, accent = accent, onAccentChange = onAccentChange) }
        composable(Routes.ReaderSettings) { ReaderSettingsScreen(onBack = back) }
        composable(Routes.LibrarySettings) { LibrarySettingsScreen(onBack = back, onMigrate = { nav.navigate(Routes.Migrate) }) }
        composable(Routes.Trackers) { TrackerSettingsScreen(onBack = back) }

        composable(Routes.System) { SystemSettingsScreen(onBack = back) }
    }
}

@Composable
private fun MainTabs(nav: NavHostController, emptyLibrary: Boolean = false) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Library) }

    val libraryVm: LibraryViewModel = viewModel()
    val updatesVm: UpdatesViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()

    val libraryItems by libraryVm.library.collectAsState()
    val updateItems by updatesVm.library.collectAsState()
    val historyProgress by historyVm.allProgress.collectAsState()
    val historyLibrary by historyVm.library.collectAsState()

    val extMgr = remember { MangaDlApp.instance.extensionManager }
    val extensions = remember { extMgr.listExtensions() }
    val uiSources = remember(extensions) { extensions.map { it.toUiSource() } }
    val uiExtensions = remember(extensions) { extensions.map { it.toUiExtension() } }

    val uiLibrary = remember(libraryItems) { libraryItems.map { it.toUiManga() } }
    val continueItem = remember(libraryItems) {
        libraryItems.maxByOrNull { it.lastReadAt ?: 0L }
            ?.let { m -> ContinueItem(m.toUiManga(), "Ch. ${m.readCount + 1}", 0f) }
    }
    val updateGroups = remember(updateItems) { updateItems.toUpdateGroups() }
    val historyItems = remember(historyProgress, historyLibrary) {
        historyProgress.toHistoryItems(historyLibrary)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MdTheme.colors.bg)) {
        val wide = maxWidth >= 600.dp
        val content: @Composable () -> Unit = {
            when (tab) {
                MainTab.Library -> LibraryScreen(
                    items = uiLibrary,
                    continueItem = continueItem,
                    onOpenManga = { nav.navigate(Routes.Detail) },
                    onResume = { nav.navigate(Routes.Reader) },
                    onBrowse = { tab = MainTab.Browse },
                    onImport = { nav.navigate(Routes.Local) },
                )
                MainTab.Updates -> UpdatesScreen(updateGroups, onOpenChapter = { nav.navigate(Routes.Reader) })
                MainTab.History -> HistoryScreen(historyItems, onResume = { nav.navigate(Routes.Reader) })
                MainTab.Browse -> BrowseScreen(
                    onOpenSource = { nav.navigate(Routes.Source) },
                    onSearch = { nav.navigate(Routes.Search) },
                    onMigrate = { nav.navigate(Routes.Migrate) },
                    sources = uiSources,
                    extensions = uiExtensions,
                )
                MainTab.More -> MoreScreen(
                    onProfile = { nav.navigate(Routes.Profile) },
                    onOpen = { d ->
                        nav.navigate(
                            when (d) {
                                MoreDestination.Downloads -> Routes.Downloads
                                MoreDestination.Notifications -> Routes.Notifications
                                MoreDestination.Statistics -> Routes.Stats
                                MoreDestination.Import -> Routes.Local
                                MoreDestination.Backup -> Routes.System
                                MoreDestination.Settings -> Routes.Settings
                                MoreDestination.Help -> Routes.Help
                                MoreDestination.AllScreens -> Routes.Settings
                            },
                        )
                    },
                )
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                MdNavRail(tab, { tab = it })
                Box(Modifier.weight(1f).statusBarsPadding().navigationBarsPadding()) { content() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).statusBarsPadding()) { content() }
                MdBottomNav(tab, { tab = it })
            }
        }
    }
}

