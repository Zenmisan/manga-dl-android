package com.mangadl.android.ui.screens.reader

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.prefs.dataStore
import androidx.compose.ui.tooling.preview.Preview
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val NOVEL_FONT_SIZE = intPreferencesKey("novel_font_size")
private val NOVEL_FONT_FAMILY = stringPreferencesKey("novel_font_family") // "serif" | "sans"
private val NOVEL_LINE_HEIGHT = stringPreferencesKey("novel_line_height") // "1.5" | "1.7" | "2.0"
private val NOVEL_THEME = stringPreferencesKey("novel_theme") // "dark" | "sepia" | "white"

private data class NovelTheme(
    val background: Color,
    val textColor: Color,
    val name: String,
)

private val novelThemes = mapOf(
    "dark" to NovelTheme(Color(0xFF0D0D0D), Color(0xD9FFFFFF), "Dark"),
    "sepia" to NovelTheme(Color(0xFFF4ECD8), Color(0xFF3D2B1F), "Sepia"),
    "white" to NovelTheme(Color(0xFFFFFFFF), Color(0xFF111111), "White"),
)

@Composable
fun NovelReaderScreen(
    provider: String,
    novelId: String,
    chapterId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val extensionManager = MangaDlApp.instance.extensionManager
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showUi by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    // Prefs from DataStore
    var fontSize by remember { mutableStateOf(17) }
    var fontFamily by remember { mutableStateOf("sans") }
    var lineHeight by remember { mutableStateOf("1.7") }
    var theme by remember { mutableStateOf("dark") }

    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            fontSize = prefs[NOVEL_FONT_SIZE] ?: 17
            fontFamily = prefs[NOVEL_FONT_FAMILY] ?: "sans"
            lineHeight = prefs[NOVEL_LINE_HEIGHT] ?: "1.7"
            theme = prefs[NOVEL_THEME] ?: "dark"
        }.collect {}
    }

    LaunchedEffect(provider, chapterId) {
        loading = true
        error = null
        try {
            val pages = extensionManager.getPages(provider, chapterId)
            content = pages.firstOrNull() ?: ""
        } catch (e: Exception) {
            error = e.message ?: "Failed to load chapter"
        } finally {
            loading = false
        }
    }

    val activeTheme = novelThemes[theme] ?: novelThemes["dark"]!!
    val isHtml = content.trimStart().startsWith("<")
    val lineHeightSp = (lineHeight.toFloatOrNull() ?: 1.7f)

    // Scroll progress
    val scrollProgress = if (scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeTheme.background)
            .pointerInput(Unit) {
                detectTapGestures { showUi = !showUi; showSettings = false }
            },
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MangaDlColors.Primary,
            )
            error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MangaDlColors.Primary, modifier = Modifier.size(48.dp))
                Text(error ?: "Unknown error", color = activeTheme.textColor, fontSize = 14.sp)
            }
            isHtml -> AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadDataWithBaseURL(null, wrapHtml(content, activeTheme, fontSize, lineHeight), "text/html", "UTF-8", null)
                    }
                },
                update = { wv ->
                    wv.loadDataWithBaseURL(null, wrapHtml(content, activeTheme, fontSize, lineHeight), "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 64.dp),
            )
            else -> SelectionContainer {
                val ff = if (fontFamily == "serif") FontFamily.Serif else FontFamily.SansSerif
                Text(
                    text = content,
                    style = TextStyle(
                        color = activeTheme.textColor,
                        fontSize = fontSize.sp,
                        fontFamily = ff,
                        lineHeight = (fontSize * lineHeightSp).sp,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 72.dp),
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { scrollProgress },
            modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
            color = MangaDlColors.Primary,
            trackColor = Color.Transparent,
        )

        // Top UI overlay
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(start = 4.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = chapterId.split(":").lastOrNull() ?: chapterId,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        // Bottom UI overlay
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { /* prev chapter */ }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                    Text("Previous", color = Color.White, fontSize = 13.sp)
                }
                Text(
                    "${(scrollProgress * 100).toInt()}%",
                    color = Color(0xBFFFFFFF),
                    fontSize = 12.sp,
                )
                TextButton(onClick = { /* next chapter */ }) {
                    Text("Next", color = Color.White, fontSize = 13.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Settings panel
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF0111111))
                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Text Size", color = MangaDlColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = {
                        fontSize = it.toInt()
                        scope.launch { context.dataStore.updateData { prefs -> prefs.toMutablePreferences().also { p -> p[NOVEL_FONT_SIZE] = fontSize } } }
                    },
                    valueRange = 14f..24f,
                    colors = SliderDefaults.colors(thumbColor = MangaDlColors.Primary, activeTrackColor = MangaDlColors.Primary),
                )
                Text("${fontSize}sp", color = MangaDlColors.TextPrimary, fontSize = 12.sp)

                Text("Font", color = MangaDlColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sans" to "Sans", "serif" to "Serif").forEach { (k, label) ->
                        val selected = fontFamily == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MangaDlColors.Primary else MangaDlColors.CardBg)
                                .border(1.dp, if (selected) MangaDlColors.Primary else MangaDlColors.CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    fontFamily = k
                                    scope.launch { context.dataStore.updateData { prefs -> prefs.toMutablePreferences().also { p -> p[NOVEL_FONT_FAMILY] = k } } }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Text("Line Height", color = MangaDlColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1.5", "1.7", "2.0").forEach { lh ->
                        val selected = lineHeight == lh
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MangaDlColors.Primary else MangaDlColors.CardBg)
                                .border(1.dp, if (selected) MangaDlColors.Primary else MangaDlColors.CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    lineHeight = lh
                                    scope.launch { context.dataStore.updateData { prefs -> prefs.toMutablePreferences().also { p -> p[NOVEL_LINE_HEIGHT] = lh } } }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(lh, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Text("Theme", color = MangaDlColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark" to "Dark", "sepia" to "Sepia", "white" to "White").forEach { (k, label) ->
                        val selected = theme == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MangaDlColors.Primary else MangaDlColors.CardBg)
                                .border(1.dp, if (selected) MangaDlColors.Primary else MangaDlColors.CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    theme = k
                                    scope.launch { context.dataStore.updateData { prefs -> prefs.toMutablePreferences().also { p -> p[NOVEL_THEME] = k } } }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.value.toLong()
    val r = (argb shr 48) and 0xFF
    val g = (argb shr 40) and 0xFF
    val b = (argb shr 32) and 0xFF
    return "#%02x%02x%02x".format(r, g, b)
}

private fun wrapHtml(body: String, theme: NovelTheme, fontSize: Int, lineHeight: String): String {
    val bg = colorToHex(theme.background)
    val text = colorToHex(theme.textColor)
    return """
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        body { background: $bg; color: $text; font-size: ${fontSize}px; line-height: $lineHeight;
               font-family: sans-serif; padding: 20px; margin: 0; }
        img { max-width: 100%; }
        </style></head><body>$body</body></html>
    """.trimIndent()
}

@Preview(showBackground = true, backgroundColor = 0xFF13111A)
@Composable
private fun NovelReaderScreenPreview() {
    MangaDlTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF13111A))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        "The Wandering Inn · Ch. 1",
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    repeat(6) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1AFFFFFF))
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
