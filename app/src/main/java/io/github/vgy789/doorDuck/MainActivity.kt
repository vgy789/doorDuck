package io.github.vgy789.doorDuck

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
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
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { }
                var permissionRequested by remember { mutableStateOf(false) }
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
                LaunchedEffect(state.hasStoredCredentials, state.autoRefreshEnabled) {
                    if (permissionRequested) return@LaunchedEffect
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
                    if (!state.hasStoredCredentials || !state.autoRefreshEnabled) return@LaunchedEffect
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        permissionRequested = true
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
