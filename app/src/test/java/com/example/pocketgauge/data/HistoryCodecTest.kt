package com.example.pocketgauge.data

import com.example.pocketgauge.model.HistoryRecord
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceDimension
import com.example.pocketgauge.model.ReferenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {

    private fun record(id: String, time: Long = 0L) = HistoryRecord(
        id = id,
        measurementType = MeasurementType.WIDTH,
        customLabel = null,
        resultMm = "45.3",
        unit = MeasurementUnit.MILLIMETER,
        decimalPlaces = 2,
        referenceType = ReferenceType.CREDIT_CARD,
        referenceName = "Credit Card",
        referenceSizeMm = "85.60",
        referenceDisplayValue = "85.60",
        referenceDisplayUnit = MeasurementUnit.MILLIMETER,
        referenceDimension = ReferenceDimension.WIDTH,
        timestampMillis = time,
    )

    @Test
    fun serializationRoundTrip() {
        val records = listOf(record("a", 1), record("b", 2).copy(measurementType = MeasurementType.CUSTOM, customLabel = "Bolt"))
        assertEquals(records, HistoryCodec.decode(HistoryCodec.encode(records)))
    }

    @Test
    fun decodeHandlesMissingOrCorruptData() {
        assertTrue(HistoryCodec.decode(null).isEmpty())
        assertTrue(HistoryCodec.decode("").isEmpty())
        assertTrue(HistoryCodec.decode("{not json").isEmpty())
    }

    @Test
    fun corruptEntryIsSkippedNotEverything() {
        val valid = HistoryCodec.encode(listOf(record("ok")))
        val mixed = valid.removeSuffix("]") + ",{\"id\":\"bad\",\"measurementType\":\"UNKNOWN\"}]"
        assertEquals(listOf("ok"), HistoryCodec.decode(mixed).map { it.id })
    }

    @Test
    fun historyIsCappedAtFiftyDroppingOldest() {
        var list = emptyList<HistoryRecord>()
        for (i in 1..51) list = HistoryCodec.addCapped(list, record("r$i", i.toLong()))
        assertEquals(HistoryCodec.MAX_RECORDS, list.size)
        assertEquals("r51", list.first().id)
        assertTrue(list.none { it.id == "r1" })
    }
}
