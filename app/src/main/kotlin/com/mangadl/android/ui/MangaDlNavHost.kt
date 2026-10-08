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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import com.mangadl.android.data.ui.DownloadState
import com.mangadl.android.ui.viewmodels.BrowseSourceViewModel
import com.mangadl.android.ui.viewmodels.DetailState
import com.mangadl.android.ui.viewmodels.DownloadQueueViewModel
import com.mangadl.android.ui.viewmodels.DownloadViewModel
import com.mangadl.android.ui.viewmodels.MangaDetailViewModel
import com.mangadl.android.ui.viewmodels.NavStateHolder
import com.mangadl.android.ui.viewmodels.NotificationsViewModel
import com.mangadl.android.ui.viewmodels.HistoryViewModel
import com.mangadl.android.ui.viewmodels.LibraryViewModel
import com.mangadl.android.ui.viewmodels.NovelReaderState
import com.mangadl.android.ui.viewmodels.NovelReaderViewModel
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
    const val Sources = "sources"
    const val Leaderboard = "leaderboard"
}

@Composable
fun MangaDlNavHost(onAccentChange: (Accent) -> Unit, startDestination: String = Routes.Welcome) {
    val nav = rememberNavController()
    val accent = Accent.entries.first { it.main == MdTheme.colors.accent }
    val back: () -> Unit = { nav.popBackStack() }
    val goHome: () -> Unit = {
        nav.navigate(Routes.Main) { popUpTo(0) { inclusive = true } }
    }
    val navState: NavStateHolder = viewModel()

    var selectedSource by remember { mutableStateOf<com.mangadl.android.data.ui.UiSource?>(null) }

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
            WelcomeScreen(onStartReading = { nav.navigate(Routes.Register) }, onSignIn = { nav.navigate(Routes.Login) })
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

        composable(Routes.Main) { MainTabs(nav, onSourceSelected = { selectedSource = it }) }
        composable(Routes.LibraryEmpty) { MainTabs(nav, emptyLibrary = true, onSourceSelected = { selectedSource = it }) }

        composable(Routes.Detail) {
            val detailVm: MangaDetailViewModel = viewModel()
            val dlQueueVm: com.mangadl.android.ui.viewmodels.DownloadQueueViewModel = viewModel()
            val detailState by detailVm.state.collectAsState()
            val inLibrary by detailVm.inLibrary.collectAsState()
            val allCategories by detailVm.allCategories.collectAsState()
            val mangaCategories by detailVm.mangaCategories.collectAsState()

            LaunchedEffect(navState.mangaId, navState.sourceId) {
                if (navState.mangaId.isNotEmpty()) detailVm.load(navState.sourceId, navState.mangaId)
            }

            val openReader: (String, String) -> Unit = { chapterId, chapterLabel ->
                navState.chapterId = chapterId
                navState.chapterLabel = chapterLabel
                navState.isLocalRead = false
                nav.navigate(Routes.Reader)
            }

            when (val s = detailState) {
                is DetailState.Loading -> {
                    Box(Modifier.fillMaxSize().background(MdTheme.colors.bg), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MdTheme.colors.accent)
                    }
                }
                is DetailState.Error -> {
                    Box(Modifier.fillMaxSize().background(MdTheme.colors.bg), contentAlignment = Alignment.Center) {
                        Text(s.message, color = MdTheme.colors.fgMuted)
                    }
                }
                is DetailState.Success -> {
                    val detail = s.detail
                    val coverColor = androidx.compose.ui.graphics.Color(0xFF3A1518)
                    val manga = com.mangadl.android.data.ui.Manga(
                        id = detail.id, title = detail.title,
                        cover = coverColor, coverUrl = detail.coverUrl, source = detail.provider, inLibrary = inLibrary,
                    )
                    val firstUnread = detail.chapters.firstOrNull { ch ->
                        true
                    }
                    MangaDetailScreen(
                        manga = manga,
                        chapters = detail.chapters.map { ch ->
                            com.mangadl.android.data.ui.UiChapter(
                                number = if (ch.number > 0) ch.number.toString().trimEnd('0').trimEnd('.') else "",
                                title = ch.title,
                                meta = ch.publishedAt,
                            )
                        },
                        categories = allCategories,
                        assignedCategoryIds = mangaCategories,
                        onUpdateCategories = { detailVm.setCategories(it) },
                        onCreateCategory = { detailVm.createCategory(it) },
                        onBack = back,
                        onResume = {
                            firstUnread?.let { ch ->
                                navState.chapterId = ch.id
                                navState.chapterLabel = "Ch. ${ch.number} · ${ch.title}"
                                navState.isLocalRead = false
                            }
                            navState.chapters = detail.chapters
                            if (com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(detail.provider)) {
                                nav.navigate(Routes.NovelReader)
                            } else {
                                nav.navigate(Routes.Reader)
                            }
                        },
                        onOpenChapter = { uiCh ->
                            val matching = detail.chapters.find { ch ->
                                (if (ch.number > 0) ch.number.toString().trimEnd('0').trimEnd('.') else "") == uiCh.number
                            }
                            matching?.let { ch ->
                                navState.chapterId = ch.id
                                navState.chapterLabel = "Ch. ${ch.number} · ${ch.title}"
                                navState.isLocalRead = false
                            }
                            navState.chapters = detail.chapters
                            if (com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(detail.provider)) {
                                nav.navigate(Routes.NovelReader)
                            } else {
                                nav.navigate(Routes.Reader)
                            }
                        },
                        onToggleLibrary = { detailVm.toggleLibrary(detail) },
                        onDownloadChapter = { uiCh ->
                            val matching = detail.chapters.find { ch ->
                                (if (ch.number > 0) ch.number.toString().trimEnd('0').trimEnd('.') else "") == uiCh.number
                            }
                            matching?.let { ch ->
                                dlQueueVm.enqueue(
                                    mangaId = detail.id,
                                    mangaTitle = detail.title,
                                    chapterId = ch.id,
                                    chapterTitle = ch.title.ifEmpty { "Chapter ${ch.number}" },
                                    provider = detail.provider,
                                )
                            }
                        },
                        onDownloadAllChapters = {
                            dlQueueVm.enqueueBatch(
                                mangaId = detail.id,
                                mangaTitle = detail.title,
                                provider = detail.provider,
                                chapters = detail.chapters,
                            )
                        },
                        genres = detail.genres,
                        synopsis = detail.description,
                        authors = detail.authors,
                        resumeLabel = firstUnread?.let { ch ->
                            if (ch.number > 0) "Read Ch. ${ch.number.toString().trimEnd('0').trimEnd('.')}" else "Start Reading"
                        } ?: "Start Reading",
                    )
                }
            }
        }
        composable(Routes.Tracking) {
            val detailVm: MangaDetailViewModel = viewModel()
            val detailState by detailVm.state.collectAsState()
            val inLibrary by detailVm.inLibrary.collectAsState()
            LaunchedEffect(navState.mangaId) {
                if (navState.mangaId.isNotEmpty()) detailVm.load(navState.sourceId, navState.mangaId)
            }
            val detail = (detailState as? DetailState.Success)?.detail
            MangaDetailScreen(
                manga = com.mangadl.android.data.ui.Manga(
                    id = detail?.id ?: navState.mangaId,
                    title = detail?.title ?: navState.mangaTitle,
                    cover = androidx.compose.ui.graphics.Color(0xFF3A1518),
                    coverUrl = detail?.coverUrl,
                    source = detail?.provider ?: navState.sourceId,
                    inLibrary = inLibrary,
                ),
                chapters = detail?.chapters?.map { ch ->
                    com.mangadl.android.data.ui.UiChapter(
                        number = if (ch.number > 0) ch.number.toString().trimEnd('0').trimEnd('.') else "",
                        title = ch.title, meta = ch.publishedAt,
                    )
                } ?: emptyList(),
                onBack = back,
                onResume = {
                    if (com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(detail?.provider ?: navState.sourceId)) {
                        nav.navigate(Routes.NovelReader)
                    } else {
                        nav.navigate(Routes.Reader)
                    }
                },
                onOpenChapter = {
                    if (com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(detail?.provider ?: navState.sourceId)) {
                        nav.navigate(Routes.NovelReader)
                    } else {
                        nav.navigate(Routes.Reader)
                    }
                },
                initiallyTracking = true,
                trackers = emptyList<UiTracker>(),
            )
        }
        composable(Routes.Reader) {
            val chapters = navState.chapters
            val currentIndex = chapters.indexOfFirst { it.id == navState.chapterId }
            val hasPrev = currentIndex > 0
            val hasNext = currentIndex in 0 until (chapters.size - 1)

            ReaderScreen(
                onClose = back,
                hasPrev = hasPrev,
                hasNext = hasNext,
                onPrevChapter = {
                    if (hasPrev) {
                        val prevCh = chapters[currentIndex - 1]
                        navState.chapterId = prevCh.id
                        navState.chapterLabel = if (prevCh.number > 0) "Ch. ${prevCh.number.toString().trimEnd('0').trimEnd('.')} · ${prevCh.title}" else prevCh.title
                    }
                },
                onNextChapter = {
                    if (hasNext) {
                        val nextCh = chapters[currentIndex + 1]
                        navState.chapterId = nextCh.id
                        navState.chapterLabel = if (nextCh.number > 0) "Ch. ${nextCh.number.toString().trimEnd('0').trimEnd('.')} · ${nextCh.title}" else nextCh.title
                    }
                },
                onOpenSettings = { nav.navigate(Routes.ReaderSettings) },
            )
        }
        composable(Routes.NovelReader) {
            val novelVm: NovelReaderViewModel = viewModel()
            val novelState by novelVm.state.collectAsState()

            LaunchedEffect(navState.sourceId, navState.mangaId, navState.chapterId) {
                if (navState.chapterId.isNotEmpty() && navState.sourceId.isNotEmpty()) {
                    novelVm.loadChapter(
                        provider = navState.sourceId,
                        novelId = navState.mangaId,
                        chapterId = navState.chapterId,
                        chapterTitle = navState.chapterLabel,
                    )
                }
            }

            val chapters = navState.chapters
            val currentIndex = chapters.indexOfFirst { it.id == navState.chapterId }
            val hasPrev = currentIndex > 0
            val hasNext = currentIndex in 0 until (chapters.size - 1)

            val paragraphs = (novelState as? NovelReaderState.Success)?.paragraphs ?: emptyList()
            val loading = novelState is NovelReaderState.Loading
            val error = (novelState as? NovelReaderState.Error)?.message

            NovelReaderScreen(
                novelTitle = navState.mangaTitle,
                chapterLabel = navState.chapterLabel,
                paragraphs = paragraphs,
                loading = loading,
                error = error,
                hasPrev = hasPrev,
                hasNext = hasNext,
                chapters = chapters,
                onClose = back,
                onSelectChapter = { ch ->
                    navState.chapterId = ch.id
                    navState.chapterLabel = if (ch.number > 0) "Ch. ${ch.number.toString().trimEnd('0').trimEnd('.')} · ${ch.title}" else ch.title
                },
                onPrevChapter = {
                    if (hasPrev) {
                        val prevCh = chapters[currentIndex - 1]
                        navState.chapterId = prevCh.id
                        navState.chapterLabel = if (prevCh.number > 0) "Ch. ${prevCh.number.toString().trimEnd('0').trimEnd('.')} · ${prevCh.title}" else prevCh.title
                    }
                },
                onNextChapter = {
                    if (hasNext) {
                        val nextCh = chapters[currentIndex + 1]
                        navState.chapterId = nextCh.id
                        navState.chapterLabel = if (nextCh.number > 0) "Ch. ${nextCh.number.toString().trimEnd('0').trimEnd('.')} · ${nextCh.title}" else nextCh.title
                    }
                },
                onProgressChange = { pct ->
                    val chNum = if (currentIndex in 0 until chapters.size) chapters[currentIndex].number else 0f
                    novelVm.saveProgress(pct, chNum)
                },
            )
        }
        composable(Routes.Local) {
            LocalFileDetailScreen(
                onBack = back,
                onRead = { pages ->
                    navState.localPages = pages
                    navState.isLocalRead = true
                    navState.chapterId = "local"
                    navState.chapterLabel = "Local file"
                    nav.navigate(Routes.Reader)
                },
            )
        }

        composable(Routes.Search) {
            val searchVm: SearchViewModel = viewModel()
            val searchResults by searchVm.results.collectAsState()
            GlobalSearchScreen(
                onBack = back,
                onOpenManga = { m ->
                    navState.mangaId = m.id
                    navState.sourceId = m.source
                    navState.mangaTitle = m.title
                    nav.navigate(Routes.Detail)
                },
                searchResults = searchResults,
                onSearch = { searchVm.search(it) },
            )
        }
        composable(Routes.Source) {
            val browseVm: BrowseSourceViewModel = viewModel()
            val browseItems by browseVm.items.collectAsState()
            val browseLoading by browseVm.loading.collectAsState()
            val browseError by browseVm.error.collectAsState()
            val currentTab by browseVm.currentTab.collectAsState()
            val src = selectedSource
            LaunchedEffect(src?.id) {
                if (src != null) browseVm.load(src.id)
            }
            BrowseSourceScreen(
                sourceName = src?.name ?: "Source",
                items = browseItems,
                loading = browseLoading,
                error = browseError,
                currentTab = currentTab,
                onTabChange = { tab -> if (src != null) browseVm.setTab(src.id, tab) },
                onSearch = { q -> if (src != null) browseVm.search(src.id, q) },
                onLoadMore = { if (src != null) browseVm.loadMore(src.id) },
                onBack = back,
                onOpenManga = { m ->
                    navState.mangaId = m.id
                    navState.sourceId = m.source
                    navState.mangaTitle = m.title
                    nav.navigate(Routes.Detail)
                },
            )
        }
        composable(Routes.Migrate) { MigrateScreen(onBack = back, onMigrate = back) }

        composable(Routes.Notifications) {
            val notifVm: NotificationsViewModel = viewModel()
            val notifLibrary by notifVm.library.collectAsState()
            val notices = remember(notifLibrary) { notifLibrary.toNotices() }
            NotificationsScreen(notices, onBack = back)
        }
        composable(Routes.Downloads) {
            val dlQueueVm: DownloadQueueViewModel = viewModel()
            val dlItems by dlQueueVm.downloads.collectAsState()
            val isPaused by dlQueueVm.isQueuePaused.collectAsState()
            val storageBytes by dlQueueVm.storageUsedBytes.collectAsState()
            DownloadsScreen(
                items = dlItems.map { it.toUiDownloadItem() },
                onBack = back,
                isPaused = isPaused,
                storageBytes = storageBytes,
                onTogglePauseAll = { dlQueueVm.toggleQueuePaused() },
                onItemAction = { item ->
                    when (item.state) {
                        DownloadState.Failed -> dlQueueVm.retry(item.id)
                        DownloadState.Paused -> dlQueueVm.resume(item.id)
                        DownloadState.Done -> dlQueueVm.remove(item.id)
                        else -> dlQueueVm.pause(item.id)
                    }
                }
            )
        }
        composable(Routes.Stats) { StatsScreen(onBack = back) }
        composable(Routes.Profile) {
            ProfileScreen(
                onBack = back,
                onEditProfile = { nav.navigate(Routes.Account) },
                onOpenLeaderboard = { nav.navigate(Routes.Leaderboard) },
            )
        }
        composable(Routes.Leaderboard) { com.mangadl.android.ui.screens.leaderboard.LeaderboardScreen(onBack = back) }
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
        composable(Routes.Sources) { com.mangadl.android.ui.screens.sources.SourcesScreen(onBack = back) }
    }
}

@Composable
private fun MainTabs(nav: NavHostController, emptyLibrary: Boolean = false, onSourceSelected: (com.mangadl.android.data.ui.UiSource) -> Unit = {}) {
    val navState: NavStateHolder = viewModel()
    var tab by rememberSaveable { mutableStateOf(MainTab.Library) }

    val libraryVm: LibraryViewModel = viewModel()
    val updatesVm: UpdatesViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()
    val settingsVm: com.mangadl.android.ui.viewmodels.SettingsViewModel = viewModel()

    val gridColumns by settingsVm.gridColumns.collectAsState()
    val showUnreadBadges by settingsVm.showUnreadBadges.collectAsState()
    val showDownloadedBadges by settingsVm.showDownloadedBadges.collectAsState()
    val libraryDownloadedOnly by settingsVm.libraryDownloadedOnly.collectAsState()

    val libraryItems by libraryVm.library.collectAsState()
    val categories by libraryVm.categories.collectAsState()
    val selectedCategoryId by libraryVm.selectedCategoryId.collectAsState()
    val mangaCategoryMap by libraryVm.mangaCategoryMap.collectAsState()
    val newChapters by updatesVm.newChapters.collectAsState()
    val lastChecked by updatesVm.lastChecked.collectAsState()
    val historyProgress by historyVm.allProgress.collectAsState()
    val historyLibrary by historyVm.library.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appPrefs = remember { com.mangadl.android.data.prefs.AppPreferences.getInstance(context) }
    val pinnedSources by appPrefs.pinnedSources.collectAsState(emptySet())

    val extMgr = remember { MangaDlApp.instance.extensionManager }
    val repoMgr = remember { MangaDlApp.instance.extensionRepoManager }
    val isRepoLoading by repoMgr.isLoading.collectAsState()
    val rawRepoExtensions by repoMgr.repoExtensions.collectAsState()

    val extensions = remember { extMgr.listExtensions() }
    val uiSources = remember(extensions) { extensions.map { it.toUiSource() } }
    val installedExtensions = remember(extensions) { extensions.map { it.toUiExtension() } }

    var availableRepoExtensions by remember { mutableStateOf<List<com.mangadl.android.data.ui.UiExtension>>(emptyList()) }
    LaunchedEffect(rawRepoExtensions, installedExtensions) {
        availableRepoExtensions = repoMgr.getAvailableExtensions(installedExtensions)
    }

    val allExtensions = remember(installedExtensions, availableRepoExtensions) {
        installedExtensions + availableRepoExtensions
    }

    val uiLibrary = remember(libraryItems) { libraryItems.map { it.toUiManga() } }
    val continueItem = remember(libraryItems) {
        libraryItems.maxByOrNull { it.lastReadAt ?: 0L }
            ?.let { m -> ContinueItem(m.toUiManga(), "Ch. ${m.readCount + 1}", 0f) }
    }
    val updateGroups = remember(newChapters) { newChapters.toUpdateGroups() }
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
                    gridColumns = gridColumns,
                    showUnreadBadges = showUnreadBadges,
                    showDownloadedBadges = showDownloadedBadges,
                    downloadedOnly = libraryDownloadedOnly,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { libraryVm.selectCategory(it) },
                    onCreateCategory = { libraryVm.createCategory(it) },
                    onDeleteCategory = { libraryVm.deleteCategory(it) },
                    onRenameCategory = { id, name -> libraryVm.renameCategory(id, name) },
                    mangaCategoryMap = mangaCategoryMap,
                    onOpenManga = { m ->
                        navState.mangaId = m.id
                        navState.sourceId = m.source
                        navState.mangaTitle = m.title
                        nav.navigate(Routes.Detail)
                    },
                    onResume = { m ->
                        navState.mangaId = m.id
                        navState.sourceId = m.source
                        navState.mangaTitle = m.title
                        nav.navigate(Routes.Detail)
                    },
                    onBrowse = { tab = MainTab.Browse },
                    onImport = { nav.navigate(Routes.Local) },
                )
                MainTab.Updates -> {
                    val dlQueueVm: DownloadQueueViewModel = viewModel()
                    UpdatesScreen(
                        groups = updateGroups,
                        lastChecked = lastChecked,
                        onOpenChapter = { item ->
                            navState.mangaId = item.manga.id
                            navState.sourceId = item.manga.source
                            navState.mangaTitle = item.manga.title
                            if (item.chapterId.isNotBlank()) {
                                navState.chapterId = item.chapterId
                                navState.chapterLabel = item.chapter
                                if (com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(item.manga.source)) {
                                    nav.navigate(Routes.NovelReader)
                                } else {
                                    nav.navigate(Routes.Reader)
                                }
                            } else {
                                nav.navigate(Routes.Detail)
                            }
                        },
                        onDownloadChapter = { item ->
                            if (item.chapterId.isNotBlank()) {
                                dlQueueVm.enqueue(
                                    mangaId = item.manga.id,
                                    mangaTitle = item.manga.title,
                                    chapterId = item.chapterId,
                                    chapterTitle = item.chapter,
                                    provider = item.manga.source,
                                )
                            }
                        },
                        onRefresh = { updatesVm.refresh() },
                    )
                }
                MainTab.History -> HistoryScreen(
                    items = historyItems,
                    onResume = { item ->
                        navState.mangaId = item.manga.id
                        navState.sourceId = item.manga.source
                        navState.mangaTitle = item.manga.title
                        nav.navigate(Routes.Detail)
                    },
                    onClearHistory = { historyVm.clearHistory() },
                )
                MainTab.Browse -> BrowseScreen(
                    onOpenSource = { src -> onSourceSelected(src); nav.navigate(Routes.Source) },
                    onSearch = { nav.navigate(Routes.Search) },
                    onMigrate = { nav.navigate(Routes.Migrate) },
                    sources = uiSources,
                    extensions = allExtensions,
                    pinnedSources = pinnedSources,
                    onTogglePin = { srcId ->
                        val updated = if (srcId in pinnedSources) pinnedSources - srcId else pinnedSources + srcId
                        coroutineScope.launch {
                            appPrefs.set(com.mangadl.android.data.prefs.PrefKeys.PINNED_SOURCES, updated)
                        }
                    },
                    onInstallExtension = { ext ->
                        coroutineScope.launch {
                            repoMgr.installExtension(context, ext)
                        }
                    },
                    onUpdateExtension = { ext ->
                        coroutineScope.launch {
                            repoMgr.installExtension(context, ext)
                        }
                    },
                    onRefreshRepo = {
                        coroutineScope.launch {
                            repoMgr.refresh()
                        }
                    },
                    isRepoLoading = isRepoLoading,
                )
                MainTab.More -> MoreScreen(
                    onProfile = { nav.navigate(Routes.Profile) },
                    onOpen = { d ->
                        nav.navigate(
                            when (d) {
                                MoreDestination.Downloads -> Routes.Downloads
                                MoreDestination.Notifications -> Routes.Notifications
                                MoreDestination.Leaderboard -> Routes.Leaderboard
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

