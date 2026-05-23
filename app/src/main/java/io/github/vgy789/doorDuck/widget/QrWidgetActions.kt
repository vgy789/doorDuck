package io.github.vgy789.doorDuck.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.SyncError

class RefreshQrAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = DoorDuckApp.container(context)
        val snapshot = container.settingsStore.getSnapshot()
        val nowMs = System.currentTimeMillis()
        val hasFreshQr = !snapshot.localImagePath.isNullOrBlank() &&
            !SyncPolicy.isExpired(snapshot.expiresAtMs, nowMs)
        if (hasFreshQr) {
            container.widgetUpdateCoordinator.forceWidgetUpdateNow()
            return
        }
        if (snapshot.isSyncInProgress) return
        if (SyncPolicy.isManualRefreshBlocked(snapshot.manualRefreshBlockedUntilMs, nowMs)) return
        if (
            snapshot.lastError == SyncError.RATE_LIMITED &&
            (snapshot.nextAutoRefreshAtMs ?: 0L) > nowMs
        ) {
            return
        }
        container.settingsStore.setManualRefreshBlockedUntil(
            SyncPolicy.nextManualRefreshAllowedAt(nowMs),
        )
        container.settingsStore.setSyncInProgress(true)
        container.workScheduler.enqueueManualRefresh(showToastOnResult = false)
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
