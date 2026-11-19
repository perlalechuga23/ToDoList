package com.example.todolist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "preferencias_usuario")

class UserPreferences(private val context: Context) {

    companion object {
        private val USER_SESSION_KEY = stringPreferencesKey("user_session")
        private val USER_PREF_KEY = stringPreferencesKey("user_preference")
    }

    suspend fun saveSession(token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_SESSION_KEY] = token
        }
    }

    suspend fun savePreferences(configValue: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PREF_KEY] = configValue
        }
    }

    val sessionFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_SESSION_KEY]
        }

    val preferencesFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_PREF_KEY]
        }
}