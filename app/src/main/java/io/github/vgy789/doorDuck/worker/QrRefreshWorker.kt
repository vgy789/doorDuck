package io.github.vgy789.doorDuck.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.RefreshResult

class QrRefreshWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val container = DoorDuckApp.container(applicationContext)
        if (inputData.getBoolean(KEY_REFRESH_ONLY_IF_DUE, false)) {
            val settings = container.settingsStore.getSettings()
            val snapshot = container.settingsStore.getSnapshot()
            val shouldRefresh = SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = settings.autoRefreshEnabled,
                localImagePath = snapshot.localImagePath,
                expiresAtMs = snapshot.expiresAtMs,
                nextAutoRefreshAtMs = snapshot.nextAutoRefreshAtMs,
            )
            if (!shouldRefresh) {
                container.widgetUpdateCoordinator.forceWidgetUpdateNow()
                return Result.success()
            }
        }

        val result = container.syncService.refreshQrCode()
        container.widgetUpdateCoordinator.forceWidgetUpdateNow()
        if (inputData.getBoolean(KEY_SHOW_RESULT_TOAST, false) && runAttemptCount == 0) {
            showResultToast(result)
        }
        return when (result) {
            is RefreshResult.Success -> Result.success()
            is RefreshResult.NotConfigured -> Result.failure()
            is RefreshResult.Failure -> {
                val isAutomaticAttempt = inputData.getBoolean(KEY_REFRESH_ONLY_IF_DUE, false)
                val autoRefreshEnabled = container.settingsStore.getSettings().autoRefreshEnabled
                val attempt = if (isAutomaticAttempt) {
                    inputData.getInt(KEY_AUTO_RETRY_ATTEMPT, 0)
                } else {
                    0
                }
                val shouldPersistCooldown = result.retryAfterMs != null
                if (shouldPersistCooldown) {
                    val retryDelayMs = SyncPolicy.nextRetryDelayMs(
                        attempt = attempt,
                        minDelayMs = result.retryAfterMs ?: 0L,
                    )
                    val retryAtMs = System.currentTimeMillis() + retryDelayMs
                    container.settingsStore.setNextAutoRefreshAt(retryAtMs)
                    if (autoRefreshEnabled) {
                        container.workScheduler.scheduleAutomaticRetry(
                            retryAtMs = retryAtMs,
                            attempt = attempt + 1,
                        )
                    }
                    Result.success()
                } else if (isAutomaticAttempt && autoRefreshEnabled) {
                    val retryDelayMs = SyncPolicy.nextRetryDelayMs(
                        attempt = attempt,
                        minDelayMs = 0L,
                    )
                    val retryAtMs = System.currentTimeMillis() + retryDelayMs
                    container.settingsStore.setNextAutoRefreshAt(retryAtMs)
                    container.workScheduler.scheduleAutomaticRetry(
                        retryAtMs = retryAtMs,
                        attempt = attempt + 1,
                    )
                    Result.success()
                } else {
                    Result.failure()
                }
            }
        }
    }

    private fun showResultToast(result: RefreshResult) {
        val textRes = when (result) {
            is RefreshResult.Success -> R.string.toast_qr_refreshed
            is RefreshResult.NotConfigured -> R.string.toast_qr_refresh_failed
            is RefreshResult.Failure -> R.string.toast_qr_refresh_failed
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, applicationContext.getString(textRes), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val KEY_SHOW_RESULT_TOAST = "show_result_toast"
        const val KEY_REFRESH_ONLY_IF_DUE = "refresh_only_if_due"
        const val KEY_AUTO_RETRY_ATTEMPT = "auto_retry_attempt"
    }
}
