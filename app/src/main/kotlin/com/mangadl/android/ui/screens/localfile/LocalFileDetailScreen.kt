package com.mangadl.android.ui.screens.localfile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.LocalFileViewModel
import java.io.File

@Composable
fun LocalFileDetailScreen(
    onBack: () -> Unit,
    onRead: () -> Unit,
) {
    val c = MdTheme.colors
    val vm: LocalFileViewModel = viewModel()
    val title by vm.title.collectAsState()
    val pages by vm.pages.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.processUri(it) } }

    // Auto-launch picker if no file loaded yet
    LaunchedEffect(Unit) {
        if (pages.isEmpty() && !loading) {
            filePicker.launch(arrayOf("application/zip", "application/x-cbz", "application/octet-stream", "*/*"))
        }
    }

    Screen {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MdIconButton(MdIcons.Back, "Back", onBack)
            MdIconButton(MdIcons.More, "More options", {})
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = c.accentSoft)
                    BodyText("Extracting pages…", size = 14.sp, color = c.fgSubtle)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BodyText(error!!, size = 14.sp, color = c.fg.copy(alpha = 0.7f))
                    MdButton("Try Another File", { filePicker.launch(arrayOf("*/*")) }, height = 44.dp, fontSize = 13.sp)
                }
            }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BodyText("No file selected.", size = 14.sp, color = c.fgSubtle)
                    MdButton("Choose CBZ File", { filePicker.launch(arrayOf("*/*")) }, height = 44.dp, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Cover = first page thumbnail
                        Box(
                            Modifier
                                .size(104.dp, 156.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF22222A))
                                .border(1.dp, c.border, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = File(pages.first()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Column(Modifier.align(Alignment.Bottom), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BodyText(
                                "Local Upload",
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.accentMuted).padding(horizontal = 8.dp, vertical = 4.dp),
                                size = 11.sp, weight = FontWeight.ExtraBold, color = c.accentLight,
                            )
                            DisplayText(title.ifEmpty { "Local File" }, 28.sp, lineHeight = 30.sp)
                            BodyText("${pages.size} pages", size = 13.sp, color = c.fgSubtle)
                        }
                    }
                }
                item {
                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdButton("Start Reading", onRead, Modifier.weight(2f), height = 50.dp, fontSize = 14.sp)
                        MdButton("Change", { filePicker.launch(arrayOf("*/*")) }, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 50.dp, fontSize = 14.sp)
                    }
                    Eyebrow("Pages", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp), color = c.fgSubtle)
                }
                itemsIndexed(pages) { i, path ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(44.dp, 66.dp).clip(RoundedCornerShape(6.dp)).background(c.surfaceRaised),
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        BodyText("Page ${i + 1}", Modifier.weight(1f), size = 14.sp, weight = FontWeight.SemiBold)
                    }
                }
                item { Box(Modifier.width(1.dp).height(32.dp)) }
            }
        }
    }
}
