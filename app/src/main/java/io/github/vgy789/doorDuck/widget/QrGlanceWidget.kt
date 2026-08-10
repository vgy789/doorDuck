package io.github.vgy789.doorDuck.widget

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import io.github.vgy789.doorDuck.DoorDuckApp
import io.github.vgy789.doorDuck.BuildConfig
import io.github.vgy789.doorDuck.MainActivity
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.QrCodeSnapshot
import io.github.vgy789.doorDuck.model.QrReadiness

class QrGlanceWidget : StyledQrGlanceWidget(WidgetStyle.TONAL_CARD) {
}

class QrCleanGlanceWidget : StyledQrGlanceWidget(WidgetStyle.WHITE) {
}

enum class WidgetStyle {
    TONAL_CARD,
    WHITE,
}

abstract class StyledQrGlanceWidget(
    private val style: WidgetStyle,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        val uiState = loadWidgetUiState(context)

        provideContent {
            GlanceTheme { WidgetContent(uiState, style) }
        }
    }
}

private suspend fun loadWidgetUiState(context: android.content.Context): WidgetUiState {
    val container = DoorDuckApp.container(context)
    val snapshot = container.settingsStore.getSnapshot()
    val hasCredentials = container.credentialsStore.hasCredentials()
    val configured = hasCredentials || (
        BuildConfig.DEBUG && !snapshot.localImagePath.isNullOrBlank()
    )
    return WidgetUiState(
        configured = configured,
        snapshot = snapshot,
        refreshPending = snapshot.isSyncInProgress ||
            SyncPolicy.isManualRefreshBlocked(snapshot.manualRefreshBlockedUntilMs, System.currentTimeMillis()),
        readiness = SyncPolicy.readiness(
            hasImage = !snapshot.localImagePath.isNullOrBlank(),
            validationStatus = snapshot.imageValidationStatus,
            expiresAtMs = snapshot.expiresAtMs,
        ),
    )
}

@Composable
private fun WidgetContent(
    uiState: WidgetUiState,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val openAppAction = actionStartActivity(Intent(context, MainActivity::class.java))
    val bitmap = uiState.snapshot.localImagePath?.let(BitmapFactory::decodeFile)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(widgetCornerRadiusResource())
            .clickable(openAppAction),
        contentAlignment = Alignment.Center,
    ) {
        WidgetChrome(style) {
            when {
                uiState.configured && bitmap != null && uiState.readiness != QrReadiness.EXPIRED &&
                    uiState.readiness != QrReadiness.MISSING_OR_INVALID -> {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = context.getString(R.string.widget_qr_content_description),
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }

                uiState.configured -> {
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_widget_refresh_button),
                        contentDescription = context.getString(
                            if (uiState.refreshPending) {
                                R.string.widget_loading
                            } else {
                                R.string.widget_refresh_content_description
                            },
                        ),
                        onClick = actionRunCallback<RefreshQrAction>(),
                        modifier = GlanceModifier.size(48.dp),
                        enabled = !uiState.refreshPending,
                        backgroundColor = GlanceTheme.colors.secondaryContainer,
                        contentColor = GlanceTheme.colors.onSecondaryContainer,
                    )
                }

                else -> {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher_foreground),
                        contentDescription = context.getString(R.string.app_name),
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetChrome(
    style: WidgetStyle,
    content: @Composable () -> Unit,
) {
    when (style) {
        WidgetStyle.TONAL_CARD -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(widgetCornerRadiusResource()),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(Color.White)
                            .cornerRadius(18.dp),
                        contentAlignment = Alignment.Center,
                        content = content,
                    )
                }
            }
        }

        WidgetStyle.WHITE -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.White)
                    .cornerRadius(widgetCornerRadiusResource()),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}

private fun widgetCornerRadiusResource(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        android.R.dimen.system_app_widget_background_radius
    } else {
        R.dimen.widget_corner_radius
    }
}

private data class WidgetUiState(
    val configured: Boolean,
    val snapshot: QrCodeSnapshot,
    val refreshPending: Boolean,
    val readiness: QrReadiness,
)
