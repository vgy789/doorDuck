package io.github.vgy789.doorDuck.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class EndpointHintImage {
    BROWSER,
    MOBILE,
}

@Composable
expect fun PlatformEndpointHintPreview(
    image: EndpointHintImage,
    contentDescription: String,
    modifier: Modifier = Modifier,
)
