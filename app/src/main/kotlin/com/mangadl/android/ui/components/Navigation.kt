package com.mangadl.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.MdTheme

enum class MainTab(val label: String, val icon: ImageVector) {
    Library("Library", MdIcons.Library),
    Updates("Updates", MdIcons.Updates),
    History("History", MdIcons.History),
    Browse("Browse", MdIcons.Browse),
    More("More", MdIcons.Menu),
}

@Composable
fun MdBottomNav(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = MdTheme.colors
    Column(modifier.fillMaxWidth().background(c.navBg)) {
        Divider(color = c.dividerStrong)
        Row(Modifier.fillMaxWidth().height(72.dp).navigationBarsPadding()) {
            MainTab.entries.forEach { tab ->
                NavItem(tab, tab == selected, { onSelect(tab) }, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
fun MdNavRail(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = MdTheme.colors
    Row(modifier.fillMaxHeight()) {
        Column(
            Modifier
                .width(96.dp)
                .fillMaxHeight()
                .background(c.navBg)
                .statusBarsPadding()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.accent),
                contentAlignment = Alignment.Center,
            ) { DisplayText("M", 20.sp) }
            VSpace(16.dp)
            MainTab.entries.forEach { tab ->
                NavItem(tab, tab == selected, { onSelect(tab) }, Modifier.width(80.dp).padding(vertical = 8.dp))
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.dividerStrong.copy(alpha = 0.08f)))
    }
}

@Composable
private fun NavItem(tab: MainTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = MdTheme.colors
    Column(
        modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(56.dp, 30.dp)
                .clip(CircleShape)
                .background(if (selected) c.accentNav else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = null, tint = if (selected) c.accentLight else c.fgSubtle, modifier = Modifier.size(22.dp))
        }
        BodyText(
            tab.label,
            size = 11.sp,
            weight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) c.fg else c.fgSubtle,
        )
    }
}
