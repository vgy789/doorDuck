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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.MainActivity
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.QrCodeSnapshot
import io.github.vgy789.doorDuck.model.SyncError

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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center,
    ) {
        if (!uiState.configured) {
            Text(context.getString(R.string.widget_not_configured))
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    uiState.snapshot.localImagePath.isNullOrBlank() -> {
                        if (uiState.snapshot.isSyncInProgress) {
                            Text(context.getString(R.string.widget_loading))
                        } else if (uiState.snapshot.lastError != null) {
                            Text("${context.getString(R.string.widget_error)}: ${uiState.snapshot.lastError.toDisplayString(context)}")
                        } else {
                            Text(
                                text = context.getString(R.string.widget_no_qr),
                            )
                        }
                    }

                    uiState.isExpired -> {
                        if (uiState.snapshot.isSyncInProgress) {
                            Text(context.getString(R.string.widget_loading))
                        } else {
                            Text(context.getString(R.string.widget_expired))
                        }
                    }

                    else -> {
                        val bitmap = BitmapFactory.decodeFile(uiState.snapshot.localImagePath)
                        if (bitmap == null) {
                            Text(context.getString(R.string.widget_no_qr))
                        } else {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = context.getString(R.string.widget_qr_content_description),
                                contentScale = ContentScale.Fit,
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(0.dp),
                            )
                        }
                    }
                }

                uiState.snapshot.lastError?.let { error ->
                    if (error != SyncError.UNKNOWN) {
                        Text(context.getString(R.string.widget_last_error, error.toDisplayString(context)))
                    }
                }

            }
        }
    }
}

private data class WidgetUiState(
    val configured: Boolean,
    val snapshot: QrCodeSnapshot,
    val isExpired: Boolean,
)

private fun SyncError.toDisplayString(context: android.content.Context): String {
    val resId = when (this) {
        SyncError.NOT_CONFIGURED -> R.string.sync_error_not_configured
        SyncError.UNAUTHORIZED -> R.string.sync_error_unauthorized
        SyncError.NETWORK -> R.string.sync_error_network
        SyncError.BOT_RESPONSE_INVALID -> R.string.sync_error_bot_response_invalid
        SyncError.IMAGE_DOWNLOAD_FAILED -> R.string.sync_error_image_download_failed
        SyncError.UNKNOWN -> R.string.sync_error_unknown
    }
    return context.getString(resId)
}
