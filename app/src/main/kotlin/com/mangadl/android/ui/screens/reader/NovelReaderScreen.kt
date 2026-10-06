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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSlider
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.Inter
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.theme.PtSerif
import kotlinx.coroutines.launch

private enum class NovelTheme(val label: String, val bg: Color, val fg: Color) {
    Dark("Dark", Color(0xFF0D0D0D), Color.White.copy(alpha = 0.86f)),
    Black("Black", Color.Black, Color.White.copy(alpha = 0.86f)),
    Sepia("Sepia", Color(0xFFE8DCC4), Color(0xFF3B2F22)),
    Light("Light", Color(0xFFF5F5F2), Color(0xFF1F1F1F)),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    novelTitle: String,
    chapterLabel: String,
    paragraphs: List<String> = emptyList(),
    loading: Boolean = false,
    error: String? = null,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    chapters: List<Chapter> = emptyList(),
    onClose: () -> Unit,
    onChapterList: () -> Unit = {},
    onSelectChapter: (Chapter) -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    initiallyShowControls: Boolean = true,
) {
    val c = MdTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPrefs = remember { AppPreferences.getInstance(context) }

    val savedTheme by appPrefs.novelTheme.collectAsState(initial = "Dark")
    val savedSize by appPrefs.novelFontSize.collectAsState(initial = 18f)
    val savedSerif by appPrefs.novelSerif.collectAsState(initial = true)

    var controls by rememberState(initiallyShowControls)
    var serif by remember(savedSerif) { mutableStateOf(savedSerif) }
    var size by remember(savedSize) { mutableStateOf(savedSize) }
    var theme by remember(savedTheme) {
        mutableStateOf(NovelTheme.entries.find { it.label.equals(savedTheme, ignoreCase = true) } ?: NovelTheme.Dark)
    }
    var showChapterSheet by rememberState(false)

    val bodyStyle = TextStyle(
        fontFamily = if (serif) PtSerif else Inter,
        fontSize = size.sp,
        lineHeight = (size * 1.75f).sp,
        color = theme.fg,
    )

    val scrollState = rememberScrollState()

    val scrollPercent = remember(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            (scrollState.value.toFloat() / scrollState.maxValue).coerceIn(0f, 1f)
        } else 0f
    }

    LaunchedEffect(scrollPercent) {
        onProgressChange(scrollPercent)
    }

    Box(Modifier.fillMaxSize().background(theme.bg)) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = c.accentSoft)
                        BodyText("Loading chapter…", size = 14.sp, color = theme.fg.copy(alpha = 0.7f))
                    }
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        BodyText(error, size = 14.sp, color = theme.fg.copy(alpha = 0.8f))
                        MdButton("Retry", { onPrevChapter() }, height = 44.dp)
                    }
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { controls = !controls }
                        .statusBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 84.dp, bottom = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    BodyText(chapterLabel.uppercase(), size = 13.sp, weight = FontWeight.ExtraBold, color = theme.fg.copy(alpha = 0.6f))
                    paragraphs.forEach { paragraph ->
                        androidx.compose.material3.Text(paragraph, style = bodyStyle)
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hasPrev) {
                            MdButton(
                                "Previous Chapter",
                                onPrevChapter,
                                Modifier.weight(1f),
                                tone = ButtonTone.Ghost,
                                height = 48.dp,
                                shape = RoundedCornerShape(12.dp),
                                fontSize = 13.sp,
                            )
                        }
                        if (hasNext) {
                            MdButton(
                                "Next Chapter",
                                onNextChapter,
                                Modifier.weight(1f),
                                tone = ButtonTone.Primary,
                                height = 48.dp,
                                shape = RoundedCornerShape(12.dp),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(c.navBg.copy(alpha = 0.94f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MdIconButton(MdIcons.Back, "Close reader", onClose)
                    Column(Modifier.weight(1f)) {
                        BodyText(novelTitle, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                        val pctText = "${(scrollPercent * 100).toInt()}%"
                        BodyText(
                            if (chapterLabel.isNotBlank()) "$chapterLabel · $pctText" else pctText,
                            size = 12.sp,
                            color = c.fg.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                    }
                    MdIconButton(MdIcons.ListBullets, "Chapter list", {
                        if (chapters.isNotEmpty()) showChapterSheet = true else onChapterList()
                    })
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.dividerStrong))
            }
        }

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(c.panel)
                    .border(1.dp, c.track, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NovelControlRow("Font") {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NovelFontOption("Serif", serif, PtSerif, Modifier.weight(1f)) {
                            serif = true
                            scope.launch { appPrefs.set(PrefKeys.NOVEL_SERIF, true) }
                        }
                        NovelFontOption("Sans", !serif, Inter, Modifier.weight(1f)) {
                            serif = false
                            scope.launch { appPrefs.set(PrefKeys.NOVEL_SERIF, false) }
                        }
                    }
                }
                NovelControlRow("Size") {
                    NovelSizeButton("A−", 13, "Smaller text") {
                        val newSize = (size - 1f).coerceAtLeast(14f)
                        size = newSize
                        scope.launch { appPrefs.set(PrefKeys.NOVEL_FONT_SIZE, newSize) }
                    }
                    MdSlider(
                        size,
                        { newSize ->
                            size = newSize
                            scope.launch { appPrefs.set(PrefKeys.NOVEL_FONT_SIZE, newSize) }
                        },
                        Modifier.weight(1f),
                        valueRange = 14f..26f,
                    )
                    NovelSizeButton("A+", 17, "Larger text") {
                        val newSize = (size + 1f).coerceAtMost(26f)
                        size = newSize
                        scope.launch { appPrefs.set(PrefKeys.NOVEL_FONT_SIZE, newSize) }
                    }
                }
                NovelControlRow("Theme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NovelTheme.entries.forEach { t ->
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(t.bg)
                                    .border(if (t == theme) 2.dp else 1.dp, if (t == theme) c.accent else c.fg.copy(alpha = 0.2f), CircleShape)
                                    .clickable(role = Role.RadioButton) {
                                        theme = t
                                        scope.launch { appPrefs.set(PrefKeys.NOVEL_THEME, t.label) }
                                    },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MdButton(
                        "Prev Chapter",
                        onPrevChapter,
                        Modifier.weight(1f),
                        enabled = hasPrev,
                        tone = ButtonTone.Ghost,
                        height = 48.dp,
                        shape = RoundedCornerShape(12.dp),
                        fontSize = 13.sp,
                        horizontalPadding = 8.dp,
                    )
                    MdButton(
                        "Next Chapter",
                        onNextChapter,
                        Modifier.weight(1f),
                        enabled = hasNext,
                        height = 48.dp,
                        shape = RoundedCornerShape(12.dp),
                        fontSize = 13.sp,
                        horizontalPadding = 8.dp,
                    )
                }
            }
        }

        if (showChapterSheet && chapters.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = c.sheet,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    DisplayText("Chapters (${chapters.size})", 20.sp)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.fillMaxWidth().height(400.dp)) {
                        items(chapters, key = { it.id }) { ch ->
                            val chNum = if (ch.number > 0) "Ch. ${ch.number.toString().trimEnd('0').trimEnd('.')}" else ""
                            val label = listOf(chNum, ch.title).filter { it.isNotBlank() }.joinToString(" · ")
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChapterSheet = false
                                        onSelectChapter(ch)
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BodyText(label, size = 14.sp, weight = FontWeight.SemiBold, maxLines = 1)
                                if (ch.publishedAt.isNotBlank()) {
                                    BodyText(ch.publishedAt, size = 12.sp, color = c.fgSubtle)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelControlRow(label: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BodyText(label, Modifier.width(64.dp), size = 13.sp, weight = FontWeight.Bold, color = MdTheme.colors.fg.copy(alpha = 0.7f))
        content()
    }
}

@Composable
private fun NovelSizeButton(label: String, fontSize: Int, description: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .border(1.dp, MdTheme.colors.border, shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BodyText(label, size = fontSize.sp, weight = FontWeight.ExtraBold)
    }
}

@Composable
private fun NovelFontOption(label: String, selected: Boolean, family: androidx.compose.ui.text.font.FontFamily, modifier: Modifier, onClick: () -> Unit) {
    val c = MdTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) c.accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) c.accent else c.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(label, style = TextStyle(fontFamily = family, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (selected) c.fg else c.fg.copy(alpha = 0.8f)))
    }
}
