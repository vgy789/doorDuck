package io.github.vgy789.doorDuck.update

import io.github.vgy789.doorDuck.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface UpdateCheckResult {
    data class Available(val release: AppRelease) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val cause: Throwable) : UpdateCheckResult
}

class UpdateRepository(
    private val settingsStore: UpdateSettingsStore,
    private val json: Json,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun settings(): UpdateSettings = settingsStore.get()

    suspend fun setAutomaticChecksEnabled(enabled: Boolean) {
        settingsStore.setAutomaticChecksEnabled(enabled)
    }

    suspend fun shouldRunAutomaticCheck(nowMs: Long = System.currentTimeMillis()): Boolean {
        return isAutomaticCheckDue(settingsStore.get(), nowMs)
    }

    suspend fun cachedResult(): UpdateCheckResult? {
        val release = settingsStore.get().cachedRelease ?: return null
        return compareWithCurrent(release)
    }

    suspend fun check(nowMs: Long = System.currentTimeMillis()): UpdateCheckResult = withContext(Dispatchers.IO) {
        val settings = settingsStore.get()
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "doorDuck/${BuildConfig.VERSION_NAME}")
            .apply { settings.etag?.takeIf(String::isNotBlank)?.let { header("If-None-Match", it) } }
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    settingsStore.touchCheck(nowMs)
                    return@use settings.cachedRelease?.let(::compareWithCurrent) ?: UpdateCheckResult.UpToDate
                }
                if (!response.isSuccessful) throw IOException("GitHub Releases HTTP ${response.code}")
                val body = response.body?.string() ?: throw IOException("GitHub response is empty")
                val release = json.decodeFromString<GitHubReleaseDto>(body).toAppRelease()
                    ?: throw IOException("Release does not contain signed Android update assets")
                settingsStore.saveCheckResult(nowMs, response.header("ETag"), release)
                compareWithCurrent(release)
            }
        }.getOrElse(UpdateCheckResult::Failed)
    }

    private fun compareWithCurrent(release: AppRelease): UpdateCheckResult {
        val current = SemanticVersion.parse(BuildConfig.VERSION_NAME)
            ?: return UpdateCheckResult.Failed(IllegalStateException("Invalid current app version"))
        val latest = SemanticVersion.parse(release.tag)
            ?: return UpdateCheckResult.Failed(IllegalStateException("Invalid release version"))
        return if (latest > current) UpdateCheckResult.Available(release) else UpdateCheckResult.UpToDate
    }

    companion object {
        const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1_000L
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/vgy789/doorDuck/releases/latest"
    }
}

internal fun isAutomaticCheckDue(settings: UpdateSettings, nowMs: Long): Boolean {
    if (!settings.automaticChecksEnabled) return false
    return settings.lastCheckedAtMs == null || nowMs - settings.lastCheckedAtMs >= UpdateRepository.CHECK_INTERVAL_MS
}
