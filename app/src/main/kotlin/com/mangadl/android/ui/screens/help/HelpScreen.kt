package com.mangadl.android.ui.screens.help

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors

private data class FaqItem(val question: String, val answer: String)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val faqs = listOf(
        FaqItem(
            "How do I add manga to my library?",
            "Tap Browse, pick a source, search for manga, open it and tap the heart icon on the detail page.",
        ),
        FaqItem(
            "How do I download chapters?",
            "On the manga detail page, long-press a chapter and tap Download. You can also download all chapters from the chapter menu.",
        ),
        FaqItem(
            "How do I sync across devices?",
            "Sign in with your account — your library syncs automatically through the cloud.",
        ),
        FaqItem(
            "Why are some images not loading?",
            "Some sources require a browser user-agent. If images fail, try a different source for the same title.",
        ),
        FaqItem(
            "What sources are available?",
            "Tap Browse to see all available sources. Built-in sources need no setup. You can browse and read without signing in.",
        ),
        FaqItem(
            "How does reading progress work?",
            "Progress syncs automatically when you read. The continue card on your Library screen shows your last position.",
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MangaDlColors.TextPrimary,
                )
            }
            Text(
                "HELP",
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "FREQUENTLY ASKED",
                    color = MangaDlColors.SectionRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(faqs.size) { i ->
                val faq = faqs[i]
                FaqCard(
                    faq = faq,
                    isExpanded = expandedItem == faq.question,
                    onToggle = {
                        expandedItem = if (expandedItem == faq.question) null else faq.question
                    },
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                // Contact support button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MangaDlColors.Primary)
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@manga-dl.app")
                                putExtra(Intent.EXTRA_SUBJECT, "Support Request — manga-dl Android")
                            }
                            context.startActivity(Intent.createChooser(intent, "Contact Support"))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Contact Support",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MangaDlColors.CardBg)
                        .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MangaDlColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Column {
                        Text(
                            "Version",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                        Text(
                            "manga-dl 1.0.0 (Android)",
                            color = MangaDlColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem, isExpanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                faq.question,
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MangaDlColors.CardBorder),
                )
                Text(
                    faq.answer,
                    color = MangaDlColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
