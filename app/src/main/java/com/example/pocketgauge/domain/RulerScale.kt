package com.example.pocketgauge.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

data class RulerTick(
    /** Position along the ruler, 0 = start, 1 = end. */
    val fraction: Float,
    val isMajor: Boolean,
    val label: String?,
)

/** Chooses readable tick spacing for the calibration ruler (e.g. 0, 2, 4 … 10 for 10 cm). */
object RulerScale {
    private const val TARGET_INTERVALS = 5

    fun niceStep(raw: Double): Double {
        if (!(raw > 0) || !raw.isFinite()) return 1.0
        val exponent = floor(log10(raw))
        val magnitude = 10.0.pow(exponent)
        val fraction = raw / magnitude
        val nice = when {
            fraction <= 1.0 + 1e-9 -> 1.0
            fraction <= 2.0 + 1e-9 -> 2.0
            fraction <= 5.0 + 1e-9 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    fun nextNiceStep(step: Double): Double = niceStep(step * 1.01)

    fun ticks(
        knownValue: Double,
        lengthPx: Float,
        minLabelSpacingPx: Float,
        minMinorSpacingPx: Float,
    ): List<RulerTick> {
        if (!(knownValue > 0) || !knownValue.isFinite() || !(lengthPx > 0)) return emptyList()
        val pxPerUnit = lengthPx / knownValue

        var major = niceStep(knownValue / TARGET_INTERVALS)
        var guard = 0
        while (major * pxPerUnit < minLabelSpacingPx && guard++ < 24) {
            major = nextNiceStep(major)
        }
        val minor = listOf(major / 10, major / 5, major / 2)
            .firstOrNull { it * pxPerUnit >= minMinorSpacingPx }
        val epsilon = major * 1e-6
        val ticks = mutableListOf<RulerTick>()

        if (minor != null) {
            var i = 1L
            while (true) {
                val value = i * minor
                if (value >= knownValue - epsilon) break
                val ratio = value / major
                if (abs(ratio - ratio.roundToLong()) > 1e-6) {
                    ticks += RulerTick((value / knownValue).toFloat(), isMajor = false, label = null)
                }
                i++
            }
        }

        val endLabelZone = (minLabelSpacingPx * 0.6) / pxPerUnit
        var j = 0L
        var lastMajor = 0.0
        while (true) {
            val value = j * major
            if (value > knownValue + epsilon) break
            val isEnd = abs(value - knownValue) <= epsilon
            val crowdsEnd = !isEnd && j > 0 && knownValue - value < endLabelZone
            ticks += RulerTick(
                fraction = (value / knownValue).toFloat().coerceIn(0f, 1f),
                isMajor = true,
                label = if (crowdsEnd) null else formatTick(value),
            )
            lastMajor = value
            j++
        }
        if (abs(lastMajor - knownValue) > epsilon) {
            ticks += RulerTick(1f, isMajor = true, label = formatTick(knownValue))
        }
        return ticks.sortedBy { it.fraction }
    }

    fun formatTick(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
