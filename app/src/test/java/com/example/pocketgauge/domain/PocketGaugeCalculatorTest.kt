package com.example.pocketgauge.domain

import com.example.pocketgauge.model.Calibration
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.PxPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PocketGaugeCalculatorTest {

    private fun assertDecimal(expected: String, actual: BigDecimal, scale: Int = 6) = assertEquals(
        "expected $expected but was $actual",
        0,
        BigDecimal(expected).setScale(scale, RoundingMode.HALF_UP).compareTo(actual.setScale(scale, RoundingMode.HALF_UP)),
    )

    private fun estimate(referenceValue: String, unit: MeasurementUnit, referencePx: String, objectPx: String): BigDecimal {
        val referenceMm = UnitConversions.toMillimeters(BigDecimal(referenceValue), unit)
        val calibration = Calibration("Test", referenceMm, BigDecimal(referencePx))
        return PocketGaugeCalculator.calculateObjectLength(BigDecimal(objectPx), calibration)
    }

    @Test
    fun calibrationCalculation() {
        val mmPerPx = PocketGaugeCalculator.calculateMillimetersPerPixel(BigDecimal("100"), BigDecimal("200"))
        assertDecimal("0.5", mmPerPx)
        val pxPerMm = PocketGaugeCalculator.calculatePixelsPerMillimeter(BigDecimal("100"), BigDecimal("200"))
        assertDecimal("2", pxPerMm)
    }

    @Test
    fun objectMeasurementTenCentimeterReference() {
        // 10 cm reference shown as 200 px; object 150 px -> 7.5 cm
        val mm = estimate("10", MeasurementUnit.CENTIMETER, "200", "150")
        assertDecimal("7.5", PocketGaugeCalculator.convertMeasurement(mm, MeasurementUnit.CENTIMETER))
    }

    @Test
    fun objectMeasurementHundredMillimeterReference() {
        // 100 mm reference shown as 250 px; object 125 px -> 50 mm
        assertDecimal("50", estimate("100", MeasurementUnit.MILLIMETER, "250", "125"))
    }

    @Test
    fun objectMeasurementViaMillimetersPerPixel() {
        val mmPerPx = PocketGaugeCalculator.calculateMillimetersPerPixel(BigDecimal("100"), BigDecimal("250"))
        assertDecimal("50", PocketGaugeCalculator.calculateObjectLength(BigDecimal("125"), mmPerPx))
    }

    @Test
    fun creditCardAccuracy() {
        // 85.60 mm card shown as 428 px; object 214 px -> 42.80 mm
        val mm = estimate("85.60", MeasurementUnit.MILLIMETER, "428", "214")
        assertEquals("42.80", MeasurementFormatter.formatValue(mm, MeasurementUnit.MILLIMETER, 2))
    }

    @Test
    fun zeroReferenceIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PocketGaugeCalculator.calculateMillimetersPerPixel(BigDecimal.ZERO, BigDecimal("200"))
        }
        assertEquals(PocketGaugeCalculator.ERROR_REFERENCE_SIZE, error.message)
    }

    @Test
    fun negativeReferenceIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PocketGaugeCalculator.calculateObjectLength(BigDecimal("10"), Calibration("x", BigDecimal("-5"), BigDecimal("200")))
        }
        assertEquals(PocketGaugeCalculator.ERROR_REFERENCE_SIZE, error.message)
    }

    @Test
    fun zeroCalibrationLengthIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PocketGaugeCalculator.calculateMillimetersPerPixel(BigDecimal("100"), BigDecimal.ZERO)
        }
        assertEquals(PocketGaugeCalculator.ERROR_CALIBRATION_LENGTH, error.message)
    }

    @Test
    fun negativeCalibrationLengthIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PocketGaugeCalculator.calculatePixelsPerMillimeter(BigDecimal("100"), BigDecimal("-1"))
        }
        assertEquals(PocketGaugeCalculator.ERROR_CALIBRATION_LENGTH, error.message)
    }

    @Test
    fun zeroMeasurementLengthIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PocketGaugeCalculator.calculateObjectLength(BigDecimal.ZERO, Calibration("x", BigDecimal("100"), BigDecimal("200")))
        }
        assertEquals(PocketGaugeCalculator.ERROR_MEASUREMENT_LENGTH, error.message)
    }

    @Test
    fun horizontalMeasurementIgnoresVerticalOffset() {
        val px = PocketGaugeCalculator.lineLengthPixels(PxPoint(100f, 300f), PxPoint(250f, 340f), MeasurementOrientation.HORIZONTAL)
        assertDecimal("150", px)
    }

    @Test
    fun verticalMeasurementIgnoresHorizontalOffset() {
        val px = PocketGaugeCalculator.lineLengthPixels(PxPoint(80f, 400f), PxPoint(60f, 100f), MeasurementOrientation.VERTICAL)
        assertDecimal("300", px)
    }

    @Test
    fun freeLineDistanceUsesPythagoras() {
        val px = PocketGaugeCalculator.lineLengthPixels(PxPoint(0f, 0f), PxPoint(300f, 400f), MeasurementOrientation.FREE)
        assertDecimal("500", px)
        val diagonal = PocketGaugeCalculator.lineLengthPixels(PxPoint(10f, 10f), PxPoint(110f, 110f), MeasurementOrientation.FREE)
        assertDecimal("141.421356", diagonal)
    }

    @Test
    fun decimalMeasurements() {
        val mm = estimate("53.98", MeasurementUnit.MILLIMETER, "337.375", "123.5")
        // 123.5 × 53.98 / 337.375 = 19.76
        assertDecimal("19.76", mm)
    }

    @Test
    fun verySmallMeasurement() {
        val mm = estimate("10", MeasurementUnit.MILLIMETER, "1000", "0.5")
        assertDecimal("0.005", mm)
        assertTrue(mm.signum() > 0)
    }

    @Test
    fun largeMeasurement() {
        val mm = estimate("30", MeasurementUnit.CENTIMETER, "10", "100000")
        assertDecimal("3000000", mm)
        assertEquals("3000.00", MeasurementFormatter.formatValue(mm, MeasurementUnit.METER, 2))
    }

    @Test
    fun nonTerminatingDivisionDoesNotLosePrecision() {
        val mm = estimate("100", MeasurementUnit.MILLIMETER, "3", "3")
        assertEquals("100.00", MeasurementFormatter.formatValue(mm, MeasurementUnit.MILLIMETER, 2))
    }

    @Test
    fun convertBetweenUnits() {
        assertDecimal("2.54", PocketGaugeCalculator.convertMeasurement(BigDecimal.ONE, MeasurementUnit.INCH, MeasurementUnit.CENTIMETER))
    }
}
