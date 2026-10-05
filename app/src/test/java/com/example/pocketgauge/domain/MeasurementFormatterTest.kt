package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MeasurementFormatterTest {

    @Test
    fun resultFormattingInEachUnit() {
        val mm = BigDecimal("74.2")
        assertEquals("74.20 mm", MeasurementFormatter.format(mm, MeasurementUnit.MILLIMETER, 2))
        assertEquals("7.42 cm", MeasurementFormatter.format(mm, MeasurementUnit.CENTIMETER, 2))
        assertEquals("2.92 in", MeasurementFormatter.format(mm, MeasurementUnit.INCH, 2))
        assertEquals("0.24 ft", MeasurementFormatter.format(mm, MeasurementUnit.FOOT, 2))
    }

    @Test
    fun decimalPlacesAreRespected() {
        val mm = BigDecimal("74.2")
        assertEquals("7.4 cm", MeasurementFormatter.format(mm, MeasurementUnit.CENTIMETER, 1))
        assertEquals("7.420 cm", MeasurementFormatter.format(mm, MeasurementUnit.CENTIMETER, 3))
        assertEquals("2.9213 in", MeasurementFormatter.format(mm, MeasurementUnit.INCH, 4))
    }

    @Test
    fun noFloatingPointNoise() {
        val mm = BigDecimal("74.200000001")
        assertEquals("7.42 cm", MeasurementFormatter.format(mm, MeasurementUnit.CENTIMETER, 2))
    }

    @Test
    fun roundsHalfUp() {
        assertEquals("1.25", MeasurementFormatter.formatFixed(BigDecimal("1.245"), 2))
    }

    @Test
    fun spokenFormat() {
        assertEquals("7.42 centimeters", MeasurementFormatter.formatSpoken(BigDecimal("74.2"), MeasurementUnit.CENTIMETER, 2))
    }

    @Test
    fun safeParsing() {
        assertEquals(0, BigDecimal("85.6").compareTo(MeasurementFormatter.parseDecimal(" 85.6 ")))
        assertEquals(0, BigDecimal("53.98").compareTo(MeasurementFormatter.parseDecimal("53,98")))
        assertEquals(0, BigDecimal("-5").compareTo(MeasurementFormatter.parseDecimal("-5")))
        assertNull(MeasurementFormatter.parseDecimal(""))
        assertNull(MeasurementFormatter.parseDecimal("."))
        assertNull(MeasurementFormatter.parseDecimal("abc"))
        assertNull(MeasurementFormatter.parseDecimal("1e5"))
        assertNull(MeasurementFormatter.parseDecimal("NaN"))
        assertNull(MeasurementFormatter.parseDecimal("Infinity"))
        assertNull(MeasurementFormatter.parseDecimal("1.2.3"))
    }

    @Test
    fun resultTitles() {
        assertEquals("OBJECT LENGTH", MeasurementFormatter.resultTitle(MeasurementType.LENGTH, null))
        assertEquals("OBJECT WIDTH", MeasurementFormatter.resultTitle(MeasurementType.WIDTH, "ignored"))
        assertEquals("SCREW", MeasurementFormatter.resultTitle(MeasurementType.CUSTOM, "Screw"))
        assertEquals("OBJECT SIZE", MeasurementFormatter.resultTitle(MeasurementType.CUSTOM, "  "))
    }

    @Test
    fun copyTextMatchesSpecification() {
        val text = MeasurementFormatter.copyText(
            lengthMm = BigDecimal("74.2"),
            unit = MeasurementUnit.CENTIMETER,
            decimals = 2,
            type = MeasurementType.LENGTH,
            customLabel = null,
            referenceName = "Credit Card",
            referenceValue = BigDecimal("85.60"),
            referenceUnit = MeasurementUnit.MILLIMETER,
        )
        assertEquals("Estimated length: 7.42 cm\nReference: Credit Card\nCalibration: 85.60 mm", text)
    }
}
