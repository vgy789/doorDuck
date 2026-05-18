package io.github.vgy789.doorDuck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
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
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
