package com.mangadl.android.ui.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import androidx.compose.foundation.clickable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = MdTheme.colors
    val faqs = listOf(
        "Where are downloads saved?" to "As CBZ files with ComicInfo.xml. Change the folder in Settings › System › Download location.",
        "How do I add a source?" to "Open Browse › Extensions and install one, or connect Komga or Suwayomi in Settings › System.",
        "How does cloud sync work?" to "Sign in and your library, history and progress sync through your backend automatically.",
        "Why won't a chapter load?" to "The source may be down or rate-limiting. Retry, or open it in WebView to check.",
    )
    var open by rememberState(0)
    var category by rememberState("Bug Report")
    var message by rememberState("")
    Screen {
        BackHeader("Help center", onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column {
                Eyebrow("Frequently asked", Modifier.padding(bottom = 6.dp))
                faqs.forEachIndexed { i, (q, a) ->
                    val expanded = open == i
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable(role = Role.Button) { open = if (expanded) -1 else i },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BodyText(q, Modifier.weight(1f), size = 15.sp, weight = if (expanded) FontWeight.Bold else FontWeight.SemiBold)
                            Icon(if (expanded) MdIcons.ChevronUp else MdIcons.ChevronDown, null, tint = c.fg, modifier = Modifier.size(18.dp))
                        }
                        if (expanded) BodyText(a, Modifier.padding(bottom = 14.dp), color = c.fgMuted, lineHeight = 21.sp)
                        Divider(color = c.surfaceHigh)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Contact support", color = c.fgSubtle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Bug Report", "Feature Request", "Account", "Source / Extension").forEach {
                        PillChip(it, it == category, { category = it }, fontSize = 12.sp)
                    }
                }
                MdTextField(
                    message, { message = it },
                    label = "Message",
                    placeholder = "What happened? Include the source and chapter if it's a loading issue.",
                    multiline = true, height = 110.dp,
                )
                MdButton("Send Message", {}, Modifier.fillMaxWidth(), height = 50.dp, fontSize = 14.sp)
            }
        }
    }
}
