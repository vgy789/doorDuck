package io.github.vgy789.doorDuck.widget

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.MainActivity
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.QrCodeSnapshot

class QrGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        val container = DoorDuckApp.container(context)
        val snapshot = container.settingsStore.getSnapshot()
        val hasCredentials = container.credentialsStore.hasCredentials()
        val uiState = WidgetUiState(
            configured = hasCredentials,
            snapshot = snapshot,
            isExpired = SyncPolicy.isExpired(snapshot.expiresAtMs),
        )

        provideContent {
            GlanceTheme { WidgetContent(uiState) }
        }
    }
}

@Composable
private fun WidgetContent(uiState: WidgetUiState) {
    val context = LocalContext.current
    val openAppAction = actionStartActivity(Intent(context, MainActivity::class.java))
    val bitmap = uiState.snapshot.localImagePath?.let(BitmapFactory::decodeFile)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.configured && bitmap != null && !uiState.isExpired -> {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(R.string.widget_qr_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(0.dp)
                        .clickable(openAppAction),
                )
            }

            uiState.configured -> {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh_button),
                    contentDescription = context.getString(R.string.widget_refresh_content_description),
                    modifier = GlanceModifier
                        .size(48.dp)
                        .clickable(actionRunCallback<RefreshQrAction>()),
                )
            }

            else -> {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher_foreground),
                    contentDescription = context.getString(R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier
                        .size(40.dp)
                        .clickable(openAppAction),
                )
            }
        }
    }
}

private data class WidgetUiState(
    val configured: Boolean,
    val snapshot: QrCodeSnapshot,
    val isExpired: Boolean,
)
