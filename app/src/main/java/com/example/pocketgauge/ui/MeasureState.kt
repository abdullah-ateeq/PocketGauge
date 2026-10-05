package com.example.pocketgauge.ui

import com.example.pocketgauge.domain.LineBounds
import com.example.pocketgauge.domain.LineSegment
import com.example.pocketgauge.domain.ReferencePresets
import com.example.pocketgauge.model.Calibration
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceDimension
import com.example.pocketgauge.model.ReferenceDraft
import com.example.pocketgauge.model.ReferenceType
import com.example.pocketgauge.model.ScreenSignature
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MeasureStep { CALIBRATE, MEASURE }

/** Pixel geometry of the calibration ruler, reported by the UI after layout. */
data class CalibrationArea(
    val startX: Float,
    val maxEndX: Float,
    val minLengthPx: Float,
    val shortLengthPx: Float,
    val edgeMarginPx: Float,
) {
    val maxLength: Float get() = max(minLengthPx, maxEndX - startX)
}

/** Pixel geometry of the measurement canvas, reported by the UI after layout. */
data class MeasureArea(
    val bounds: LineBounds,
    val minLengthPx: Float,
)

data class DisplayInfo(
    val xdpi: Float,
    val ydpi: Float,
    val densityDpi: Int,
) {
    val signature: ScreenSignature
        get() = ScreenSignature(densityDpi, min(xdpi, ydpi).roundToInt(), max(xdpi, ydpi).roundToInt())

    /**
     * Rough pixels-per-millimeter from reported density. Only used to place the initial
     * calibration line; the user's calibration always replaces it.
     */
    val estimatedPxPerMm: Float
        get() {
            val reported = (xdpi + ydpi) / 2f
            val plausible = reported.isFinite() && reported > densityDpi * 0.6f && reported < densityDpi * 1.6f
            return (if (plausible) reported else densityDpi.toFloat()) / 25.4f
        }
}

internal data class Session(
    val step: MeasureStep = MeasureStep.CALIBRATE,
    val reference: ReferenceDraft = ReferencePresets.defaultDraft(ReferenceType.CUSTOM),
    val calibrationPx: Float = 0f,
    val calibrationTouched: Boolean = false,
    val calibrationArea: CalibrationArea? = null,
    val calibration: Calibration? = null,
    val notice: String? = null,
    val measurementType: MeasurementType = MeasurementType.LENGTH,
    val customLabel: String = "",
    val orientation: MeasurementOrientation = MeasurementOrientation.HORIZONTAL,
    val line: LineSegment? = null,
    val measureArea: MeasureArea? = null,
    val resultUnit: MeasurementUnit = MeasurementUnit.CENTIMETER,
    val display: DisplayInfo? = null,
)

data class MeasureUiState(
    val step: MeasureStep,
    // Reference
    val reference: ReferenceDraft,
    val referenceName: String,
    val dimensions: List<ReferenceDimension>,
    val referenceError: String?,
    val knownSizeText: String?,
    val knownValue: Double?,
    // Calibration
    val calibrationPx: Float,
    val calibrationStartX: Float,
    val calibrationWarning: String?,
    val fitHint: String?,
    val canContinue: Boolean,
    val isCalibrated: Boolean,
    val calibrationSummary: String?,
    val calibrationReferenceName: String?,
    val notice: String?,
    // Measurement
    val measurementType: MeasurementType,
    val customLabel: String,
    val orientation: MeasurementOrientation,
    val line: LineSegment?,
    val millimetersPerPixel: Double?,
    // Result
    val resultUnit: MeasurementUnit,
    val resultTitle: String,
    val resultText: String?,
    val resultSpoken: String?,
    val secondaryResults: List<String>,
    val resultError: String?,
    val largeMeasurementWarning: String?,
    val canSave: Boolean,
)
