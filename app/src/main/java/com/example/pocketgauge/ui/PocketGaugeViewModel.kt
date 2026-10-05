package com.example.pocketgauge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketgauge.data.HistoryRepository
import com.example.pocketgauge.data.SettingsRepository
import com.example.pocketgauge.data.pocketGaugeDataStore
import com.example.pocketgauge.domain.Handle
import com.example.pocketgauge.domain.LineGeometry
import com.example.pocketgauge.domain.MeasurementFormatter
import com.example.pocketgauge.domain.PocketGaugeCalculator
import com.example.pocketgauge.domain.QuickReference
import com.example.pocketgauge.domain.ReferencePresets
import com.example.pocketgauge.model.AppSettings
import com.example.pocketgauge.model.Calibration
import com.example.pocketgauge.model.HistoryRecord
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceDimension
import com.example.pocketgauge.model.ReferenceDraft
import com.example.pocketgauge.model.ReferenceType
import com.example.pocketgauge.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

class PocketGaugeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application.pocketGaugeDataStore)
    private val historyRepository = HistoryRepository(application.pocketGaugeDataStore)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val history: StateFlow<List<HistoryRecord>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val session = MutableStateFlow(Session())

    val measureState: StateFlow<MeasureUiState> = combine(session, settings, ::buildState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildState(Session(), AppSettings()))

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settings.first()
            session.update {
                it.copy(measurementType = initial.defaultMeasurementType, resultUnit = initial.defaultUnit)
            }
        }
    }

    // region Reference

    fun selectReferenceType(type: ReferenceType) = updateReference(reguess = true) {
        if (it.type == type) it else ReferencePresets.defaultDraft(type)
    }

    fun selectReferenceDimension(dimension: ReferenceDimension) = updateReference(reguess = true) {
        if (it.type == ReferenceType.CREDIT_CARD) ReferencePresets.defaultDraft(it.type, dimension) else it.copy(dimension = dimension)
    }

    fun selectRulerLength(lengthCm: BigDecimal) = updateReference(reguess = true) {
        it.copy(valueText = MeasurementFormatter.formatInput(lengthCm), unit = MeasurementUnit.CENTIMETER)
    }

    fun applyQuickReference(quick: QuickReference) = updateReference(reguess = true) {
        it.copy(valueText = MeasurementFormatter.formatInput(quick.value), unit = quick.unit)
    }

    fun setReferenceValue(text: String) = updateReference(reguess = false) {
        it.copy(valueText = text.filter { c -> c.isDigit() || c == '.' || c == ',' || c == '-' }.take(12))
    }

    fun setReferenceUnit(unit: MeasurementUnit) = updateReference(reguess = false) { it.copy(unit = unit) }

    fun setReferenceName(name: String) = updateReference(reguess = false) { it.copy(name = name.take(40)) }

    private fun updateReference(reguess: Boolean, transform: (ReferenceDraft) -> ReferenceDraft) {
        session.update { current ->
            val draft = transform(current.reference)
            if (draft == current.reference) return@update current
            var next = invalidateIfReferenceChanged(current.copy(reference = draft))
            if (reguess || !next.calibrationTouched) next = next.withGuessedCalibration()
            next
        }
    }

    /** A confirmed calibration is never silently reused for a different reference. */
    private fun invalidateIfReferenceChanged(s: Session): Session {
        val calibration = s.calibration ?: return s
        val parsed = MeasurementFormatter.parseDecimal(s.reference.valueText)
        val knownMm = ReferencePresets.knownLengthMm(s.reference)
        val unchanged = parsed != null && knownMm != null &&
            knownMm.compareTo(calibration.referenceLengthMm) == 0 &&
            s.reference.type == calibration.referenceType &&
            s.reference.dimension == calibration.referenceDimension
        return if (unchanged) {
            s.copy(
                calibration = calibration.copy(
                    referenceName = ReferencePresets.displayName(s.reference),
                    displayValue = parsed,
                    displayUnit = s.reference.unit,
                ),
            )
        } else {
            s.copy(calibration = null, step = MeasureStep.CALIBRATE, notice = NOTICE_REFERENCE_CHANGED)
        }
    }

    // endregion

    // region Calibration

    fun setDisplayInfo(info: DisplayInfo) = session.update { s ->
        if (s.display == info) return@update s
        var next = s.copy(display = info)
        val signature = next.calibration?.screenSignature
        if (signature != null && signature != info.signature) {
            next = next.copy(calibration = null, step = MeasureStep.CALIBRATE, notice = NOTICE_DISPLAY_CHANGED)
            next = next.withGuessedCalibration()
        } else if (next.calibrationPx <= 0f) {
            next = next.withGuessedCalibration()
        }
        next
    }

    fun onCalibrationAreaChanged(area: CalibrationArea) = session.update { s ->
        if (s.calibrationArea == area) return@update s
        val next = s.copy(calibrationArea = area)
        if (next.calibrationPx <= 0f) {
            next.withGuessedCalibration()
        } else {
            next.copy(calibrationPx = next.calibrationPx.coerceIn(area.minLengthPx, area.maxLength))
        }
    }

    /** Fine/coarse buttons and drag both route here. */
    fun adjustCalibration(deltaPx: Float) = session.update { s ->
        val area = s.calibrationArea ?: return@update s
        val px = (s.calibrationPx + deltaPx).coerceIn(area.minLengthPx, area.maxLength)
        if (px == s.calibrationPx) return@update s
        s.copy(
            calibrationPx = px,
            calibrationTouched = true,
            calibration = s.calibration?.copy(screenReferenceLengthPx = px.toBigDecimal()),
        )
    }

    fun resetCalibrationLine() = session.update { s ->
        s.copy(calibration = null, notice = null).withGuessedCalibration()
    }

    fun confirmCalibration(): Boolean {
        var confirmed = false
        session.update { s ->
            val parsed = MeasurementFormatter.parseDecimal(s.reference.valueText)
            val knownMm = ReferencePresets.knownLengthMm(s.reference)
            if (parsed == null || knownMm == null || knownMm.signum() <= 0 || s.calibrationPx <= 0f) {
                return@update s
            }
            confirmed = true
            s.copy(
                step = MeasureStep.MEASURE,
                notice = null,
                calibration = Calibration(
                    referenceName = ReferencePresets.displayName(s.reference),
                    referenceLengthMm = knownMm,
                    screenReferenceLengthPx = s.calibrationPx.toBigDecimal(),
                    referenceType = s.reference.type,
                    referenceDimension = s.reference.dimension,
                    displayValue = parsed,
                    displayUnit = s.reference.unit,
                    screenSignature = s.display?.signature,
                ),
            )
        }
        return confirmed
    }

    fun recalibrate() = session.update { s ->
        s.copy(
            step = MeasureStep.CALIBRATE,
            notice = null,
            calibrationPx = s.calibration?.screenReferenceLengthPx?.toFloat() ?: s.calibrationPx,
        )
    }

    fun continueToMeasure() = session.update { s ->
        if (s.calibration != null) s.copy(step = MeasureStep.MEASURE, notice = null) else s
    }

    private fun Session.withGuessedCalibration(): Session {
        calibration?.let { return copy(calibrationPx = it.screenReferenceLengthPx.toFloat()) }
        val area = calibrationArea ?: return copy(calibrationTouched = false)
        val display = display ?: return copy(calibrationTouched = false)
        val knownMm = ReferencePresets.knownLengthMm(reference)?.takeIf { it.signum() > 0 }
        val guess = knownMm?.toFloat()?.times(display.estimatedPxPerMm)
        val px = if (guess != null && guess.isFinite()) guess else area.maxLength / 2f
        return copy(calibrationPx = px.coerceIn(area.minLengthPx, area.maxLength), calibrationTouched = false)
    }

    // endregion

    // region Measurement

    fun onMeasureAreaChanged(area: MeasureArea) = session.update { s ->
        if (s.measureArea == area && s.line != null) return@update s
        val line = s.line?.let { LineGeometry.fitLine(it, area.bounds) }
            ?: LineGeometry.defaultLine(s.orientation, area.bounds)
        s.copy(measureArea = area, line = line)
    }

    fun moveHandle(handle: Handle, deltaX: Float, deltaY: Float) = session.update { s ->
        val area = s.measureArea ?: return@update s
        val line = s.line ?: return@update s
        s.copy(line = LineGeometry.moveHandle(line, handle, deltaX, deltaY, s.orientation, area.bounds, area.minLengthPx))
    }

    fun setOrientation(orientation: MeasurementOrientation) = session.update { s ->
        if (s.orientation == orientation) return@update s
        val area = s.measureArea
        val line = s.line
        val aligned = if (area != null && line != null) {
            LineGeometry.alignToOrientation(line, orientation, area.bounds, area.minLengthPx)
        } else {
            line
        }
        s.copy(orientation = orientation, line = aligned)
    }

    fun setMeasurementType(type: MeasurementType) = session.update { it.copy(measurementType = type) }

    fun setCustomLabel(label: String) = session.update { it.copy(customLabel = label.take(32)) }

    fun setResultUnit(unit: MeasurementUnit) = session.update { it.copy(resultUnit = unit) }

    /** Clears calibration, measurement and result. History is untouched. */
    fun resetSession() {
        val current = settings.value
        session.update { s ->
            Session(
                display = s.display,
                calibrationArea = s.calibrationArea,
                measurementType = current.defaultMeasurementType,
                resultUnit = current.defaultUnit,
            ).withGuessedCalibration()
        }
    }

    fun copyText(): String? {
        val s = session.value
        val calibration = s.calibration ?: return null
        val mm = currentResultMm(s) ?: return null
        return MeasurementFormatter.copyText(
            lengthMm = mm,
            unit = s.resultUnit,
            decimals = settings.value.decimalPlaces,
            type = s.measurementType,
            customLabel = s.customLabel,
            referenceName = calibration.referenceName,
            referenceValue = calibration.displayValue,
            referenceUnit = calibration.displayUnit,
        )
    }

    fun saveCurrentMeasurement(): Boolean {
        val s = session.value
        val calibration = s.calibration ?: return false
        val mm = currentResultMm(s) ?: return false
        val record = HistoryRecord(
            id = UUID.randomUUID().toString(),
            measurementType = s.measurementType,
            customLabel = s.customLabel.trim().takeIf { s.measurementType == MeasurementType.CUSTOM && it.isNotEmpty() },
            resultMm = mm.toPlainString(),
            unit = s.resultUnit,
            decimalPlaces = settings.value.decimalPlaces,
            referenceType = calibration.referenceType,
            referenceName = calibration.referenceName,
            referenceSizeMm = calibration.referenceLengthMm.toPlainString(),
            referenceDisplayValue = calibration.displayValue.toPlainString(),
            referenceDisplayUnit = calibration.displayUnit,
            referenceDimension = calibration.referenceDimension,
            timestampMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch { historyRepository.add(record) }
        return true
    }

    private fun currentResultMm(s: Session): BigDecimal? {
        val calibration = s.calibration ?: return null
        val line = s.line ?: return null
        if (s.step != MeasureStep.MEASURE) return null
        val px = PocketGaugeCalculator.lineLengthPixels(line.start, line.end, s.orientation)
        return runCatching { PocketGaugeCalculator.calculateObjectLength(px, calibration) }.getOrNull()
    }

    // endregion

    // region History

    /** Restores the saved details as the starting point; the old calibration is never reused. */
    fun restoreFromHistory(record: HistoryRecord) = session.update { s ->
        val name = when (record.referenceType) {
            ReferenceType.COIN -> record.referenceName.takeUnless { it == "Coin" }.orEmpty()
            ReferenceType.CUSTOM -> record.referenceName.takeUnless { it == ReferencePresets.CUSTOM_DEFAULT_NAME }.orEmpty()
            else -> ""
        }
        val draft = ReferenceDraft(
            type = record.referenceType,
            name = name,
            dimension = record.referenceDimension,
            valueText = record.referenceDisplayValueDecimal?.let(MeasurementFormatter::formatInput).orEmpty(),
            unit = record.referenceDisplayUnit,
        )
        s.copy(
            step = MeasureStep.CALIBRATE,
            reference = draft,
            calibration = null,
            measurementType = record.measurementType,
            customLabel = record.customLabel.orEmpty(),
            resultUnit = record.unit.takeIf { it in MeasurementUnit.resultUnits } ?: s.resultUnit,
            notice = NOTICE_RESTORED,
        ).withGuessedCalibration()
    }

    fun copyText(record: HistoryRecord): String? {
        val mm = record.resultMmValue ?: return null
        val referenceValue = record.referenceDisplayValueDecimal ?: return null
        return MeasurementFormatter.copyText(
            lengthMm = mm,
            unit = record.unit,
            decimals = record.decimalPlaces,
            type = record.measurementType,
            customLabel = record.customLabel,
            referenceName = record.referenceName,
            referenceValue = referenceValue,
            referenceUnit = record.referenceDisplayUnit,
        )
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    // endregion

    // region Settings

    fun setThemeMode(mode: ThemeMode) = launchSetting { setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launchSetting { setDynamicColor(enabled) }
    fun setDecimalPlaces(places: Int) = launchSetting { setDecimalPlaces(places) }
    fun setShowSecondaryUnits(enabled: Boolean) = launchSetting { setShowSecondaryUnits(enabled) }
    fun setHapticFeedback(enabled: Boolean) = launchSetting { setHapticFeedback(enabled) }
    fun setShowCalibrationTips(enabled: Boolean) = launchSetting { setShowCalibrationTips(enabled) }
    fun setShowRuler(enabled: Boolean) = launchSetting { setShowRuler(enabled) }

    fun setDefaultUnit(unit: MeasurementUnit) {
        session.update { it.copy(resultUnit = unit) }
        launchSetting { setDefaultUnit(unit) }
    }

    fun setDefaultMeasurementType(type: MeasurementType) {
        session.update { it.copy(measurementType = type) }
        launchSetting { setDefaultMeasurementType(type) }
    }

    fun resetSettings() {
        val defaults = AppSettings()
        session.update { it.copy(resultUnit = defaults.defaultUnit, measurementType = defaults.defaultMeasurementType) }
        launchSetting { reset() }
    }

    private fun launchSetting(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    // endregion

    private fun buildState(s: Session, settings: AppSettings): MeasureUiState {
        val draft = s.reference
        val parsed = MeasurementFormatter.parseDecimal(draft.valueText)
        val knownMm = ReferencePresets.knownLengthMm(draft)
        val referenceError = when {
            draft.valueText.isBlank() ->
                if (draft.type == ReferenceType.COIN) "Enter the coin's diameter." else "Enter the reference size."
            parsed == null -> "Enter a valid number."
            parsed.signum() <= 0 -> PocketGaugeCalculator.ERROR_REFERENCE_SIZE
            else -> null
        }
        val referenceValid = referenceError == null && parsed != null && knownMm != null

        val area = s.calibrationArea
        val calibrationWarning = when {
            area == null || !referenceValid -> null
            s.calibrationPx < area.shortLengthPx -> "Use a larger reference on screen for better calibration."
            s.calibrationPx >= area.maxLength - area.edgeMarginPx -> "Keep the reference line fully visible."
            else -> null
        }
        val estimatedPx = if (referenceValid) s.display?.let { knownMm!!.toFloat() * it.estimatedPxPerMm } else null
        val fitHint = if (area != null && estimatedPx != null && estimatedPx > area.maxLength * 1.05f) {
            "This reference may be longer than the screen. Rotate to landscape or use a shorter known length."
        } else {
            null
        }

        val calibration = s.calibration
        val decimals = settings.decimalPlaces
        var resultMm: BigDecimal? = null
        var resultError: String? = null
        var largeWarning: String? = null
        val line = s.line
        if (s.step == MeasureStep.MEASURE && calibration != null && line != null) {
            val px = PocketGaugeCalculator.lineLengthPixels(line.start, line.end, s.orientation)
            runCatching { PocketGaugeCalculator.calculateObjectLength(px, calibration) }
                .onSuccess { resultMm = it }
                .onFailure { resultError = it.message ?: PocketGaugeCalculator.ERROR_MEASUREMENT_LENGTH }
            val bounds = s.measureArea?.bounds
            if (bounds != null) {
                val extent = when (s.orientation) {
                    MeasurementOrientation.HORIZONTAL -> bounds.width
                    MeasurementOrientation.VERTICAL -> bounds.height
                    MeasurementOrientation.FREE -> bounds.diagonal
                }
                if (extent > 0f && px.toFloat() >= extent * LARGE_MEASUREMENT_RATIO) {
                    largeWarning = "Large measurements may be less reliable."
                }
            }
        }
        val mm = resultMm
        val secondary = if (mm != null && settings.showSecondaryUnits) {
            MeasurementUnit.secondaryUnits.filter { it != s.resultUnit }
                .map { MeasurementFormatter.format(mm, it, decimals) }
        } else {
            emptyList()
        }
        val title = MeasurementFormatter.resultTitle(s.measurementType, s.customLabel)

        return MeasureUiState(
            step = s.step,
            reference = draft,
            referenceName = ReferencePresets.displayName(draft),
            dimensions = ReferencePresets.dimensionsFor(draft.type),
            referenceError = referenceError,
            knownSizeText = if (referenceValid) MeasurementFormatter.formatReference(parsed!!, draft.unit) else null,
            knownValue = if (referenceValid) parsed!!.toDouble() else null,
            calibrationPx = s.calibrationPx,
            calibrationStartX = area?.startX ?: 0f,
            calibrationWarning = calibrationWarning,
            fitHint = fitHint,
            canContinue = referenceValid && area != null && s.calibrationPx > 0f,
            isCalibrated = calibration != null,
            calibrationSummary = calibration?.let { MeasurementFormatter.formatReference(it.displayValue, it.displayUnit) },
            calibrationReferenceName = calibration?.referenceName,
            notice = s.notice,
            measurementType = s.measurementType,
            customLabel = s.customLabel,
            orientation = s.orientation,
            line = line,
            millimetersPerPixel = calibration?.let {
                runCatching {
                    PocketGaugeCalculator.calculateMillimetersPerPixel(it.referenceLengthMm, it.screenReferenceLengthPx).toDouble()
                }.getOrNull()
            },
            resultUnit = s.resultUnit,
            resultTitle = title,
            resultText = mm?.let { MeasurementFormatter.format(it, s.resultUnit, decimals) },
            resultSpoken = mm?.let {
                "Estimated ${title.lowercase()} ${MeasurementFormatter.formatSpoken(it, s.resultUnit, decimals)}"
            },
            secondaryResults = secondary,
            resultError = resultError,
            largeMeasurementWarning = largeWarning,
            canSave = mm != null,
        )
    }

    companion object {
        const val NOTICE_REFERENCE_CHANGED = "Reference changed. Please calibrate again."
        const val NOTICE_DISPLAY_CHANGED = "Display changed. Please calibrate again."
        const val NOTICE_RESTORED = "Saved result restored. Calibrate again to measure."
        private const val LARGE_MEASUREMENT_RATIO = 0.9f
    }
}
