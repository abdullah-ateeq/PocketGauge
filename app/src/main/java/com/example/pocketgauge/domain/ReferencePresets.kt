package com.example.pocketgauge.domain

import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceDimension
import com.example.pocketgauge.model.ReferenceDraft
import com.example.pocketgauge.model.ReferenceType
import java.math.BigDecimal

data class QuickReference(val label: String, val value: BigDecimal, val unit: MeasurementUnit)

object ReferencePresets {
    /** ISO/IEC 7810 ID-1 card size. */
    val CARD_WIDTH_MM = BigDecimal("85.60")
    val CARD_HEIGHT_MM = BigDecimal("53.98")

    val RULER_LENGTHS_CM: List<BigDecimal> = listOf(10, 15, 20, 30).map { BigDecimal(it) }

    val QUICK_REFERENCES = listOf(
        QuickReference("10 mm", BigDecimal(10), MeasurementUnit.MILLIMETER),
        QuickReference("1 cm", BigDecimal.ONE, MeasurementUnit.CENTIMETER),
        QuickReference("5 cm", BigDecimal(5), MeasurementUnit.CENTIMETER),
        QuickReference("10 cm", BigDecimal.TEN, MeasurementUnit.CENTIMETER),
        QuickReference("15 cm", BigDecimal(15), MeasurementUnit.CENTIMETER),
        QuickReference("1 in", BigDecimal.ONE, MeasurementUnit.INCH),
    )

    const val CUSTOM_DEFAULT_NAME = "Custom Reference"

    fun dimensionsFor(type: ReferenceType): List<ReferenceDimension> = when (type) {
        ReferenceType.CREDIT_CARD -> listOf(ReferenceDimension.WIDTH, ReferenceDimension.HEIGHT)
        ReferenceType.RULER -> listOf(ReferenceDimension.LENGTH)
        ReferenceType.COIN -> listOf(ReferenceDimension.DIAMETER)
        ReferenceType.CUSTOM -> listOf(ReferenceDimension.LENGTH)
    }

    fun defaultDraft(
        type: ReferenceType,
        dimension: ReferenceDimension = dimensionsFor(type).first(),
    ): ReferenceDraft = when (type) {
        ReferenceType.CREDIT_CARD -> ReferenceDraft(
            type = type,
            name = "",
            dimension = dimension,
            valueText = if (dimension == ReferenceDimension.HEIGHT) "53.98" else "85.60",
            unit = MeasurementUnit.MILLIMETER,
        )
        ReferenceType.RULER -> ReferenceDraft(type, "", ReferenceDimension.LENGTH, "10", MeasurementUnit.CENTIMETER)
        ReferenceType.COIN -> ReferenceDraft(type, "", ReferenceDimension.DIAMETER, "", MeasurementUnit.MILLIMETER)
        ReferenceType.CUSTOM -> ReferenceDraft(type, "", ReferenceDimension.LENGTH, "10", MeasurementUnit.CENTIMETER)
    }

    /** Human readable reference name, e.g. "Credit Card" or "10 cm ruler". */
    fun displayName(draft: ReferenceDraft): String = when (draft.type) {
        ReferenceType.CREDIT_CARD -> "Credit Card"
        ReferenceType.RULER -> {
            val value = MeasurementFormatter.parseDecimal(draft.valueText)
            if (value != null && value.signum() > 0) {
                "${MeasurementFormatter.formatInput(value)} ${draft.unit.symbol} ruler"
            } else {
                "Ruler"
            }
        }
        ReferenceType.COIN -> draft.name.trim().ifEmpty { "Coin" }
        ReferenceType.CUSTOM -> draft.name.trim().ifEmpty { CUSTOM_DEFAULT_NAME }
    }

    /** Known reference length in millimeters, or null when the input is not a number. */
    fun knownLengthMm(draft: ReferenceDraft): BigDecimal? =
        MeasurementFormatter.parseDecimal(draft.valueText)?.let { UnitConversions.toMillimeters(it, draft.unit) }
}
