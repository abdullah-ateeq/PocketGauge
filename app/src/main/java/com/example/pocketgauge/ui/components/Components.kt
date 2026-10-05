package com.example.pocketgauge.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pocketgauge.ui.theme.GaugeType

@Composable
fun BrandHeader(subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = "POCKETGAUGE",
            style = GaugeType.brand,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = GaugeType.section,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun HintText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

/** Inline notice. Uses an icon and text, never color alone. */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    val container = if (warning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (warning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (warning) GaugeIcons.Warning else Icons.Outlined.Info,
                contentDescription = if (warning) "Warning" else "Note",
                modifier = Modifier.size(18.dp),
            )
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

class GaugeHaptics(private val feedback: HapticFeedback, private val enabled: Boolean) {
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }
}

@Composable
fun rememberGaugeHaptics(enabled: Boolean): GaugeHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { GaugeHaptics(feedback, enabled) }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("PocketGauge measurement", text))
}

/** Small line icons drawn in code so the app ships no image assets. */
object GaugeIcons {
    private fun strokeIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = block,
                )
            }
            .build()

    val Ruler: ImageVector by lazy {
        strokeIcon("Ruler") {
            moveTo(2.5f, 8f); lineTo(21.5f, 8f); lineTo(21.5f, 16f); lineTo(2.5f, 16f); close()
            moveTo(6.5f, 8f); lineTo(6.5f, 11f)
            moveTo(10.5f, 8f); lineTo(10.5f, 12.5f)
            moveTo(14.5f, 8f); lineTo(14.5f, 11f)
            moveTo(18.5f, 8f); lineTo(18.5f, 12.5f)
        }
    }

    val Copy: ImageVector by lazy {
        strokeIcon("Copy") {
            moveTo(9f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); lineTo(9f, 20f); close()
            moveTo(15f, 9f); lineTo(15f, 4f); lineTo(4f, 4f); lineTo(4f, 15f); lineTo(9f, 15f)
        }
    }

    val Save: ImageVector by lazy {
        strokeIcon("Save") {
            moveTo(6f, 3.5f); lineTo(18f, 3.5f); lineTo(18f, 20.5f); lineTo(12f, 16.5f); lineTo(6f, 20.5f); close()
        }
    }

    val Recalibrate: ImageVector by lazy {
        strokeIcon("Recalibrate") {
            moveTo(3f, 12f); lineTo(21f, 12f)
            moveTo(3f, 8f); lineTo(3f, 16f)
            moveTo(21f, 8f); lineTo(21f, 16f)
            moveTo(9f, 12f); lineTo(9f, 9f)
            moveTo(15f, 12f); lineTo(15f, 9f)
        }
    }

    val Warning: ImageVector by lazy {
        strokeIcon("Warning") {
            moveTo(12f, 3.5f); lineTo(21.5f, 20f); lineTo(2.5f, 20f); close()
            moveTo(12f, 9.5f); lineTo(12f, 14f)
            moveTo(12f, 17f); lineTo(12f, 17.2f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check") {
            moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f)
        }
    }

    val Reset: ImageVector by lazy {
        strokeIcon("Reset") {
            moveTo(12f, 4f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4f, y1 = 12f)
            moveTo(1.5f, 13.5f); lineTo(4f, 11f); lineTo(6.5f, 13.5f)
        }
    }
}
