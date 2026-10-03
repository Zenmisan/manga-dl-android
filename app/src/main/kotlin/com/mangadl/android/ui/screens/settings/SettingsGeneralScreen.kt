package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
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
    val ambilight by prefs.ambilight.collectAsState(initial = false)
    val incognitoMode by prefs.incognitoMode.collectAsState(initial = false)
    val newChapterAlerts by prefs.newChapterAlerts.collectAsState(initial = true)
    val backgroundUpdates by prefs.backgroundUpdates.collectAsState(initial = true)
    val downloadWifiOnly by prefs.downloadWifiOnly.collectAsState(initial = true)
    val backendUrl by prefs.backendUrl.collectAsState(initial = "")
    val apiKey by prefs.apiKey.collectAsState(initial = "")

    var backendUrlDraft by remember(backendUrl) { mutableStateOf(backendUrl) }
    var apiKeyDraft by remember(apiKey) { mutableStateOf(apiKey) }
    var connectionStatus by remember { mutableStateOf("connected") }
    var apiKeyVisible by remember { mutableStateOf(false) }

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
                // ── Appearance ────────────────────────────────────────────
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
                            listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (value, label) ->
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
                            accentSwatches.forEach { (hex, _) ->
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
                item {
                    SettingRow(
                        title = "Ambilight from covers",
                        subtitle = "Tint headers and status bar with cover colour",
                        trailing = {
                            MangaDlSwitch(checked = ambilight, onCheckedChange = { scope.launch { prefs.set(PrefKeys.AMBILIGHT, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // ── Connection ────────────────────────────────────────────
                item {
                    SectionLabel("Connection", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Backend URL", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Used for sync and backups only", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                        SettingsTextField(
                            value = backendUrlDraft,
                            onValueChange = { backendUrlDraft = it },
                            placeholder = "https://[your-server]:8000",
                            onDone = { scope.launch { prefs.set(PrefKeys.BACKEND_URL, backendUrlDraft) } },
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("API key", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        SettingsTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            placeholder = "••••••••",
                            isPassword = !apiKeyVisible,
                            onDone = { scope.launch { prefs.set(PrefKeys.API_KEY, apiKeyDraft) } },
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Status: $connectionStatus",
                                color = if (connectionStatus == "connected") Color(0xFF22c55e) else MangaDlColors.TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("Last checked [time]", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(999.dp))
                                .clickable { connectionStatus = "checking…" }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Test", color = MangaDlColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // ── Behaviour ─────────────────────────────────────────────
                item {
                    SectionLabel("Behaviour", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    SettingRow(
                        title = "Incognito mode",
                        subtitle = "Pause history while reading",
                        trailing = {
                            MangaDlSwitch(checked = incognitoMode, onCheckedChange = { scope.launch { prefs.set(PrefKeys.INCOGNITO_MODE, it) } })
                        },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    SettingRow(
                        title = "Push notifications",
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

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    onDone: () -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111111))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        textStyle = TextStyle(color = MangaDlColors.TextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(MangaDlColors.Primary),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Uri,
        ),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = MangaDlColors.TextSecondary, fontSize = 14.sp)
            }
            inner()
        },
    )
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
            .padding(vertical = 14.dp),
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun SettingsGeneralScreenPreview() {
    MangaDlTheme {
        Column(Modifier.fillMaxSize().background(MangaDlColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MangaDlColors.TextPrimary, modifier = Modifier.padding(8.dp))
                Text("GENERAL", style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }
            SectionLabel("Appearance", color = MangaDlColors.SectionRed, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0DFFFFFF))
                    .padding(4.dp),
            ) {
                listOf("Dark", "Light", "System").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (i == 0) MangaDlColors.TextPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (i == 0) MangaDlColors.Background else MangaDlColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
