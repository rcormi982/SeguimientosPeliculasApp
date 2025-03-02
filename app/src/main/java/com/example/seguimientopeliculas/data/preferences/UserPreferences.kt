package com.example.seguimientopeliculas.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para inicializar DataStore
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(context: Context) {

    private val dataStore = context.dataStore

    // Claves para las preferencias
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    private val jwtTokenKey = stringPreferencesKey("jwt_token")
    private val userIdKey = stringPreferencesKey("user_id")

    // Leer el estado de sesión
    val isLoggedIn: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[isLoggedInKey] ?: false // Devuelve false si no está configurado
        }

    // Leer el token JWT
    val jwtToken: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[jwtTokenKey]
        }

    // Leer el ID del usuario
    val userId: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[userIdKey]
        }

    // Guardar datos en DataStore
    suspend fun saveUserSession(isLoggedIn: Boolean, jwtToken: String?, userId: String?) {
        dataStore.edit { preferences ->
            preferences[isLoggedInKey] = isLoggedIn
            preferences[jwtTokenKey] = jwtToken ?: ""
            preferences[userIdKey] = userId ?: ""
        }
    }

    // Limpiar la sesión
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
