package io.github.vgy789.doorDuck.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
actual fun PlatformAppLogo(
    modifier: Modifier,
    contentDescription: String,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "🦆",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
