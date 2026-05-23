package io.github.vgy789.doorDuck

import io.github.vgy789.doorDuck.domain.ConnectionCheckErrorMapper
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.ui.InputSanitizer
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
        assertEquals("https://example.com/api/v1/", endpoint)
    }

    @Test
    fun wizard_state_machine_moves_forward_to_done() {
        var step = WizardStep.WELCOME
        step = WizardStateMachine.next(step)
        step = WizardStateMachine.next(step)
        step = WizardStateMachine.next(step)
        step = WizardStateMachine.next(step)
        assertEquals(WizardStep.DONE, step)
    }

    @Test
    fun wizard_state_machine_moves_backwards() {
        var step = WizardStep.CHECK_CONNECTION
        step = WizardStateMachine.previous(step)
        step = WizardStateMachine.previous(step)
        assertEquals(WizardStep.USER_ID, step)
    }

    @Test
    fun wizard_cannot_finish_without_connection_check() {
        val canProceed = WizardStateMachine.canProceed(
            step = WizardStep.CHECK_CONNECTION,
            userId = "uid",
            token = "token",
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
                nowMs = 1_500L,
            ),
        )

        assertTrue(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 2_000L,
                nowMs = 2_000L,
            ),
        )

        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = false,
                localImagePath = "/tmp/qr.png",
                expiresAtMs = 1_000L,
                nextAutoRefreshAtMs = 1_000L,
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
    fun auto_retry_slots_are_anchored_to_expire_time() {
        val expireAtMs = 1_000L

        assertEquals(
            expireAtMs + 1L * 60L * 60L * 1000L,
            SyncPolicy.nextRetryAtMs(
                expiresAtMs = expireAtMs,
                attempt = 0,
                nowMs = expireAtMs + 10_000L,
            ),
        )

        assertEquals(
            expireAtMs + 3L * 60L * 60L * 1000L,
            SyncPolicy.nextRetryAtMs(
                expiresAtMs = expireAtMs,
                attempt = 1,
                nowMs = expireAtMs + 70L * 60L * 1000L,
            ),
        )
    }

    @Test
    fun auto_retry_respects_bot_cooldown_if_it_pushes_later_than_slot() {
        val expireAtMs = 1_000L
        val nowMs = expireAtMs + 50L * 60L * 1000L
        val cooldownMs = 20L * 60L * 1000L

        assertEquals(
            nowMs + cooldownMs,
            SyncPolicy.nextRetryAtMs(
                expiresAtMs = expireAtMs,
                attempt = 0,
                nowMs = nowMs,
                minDelayMs = cooldownMs,
            ),
        )
    }
}
