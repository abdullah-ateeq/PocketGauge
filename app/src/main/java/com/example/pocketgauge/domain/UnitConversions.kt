package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementUnit
import java.math.BigDecimal
import java.math.MathContext

/**
 * The single place where length units are converted. Everything is normalized to millimeters.
 */
object UnitConversions {
    val MATH_CONTEXT: MathContext = MathContext.DECIMAL128

    val MM_PER_CM: BigDecimal = BigDecimal.TEN
    val MM_PER_METER: BigDecimal = BigDecimal(1000)
    val MM_PER_INCH: BigDecimal = BigDecimal("25.4")
    val MM_PER_FOOT: BigDecimal = BigDecimal("304.8")

    fun mmToMm(mm: BigDecimal): BigDecimal = mm

    fun mmToCm(mm: BigDecimal): BigDecimal = mm.divide(MM_PER_CM, MATH_CONTEXT)

    fun mmToMeters(mm: BigDecimal): BigDecimal = mm.divide(MM_PER_METER, MATH_CONTEXT)

    fun mmToInches(mm: BigDecimal): BigDecimal = mm.divide(MM_PER_INCH, MATH_CONTEXT)

    fun mmToFeet(mm: BigDecimal): BigDecimal = mm.divide(MM_PER_FOOT, MATH_CONTEXT)

    fun cmToMm(cm: BigDecimal): BigDecimal = toMillimeters(cm, MeasurementUnit.CENTIMETER)

    fun millimetersPer(unit: MeasurementUnit): BigDecimal = when (unit) {
        MeasurementUnit.MILLIMETER -> BigDecimal.ONE
        MeasurementUnit.CENTIMETER -> MM_PER_CM
        MeasurementUnit.METER -> MM_PER_METER
        MeasurementUnit.INCH -> MM_PER_INCH
        MeasurementUnit.FOOT -> MM_PER_FOOT
    }

    fun toMillimeters(value: BigDecimal, unit: MeasurementUnit): BigDecimal =
        value.multiply(millimetersPer(unit), MATH_CONTEXT)

    fun fromMillimeters(mm: BigDecimal, unit: MeasurementUnit): BigDecimal = when (unit) {
        MeasurementUnit.MILLIMETER -> mmToMm(mm)
        MeasurementUnit.CENTIMETER -> mmToCm(mm)
        MeasurementUnit.METER -> mmToMeters(mm)
        MeasurementUnit.INCH -> mmToInches(mm)
        MeasurementUnit.FOOT -> mmToFeet(mm)
    }
}
