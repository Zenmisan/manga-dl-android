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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSlider
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.Inter
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.theme.PtSerif

private enum class NovelTheme(val label: String, val bg: Color, val fg: Color) {
    Dark("Dark", Color(0xFF0D0D0D), Color.White.copy(alpha = 0.86f)),
    Black("Black", Color.Black, Color.White.copy(alpha = 0.86f)),
    Sepia("Sepia", Color(0xFFE8DCC4), Color(0xFF3B2F22)),
    Light("Light", Color(0xFFF5F5F2), Color(0xFF1F1F1F)),
}

@Composable
fun NovelReaderScreen(
    novelTitle: String,
    chapterLabel: String,
    onClose: () -> Unit,
    onChapterList: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    initiallyShowControls: Boolean = true,
) {
    val c = MdTheme.colors
    var controls by rememberState(initiallyShowControls)
    var serif by rememberState(true)
    var size by rememberState(18f)
    var theme by rememberState(NovelTheme.Dark)
    val bodyStyle = TextStyle(
        fontFamily = if (serif) PtSerif else Inter,
        fontSize = size.sp,
        lineHeight = (size * 1.7f).sp,
        color = theme.fg,
    )

    Box(Modifier.fillMaxSize().background(theme.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(remember { MutableInteractionSource() }, indication = null) { controls = !controls }
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 84.dp, bottom = 320.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BodyText(chapterLabel.uppercase(), size = 13.sp, weight = FontWeight.ExtraBold, color = theme.fg.copy(alpha = 0.6f))
            listOf(
                "[Chapter text loads here in the reading font. Size, line height, margins and theme all come from the controls below.]",
                "[Paragraphs keep generous spacing so long sessions stay comfortable on a phone screen.]",
                "[Scroll progress is saved per chapter and synced like manga progress.]",
            ).forEach { androidx.compose.material3.Text(it, style = bodyStyle) }
        }

        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(c.navBg.copy(alpha = 0.94f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MdIconButton(MdIcons.Back, "Close reader", onClose)
                    Column(Modifier.weight(1f)) {
                        BodyText(novelTitle, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                        BodyText("Ch. 12 · 34%", size = 12.sp, color = c.fg.copy(alpha = 0.65f))
                    }
                    MdIconButton(MdIcons.ListBullets, "Chapter list", onChapterList)
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
                        NovelFontOption("Serif", serif, PtSerif, Modifier.weight(1f)) { serif = true }
                        NovelFontOption("Sans", !serif, Inter, Modifier.weight(1f)) { serif = false }
                    }
                }
                NovelControlRow("Size") {
                    NovelSizeButton("A−", 13, "Smaller text") { size = (size - 1f).coerceAtLeast(14f) }
                    MdSlider(size, { size = it }, Modifier.weight(1f), valueRange = 14f..26f)
                    NovelSizeButton("A+", 17, "Larger text") { size = (size + 1f).coerceAtMost(26f) }
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
                                    .clickable(role = Role.RadioButton) { theme = t },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MdButton("Prev Chapter", onPrevChapter, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 48.dp, shape = RoundedCornerShape(12.dp), fontSize = 13.sp, horizontalPadding = 8.dp)
                    MdButton("Next Chapter", onNextChapter, Modifier.weight(1f), height = 48.dp, shape = RoundedCornerShape(12.dp), fontSize = 13.sp, horizontalPadding = 8.dp)
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
