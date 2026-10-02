package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.db.AppDatabase
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val updateInterval by prefs.updateInterval.collectAsState(initial = "manual")
    val autoDownloadNew by prefs.autoDownloadNew.collectAsState(initial = false)
    val libraryDisplay by prefs.libraryDisplay.collectAsState(initial = "grid")

    var showResetProgressDialog by remember { mutableStateOf(false) }
    var showRemoveAllDialog by remember { mutableStateOf(false) }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Reset reading progress?", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all reading progress for every manga. This cannot be undone.", color = MangaDlColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetProgressDialog = false
                    scope.launch {
                        db.progressDao().deleteAll()
                        snackbarHostState.showSnackbar("Reading progress reset")
                    }
                }) {
                    Text("Reset", color = MangaDlColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text("Cancel", color = MangaDlColors.TextSecondary)
                }
            },
        )
    }

    if (showRemoveAllDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAllDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Remove all from library?", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove every manga from your library. Downloads will remain. This cannot be undone.", color = MangaDlColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAllDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Not yet implemented")
                    }
                }) {
                    Text("Remove all", color = MangaDlColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllDialog = false }) {
                    Text("Cancel", color = MangaDlColors.TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = MangaDlColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
                }
                Text("Library".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                // Updates
                item {
                    SectionLabel("Updates", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    LibrarySegmentedRow(
                        label = "Update interval",
                        options = listOf("Manual" to "manual", "12h" to "12h", "24h" to "24h", "48h" to "48h"),
                        selected = updateInterval,
                        onSelect = { scope.launch { prefs.set(PrefKeys.UPDATE_INTERVAL, it) } },
                    )
                }
                item {
                    LibraryToggleRow(
                        title = "Auto-download new chapters",
                        subtitle = "Download new chapters automatically when found",
                        checked = autoDownloadNew,
                        onCheckedChange = { scope.launch { prefs.set(PrefKeys.AUTO_DOWNLOAD_NEW, it) } },
                    )
                }

                // Display
                item {
                    SectionLabel("Display", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    LibrarySegmentedRow(
                        label = "Library display",
                        options = listOf("Grid" to "grid", "List" to "list"),
                        selected = libraryDisplay,
                        onSelect = { scope.launch { prefs.set(PrefKeys.LIBRARY_DISPLAY, it) } },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Default category", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Categories coming soon", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                        }
                        Text("All", color = MangaDlColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Danger zone
                item {
                    SectionLabel("Danger zone", color = MangaDlColors.Primary, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { showResetProgressDialog = true }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Reset reading progress", color = MangaDlColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { showRemoveAllDialog = true }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Remove all from library", color = MangaDlColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun LibraryToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = MangaDlColors.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        MangaDlSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

@Composable
private fun LibrarySegmentedRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0DFFFFFF))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (optLabel, optValue) ->
                val active = selected == optValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) MangaDlColors.TextPrimary else Color.Transparent)
                        .clickable { onSelect(optValue) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        optLabel,
                        color = if (active) MangaDlColors.Background else MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}
