package io.github.vgy789.doorDuck.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WidgetUpdateCoordinator(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val updateMutex = Mutex()

    suspend fun forceWidgetUpdateNow() {
        updateMutex.withLock {
            QrGlanceWidget().updateAll(appContext)
            QrCleanGlanceWidget().updateAll(appContext)
        }
    }
}
