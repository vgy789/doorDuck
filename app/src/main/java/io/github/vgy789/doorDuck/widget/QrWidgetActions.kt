package io.github.vgy789.doorDuck.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.model.Defaults

class RefreshQrAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = DoorDuckApp.container(context)
        container.settingsStore.setSyncInProgress(true)
        container.workScheduler.enqueueManualRefresh(showToastOnResult = true)
        container.widgetUpdateCoordinator.forceWidgetUpdateNow()
    }
}

class ToggleVisibilityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = DoorDuckApp.container(context)
        val snapshot = container.settingsStore.getSnapshot()
        val nowMs = System.currentTimeMillis()
        val isHidden = (snapshot.revealUntilMs ?: 0L) <= nowMs
        if (isHidden) {
            container.settingsStore.setRevealUntil(nowMs + Defaults.revealDurationMillis)
        } else {
            container.settingsStore.setRevealUntil(null)
        }
        container.widgetUpdateCoordinator.forceWidgetUpdateNow()
    }
}
