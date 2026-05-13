package io.github.vgy789.doorDuck.domain

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.vgy789.doorDuck.worker.QrRefreshWorker
import java.util.concurrent.TimeUnit

class QrWorkScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleRefreshAtExpiration(expiresAtMs: Long) {
        val nowMs = System.currentTimeMillis()
        val delayMs = (SyncPolicy.refreshAtMs(expiresAtMs) - nowMs).coerceAtLeast(0L)
        val oneTime = OneTimeWorkRequestBuilder<QrRefreshWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            EXPIRY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTime,
        )
    }

    fun enqueueManualRefresh(showToastOnResult: Boolean = false) {
        val oneTime = OneTimeWorkRequestBuilder<QrRefreshWorker>()
            .setInputData(
                Data.Builder()
                    .putBoolean(QrRefreshWorker.KEY_SHOW_RESULT_TOAST, showToastOnResult)
                    .build(),
            )
            .setConstraints(defaultConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTime,
        )
    }

    fun ensureReliabilityWatchdog() {
        val periodic = PeriodicWorkRequestBuilder<QrRefreshWorker>(24, TimeUnit.HOURS)
            .setInputData(
                Data.Builder()
                    .putBoolean(QrRefreshWorker.KEY_REFRESH_ONLY_IF_DUE, true)
                    .build(),
            )
            .setConstraints(defaultConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WATCHDOG_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
    }

    private fun defaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    companion object {
        const val EXPIRY_WORK_NAME = "door_duck_expiry_refresh"
        const val MANUAL_WORK_NAME = "door_duck_manual_refresh"
        const val WATCHDOG_WORK_NAME = "door_duck_reliability_watchdog"
    }
}
