package io.github.vgy789.doorDuck

import android.app.Application
import android.content.Context

class DoorDuckApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    companion object {
        fun container(context: Context): AppContainer {
            val app = context.applicationContext as DoorDuckApp
            return app.appContainer
        }
    }
}
