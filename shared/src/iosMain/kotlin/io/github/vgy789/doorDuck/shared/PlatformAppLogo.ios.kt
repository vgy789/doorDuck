package io.github.vgy789.doorDuck.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@Composable
actual fun PlatformAppLogo(
    modifier: Modifier,
    contentDescription: String,
) {
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                clipsToBounds = true
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.image = UIImage.imageNamed("Image")
        },
    )
}
