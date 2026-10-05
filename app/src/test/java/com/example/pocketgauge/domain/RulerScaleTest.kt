package com.example.pocketgauge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulerScaleTest {

    @Test
    fun tenCentimeterLabels() {
        val ticks = RulerScale.ticks(10.0, lengthPx = 1000f, minLabelSpacingPx = 80f, minMinorSpacingPx = 15f)
        assertEquals(listOf("0", "2", "4", "6", "8", "10"), ticks.mapNotNull { it.label })
    }

    @Test
    fun fourInchLabels() {
        val ticks = RulerScale.ticks(4.0, lengthPx = 1000f, minLabelSpacingPx = 80f, minMinorSpacingPx = 15f)
        assertEquals(listOf("0", "1", "2", "3", "4"), ticks.mapNotNull { it.label })
    }

    @Test
    fun cardWidthEndsWithKnownValue() {
        val labels = RulerScale.ticks(85.6, lengthPx = 1000f, minLabelSpacingPx = 80f, minMinorSpacingPx = 15f).mapNotNull { it.label }
        assertEquals("0", labels.first())
        assertEquals("85.6", labels.last())
    }

    @Test
    fun tickCountStaysSensibleOnShortRulers() {
        val ticks = RulerScale.ticks(300.0, lengthPx = 200f, minLabelSpacingPx = 80f, minMinorSpacingPx = 15f)
        assertTrue(ticks.size <= 20)
        assertTrue(ticks.all { it.fraction in 0f..1f })
    }

    @Test
    fun invalidInputProducesNoTicks() {
        assertTrue(RulerScale.ticks(0.0, 500f, 80f, 15f).isEmpty())
        assertTrue(RulerScale.ticks(10.0, 0f, 80f, 15f).isEmpty())
        assertTrue(RulerScale.ticks(Double.NaN, 500f, 80f, 15f).isEmpty())
    }
}
