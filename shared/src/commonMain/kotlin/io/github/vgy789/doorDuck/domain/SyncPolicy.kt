package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.platform.currentTimeMillis
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.QrReadiness
import io.github.vgy789.doorDuck.model.SyncError

object SyncPolicy {
    fun refreshAtMs(
        expiresAtMs: Long,
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Long {
        return (expiresAtMs - behavior.refreshBeforeExpirationMs).coerceAtLeast(0L)
    }

    fun nextRetryDelayMs(
        attempt: Int,
        minDelayMs: Long = 0L,
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Long {
        val schedule = behavior.automaticRetryDelaysMs
        val scheduleMs = schedule.getOrElse(attempt.coerceAtLeast(0)) { schedule.last() }
        return maxOf(scheduleMs, minDelayMs.coerceAtLeast(0L))
    }

    fun nextRetryAtMs(
        attempt: Int,
        nowMs: Long = currentTimeMillis(),
        minDelayMs: Long = 0L,
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Long {
        return nowMs + nextRetryDelayMs(
            attempt = attempt,
            minDelayMs = minDelayMs,
            behavior = behavior,
        )
    }

    fun shouldRefreshNow(
        autoRefreshEnabled: Boolean,
        localImagePath: String?,
        expiresAtMs: Long?,
        nextAutoRefreshAtMs: Long?,
        lastError: SyncError? = null,
        nowMs: Long = currentTimeMillis(),
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Boolean {
        if (!autoRefreshEnabled) return false
        if (lastError == SyncError.UNAUTHORIZED) return false
        if (localImagePath.isNullOrBlank()) return false
        if (expiresAtMs == null) return false
        if (nowMs < refreshAtMs(expiresAtMs, behavior)) return false
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

    fun shouldDisplayQr(
        hasImage: Boolean,
        validationStatus: QrImageValidationStatus,
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return when (readiness(hasImage, validationStatus, expiresAtMs, nowMs)) {
            QrReadiness.READY,
            QrReadiness.CHECK_REQUIRED,
            -> true
            QrReadiness.EXPIRED,
            QrReadiness.MISSING_OR_INVALID,
            -> false
        }
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
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Long {
        return nowMs + behavior.manualRefreshCooldownMs
    }

    fun widgetRevealUntil(
        nowMs: Long = currentTimeMillis(),
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Long {
        return nowMs + behavior.widgetRevealDurationMs
    }

    fun isSyncInProgress(
        storedInProgress: Boolean,
        startedAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
        behavior: DoorDuckBehavior = DoorDuckBehavior.Default,
    ): Boolean {
        if (!storedInProgress || startedAtMs == null) return false
        val elapsedMs = nowMs - startedAtMs
        return elapsedMs in 0 until behavior.syncInProgressTimeoutMs
    }
}
