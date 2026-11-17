package com.ignatrozhko.governedworkflow.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppSettings(private val dataStore: DataStore<Preferences>) {

    val terminalFontSize: Flow<Int> = dataStore.data
        .map { it[TERMINAL_FONT_SIZE_KEY] ?: DEFAULT_TERMINAL_FONT_SIZE }
        .distinctUntilChanged()

    suspend fun setTerminalFontSize(size: Int) {
        dataStore.edit { it[TERMINAL_FONT_SIZE_KEY] = size.coerceIn(MIN_TERMINAL_FONT_SIZE, MAX_TERMINAL_FONT_SIZE) }
    }

    companion object {
        const val DEFAULT_TERMINAL_FONT_SIZE = 12
        const val MIN_TERMINAL_FONT_SIZE = 6
        const val MAX_TERMINAL_FONT_SIZE = 32
        private val TERMINAL_FONT_SIZE_KEY = intPreferencesKey("terminal_font_size")
    }
}
