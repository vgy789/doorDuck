package io.github.vgy789.doorDuck.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.vgy789.doorDuck.DoorDuckApp

class WidgetRefreshWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return runCatching {
            DoorDuckApp.container(applicationContext)
                .widgetUpdateCoordinator
                .forceWidgetUpdateNow()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
