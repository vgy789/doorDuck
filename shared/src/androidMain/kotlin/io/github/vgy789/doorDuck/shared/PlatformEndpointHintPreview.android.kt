package io.github.vgy789.doorDuck.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformEndpointHintPreview(
    image: EndpointHintImage,
    contentDescription: String,
    modifier: Modifier,
) {
    Text(
        text = contentDescription,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
