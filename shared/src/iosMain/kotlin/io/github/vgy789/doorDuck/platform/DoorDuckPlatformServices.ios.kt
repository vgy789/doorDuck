package io.github.vgy789.doorDuck.platform

import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.SyncError
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUserDefaults
import platform.posix.gettimeofday
import platform.posix.timeval

private const val KEY_ENDPOINT = "door_duck.endpoint"
private const val KEY_TOKEN = "door_duck.token"
private const val KEY_USER_ID = "door_duck.user_id"
private const val KEY_LAST_SUCCESS_AT = "door_duck.last_success_at"
private const val KEY_EXPIRES_AT = "door_duck.expires_at"
private const val KEY_MANUAL_REFRESH_BLOCKED_UNTIL = "door_duck.manual_refresh_blocked_until"
private const val KEY_LAST_CONNECTION_RESULT = "door_duck.last_connection_result"
private const val KEY_LAST_SYNC_ERROR = "door_duck.last_sync_error"
private const val KEY_QR_IMAGE_BASE64 = "door_duck.qr_image_base64"
private const val APP_GROUP_ID = "group.io.github.vgy789.doorDuck"
private const val APPLE_REFERENCE_EPOCH_SECONDS = 978307200.0
private const val POLL_ATTEMPTS = 6
private const val POLL_DELAY_MS = 2_000L

private val dataUriImageRegex = Regex("!\\[[^\\]]*\\]\\((data:image/[^;]+;base64,[^)]+)\\)")
private val expirationRegex = Regex("(?i)expire\\s+on\\s+(\\d{2}\\.\\d{2}\\.\\d{4})")

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private val client = HttpClient(Darwin) {
    expectSuccess = false
}

actual object DoorDuckPlatformServices {
    actual fun currentLanguageCode(): String {
        return NSUserDefaults.standardUserDefaults
            .stringArrayForKey("AppleLanguages")
            ?.firstOrNull()
            ?.toString()
            ?.substringBefore('-')
            ?: "en"
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long = memScoped {
        val timeValue = alloc<timeval>()
        gettimeofday(timeValue.ptr, null)
        (timeValue.tv_sec * 1000L) + (timeValue.tv_usec / 1000L)
    }

    actual suspend fun loadPersistedState(): PersistedDoorDuckState? {
        val defaults = sharedDefaults()
        val endpoint = defaults.stringForKey(KEY_ENDPOINT)?.takeIf { it.isNotBlank() } ?: Defaults.defaultEndpoint
        val token = defaults.stringForKey(KEY_TOKEN).orEmpty()
        val userId = defaults.stringForKey(KEY_USER_ID).orEmpty()
        val lastSuccessAt = defaults.stringForKey(KEY_LAST_SUCCESS_AT)?.toLongOrNull()
        val expiresAt = defaults.stringForKey(KEY_EXPIRES_AT)?.toLongOrNull()
        val manualRefreshBlockedUntilMs = defaults.stringForKey(KEY_MANUAL_REFRESH_BLOCKED_UNTIL)?.toLongOrNull()
        val result = defaults.stringForKey(KEY_LAST_CONNECTION_RESULT)?.let {
            runCatching { ConnectionCheckResult.valueOf(it) }.getOrNull()
        }
        val lastSyncError = defaults.stringForKey(KEY_LAST_SYNC_ERROR)?.let {
            runCatching { SyncError.valueOf(it) }.getOrNull()
        }
        val qrImageBase64 = defaults.stringForKey(KEY_QR_IMAGE_BASE64)
        if (
            token.isBlank() &&
            userId.isBlank() &&
            result == null &&
            lastSyncError == null &&
            manualRefreshBlockedUntilMs == null &&
            qrImageBase64.isNullOrBlank() &&
            endpoint == Defaults.defaultEndpoint
        ) {
            return null
        }
        return PersistedDoorDuckState(
            endpoint = endpoint,
            authToken = token,
            userId = userId,
            lastSuccessAtMs = lastSuccessAt,
            expiresAtMs = expiresAt,
            manualRefreshBlockedUntilMs = manualRefreshBlockedUntilMs,
            lastConnectionResult = result,
            lastSyncError = lastSyncError,
            qrImageBase64 = qrImageBase64,
        )
    }

    actual suspend fun savePersistedState(state: PersistedDoorDuckState) {
        val defaults = sharedDefaults()
        defaults.setObject(state.endpoint, KEY_ENDPOINT)
        defaults.setObject(state.authToken, KEY_TOKEN)
        defaults.setObject(state.userId, KEY_USER_ID)
        saveOptionalString(defaults, KEY_LAST_SUCCESS_AT, state.lastSuccessAtMs?.toString())
        saveOptionalString(defaults, KEY_EXPIRES_AT, state.expiresAtMs?.toString())
        saveOptionalString(defaults, KEY_MANUAL_REFRESH_BLOCKED_UNTIL, state.manualRefreshBlockedUntilMs?.toString())
        saveOptionalString(defaults, KEY_LAST_CONNECTION_RESULT, state.lastConnectionResult?.name)
        saveOptionalString(defaults, KEY_LAST_SYNC_ERROR, state.lastSyncError?.name)
        saveOptionalString(defaults, KEY_QR_IMAGE_BASE64, state.qrImageBase64)
    }

    actual suspend fun clearPersistedState() {
        val defaults = sharedDefaults()
        defaults.removeObjectForKey(KEY_ENDPOINT)
        defaults.removeObjectForKey(KEY_TOKEN)
        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_LAST_SUCCESS_AT)
        defaults.removeObjectForKey(KEY_EXPIRES_AT)
        defaults.removeObjectForKey(KEY_MANUAL_REFRESH_BLOCKED_UNTIL)
        defaults.removeObjectForKey(KEY_LAST_CONNECTION_RESULT)
        defaults.removeObjectForKey(KEY_LAST_SYNC_ERROR)
        defaults.removeObjectForKey(KEY_QR_IMAGE_BASE64)
    }

    actual suspend fun verifyCredentials(
        endpoint: String,
        credentials: Credentials,
    ): ConnectionCheckResult {
        val normalizedEndpoint = endpoint.trim().removeSuffix("/")
        val meResponse = runCatching {
            executeJsonRequest(
                url = "$normalizedEndpoint/me",
                method = "GET",
                credentials = credentials,
                body = null,
            )
        }.getOrElse { return ConnectionCheckResult.NETWORK_ERROR }

        if (meResponse.statusCode == 401 || meResponse.statusCode == 403) {
            return ConnectionCheckResult.UNAUTHORIZED
        }
        if (meResponse.statusCode !in 200..299) {
            return ConnectionCheckResult.UNKNOWN
        }

        val me = runCatching { json.decodeFromString<MeResponse>(meResponse.body) }.getOrNull()
        if (me?.success != true) {
            return ConnectionCheckResult.UNAUTHORIZED
        }

        val dmResponse = runCatching {
            executeJsonRequest(
                url = "$normalizedEndpoint/dm.create",
                method = "POST",
                credentials = credentials,
                body = json.encodeToString(DmCreateRequest.serializer(), DmCreateRequest(Defaults.botUsername)),
            )
        }.getOrElse { return ConnectionCheckResult.NETWORK_ERROR }

        return when {
            dmResponse.statusCode == 401 || dmResponse.statusCode == 403 -> ConnectionCheckResult.UNAUTHORIZED
            dmResponse.statusCode == 400 || dmResponse.statusCode == 404 -> ConnectionCheckResult.BOT_UNAVAILABLE
            dmResponse.statusCode !in 200..299 -> ConnectionCheckResult.UNKNOWN
            else -> {
                val dm = runCatching { json.decodeFromString<DmCreateResponse>(dmResponse.body) }.getOrNull()
                val roomId = dm?.room?.rid ?: dm?.room?.id
                if (dm?.success == true && !roomId.isNullOrBlank()) {
                    ConnectionCheckResult.SUCCESS
                } else {
                    ConnectionCheckResult.BOT_UNAVAILABLE
                }
            }
        }
    }

    actual suspend fun refreshQrCode(
        endpoint: String,
        credentials: Credentials,
    ): PlatformQrRefreshResult {
        if (credentials.authToken.isBlank() || credentials.userId.isBlank()) {
            return PlatformQrRefreshResult.NotConfigured
        }

        val normalizedEndpoint = endpoint.trim().removeSuffix("/")
        val roomId = runCatching {
            createDirectMessageRoom(normalizedEndpoint, credentials)
        }.getOrElse { throwable ->
            return PlatformQrRefreshResult.Failure(mapSyncError(throwable))
        }

        val enterSucceeded = runCatching {
            executeEnterCommand(normalizedEndpoint, credentials, roomId)
        }.getOrElse { throwable ->
            return PlatformQrRefreshResult.Failure(mapSyncError(throwable))
        }
        if (!enterSucceeded) {
            return PlatformQrRefreshResult.Failure(SyncError.BOT_RESPONSE_INVALID)
        }

        val payload = runCatching {
            pollForQrPayload(normalizedEndpoint, credentials, roomId)
        }.getOrElse { throwable ->
            return PlatformQrRefreshResult.Failure(mapSyncError(throwable))
        } ?: return PlatformQrRefreshResult.Failure(SyncError.BOT_RESPONSE_INVALID)

        val receivedAtMs = currentTimeMillis()
        return PlatformQrRefreshResult.Success(
            qrImageBase64 = payload.qrImageBase64,
            receivedAtMs = receivedAtMs,
            expiresAtMs = payload.expiresAtMs,
        )
    }
}

actual fun formatEpochMillis(value: Long): String {
    val formatter = NSDateFormatter()
    formatter.setDateFormat("dd.MM.yyyy HH:mm")
    return formatter.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = (value.toDouble() / 1000.0) - APPLE_REFERENCE_EPOCH_SECONDS),
    )
}

actual fun formatEpochDate(value: Long): String {
    val formatter = NSDateFormatter()
    formatter.setDateFormat("dd.MM.yyyy")
    return formatter.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = (value.toDouble() / 1000.0) - APPLE_REFERENCE_EPOCH_SECONDS),
    )
}

private data class HttpPayload(
    val statusCode: Int,
    val body: String,
)

private data class QrPayload(
    val qrImageBase64: String,
    val expiresAtMs: Long?,
)

private suspend fun executeJsonRequest(
    url: String,
    method: String,
    credentials: Credentials,
    body: String?,
): HttpPayload {
    val response = when (method) {
        "GET" -> client.get(url) {
            accept(ContentType.Application.Json)
            header("X-Auth-Token", credentials.authToken)
            header("X-User-Id", credentials.userId)
        }

        "POST" -> client.post(url) {
            accept(ContentType.Application.Json)
            header("Content-Type", ContentType.Application.Json.toString())
            header("X-Auth-Token", credentials.authToken)
            header("X-User-Id", credentials.userId)
            if (body != null) {
                setBody(body)
            }
        }

        else -> error("Unsupported method: $method")
    }

    return HttpPayload(
        statusCode = response.status.value,
        body = response.bodyAsText(),
    )
}

private suspend fun createDirectMessageRoom(
    endpoint: String,
    credentials: Credentials,
): String {
    val response = executeJsonRequest(
        url = "$endpoint/dm.create",
        method = "POST",
        credentials = credentials,
        body = json.encodeToString(DmCreateRequest.serializer(), DmCreateRequest(Defaults.botUsername)),
    )
    if (response.statusCode == 401 || response.statusCode == 403) {
        throw SyncFailure(SyncError.UNAUTHORIZED)
    }
    if (response.statusCode !in 200..299) {
        throw SyncFailure(SyncError.BOT_RESPONSE_INVALID)
    }
    val parsed = json.decodeFromString<DmCreateResponse>(response.body)
    val roomId = parsed.room?.rid ?: parsed.room?.id
    if (!parsed.success || roomId.isNullOrBlank()) {
        throw SyncFailure(SyncError.BOT_RESPONSE_INVALID)
    }
    return roomId
}

private suspend fun executeEnterCommand(
    endpoint: String,
    credentials: Credentials,
    roomId: String,
): Boolean {
    val slashResponse = executeJsonRequest(
        url = "$endpoint/commands.run",
        method = "POST",
        credentials = credentials,
        body = json.encodeToString(
            RunCommandRequest.serializer(),
            RunCommandRequest(
                command = "enter",
                roomId = roomId,
                params = "",
            ),
        ),
    )
    if (slashResponse.statusCode == 401 || slashResponse.statusCode == 403) {
        throw SyncFailure(SyncError.UNAUTHORIZED)
    }
    if (slashResponse.statusCode in 200..299) {
        val parsed = runCatching {
            json.decodeFromString(RunCommandResponse.serializer(), slashResponse.body)
        }.getOrNull()
        if (parsed?.success == true) {
            return true
        }
    }
    if (slashResponse.statusCode !in listOf(400, 404, 405)) {
        return false
    }

    val fallbackResponse = executeJsonRequest(
        url = "$endpoint/chat.sendMessage",
        method = "POST",
        credentials = credentials,
        body = json.encodeToString(
            SendMessageRequest.serializer(),
            SendMessageRequest(
                message = SendMessagePayload(rid = roomId, msg = "/enter"),
            ),
        ),
    )
    if (fallbackResponse.statusCode == 401 || fallbackResponse.statusCode == 403) {
        throw SyncFailure(SyncError.UNAUTHORIZED)
    }
    if (fallbackResponse.statusCode !in 200..299) {
        return false
    }
    val parsed = runCatching {
        json.decodeFromString(SendMessageResponse.serializer(), fallbackResponse.body)
    }.getOrNull()
    return parsed?.success == true
}

private suspend fun pollForQrPayload(
    endpoint: String,
    credentials: Credentials,
    roomId: String,
): QrPayload? {
    repeat(POLL_ATTEMPTS) { attempt ->
        val response = executeJsonRequest(
            url = "$endpoint/im.messages?roomId=$roomId&count=30",
            method = "GET",
            credentials = credentials,
            body = null,
        )
        if (response.statusCode == 401 || response.statusCode == 403) {
            throw SyncFailure(SyncError.UNAUTHORIZED)
        }
        if (response.statusCode !in 200..299) {
            throw SyncFailure(SyncError.NETWORK)
        }
        val parsed = runCatching {
            json.decodeFromString(ImMessagesResponse.serializer(), response.body)
        }.getOrNull()
        val payload = parsed?.messages?.firstNotNullOfOrNull(::extractQrPayload)
        if (payload != null) {
            return payload
        }
        if (attempt < POLL_ATTEMPTS - 1) {
            delay(POLL_DELAY_MS)
        }
    }
    return null
}

private fun extractQrPayload(message: MessageDto): QrPayload? {
    if (message.user?.username.orEmpty() != Defaults.botUsername) {
        return null
    }
    val raw = message.message.orEmpty()
    val match = dataUriImageRegex.find(raw) ?: return null
    val dataUri = match.groupValues.getOrNull(1).orEmpty()
    val qrImageBase64 = dataUri.substringAfter("base64,", "")
    if (qrImageBase64.isBlank()) {
        return null
    }
    return QrPayload(
        qrImageBase64 = qrImageBase64,
        expiresAtMs = extractExpirationFromMessage(raw),
    )
}

private fun extractExpirationFromMessage(message: String): Long? {
    val rawDate = expirationRegex.find(message)?.groupValues?.getOrNull(1) ?: return null
    val formatter = NSDateFormatter()
    formatter.setDateFormat("dd.MM.yyyy Z")
    val date = formatter.dateFromString("$rawDate +0300") ?: return null
    return ((date.timeIntervalSinceReferenceDate + APPLE_REFERENCE_EPOCH_SECONDS) * 1000.0).toLong()
}

private fun mapSyncError(throwable: Throwable): SyncError {
    return (throwable as? SyncFailure)?.error ?: SyncError.NETWORK
}

private fun saveOptionalString(
    defaults: NSUserDefaults,
    key: String,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        defaults.removeObjectForKey(key)
    } else {
        defaults.setObject(value, key)
    }
}

private fun sharedDefaults(): NSUserDefaults {
    return NSUserDefaults(suiteName = APP_GROUP_ID)
}

private class SyncFailure(val error: SyncError) : RuntimeException()

@Serializable
private data class MeResponse(
    val success: Boolean? = null,
)

@Serializable
private data class DmCreateRequest(
    val username: String,
)

@Serializable
private data class DmCreateResponse(
    val success: Boolean = false,
    val room: RoomDto? = null,
)

@Serializable
private data class RunCommandRequest(
    val command: String,
    val roomId: String,
    val params: String? = null,
)

@Serializable
private data class RunCommandResponse(
    val success: Boolean = false,
)

@Serializable
private data class RoomDto(
    val rid: String? = null,
    @SerialName("_id") val id: String? = null,
)

@Serializable
private data class SendMessageRequest(
    val message: SendMessagePayload,
)

@Serializable
private data class SendMessagePayload(
    val rid: String,
    val msg: String,
)

@Serializable
private data class SendMessageResponse(
    val success: Boolean = false,
)

@Serializable
private data class ImMessagesResponse(
    val success: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
)

@Serializable
private data class MessageDto(
    @SerialName("_id") val id: String? = null,
    @SerialName("msg") val message: String? = null,
    @SerialName("u") val user: MessageUserDto? = null,
)

@Serializable
private data class MessageUserDto(
    @SerialName("_id") val id: String? = null,
    val username: String? = null,
)
