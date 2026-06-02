package io.github.vgy789.doorDuck.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@Composable
actual fun PlatformTokenExamplePreview(
    contentDescription: String,
    modifier: Modifier,
) {
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                backgroundColor = UIColor.clearColor
                image = UIImage.imageNamed("TokenPopupExample")
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.image = UIImage.imageNamed("TokenPopupExample")
        },
    )
}
