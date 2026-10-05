package com.example.pocketgauge.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pocketgauge.domain.MeasurementFormatter
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ThemeMode
import com.example.pocketgauge.ui.PocketGaugeViewModel
import com.example.pocketgauge.ui.components.HintText
import com.example.pocketgauge.ui.components.SectionLabel
import com.example.pocketgauge.ui.theme.GaugeType
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: PocketGaugeViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SETTINGS", style = GaugeType.brand)

        SectionLabel("APPEARANCE")
        SettingLabel("Theme")
        Segmented(
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            description = "Theme",
            onSelect = viewModel::setThemeMode,
        )
        SwitchRow(
            title = "Dynamic color",
            subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or newer",
            checked = settings.dynamicColor && dynamicSupported,
            enabled = dynamicSupported,
            onChange = viewModel::setDynamicColor,
        )

        HorizontalDivider()
        SectionLabel("MEASUREMENT")
        SettingLabel("Default unit")
        Segmented(
            options = MeasurementUnit.resultUnits,
            selected = settings.defaultUnit,
            label = { it.symbol },
            description = "Default unit",
            onSelect = viewModel::setDefaultUnit,
        )
        SettingLabel("Default measurement type")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MeasurementType.LENGTH, MeasurementType.WIDTH, MeasurementType.HEIGHT, MeasurementType.DIAMETER).forEach { type ->
                FilterChip(
                    selected = settings.defaultMeasurementType == type,
                    onClick = { viewModel.setDefaultMeasurementType(type) },
                    label = { Text(type.label) },
                )
            }
        }
        SettingLabel("Decimal places")
        Segmented(
            options = (MeasurementFormatter.MIN_DECIMALS..MeasurementFormatter.MAX_DECIMALS).toList(),
            selected = settings.decimalPlaces,
            label = { it.toString() },
            description = "Decimal places",
            onSelect = viewModel::setDecimalPlaces,
        )
        SwitchRow(
            title = "Show secondary units",
            subtitle = "Also show the result in mm, cm and in",
            checked = settings.showSecondaryUnits,
            onChange = viewModel::setShowSecondaryUnits,
        )

        HorizontalDivider()
        SectionLabel("INTERFACE")
        SwitchRow("Haptic feedback", "Subtle feedback on adjust, save and reset", settings.hapticFeedback, onChange = viewModel::setHapticFeedback)
        SwitchRow("Show calibration tips", "Short alignment instructions", settings.showCalibrationTips, onChange = viewModel::setShowCalibrationTips)
        SwitchRow("Show ruler", "Tick marks on the calibration line and measurement scale", settings.showRuler, onChange = viewModel::setShowRuler)

        HorizontalDivider()
        SectionLabel("DATA")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.weight(1f)) { Text("RESET SETTINGS") }
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) { Text("CLEAR HISTORY") }
        }
        HintText("Everything stays on this device. PocketGauge works fully offline.")
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults. Saved history is kept.",
            confirm = "RESET",
            onConfirm = {
                viewModel.resetSettings()
                scope.launch { snackbarHostState.showSnackbar("Settings reset") }
            },
            onDismiss = { confirmReset = false },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear history?",
            text = "All saved measurements will be removed. This cannot be undone.",
            confirm = "CLEAR",
            onConfirm = {
                viewModel.clearHistory()
                scope.launch { snackbarHostState.showSnackbar("History cleared") }
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}
