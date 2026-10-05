package com.example.pocketgauge.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.pocketgauge.ui.theme.GaugeType
import kotlinx.coroutines.delay

private const val ANIMATION_MS = 1150
private const val HOLD_MS = 200L

/** Phase progress within [start, end] of the overall animation. */
private fun phase(progress: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(ANIMATION_MS, easing = LinearEasing))
        delay(HOLD_MS)
        currentOnFinished()
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "7.42 cm",
                style = GaugeType.value,
                color = colors.primary,
                modifier = Modifier.graphicsLayer { alpha = phase(progress.value, 0.55f, 0.8f) },
            )
            Canvas(Modifier.width(220.dp).height(40.dp)) {
                val p = progress.value
                val appear = phase(p, 0f, 0.15f)
                val marks = phase(p, 0.12f, 0.4f)
                val expand = phase(p, 0.35f, 0.7f)
                val y = size.height * 0.6f
                val cx = size.width / 2f
                val half = (size.width * 0.12f) + (size.width * 0.38f) * expand
                val lineColor = colors.primary.copy(alpha = appear)

                // Calibration marks
                val tickCount = 10
                for (i in 0..tickCount) {
                    val x = cx - size.width * 0.5f + size.width * i / tickCount
                    val major = i % 5 == 0
                    val h = (if (major) 14.dp.toPx() else 7.dp.toPx()) * marks
                    drawLine(colors.outline.copy(alpha = marks * 0.8f), Offset(x, y - h), Offset(x, y), 1.dp.toPx())
                }
                drawLine(lineColor, Offset(cx - half, y), Offset(cx + half, y), 3.dp.toPx(), cap = StrokeCap.Round)
                listOf(cx - half, cx + half).forEach { x ->
                    drawCircle(lineColor, radius = 5.dp.toPx() * appear, center = Offset(x, y))
                }
            }
            Text(
                text = "POCKETGAUGE",
                style = GaugeType.brandLarge,
                color = colors.onBackground,
                modifier = Modifier.graphicsLayer {
                    val t = phase(progress.value, 0.72f, 1f)
                    alpha = t
                    translationY = (1f - t) * 12.dp.toPx()
                },
            )
            Text(
                text = "Estimate size with a calibrated screen",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = phase(progress.value, 0.8f, 1f) },
            )
        }
    }
}
