package io.github.vgy789.doorDuck.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.platform.formatEpochDate
import io.github.vgy789.doorDuck.platform.formatEpochMillis

@Composable
internal fun doorDuckBackgroundBrush(): Brush {
    return if (isDoorDuckDarkTheme()) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF16120D), Color(0xFF201A14), Color(0xFF171411)),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFBF3), Color(0xFFF8F0E2), Color(0xFFF6F1EA)),
        )
    }
}

@Composable
internal fun DoorDuckHeader(
    strings: SharedStrings,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val dark = isDoorDuckDarkTheme()
    val heroBrush = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF332612), Color(0xFF211912)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFF6DD), Color(0xFFFFECD0)))
    }
    val borderColor = if (dark) Color(0xFF5D4521) else Color(0xFFE6D1A7)
    val badgeColor = if (dark) Color(0xFFF2C64D) else Color(0xFFE3A919)
    val primaryTextColor = if (dark) Color(0xFFFFF6E8) else Color(0xFF2F2415)
    val secondaryTextColor = if (dark) Color(0xFFF0E4C8) else Color(0xFF6B5940)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (dark) 0.dp else 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor,
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlatformAppLogo(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            contentDescription = strings.appTitle,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.homeTitle,
                        color = primaryTextColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "Rocket.Chat QR Sync",
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (dark) Color(0xFFFFE7B5) else Color(0xFF6F4700),
                    ),
                ) {
                    Text("⚙")
                    Box(modifier = Modifier.size(8.dp))
                    Text(
                        text = actionLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DoorDuckStatusCard(
    strings: SharedStrings,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    qrImageBase64: String?,
    isRefreshingQr: Boolean,
) {
    val dark = isDoorDuckDarkTheme()
    val hasActiveQr = !qrImageBase64.isNullOrBlank() && !SyncPolicy.isExpired(expiresAtMs)
    val headline = when {
        isRefreshingQr -> strings.statusRefreshingHeadline
        lastSyncError != null -> strings.statusNeedsRefreshHeadline
        hasActiveQr -> strings.statusFreshHeadline
        else -> strings.statusMissingHeadline
    }
    val containerColor = if (dark) Color(0xFF1F2A1D) else Color(0xFFF3FBEF)
    val borderColor = if (dark) Color(0xFF355B34) else Color(0xFFB8DBA9)
    val badgeColor = if (dark) Color(0xFF88D89F) else Color(0xFFCBEFBC)
    val headlineColor = if (dark) Color(0xFFA5F1B9) else Color(0xFF247A39)
    val textColor = if (dark) Color(0xFFEDE6D8) else Color(0xFF4E4335)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = badgeColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "OK",
                            color = Color(0xFF146B32),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = headline,
                    modifier = Modifier.weight(1f),
                    color = headlineColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(modifier = Modifier.size(24.dp))
            }
            Text(
                text = "${strings.statusLastCheck}: ${lastSuccessAtMs?.let(::formatEpochMillis) ?: strings.statusNever}",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${strings.statusExpiresAt}: ${expiresAtMs?.let(::formatEpochDate) ?: strings.statusUnknown}",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun DoorDuckQrCard(
    strings: SharedStrings,
    qrImageBase64: String?,
    isRefreshingQr: Boolean,
    onRefreshQr: () -> Unit,
) {
    val dark = isDoorDuckDarkTheme()
    val qrFrameColor = if (dark) Color(0xFFFFFEFB) else Color(0xFFFFFFFF)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings.qrTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = qrFrameColor,
                shadowElevation = if (dark) 0.dp else 10.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlatformQrPreview(
                        base64 = qrImageBase64,
                        emptyText = strings.qrEmpty,
                        contentDescription = strings.qrTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(maxWidth = 360.dp)
                            .aspectRatio(1f),
                    )
                }
            }
            Button(
                onClick = onRefreshQr,
                enabled = !isRefreshingQr,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) Color(0xFFF2C64D) else Color(0xFFE8B12C),
                    contentColor = Color(0xFF2C220F),
                ),
            ) {
                Text(if (isRefreshingQr) strings.connectionChecking else strings.actionRefreshNow)
            }
        }
    }
}

@Composable
internal fun DoorDuckConnectionCard(
    strings: SharedStrings,
    expanded: Boolean,
    showExpandToggle: Boolean,
    endpoint: String,
    userId: String,
    token: String,
    isCheckingConnection: Boolean,
    lastConnectionResult: ConnectionCheckResult?,
    onExpandedChange: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit,
) {
    val dark = isDoorDuckDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.connectionSectionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = strings.connectionSectionBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showExpandToggle) {
                    TextButton(onClick = onExpandedChange) {
                        Text(if (expanded) strings.actionCollapse else strings.actionExpand)
                    }
                }
            }

            if (expanded) {
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = onEndpointChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.endpointLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = onUserIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.userIdLabel) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.tokenLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCheckConnection,
                        enabled = !isCheckingConnection,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dark) Color(0xFFF2C64D) else Color(0xFFF1D38E),
                            contentColor = Color(0xFF2D220F),
                        ),
                    ) {
                        Text(if (isCheckingConnection) strings.connectionChecking else strings.actionCheckConnection)
                    }
                    OutlinedButton(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(strings.actionSave)
                    }
                }
                if (lastConnectionResult != null) {
                    Text(
                        text = "${strings.statusConnection}: ${strings.connectionMessage(lastConnectionResult)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DoorDuckWidgetCard(
    strings: SharedStrings,
    onWidgetAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strings.widgetTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = strings.widgetBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onWidgetAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDoorDuckDarkTheme()) Color(0xFFFFE7B5) else Color(0xFF6F4700),
                ),
            ) {
                Text(strings.widgetAction)
            }
        }
    }
}

@Composable
internal fun DoorDuckHelpCard(
    strings: SharedStrings,
    onOpenTokensPage: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.instructionTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = strings.instructionBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(strings.instructionStep1, style = MaterialTheme.typography.bodySmall)
            Text(strings.instructionStep2, style = MaterialTheme.typography.bodySmall)
            Text(strings.instructionStep3, style = MaterialTheme.typography.bodySmall)
            Text(strings.instructionStep4, style = MaterialTheme.typography.bodySmall)
            Text(strings.instructionStep5, style = MaterialTheme.typography.bodySmall)
            Text(strings.instructionStep6, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenTokensPage) {
                Text(strings.instructionOpenLink)
            }
        }
    }
}

@Composable
internal fun DoorDuckCreditsCard(
    strings: SharedStrings,
    onOpenGithubPage: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenGithubPage) {
                Text("★", fontSize = 12.sp, color = Color(0xFFE8B12C))
                Box(modifier = Modifier.width(4.dp))
                Text(
                    strings.githubLinkLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.githubOwnerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun DoorDuckInfoBanner(message: String) {
    val dark = isDoorDuckDarkTheme()
    val containerColor = if (dark) Color(0xFF332612) else Color(0xFFFFF2D3)
    val borderColor = if (dark) Color(0xFF6A5123) else Color(0xFFEACD8A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun isDoorDuckDarkTheme(): Boolean = MaterialTheme.colorScheme.background.red < 0.2f
