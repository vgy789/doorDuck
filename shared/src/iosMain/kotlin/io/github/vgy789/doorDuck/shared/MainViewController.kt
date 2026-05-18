package io.github.vgy789.doorDuck.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    DoorDuckSharedApp()
}
