package com.example.pocketgauge.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.pocketgauge.model.AppSettings
import com.example.pocketgauge.model.HistoryRecord
import com.example.pocketgauge.model.MeasurementType
import com.example.pocketgauge.model.MeasurementUnit
import com.example.pocketgauge.model.ReferenceType
import com.example.pocketgauge.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepositoriesTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(tempFolder.root, "test.preferences_pb")
    }

    @After
    fun tearDown() {
        if (::scope.isInitialized) scope.cancel()
    }

    private fun newStore(): DataStore<Preferences> {
        if (::scope.isInitialized) scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @Test
    fun settingsPersistAcrossInstances() = runBlocking {
        val repository = SettingsRepository(newStore())
        assertEquals(AppSettings(), repository.settings.first())

        repository.setThemeMode(ThemeMode.DARK)
        repository.setDefaultUnit(MeasurementUnit.INCH)
        repository.setDefaultMeasurementType(MeasurementType.DIAMETER)
        repository.setDecimalPlaces(3)
        repository.setShowRuler(false)
        repository.setHapticFeedback(false)

        val reopened = SettingsRepository(newStore()).settings.first()
        assertEquals(ThemeMode.DARK, reopened.themeMode)
        assertEquals(MeasurementUnit.INCH, reopened.defaultUnit)
        assertEquals(MeasurementType.DIAMETER, reopened.defaultMeasurementType)
        assertEquals(3, reopened.decimalPlaces)
        assertEquals(false, reopened.showRuler)
        assertEquals(false, reopened.hapticFeedback)
    }

    @Test
    fun decimalPlacesAreClamped() = runBlocking {
        val repository = SettingsRepository(newStore())
        repository.setDecimalPlaces(9)
        assertEquals(4, repository.settings.first().decimalPlaces)
    }

    @Test
    fun resetSettingsKeepsHistory() = runBlocking {
        val store = newStore()
        val settings = SettingsRepository(store)
        val history = HistoryRepository(store)
        settings.setThemeMode(ThemeMode.LIGHT)
        history.add(sampleRecord("keep"))

        settings.reset()

        assertEquals(AppSettings(), settings.settings.first())
        assertEquals(listOf("keep"), history.history.first().map { it.id })
    }

    @Test
    fun historyAddDeleteClear() = runBlocking {
        val history = HistoryRepository(newStore())
        history.add(sampleRecord("a"))
        history.add(sampleRecord("b"))
        assertEquals(listOf("b", "a"), history.history.first().map { it.id })

        history.delete("b")
        assertEquals(listOf("a"), history.history.first().map { it.id })

        history.clear()
        assertEquals(emptyList<HistoryRecord>(), history.history.first())
    }

    private fun sampleRecord(id: String) = HistoryRecord(
        id = id,
        measurementType = MeasurementType.LENGTH,
        resultMm = "74.2",
        unit = MeasurementUnit.CENTIMETER,
        referenceType = ReferenceType.RULER,
        referenceName = "10 cm ruler",
        referenceSizeMm = "100",
        referenceDisplayValue = "10",
        referenceDisplayUnit = MeasurementUnit.CENTIMETER,
        timestampMillis = 1L,
    )
}
