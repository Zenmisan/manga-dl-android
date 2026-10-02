package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

private val accentSwatches = listOf(
    "#dc2626" to "Red",
    "#2563eb" to "Blue",
    "#7c3aed" to "Purple",
    "#16a34a" to "Green",
    "#ea580c" to "Orange",
    "#db2777" to "Pink",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val theme by prefs.theme.collectAsState(initial = "dark")
    val accentColor by prefs.accentColor.collectAsState(initial = "#dc2626")
    val incognitoMode by prefs.incognitoMode.collectAsState(initial = false)
    val newChapterAlerts by prefs.newChapterAlerts.collectAsState(initial = true)
    val backgroundUpdates by prefs.backgroundUpdates.collectAsState(initial = true)
    val downloadWifiOnly by prefs.downloadWifiOnly.collectAsState(initial = true)

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
                Text("General".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                // Appearance
                item {
                    SectionLabel("Appearance", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Theme", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0DFFFFFF))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("dark" to "Dark", "light" to "Light", "amoled" to "AMOLED").forEach { (value, label) ->
                                val active = theme == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (active) MangaDlColors.TextPrimary else Color.Transparent)
                                        .clickable { scope.launch { prefs.set(PrefKeys.THEME, value) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
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
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Accent color", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            accentSwatches.forEach { (hex, name) ->
                                val isSelected = accentColor == hex
                                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, MangaDlColors.TextPrimary, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { scope.launch { prefs.set(PrefKeys.ACCENT_COLOR, hex) } },
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Reading
                item {
                    SectionLabel("Reading", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SettingRow(
                        title = "New chapter alerts",
                        subtitle = "Notify when tracked manga updates",
                        trailing = {
                            MangaDlSwitch(checked = newChapterAlerts, onCheckedChange = { scope.launch { prefs.set(PrefKeys.NEW_CHAPTER_ALERTS, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    SettingRow(
                        title = "Background updates",
                        subtitle = "Check for chapters in background",
                        trailing = {
                            MangaDlSwitch(checked = backgroundUpdates, onCheckedChange = { scope.launch { prefs.set(PrefKeys.BACKGROUND_UPDATES, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    SettingRow(
                        title = "Download over Wi-Fi only",
                        subtitle = "Avoid mobile data charges",
                        trailing = {
                            MangaDlSwitch(checked = downloadWifiOnly, onCheckedChange = { scope.launch { prefs.set(PrefKeys.DOWNLOAD_WIFI_ONLY, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Privacy
                item {
                    SectionLabel("Privacy", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SettingRow(
                        title = "Incognito mode",
                        subtitle = "Hides reading activity from history",
                        trailing = {
                            MangaDlSwitch(checked = incognitoMode, onCheckedChange = { scope.launch { prefs.set(PrefKeys.INCOGNITO_MODE, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Data
                item {
                    SectionLabel("Data", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Image cache cleared")
                                }
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Clear image cache",
                            color = MangaDlColors.Primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
            }
        }
        trailing()
    }
}
