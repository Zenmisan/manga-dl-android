package com.mangadl.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.theme.MdType

enum class ButtonTone { Primary, Ghost, Danger, DangerFill, Soft, Success }

@Composable
fun MdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Primary,
    height: Dp = 54.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    fontSize: TextUnit = 15.sp,
    horizontalPadding: Dp = 20.dp,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = MdTheme.colors
    val (bg, fg, border) = when (tone) {
        ButtonTone.Primary -> Triple(c.accent, Color.White, Color.Transparent)
        ButtonTone.Ghost -> Triple(Color.Transparent, c.fg, c.borderStrong)
        ButtonTone.Danger -> Triple(Color.Transparent, c.errorText, c.errorBorder.copy(alpha = 0.6f))
        ButtonTone.DangerFill -> Triple(c.danger, Color.White, Color.Transparent)
        ButtonTone.Soft -> Triple(c.accentMuted, c.accentSoft, Color.Transparent)
        ButtonTone.Success -> Triple(c.successSoft, c.successText, Color.Transparent)
    }
    val weight = if (tone == ButtonTone.Ghost) FontWeight.Bold else FontWeight.ExtraBold
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(height)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(18.dp))
            HSpace(10.dp)
        }
        BodyText(text, size = fontSize, weight = weight, color = fg, maxLines = 1)
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Ghost,
    height: Dp = 36.dp,
) = MdButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    tone = tone,
    height = height,
    shape = CircleShape,
    fontSize = 12.sp,
    horizontalPadding = 14.dp,
)

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    fontSize: TextUnit = 13.sp,
) {
    val c = MdTheme.colors
    val shape = CircleShape
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (selected) c.fg else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else c.border, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BodyText(
            text = text,
            size = fontSize,
            weight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) c.bg else c.fgMuted,
            maxLines = 1,
        )
    }
}

@Composable
fun UnderlineTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MdTheme.colors
    Box(modifier.fillMaxWidth()) {
        Divider(Modifier.align(Alignment.BottomStart), c.dividerStrong)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            tabs.forEachIndexed { i, label ->
                val on = i == selected
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .clickable(role = Role.Tab) { onSelect(i) }
                        .semantics { this.selected = on },
                ) {
                    Box(Modifier.height(42.dp), contentAlignment = Alignment.Center) {
                        BodyText(
                            label,
                            size = 14.sp,
                            weight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (on) c.fg else c.fgSubtle,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) c.accent else Color.Transparent))
                }
            }
        }
    }
}

@Composable
fun MdSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 28.dp,
) {
    val c = MdTheme.colors
    val knob = height - 6.dp
    val x by animateDpAsState(if (checked) width - knob - 3.dp else 3.dp, label = "switch")
    Box(
        modifier
            .size(width, height)
            .clip(CircleShape)
            .background(if (checked) c.accent else c.switchOff)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(knob)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun MdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    height: Dp = 50.dp,
    multiline: Boolean = false,
) {
    val c = MdTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) BodyText(label, size = 13.sp, weight = FontWeight.SemiBold, color = c.fg.copy(alpha = 0.8f))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            textStyle = MdType.body(15.sp).copy(color = c.fg),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .then(if (multiline) Modifier.heightIn(min = height) else Modifier.height(height))
                        .clip(shape)
                        .background(if (isError) c.accentFaint else c.surfaceRaised)
                        .border(1.dp, if (isError) c.errorBorder else c.border, shape)
                        .padding(horizontal = 14.dp, vertical = if (multiline) 12.dp else 0.dp),
                    contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) BodyText(placeholder, size = 15.sp, color = c.placeholder)
                    inner()
                }
            },
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = MdTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(c.surfaceRaised)
            .border(1.dp, if (focused) c.accent else c.track, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(MdIcons.Search, null, tint = c.fgSubtle, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MdType.body(15.sp).copy(color = c.fg),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) BodyText(placeholder, size = 15.sp, color = c.fgSubtle)
                    inner()
                }
            },
        )
        trailing?.invoke()
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
) {
    val c = MdTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.fg else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                BodyText(option, size = 13.sp, weight = FontWeight.Bold, color = if (on) c.bg else c.fgMuted, maxLines = 1)
            }
        }
    }
}

@Composable
fun AccentSwatches(selected: Accent, onSelect: (Accent) -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val c = MdTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Accent.entries.forEach { accent ->
            val on = accent == selected
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, if (on) c.fg else Color.Transparent, CircleShape)
                    .clickable(role = Role.RadioButton) { onSelect(accent) }
                    .semantics { this.selected = on }
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(accent.main),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val c = MdTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        thumb = {
            Box(Modifier.size(16.dp).clip(CircleShape).background(c.fg))
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span == 0f) 0f else (state.value - state.valueRange.start) / span
            ProgressBar(fraction, color = c.accent, track = c.fg.copy(alpha = 0.18f))
        },
    )
}

@Composable
fun <T> rememberState(initial: T) = remember { androidx.compose.runtime.mutableStateOf(initial) }
