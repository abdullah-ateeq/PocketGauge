package com.example.pocketgauge.ui.measure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pocketgauge.domain.Handle
import com.example.pocketgauge.model.AppSettings
import com.example.pocketgauge.model.MeasurementOrientation
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.ui.DisplayInfo
import com.example.pocketgauge.ui.MeasureStep
import com.example.pocketgauge.ui.MeasureUiState
import com.example.pocketgauge.ui.PocketGaugeViewModel
import com.example.pocketgauge.ui.components.BrandHeader
import com.example.pocketgauge.ui.components.GaugeHaptics
import com.example.pocketgauge.ui.components.GaugeIcons
import com.example.pocketgauge.ui.components.HintText
import com.example.pocketgauge.ui.components.NoticeBanner
import com.example.pocketgauge.ui.components.SectionLabel
import com.example.pocketgauge.ui.components.copyToClipboard
import com.example.pocketgauge.ui.components.rememberGaugeHaptics
import com.example.pocketgauge.ui.theme.GaugeType
import kotlinx.coroutines.launch

private const val FINE_STEP_PX = 1f
private const val COARSE_STEP_PX = 10f
private val Gutter = 16.dp

const val ACCURACY_MESSAGE = "Estimated measurement. Accuracy depends on calibration, alignment, and object position."

@Composable
fun MeasureScreen(
    viewModel: PocketGaugeViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.measureState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberGaugeHaptics(settings.hapticFeedback)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val metrics = context.resources.displayMetrics
    LaunchedEffect(metrics.xdpi, metrics.ydpi, metrics.densityDpi) {
        viewModel.setDisplayInfo(DisplayInfo(metrics.xdpi, metrics.ydpi, metrics.densityDpi))
    }

    val actions = MeasureActions(
        onCopy = {
            viewModel.copyText()?.let { text ->
                copyToClipboard(context, text)
                scope.launch { snackbarHostState.showSnackbar("Measurement copied") }
            }
        },
        onSave = {
            if (viewModel.saveCurrentMeasurement()) {
                haptics.confirm()
                scope.launch { snackbarHostState.showSnackbar("Measurement saved") }
            }
        },
        onReset = {
            viewModel.resetSession()
            haptics.confirm()
            scope.launch { snackbarHostState.showSnackbar("Measurement reset") }
        },
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp && (maxWidth > maxHeight || maxWidth >= 840.dp)
        when (state.step) {
            MeasureStep.CALIBRATE -> CalibrationStep(state, settings, viewModel, haptics, wide)
            MeasureStep.MEASURE -> MeasurementStep(
                state = state,
                settings = settings,
                viewModel = viewModel,
                haptics = haptics,
                actions = actions,
                wide = wide,
                canvasHeight = (maxHeight * 0.56f).coerceAtLeast(300.dp),
            )
        }
    }
}

private class MeasureActions(
    val onCopy: () -> Unit,
    val onSave: () -> Unit,
    val onReset: () -> Unit,
)

// region Calibration step

@Composable
private fun CalibrationStep(
    state: MeasureUiState,
    settings: AppSettings,
    viewModel: PocketGaugeViewModel,
    haptics: GaugeHaptics,
    wide: Boolean,
) {
    val ruler: @Composable () -> Unit = {
        CalibrationRuler(
            lengthPx = state.calibrationPx,
            knownValue = state.knownValue,
            knownSizeText = state.knownSizeText,
            showTicks = settings.showRuler,
            onAreaChanged = viewModel::onCalibrationAreaChanged,
            onDrag = viewModel::adjustCalibration,
            onDragStart = haptics::tick,
            onIncrease = { viewModel.adjustCalibration(FINE_STEP_PX) },
            onDecrease = { viewModel.adjustCalibration(-FINE_STEP_PX) },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TopBlock(state, viewModel, subtitle = "Estimate an object's size.")
        if (wide) {
            CalibrationTitle(state, settings.showCalibrationTips, Modifier.padding(horizontal = Gutter))
            ruler()
            Row(
                modifier = Modifier.padding(horizontal = Gutter),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("REFERENCE")
                    ReferenceEditor(state, viewModel, settings.showCalibrationTips)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("CALIBRATION")
                    CalibrationControls(state, viewModel, haptics)
                }
            }
        } else {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("REFERENCE")
                ReferenceEditor(state, viewModel, settings.showCalibrationTips)
            }
            HorizontalDivider(Modifier.padding(horizontal = Gutter))
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("CALIBRATION")
                CalibrationTitle(state, settings.showCalibrationTips)
            }
            ruler()
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CalibrationControls(state, viewModel, haptics)
            }
        }
    }
}

@Composable
private fun CalibrationTitle(state: MeasureUiState, showTips: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CALIBRATE REFERENCE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Known size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.knownSizeText ?: "—", style = GaugeType.value)
        }
        if (showTips) {
            HintText("Place the physical reference next to the screen. Adjust the line until it matches the reference length.")
            HintText("Keep the object and reference in the same plane for better results.")
        }
    }
}

@Composable
private fun CalibrationControls(state: MeasureUiState, viewModel: PocketGaugeViewModel, haptics: GaugeHaptics) {
    val adjust: (Float) -> Unit = { delta ->
        viewModel.adjustCalibration(delta)
        haptics.tick()
    }
    val enabled = state.knownSizeText != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedIconButton(
            onClick = { adjust(-COARSE_STEP_PX) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Decrease calibration length, coarse" },
        ) { Text("«", style = MaterialTheme.typography.titleMedium) }
        FilledTonalIconButton(
            onClick = { adjust(-FINE_STEP_PX) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Decrease calibration length" },
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        FilledTonalIconButton(
            onClick = { adjust(FINE_STEP_PX) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Increase calibration length" },
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        OutlinedIconButton(
            onClick = { adjust(COARSE_STEP_PX) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Increase calibration length, coarse" },
        ) { Text("»", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = {
                viewModel.resetCalibrationLine()
                haptics.tick()
            },
            modifier = Modifier.semantics { contentDescription = "Reset calibration line" },
        ) {
            Icon(GaugeIcons.Reset, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("RESET")
        }
    }
    HintText("Outer buttons: coarse  ·  inner buttons: fine. You can also drag the ruler.")

    state.calibrationWarning?.let { NoticeBanner(it, warning = true) }
    state.fitHint?.let { NoticeBanner(it) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.isCalibrated) {
            Icon(GaugeIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text("CALIBRATED", style = GaugeType.section, color = MaterialTheme.colorScheme.primary)
                Text("Reference: ${state.calibrationSummary}", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Icon(GaugeIcons.Ruler, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Column {
                Text("NOT CALIBRATED", style = GaugeType.section, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Match the line to your reference, then continue.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Button(
        onClick = {
            if (viewModel.confirmCalibration()) haptics.confirm()
        },
        enabled = state.canContinue,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) { Text("CONTINUE") }
}

// endregion

// region Measurement step

@Composable
private fun MeasurementStep(
    state: MeasureUiState,
    settings: AppSettings,
    viewModel: PocketGaugeViewModel,
    haptics: GaugeHaptics,
    actions: MeasureActions,
    wide: Boolean,
    canvasHeight: Dp,
) {
    val canvas: @Composable (Modifier) -> Unit = { modifier ->
        MeasurementCanvas(
            line = state.line,
            orientation = state.orientation,
            millimetersPerPixel = state.millimetersPerPixel,
            imperialScale = state.resultUnit == MeasurementUnit.INCH || state.resultUnit == MeasurementUnit.FOOT,
            showScale = settings.showRuler,
            label = state.resultText,
            spokenResult = state.resultSpoken,
            onAreaChanged = viewModel::onMeasureAreaChanged,
            onMoveHandle = { handle: Handle, dx: Float, dy: Float -> viewModel.moveHandle(handle, dx, dy) },
            onHandleGrab = haptics::tick,
            modifier = modifier,
        )
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TopBlock(state, viewModel, subtitle = "Estimate an object's size.")
                Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalibrationSummary(state, viewModel)
                    MeasurementOptions(state, viewModel)
                    ResultPanel(state, settings, viewModel, actions)
                }
            }
            canvas(
                Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBlock(state, viewModel, subtitle = "Estimate an object's size.")
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CalibrationSummary(state, viewModel)
                MeasurementOptions(state, viewModel)
            }
            canvas(
                Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
            )
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (settings.showCalibrationTips) {
                    HintText("For best results, keep the reference and object at the same distance and angle.")
                }
                ResultPanel(state, settings, viewModel, actions)
            }
        }
    }
}

@Composable
private fun CalibrationSummary(state: MeasureUiState, viewModel: PocketGaugeViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(GaugeIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("CALIBRATED", style = GaugeType.overline, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${state.calibrationReferenceName.orEmpty()} · ${state.calibrationSummary.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(
                onClick = viewModel::recalibrate,
                modifier = Modifier.semantics { contentDescription = "Recalibrate" },
            ) { Text("RECALIBRATE") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MeasurementOptions(state: MeasureUiState, viewModel: PocketGaugeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("MEASUREMENT")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Measurement type" },
        ) {
            MeasurementType.entries.forEach { type ->
                FilterChip(
                    selected = state.measurementType == type,
                    onClick = { viewModel.setMeasurementType(type) },
                    label = { Text(type.label) },
                )
            }
        }
        if (state.measurementType == MeasurementType.CUSTOM) {
            OutlinedTextField(
                value = state.customLabel,
                onValueChange = viewModel::setCustomLabel,
                label = { Text("Measurement label") },
                placeholder = { Text("e.g. Screw length") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Line orientation" },
        ) {
            val options = MeasurementOrientation.entries
            options.forEachIndexed { index, orientation ->
                SegmentedButton(
                    selected = state.orientation == orientation,
                    onClick = { viewModel.setOrientation(orientation) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(orientation.label.uppercase(), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ResultPanel(
    state: MeasureUiState,
    settings: AppSettings,
    viewModel: PocketGaugeViewModel,
    actions: MeasureActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("RESULT")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.resultTitle,
                style = GaugeType.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "ESTIMATE",
                style = GaugeType.overline,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(
            text = state.resultText ?: "—",
            style = GaugeType.result,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = state.resultSpoken ?: "No estimate yet"
            },
        )
        if (state.secondaryResults.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                state.secondaryResults.forEach { text ->
                    Text(text, style = GaugeType.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        state.resultError?.let { HintText(it, color = MaterialTheme.colorScheme.error) }
        state.largeMeasurementWarning?.let { NoticeBanner(it, warning = true) }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Result unit" },
        ) {
            val units = MeasurementUnit.resultUnits
            units.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = state.resultUnit == unit,
                    onClick = { viewModel.setResultUnit(unit) },
                    shape = SegmentedButtonDefaults.itemShape(index, units.size),
                    label = { Text(unit.symbol) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow("Reference", state.calibrationReferenceName ?: "—")
            DetailRow("Calibration", state.calibrationSummary ?: "—")
            DetailRow(
                "Measurement",
                if (state.measurementType == MeasurementType.CUSTOM) state.customLabel.ifBlank { "Custom" } else state.measurementType.label,
            )
        }

        HintText(ACCURACY_MESSAGE)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = actions.onCopy,
                enabled = state.canSave,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Copy measurement" },
            ) {
                Icon(GaugeIcons.Copy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("COPY")
            }
            Button(
                onClick = actions.onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Save measurement" },
            ) {
                Icon(GaugeIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SAVE")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = viewModel::recalibrate,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Recalibrate" },
            ) {
                Icon(GaugeIcons.Recalibrate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("RECALIBRATE")
            }
            TextButton(
                onClick = actions.onReset,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Reset" },
            ) {
                Icon(GaugeIcons.Reset, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("RESET")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

// endregion

// region Header + stepper

@Composable
private fun TopBlock(state: MeasureUiState, viewModel: PocketGaugeViewModel, subtitle: String) {
    Column(
        modifier = Modifier.padding(start = Gutter, end = Gutter, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BrandHeader(subtitle = subtitle)
        WorkflowStepper(
            step = state.step,
            referenceReady = state.knownSizeText != null,
            isCalibrated = state.isCalibrated,
            onCalibrate = viewModel::recalibrate,
            onMeasure = viewModel::continueToMeasure,
        )
        state.notice?.let { NoticeBanner(it, warning = true) }
    }
}

@Composable
private fun WorkflowStepper(
    step: MeasureStep,
    referenceReady: Boolean,
    isCalibrated: Boolean,
    onCalibrate: () -> Unit,
    onMeasure: () -> Unit,
) {
    val measuring = step == MeasureStep.MEASURE
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepItem(1, "Reference", done = referenceReady, active = !measuring && !referenceReady, onClick = if (measuring) onCalibrate else null)
        StepConnector(Modifier.weight(1f), filled = referenceReady)
        StepItem(2, "Calibrate", done = measuring, active = !measuring && referenceReady, onClick = if (measuring) onCalibrate else null)
        StepConnector(Modifier.weight(1f), filled = measuring)
        StepItem(3, "Measure", done = false, active = measuring, onClick = if (!measuring && isCalibrated) onMeasure else null)
    }
}

@Composable
private fun StepItem(number: Int, label: String, done: Boolean, active: Boolean, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val state = when {
        done -> "completed"
        active -> "current step"
        else -> "not started"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "Step $number, $label, $state" }
            .padding(vertical = 6.dp, horizontal = 2.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .background(if (done) colors.primary else colors.surface, CircleShape)
                .border(1.5.dp, if (done || active) colors.primary else colors.outlineVariant, CircleShape),
        ) {
            if (done) {
                Icon(GaugeIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active || done) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepConnector(modifier: Modifier, filled: Boolean) {
    Box(
        modifier
            .padding(horizontal = 6.dp)
            .height(1.5.dp)
            .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    )
}

// endregion
