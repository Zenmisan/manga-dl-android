package com.mangadl.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mangadl.android.R

val Anton = FontFamily(Font(R.font.anton_regular))

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_black, FontWeight.Black),
)

val PtSerif = FontFamily(
    Font(R.font.ptserif_regular, FontWeight.Normal),
    Font(R.font.ptserif_bold, FontWeight.Bold),
)

object MdType {
    fun display(size: TextUnit, lineHeight: TextUnit = size) =
        TextStyle(fontFamily = Anton, fontSize = size, lineHeight = lineHeight)

    fun body(
        size: TextUnit,
        weight: FontWeight = FontWeight.Normal,
        lineHeight: TextUnit = TextUnit.Unspecified,
    ) = TextStyle(fontFamily = Inter, fontSize = size, fontWeight = weight, lineHeight = lineHeight)

    val eyebrow = TextStyle(
        fontFamily = Inter,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.2.em,
    )
}

val AntonStyle = TextStyle(fontFamily = Anton, fontSize = 28.sp)
val AntonStyleSub = TextStyle(fontFamily = Anton, fontSize = 24.sp)
val SectionLabelStyle = TextStyle(fontFamily = Inter, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.em)

val Typography = Typography(
    bodyLarge = MdType.body(16.sp),
    bodyMedium = MdType.body(14.sp),
    bodySmall = MdType.body(12.sp),
    labelLarge = MdType.body(14.sp),
    titleLarge = MdType.body(22.sp, FontWeight.Bold),
    titleMedium = MdType.body(16.sp, FontWeight.SemiBold),
    titleSmall = MdType.body(14.sp, FontWeight.SemiBold),
)
