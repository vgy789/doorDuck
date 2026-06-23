package io.github.vgy789.doorDuck

import android.content.Context
import io.github.vgy789.doorDuck.data.QrImageStore
import io.github.vgy789.doorDuck.data.SecureCredentialsStore
import io.github.vgy789.doorDuck.data.SettingsStore
import io.github.vgy789.doorDuck.domain.QrSyncService
import io.github.vgy789.doorDuck.domain.SyncNotificationManager
import io.github.vgy789.doorDuck.domain.QrWorkScheduler
import io.github.vgy789.doorDuck.network.RocketChatClientFactory
import io.github.vgy789.doorDuck.widget.WidgetUpdateCoordinator
import io.github.vgy789.doorDuck.update.ApkUpdateManager
import io.github.vgy789.doorDuck.update.UpdateRepository
import io.github.vgy789.doorDuck.update.UpdateSettingsStore
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    val appContext: Context = context.applicationContext
    val settingsStore = SettingsStore(context)
    val credentialsStore = SecureCredentialsStore(context)
    val imageStore = QrImageStore(context)
    val workScheduler = QrWorkScheduler(context)
    val syncNotificationManager = SyncNotificationManager(context)
    val widgetUpdateCoordinator = WidgetUpdateCoordinator(context)
    val clientFactory = RocketChatClientFactory(json = json)
    val updateSettingsStore = UpdateSettingsStore(context, json)
    val updateRepository = UpdateRepository(updateSettingsStore, json)
    val apkUpdateManager = ApkUpdateManager(context.applicationContext)
    val syncService = QrSyncService(
        settingsStore = settingsStore,
        credentialsStore = credentialsStore,
        clientFactory = clientFactory,
        imageStore = imageStore,
        workScheduler = workScheduler,
        notificationManager = syncNotificationManager,
    )

    init {
        workScheduler.ensureReliabilityWatchdog()
        apkUpdateManager.cleanup()
    }
}
