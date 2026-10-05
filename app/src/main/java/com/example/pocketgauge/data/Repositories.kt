package com.example.pocketgauge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pocketgauge.domain.MeasurementFormatter
import com.example.pocketgauge.model.AppSettings
import com.example.pocketgauge.model.HistoryRecord
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.pocketGaugeDataStore: DataStore<Preferences> by preferencesDataStore(name = "pocketgauge")

private fun Flow<Preferences>.safe(): Flow<Preferences> = catch { error ->
    if (error is IOException) emit(emptyPreferences()) else throw error
}

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DEFAULT_TYPE = stringPreferencesKey("default_measurement_type")
        val DECIMALS = intPreferencesKey("decimal_places")
        val SECONDARY_UNITS = booleanPreferencesKey("show_secondary_units")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
        val TIPS = booleanPreferencesKey("show_calibration_tips")
        val RULER = booleanPreferencesKey("show_ruler")
        val ALL = listOf(THEME, DYNAMIC_COLOR, DEFAULT_UNIT, DEFAULT_TYPE, DECIMALS, SECONDARY_UNITS, HAPTICS, TIPS, RULER)
    }

    val settings: Flow<AppSettings> = dataStore.data.safe().map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            themeMode = enumOrDefault(prefs[Keys.THEME], defaults.themeMode),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultUnit = enumOrDefault(prefs[Keys.DEFAULT_UNIT], defaults.defaultUnit)
                .takeIf { it in MeasurementUnit.resultUnits } ?: defaults.defaultUnit,
            defaultMeasurementType = enumOrDefault(prefs[Keys.DEFAULT_TYPE], defaults.defaultMeasurementType),
            decimalPlaces = (prefs[Keys.DECIMALS] ?: defaults.decimalPlaces)
                .coerceIn(MeasurementFormatter.MIN_DECIMALS, MeasurementFormatter.MAX_DECIMALS),
            showSecondaryUnits = prefs[Keys.SECONDARY_UNITS] ?: defaults.showSecondaryUnits,
            hapticFeedback = prefs[Keys.HAPTICS] ?: defaults.hapticFeedback,
            showCalibrationTips = prefs[Keys.TIPS] ?: defaults.showCalibrationTips,
            showRuler = prefs[Keys.RULER] ?: defaults.showRuler,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setDefaultUnit(unit: MeasurementUnit) = dataStore.edit { it[Keys.DEFAULT_UNIT] = unit.name }
    suspend fun setDefaultMeasurementType(type: MeasurementType) = dataStore.edit { it[Keys.DEFAULT_TYPE] = type.name }
    suspend fun setDecimalPlaces(places: Int) = dataStore.edit {
        it[Keys.DECIMALS] = places.coerceIn(MeasurementFormatter.MIN_DECIMALS, MeasurementFormatter.MAX_DECIMALS)
    }
    suspend fun setShowSecondaryUnits(enabled: Boolean) = dataStore.edit { it[Keys.SECONDARY_UNITS] = enabled }
    suspend fun setHapticFeedback(enabled: Boolean) = dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setShowCalibrationTips(enabled: Boolean) = dataStore.edit { it[Keys.TIPS] = enabled }
    suspend fun setShowRuler(enabled: Boolean) = dataStore.edit { it[Keys.RULER] = enabled }

    /** Restores defaults without touching history. */
    suspend fun reset() = dataStore.edit { prefs -> Keys.ALL.forEach { prefs.remove(it) } }
}

class HistoryRepository(private val dataStore: DataStore<Preferences>) {
    private val historyKey = stringPreferencesKey("history_json")

    val history: Flow<List<HistoryRecord>> = dataStore.data.safe().map { HistoryCodec.decode(it[historyKey]) }

    suspend fun add(record: HistoryRecord) = dataStore.edit { prefs ->
        prefs[historyKey] = HistoryCodec.encode(HistoryCodec.addCapped(HistoryCodec.decode(prefs[historyKey]), record))
    }

    suspend fun delete(id: String) = dataStore.edit { prefs ->
        prefs[historyKey] = HistoryCodec.encode(HistoryCodec.decode(prefs[historyKey]).filterNot { it.id == id })
    }

    suspend fun clear() = dataStore.edit { it.remove(historyKey) }
}
