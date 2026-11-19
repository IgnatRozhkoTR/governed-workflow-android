package com.ignatrozhko.governedworkflow.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppSettings(private val dataStore: DataStore<Preferences>) {

    val terminalFontSize: Flow<Int> = dataStore.data
        .map { it[TERMINAL_FONT_SIZE_KEY] ?: DEFAULT_TERMINAL_FONT_SIZE }
        .distinctUntilChanged()

    /** Off by default: the watcher keeps a foreground service and a persistent notification alive. */
    val isGateWatchEnabled: Flow<Boolean> = dataStore.data
        .map { it[GATE_WATCH_KEY] ?: false }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences -> ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE_KEY] } ?: ThemeMode.System }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setGateWatchEnabled(isEnabled: Boolean) {
        dataStore.edit { it[GATE_WATCH_KEY] = isEnabled }
    }

    suspend fun setTerminalFontSize(size: Int) {
        dataStore.edit { it[TERMINAL_FONT_SIZE_KEY] = size.coerceIn(MIN_TERMINAL_FONT_SIZE, MAX_TERMINAL_FONT_SIZE) }
    }

    companion object {
        const val DEFAULT_TERMINAL_FONT_SIZE = 12
        const val MIN_TERMINAL_FONT_SIZE = 6
        const val MAX_TERMINAL_FONT_SIZE = 32
        private val TERMINAL_FONT_SIZE_KEY = intPreferencesKey("terminal_font_size")
        private val GATE_WATCH_KEY = booleanPreferencesKey("gate_watch_enabled")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
