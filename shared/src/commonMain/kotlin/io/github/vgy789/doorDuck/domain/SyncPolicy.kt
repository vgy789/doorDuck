package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.platform.currentTimeMillis

object SyncPolicy {
    private const val dayInMs = 24L * 60L * 60L * 1000L
    private const val refreshLeadTimeMs = dayInMs

    fun expiresAtMs(receivedAtMs: Long): Long {
        return receivedAtMs + Defaults.qrValidityDays * dayInMs
    }

    fun refreshAtMs(expiresAtMs: Long): Long {
        return (expiresAtMs - refreshLeadTimeMs).coerceAtLeast(0L)
    }

    fun shouldRefreshNow(
        localImagePath: String?,
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        if (localImagePath.isNullOrBlank()) return true
        if (expiresAtMs == null) return true
        return nowMs >= refreshAtMs(expiresAtMs)
    }

    fun isExpired(
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return expiresAtMs != null && nowMs >= expiresAtMs
    }
}
