package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.platform.currentTimeMillis
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.QrReadiness
import io.github.vgy789.doorDuck.model.SyncError

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

    fun nextRetryAtMs(
        attempt: Int,
        nowMs: Long = currentTimeMillis(),
        minDelayMs: Long = 0L,
    ): Long {
        return nowMs + nextRetryDelayMs(attempt = attempt, minDelayMs = minDelayMs)
    }

    fun shouldRefreshNow(
        autoRefreshEnabled: Boolean,
        localImagePath: String?,
        expiresAtMs: Long?,
        nextAutoRefreshAtMs: Long?,
        lastError: SyncError? = null,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        if (!autoRefreshEnabled) return false
        if (lastError == SyncError.UNAUTHORIZED) return false
        if (localImagePath.isNullOrBlank()) return false
        if (expiresAtMs == null) return false
        if (nowMs < refreshAtMs(expiresAtMs)) return false
        if (nextAutoRefreshAtMs != null && nowMs < nextAutoRefreshAtMs) return false
        return true
    }

    fun readiness(
        hasImage: Boolean,
        validationStatus: QrImageValidationStatus,
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): QrReadiness {
        if (!hasImage || validationStatus == QrImageValidationStatus.INVALID) {
            return QrReadiness.MISSING_OR_INVALID
        }
        if (validationStatus == QrImageValidationStatus.UNKNOWN || expiresAtMs == null) {
            return QrReadiness.CHECK_REQUIRED
        }
        return if (nowMs >= expiresAtMs) QrReadiness.EXPIRED else QrReadiness.READY
    }

    fun isExpired(
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return expiresAtMs != null && nowMs >= expiresAtMs
    }

    fun isManualRefreshBlocked(
        blockedUntilMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return blockedUntilMs != null && nowMs < blockedUntilMs
    }

    fun nextManualRefreshAllowedAt(
        nowMs: Long = currentTimeMillis(),
    ): Long {
        return nowMs + Defaults.manualRefreshCooldownMillis
    }

    fun isSyncInProgress(
        storedInProgress: Boolean,
        startedAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        if (!storedInProgress || startedAtMs == null) return false
        val elapsedMs = nowMs - startedAtMs
        return elapsedMs in 0 until Defaults.syncInProgressTimeoutMillis
    }
}
