package io.github.vgy789.doorDuck.model

object Defaults {
    const val baseEndpoint = DoorDuckSecrets.baseEndpoint
    const val rocketTokensUrl = DoorDuckSecrets.rocketTokensUrl
    const val intensiveMskEndpoint = DoorDuckSecrets.intensiveMskEndpoint
    const val intensiveNskEndpoint = DoorDuckSecrets.intensiveNskEndpoint
    const val intensiveKznEndpoint = DoorDuckSecrets.intensiveKznEndpoint
    const val donatePhoneValue = DoorDuckSecrets.donatePhoneValue
    const val donateCardValue = DoorDuckSecrets.donateCardValue
    const val intensiveEndpoint = intensiveMskEndpoint
    const val defaultEndpoint = baseEndpoint
    const val revealDurationMillis = 60_000L
    const val manualRefreshCooldownMillis = 5_000L
    const val syncInProgressTimeoutMillis = 5L * 60L * 1000L
    const val botUsername = "qr-code-generator.bot"
}

enum class IntensiveCampus(val endpoint: String?) {
    MOSCOW(Defaults.intensiveMskEndpoint),
    NOVOSIBIRSK(Defaults.intensiveNskEndpoint),
    KAZAN(Defaults.intensiveKznEndpoint),
    OTHER(null),
}

data class UserSettings(
    val endpoint: String = Defaults.defaultEndpoint,
    val autoRefreshEnabled: Boolean = true,
    val maxBrightnessEnabled: Boolean = false,
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
    BOT_NOT_FOUND,
    BOT_RESPONSE_INVALID,
    IMAGE_DOWNLOAD_FAILED,
    UNKNOWN,
}

enum class ConnectionCheckResult {
    SUCCESS,
    UNAUTHORIZED,
    BOT_NOT_FOUND,
    BOT_UNAVAILABLE,
    NETWORK_ERROR,
    UNKNOWN,
}

enum class QrImageValidationStatus {
    UNKNOWN,
    VALID,
    INVALID,
}

enum class QrReadiness {
    READY,
    CHECK_REQUIRED,
    EXPIRED,
    MISSING_OR_INVALID,
}

data class QrCodeSnapshot(
    val localImagePath: String? = null,
    val receivedAtMs: Long? = null,
    val expiresAtMs: Long? = null,
    val nextAutoRefreshAtMs: Long? = null,
    val manualRefreshBlockedUntilMs: Long? = null,
    val lastSuccessAtMs: Long? = null,
    val imageValidationStatus: QrImageValidationStatus = QrImageValidationStatus.UNKNOWN,
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
