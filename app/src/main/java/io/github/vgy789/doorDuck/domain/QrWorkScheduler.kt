package io.github.vgy789.doorDuck.domain

import android.content.Context
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
        val delayMs = (SyncPolicy.refreshAtMs(expiresAtMs) - System.currentTimeMillis()).coerceAtLeast(0L)
        enqueueAutomaticRefresh(delayMs = delayMs, attempt = 0)
    }

    fun scheduleAutomaticRetry(retryAtMs: Long, attempt: Int) {
        val delayMs = (retryAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        enqueueAutomaticRefresh(delayMs = delayMs, attempt = attempt)
    }

    fun cancelAutomaticRefresh() {
        workManager.cancelUniqueWork(EXPIRY_WORK_NAME)
    }

    fun cancelAllRefreshWork() {
        workManager.cancelUniqueWork(EXPIRY_WORK_NAME)
        workManager.cancelUniqueWork(MANUAL_WORK_NAME)
        workManager.cancelUniqueWork(WATCHDOG_WORK_NAME)
    }

    fun enqueueAutomaticRefresh(delayMs: Long, attempt: Int) {
        val oneTime = OneTimeWorkRequestBuilder<QrRefreshWorker>()
            .setInputData(
                Data.Builder()
                    .putBoolean(QrRefreshWorker.KEY_REFRESH_ONLY_IF_DUE, true)
                    .putInt(QrRefreshWorker.KEY_AUTO_RETRY_ATTEMPT, attempt)
                    .build(),
            )
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(defaultConstraints())
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
            .build()
        workManager.enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.KEEP,
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
