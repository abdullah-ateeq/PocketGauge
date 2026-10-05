package com.example.pocketgauge.domain

import com.example.pocketgauge.model.Calibration
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.PxPoint
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Proportional measurement engine.
 *
 * objectLength = objectScreenLength × (referenceLength ÷ referenceScreenLength)
 *
 * Invalid input throws [IllegalArgumentException] with a user-facing message.
 */
object PocketGaugeCalculator {
    const val ERROR_REFERENCE_SIZE = "Reference size must be greater than zero."
    const val ERROR_CALIBRATION_LENGTH = "Calibration length must be greater than zero."
    const val ERROR_MEASUREMENT_LENGTH = "Measurement length must be greater than zero."

    private val mc = UnitConversions.MATH_CONTEXT

    fun calculateMillimetersPerPixel(
        knownReferenceMm: BigDecimal,
        referencePixels: BigDecimal,
    ): BigDecimal {
        validateCalibration(knownReferenceMm, referencePixels)
        return knownReferenceMm.divide(referencePixels, mc)
    }

    fun calculatePixelsPerMillimeter(
        knownReferenceMm: BigDecimal,
        referencePixels: BigDecimal,
    ): BigDecimal {
        validateCalibration(knownReferenceMm, referencePixels)
        return referencePixels.divide(knownReferenceMm, mc)
    }

    fun calculateObjectLength(
        objectPixels: BigDecimal,
        millimetersPerPixel: BigDecimal,
    ): BigDecimal {
        require(millimetersPerPixel.signum() > 0) { ERROR_CALIBRATION_LENGTH }
        require(objectPixels.signum() > 0) { ERROR_MEASUREMENT_LENGTH }
        return objectPixels.multiply(millimetersPerPixel, mc)
    }

    /** Multiplies before dividing so the result carries no intermediate rounding. */
    fun calculateObjectLength(
        objectPixels: BigDecimal,
        calibration: Calibration,
    ): BigDecimal {
        validateCalibration(calibration.referenceLengthMm, calibration.screenReferenceLengthPx)
        require(objectPixels.signum() > 0) { ERROR_MEASUREMENT_LENGTH }
        return objectPixels
            .multiply(calibration.referenceLengthMm, mc)
            .divide(calibration.screenReferenceLengthPx, mc)
    }

    fun convertMeasurement(lengthMm: BigDecimal, unit: MeasurementUnit): BigDecimal =
        UnitConversions.fromMillimeters(lengthMm, unit)

    fun convertMeasurement(value: BigDecimal, from: MeasurementUnit, to: MeasurementUnit): BigDecimal =
        UnitConversions.fromMillimeters(UnitConversions.toMillimeters(value, from), to)

    /**
     * Screen distance between two endpoints. Horizontal and vertical modes use the single axis;
     * free mode uses the Euclidean distance computed in double precision.
     */
    fun lineLengthPixels(
        start: PxPoint,
        end: PxPoint,
        orientation: MeasurementOrientation,
    ): BigDecimal {
        val dx = end.x.toDouble() - start.x.toDouble()
        val dy = end.y.toDouble() - start.y.toDouble()
        val distance = when (orientation) {
            MeasurementOrientation.HORIZONTAL -> abs(dx)
            MeasurementOrientation.VERTICAL -> abs(dy)
            MeasurementOrientation.FREE -> hypot(dx, dy)
        }
        return if (distance.isFinite()) BigDecimal.valueOf(distance) else BigDecimal.ZERO
    }

    private fun validateCalibration(knownReferenceMm: BigDecimal, referencePixels: BigDecimal) {
        require(knownReferenceMm.signum() > 0) { ERROR_REFERENCE_SIZE }
        require(referencePixels.signum() > 0) { ERROR_CALIBRATION_LENGTH }
    }
}
