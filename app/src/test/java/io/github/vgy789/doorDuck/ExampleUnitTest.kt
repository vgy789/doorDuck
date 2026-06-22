package io.github.vgy789.doorDuck

import io.github.vgy789.doorDuck.domain.ConnectionCheckErrorMapper
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.QrReadiness
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.ui.InputSanitizer
import io.github.vgy789.doorDuck.ui.RocketCredentialsExtractor
import io.github.vgy789.doorDuck.ui.WizardStateMachine
import io.github.vgy789.doorDuck.ui.WizardStep
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

import org.junit.Assert.*
import retrofit2.HttpException
import retrofit2.Response

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun sanitizer_removesWhitespaceEverywhere() {
        assertEquals("abc123", InputSanitizer.noWhitespace("  a b c \n 1 2 3  "))
    }

    @Test
    fun endpoint_sanitizer_removesSpaces_keepsTrailingSlash() {
        val endpoint = InputSanitizer.endpoint(" https://example.com/api/v1/ ")
        assertEquals("https://example.com/api/v1", endpoint)
    }

    @Test
    fun tokens_page_sanitizer_uses_selected_endpoint() {
        val url = InputSanitizer.tokensPageUrl(" https://example.com/api/v1/ ")
        assertEquals("https://example.com/account/tokens", url)
    }

    @Test
    fun wizard_state_machine_moves_forward_to_done() {
        val step = WizardStateMachine.next(WizardStep.CREDENTIALS)
        assertEquals(WizardStep.DONE, step)
    }

    @Test
    fun wizard_state_machine_moves_backwards() {
        val step = WizardStateMachine.previous(WizardStep.DONE)
        assertEquals(WizardStep.CREDENTIALS, step)
    }

    @Test
    fun extractor_finds_token_and_user_id_in_popup_text() {
        val extracted = RocketCredentialsExtractor.extract(
            """
            Идентификатор персонального доступа успешно сгенерирован
            Токен: pQsvRJbAjqIzdELSbFmFpkptIqotQItZdZ8VnDWi2pL
            Ваш Id пользователя: it8XJYMhb7cpcJdLx
            """.trimIndent(),
        )

        assertEquals("pQsvRJbAjqIzdELSbFmFpkptIqotQItZdZ8VnDWi2pL", extracted.authToken)
        assertEquals("it8XJYMhb7cpcJdLx", extracted.userId)
    }

    @Test
    fun wizard_requires_extracted_credentials() {
        val canProceed = WizardStateMachine.canProceed(
            step = WizardStep.CREDENTIALS,
            userId = "",
            token = "",
            connectionCheckPassed = false,
        )
        assertFalse(canProceed)
    }

    @Test
    fun connection_check_mapper_maps_unauthorized() {
        val response = Response.error<String>(401, "unauthorized".toResponseBody("text/plain".toMediaType()))
        val result = ConnectionCheckErrorMapper.map(HttpException(response))
        assertEquals(ConnectionCheckResult.UNAUTHORIZED, result)
    }

    @Test
    fun connection_check_mapper_maps_network() {
        val result = ConnectionCheckErrorMapper.map(IOException("offline"))
        assertEquals(ConnectionCheckResult.NETWORK_ERROR, result)
    }

    @Test
    fun auto_refresh_waits_for_explicit_expire_and_retry_window() {
        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = null,
                nextAutoRefreshAtMs = null,
                nowMs = 1_000L,
            ),
        )

        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 2_000L,
                lastError = null,
                nowMs = 1_500L,
            ),
        )

        assertTrue(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 2_000L,
                lastError = null,
                nowMs = 2_000L,
            ),
        )

        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = false,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 1_000L,
                lastError = null,
                nowMs = 2_000L,
            ),
        )
    }

    @Test
    fun manual_refresh_cooldown_blocks_repeated_taps() {
        assertTrue(SyncPolicy.isManualRefreshBlocked(blockedUntilMs = 5_000L, nowMs = 4_999L))
        assertFalse(SyncPolicy.isManualRefreshBlocked(blockedUntilMs = 5_000L, nowMs = 5_000L))
        assertEquals(6_000L, SyncPolicy.nextManualRefreshAllowedAt(nowMs = 1_000L))
    }

    @Test
    fun auto_retry_slots_are_relative_to_now() {
        assertEquals(
            10_000L + 1L * 60L * 60L * 1000L,
            SyncPolicy.nextRetryAtMs(
                attempt = 0,
                nowMs = 10_000L,
            ),
        )

        assertEquals(
            20_000L + 2L * 60L * 60L * 1000L,
            SyncPolicy.nextRetryAtMs(
                attempt = 1,
                nowMs = 20_000L,
            ),
        )
    }

    @Test
    fun auto_retry_respects_bot_cooldown_if_it_pushes_later_than_slot() {
        val nowMs = 50L * 60L * 1000L
        val cooldownMs = 20L * 60L * 1000L

        assertEquals(
            nowMs + 60L * 60L * 1000L,
            SyncPolicy.nextRetryAtMs(
                attempt = 0,
                nowMs = nowMs,
                minDelayMs = cooldownMs,
            ),
        )
    }

    @Test
    fun unauthorized_blocks_automatic_refresh() {
        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 1_000L,
                lastError = SyncError.UNAUTHORIZED,
                nowMs = 2_000L,
            ),
        )
    }

    @Test
    fun readiness_covers_all_states() {
        assertEquals(
            QrReadiness.MISSING_OR_INVALID,
            SyncPolicy.readiness(false, QrImageValidationStatus.UNKNOWN, expiresAtMs = null, nowMs = 0L),
        )
        assertEquals(
            QrReadiness.CHECK_REQUIRED,
            SyncPolicy.readiness(true, QrImageValidationStatus.UNKNOWN, expiresAtMs = null, nowMs = 0L),
        )
        assertEquals(
            QrReadiness.READY,
            SyncPolicy.readiness(true, QrImageValidationStatus.VALID, expiresAtMs = 2_000L, nowMs = 1_000L),
        )
        assertEquals(
            QrReadiness.EXPIRED,
            SyncPolicy.readiness(true, QrImageValidationStatus.VALID, expiresAtMs = 2_000L, nowMs = 2_000L),
        )
        assertEquals(
            QrReadiness.MISSING_OR_INVALID,
            SyncPolicy.readiness(true, QrImageValidationStatus.INVALID, expiresAtMs = 2_000L, nowMs = 1_000L),
        )
    }
}
