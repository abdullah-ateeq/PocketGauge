package com.example.pocketgauge.data

import com.example.pocketgauge.model.HistoryRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** JSON encoding for history. Corrupt or unknown entries are skipped instead of discarding everything. */
object HistoryCodec {
    const val MAX_RECORDS = 50

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(records: List<HistoryRecord>): String = json.encodeToString(records)

    fun decode(raw: String?): List<HistoryRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { json.parseToJsonElement(raw) as? JsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element: JsonElement ->
            runCatching { json.decodeFromJsonElement(HistoryRecord.serializer(), element) }.getOrNull()
        }
    }

    /** Newest first; drops the oldest records beyond [MAX_RECORDS]. */
    fun addCapped(records: List<HistoryRecord>, record: HistoryRecord): List<HistoryRecord> =
        (listOf(record) + records.filterNot { it.id == record.id }).take(MAX_RECORDS)
}
