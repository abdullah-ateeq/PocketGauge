package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import java.math.BigDecimal
import java.math.RoundingMode

object MeasurementFormatter {
    const val MIN_DECIMALS = 1
    const val MAX_DECIMALS = 4

    private val decimalPattern = Regex("""-?\d*\.?\d*""")

    /** Value in [unit] rounded half-up to [decimals] places, e.g. "7.42". */
    fun formatValue(lengthMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        formatFixed(UnitConversions.fromMillimeters(lengthMm, unit), decimals)

    /** e.g. "7.42 cm". */
    fun format(lengthMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatValue(lengthMm, unit, decimals)} ${unit.symbol}"

    /** e.g. "7.42 centimeters", for accessibility. */
    fun formatSpoken(lengthMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatValue(lengthMm, unit, decimals)} ${unit.spokenName}"

    fun formatFixed(value: BigDecimal, decimals: Int): String =
        value.setScale(decimals.coerceIn(0, MAX_DECIMALS), RoundingMode.HALF_UP).toPlainString()

    /** A reference value as typed in its own unit, e.g. "85.60 mm". */
    fun formatReference(value: BigDecimal, unit: MeasurementUnit): String =
        "${formatFixed(value, 2)} ${unit.symbol}"

    /** Compact representation for input fields, e.g. 10.00 -> "10". */
    fun formatInput(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    /**
     * Lenient, safe parsing of user input. Accepts "," as decimal separator.
     * Returns null for anything that is not a plain decimal number.
     */
    fun parseDecimal(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.length > 18) return null
        if (!decimalPattern.matches(normalized)) return null
        if (normalized.none { it.isDigit() }) return null
        return normalized.toBigDecimalOrNull()
    }

    /** e.g. "OBJECT LENGTH" or a custom label in upper case. */
    fun resultTitle(type: MeasurementType, customLabel: String?): String = when (type) {
        MeasurementType.CUSTOM -> customLabel?.trim()?.takeIf { it.isNotEmpty() }?.uppercase() ?: "OBJECT SIZE"
        else -> "OBJECT ${type.label.uppercase()}"
    }

    /** e.g. "length" for use inside sentences. */
    fun measurementNoun(type: MeasurementType, customLabel: String?): String = when (type) {
        MeasurementType.CUSTOM -> customLabel?.trim()?.takeIf { it.isNotEmpty() }?.lowercase() ?: "size"
        else -> type.label.lowercase()
    }

    fun copyText(
        lengthMm: BigDecimal,
        unit: MeasurementUnit,
        decimals: Int,
        type: MeasurementType,
        customLabel: String?,
        referenceName: String,
        referenceValue: BigDecimal,
        referenceUnit: MeasurementUnit,
    ): String = buildString {
        append("Estimated ${measurementNoun(type, customLabel)}: ${format(lengthMm, unit, decimals)}\n")
        append("Reference: $referenceName\n")
        append("Calibration: ${formatReference(referenceValue, referenceUnit)}")
    }
}
