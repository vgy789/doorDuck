package io.github.vgy789.doorDuck

import android.content.Context
import io.github.vgy789.doorDuck.data.QrImageStore
import io.github.vgy789.doorDuck.data.SecureCredentialsStore
import io.github.vgy789.doorDuck.data.SettingsStore
import io.github.vgy789.doorDuck.domain.QrSyncService
import io.github.vgy789.doorDuck.domain.QrWorkScheduler
import io.github.vgy789.doorDuck.network.RocketChatClientFactory
import io.github.vgy789.doorDuck.widget.WidgetUpdateCoordinator
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settingsStore = SettingsStore(context)
    val credentialsStore = SecureCredentialsStore(context)
    val imageStore = QrImageStore(context)
    val workScheduler = QrWorkScheduler(context)
    val widgetUpdateCoordinator = WidgetUpdateCoordinator(context)
    val clientFactory = RocketChatClientFactory(
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )
    val syncService = QrSyncService(
        settingsStore = settingsStore,
        credentialsStore = credentialsStore,
        clientFactory = clientFactory,
        imageStore = imageStore,
        workScheduler = workScheduler,
    )

    init {
        workScheduler.ensureReliabilityWatchdog()
    }
}
