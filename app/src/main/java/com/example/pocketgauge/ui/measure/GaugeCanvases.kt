package com.example.pocketgauge.ui.measure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.pocketgauge.domain.Handle
import com.example.pocketgauge.domain.LineBounds
import com.example.pocketgauge.domain.LineSegment
import com.example.pocketgauge.domain.RulerScale
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.PxPoint
import com.example.pocketgauge.ui.CalibrationArea
import com.example.pocketgauge.ui.MeasureArea
import com.example.pocketgauge.ui.theme.GaugeType
import kotlin.math.hypot
import kotlin.math.roundToInt

private val RulerStartInset = 16.dp
private val RulerEndInset = 26.dp

/**
 * Horizontal calibration ruler. The left edge (0) is fixed; dragging anywhere on the ruler
 * moves the right end so it can be matched to the physical reference.
 */
@Composable
fun CalibrationRuler(
    lengthPx: Float,
    knownValue: Double?,
    knownSizeText: String?,
    showTicks: Boolean,
    onAreaChanged: (CalibrationArea) -> Unit,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val tickStyle = GaugeType.tick.copy(color = colors.onSurfaceVariant)
    val labelStyle = GaugeType.number.copy(color = colors.onSurface)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnAreaChanged by rememberUpdatedState(onAreaChanged)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .onSizeChanged { size ->
                with(density) {
                    currentOnAreaChanged(
                        CalibrationArea(
                            startX = RulerStartInset.toPx(),
                            maxEndX = size.width - RulerEndInset.toPx(),
                            minLengthPx = 24.dp.toPx(),
                            shortLengthPx = 96.dp.toPx(),
                            edgeMarginPx = 4.dp.toPx(),
                        ),
                    )
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragStart = { currentOnDragStart() }) { change, amount ->
                    change.consume()
                    currentOnDrag(amount)
                }
            }
            .semantics {
                contentDescription = "Calibration line"
                stateDescription = knownSizeText?.let { "Represents $it. Drag to match your reference." }
                    ?: "Enter a reference size first"
                customActions = listOf(
                    CustomAccessibilityAction("Increase calibration length") { onIncrease(); true },
                    CustomAccessibilityAction("Decrease calibration length") { onDecrease(); true },
                )
            },
    ) {
        if (lengthPx <= 0f) return@Canvas
        val startX = RulerStartInset.toPx()
        val endX = startX + lengthPx
        val lineY = 72.dp.toPx()
        val bandTop = 22.dp.toPx()

        drawRoundRect(
            color = colors.primary.copy(alpha = 0.07f),
            topLeft = Offset(startX, bandTop),
            size = Size(lengthPx, lineY - bandTop),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        if (showTicks && knownValue != null) {
            val ticks = RulerScale.ticks(
                knownValue = knownValue,
                lengthPx = lengthPx,
                minLabelSpacingPx = 34.dp.toPx(),
                minMinorSpacingPx = 6.dp.toPx(),
            )
            ticks.forEach { tick ->
                val x = startX + tick.fraction * lengthPx
                val height = if (tick.isMajor) 18.dp.toPx() else 9.dp.toPx()
                drawLine(
                    color = if (tick.isMajor) colors.onSurface.copy(alpha = 0.8f) else colors.onSurfaceVariant.copy(alpha = 0.6f),
                    start = Offset(x, lineY - height),
                    end = Offset(x, lineY),
                    strokeWidth = if (tick.isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                )
                tick.label?.let { label ->
                    val layout = textMeasurer.measure(label, tickStyle)
                    val labelX = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                    drawText(layout, topLeft = Offset(labelX, lineY - height - 3.dp.toPx() - layout.size.height))
                }
            }
        }

        drawLine(colors.primary, Offset(startX, lineY), Offset(endX, lineY), 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(startX, 8.dp.toPx()), Offset(startX, lineY + 26.dp.toPx()), 2.dp.toPx())
        drawLine(colors.primary, Offset(endX, 8.dp.toPx()), Offset(endX, lineY + 40.dp.toPx()), 2.dp.toPx())

        val handleCenter = Offset(endX, lineY + 52.dp.toPx())
        drawCircle(colors.primary.copy(alpha = 0.16f), radius = 22.dp.toPx(), center = handleCenter)
        drawCircle(colors.primary, radius = 13.dp.toPx(), center = handleCenter)
        val grip = 4.dp.toPx()
        listOf(-grip, 0f, grip).forEach { dx ->
            drawLine(
                colors.onPrimary,
                Offset(handleCenter.x + dx, handleCenter.y - 5.dp.toPx()),
                Offset(handleCenter.x + dx, handleCenter.y + 5.dp.toPx()),
                1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        knownSizeText?.let { text ->
            val layout = textMeasurer.measure(text, labelStyle)
            val fitsLeftOfHandle = startX + 8.dp.toPx() + layout.size.width < handleCenter.x - 26.dp.toPx()
            val textX = if (fitsLeftOfHandle) startX + 8.dp.toPx() else (handleCenter.x + 28.dp.toPx())
                .coerceAtMost(size.width - layout.size.width)
            drawText(layout, topLeft = Offset(textX, lineY + 10.dp.toPx()))
        }
    }
}

/**
 * Measurement area with a draggable two-handle line. Handles are separate touch targets so each
 * has its own accessibility node; the Canvas only draws.
 */
@Composable
fun MeasurementCanvas(
    line: LineSegment?,
    orientation: MeasurementOrientation,
    millimetersPerPixel: Double?,
    imperialScale: Boolean,
    showScale: Boolean,
    label: String?,
    spokenResult: String?,
    onAreaChanged: (MeasureArea) -> Unit,
    onMoveHandle: (Handle, Float, Float) -> Unit,
    onHandleGrab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = GaugeType.canvasLabel.copy(color = colors.onSurface)
    val currentOnAreaChanged by rememberUpdatedState(onAreaChanged)

    Box(
        modifier = modifier
            .background(colors.surfaceContainerLowest)
            .onSizeChanged { size ->
                with(density) {
                    val inset = 28.dp.toPx()
                    currentOnAreaChanged(
                        MeasureArea(
                            bounds = LineBounds(inset, inset, size.width - inset, size.height - inset),
                            minLengthPx = 24.dp.toPx(),
                        ),
                    )
                }
            },
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    contentDescription = "Measurement line"
                    stateDescription = spokenResult ?: "Not measured"
                },
        ) {
            val mmPerPx = millimetersPerPixel?.takeIf { it > 0 && it.isFinite() }
            val majorMm = if (imperialScale) 25.4 else 10.0
            val minorMm = if (imperialScale) 25.4 / 4 else 5.0

            if (showScale && mmPerPx != null) drawGrid((majorMm / mmPerPx).toFloat(), colors.outlineVariant.copy(alpha = 0.45f))

            val segment = line ?: return@Canvas
            val start = Offset(segment.start.x, segment.start.y)
            val end = Offset(segment.end.x, segment.end.y)
            val length = hypot(end.x - start.x, end.y - start.y)
            if (length < 1f) return@Canvas
            val ux = (end.x - start.x) / length
            val uy = (end.y - start.y) / length
            // Normal pointing to the "lower/left" side; ticks go there, the label goes opposite.
            var nx = -uy
            var ny = ux
            if (ny < 0f || (ny == 0f && nx > 0f)) {
                nx = -nx; ny = -ny
            }

            if (showScale && mmPerPx != null) {
                val minorPx = (minorMm / mmPerPx).toFloat()
                val majorPx = (majorMm / mmPerPx).toFloat()
                val ratio = (majorMm / minorMm).roundToInt()
                val drawMinor = minorPx >= 6.dp.toPx()
                val stepPx = if (drawMinor) minorPx else majorPx
                if (stepPx >= 6.dp.toPx()) {
                    var i = 1
                    var distance = stepPx
                    while (distance < length - 1f) {
                        val major = !drawMinor || i % ratio == 0
                        val tickLength = if (major) 11.dp.toPx() else 6.dp.toPx()
                        val p = Offset(start.x + ux * distance, start.y + uy * distance)
                        drawLine(
                            colors.primary.copy(alpha = if (major) 0.85f else 0.5f),
                            p,
                            Offset(p.x + nx * tickLength, p.y + ny * tickLength),
                            if (major) 1.5.dp.toPx() else 1.dp.toPx(),
                        )
                        i++
                        distance = stepPx * i
                    }
                }
            }

            drawLine(colors.primary, start, end, 3.dp.toPx(), cap = StrokeCap.Round)

            val marker = 15.dp.toPx()
            listOf(start, end).forEach { p ->
                drawLine(
                    colors.onSurface,
                    Offset(p.x - nx * marker, p.y - ny * marker),
                    Offset(p.x + nx * marker, p.y + ny * marker),
                    2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            listOf(start, end).forEach { p ->
                drawCircle(colors.primary.copy(alpha = 0.14f), radius = 24.dp.toPx(), center = p)
                drawCircle(colors.surface, radius = 12.dp.toPx(), center = p)
                drawCircle(colors.primary, radius = 12.dp.toPx(), center = p, style = Stroke(3.dp.toPx()))
                drawCircle(colors.primary, radius = 2.5.dp.toPx(), center = p)
            }

            label?.let { text ->
                val layout = textMeasurer.measure(text, labelStyle)
                val padH = 10.dp.toPx()
                val padV = 5.dp.toPx()
                val w = layout.size.width + padH * 2
                val h = layout.size.height + padV * 2
                val offset = 34.dp.toPx()
                val cx = (start.x + end.x) / 2f - nx * offset
                val cy = (start.y + end.y) / 2f - ny * offset
                val left = (cx - w / 2f).coerceIn(4.dp.toPx(), (size.width - w - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                val top = (cy - h / 2f).coerceIn(4.dp.toPx(), (size.height - h - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                drawRoundRect(colors.surfaceContainerHigh, Offset(left, top), Size(w, h), CornerRadius(h / 2f))
                drawRoundRect(colors.outlineVariant, Offset(left, top), Size(w, h), CornerRadius(h / 2f), style = Stroke(1.dp.toPx()))
                drawText(layout, topLeft = Offset(left + padH, top + padV))
            }
        }

        if (line != null) {
            val names = when (orientation) {
                MeasurementOrientation.HORIZONTAL -> "left" to "right"
                MeasurementOrientation.VERTICAL -> "top" to "bottom"
                MeasurementOrientation.FREE -> "first" to "second"
            }
            MeasurementHandle(line.start, Handle.START, "Measurement start handle, ${names.first} end", orientation, onMoveHandle, onHandleGrab)
            MeasurementHandle(line.end, Handle.END, "Measurement end handle, ${names.second} end", orientation, onMoveHandle, onHandleGrab)
        }
    }
}

private fun DrawScope.drawGrid(spacingPx: Float, color: androidx.compose.ui.graphics.Color) {
    if (!spacingPx.isFinite() || spacingPx < 14.dp.toPx()) return
    val stroke = 1.dp.toPx()
    var x = spacingPx
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
        x += spacingPx
    }
    var y = spacingPx
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        y += spacingPx
    }
}

@Composable
private fun MeasurementHandle(
    position: PxPoint,
    handle: Handle,
    description: String,
    orientation: MeasurementOrientation,
    onMove: (Handle, Float, Float) -> Unit,
    onGrab: () -> Unit,
) {
    val density = LocalDensity.current
    val touchSize = 56.dp
    val half = with(density) { touchSize.toPx() / 2f }
    val nudge = with(density) { 2.dp.toPx() }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnGrab by rememberUpdatedState(onGrab)

    val actions = buildList {
        if (orientation != MeasurementOrientation.VERTICAL) {
            add(CustomAccessibilityAction("Move left") { onMove(handle, -nudge, 0f); true })
            add(CustomAccessibilityAction("Move right") { onMove(handle, nudge, 0f); true })
        }
        if (orientation != MeasurementOrientation.HORIZONTAL) {
            add(CustomAccessibilityAction("Move up") { onMove(handle, 0f, -nudge); true })
            add(CustomAccessibilityAction("Move down") { onMove(handle, 0f, nudge); true })
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - half).roundToInt(), (position.y - half).roundToInt()) }
            .size(touchSize)
            .semantics {
                contentDescription = description
                customActions = actions
            }
            .pointerInput(handle) {
                detectDragGestures(onDragStart = { currentOnGrab() }) { change, amount ->
                    change.consume()
                    currentOnMove(handle, amount.x, amount.y)
                }
            },
    )
}
