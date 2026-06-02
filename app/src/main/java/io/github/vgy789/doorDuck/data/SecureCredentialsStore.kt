package io.github.vgy789.doorDuck.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.vgy789.doorDuck.model.Credentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureCredentialsStore(context: Context) {
    private val hasCredentialsState = MutableStateFlow(false)

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    init {
        hasCredentialsState.value = hasCredentialsInternal()
    }

    fun observeHasCredentials(): Flow<Boolean> = hasCredentialsState.asStateFlow()

    fun hasCredentials(): Boolean = hasCredentialsInternal()

    fun load(): Credentials? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null).orEmpty()
        val userId = prefs.getString(KEY_USER_ID, null).orEmpty()
        if (token.isBlank() || userId.isBlank()) return null
        return Credentials(authToken = token, userId = userId)
    }

    fun loadAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun loadUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun save(authToken: String, userId: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, authToken)
            .putString(KEY_USER_ID, userId)
            .apply()
        hasCredentialsState.value = true
    }

    fun clear() {
        prefs.edit().clear().commit()
        hasCredentialsState.value = false
    }

    private fun hasCredentialsInternal(): Boolean {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        return !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    private companion object {
        const val PREFS_NAME = "door_duck_secure_credentials"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_USER_ID = "user_id"
    }
}
