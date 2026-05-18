package io.github.vgy789.doorDuck.model

object Defaults {
    const val defaultEndpoint = "https://rocketchat-student.21-school.ru/api/v1"
    const val qrValidityDays = 30
    const val revealDurationMillis = 60_000L
    const val botUsername = "qr-code-generator.bot"
}

data class UserSettings(
    val endpoint: String = Defaults.defaultEndpoint,
)

data class Credentials(
    val authToken: String,
    val userId: String,
)

enum class SyncError {
    NOT_CONFIGURED,
    UNAUTHORIZED,
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
    data class Failure(val error: SyncError) : RefreshResult
}
