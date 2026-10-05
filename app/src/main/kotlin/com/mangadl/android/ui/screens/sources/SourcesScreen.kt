package com.mangadl.android.ui.screens.sources

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.theme.MdTheme

private const val PREFS = "manga_dl_sources"
private const val KEY_DISABLED = "disabled_sources"

@Composable
fun SourcesScreen(onBack: () -> Unit = {}) {
    val extensionManager = MangaDlApp.instance.extensionManager
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val allSources = remember { extensionManager.listExtensions().sortedBy { it.name } }
    val c = MdTheme.colors

    var disabledIds by remember {
        mutableStateOf(prefs.getStringSet(KEY_DISABLED, emptySet())?.toSet() ?: emptySet())
    }

    fun toggle(id: String) {
        disabledIds = if (id in disabledIds) disabledIds - id else disabledIds + id
        prefs.edit().putStringSet(KEY_DISABLED, disabledIds).apply()
    }

    val enabled = allSources.filter { it.id !in disabledIds }
    val disabled = allSources.filter { it.id in disabledIds }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        BackHeader("Sources", onBack)

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
            if (enabled.isNotEmpty()) {
                item { Eyebrow("Enabled (${enabled.size})", Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                items(enabled, key = { it.id }) { source ->
                    SourceRow(source.name, source.lang, enabled = true) { toggle(source.id) }
                    Divider(Modifier.padding(start = 72.dp))
                }
            }
            if (disabled.isNotEmpty()) {
                item { Eyebrow("Disabled (${disabled.size})", Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                items(disabled, key = { it.id }) { source ->
                    SourceRow(source.name, source.lang, enabled = false) { toggle(source.id) }
                    Divider(Modifier.padding(start = 72.dp))
                }
            }
            if (allSources.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(52.dp).background(c.accentMuted, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(MdIcons.Browse, null, tint = c.accentLight, modifier = Modifier.size(26.dp)) }
                            BodyText("No sources available", size = 18.sp, weight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(name: String, lang: String, enabled: Boolean, onToggle: () -> Unit) {
    val c = MdTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.surface),
            contentAlignment = Alignment.Center,
        ) { Icon(MdIcons.Browse, null, tint = c.fgSubtle, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            BodyText(name, size = 15.sp, weight = FontWeight.SemiBold)
            BodyText(lang.uppercase(), size = 12.sp, color = c.fgSubtle)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = c.accent, checkedTrackColor = c.accentMuted),
        )
    }
}
