package com.mangadl.android.ui.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import androidx.compose.ui.tooling.preview.Preview
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAccountScreen(onBack: () -> Unit, onSignOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val session = remember { SupabaseManager.client.auth.currentSessionOrNull() }
    val email = session?.user?.email ?: "Guest"
    val displayName = session?.user?.userMetadata
        ?.let { it["full_name"]?.toString()?.trim('"') }
        ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
    val initial = displayName.firstOrNull()?.uppercaseChar() ?: 'G'

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Sign out?", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("You'll be signed out of your account.", color = MangaDlColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    scope.launch {
                        try {
                            SupabaseManager.client.auth.signOut()
                        } catch (_: Exception) {}
                        onSignOut()
                    }
                }) {
                    Text("Sign out", color = MangaDlColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = MangaDlColors.TextSecondary)
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Delete account?", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This action is permanent and cannot be undone. All your data will be deleted.", color = MangaDlColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Account deletion is not yet implemented")
                    }
                }) {
                    Text("Delete", color = MangaDlColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
                Text("Account".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                // Profile card
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MangaDlColors.CardBg)
                            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MangaDlColors.AvatarBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initial.toString(),
                                    color = MangaDlColors.PrimaryLight,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(displayName, color = MangaDlColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(email, color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Connected services
                item {
                    SectionLabel("Connected services", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    ConnectedServiceRow(name = "AniList", connected = false)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }
                item {
                    ConnectedServiceRow(name = "MyAnimeList", connected = false)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                }

                // Account actions
                item {
                    SectionLabel("Account", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MangaDlColors.Primary),
                    ) {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MangaDlColors.Primary),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MangaDlColors.Primary),
                    ) {
                        Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ConnectedServiceRow(name: String, connected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (connected) Color(0x2216a34a) else Color(0x22FFFFFF))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (connected) "Connected" else "Not connected",
                color = if (connected) Color(0xFF4ade80) else MangaDlColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun SettingsAccountScreenPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MangaDlColors.TextPrimary, modifier = Modifier.padding(8.dp))
                Text("ACCOUNT", style = AntonStyleSub, color = MangaDlColors.TextPrimary)
            }
            SectionLabel("Profile", color = MangaDlColors.SectionRed, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp))
        }
    }
}
