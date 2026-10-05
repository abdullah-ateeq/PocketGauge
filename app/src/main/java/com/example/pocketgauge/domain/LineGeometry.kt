package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.PxPoint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Usable area for handles, in canvas pixels. */
data class LineBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val diagonal: Float get() = hypot(width, height)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class LineSegment(val start: PxPoint, val end: PxPoint)

enum class Handle { START, END }

/** Pure geometry for keeping the measurement line usable and inside its area. */
object LineGeometry {

    fun clampPoint(point: PxPoint, bounds: LineBounds): PxPoint = PxPoint(
        x = point.x.coerceIn(bounds.left, max(bounds.left, bounds.right)),
        y = point.y.coerceIn(bounds.top, max(bounds.top, bounds.bottom)),
    )

    fun distance(a: PxPoint, b: PxPoint): Float = hypot(b.x - a.x, b.y - a.y)

    /** Translates the line back inside [bounds] when possible, then clamps both ends. */
    fun fitLine(line: LineSegment, bounds: LineBounds): LineSegment {
        val minX = min(line.start.x, line.end.x)
        val maxX = max(line.start.x, line.end.x)
        val minY = min(line.start.y, line.end.y)
        val maxY = max(line.start.y, line.end.y)
        val dx = when {
            maxX - minX > bounds.width -> 0f
            minX < bounds.left -> bounds.left - minX
            maxX > bounds.right -> bounds.right - maxX
            else -> 0f
        }
        val dy = when {
            maxY - minY > bounds.height -> 0f
            minY < bounds.top -> bounds.top - minY
            maxY > bounds.bottom -> bounds.bottom - maxY
            else -> 0f
        }
        return LineSegment(
            start = clampPoint(PxPoint(line.start.x + dx, line.start.y + dy), bounds),
            end = clampPoint(PxPoint(line.end.x + dx, line.end.y + dy), bounds),
        )
    }

    /** A line of [length] centered on [center] along the orientation's axis (free uses horizontal). */
    fun centeredLine(
        center: PxPoint,
        length: Float,
        orientation: MeasurementOrientation,
        bounds: LineBounds,
    ): LineSegment {
        val line = when (orientation) {
            MeasurementOrientation.VERTICAL -> {
                val half = length.coerceAtMost(bounds.height) / 2f
                LineSegment(PxPoint(center.x, center.y - half), PxPoint(center.x, center.y + half))
            }
            else -> {
                val half = length.coerceAtMost(bounds.width) / 2f
                LineSegment(PxPoint(center.x - half, center.y), PxPoint(center.x + half, center.y))
            }
        }
        return fitLine(line, bounds)
    }

    fun defaultLine(orientation: MeasurementOrientation, bounds: LineBounds): LineSegment {
        val length = if (orientation == MeasurementOrientation.VERTICAL) bounds.height * 0.5f else bounds.width * 0.5f
        return centeredLine(PxPoint(bounds.centerX, bounds.centerY), length, orientation, bounds)
    }

    /** Re-aligns an existing line to a new orientation, preserving its length and center. */
    fun alignToOrientation(
        line: LineSegment,
        orientation: MeasurementOrientation,
        bounds: LineBounds,
        minLength: Float,
    ): LineSegment {
        if (orientation == MeasurementOrientation.FREE) return fitLine(line, bounds)
        val center = PxPoint((line.start.x + line.end.x) / 2f, (line.start.y + line.end.y) / 2f)
        val length = distance(line.start, line.end).coerceAtLeast(minLength)
        return centeredLine(center, length, orientation, bounds)
    }

    /**
     * Moves one handle by a drag delta, honoring the orientation's axis lock, the bounds,
     * and the minimum visible length. Horizontal/vertical lines keep start before end.
     */
    fun moveHandle(
        line: LineSegment,
        handle: Handle,
        deltaX: Float,
        deltaY: Float,
        orientation: MeasurementOrientation,
        bounds: LineBounds,
        minLength: Float,
    ): LineSegment {
        val moving = if (handle == Handle.START) line.start else line.end
        val other = if (handle == Handle.START) line.end else line.start
        val moved = when (orientation) {
            MeasurementOrientation.HORIZONTAL -> {
                var x = (moving.x + deltaX).coerceIn(bounds.left, bounds.right)
                x = if (handle == Handle.START) min(x, other.x - minLength) else max(x, other.x + minLength)
                PxPoint(x.coerceIn(bounds.left, bounds.right), moving.y)
            }
            MeasurementOrientation.VERTICAL -> {
                var y = (moving.y + deltaY).coerceIn(bounds.top, bounds.bottom)
                y = if (handle == Handle.START) min(y, other.y - minLength) else max(y, other.y + minLength)
                PxPoint(moving.x, y.coerceIn(bounds.top, bounds.bottom))
            }
            MeasurementOrientation.FREE -> {
                val candidate = clampPoint(PxPoint(moving.x + deltaX, moving.y + deltaY), bounds)
                if (distance(candidate, other) < minLength) moving else candidate
            }
        }
        return if (handle == Handle.START) line.copy(start = moved) else line.copy(end = moved)
    }
}
