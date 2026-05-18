package io.github.vgy789.doorDuck.ui

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.widget.QrGlanceWidgetReceiver
import java.text.DateFormat
import java.util.Date

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var connectionExpanded by remember(state.mode) { mutableStateOf(state.mode == ScreenMode.SETTINGS) }
    var showQrDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(doorDuckBackgroundBrush())
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderCard(
                mode = state.mode,
                hasStoredCredentials = state.hasStoredCredentials,
                onOpenSettings = viewModel::openSettings,
                onOpenWizard = viewModel::openWizard,
            )

            when (state.mode) {
                ScreenMode.WIZARD -> WizardContent(state = state, viewModel = viewModel)
                ScreenMode.SETTINGS -> SettingsDashboard(
                    state = state,
                    viewModel = viewModel,
                    connectionExpanded = connectionExpanded,
                    onToggleConnection = { connectionExpanded = !connectionExpanded },
                    onOpenQr = { showQrDialog = true },
                )
            }

            state.infoMessage?.let { message ->
                NoticeCard(message = message)
            }
        }
    }

    val dialogQrImagePath = state.qrImagePath
    if (showQrDialog && dialogQrImagePath != null) {
        QrDialog(
            qrImagePath = dialogQrImagePath,
            onDismiss = { showQrDialog = false },
        )
    }
}

@Composable
private fun HeaderCard(
    mode: ScreenMode,
    hasStoredCredentials: Boolean,
    onOpenSettings: () -> Unit,
    onOpenWizard: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val heroBrush = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF342711), Color(0xFF201811)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFF6DE), Color(0xFFFFEBCB)))
    }
    val borderColor = if (dark) Color(0xFF5D4521) else Color(0xFFE6D1A7)
    val badgeColor = if (dark) Color(0x1FFFF1C8) else Color(0x14A56A00)
    val primaryTextColor = if (dark) Color(0xFFFFF6E7) else Color(0xFF2E2418)
    val secondaryTextColor = if (dark) Color(0xFFF0E3C6) else Color(0xFF6A5740)
    val actionTint = if (dark) Color(0xFFFFE7B5) else Color(0xFF4A3211)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (dark) 0.dp else 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(62.dp),
                shape = RoundedCornerShape(20.dp),
                color = badgeColor,
            ) {
                Box(modifier = Modifier.padding(3.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                )
                Text(
                    text = stringResource(R.string.dashboard_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryTextColor,
                )
            }

            if (mode == ScreenMode.SETTINGS) {
                IconButton(onClick = onOpenWizard) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_run_wizard), tint = actionTint)
                }
            } else if (hasStoredCredentials) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_open_settings), tint = actionTint)
                }
            }
        }
    }
}

@Composable
private fun WizardContent(
    state: MainUiState,
    viewModel: MainViewModel,
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.wizard_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            when (state.wizardStep) {
                WizardStep.WELCOME -> {
                    Text(stringResource(R.string.wizard_welcome), style = MaterialTheme.typography.bodyLarge)
                }

                WizardStep.USER_ID -> {
                    Text(stringResource(R.string.wizard_user_id_hint), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = state.userId,
                        onValueChange = viewModel::onUserIdChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.user_id_label)) },
                        singleLine = true,
                    )
                    HelpCard()
                }

                WizardStep.TOKEN -> {
                    Text(stringResource(R.string.wizard_token_hint), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = state.authToken,
                        onValueChange = viewModel::onTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.token_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                WizardStep.CHECK_CONNECTION -> {
                    Text(stringResource(R.string.wizard_check_hint), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = state.endpoint,
                        onValueChange = viewModel::onEndpointChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.endpoint_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    FilledTonalButton(
                        onClick = viewModel::checkConnection,
                        enabled = !state.isConnectionCheckInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isConnectionCheckInProgress) {
                                stringResource(R.string.action_check_connection)
                            } else {
                                stringResource(R.string.action_check_connection)
                            },
                        )
                    }
                }

                WizardStep.DONE -> {
                    Text(stringResource(R.string.wizard_done), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = viewModel::openSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }

            WizardButtons(
                state = state,
                onBack = viewModel::wizardBack,
                onNext = viewModel::wizardNext,
            )
        }
    }
}

@Composable
private fun WizardButtons(
    state: MainUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.wizardStep != WizardStep.WELCOME) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            val nextLabel = when (state.wizardStep) {
                WizardStep.WELCOME -> R.string.action_start
                WizardStep.USER_ID, WizardStep.TOKEN -> R.string.action_next
                WizardStep.CHECK_CONNECTION -> R.string.action_finish
                WizardStep.DONE -> R.string.action_open_settings
            }
            Text(stringResource(nextLabel))
        }
    }
}

@Composable
private fun SettingsDashboard(
    state: MainUiState,
    viewModel: MainViewModel,
    connectionExpanded: Boolean,
    onToggleConnection: () -> Unit,
    onOpenQr: () -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatusCard(state = state)
        QrCard(state = state, onRefresh = viewModel::refreshNow, onRunWizard = viewModel::openWizard, onOpenQr = onOpenQr)
        WidgetInstallCard()
        ConnectionCard(
            state = state,
            connectionExpanded = connectionExpanded,
            onCopyEndpoint = { copyToClipboard(context, "endpoint", state.endpoint) },
            onCopyUserId = { copyToClipboard(context, "user_id", state.userId) },
            onToggleConnection = onToggleConnection,
            onEndpointChange = viewModel::onEndpointChange,
            onUserIdChange = viewModel::onUserIdChange,
            onTokenChange = viewModel::onTokenChange,
            onCheckConnection = viewModel::checkConnection,
            onResetEndpoint = viewModel::resetEndpointToDefault,
        )
        HelpCard()
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    val hasActiveQr = state.qrImagePath != null && !SyncPolicy.isExpired(state.expiresAtMs)
    val headline = when {
        state.syncInProgress -> stringResource(R.string.status_qr_refreshing)
        state.lastError != null -> stringResource(R.string.status_qr_needs_attention)
        hasActiveQr -> stringResource(R.string.status_qr_fresh)
        else -> stringResource(R.string.status_qr_missing)
    }
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val containerColor = if (dark) Color(0xFF1F2A1D) else Color(0xFFF3FBEF)
    val borderColor = if (dark) Color(0xFF355B34) else Color(0xFFB8DBA9)
    val badgeColor = if (dark) Color(0xFF88D89F) else Color(0xFFCBEFBC)
    val headlineColor = if (dark) Color(0xFFA6F0BA) else Color(0xFF247A39)
    val textColor = if (dark) Color(0xFFF3EBDD) else Color(0xFF4E4335)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = badgeColor) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF167335))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(headline, color = headlineColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.status_last_sync, state.lastSuccessAtMs?.formatDateTime() ?: stringResource(R.string.status_never)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                Text(
                    stringResource(R.string.status_expires, state.expiresAtMs?.formatDateOnly() ?: stringResource(R.string.status_unknown)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun QrCard(
    state: MainUiState,
    onRefresh: () -> Unit,
    onRunWizard: () -> Unit,
    onOpenQr: () -> Unit,
) {
    val bitmap = remember(state.qrImagePath) {
        state.qrImagePath?.let { BitmapFactory.decodeFile(it) }
    }

    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.app_qr_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onOpenQr, enabled = bitmap != null) {
                    Icon(Icons.Filled.OpenInFull, contentDescription = stringResource(R.string.widget_qr_content_description))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 10.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap == null) {
                        Text(stringResource(R.string.app_qr_empty))
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.widget_qr_content_description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(maxWidth = 340.dp)
                                .aspectRatio(1f),
                        )
                    }
                }
            }

            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Box(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.manual_refresh))
            }

            OutlinedButton(
                onClick = onRunWizard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFFFE7B5) else Color(0xFF6F4700),
                ),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Box(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.action_run_wizard))
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    state: MainUiState,
    connectionExpanded: Boolean,
    onCopyEndpoint: () -> Unit,
    onCopyUserId: () -> Unit,
    onToggleConnection: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onCheckConnection: () -> Unit,
    onResetEndpoint: () -> Unit,
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.connection_section_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.connection_section_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleConnection) {
                    Icon(
                        imageVector = if (connectionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            if (connectionExpanded) {
                ConnectionFieldHeader(
                    icon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    title = stringResource(R.string.endpoint_label),
                    onCopy = onCopyEndpoint,
                    extraAction = onResetEndpoint,
                    extraIcon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_reset_endpoint)) },
                )
                OutlinedTextField(
                    value = state.endpoint,
                    onValueChange = onEndpointChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                ConnectionFieldHeader(
                    icon = { Icon(Icons.Filled.PersonOutline, contentDescription = null) },
                    title = stringResource(R.string.user_id_label),
                    onCopy = onCopyUserId,
                )
                OutlinedTextField(
                    value = state.userId,
                    onValueChange = onUserIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ConnectionFieldHeader(
                    icon = { Icon(Icons.Filled.Key, contentDescription = null) },
                    title = stringResource(R.string.token_label),
                    onCopy = null,
                )
                OutlinedTextField(
                    value = state.authToken,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                FilledTonalButton(
                    onClick = onCheckConnection,
                    enabled = !state.isConnectionCheckInProgress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFF2C64D) else Color(0xFFF5D68A),
                        contentColor = Color(0xFF2D220F),
                    ),
                ) {
                    Text(stringResource(R.string.action_check_connection))
                }

                state.lastConnectionCheckResult?.let { result ->
                    val connectionStatusText = when (result) {
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.SUCCESS -> stringResource(R.string.connection_ok)
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.UNAUTHORIZED -> stringResource(R.string.connection_unauthorized)
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.BOT_UNAVAILABLE -> stringResource(R.string.connection_bot_unavailable)
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.NETWORK_ERROR -> stringResource(R.string.connection_network_error)
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.UNKNOWN -> stringResource(R.string.connection_unknown_error)
                    }
                    Text(
                        text = connectionStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

@Composable
private fun ConnectionFieldHeader(
    icon: @Composable () -> Unit,
    title: String,
    onCopy: (() -> Unit)?,
    extraAction: (() -> Unit)? = null,
    extraIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (extraAction != null && extraIcon != null) {
                IconButton(onClick = extraAction) {
                    extraIcon()
                }
            }
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun WidgetInstallCard() {
    val context = LocalContext.current
    val pinRequestedMessage = stringResource(R.string.widget_pin_requested)
    val pinNotSupportedMessage = stringResource(R.string.widget_pin_not_supported)
    val pinFailedMessage = stringResource(R.string.widget_pin_failed)
    DashboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.widget_install_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.widget_install_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val result = requestPinQrWidget(context)
                    val message = when (result) {
                        WidgetPinRequestResult.REQUESTED -> pinRequestedMessage
                        WidgetPinRequestResult.NOT_SUPPORTED -> pinNotSupportedMessage
                        WidgetPinRequestResult.FAILED -> pinFailedMessage
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFFFE7B5) else Color(0xFF6F4700),
                ),
            ) {
                Text(stringResource(R.string.widget_install_action))
            }
        }
    }
}

@Composable
private fun HelpCard() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.rocket_tokens_url)
    val githubUrl = stringResource(R.string.github_url)

    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.instruction_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.token_help_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(stringResource(R.string.instruction_step_1), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_2), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_3), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_4), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_5), style = MaterialTheme.typography.bodySmall)

            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text(stringResource(R.string.instruction_open_link))
                Box(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { uriHandler.openUri(githubUrl) }) {
                    Text(
                        text = stringResource(R.string.instruction_github_link),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(R.string.instruction_s21_login),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(message: String) {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
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
            color = if (dark) Color(0xFFFFF0D3) else Color(0xFF5C3F00),
        )
    }
}

@Composable
private fun QrDialog(
    qrImagePath: String,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(qrImagePath) { BitmapFactory.decodeFile(qrImagePath) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(30.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.app_qr_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.widget_qr_content_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(maxWidth = 420.dp)
                            .aspectRatio(1f),
                    )
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_back))
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun doorDuckBackgroundBrush(): Brush {
    return if (MaterialTheme.colorScheme.background.red < 0.2f) {
        Brush.verticalGradient(listOf(Color(0xFF16120D), Color(0xFF201A14), Color(0xFF171411)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFBF3), Color(0xFFF8F0E2), Color(0xFFF6F1EA)))
    }
}

private fun requestPinQrWidget(context: Context): WidgetPinRequestResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return WidgetPinRequestResult.NOT_SUPPORTED
    }
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        ?: return WidgetPinRequestResult.NOT_SUPPORTED
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        return WidgetPinRequestResult.NOT_SUPPORTED
    }
    val provider = ComponentName(context, QrGlanceWidgetReceiver::class.java)
    return if (appWidgetManager.requestPinAppWidget(provider, null, null)) {
        WidgetPinRequestResult.REQUESTED
    } else {
        WidgetPinRequestResult.FAILED
    }
}

private enum class WidgetPinRequestResult {
    REQUESTED,
    NOT_SUPPORTED,
    FAILED,
}

private fun Long.formatDateTime(): String = DateFormat.getDateTimeInstance().format(Date(this))
private fun Long.formatDateOnly(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(this))

@Composable
private fun SyncError.toDisplayString(): String {
    return when (this) {
        SyncError.NOT_CONFIGURED -> stringResource(R.string.sync_error_not_configured)
        SyncError.UNAUTHORIZED -> stringResource(R.string.sync_error_unauthorized)
        SyncError.NETWORK -> stringResource(R.string.sync_error_network)
        SyncError.BOT_RESPONSE_INVALID -> stringResource(R.string.sync_error_bot_response_invalid)
        SyncError.IMAGE_DOWNLOAD_FAILED -> stringResource(R.string.sync_error_image_download_failed)
        SyncError.UNKNOWN -> stringResource(R.string.sync_error_unknown)
    }
}
