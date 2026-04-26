package com.rbt.survey.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ID = stringPreferencesKey("user_id")
        val DGPS_DEVICE_ADDRESS = stringPreferencesKey("dgps_device_address")
        val CORS_HOST = stringPreferencesKey("cors_host")
        val CORS_PORT = stringPreferencesKey("cors_port")
        val CORS_MOUNTPOINT = stringPreferencesKey("cors_mountpoint")
        val CORS_USER = stringPreferencesKey("cors_user")
        val CORS_PASS = stringPreferencesKey("cors_pass")
        val USE_DGPS = stringPreferencesKey("use_dgps") // "true" or "false"
        val USE_CORS = stringPreferencesKey("use_cors") // "true" or "false"
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val dgpsDeviceAddress: Flow<String?> = context.dataStore.data.map { it[DGPS_DEVICE_ADDRESS] }
    val corsHost: Flow<String?> = context.dataStore.data.map { it[CORS_HOST] }
    val corsPort: Flow<String?> = context.dataStore.data.map { it[CORS_PORT] }
    val corsMountpoint: Flow<String?> = context.dataStore.data.map { it[CORS_MOUNTPOINT] }
    val corsUser: Flow<String?> = context.dataStore.data.map { it[CORS_USER] }
    val corsPass: Flow<String?> = context.dataStore.data.map { it[CORS_PASS] }
    val useDgps: Flow<Boolean> = context.dataStore.data.map { it[USE_DGPS] == "true" }
    val useCors: Flow<Boolean> = context.dataStore.data.map { it[USE_CORS] != "false" } // Default to true

    suspend fun saveAuthData(token: String, refreshToken: String, name: String, email: String, userId: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USER_NAME] = name
            prefs[USER_EMAIL] = email
            prefs[USER_ID] = userId
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveDgpsSettings(
        address: String?,
        host: String?,
        port: String?,
        mountpoint: String?,
        user: String?,
        pass: String?,
        useDgps: Boolean,
        useCors: Boolean
    ) {
        context.dataStore.edit { prefs ->
            address?.let { prefs[DGPS_DEVICE_ADDRESS] = it }
            host?.let { prefs[CORS_HOST] = it }
            port?.let { prefs[CORS_PORT] = it }
            mountpoint?.let { prefs[CORS_MOUNTPOINT] = it }
            user?.let { prefs[CORS_USER] = it }
            pass?.let { prefs[CORS_PASS] = it }
            prefs[USE_DGPS] = useDgps.toString()
            prefs[USE_CORS] = useCors.toString()
        }
    }
}
