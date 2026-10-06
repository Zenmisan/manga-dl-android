package com.mangadl.android.ui.screens.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.UpdateGroup
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.TabHeader
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun UpdatesScreen(groups: List<UpdateGroup>, onOpenChapter: () -> Unit, onRefresh: () -> Unit = {}, lastChecked: String? = null) {
    val c = MdTheme.colors
    Column(Modifier.fillMaxSize()) {
        TabHeader("Updates", Modifier.padding(bottom = 0.dp)) {
            MdIconButton(MdIcons.Refresh, "Check for new chapters", onRefresh)
        }
        val checkedLabel = if (lastChecked != null) "Last checked $lastChecked" else "Not checked yet"
        BodyText(checkedLabel, Modifier.padding(start = 20.dp, bottom = 8.dp), size = 12.sp, color = c.fgSubtle)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            groups.forEach { group ->
                item { Eyebrow(group.label, Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp), color = c.fgSubtle) }
                items(group.items) { u ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenChapter)
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CoverArt(u.manga.cover, Modifier.size(44.dp, 64.dp), RoundedCornerShape(6.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BodyText(u.manga.title, size = 15.sp, weight = FontWeight.Bold)
                            BodyText(u.chapter, size = 13.sp, color = c.fg.copy(alpha = 0.65f))
                        }
                        MdIconButton(MdIcons.Download, "Download chapter", {}, tint = c.fg.copy(alpha = 0.75f), iconSize = 20.dp)
                    }
                }
            }
        }
    }
}
