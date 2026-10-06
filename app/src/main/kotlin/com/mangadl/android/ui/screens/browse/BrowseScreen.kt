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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.UiExtension
import com.mangadl.android.data.ui.UiSource
import com.mangadl.android.data.ui.ExtensionState
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSwitch
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.SearchField
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
        if (tab == 0) SourcesTab(sources, onOpenSource) else ExtensionsTab(extensions)
    }
}

@Composable
private fun SourcesTab(sources: List<UiSource>, onOpenSource: (UiSource) -> Unit) {
    val c = MdTheme.colors
    val lastUsed = sources.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (lastUsed != null) {
            item { Eyebrow("Last used", Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp)) }
            item { SourceRow(lastUsed, onOpenSource, pin = false) }
        }
        item { Eyebrow("All sources", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp), color = c.fgSubtle) }
        items(sources) { SourceRow(it, onOpenSource, pin = true) }
    }
}

@Composable
private fun SourceRow(source: UiSource, onOpen: (UiSource) -> Unit, pin: Boolean) {
    val c = MdTheme.colors
    var pinned by rememberState(false)
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
        if (pin) {
            MdIconButton(
                MdIcons.Pin,
                if (pinned) "Unpin source" else "Pin source",
                { pinned = !pinned },
                tint = if (pinned) c.accentLight else c.fgSubtle,
                iconSize = 18.dp,
            )
        }
    }
}

@Composable
private fun ExtensionsTab(extensions: List<UiExtension>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("manga_dl_sources", android.content.Context.MODE_PRIVATE) }
    var disabledSources by remember {
        mutableStateOf(prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet())
    }

    val c = MdTheme.colors
    var lang by rememberState("English")
    var adult by rememberState(false)
    val updates = extensions.filter { it.state == ExtensionState.UpdateAvailable }
    val installed = extensions.filter { it.state == ExtensionState.Installed }
    val available = extensions.filter { it.state == ExtensionState.Available }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("English", lang == "English", { lang = "English" }, height = 34.dp, fontSize = 12.sp)
                PillChip("All languages", lang == "All", { lang = "All" }, height = 34.dp, fontSize = 12.sp)
                PillChip("Show 18+", adult, { adult = !adult }, height = 34.dp, fontSize = 12.sp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Updates pending · ${updates.size}", Modifier.weight(1f))
                PillButton("Update All", {}, tone = ButtonTone.Primary)
            }
        }
        items(updates) { ExtensionRow(it) { PillButton("Update", {}, tone = ButtonTone.Soft) } }
        item { Eyebrow("Installed", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp), color = c.fgSubtle) }
        items(installed) { ext ->
            ExtensionRow(ext) {
                val isEnabled = ext.id !in disabledSources
                MdSwitch(isEnabled, { v ->
                    val updated = if (v) disabledSources - ext.id else disabledSources + ext.id
                    disabledSources = updated
                    prefs.edit().putStringSet("disabled_sources", updated).apply()
                })
            }
        }
        item { Eyebrow("Available · Keiyoushi index", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp), color = c.fgSubtle) }
        items(available) { ExtensionRow(it) { PillButton("Install", {}) } }
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
            BodyText(e.name, size = 15.sp, weight = FontWeight.Bold)
            BodyText(e.meta, size = 12.sp, color = c.fgSubtle)
        }
        action()
    }
}
