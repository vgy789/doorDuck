package io.github.vgy789.doorDuck.platform

import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.SyncError
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private var cachedState: PersistedDoorDuckState? = null

actual object DoorDuckPlatformServices {
    actual fun currentLanguageCode(): String = Locale.getDefault().language

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual suspend fun loadPersistedState(): PersistedDoorDuckState? = cachedState

    actual suspend fun savePersistedState(state: PersistedDoorDuckState) {
        cachedState = state
    }

    actual suspend fun clearPersistedState() {
        cachedState = null
    }

    actual suspend fun verifyCredentials(
        endpoint: String,
        credentials: Credentials,
    ): ConnectionCheckResult = ConnectionCheckResult.UNKNOWN

    actual suspend fun refreshQrCode(
        endpoint: String,
        credentials: Credentials,
    ): PlatformQrRefreshResult {
        return if (credentials.authToken.isBlank() || credentials.userId.isBlank()) {
            PlatformQrRefreshResult.NotConfigured
        } else {
            PlatformQrRefreshResult.Failure(SyncError.UNKNOWN)
        }
    }
}

actual fun formatEpochMillis(value: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))
}
