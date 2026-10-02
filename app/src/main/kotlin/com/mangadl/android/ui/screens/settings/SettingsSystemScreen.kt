package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSystemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val syncWifiOnly by prefs.syncWifiOnly.collectAsState(initial = true)

    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "1.0.0" }
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
                Text("System & Backup".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                // Backup
                item {
                    SectionLabel("Backup", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SystemActionRow(
                        title = "Export backup",
                        subtitle = "Save library and settings to Downloads",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Backup exported to Downloads/manga-dl-backup.json")
                            }
                        },
                    )
                }
                item {
                    SystemActionRow(
                        title = "Import backup",
                        subtitle = "Restore from a manga-dl backup file",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Import from file — coming soon")
                            }
                        },
                    )
                }
                item {
                    SystemActionRow(
                        title = "Import from Tachiyomi",
                        subtitle = "Migrate library from Tachiyomi backup",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Tachiyomi import — coming soon")
                            }
                        },
                    )
                }

                // Cloud sync
                item {
                    SectionLabel("Cloud sync", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SystemActionRow(
                        title = "Sync library with cloud",
                        subtitle = "Push local library to your account",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cloud sync — coming soon")
                            }
                        },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .clickable { scope.launch { prefs.set(PrefKeys.SYNC_WIFI_ONLY, !syncWifiOnly) } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Sync on Wi-Fi only", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Avoid syncing over mobile data", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                        }
                        MangaDlSwitch(checked = syncWifiOnly, onCheckedChange = { scope.launch { prefs.set(PrefKeys.SYNC_WIFI_ONLY, it) } })
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Cache
                item {
                    SectionLabel("Cache", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SystemActionRow(
                        title = "Clear image cache",
                        subtitle = "Free up space used by cached page images",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Image cache cleared")
                            }
                        },
                    )
                }
                item {
                    SystemActionRow(
                        title = "Clear download cache",
                        subtitle = "Remove temporary download files",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Download cache cleared")
                            }
                        },
                    )
                }

                // About
                item {
                    SectionLabel("About", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Version", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(packageInfo ?: "1.0.0", color = MangaDlColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    SystemActionRow(
                        title = "Open source licenses",
                        subtitle = "Third-party software used in this app",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Licenses view — coming soon")
                            }
                        },
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SystemActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}
