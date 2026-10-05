package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UnitConversionsTest {

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals("expected $expected but was $actual", 0, BigDecimal(expected).compareTo(actual))

    @Test
    fun mmToCm() = assertDecimal("7.42", UnitConversions.mmToCm(BigDecimal("74.2")))

    @Test
    fun mmToInch() = assertDecimal("2", UnitConversions.mmToInches(BigDecimal("50.8")))

    @Test
    fun mmToFeet() = assertDecimal("1", UnitConversions.mmToFeet(BigDecimal("304.8")))

    @Test
    fun mmToMeters() = assertDecimal("1.5", UnitConversions.mmToMeters(BigDecimal("1500")))

    @Test
    fun cmToMm() = assertDecimal("75", UnitConversions.cmToMm(BigDecimal("7.5")))

    @Test
    fun toMillimetersForEveryUnit() {
        assertDecimal("10", UnitConversions.toMillimeters(BigDecimal.TEN, MeasurementUnit.MILLIMETER))
        assertDecimal("100", UnitConversions.toMillimeters(BigDecimal.TEN, MeasurementUnit.CENTIMETER))
        assertDecimal("1000", UnitConversions.toMillimeters(BigDecimal.ONE, MeasurementUnit.METER))
        assertDecimal("25.4", UnitConversions.toMillimeters(BigDecimal.ONE, MeasurementUnit.INCH))
        assertDecimal("304.8", UnitConversions.toMillimeters(BigDecimal.ONE, MeasurementUnit.FOOT))
    }

    @Test
    fun roundTripPreservesValue() {
        MeasurementUnit.entries.forEach { unit ->
            val mm = BigDecimal("123.456")
            val roundTrip = UnitConversions.toMillimeters(UnitConversions.fromMillimeters(mm, unit), unit)
            assertEquals(unit.name, 0, mm.compareTo(roundTrip.setScale(10, java.math.RoundingMode.HALF_UP)))
        }
    }
}
