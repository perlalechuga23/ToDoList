package com.example.todolist

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "preferencias_usuario")

class UserPreferences(private val context: Context) {
    // Clase inicial de DataStore
}