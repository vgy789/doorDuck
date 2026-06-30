package io.github.vgy789.doorDuck.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.config.AndroidEndpointSecrets
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
            if (settings.endpoint.isIntensiveEndpoint()) {
                container.settingsStore.setAutoRefreshEnabled(false)
                container.settingsStore.setNextAutoRefreshAt(null)
                container.workScheduler.cancelAutomaticRefresh()
                container.widgetUpdateCoordinator.forceWidgetUpdateNow()
                return Result.success()
            }
            val snapshot = container.settingsStore.getSnapshot()
            val shouldRefresh = SyncPolicy.shouldRefreshNow(
                autoRefreshEnabled = settings.autoRefreshEnabled,
                localImagePath = snapshot.localImagePath,
                expiresAtMs = snapshot.expiresAtMs,
                nextAutoRefreshAtMs = snapshot.nextAutoRefreshAtMs,
                lastError = snapshot.lastError,
            )
            if (!shouldRefresh) {
                container.widgetUpdateCoordinator.forceWidgetUpdateNow()
                return Result.success()
            }
        }

        if (!container.settingsStore.tryStartSync()) {
            container.widgetUpdateCoordinator.forceWidgetUpdateNow()
            return Result.success()
        }
        val result = try {
            container.syncService.refreshQrCode()
        } finally {
            container.settingsStore.clearInProgress()
        }
        container.widgetUpdateCoordinator.forceWidgetUpdateNow()
        if (inputData.getBoolean(KEY_SHOW_RESULT_TOAST, false) && runAttemptCount == 0) {
            showResultToast(result)
        }
        return when (result) {
            is RefreshResult.Success -> Result.success()
            is RefreshResult.NotConfigured -> Result.failure()
            is RefreshResult.Failure -> {
                val isAutomaticAttempt = inputData.getBoolean(KEY_REFRESH_ONLY_IF_DUE, false)
                val settings = container.settingsStore.getSettings()
                val autoRefreshEnabled = settings.autoRefreshEnabled && !settings.endpoint.isIntensiveEndpoint()
                val snapshot = container.settingsStore.getSnapshot()
                val attempt = if (isAutomaticAttempt) {
                    inputData.getInt(KEY_AUTO_RETRY_ATTEMPT, 0)
                } else {
                    0
                }
                val shouldPersistCooldown = result.retryAfterMs != null
                if (result.error == io.github.vgy789.doorDuck.model.SyncError.UNAUTHORIZED) {
                    container.settingsStore.setNextAutoRefreshAt(null)
                    container.workScheduler.cancelAutomaticRefresh()
                    Result.failure()
                } else if (shouldPersistCooldown) {
                    val retryAtMs = SyncPolicy.nextRetryAtMs(
                        attempt = attempt,
                        nowMs = System.currentTimeMillis(),
                        minDelayMs = result.retryAfterMs ?: 0L,
                    )
                    container.settingsStore.setNextAutoRefreshAt(retryAtMs)
                    if (autoRefreshEnabled) {
                        container.workScheduler.scheduleAutomaticRetry(
                            retryAtMs = retryAtMs,
                            attempt = attempt + 1,
                        )
                    }
                    Result.success()
                } else if (isAutomaticAttempt && autoRefreshEnabled) {
                    val retryAtMs = SyncPolicy.nextRetryAtMs(
                        attempt = attempt,
                        nowMs = System.currentTimeMillis(),
                    )
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

private fun String.isIntensiveEndpoint(): Boolean {
    return AndroidEndpointSecrets.isIntensiveEndpoint(this)
}
