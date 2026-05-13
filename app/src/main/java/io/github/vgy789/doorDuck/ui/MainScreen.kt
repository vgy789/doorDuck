package io.github.vgy789.doorDuck.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.widget.QrGlanceWidgetReceiver
import java.text.DateFormat
import java.util.Date

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.mode) {
                ScreenMode.WIZARD -> WizardContent(state = state, viewModel = viewModel)
                ScreenMode.SETTINGS -> SettingsContent(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun WizardContent(state: MainUiState, viewModel: MainViewModel) {
    Text(
        text = stringResource(R.string.wizard_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    when (state.wizardStep) {
        WizardStep.WELCOME -> {
            Text(stringResource(R.string.wizard_welcome))
            WizardButtons(
                canBack = false,
                canNext = true,
                nextText = stringResource(R.string.action_start),
                onBack = viewModel::wizardBack,
                onNext = viewModel::wizardNext,
            )
        }

        WizardStep.USER_ID -> {
            Text(stringResource(R.string.wizard_user_id_hint))
            OutlinedTextField(
                value = state.userId,
                onValueChange = viewModel::onUserIdChange,
                label = { Text(stringResource(R.string.user_id_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            InstructionCard()
            WizardButtons(
                canBack = true,
                canNext = state.userId.isNotBlank(),
                nextText = stringResource(R.string.action_next),
                onBack = viewModel::wizardBack,
                onNext = viewModel::wizardNext,
            )
        }

        WizardStep.TOKEN -> {
            Text(stringResource(R.string.wizard_token_hint))
            OutlinedTextField(
                value = state.authToken,
                onValueChange = viewModel::onTokenChange,
                label = { Text(stringResource(R.string.token_label)) },
                visualTransformation = if (state.tokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleTokenVisibility) {
                        Icon(
                            imageVector = if (state.tokenVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (state.tokenVisible) {
                                stringResource(R.string.action_hide_token)
                            } else {
                                stringResource(R.string.action_show_token)
                            },
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            WizardButtons(
                canBack = true,
                canNext = state.authToken.isNotBlank(),
                nextText = stringResource(R.string.action_next),
                onBack = viewModel::wizardBack,
                onNext = viewModel::wizardNext,
            )
        }

        WizardStep.CHECK_CONNECTION -> {
            Text(stringResource(R.string.wizard_check_hint))
            Button(
                onClick = viewModel::checkConnection,
                enabled = !state.isConnectionCheckInProgress,
            ) {
                Text(stringResource(R.string.action_check_connection))
            }
            WizardButtons(
                canBack = true,
                canNext = state.connectionCheckPassed,
                nextText = stringResource(R.string.action_finish),
                onBack = viewModel::wizardBack,
                onNext = viewModel::wizardNext,
            )
        }

        WizardStep.DONE -> {
            Text(stringResource(R.string.wizard_done))
            Button(onClick = viewModel::openSettings) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }

    state.infoMessage?.let { message ->
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WizardButtons(
    canBack: Boolean,
    canNext: Boolean,
    nextText: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canBack) {
            Button(onClick = onBack) {
                Text(stringResource(R.string.action_back))
            }
        }
        Button(onClick = onNext, enabled = canNext) {
            Text(nextText)
        }
    }
}

@Composable
private fun SettingsContent(state: MainUiState, viewModel: MainViewModel) {
    Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    OutlinedTextField(
        value = state.endpoint,
        onValueChange = viewModel::onEndpointChange,
        label = { Text(stringResource(R.string.endpoint_label)) },
        trailingIcon = {
            IconButton(onClick = viewModel::resetEndpointToDefault) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.action_reset_endpoint),
                )
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = state.userId,
        onValueChange = viewModel::onUserIdChange,
        label = { Text(stringResource(R.string.user_id_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.authToken,
            onValueChange = viewModel::onTokenChange,
            label = { Text(stringResource(R.string.token_label)) },
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (state.authToken.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearToken) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear_token),
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = viewModel::checkConnection,
            enabled = !state.isConnectionCheckInProgress,
        ) {
            Text(stringResource(R.string.action_check_connection))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = viewModel::refreshNow) {
            Text(stringResource(R.string.manual_refresh))
        }
        Button(onClick = viewModel::openWizard) {
            Text(stringResource(R.string.action_run_wizard))
        }
    }

    state.infoMessage?.let { message ->
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }

    QrPreviewCard(state)
    StatusSection(state = state)
    WidgetInstallCard()
    InstructionCard()
}

@Composable
private fun QrPreviewCard(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.app_qr_title), style = MaterialTheme.typography.titleMedium)
            val bitmap = remember(state.qrImagePath) {
                state.qrImagePath?.let { BitmapFactory.decodeFile(it) }
            }
            if (bitmap == null) {
                Text(stringResource(R.string.app_qr_empty))
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.widget_qr_content_description),
                    modifier = Modifier.size(180.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusSection(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.status_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.status_in_progress, state.syncInProgress.toString()))
            Text(stringResource(R.string.status_last_error, state.lastError?.toDisplayString() ?: stringResource(R.string.status_none)))
            Text(stringResource(R.string.status_last_sync, state.lastSuccessAtMs?.formatDateTime() ?: stringResource(R.string.status_never)))
            Text(stringResource(R.string.status_expires, state.expiresAtMs?.formatDateTime() ?: stringResource(R.string.status_unknown)))
        }
    }
}

@Composable
private fun WidgetInstallCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.widget_install_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.widget_install_hint))
            Button(
                onClick = {
                    val result = requestPinQrWidget(context)
                    val messageRes = when (result) {
                        WidgetPinRequestResult.REQUESTED -> R.string.widget_pin_requested
                        WidgetPinRequestResult.NOT_SUPPORTED -> R.string.widget_pin_not_supported
                        WidgetPinRequestResult.FAILED -> R.string.widget_pin_failed
                    }
                    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_LONG).show()
                },
            ) {
                Text(stringResource(R.string.widget_install_action))
            }
            Text(stringResource(R.string.widget_install_step_1))
            Text(stringResource(R.string.widget_install_step_2))
            Text(stringResource(R.string.widget_install_step_3))
        }
    }
}

@Composable
private fun InstructionCard() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.rocket_tokens_url)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.instruction_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.instruction_step_1))
            Text(stringResource(R.string.instruction_step_2))
            Text(stringResource(R.string.instruction_step_3))
            Text(stringResource(R.string.instruction_step_4))
            Text(stringResource(R.string.instruction_step_5))
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text(stringResource(R.string.instruction_open_link))
            }
        }
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
