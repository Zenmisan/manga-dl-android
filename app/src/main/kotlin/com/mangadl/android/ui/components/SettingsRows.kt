package com.mangadl.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(title, Modifier.padding(top = 20.dp, bottom = 4.dp))
        content()
    }
}

@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    labelColor: Color = MdTheme.colors.fg,
    onClick: (() -> Unit)? = null,
    below: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                BodyText(label, size = 15.sp, weight = FontWeight.SemiBold, color = labelColor)
                if (description != null) {
                    VSpace(2.dp)
                    BodyText(description, size = 12.sp, color = MdTheme.colors.fgSubtle, lineHeight = 17.sp)
                }
            }
            trailing?.invoke()
        }
        if (below != null) {
            below()
            VSpace(12.dp)
        }
        Divider()
    }
}

@Composable
fun SwitchSetting(
    label: String,
    initial: Boolean,
    description: String? = null,
    value: Boolean? = null,
    onValueChange: ((Boolean) -> Unit)? = null,
) {
    var on by rememberState(initial)
    val actualOn = value ?: on
    val actualSet: (Boolean) -> Unit = onValueChange ?: { on = it }
    SettingRow(label, description = description, onClick = { actualSet(!actualOn) }) {
        MdSwitch(actualOn, actualSet)
    }
}

@Composable
fun ValueSetting(label: String, value: String, description: String? = null, onClick: () -> Unit = {}) {
    val c = MdTheme.colors
    SettingRow(label, description = description, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (value.isNotEmpty()) BodyText(value, size = 13.sp, weight = FontWeight.SemiBold, color = c.fgMuted)
            Icon(MdIcons.ChevronRight, null, tint = c.fgMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ButtonSetting(
    label: String,
    buttonText: String,
    description: String? = null,
    tone: ButtonTone = ButtonTone.Ghost,
    onClick: () -> Unit = {},
) {
    SettingRow(label, description = description) {
        PillButton(buttonText, onClick, tone = tone, height = 38.dp)
    }
}

@Composable
fun SegmentedSetting(
    label: String,
    options: List<String>,
    initial: String,
    description: String? = null,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    var v by rememberState(initial)
    val actualV = value ?: v
    val actualSet: (String) -> Unit = onValueChange ?: { v = it }
    SettingRow(label, description = description, below = { Segmented(options, actualV, actualSet) })
}

@Composable
fun SliderSetting(
    label: String,
    initial: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String? = null,
    valueLabel: (Float) -> String = { it.toInt().toString() },
    value: Float? = null,
    onValueChange: ((Float) -> Unit)? = null,
) {
    // Same local-first pattern as InputSetting: `value` is typically DataStore-backed and lags a
    // frame behind each drag event, which would otherwise make the thumb visibly snap back.
    var local by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(value ?: initial) }
    val actualSet: (Float) -> Unit = { local = it; (onValueChange ?: {})(it) }
    SettingRow(
        label,
        description = description,
        below = {
            Column {
                MdSlider(local, actualSet, valueRange = valueRange)
                BodyText(valueLabel(local), size = 12.sp, color = MdTheme.colors.fgSubtle)
            }
        },
    )
}

@Composable
fun AccentSetting(label: String, initial: Accent, onSelect: (Accent) -> Unit = {}) {
    var value by rememberState(initial)
    SettingRow(label, below = {
        AccentSwatches(value, {
            value = it
            onSelect(it)
        })
    })
}

@Composable
fun InputSetting(
    label: String,
    initial: String,
    description: String? = null,
    isPassword: Boolean = false,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    // Seeded once from `value`/`initial`, then purely local: `value` here is typically backed by
    // an async DataStore Flow, so re-reading it every keystroke would show the stale pre-write
    // string for a frame and reset the cursor to the start on every character typed. Edits are
    // still forwarded via onValueChange for persistence; the field itself is the source of truth
    // for what's on screen.
    var local by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(value ?: initial) }
    val actualSet: (String) -> Unit = { local = it; (onValueChange ?: { v -> local = v })(it) }
    SettingRow(label, description = description, below = {
        MdTextField(local, actualSet, isPassword = isPassword, height = 46.dp)
    })
}
