package com.mangadl.android.ui.screens.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSlider
import com.mangadl.android.ui.components.PagePlaceholder
import com.mangadl.android.ui.components.ZoomableBox
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.NavStateHolder
import com.mangadl.android.ui.viewmodels.ReaderState
import com.mangadl.android.ui.viewmodels.ReaderViewModel
import kotlinx.coroutines.launch

enum class ReadingMode(val label: String) { LTR("LTR pager"), RTL("RTL pager"), Vertical("Vertical"), Webtoon("Webtoon") }

@Composable
fun ReaderScreen(
    onClose: () -> Unit,
    hasPrev: Boolean = true,
    hasNext: Boolean = true,
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    initialMode: ReadingMode = ReadingMode.RTL,
    initiallyShowControls: Boolean = true,
) {
    val context = LocalContext.current
    val navState: NavStateHolder = viewModel()
    val vm: ReaderViewModel = viewModel()
    val readerState by vm.state.collectAsState()

    val appPrefs = remember { AppPreferences.getInstance(context) }
    val savedDirection by appPrefs.readerDirection.collectAsState(initial = initialMode.name.lowercase())
    val savedCropBorders by appPrefs.cropBorders.collectAsState(initial = false)
    val keepScreenOnPref by appPrefs.keepScreenOn.collectAsState(initial = true)
    val savedTapZones by appPrefs.tapZones.collectAsState(initial = "default")
    val savedShowPageNumber by appPrefs.showPageNumber.collectAsState(initial = true)
    val savedFullScreen by appPrefs.fullScreen.collectAsState(initial = true)
    val savedSidePadding by appPrefs.sidePadding.collectAsState(initial = "0")
    val savedVolumeKeys by appPrefs.volumeKeysTurnPages.collectAsState(initial = true)
    val savedReaderBackground by appPrefs.readerBackground.collectAsState(initial = "black")
    val savedHaptic by appPrefs.hapticFeedback.collectAsState(initial = true)
    val savedDualPage by appPrefs.dualPageSpread.collectAsState(initial = "off")
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(navState.chapterId, navState.isLocalRead) {
        if (navState.isLocalRead && navState.localPages.isNotEmpty()) {
            vm.loadLocal(navState.localPages)
        } else if (navState.chapterId.isNotEmpty() && navState.sourceId.isNotEmpty()) {
            vm.load(navState.sourceId, navState.chapterId)
        }
    }

    val c = MdTheme.colors
    val scope = rememberCoroutineScope()
    var controls by rememberState(initiallyShowControls)
    var mode by rememberState(initialMode)
    var cropBorders by rememberState(false)
    var bookmarked by rememberState(false)
    var showMoreOptions by rememberState(false)
    var brightness by rememberState(1f)

    LaunchedEffect(savedDirection) {
        when (savedDirection.lowercase()) {
            "ltr" -> mode = ReadingMode.LTR
            "rtl" -> mode = ReadingMode.RTL
            "vertical" -> mode = ReadingMode.Vertical
            "webtoon" -> mode = ReadingMode.Webtoon
        }
    }

    LaunchedEffect(savedCropBorders) {
        cropBorders = savedCropBorders
    }

    DisposableEffect(keepScreenOnPref) {
        val activity = context as? Activity
        if (keepScreenOnPref) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(savedFullScreen) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        if (savedFullScreen && controller != null) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    val onModeChange: (ReadingMode) -> Unit = { newMode ->
        mode = newMode
        scope.launch {
            appPrefs.set(PrefKeys.READER_DIRECTION, newMode.name.lowercase())
        }
    }

    val pages = (readerState as? ReaderState.Success)?.pages ?: emptyList()
    val pageCount = pages.size.coerceAtLeast(1)

    val pager = rememberPagerState(initialPage = 0) { pageCount }

    val onPageTap: (Float) -> Unit = { xFraction ->
        if (savedTapZones == "disabled") {
            controls = !controls
        } else {
            val forward = if (mode == ReadingMode.RTL) xFraction < 0.3f else xFraction > 0.7f
            val backward = if (mode == ReadingMode.RTL) xFraction > 0.7f else xFraction < 0.3f
            when {
                forward -> {
                    val target = (pager.currentPage + 1).coerceAtMost(pageCount - 1)
                    if (target != pager.currentPage) scope.launch { pager.animateScrollToPage(target) }
                }
                backward -> {
                    val target = (pager.currentPage - 1).coerceAtLeast(0)
                    if (target != pager.currentPage) scope.launch { pager.animateScrollToPage(target) }
                }
                else -> controls = !controls
            }
        }
    }

    LaunchedEffect(pager.currentPage, pages.size) {
        if (pages.isNotEmpty()) {
            val chNum = navState.chapters.find { it.id == navState.chapterId }?.number ?: 0f
            vm.saveProgress(navState.mangaId, navState.chapterId, navState.sourceId, pager.currentPage, pages.size, chNum)
        }
    }

    LaunchedEffect(pager.currentPage) {
        if (savedHaptic) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    DisposableEffect(savedVolumeKeys, mode, pageCount) {
        ReaderKeyEvents.onVolumeKey = { isVolumeUp ->
            if (!savedVolumeKeys) {
                false
            } else {
                // Volume-up moves "backward" and volume-down "forward" in reading order; reversed
                // for RTL since the pager's own page order is already reversed there.
                val forward = if (mode == ReadingMode.RTL) isVolumeUp else !isVolumeUp
                val target = (pager.currentPage + if (forward) 1 else -1).coerceIn(0, pageCount - 1)
                if (target != pager.currentPage) scope.launch { pager.animateScrollToPage(target) }
                true
            }
        }
        onDispose { ReaderKeyEvents.onVolumeKey = null }
    }

    val readerBg = when (savedReaderBackground) {
        "gray" -> Color(0xFF1C1C1C)
        "white" -> Color.White
        else -> c.readerBg
    }

    Box(Modifier.fillMaxSize().background(readerBg)) {
        when {
            readerState is ReaderState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
            readerState is ReaderState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BodyText((readerState as ReaderState.Error).message, color = c.fgMuted)
                }
            }
            else -> when (mode) {
                ReadingMode.LTR, ReadingMode.RTL -> {
                    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val dualPageActive = savedDualPage != "off" && isLandscape
                    HorizontalPager(
                        state = pager,
                        reverseLayout = mode == ReadingMode.RTL,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        if (dualPageActive && page + 1 < pages.size) {
                            val order = if (mode == ReadingMode.RTL) listOf(page + 1, page) else listOf(page, page + 1)
                            Row(Modifier.fillMaxSize()) {
                                order.forEach { idx ->
                                    MangaPage(
                                        url = pages.getOrElse(idx) { "" },
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        cropBorders = cropBorders,
                                        onTap = onPageTap,
                                    )
                                }
                            }
                        } else {
                            MangaPage(
                                url = pages.getOrElse(page) { "" },
                                modifier = Modifier.fillMaxSize(),
                                cropBorders = cropBorders,
                                onTap = onPageTap,
                            )
                        }
                    }
                }
                ReadingMode.Vertical -> VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    MangaPage(
                        url = pages.getOrElse(page) { "" },
                        modifier = Modifier.fillMaxSize(),
                        cropBorders = cropBorders,
                        onTap = onPageTap,
                    )
                }
                ReadingMode.Webtoon -> {
                    val sidePaddingFraction = ((savedSidePadding.toIntOrNull() ?: 0).coerceIn(0, 80) / 100f) / 2f
                    LazyColumn(
                        Modifier.fillMaxSize(),
                    ) {
                        items(pages.size) { i ->
                            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                                MangaPage(
                                    url = pages[i],
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = maxWidth * sidePaddingFraction),
                                    webtoon = true,
                                    cropBorders = cropBorders,
                                    onTap = onPageTap,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (brightness < 1f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 0.85f)))
            )
        }

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            Column(Modifier.fillMaxWidth().background(c.navBg.copy(alpha = 0.92f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MdIconButton(MdIcons.Back, "Close reader", onClose)
                    Column(Modifier.weight(1f)) {
                        BodyText(navState.mangaTitle, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                        BodyText(navState.chapterLabel, size = 12.sp, color = c.fg.copy(alpha = 0.65f), maxLines = 1)
                    }
                    MdIconButton(
                        if (bookmarked) MdIcons.BookmarkFilled else MdIcons.Bookmark,
                        if (bookmarked) "Remove bookmark" else "Bookmark page",
                        { bookmarked = !bookmarked },
                        tint = if (bookmarked) c.accentLight else c.fg,
                    )
                    MdIconButton(MdIcons.More, "More options", { showMoreOptions = true })
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.dividerStrong))
            }
        }

        ReaderMoreOptionsPanel(
            visible = showMoreOptions,
            mode = mode,
            onModeChange = {
                onModeChange(it)
                showMoreOptions = false
            },
            brightness = brightness,
            onBrightnessChange = { brightness = it },
            onDismiss = { showMoreOptions = false },
        )

        AnimatedVisibility(controls && !showMoreOptions, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(c.navBg.copy(alpha = 0.92f))
                        .border(1.dp, c.dividerStrong, CircleShape)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MdIconButton(
                        MdIcons.PrevChapter,
                        "Previous chapter",
                        { if (hasPrev) onPrevChapter() },
                        background = if (hasPrev) c.surfaceHigh else c.surfaceHigh.copy(alpha = 0.4f),
                        tint = if (hasPrev) c.fg else c.fgMuted,
                        iconSize = 20.dp,
                    )
                    if (savedShowPageNumber) {
                        BodyText("${pager.currentPage + 1}", Modifier.width(24.dp), size = 12.sp, weight = FontWeight.Bold)
                    }
                    MdSlider(
                        value = (pager.currentPage + 1).toFloat(),
                        onValueChange = { v -> scope.launch { pager.scrollToPage(v.toInt() - 1) } },
                        valueRange = 1f..pageCount.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    if (savedShowPageNumber) {
                        BodyText("$pageCount", Modifier.width(24.dp), size = 12.sp, weight = FontWeight.Bold, color = c.fg.copy(alpha = 0.65f))
                    }
                    MdIconButton(
                        MdIcons.NextChapter,
                        "Next chapter",
                        { if (hasNext) onNextChapter() },
                        background = if (hasNext) c.surfaceHigh else c.surfaceHigh.copy(alpha = 0.4f),
                        tint = if (hasNext) c.fg else c.fgMuted,
                        iconSize = 20.dp,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.navBg.copy(alpha = 0.92f))
                        .border(1.dp, c.dividerStrong, RoundedCornerShape(20.dp))
                        .padding(6.dp),
                ) {
                    ToolButton(MdIcons.Pages, mode.label, Modifier.weight(1f)) {
                        val next = ReadingMode.entries[(mode.ordinal + 1) % ReadingMode.entries.size]
                        onModeChange(next)
                    }
                    ToolButton(MdIcons.Crop, if (cropBorders) "Cropped" else "Crop", Modifier.weight(1f)) {
                        cropBorders = !cropBorders
                        scope.launch {
                            appPrefs.set(PrefKeys.CROP_BORDERS, cropBorders)
                        }
                    }
                    ToolButton(MdIcons.Sun, "Filters", Modifier.weight(1f)) {
                        showMoreOptions = true
                    }
                    ToolButton(MdIcons.Sliders, "Settings", Modifier.weight(1f), onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun MangaPage(
    url: String,
    modifier: Modifier = Modifier,
    webtoon: Boolean = false,
    cropBorders: Boolean = false,
    onTap: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    ZoomableBox(
        modifier = modifier,
        enabled = !webtoon,
        onTap = onTap,
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .addHeader("Referer", url.substringBefore("/", "").let {
                    if (url.startsWith("http")) url.split("/").take(3).joinToString("/") + "/" else ""
                })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = if (webtoon) ContentScale.FillWidth else if (cropBorders) ContentScale.Crop else ContentScale.Fit,
            loading = {
                Box(
                    if (webtoon) Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    else Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MdTheme.colors.accent, modifier = Modifier.size(32.dp))
                }
            },
            modifier = if (webtoon) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ReaderMoreOptionsPanel(
    visible: Boolean,
    mode: ReadingMode,
    onModeChange: (ReadingMode) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MdTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(visible = visible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(c.sheet)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                BodyText("Options", size = 16.sp, weight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Reading mode", Modifier.padding(bottom = 4.dp))
                    ReadingMode.entries.forEach { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mode == m) c.accentFaint else Color.Transparent)
                                .clickable { onModeChange(m) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BodyText(
                                m.label, Modifier.weight(1f), size = 14.sp,
                                weight = if (mode == m) FontWeight.Bold else FontWeight.Normal,
                                color = if (mode == m) c.accentSoft else c.fg,
                            )
                            if (mode == m) Icon(MdIcons.Check, null, tint = c.accentSoft, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = c.surfaceHigh)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Brightness")
                    MdSlider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.1f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, tint = MdTheme.colors.fg, modifier = Modifier.size(20.dp))
        BodyText(label, size = 11.sp, weight = FontWeight.SemiBold, maxLines = 1)
    }
}
