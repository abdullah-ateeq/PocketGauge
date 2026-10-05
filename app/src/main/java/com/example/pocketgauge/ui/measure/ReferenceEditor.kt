package com.example.pocketgauge.ui.measure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pocketgauge.domain.MeasurementFormatter
import com.example.pocketgauge.domain.ReferencePresets
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceType
import com.example.pocketgauge.ui.MeasureUiState
import com.example.pocketgauge.ui.PocketGaugeViewModel
import com.example.pocketgauge.ui.components.GaugeIcons
import com.example.pocketgauge.ui.components.HintText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceEditor(
    state: MeasureUiState,
    viewModel: PocketGaugeViewModel,
    showTips: Boolean,
    modifier: Modifier = Modifier,
) {
    val reference = state.reference
    val focusManager = LocalFocusManager.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Reference selector" },
        ) {
            ReferenceType.entries.forEach { type ->
                val selected = reference.type == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectReferenceType(type) },
                    label = { Text(type.shortName) },
                    leadingIcon = if (selected) {
                        { Icon(GaugeIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
        }

        Text(
            text = state.referenceName,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )

        when (reference.type) {
            ReferenceType.CREDIT_CARD -> {
                HintText("Assumes a standard ISO/IEC ID-1 card: 85.60 × 53.98 mm. Edit the size if yours differs.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dimensions.forEach { dimension ->
                        FilterChip(
                            selected = reference.dimension == dimension,
                            onClick = { viewModel.selectReferenceDimension(dimension) },
                            label = { Text("Measure ${dimension.label.lowercase()}") },
                        )
                    }
                }
            }
            ReferenceType.RULER -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val current = MeasurementFormatter.parseDecimal(reference.valueText)
                    ReferencePresets.RULER_LENGTHS_CM.forEach { length ->
                        FilterChip(
                            selected = reference.unit == MeasurementUnit.CENTIMETER && current?.compareTo(length) == 0,
                            onClick = { viewModel.selectRulerLength(length) },
                            label = { Text("${MeasurementFormatter.formatInput(length)} cm") },
                        )
                    }
                }
                HintText("You can calibrate against any marked section of the ruler.")
            }
            ReferenceType.COIN -> {
                OutlinedTextField(
                    value = reference.name,
                    onValueChange = viewModel::setReferenceName,
                    label = { Text("Coin name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                HintText("Coin sizes vary by country. Enter the known diameter of the coin you have.")
            }
            ReferenceType.CUSTOM -> {
                OutlinedTextField(
                    value = reference.name,
                    onValueChange = viewModel::setReferenceName,
                    label = { Text("Reference name") },
                    placeholder = { Text("e.g. Known object") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "Quick reference lengths" },
                ) {
                    ReferencePresets.QUICK_REFERENCES.forEach { quick ->
                        SuggestionChip(onClick = { viewModel.applyQuickReference(quick) }, label = { Text(quick.label) })
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reference.valueText,
                onValueChange = viewModel::setReferenceValue,
                label = { Text("Known ${reference.dimension.label.lowercase()}") },
                singleLine = true,
                isError = state.referenceError != null && reference.valueText.isNotBlank(),
                supportingText = state.referenceError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Known reference length" },
            )
            UnitDropdown(
                selected = reference.unit,
                onSelect = viewModel::setReferenceUnit,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (showTips) {
            HintText("Place the known reference and the object on the same surface and align them with the on-screen guide.")
        }
    }
}

@Composable
private fun UnitDropdown(
    selected: MeasurementUnit,
    onSelect: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .heightIn(min = 56.dp)
                .semantics { contentDescription = "Reference unit, ${selected.spokenName}" },
        ) {
            Text(selected.symbol)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasurementUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.symbol}  ·  ${unit.spokenName}") },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    },
                    leadingIcon = if (unit == selected) {
                        { Icon(GaugeIcons.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
