package io.github.vgy789.doorDuck.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.vgy789.doorDuck.worker.WidgetRefreshWorker

class WidgetRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isWidgetRestoreAction(intent.action)) return

        WorkManager.getInstance(context).enqueueUniqueWork(
            WIDGET_RESTORE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    companion object {
        const val WIDGET_RESTORE_WORK_NAME = "door_duck_widget_restore"
    }
}

internal fun isWidgetRestoreAction(action: String?): Boolean {
    return action == Intent.ACTION_BOOT_COMPLETED
}
