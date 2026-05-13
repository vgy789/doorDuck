package io.github.vgy789.doorDuck.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
            updateWidget(QrGlanceWidget(), QrGlanceWidget::class.java, QrGlanceWidgetReceiver::class.java)
        }
    }

    private suspend fun updateWidget(
        widget: androidx.glance.appwidget.GlanceAppWidget,
        widgetClass: Class<out androidx.glance.appwidget.GlanceAppWidget>,
        receiverClass: Class<out GlanceAppWidgetReceiver>,
    ) {
        widget.updateAll(appContext)
        val glanceManager = GlanceAppWidgetManager(appContext)
        val glanceIds = glanceManager.getGlanceIds(widgetClass)
        val appWidgetIds = IntArray(glanceIds.size)
        glanceIds.forEachIndexed { index, glanceId ->
            widget.update(appContext, glanceId)
            appWidgetIds[index] = glanceManager.getAppWidgetId(glanceId)
        }
        if (appWidgetIds.isNotEmpty()) {
            val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = ComponentName(appContext, receiverClass)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            appContext.sendBroadcast(updateIntent)
        }
    }
}
