package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.ExtensionState
import com.mangadl.android.data.ui.UiExtension
import com.mangadl.android.data.ui.UiSource
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSwitch
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.UnderlineTabs
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun BrowseScreen(
    onOpenSource: (UiSource) -> Unit,
    onSearch: () -> Unit,
    onMigrate: () -> Unit,
    initialTab: Int = 0,
    sources: List<UiSource> = emptyList(),
    extensions: List<UiExtension> = emptyList(),
    pinnedSources: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onInstallExtension: (UiExtension) -> Unit = {},
    onUpdateExtension: (UiExtension) -> Unit = {},
    onRefreshRepo: () -> Unit = {},
    isRepoLoading: Boolean = false,
) {
    val c = MdTheme.colors
    var tab by rememberState(initialTab)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DisplayText("Browse", 30.sp)
            if (tab == 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceRaised)
                        .border(1.dp, c.track, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onSearch)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(MdIcons.Search, null, tint = c.fgSubtle, modifier = Modifier.size(20.dp))
                    BodyText("Search all sources", size = 15.sp, color = c.fgSubtle)
                }
            }
            UnderlineTabs(listOf("Sources", "Extensions", "Migrate"), tab, { if (it == 2) onMigrate() else tab = it })
        }
        if (tab == 0) {
            SourcesTab(
                sources = sources,
                pinnedSources = pinnedSources,
                onOpenSource = onOpenSource,
                onTogglePin = onTogglePin,
            )
        } else {
            ExtensionsTab(
                extensions = extensions,
                onInstallExtension = onInstallExtension,
                onUpdateExtension = onUpdateExtension,
                onRefreshRepo = onRefreshRepo,
                isRepoLoading = isRepoLoading,
            )
        }
    }
}

@Composable
private fun SourcesTab(
    sources: List<UiSource>,
    pinnedSources: Set<String>,
    onOpenSource: (UiSource) -> Unit,
    onTogglePin: (String) -> Unit,
) {
    val c = MdTheme.colors
    val pinned = sources.filter { it.id in pinnedSources }
    val unpinned = sources.filter { it.id !in pinnedSources }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (pinned.isNotEmpty()) {
            item { Eyebrow("Pinned (${pinned.size})", Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp), color = c.accentLight) }
            items(pinned, key = { "pinned_${it.id}" }) { src ->
                SourceRow(
                    source = src,
                    onOpen = onOpenSource,
                    isPinned = true,
                    onTogglePin = { onTogglePin(src.id) },
                )
            }
        }

        item { Eyebrow("All sources (${unpinned.size})", Modifier.padding(start = 20.dp, top = if (pinned.isNotEmpty()) 16.dp else 10.dp, bottom = 6.dp), color = c.fgSubtle) }
        items(unpinned, key = { "all_${it.id}" }) { src ->
            SourceRow(
                source = src,
                onOpen = onOpenSource,
                isPinned = false,
                onTogglePin = { onTogglePin(src.id) },
            )
        }
    }
}

@Composable
private fun SourceRow(
    source: UiSource,
    onOpen: (UiSource) -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
) {
    val c = MdTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(source) }
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LogoTile(source.initial, source.color)
        Column(Modifier.weight(1f)) {
            BodyText(source.name, size = 15.sp, weight = FontWeight.Bold)
            BodyText(source.meta, size = 12.sp, color = c.fgSubtle)
        }
        PillButton("Latest", { onOpen(source) })
        MdIconButton(
            MdIcons.Pin,
            if (isPinned) "Unpin source" else "Pin source",
            onTogglePin,
            tint = if (isPinned) c.accentLight else c.fgSubtle,
            iconSize = 18.dp,
        )
    }
}

@Composable
private fun ExtensionsTab(
    extensions: List<UiExtension>,
    onInstallExtension: (UiExtension) -> Unit,
    onUpdateExtension: (UiExtension) -> Unit,
    onRefreshRepo: () -> Unit,
    isRepoLoading: Boolean,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("manga_dl_sources", android.content.Context.MODE_PRIVATE) }
    var disabledSources by remember {
        mutableStateOf(prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet())
    }

    val c = MdTheme.colors
    var lang by rememberState("English")
    var adult by rememberState(false)

    val filtered = remember(extensions, lang, adult) {
        extensions.filter { ext ->
            val langMatches = if (lang == "English") {
                ext.lang.equals("en", ignoreCase = true) || ext.lang.equals("all", ignoreCase = true)
            } else true

            val nsfwMatches = if (!adult) !ext.isNsfw else true
            langMatches && nsfwMatches
        }
    }

    val updates = remember(filtered) { filtered.filter { it.state == ExtensionState.UpdateAvailable } }
    val installed = remember(filtered) { filtered.filter { it.state == ExtensionState.Installed } }
    val available = remember(filtered) { filtered.filter { it.state == ExtensionState.Available } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("English", lang == "English", { lang = "English" }, height = 34.dp, fontSize = 12.sp)
                PillChip("All languages", lang == "All", { lang = "All" }, height = 34.dp, fontSize = 12.sp)
                PillChip("Show 18+", adult, { adult = !adult }, height = 34.dp, fontSize = 12.sp)
            }
        }

        if (updates.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eyebrow("Updates pending · ${updates.size}", Modifier.weight(1f))
                    PillButton("Update All", { updates.forEach(onUpdateExtension) }, tone = ButtonTone.Primary)
                }
            }
            items(updates, key = { "upd_${it.id}" }) { ext ->
                ExtensionRow(ext) {
                    PillButton("Update", { onUpdateExtension(ext) }, tone = ButtonTone.Soft)
                }
            }
        }

        item { Eyebrow("Installed (${installed.size})", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp), color = c.fgSubtle) }
        items(installed, key = { "inst_${it.id}" }) { ext ->
            ExtensionRow(ext) {
                val isEnabled = ext.id !in disabledSources
                MdSwitch(isEnabled, { v ->
                    val updated = if (v) disabledSources - ext.id else disabledSources + ext.id
                    disabledSources = updated
                    prefs.edit().putStringSet("disabled_sources", updated).apply()
                })
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow("Available · Keiyoushi index (${available.size})", Modifier.weight(1f), color = c.fgSubtle)
                if (isRepoLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = c.accent)
                } else {
                    PillButton("Refresh", onRefreshRepo, tone = ButtonTone.Ghost)
                }
            }
        }

        items(available, key = { "avail_${it.id}" }) { ext ->
            ExtensionRow(ext) {
                PillButton("Install", { onInstallExtension(ext) })
            }
        }
    }
}

@Composable
private fun ExtensionRow(e: UiExtension, action: @Composable () -> Unit) {
    val c = MdTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LogoTile(e.initial, e.color)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText(e.name, size = 15.sp, weight = FontWeight.Bold)
                if (e.isNsfw) {
                    BodyText("18+", size = 10.sp, color = c.accentLight, weight = FontWeight.Bold)
                }
            }
            BodyText(e.meta, size = 12.sp, color = c.fgSubtle)
        }
        action()
    }
}
