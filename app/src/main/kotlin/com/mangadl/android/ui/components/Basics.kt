package com.mangadl.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.theme.MdType

@Composable
fun DisplayText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = MdTheme.colors.fg,
    lineHeight: TextUnit = size,
    maxLines: Int = Int.MAX_VALUE,
    uppercase: Boolean = true,
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        color = color,
        style = MdType.display(size, lineHeight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = MdTheme.colors.fg,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MdType.body(size, weight, lineHeight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MdTheme.colors.accentLight) {
    Text(text.uppercase(), modifier = modifier, color = color, style = MdType.eyebrow)
}

@Composable
fun MdIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MdTheme.colors.fg,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun TabHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DisplayText(title, 30.sp, Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = actions)
    }
}

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 26.sp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MdIconButton(MdIcons.Back, "Back", onBack)
        DisplayText(title, titleSize, Modifier.weight(1f), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, height: Dp = 22.dp) {
    val c = MdTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = height)
            .height(height)
            .clip(CircleShape)
            .background(c.accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BodyText(count.toString(), size = 11.sp, weight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
fun InLibraryTag(modifier: Modifier = Modifier) {
    BodyText(
        "In Library",
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MdTheme.colors.accent)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        size = 10.sp,
        weight = FontWeight.ExtraBold,
        color = Color.White,
    )
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MdTheme.colors.accent,
    track: Color = MdTheme.colors.track,
    height: Dp = 4.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun CoverArt(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Color(0xFF1A2433),
    shape: Shape = RoundedCornerShape(10.dp),
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val url = imageUrl?.trim()
    val resolvedUrl = androidx.compose.runtime.remember(url) {
        if (!url.isNullOrBlank() && url.startsWith("/")) {
            "${com.mangadl.android.BuildConfig.BACKEND_URL.trimEnd('/')}/api$url"
        } else url
    }
    val referer = androidx.compose.runtime.remember(resolvedUrl) {
        if (!resolvedUrl.isNullOrBlank() && resolvedUrl.startsWith("http")) {
            resolvedUrl.split("/").take(3).joinToString("/") + "/"
        } else ""
    }

    Box(modifier.clip(shape).background(placeholderColor)) {
        if (!resolvedUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .apply {
                        if (referer.isNotEmpty()) addHeader("Referer", referer)
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(placeholderColor),
                error = androidx.compose.ui.graphics.painter.ColorPainter(placeholderColor),
            )
        }
        content()
    }
}

@Composable
fun CoverArt(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    imageUrl: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    CoverArt(
        imageUrl = imageUrl,
        modifier = modifier,
        placeholderColor = color,
        shape = shape,
        content = content,
    )
}

@Composable
fun CoverArt(
    manga: com.mangadl.android.data.ui.Manga,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    dimmed: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val placeholder = if (dimmed) manga.cover.copy(alpha = 0.55f) else manga.cover
    CoverArt(
        imageUrl = manga.coverUrl,
        modifier = modifier,
        placeholderColor = placeholder,
        shape = shape,
        contentDescription = contentDescription ?: manga.title,
        content = content,
    )
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    background: Color = MdTheme.colors.surfaceRaised,
    borderColor: Color = MdTheme.colors.dividerStrong,
    padding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = MdTheme.colors.divider) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun Bloom(modifier: Modifier = Modifier, color: Color = MdTheme.colors.accent, alpha: Float = 0.24f) {
    Spacer(
        modifier.drawBehind {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
            )
        },
    )
}

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val c = MdTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(c.track))
        BodyText("OR", size = 12.sp, weight = FontWeight.Bold, color = c.fgFaint)
        Box(Modifier.weight(1f).height(1.dp).background(c.track))
    }
}

@Composable
fun HSpace(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun LogoTile(text: String, color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp, anton: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (anton) DisplayText(text, 16.sp) else BodyText(text, size = 12.sp, weight = FontWeight.Black)
    }
}

@Composable
fun LabelStack(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 15.sp,
    titleWeight: FontWeight = FontWeight.SemiBold,
    titleColor: Color = MdTheme.colors.fg,
    subtitleColor: Color = MdTheme.colors.fgSubtle,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BodyText(title, size = titleSize, weight = titleWeight, color = titleColor, maxLines = 1)
        if (subtitle != null) BodyText(subtitle, size = 12.sp, color = subtitleColor, lineHeight = 17.sp)
    }
}
