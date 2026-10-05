package com.mangadl.android.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.Notice
import com.mangadl.android.data.ui.NoticeKind
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun NotificationsScreen(notices: List<Notice>, onBack: () -> Unit) {
    val c = MdTheme.colors
    var list by rememberState(notices)
    Screen {
        BackHeader("Notifications", onBack) {
            TextLink("Mark All Read", { list = list.map { it.copy(unread = false) } }, size = 13.sp)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(list) { n ->
                val (iconBg, iconFg) = when (n.kind) {
                    NoticeKind.Error -> c.errorSoft to c.errorText
                    NoticeKind.Success -> c.successSoft to c.successText
                    NoticeKind.Info -> c.surfaceHigh to c.fg
                }
                val icon = when (n.icon) {
                    "warn" -> MdIcons.Warning
                    "download" -> MdIcons.Download
                    "sync" -> MdIcons.Refresh
                    "up" -> MdIcons.ArrowUp
                    else -> MdIcons.Updates
                }
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (n.unread) c.accent.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable {}
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = iconFg, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BodyText(n.title, weight = FontWeight.Bold)
                            BodyText(n.body, size = 13.sp, color = c.fg.copy(alpha = 0.7f), lineHeight = 18.sp)
                            BodyText(n.whenText, size = 12.sp, color = c.fgFaint)
                        }
                        if (n.unread) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(c.accent))
                    }
                    Divider()
                }
            }
        }
    }
}
