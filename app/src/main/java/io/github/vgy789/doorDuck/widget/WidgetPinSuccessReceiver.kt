package io.github.vgy789.doorDuck.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.vgy789.doorDuck.R

class WidgetPinSuccessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_PINNED) return

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        Toast.makeText(
            context,
            R.string.widget_pin_success,
            Toast.LENGTH_LONG,
        ).show()
    }

    companion object {
        const val ACTION_WIDGET_PINNED = "io.github.vgy789.doorDuck.WIDGET_PINNED"
    }
}
