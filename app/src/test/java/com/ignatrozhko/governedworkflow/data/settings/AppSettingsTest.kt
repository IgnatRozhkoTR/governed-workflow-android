package com.ignatrozhko.governedworkflow.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppSettingsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "settings.preferences_pb") }

    @Test
    fun extraKeysMode_shouldDefaultToShown_whenNeverSet() = runBlocking {
        val mode = withSettings { it.extraKeysMode.first() }

        assertEquals(ExtraKeysMode.Shown, mode)
    }

    @Test
    fun extraKeysMode_shouldSurviveRestart_whenSet() = runBlocking {
        withSettings { it.setExtraKeysMode(ExtraKeysMode.Hidden) }

        val mode = withSettings { it.extraKeysMode.first() }

        assertEquals(ExtraKeysMode.Hidden, mode)
    }

    @Test
    fun extraKeysMode_shouldFallBackToShown_whenStoredValueIsUnknown() = runBlocking {
        withStore { store -> store.edit { it[stringPreferencesKey("terminal_extra_keys_mode")] = "Sideways" } }

        val mode = withSettings { it.extraKeysMode.first() }

        assertEquals(ExtraKeysMode.Shown, mode)
    }

    /** Each call opens and closes its own store on the same file, as an app restart would. */
    private suspend fun <T> withStore(block: suspend (DataStore<Preferences>) -> T): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        return try {
            block(store)
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }

    private suspend fun <T> withSettings(block: suspend (AppSettings) -> T): T = withStore { block(AppSettings(it)) }
}
