package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.PxPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineGeometryTest {
    private val bounds = LineBounds(0f, 0f, 1000f, 800f)
    private val line = LineSegment(PxPoint(200f, 400f), PxPoint(600f, 400f))

    @Test
    fun horizontalDragOnlyMovesX() {
        val moved = LineGeometry.moveHandle(line, Handle.END, 50f, 90f, MeasurementOrientation.HORIZONTAL, bounds, 20f)
        assertEquals(PxPoint(650f, 400f), moved.end)
    }

    @Test
    fun verticalDragOnlyMovesY() {
        val vertical = LineSegment(PxPoint(500f, 100f), PxPoint(500f, 500f))
        val moved = LineGeometry.moveHandle(vertical, Handle.START, 80f, -40f, MeasurementOrientation.VERTICAL, bounds, 20f)
        assertEquals(PxPoint(500f, 60f), moved.start)
    }

    @Test
    fun handlesStayInsideBounds() {
        val moved = LineGeometry.moveHandle(line, Handle.END, 5000f, 0f, MeasurementOrientation.HORIZONTAL, bounds, 20f)
        assertEquals(1000f, moved.end.x)
        val free = LineGeometry.moveHandle(line, Handle.START, -900f, -900f, MeasurementOrientation.FREE, bounds, 20f)
        assertEquals(PxPoint(0f, 0f), free.start)
    }

    @Test
    fun minimumLengthIsEnforced() {
        val moved = LineGeometry.moveHandle(line, Handle.END, -1000f, 0f, MeasurementOrientation.HORIZONTAL, bounds, 24f)
        assertEquals(224f, moved.end.x)
        val free = LineGeometry.moveHandle(line, Handle.END, -395f, 0f, MeasurementOrientation.FREE, bounds, 24f)
        assertEquals(line.end, free.end)
    }

    @Test
    fun fitLineKeepsLengthWhenAreaShrinks() {
        val fitted = LineGeometry.fitLine(line, LineBounds(0f, 0f, 500f, 300f))
        assertEquals(400f, fitted.end.x - fitted.start.x)
        assertTrue(fitted.end.x <= 500f && fitted.start.y <= 300f)
    }

    @Test
    fun orientationChangePreservesLength() {
        val vertical = LineGeometry.alignToOrientation(line, MeasurementOrientation.VERTICAL, bounds, 24f)
        assertEquals(vertical.start.x, vertical.end.x)
        assertEquals(400f, vertical.end.y - vertical.start.y)
    }
}
