package io.github.vgy789.doorDuck.model

object Defaults {
    const val defaultEndpoint = ""
    const val revealDurationMillis = 60_000L
    const val manualRefreshCooldownMillis = 5_000L
    const val botUsername = "qr-code-generator.bot"
}

data class UserSettings(
    val endpoint: String = Defaults.defaultEndpoint,
    val autoRefreshEnabled: Boolean = true,
)

data class Credentials(
    val authToken: String,
    val userId: String,
)

enum class SyncError {
    NOT_CONFIGURED,
    UNAUTHORIZED,
    RATE_LIMITED,
    NETWORK,
    BOT_RESPONSE_INVALID,
    IMAGE_DOWNLOAD_FAILED,
    UNKNOWN,
}

enum class ConnectionCheckResult {
    SUCCESS,
    UNAUTHORIZED,
    BOT_UNAVAILABLE,
    NETWORK_ERROR,
    UNKNOWN,
}

data class QrCodeSnapshot(
    val localImagePath: String? = null,
    val receivedAtMs: Long? = null,
    val expiresAtMs: Long? = null,
    val nextAutoRefreshAtMs: Long? = null,
    val manualRefreshBlockedUntilMs: Long? = null,
    val lastSuccessAtMs: Long? = null,
    val revealUntilMs: Long? = null,
    val isSyncInProgress: Boolean = false,
    val lastError: SyncError? = null,
)

data class AppState(
    val settings: UserSettings,
    val snapshot: QrCodeSnapshot,
    val hasCredentials: Boolean,
)

sealed interface RefreshResult {
    data class Success(val snapshot: QrCodeSnapshot) : RefreshResult
    data object NotConfigured : RefreshResult
    data class Failure(
        val error: SyncError,
        val retryAfterMs: Long? = null,
    ) : RefreshResult
}
