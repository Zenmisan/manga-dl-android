package com.mangadl.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun Screen(
    modifier: Modifier = Modifier,
    background: Color = MdTheme.colors.bg,
    insetBottom: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .then(if (insetBottom) Modifier.navigationBarsPadding() else Modifier),
        content = content,
    )
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: TextUnit = 14.sp) {
    BodyText(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        size = size,
        weight = FontWeight.Bold,
        color = MdTheme.colors.accentLight,
    )
}

@Composable
fun MdCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = MdTheme.colors
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier
            .heightIn(min = 44.dp)
            .toggleable(checked, onValueChange = onCheckedChange, role = Role.Checkbox),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(shape)
                .background(if (checked) c.accent else Color.Transparent)
                .border(1.5.dp, if (checked) c.accent else c.borderStrong.copy(alpha = 0.4f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(MdIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        BodyText(label, size = 14.sp, color = c.fg.copy(alpha = 0.85f))
    }
}

@Composable
fun SelectableCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = MdTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) c.accentFaint else Color.Transparent)
            .border(1.dp, if (selected) c.accent else c.dividerStrong, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
    ) { content() }
}

@Composable
fun PagePlaceholder(label: String, modifier: Modifier = Modifier) {
    val ink = Color(0xFF1A1A1A)
    Column(
        modifier.background(Color(0xFFE9E6DF)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val panel = Modifier.border(2.dp, ink)
        Box(panel.weight(1f).fillMaxSize().background(Color(0xFFD4D0C6)), contentAlignment = Alignment.Center) {
            BodyText(label, size = 12.sp, weight = FontWeight.Bold, color = Color(0xFF3A3A3A))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(panel.weight(1f).fillMaxSize().background(Color(0xFFDCD8CF)))
            Box(panel.weight(1f).fillMaxSize().background(Color(0xFFCFCAC0)))
        }
        Box(panel.weight(1f).fillMaxSize().background(Color(0xFFD8D4CA)))
    }
}
