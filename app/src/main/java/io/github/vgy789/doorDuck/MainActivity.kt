package io.github.vgy789.doorDuck

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vgy789.doorDuck.ui.MainScreen
import io.github.vgy789.doorDuck.ui.DoorDuckTheme
import io.github.vgy789.doorDuck.ui.MainViewModel
import io.github.vgy789.doorDuck.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoorDuckTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(DoorDuckApp.container(this)),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DisposableEffect(state.maxBrightnessEnabled) {
                    val attributes = window.attributes
                    attributes.screenBrightness = if (state.maxBrightnessEnabled) {
                        1f
                    } else {
                        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                    window.attributes = attributes
                    onDispose {
                        val resetAttributes = window.attributes
                        resetAttributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window.attributes = resetAttributes
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
