package com.mangadl.android.ui.screens.reader

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSlider
import com.mangadl.android.ui.components.PagePlaceholder
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import kotlinx.coroutines.launch

enum class ReadingMode(val label: String) { LTR("LTR pager"), RTL("RTL pager"), Vertical("Vertical"), Webtoon("Webtoon") }

@Composable
fun ReaderScreen(
    title: String,
    chapterLabel: String,
    onClose: () -> Unit,
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    pageCount: Int = 38,
    startPage: Int = 14,
    initialMode: ReadingMode = ReadingMode.RTL,
    initiallyShowControls: Boolean = true,
) {
    val c = MdTheme.colors
    var controls by rememberState(initiallyShowControls)
    var mode by rememberState(initialMode)
    var bookmarked by rememberState(false)
    var showMoreOptions by rememberState(false)
    var brightness by rememberState(1f)
    val pager = rememberPagerState(initialPage = startPage - 1) { pageCount }
    val scope = rememberCoroutineScope()
    val toggle = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { controls = !controls }

    Box(Modifier.fillMaxSize().background(c.readerBg)) {
        when (mode) {
            ReadingMode.LTR, ReadingMode.RTL -> HorizontalPager(
                state = pager,
                reverseLayout = mode == ReadingMode.RTL,
                modifier = Modifier.fillMaxSize(),
            ) { page -> ReaderPage(page, toggle) }
            ReadingMode.Vertical -> VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page -> ReaderPage(page, toggle) }
            ReadingMode.Webtoon -> LazyColumn(Modifier.fillMaxSize().then(toggle)) {
                items(pageCount) { page ->
                    PagePlaceholder("[Page ${page + 1}]", Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                }
            }
        }

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            Column(Modifier.fillMaxWidth().background(c.navBg.copy(alpha = 0.92f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MdIconButton(MdIcons.Back, "Close reader", onClose)
                    Column(Modifier.weight(1f)) {
                        BodyText(title, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                        BodyText(chapterLabel, size = 12.sp, color = c.fg.copy(alpha = 0.65f), maxLines = 1)
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
            onModeChange = { mode = it; showMoreOptions = false },
            brightness = brightness,
            onBrightnessChange = { brightness = it },
            onDismiss = { showMoreOptions = false },
        )

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
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
                    MdIconButton(MdIcons.PrevChapter, "Previous chapter", onPrevChapter, background = c.surfaceHigh, iconSize = 20.dp)
                    BodyText("${pager.currentPage + 1}", Modifier.width(24.dp), size = 12.sp, weight = FontWeight.Bold)
                    MdSlider(
                        value = (pager.currentPage + 1).toFloat(),
                        onValueChange = { v -> scope.launch { pager.scrollToPage(v.toInt() - 1) } },
                        valueRange = 1f..pageCount.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    BodyText("$pageCount", Modifier.width(24.dp), size = 12.sp, weight = FontWeight.Bold, color = c.fg.copy(alpha = 0.65f))
                    MdIconButton(MdIcons.NextChapter, "Next chapter", onNextChapter, background = c.surfaceHigh, iconSize = 20.dp)
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
                        mode = ReadingMode.entries[(mode.ordinal + 1) % ReadingMode.entries.size]
                    }
                    ToolButton(MdIcons.Crop, "Crop", Modifier.weight(1f)) {}
                    ToolButton(MdIcons.Sun, "Filters", Modifier.weight(1f)) {}
                    ToolButton(MdIcons.Sliders, "Settings", Modifier.weight(1f), onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun ReaderPage(page: Int, toggle: Modifier) {
    Box(Modifier.fillMaxSize().then(toggle), contentAlignment = Alignment.Center) {
        PagePlaceholder("[Page ${page + 1}]", Modifier.fillMaxWidth().aspectRatio(2f / 3f))
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
