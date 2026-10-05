package com.example.pocketgauge.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pocketgauge.data.HistoryCodec
import com.example.pocketgauge.domain.MeasurementFormatter
import com.example.pocketgauge.model.HistoryRecord
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.ui.PocketGaugeViewModel
import com.example.pocketgauge.ui.components.GaugeIcons
import com.example.pocketgauge.ui.components.HintText
import com.example.pocketgauge.ui.components.copyToClipboard
import com.example.pocketgauge.ui.theme.GaugeType
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    viewModel: PocketGaugeViewModel,
    snackbarHostState: SnackbarHostState,
    onMeasureAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("HISTORY", style = GaugeType.brand)
                Text(
                    "${history.size} of ${HistoryCodec.MAX_RECORDS} saved estimates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { confirmClear = true }, enabled = history.isNotEmpty()) { Text("CLEAR ALL") }
        }

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(GaugeIcons.Ruler, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                    Text("No saved measurements", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use SAVE on the Measure screen to keep an estimate here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(history, key = { it.id }) { record ->
                    HistoryItem(
                        record = record,
                        date = dateFormat.format(Date(record.timestampMillis)),
                        onOpen = { openId = record.id },
                        onDelete = { viewModel.deleteRecord(record.id) },
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }

    val opened = history.firstOrNull { it.id == openId }
    if (opened != null) {
        HistoryDetailDialog(
            record = opened,
            date = dateFormat.format(Date(opened.timestampMillis)),
            showSecondary = settings.showSecondaryUnits,
            onDismiss = { openId = null },
            onCopy = {
                viewModel.copyText(opened)?.let { text ->
                    copyToClipboard(context, text)
                    scope.launch { snackbarHostState.showSnackbar("Measurement copied") }
                }
            },
            onMeasureAgain = {
                viewModel.restoreFromHistory(opened)
                openId = null
                onMeasureAgain()
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("All saved measurements will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClear = false
                    scope.launch { snackbarHostState.showSnackbar("History cleared") }
                }) { Text("CLEAR ALL") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("CANCEL") } },
        )
    }
}

private fun resultText(record: HistoryRecord): String =
    record.resultMmValue?.let { MeasurementFormatter.format(it, record.unit, record.decimalPlaces) } ?: "—"

private fun referenceText(record: HistoryRecord): String {
    val value = record.referenceDisplayValueDecimal ?: return record.referenceName
    return "${record.referenceName} · ${MeasurementFormatter.formatReference(value, record.referenceDisplayUnit)}"
}

@Composable
private fun HistoryItem(
    record: HistoryRecord,
    date: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = MeasurementFormatter.resultTitle(record.measurementType, record.customLabel)
    val result = resultText(record)
    Surface(
        onClick = onOpen,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, $result, reference ${record.referenceName}, $date. Open details." },
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = GaugeType.overline, color = MaterialTheme.colorScheme.primary)
                Text(result, style = GaugeType.value)
                Text("Reference: ${referenceText(record)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete measurement")
            }
        }
    }
}

@Composable
private fun HistoryDetailDialog(
    record: HistoryRecord,
    date: String,
    showSecondary: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onMeasureAgain: () -> Unit,
) {
    val mm = record.resultMmValue
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(MeasurementFormatter.resultTitle(record.measurementType, record.customLabel), style = GaugeType.section) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(resultText(record), style = GaugeType.resultCompact)
                if (showSecondary && mm != null) {
                    Text(
                        MeasurementUnit.secondaryUnits.filter { it != record.unit }
                            .joinToString("   ·   ") { MeasurementFormatter.format(mm, it, record.decimalPlaces) },
                        style = GaugeType.number,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Detail("Reference", record.referenceName)
                record.referenceDisplayValueDecimal?.let {
                    Detail("Calibration", MeasurementFormatter.formatReference(it, record.referenceDisplayUnit))
                }
                Detail("Measurement", record.customLabel ?: record.measurementType.label)
                Detail("Saved", date)
                Spacer(Modifier.height(6.dp))
                HintText("Saved estimate. Its calibration is no longer active — calibrate again to measure.")
            }
        },
        confirmButton = { TextButton(onClick = onMeasureAgain) { Text("MEASURE AGAIN") } },
        dismissButton = {
            Row {
                TextButton(onClick = onCopy, modifier = Modifier.semantics { contentDescription = "Copy measurement" }) { Text("COPY") }
                TextButton(onClick = onDismiss) { Text("CLOSE") }
            }
        },
    )
}

@Composable
private fun Detail(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
