package com.example.pocketgauge.ui.about

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.pocketgauge.ui.components.GaugeIcons
import com.example.pocketgauge.ui.components.SectionLabel
import com.example.pocketgauge.ui.theme.GaugeType

private val features = listOf(
    "Screen Calibration",
    "Reference Objects",
    "Length Measurement",
    "Width Measurement",
    "Height Measurement",
    "Diameter Estimation",
    "Multiple Units",
    "Measurement History",
    "Copy Result",
    "Offline",
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PocketGauge", style = GaugeType.brand.copy(letterSpacing = GaugeType.brand.letterSpacing / 4))
        Text("Version 1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RulerAccent()
        Text(
            "An offline screen-calibrated utility for estimating the size of physical objects using a known reference.",
            style = MaterialTheme.typography.bodyLarge,
        )

        SectionLabel("FEATURES")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(GaugeIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }

        SectionLabel("ACCURACY")
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(14.dp)) {
            Text(
                "PocketGauge provides an estimate based on visual alignment and screen calibration. It is not a precision measuring instrument.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }

        SectionLabel("PRIVACY")
        Text(
            "No camera, location, network or accounts. Settings and history are stored only on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RulerAccent() {
    val color = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        val step = 8.dp.toPx()
        var i = 0
        var x = 0f
        while (x <= size.width) {
            val h = if (i % 5 == 0) size.height else size.height * 0.45f
            drawLine(color, Offset(x, y - h), Offset(x, y), 1.dp.toPx())
            i++
            x = step * i
        }
    }
}
