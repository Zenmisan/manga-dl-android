package com.mangadl.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTrackersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val anilistConnected by prefs.anilistConnected.collectAsState(initial = false)
    val malConnected by prefs.malConnected.collectAsState(initial = false)

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
                Text("Trackers".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }

            Text(
                text = "Sync your reading progress with tracking services",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(12.dp)) }

                // AniList
                item {
                    TrackerCard(
                        name = "AniList",
                        subtitle = "Anime/Manga tracking",
                        avatarColor = Color(0xFF00BCD4),
                        avatarLetter = "A",
                        connected = anilistConnected,
                        onConnect = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connecting via browser...")
                            }
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://anilist.co/api/v2/oauth/authorize?client_id=51213&response_type=token"),
                            )
                            context.startActivity(intent)
                        },
                        onDisconnect = {
                            scope.launch {
                                prefs.set(PrefKeys.ANILIST_CONNECTED, false)
                                snackbarHostState.showSnackbar("AniList disconnected")
                            }
                        },
                    )
                }

                // MyAnimeList
                item {
                    TrackerCard(
                        name = "MyAnimeList",
                        subtitle = "Anime/Manga tracking",
                        avatarColor = Color(0xFF2E51A2),
                        avatarLetter = "M",
                        connected = malConnected,
                        onConnect = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connecting via browser...")
                            }
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://myanimelist.net/v1/oauth2/authorize"),
                            )
                            context.startActivity(intent)
                        },
                        onDisconnect = {
                            scope.launch {
                                prefs.set(PrefKeys.MAL_CONNECTED, false)
                                snackbarHostState.showSnackbar("MyAnimeList disconnected")
                            }
                        },
                    )
                }

                // Kitsu placeholder
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MangaDlColors.CardBg)
                            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6B35)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("K", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Kitsu", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Anime/Manga tracking", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x22FFFFFF))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("Coming soon", color = MangaDlColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TrackerCard(
    name: String,
    subtitle: String,
    avatarColor: Color,
    avatarLetter: String,
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(avatarLetter, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(name, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                if (connected) {
                    Spacer(Modifier.height(4.dp))
                    Text("Connected", color = Color(0xFF4ade80), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(999.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0x44FFFFFF)),
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MangaDlColors.TextSecondary),
                ) {
                    Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MangaDlColors.Primary),
                ) {
                    Text("Connect", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
