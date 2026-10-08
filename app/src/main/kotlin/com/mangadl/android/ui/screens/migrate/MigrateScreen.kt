package com.mangadl.android.ui.screens.migrate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.data.ui.toUiManga
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdCheckbox
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.SelectableCard
import com.mangadl.android.ui.components.SurfaceCard
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.MigrateMatch
import com.mangadl.android.ui.viewmodels.MigrateViewModel

private val COVER_PALETTE = listOf(
    Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2E2412), Color(0xFF2D1716),
    Color(0xFF13282A), Color(0xFF311A1F), Color(0xFF1D2A1A), Color(0xFF1B2030),
    Color(0xFF2B1A2E), Color(0xFF22222A), Color(0xFF1E3A5F), Color(0xFF1F2C4F),
)
private fun coverColor(id: String): Color {
    val idx = id.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
    return COVER_PALETTE[idx]
}

@Composable
fun MigrateScreen(onBack: () -> Unit, onMigrate: () -> Unit) {
    val c = MdTheme.colors
    val vm: MigrateViewModel = viewModel()
    val library by vm.library.collectAsState()
    val sourceManga by vm.sourceManga.collectAsState()
    val matches by vm.matches.collectAsState()
    val searching by vm.searching.collectAsState()

    var pickIdx by rememberState(-1)
    var keepRead by rememberState(true)
    var deleteOld by rememberState(false)

    Screen {
        BackHeader("Migrate", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Source manga picker
            if (sourceManga == null) {
                Eyebrow("Choose manga to migrate", Modifier.padding(bottom = 4.dp))
                if (library.isEmpty()) {
                    BodyText("Library is empty.", size = 13.sp, color = c.fgSubtle)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        library.forEach { m ->
                            SurfaceCard(Modifier.fillMaxWidth().clickable { vm.selectSource(m) }, radius = 12.dp, background = c.surface) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CoverArt(coverColor(m.id), Modifier.size(40.dp, 60.dp), RoundedCornerShape(6.dp), imageUrl = m.coverUrl)
                                    Column(Modifier.weight(1f)) {
                                        BodyText(m.title, size = 14.sp, weight = FontWeight.Bold, maxLines = 1)
                                        BodyText(m.provider, size = 12.sp, color = c.fgSubtle)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Source manga card
                SurfaceCard(Modifier.fillMaxWidth(), radius = 14.dp, background = c.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CoverArt(coverColor(sourceManga!!.id), Modifier.size(44.dp, 66.dp), RoundedCornerShape(6.dp), imageUrl = sourceManga!!.coverUrl)
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Moving from", color = c.fgSubtle)
                            BodyText(sourceManga!!.title, Modifier.padding(top = 4.dp), size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                            BodyText("${sourceManga!!.provider} · ${sourceManga!!.readCount} of ${sourceManga!!.totalChapters} read", size = 12.sp, color = c.fgSubtle)
                        }
                        BodyText("Change", size = 12.sp, color = c.accentSoft,
                            modifier = Modifier.clickable { vm.selectSource(library.first()) })
                    }
                }

                Eyebrow(if (searching) "Searching…" else "Pick a match")
                if (searching) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.accentSoft, modifier = Modifier.size(32.dp))
                    }
                } else if (matches.isEmpty()) {
                    BodyText("No matches found on other sources.", size = 13.sp, color = c.fgSubtle)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matches.forEachIndexed { i, match ->
                            SelectableCard(i == pickIdx, { pickIdx = i }, Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    RadioDot(i == pickIdx)
                                    CoverArt(coverColor(match.result.id), Modifier.size(40.dp, 60.dp), RoundedCornerShape(6.dp), imageUrl = match.result.coverUrl)
                                    Column(Modifier.weight(1f)) {
                                        BodyText(match.extensionName, weight = FontWeight.Bold)
                                        BodyText(match.result.title, size = 12.sp, color = c.fg.copy(alpha = 0.65f), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    Eyebrow("Bring along", Modifier.padding(bottom = 6.dp), color = c.fgSubtle)
                    MdCheckbox(keepRead, { keepRead = it }, "Read chapters")
                    MdCheckbox(deleteOld, { deleteOld = it }, "Delete old entry")
                }
            }
        }

        if (sourceManga != null) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MdButton(
                    "Copy",
                    {
                        val m = matches.getOrNull(pickIdx) ?: return@MdButton
                        vm.copy(m, onDone = onMigrate)
                    },
                    Modifier.weight(1f),
                    tone = ButtonTone.Ghost,
                    height = 52.dp,
                    fontSize = 14.sp,
                )
                MdButton(
                    "Migrate",
                    {
                        val m = matches.getOrNull(pickIdx) ?: return@MdButton
                        vm.migrate(m, keepRead, deleteOld, onDone = onMigrate)
                    },
                    Modifier.weight(2f),
                    height = 52.dp,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val c = MdTheme.colors
    Box(
        Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (selected) c.accent else c.borderStrong.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent))
    }
}
