package io.github.vgy789.doorDuck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorDuckBehaviorTest {
    @Test
    fun defaultValuesPreserveExistingTiming() {
        val behavior = DoorDuckBehavior.Default

        assertEquals(0L, behavior.refreshBeforeExpirationMs)
        assertEquals(
            listOf(1L, 2L, 3L, 6L, 12L, 24L).map { it * 60L * 60L * 1_000L },
            behavior.automaticRetryDelaysMs,
        )
        assertEquals(5_000L, behavior.manualRefreshCooldownMs)
        assertEquals(60_000L, behavior.widgetRevealDurationMs)
        assertEquals(5L * 60L * 1_000L, behavior.syncInProgressTimeoutMs)
    }

    @Test
    fun policyCanBeExploredWithoutChangingGlobalState() {
        val behavior = DoorDuckBehavior(
            refreshBeforeExpirationMs = 10_000L,
            automaticRetryDelaysMs = listOf(1_000L, 2_000L),
        )

        assertEquals(90_000L, SyncPolicy.refreshAtMs(100_000L, behavior))
        assertEquals(2_000L, SyncPolicy.nextRetryDelayMs(attempt = 8, behavior = behavior))
        assertFalse(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "qr.png",
                expiresAtMs = 100_000L,
                nextAutoRefreshAtMs = null,
                nowMs = 89_999L,
                behavior = behavior,
            ),
        )
        assertTrue(
            SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = true,
                localImagePath = "qr.png",
                expiresAtMs = 100_000L,
                nextAutoRefreshAtMs = null,
                nowMs = 90_000L,
                behavior = behavior,
            ),
        )
    }

    @Test
    fun invalidTimingIsRejectedNearItsDefinition() {
        assertThrows(IllegalArgumentException::class.java) {
            DoorDuckBehavior(automaticRetryDelaysMs = listOf(2_000L, 1_000L))
        }
    }
}
