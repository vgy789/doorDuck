package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.platform.currentTimeMillis

object SyncPolicy {
    fun refreshAtMs(expiresAtMs: Long): Long {
        return expiresAtMs
    }

    fun nextRetryDelayMs(
        attempt: Int,
        minDelayMs: Long = 0L,
    ): Long {
        val scheduleMs = when (attempt.coerceAtLeast(0)) {
            0 -> 60L * 60L * 1000L
            1 -> 2L * 60L * 60L * 1000L
            2 -> 3L * 60L * 60L * 1000L
            3 -> 6L * 60L * 60L * 1000L
            4 -> 12L * 60L * 60L * 1000L
            else -> 24L * 60L * 60L * 1000L
        }
        return maxOf(scheduleMs, minDelayMs.coerceAtLeast(0L))
    }

    fun shouldRefreshNow(
        autoRefreshEnabled: Boolean,
        localImagePath: String?,
        expiresAtMs: Long?,
        nextAutoRefreshAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        if (!autoRefreshEnabled) return false
        if (localImagePath.isNullOrBlank()) return false
        if (expiresAtMs == null) return false
        if (nowMs < refreshAtMs(expiresAtMs)) return false
        if (nextAutoRefreshAtMs != null && nowMs < nextAutoRefreshAtMs) return false
        return true
    }

    fun isExpired(
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return expiresAtMs != null && nowMs >= expiresAtMs
    }
}
