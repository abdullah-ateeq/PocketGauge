package com.example.pocketgauge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val Typography = base.copy(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
)

/** App-specific text styles on top of Material typography. */
object GaugeType {
    private const val TABULAR = "tnum"

    val brand = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
    val brandLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
    val section = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
    val overline = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
    val result = TextStyle(fontSize = 52.sp, lineHeight = 58.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR, letterSpacing = (-0.5).sp)
    val resultCompact = TextStyle(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val value = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR)
    val number = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val tick = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val canvasLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR)
}
