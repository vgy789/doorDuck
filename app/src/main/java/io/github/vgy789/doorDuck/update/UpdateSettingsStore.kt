package io.github.vgy789.doorDuck.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "door_duck_updates")

data class UpdateSettings(
    val automaticChecksEnabled: Boolean = false,
    val lastCheckedAtMs: Long? = null,
    val etag: String? = null,
    val cachedRelease: AppRelease? = null,
    val readyReleaseTag: String? = null,
)

class UpdateSettingsStore(
    private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val automaticChecksEnabled = booleanPreferencesKey("automatic_checks_enabled")
        val lastCheckedAtMs = longPreferencesKey("last_checked_at_ms")
        val etag = stringPreferencesKey("etag")
        val cachedRelease = stringPreferencesKey("cached_release")
        val readyReleaseTag = stringPreferencesKey("ready_release_tag")
    }

    suspend fun get(): UpdateSettings {
        val prefs = context.updateDataStore.data.first()
        return UpdateSettings(
            automaticChecksEnabled = prefs[Keys.automaticChecksEnabled] ?: false,
            lastCheckedAtMs = prefs[Keys.lastCheckedAtMs],
            etag = prefs[Keys.etag],
            cachedRelease = prefs[Keys.cachedRelease]?.let { encoded ->
                runCatching { json.decodeFromString<AppRelease>(encoded) }.getOrNull()
            },
            readyReleaseTag = prefs[Keys.readyReleaseTag],
        )
    }

    suspend fun setAutomaticChecksEnabled(enabled: Boolean) {
        context.updateDataStore.edit { it[Keys.automaticChecksEnabled] = enabled }
    }

    suspend fun saveCheckResult(
        checkedAtMs: Long,
        etag: String?,
        release: AppRelease?,
    ) {
        context.updateDataStore.edit { prefs ->
            prefs[Keys.lastCheckedAtMs] = checkedAtMs
            if (etag.isNullOrBlank()) prefs.remove(Keys.etag) else prefs[Keys.etag] = etag
            if (release == null) {
                prefs.remove(Keys.cachedRelease)
            } else {
                prefs[Keys.cachedRelease] = json.encodeToString(release)
            }
        }
    }

    suspend fun touchCheck(checkedAtMs: Long) {
        context.updateDataStore.edit { it[Keys.lastCheckedAtMs] = checkedAtMs }
    }

    suspend fun clearCachedRelease() {
        context.updateDataStore.edit { it.remove(Keys.cachedRelease) }
    }

    suspend fun setReadyReleaseTag(tag: String?) {
        context.updateDataStore.edit { prefs ->
            if (tag == null) prefs.remove(Keys.readyReleaseTag) else prefs[Keys.readyReleaseTag] = tag
        }
    }

    suspend fun clear() {
        context.updateDataStore.edit { it.clear() }
    }
}
