package com.example.pocketgauge.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

enum class ReferenceType(val displayName: String, val shortName: String) {
    CREDIT_CARD("Credit/Debit Card", "Card"),
    RULER("Standard Ruler", "Ruler"),
    COIN("Custom Coin", "Coin"),
    CUSTOM("Custom Reference", "Custom"),
}

enum class ReferenceDimension(val label: String) {
    WIDTH("Width"),
    HEIGHT("Height"),
    DIAMETER("Diameter"),
    LENGTH("Length"),
}

enum class MeasurementType(val label: String) {
    LENGTH("Length"),
    WIDTH("Width"),
    HEIGHT("Height"),
    DIAMETER("Diameter"),
    CUSTOM("Custom"),
}

enum class MeasurementOrientation(val label: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical"),
    FREE("Free"),
}

enum class MeasurementUnit(val symbol: String, val spokenName: String) {
    MILLIMETER("mm", "millimeters"),
    CENTIMETER("cm", "centimeters"),
    METER("m", "meters"),
    INCH("in", "inches"),
    FOOT("ft", "feet");

    companion object {
        /** Units offered for displaying a result. */
        val resultUnits = listOf(MILLIMETER, CENTIMETER, INCH, FOOT)

        /** Units shown as secondary conversions under a result. */
        val secondaryUnits = listOf(MILLIMETER, CENTIMETER, INCH)
    }
}

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

data class ReferenceObject(
    val type: ReferenceType,
    val name: String,
    val knownLengthMm: BigDecimal,
)

/** Editable reference as the user configures it. The value stays as text so input is never rewritten. */
data class ReferenceDraft(
    val type: ReferenceType,
    val name: String,
    val dimension: ReferenceDimension,
    val valueText: String,
    val unit: MeasurementUnit,
)

/** Physical characteristics of the display, used to detect when a calibration no longer applies. */
data class ScreenSignature(
    val densityDpi: Int,
    val minDpi: Int,
    val maxDpi: Int,
)

data class Calibration(
    val referenceName: String,
    val referenceLengthMm: BigDecimal,
    val screenReferenceLengthPx: BigDecimal,
    val referenceType: ReferenceType = ReferenceType.CUSTOM,
    val referenceDimension: ReferenceDimension = ReferenceDimension.LENGTH,
    val displayValue: BigDecimal = referenceLengthMm,
    val displayUnit: MeasurementUnit = MeasurementUnit.MILLIMETER,
    val screenSignature: ScreenSignature? = null,
)

data class MeasurementResult(
    val type: MeasurementType,
    val screenLengthPx: BigDecimal,
    val estimatedLengthMm: BigDecimal,
    val displayUnit: MeasurementUnit,
)

data class PxPoint(val x: Float, val y: Float)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultUnit: MeasurementUnit = MeasurementUnit.CENTIMETER,
    val defaultMeasurementType: MeasurementType = MeasurementType.LENGTH,
    val decimalPlaces: Int = 2,
    val showSecondaryUnits: Boolean = true,
    val hapticFeedback: Boolean = true,
    val showCalibrationTips: Boolean = true,
    val showRuler: Boolean = true,
)

@Serializable
data class HistoryRecord(
    val id: String,
    val measurementType: MeasurementType,
    val customLabel: String? = null,
    val resultMm: String,
    val unit: MeasurementUnit,
    val decimalPlaces: Int = 2,
    val referenceType: ReferenceType,
    val referenceName: String,
    val referenceSizeMm: String,
    val referenceDisplayValue: String,
    val referenceDisplayUnit: MeasurementUnit,
    val referenceDimension: ReferenceDimension = ReferenceDimension.LENGTH,
    val timestampMillis: Long,
) {
    val resultMmValue: BigDecimal? get() = resultMm.toBigDecimalOrNull()
    val referenceSizeMmValue: BigDecimal? get() = referenceSizeMm.toBigDecimalOrNull()
    val referenceDisplayValueDecimal: BigDecimal? get() = referenceDisplayValue.toBigDecimalOrNull()
}
