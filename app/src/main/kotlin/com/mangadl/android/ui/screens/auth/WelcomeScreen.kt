package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.AntonFontFamily
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onGuest: () -> Unit,
) {
    val glowColor = MangaDlColors.Primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        // Red radial glow centered upper area
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopCenter)
                .offset(y = 120.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.35f),
                                glowColor.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension / 2f,
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // App badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MangaDlColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "M",
                    fontFamily = AntonFontFamily,
                    fontSize = 38.sp,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "YOUR MANGA,\nEVERYWHERE.",
                style = AntonStyle.copy(
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center,
                ),
                color = MangaDlColors.TextPrimary,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Your library, synced across every device.",
                color = MangaDlColors.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            // Start Reading — primary CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MangaDlColors.Primary)
                    .clickable(onClick = onGuest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Start Reading",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Sign In — secondary CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                    .clickable(onClick = onSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Sign In",
                    color = MangaDlColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Create an account",
                color = MangaDlColors.PrimaryLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onCreateAccount),
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun WelcomeScreenPreview() {
    MangaDlTheme {
        WelcomeScreen(
            onSignIn = {},
            onCreateAccount = {},
            onGuest = {},
        )
    }
}
