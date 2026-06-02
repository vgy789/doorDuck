package io.github.vgy789.doorDuck.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vgy789.doorDuck.model.AppState
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.QrCodeSnapshot
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.model.UserSettings
import io.github.vgy789.doorDuck.ui.InputSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "door_duck_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val endpoint = stringPreferencesKey("endpoint")
        val autoRefreshEnabled = booleanPreferencesKey("auto_refresh_enabled")
        val maxBrightnessEnabled = booleanPreferencesKey("max_brightness_enabled")
        val qrPath = stringPreferencesKey("qr_path")
        val receivedAtMs = longPreferencesKey("received_at_ms")
        val expiresAtMs = longPreferencesKey("expires_at_ms")
        val nextAutoRefreshAtMs = longPreferencesKey("next_auto_refresh_at_ms")
        val manualRefreshBlockedUntilMs = longPreferencesKey("manual_refresh_blocked_until_ms")
        val lastSuccessAtMs = longPreferencesKey("last_success_at_ms")
        val revealUntilMs = longPreferencesKey("reveal_until_ms")
        val syncInProgress = booleanPreferencesKey("sync_in_progress")
        val lastError = stringPreferencesKey("last_error")
    }

    fun observeAppState(hasCredentialsFlow: Flow<Boolean>): Flow<AppState> {
        return combine(context.dataStore.data, hasCredentialsFlow) { prefs, hasCredentials ->
            AppState(
                settings = prefs.toUserSettings(),
                snapshot = prefs.toSnapshot(),
                hasCredentials = hasCredentials,
            )
        }
    }

    suspend fun getSettings(): UserSettings = context.dataStore.data.first().toUserSettings()

    suspend fun getSnapshot(): QrCodeSnapshot = context.dataStore.data.first().toSnapshot()

    suspend fun updateCredentialsSettings(endpoint: String, autoRefreshEnabled: Boolean? = null) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.endpoint] = InputSanitizer.endpoint(endpoint)
            if (autoRefreshEnabled != null) {
                mutablePrefs[Keys.autoRefreshEnabled] = autoRefreshEnabled
            }
        }
    }

    suspend fun setAutoRefreshEnabled(enabled: Boolean) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.autoRefreshEnabled] = enabled
        }
    }

    suspend fun setMaxBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.maxBrightnessEnabled] = enabled
        }
    }

    suspend fun setSyncInProgress(inProgress: Boolean) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.syncInProgress] = inProgress
        }
    }

    suspend fun setRevealUntil(revealUntilMs: Long?) {
        context.dataStore.edit { mutablePrefs ->
            if (revealUntilMs == null) {
                mutablePrefs.remove(Keys.revealUntilMs)
            } else {
                mutablePrefs[Keys.revealUntilMs] = revealUntilMs
            }
        }
    }

    suspend fun setNextAutoRefreshAt(nextAutoRefreshAtMs: Long?) {
        context.dataStore.edit { mutablePrefs ->
            if (nextAutoRefreshAtMs == null) {
                mutablePrefs.remove(Keys.nextAutoRefreshAtMs)
            } else {
                mutablePrefs[Keys.nextAutoRefreshAtMs] = nextAutoRefreshAtMs
            }
        }
    }

    suspend fun setManualRefreshBlockedUntil(manualRefreshBlockedUntilMs: Long?) {
        context.dataStore.edit { mutablePrefs ->
            if (manualRefreshBlockedUntilMs == null) {
                mutablePrefs.remove(Keys.manualRefreshBlockedUntilMs)
            } else {
                mutablePrefs[Keys.manualRefreshBlockedUntilMs] = manualRefreshBlockedUntilMs
            }
        }
    }

    suspend fun saveSyncSuccess(path: String, receivedAtMs: Long, expiresAtMs: Long?, nextAutoRefreshAtMs: Long?) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.qrPath] = path
            mutablePrefs[Keys.receivedAtMs] = receivedAtMs
            if (expiresAtMs == null) {
                mutablePrefs.remove(Keys.expiresAtMs)
            } else {
                mutablePrefs[Keys.expiresAtMs] = expiresAtMs
            }
            if (nextAutoRefreshAtMs == null) {
                mutablePrefs.remove(Keys.nextAutoRefreshAtMs)
            } else {
                mutablePrefs[Keys.nextAutoRefreshAtMs] = nextAutoRefreshAtMs
            }
            mutablePrefs[Keys.lastSuccessAtMs] = receivedAtMs
            mutablePrefs.remove(Keys.lastError)
            mutablePrefs[Keys.syncInProgress] = false
        }
    }

    suspend fun saveSyncError(error: SyncError) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.lastError] = error.name
            mutablePrefs[Keys.syncInProgress] = false
        }
    }

    suspend fun clearInProgress() {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[Keys.syncInProgress] = false
        }
    }

    suspend fun clear() {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs.clear()
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val endpointValue = this[Keys.endpoint].orEmpty().ifBlank { Defaults.defaultEndpoint }
        return UserSettings(
            endpoint = endpointValue,
            autoRefreshEnabled = this[Keys.autoRefreshEnabled] ?: true,
            maxBrightnessEnabled = this[Keys.maxBrightnessEnabled] ?: false,
        )
    }

    private fun Preferences.toSnapshot(): QrCodeSnapshot {
        return QrCodeSnapshot(
            localImagePath = this[Keys.qrPath],
            receivedAtMs = this[Keys.receivedAtMs],
            expiresAtMs = this[Keys.expiresAtMs],
            nextAutoRefreshAtMs = this[Keys.nextAutoRefreshAtMs],
            manualRefreshBlockedUntilMs = this[Keys.manualRefreshBlockedUntilMs],
            lastSuccessAtMs = this[Keys.lastSuccessAtMs],
            revealUntilMs = this[Keys.revealUntilMs],
            isSyncInProgress = this[Keys.syncInProgress] ?: false,
            lastError = this[Keys.lastError]?.let {
                runCatching { SyncError.valueOf(it) }.getOrNull()
            },
        )
    }
}
