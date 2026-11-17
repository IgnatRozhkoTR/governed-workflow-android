package com.ignatrozhko.governedworkflow.data.server

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

class ServerNotFoundException(id: String) : Exception("Server $id is not saved.")

/** Server list and selection in DataStore; tokens are stored only as Keystore-encrypted blobs. */
class ServerRepository(
    private val dataStore: DataStore<Preferences>,
    private val tokenCipher: TokenCipher,
    private val json: Json,
) {
    private val listSerializer = ListSerializer(ServerEntry.serializer())

    val servers: Flow<List<ServerEntry>> = dataStore.data.map(::readServers).distinctUntilChanged()

    val selectedServerId: Flow<String?> = dataStore.data.map { it[SELECTED_SERVER_KEY] }.distinctUntilChanged()

    val selectedCredentials: Flow<ServerCredentials?> = dataStore.data
        .map { preferences -> preferences[SELECTED_SERVER_KEY]?.let { id -> readCredentials(preferences, id) } }
        .distinctUntilChanged()

    suspend fun save(id: String?, name: String, urls: List<String>, token: String): ServerEntry {
        val entry = ServerEntry(id ?: UUID.randomUUID().toString(), name, urls)
        val encryptedToken = tokenCipher.encrypt(token)
        dataStore.edit { preferences ->
            val others = readServers(preferences).filterNot { it.id == entry.id }
            preferences[SERVERS_KEY] = json.encodeToString(listSerializer, others + entry)
            preferences[tokenKey(entry.id)] = encryptedToken
        }
        return entry
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            preferences[SERVERS_KEY] = json.encodeToString(listSerializer, readServers(preferences).filterNot { it.id == id })
            preferences.remove(tokenKey(id))
            if (preferences[SELECTED_SERVER_KEY] == id) preferences.remove(SELECTED_SERVER_KEY)
        }
    }

    suspend fun select(id: String) {
        dataStore.edit { it[SELECTED_SERVER_KEY] = id }
    }

    suspend fun credentials(id: String): ServerCredentials =
        readCredentials(dataStore.data.first(), id) ?: throw ServerNotFoundException(id)

    private fun readCredentials(preferences: Preferences, id: String): ServerCredentials? {
        val server = readServers(preferences).firstOrNull { it.id == id } ?: return null
        val encryptedToken = preferences[tokenKey(id)] ?: return null
        return ServerCredentials(server, tokenCipher.decrypt(encryptedToken))
    }

    private fun readServers(preferences: Preferences): List<ServerEntry> =
        preferences[SERVERS_KEY]?.let { json.decodeFromString(listSerializer, it) }.orEmpty()

    private fun tokenKey(id: String) = stringPreferencesKey("$TOKEN_KEY_PREFIX$id")

    private companion object {
        val SERVERS_KEY = stringPreferencesKey("servers")
        val SELECTED_SERVER_KEY = stringPreferencesKey("selected_server_id")
        const val TOKEN_KEY_PREFIX = "token_"
    }
}
