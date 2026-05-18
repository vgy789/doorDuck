package io.github.vgy789.doorDuck.platform

import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.SyncError

data class PersistedDoorDuckState(
    val endpoint: String,
    val authToken: String,
    val userId: String,
    val lastSuccessAtMs: Long?,
    val expiresAtMs: Long?,
    val lastConnectionResult: ConnectionCheckResult?,
    val lastSyncError: SyncError?,
    val qrImageBase64: String?,
)

sealed interface PlatformQrRefreshResult {
    data class Success(
        val qrImageBase64: String,
        val receivedAtMs: Long,
        val expiresAtMs: Long,
    ) : PlatformQrRefreshResult

    data object NotConfigured : PlatformQrRefreshResult

    data class Failure(val error: SyncError) : PlatformQrRefreshResult
}

expect object DoorDuckPlatformServices {
    fun currentLanguageCode(): String
    fun currentTimeMillis(): Long
    suspend fun loadPersistedState(): PersistedDoorDuckState?
    suspend fun savePersistedState(state: PersistedDoorDuckState)
    suspend fun clearPersistedState()
    suspend fun verifyCredentials(endpoint: String, credentials: Credentials): ConnectionCheckResult
    suspend fun refreshQrCode(endpoint: String, credentials: Credentials): PlatformQrRefreshResult
}

expect fun formatEpochMillis(value: Long): String
