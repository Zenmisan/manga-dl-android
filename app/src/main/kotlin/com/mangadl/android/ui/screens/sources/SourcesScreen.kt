package com.mangadl.android.ui.screens.sources

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.extensions.ExtensionManager
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.SearchField
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import kotlinx.coroutines.launch

private const val PREFS = "manga_dl_sources"
private const val KEY_DISABLED = "disabled_sources"

@Composable
fun SourcesScreen(onBack: () -> Unit = {}) {
    val extensionManager = MangaDlApp.instance.extensionManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPrefs = remember { AppPreferences.getInstance(context) }
    val pinnedSources by appPrefs.pinnedSources.collectAsState(initial = emptySet())

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val allSources = remember { extensionManager.listExtensions().sortedBy { it.name } }
    val c = MdTheme.colors

    var searchQuery by rememberState("")
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Manga, 2: Novels

    var disabledIds by remember {
        mutableStateOf(prefs.getStringSet(KEY_DISABLED, emptySet())?.toSet() ?: emptySet())
    }

    fun toggle(id: String) {
        disabledIds = if (id in disabledIds) disabledIds - id else disabledIds + id
        prefs.edit().putStringSet(KEY_DISABLED, disabledIds).apply()
    }

    val mangaSources = remember(allSources) { allSources.filter { !ExtensionManager.isNovelSource(it.id) } }
    val novelSources = remember(allSources) { allSources.filter { ExtensionManager.isNovelSource(it.id) } }

    val filteredList = remember(allSources, selectedFilterTab, searchQuery) {
        val baseList = when (selectedFilterTab) {
            1 -> mangaSources
            2 -> novelSources
            else -> allSources
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }
        }
    }

    val pinnedList = remember(filteredList, pinnedSources) {
        filteredList.filter { it.id in pinnedSources }
    }
    val otherList = remember(filteredList, pinnedSources) {
        filteredList.filter { it.id !in pinnedSources }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        BackHeader("Sources & Extensions", onBack)

        Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search 24 native sources…",
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("All (${allSources.size})", selectedFilterTab == 0, { selectedFilterTab = 0 })
                PillChip("Manga (${mangaSources.size})", selectedFilterTab == 1, { selectedFilterTab = 1 })
                PillChip("Web Novels (${novelSources.size})", selectedFilterTab == 2, { selectedFilterTab = 2 })
            }
        }

        Divider(Modifier.padding(top = 8.dp), color = c.dividerStrong)

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
            if (pinnedList.isNotEmpty()) {
                item {
                    Eyebrow(
                        "Pinned (${pinnedList.size})",
                        Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = c.accentLight,
                    )
                }
                items(pinnedList, key = { "pinned_${it.id}" }) { source ->
                    val isNovel = ExtensionManager.isNovelSource(source.id)
                    val isEnabled = source.id !in disabledIds
                    val isPinned = source.id in pinnedSources
                    SourceDetailRow(
                        name = source.name,
                        lang = source.lang,
                        isNovel = isNovel,
                        enabled = isEnabled,
                        isPinned = isPinned,
                        onTogglePin = { scope.launch { appPrefs.togglePinnedSource(source.id) } },
                        onToggleEnabled = { toggle(source.id) },
                    )
                    Divider(Modifier.padding(start = 72.dp))
                }
            }

            if (otherList.isNotEmpty()) {
                item {
                    Eyebrow(
                        if (pinnedList.isNotEmpty()) "All Sources (${otherList.size})" else "Active Sources (${otherList.size})",
                        Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = c.fgSubtle,
                    )
                }
                items(otherList, key = { "src_${it.id}" }) { source ->
                    val isNovel = ExtensionManager.isNovelSource(source.id)
                    val isEnabled = source.id !in disabledIds
                    val isPinned = source.id in pinnedSources
                    SourceDetailRow(
                        name = source.name,
                        lang = source.lang,
                        isNovel = isNovel,
                        enabled = isEnabled,
                        isPinned = isPinned,
                        onTogglePin = { scope.launch { appPrefs.togglePinnedSource(source.id) } },
                        onToggleEnabled = { toggle(source.id) },
                    )
                    Divider(Modifier.padding(start = 72.dp))
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(52.dp).background(c.accentMuted, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(MdIcons.Browse, null, tint = c.accentLight, modifier = Modifier.size(26.dp)) }
                            BodyText("No matching sources found", size = 16.sp, weight = FontWeight.Bold)
                            BodyText("Try adjusting your search query", size = 13.sp, color = c.fgMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceDetailRow(
    name: String,
    lang: String,
    isNovel: Boolean,
    enabled: Boolean,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onToggleEnabled: () -> Unit,
) {
    val c = MdTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoTile(name.firstOrNull()?.uppercase() ?: "S", if (isNovel) Color(0xFF2C3E50) else Color(0xFF6B2D5C))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText(name, size = 15.sp, weight = FontWeight.SemiBold)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNovel) c.accentFaint else c.surfaceHigh)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    BodyText(if (isNovel) "Novel" else "Manga", size = 10.sp, color = if (isNovel) c.accentSoft else c.fgSubtle, weight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText(lang.uppercase(), size = 11.sp, color = c.fgSubtle, weight = FontWeight.Bold)
                BodyText("•", size = 11.sp, color = c.fgSubtle)
                BodyText("Native Kotlin Engine", size = 11.sp, color = c.accentLight)
            }
        }
        MdIconButton(
            icon = MdIcons.Pin,
            contentDescription = if (isPinned) "Unpin" else "Pin",
            onClick = onTogglePin,
            tint = if (isPinned) c.accentLight else c.fgSubtle.copy(alpha = 0.5f),
            size = 36.dp,
            iconSize = 18.dp,
        )
        Switch(
            checked = enabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(checkedThumbColor = c.accent, checkedTrackColor = c.accentMuted),
        )
    }
}
