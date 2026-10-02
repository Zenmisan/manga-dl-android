package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onGuest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("MANGA-DL", style = AntonStyle, color = MangaDlColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your manga, anywhere.",
            color = MangaDlColors.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(40.dp))
        listOf(
            "Your library synced across all devices",
            "Read offline — chapters download automatically",
            "100+ sources, no account required to browse",
        ).forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MangaDlColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(feature, color = MangaDlColors.TextSecondary, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        // Sign In button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MangaDlColors.Primary)
                .clickable(onClick = onSignIn),
            contentAlignment = Alignment.Center,
        ) {
            Text("Sign In", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        // Create Account button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Transparent)
                .border(width = 1.dp, color = Color(0x29FFFFFF), shape = RoundedCornerShape(999.dp))
                .clickable(onClick = onCreateAccount),
            contentAlignment = Alignment.Center,
        ) {
            Text("Create Account", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Continue as guest",
            color = MangaDlColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onGuest),
        )
        Spacer(Modifier.height(40.dp))
    }
}
