package io.github.vgy789.doorDuck.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformQrPreview(
    base64: String?,
    emptyText: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
)
