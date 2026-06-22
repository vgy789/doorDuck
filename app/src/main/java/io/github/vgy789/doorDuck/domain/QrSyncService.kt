package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.config.AndroidEndpointSecrets
import io.github.vgy789.doorDuck.data.QrImageStore
import io.github.vgy789.doorDuck.data.SecureCredentialsStore
import io.github.vgy789.doorDuck.data.SettingsStore
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.RefreshResult
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.network.DmCreateRequest
import io.github.vgy789.doorDuck.network.MessageDto
import io.github.vgy789.doorDuck.network.RocketChatApi
import io.github.vgy789.doorDuck.network.RocketChatClientFactory
import io.github.vgy789.doorDuck.network.RunCommandRequest
import io.github.vgy789.doorDuck.network.SendMessagePayload
import io.github.vgy789.doorDuck.network.SendMessageRequest
import io.github.vgy789.doorDuck.network.UserDto
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import kotlinx.coroutines.delay
import io.github.vgy789.doorDuck.platform.isValidQrImageBase64
import retrofit2.HttpException

class QrSyncService(
    private val settingsStore: SettingsStore,
    private val credentialsStore: SecureCredentialsStore,
    private val clientFactory: RocketChatClientFactory,
    private val imageStore: QrImageStore,
    private val workScheduler: QrWorkScheduler,
    private val notificationManager: SyncNotificationManager,
) {
    suspend fun verifyCredentialsAndBotAccess(
        endpoint: String,
        credentials: Credentials,
    ): ConnectionCheckResult {
        val meResult = runCatching {
            val api = clientFactory.create(endpoint, credentials)
            api.getMe()
        }
        val me = meResult.getOrElse { throwable ->
            return ConnectionCheckErrorMapper.map(throwable)
        }
        if (me.success == false) {
            return ConnectionCheckResult.UNAUTHORIZED
        }

        val dmResult = runCatching {
            val api = clientFactory.create(endpoint, credentials)
            val botUsername = resolveBotUsername(api) ?: return@runCatching ConnectionCheckResult.BOT_NOT_FOUND
            val dm = api.createDirectMessage(DmCreateRequest(botUsername))
            val roomId = dm.room?.rid ?: dm.room?.id
            if (!dm.success || roomId.isNullOrBlank()) {
                return@runCatching ConnectionCheckResult.BOT_UNAVAILABLE
            }
            ConnectionCheckResult.SUCCESS
        }

        return dmResult.getOrElse { throwable ->
            val http = throwable as? HttpException
            if (http != null) {
                return when (http.code()) {
                    400, 403, 404 -> ConnectionCheckResult.BOT_UNAVAILABLE
                    401 -> ConnectionCheckResult.UNAUTHORIZED
                    else -> ConnectionCheckResult.UNKNOWN
                }
            }
            return when (throwable) {
                is IOException -> ConnectionCheckResult.NETWORK_ERROR
                else -> ConnectionCheckResult.UNKNOWN
            }
        }
    }

    suspend fun refreshQrCode(): RefreshResult {
        val settings = settingsStore.getSettings()
        val credentials = credentialsStore.load() ?: return notConfigured()

        settingsStore.setSyncInProgress(true)

        return runCatching {
            val api = clientFactory.create(settings.endpoint, credentials)
            val roomId = createDm(api)
            executeEnterCommand(api, roomId)

            val pollResult = pollForQrPayload(api, roomId)
            when (pollResult) {
                is PollResult.RateLimited -> {
                    return RefreshResult.Failure(
                        error = SyncError.RATE_LIMITED,
                        retryAfterMs = pollResult.retryAfterMs,
                    )
                }
                PollResult.NoQrYet -> throw SyncException(SyncError.BOT_RESPONSE_INVALID)
                is PollResult.Success -> {
                    val payload = pollResult.payload
                    if (!isValidQrImageBase64(payload.base64)) {
                        throw SyncException(SyncError.IMAGE_DOWNLOAD_FAILED)
                    }
                    val bytes = decodeBase64(payload.base64)
                    if (bytes.isEmpty()) {
                        throw SyncException(SyncError.IMAGE_DOWNLOAD_FAILED)
                    }

                    val previousSnapshot = settingsStore.getSnapshot()
                    val candidatePath = imageStore.stageCandidate(bytes)
                    val receivedAt = System.currentTimeMillis()
                    val autoRefreshAllowed = settings.autoRefreshEnabled && !settings.endpoint.isIntensiveEndpoint()
                    val nextAutoRefreshAt = payload.expiresAtMs
                        ?.takeIf { autoRefreshAllowed }
                        ?.let(SyncPolicy::refreshAtMs)
                    try {
                        settingsStore.saveSyncSuccess(
                            path = candidatePath,
                            receivedAtMs = receivedAt,
                            expiresAtMs = payload.expiresAtMs,
                            nextAutoRefreshAtMs = nextAutoRefreshAt,
                            imageValidationStatus = QrImageValidationStatus.VALID,
                        )
                    } catch (throwable: Throwable) {
                        imageStore.deleteIfExists(candidatePath)
                        throw throwable
                    }
                    imageStore.deleteIfExists(previousSnapshot.localImagePath)
                    imageStore.deleteAllExcept(candidatePath)
                    if (autoRefreshAllowed && payload.expiresAtMs != null) {
                        workScheduler.scheduleRefreshAtExpiration(payload.expiresAtMs)
                    } else {
                        workScheduler.cancelAutomaticRefresh()
                    }
                    RefreshResult.Success(settingsStore.getSnapshot())
                }
            }
        }.getOrElse { throwable ->
            val mapped = mapSyncError(throwable)
            if (mapped == SyncError.UNAUTHORIZED) {
                settingsStore.setNextAutoRefreshAt(null)
                workScheduler.cancelAutomaticRefresh()
                notificationManager.showUnauthorizedNotification()
            }
            settingsStore.saveSyncError(mapped)
            RefreshResult.Failure(mapped)
        }.also {
            settingsStore.clearInProgress()
        }
    }

    private suspend fun notConfigured(): RefreshResult {
        settingsStore.saveSyncError(SyncError.NOT_CONFIGURED)
        return RefreshResult.NotConfigured
    }

    private suspend fun createDm(api: RocketChatApi): String {
        val botUsername = resolveBotUsername(api) ?: throw SyncException(SyncError.BOT_NOT_FOUND)
        val response = api.createDirectMessage(DmCreateRequest(botUsername))
        if (!response.success) {
            throw SyncException(SyncError.BOT_RESPONSE_INVALID)
        }
        return response.room?.rid
            ?: response.room?.id
            ?: throw SyncException(SyncError.BOT_RESPONSE_INVALID)
    }

    private suspend fun executeEnterCommand(api: RocketChatApi, roomId: String) {
        val slashAttempt = runCatching {
            api.runSlashCommand(
                RunCommandRequest(
                    command = "enter",
                    roomId = roomId,
                    params = "",
                ),
            )
        }

        val slashSucceeded = slashAttempt.getOrNull()?.success == true
        if (slashSucceeded) return

        val slashError = slashAttempt.exceptionOrNull()
        if (slashError != null && !shouldFallbackToSendMessage(slashError)) {
            throw slashError
        }

        val fallbackResponse = api.sendMessage(
            SendMessageRequest(
                message = SendMessagePayload(rid = roomId, msg = "/enter"),
            ),
        )
        if (!fallbackResponse.success) {
            throw SyncException(SyncError.BOT_RESPONSE_INVALID)
        }
    }

    private fun shouldFallbackToSendMessage(throwable: Throwable): Boolean {
        val http = throwable as? HttpException ?: return false
        return http.code() == 400 || http.code() == 404 || http.code() == 405
    }

    private suspend fun pollForQrPayload(
        api: RocketChatApi,
        roomId: String,
    ): PollResult {
        repeat(POLL_ATTEMPTS) { attempt ->
            val messages = api.getMessages(roomId = roomId, count = 30).messages
            val found = extractPollResult(messages)
            if (found != null) return found
            if (attempt < POLL_ATTEMPTS - 1) {
                delay(POLL_DELAY_MS)
            }
        }
        return PollResult.NoQrYet
    }

    private fun extractPollResult(messages: List<MessageDto>): PollResult? {
        return messages.firstNotNullOfOrNull { message ->
            if (message.u?.username.orEmpty() != Defaults.botUsername) return@firstNotNullOfOrNull null

            val inlineBase64 = extractBase64FromMessage(message.msg)
            if (inlineBase64 != null) {
                return@firstNotNullOfOrNull PollResult.Success(
                    QrPayload(
                        base64 = inlineBase64,
                        expiresAtMs = extractExpirationFromMessage(message),
                    ),
                )
            }

            val retryAfterMs = extractRetryAfterMs(message.msg)
            if (retryAfterMs != null) {
                return@firstNotNullOfOrNull PollResult.RateLimited(retryAfterMs)
            }
            null
        }
    }

    private fun extractExpirationFromMessage(message: MessageDto): Long? {
        val textSources = listOfNotNull(message.msg)

        for (text in textSources) {
            val match = expirationRegex.find(text) ?: continue
            val rawDate = match.groupValues.getOrNull(1) ?: continue
            val parsedDate = runCatching { LocalDate.parse(rawDate, expirationFormatter) }.getOrNull() ?: continue
            return parsedDate
                .atStartOfDay(ZoneOffset.ofHours(3))
                .toInstant()
                .toEpochMilli()
        }
        return null
    }

    private fun extractBase64FromMessage(msg: String?): String? {
        if (msg.isNullOrBlank()) return null
        val match = dataUriImageRegex.find(msg) ?: return null
        val dataUri = match.groupValues.getOrNull(1).orEmpty()
        return dataUri.substringAfter("base64,", "").takeIf { it.isNotBlank() }
    }

    private fun extractRetryAfterMs(msg: String?): Long? {
        if (msg.isNullOrBlank()) return null
        val seconds = rateLimitRegex.find(msg)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        return seconds.coerceAtLeast(0L) * 1000L
    }

    private fun decodeBase64(base64: String): ByteArray {
        if (base64.isBlank()) return ByteArray(0)
        return runCatching { Base64.getDecoder().decode(base64) }.getOrDefault(ByteArray(0))
    }

    private fun mapSyncError(throwable: Throwable): SyncError {
        val syncException = throwable as? SyncException
        if (syncException != null) return syncException.error

        val http = throwable as? HttpException
        if (http != null) {
            return if (http.code() == 401 || http.code() == 403) {
                SyncError.UNAUTHORIZED
            } else {
                SyncError.NETWORK
            }
        }

        return when (throwable) {
            is IOException -> SyncError.NETWORK
            else -> SyncError.UNKNOWN
        }
    }

    private class SyncException(val error: SyncError) : RuntimeException()

    private data class QrPayload(
        val base64: String,
        val expiresAtMs: Long?,
    )

    private sealed interface PollResult {
        data class Success(val payload: QrPayload) : PollResult
        data class RateLimited(val retryAfterMs: Long) : PollResult
        data object NoQrYet : PollResult
    }

    private companion object {
        const val POLL_ATTEMPTS = 6
        const val POLL_DELAY_MS = 2_000L
        val botUsernameRegex = Regex("(qr-code|code-generator|generator)", RegexOption.IGNORE_CASE)
        val expirationRegex = Regex("(?i)expire\\s+on\\s+(\\d{2}\\.\\d{2}\\.\\d{4})")
        val rateLimitRegex = Regex("(?i)please\\s+wait\\s+(\\d+)\\s+seconds?.*again")
        val dataUriImageRegex = Regex("!\\[[^\\]]*\\]\\((data:image/[^;]+;base64,[^)]+)\\)")
        val expirationFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US)
    }

    private suspend fun resolveBotUsername(api: RocketChatApi): String? {
        val exactAttempt = runCatching {
            api.createDirectMessage(DmCreateRequest(Defaults.botUsername))
        }
        val exactResponse = exactAttempt.getOrNull()
        if (exactResponse?.success == true && !(exactResponse.room?.rid ?: exactResponse.room?.id).isNullOrBlank()) {
            return Defaults.botUsername
        }
        val exactHttp = exactAttempt.exceptionOrNull() as? HttpException
        if (exactHttp != null && exactHttp.code() == 401) {
            throw exactHttp
        }
        if (exactHttp != null && exactHttp.code() !in listOf(400, 403, 404)) {
            throw exactHttp
        }

        val query = """{"type":"bot","username":{"${'$'}regex":"${botUsernameRegex.pattern}","${'$'}options":"i"}}"""
        val candidates = api.getUsers(query = query).users
            .filter { it.type == "bot" && !it.username.isNullOrBlank() }
        return selectBotCandidate(candidates)
    }

    private fun selectBotCandidate(candidates: List<UserDto>): String? {
        val usernames = candidates.mapNotNull { it.username }.distinct()
        if (usernames.isEmpty()) return null
        usernames.firstOrNull { it == Defaults.botUsername }?.let { return it }
        usernames.firstOrNull { it.contains("qr-code-generator", ignoreCase = true) }?.let { return it }
        usernames.firstOrNull { it.contains("code-generator", ignoreCase = true) }?.let { return it }
        usernames.firstOrNull { it.contains("qr", ignoreCase = true) && it.contains("generator", ignoreCase = true) }?.let { return it }
        return if (usernames.size == 1) usernames.first() else null
    }
}

private fun String.isIntensiveEndpoint(): Boolean {
    return AndroidEndpointSecrets.isIntensiveEndpoint(this)
}
