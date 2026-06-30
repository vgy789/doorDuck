package io.github.vgy789.doorDuck.ui

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.BuildConfig
import io.github.vgy789.doorDuck.config.AndroidEndpointSecrets
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.IntensiveCampus
import io.github.vgy789.doorDuck.model.QrReadiness
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.widget.QrGlanceWidgetReceiver
import io.github.vgy789.doorDuck.update.UpdateMessage
import io.github.vgy789.doorDuck.update.UpdateStatus
import io.github.vgy789.doorDuck.update.UpdateUiState
import io.github.vgy789.doorDuck.update.changelogItems
import io.github.vgy789.doorDuck.update.canRetryUpdate
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private val DashboardActionButtonHeight = 48.dp
private val DashboardActionButtonShape = RoundedCornerShape(14.dp)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val refreshCooldownNowMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = state.manualRefreshBlockedUntilMs,
        key2 = state.syncInProgress,
    ) {
        value = System.currentTimeMillis()
        val blockedUntilMs = state.manualRefreshBlockedUntilMs
        if (!state.syncInProgress && blockedUntilMs != null) {
            val waitMs = (blockedUntilMs - value).coerceAtLeast(0L)
            if (waitMs > 0L) {
                delay(waitMs + 50L)
                value = System.currentTimeMillis()
            }
        }
    }
    LaunchedEffect(state.mode) {
        scrollState.animateScrollTo(0)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(doorDuckBackgroundBrush())
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderCard(
                mode = state.mode,
                hasStoredCredentials = state.hasStoredCredentials,
                onOpenSettings = viewModel::openSettings,
            )

            when (state.mode) {
                ScreenMode.WIZARD -> WizardContent(state = state, viewModel = viewModel)
                ScreenMode.SETTINGS -> SettingsDashboard(
                    state = state,
                    viewModel = viewModel,
                    refreshCooldownNowMs = refreshCooldownNowMs,
                )
            }

            state.infoMessage?.let { message ->
                NoticeCard(message = message)
            }
        }
    }
    if (state.update.isDialogVisible) {
        UpdateDialog(
            state = state.update,
            onDismiss = viewModel::dismissUpdateDialog,
            onDownload = viewModel::downloadUpdate,
            onCancelDownload = viewModel::cancelUpdateDownload,
            onInstall = viewModel::requestUpdateInstallation,
            onOpenInstallPermission = viewModel::openUpdateInstallPermissionSettings,
            onRetry = viewModel::retryUpdateAction,
        )
    }
}

@Composable
private fun HeaderCard(
    mode: ScreenMode,
    hasStoredCredentials: Boolean,
    onOpenSettings: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
            }
        }
    }
}

@Composable
private fun WizardContent(
    state: MainUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val rocketTokensUrl = InputSanitizer.tokensPageUrl(state.wizardEndpoint)
    val tokensUrlMissingMessage = stringResource(R.string.error_tokens_url_missing)
    val canOpenTokensPage = rocketTokensUrl.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.wizard_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        DashboardCard {
            Text(
                stringResource(R.string.wizard_welcome),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DashboardCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. ${stringResource(R.string.wizard_section_endpoint_title)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                EndpointSettingsBlock(
                    endpoint = state.wizardEndpoint,
                    selectedPreset = state.wizardEndpointPreset,
                    selectedIntensiveCampus = state.wizardIntensiveCampus,
                    onEndpointChange = viewModel::onWizardEndpointChange,
                    onPresetSelected = viewModel::selectWizardEndpointPreset,
                    onIntensiveCampusSelected = viewModel::selectWizardIntensiveCampus,
                )
            }
        }

        WizardInstructionBlock(
            credentialsBlob = state.wizardCredentialsBlob,
            onCredentialsBlobChange = viewModel::onWizardCredentialsBlobChange,
            tokensButtonEnabled = canOpenTokensPage,
            onOpenTokensPage = {
                if (canOpenTokensPage) {
                    uriHandler.openUri(rocketTokensUrl)
                } else {
                    Toast.makeText(context, tokensUrlMissingMessage, Toast.LENGTH_SHORT).show()
                }
            },
        )

        val extracted = remember(state.wizardCredentialsBlob) {
            RocketCredentialsExtractor.extract(state.wizardCredentialsBlob)
        }
        val detectedUserId = InputSanitizer.noWhitespace(extracted.userId.orEmpty())
        val detectedToken = InputSanitizer.noWhitespace(extracted.authToken.orEmpty())
        if (state.wizardCredentialsBlob.isNotBlank() && (detectedUserId.isNotBlank() || detectedToken.isNotBlank())) {
            DetectedCredentialBlock(
                userId = detectedUserId,
                token = detectedToken,
            )
        }

        WizardButtons(
            isCheckingConnection = state.isConnectionCheckInProgress,
            canProceed = state.wizardCredentialsBlob.isNotBlank(),
            onNext = viewModel::wizardNext,
        )
    }
}

@Composable
private fun WizardInstructionBlock(
    credentialsBlob: String,
    onCredentialsBlobChange: (String) -> Unit,
    tokensButtonEnabled: Boolean,
    onOpenTokensPage: () -> Unit,
) {
    var exampleExpanded by remember { mutableStateOf(false) }
    var exampleFullscreen by remember { mutableStateOf(false) }
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "2. ${stringResource(R.string.instruction_title)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            StepText(
                number = "1",
                text = stringResource(R.string.instruction_step_1),
                highlights = listOf("по кнопке ниже", "using the button below"),
            )
            StepText(
                number = "2",
                text = stringResource(R.string.instruction_step_2),
                highlights = listOf("@student.21-school.ru"),
            )
            TokenLinkButton(
                enabled = tokensButtonEnabled,
                onClick = onOpenTokensPage,
            )
            StepText(
                number = "3",
                text = stringResource(R.string.instruction_step_3),
                highlights = listOf("Add new Personal", "Add"),
            )
            StepText(
                number = "4",
                text = stringResource(R.string.instruction_step_4),
                highlights = listOf("введи пароль", "Enter your Rocket.Chat password"),
            )
            StepText(
                number = "5",
                text = stringResource(R.string.instruction_step_5),
                highlights = listOf("Token", "Id", "token", "user Id"),
            )
            TextButton(onClick = { exampleExpanded = !exampleExpanded }) {
                Text(
                    if (exampleExpanded) {
                        stringResource(R.string.instruction_hide_example)
                    } else {
                        stringResource(R.string.instruction_view_example)
                    },
                )
            }
            AnimatedVisibility(visible = exampleExpanded) {
                Image(
                    painter = painterResource(R.drawable.token_popup_example),
                    contentDescription = stringResource(R.string.instruction_view_example),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { exampleFullscreen = true },
                    contentScale = ContentScale.Fit,
                )
            }
            if (exampleFullscreen) {
                TokenExampleFullscreenDialog(onDismiss = { exampleFullscreen = false })
            }
            StepText(number = "6", text = stringResource(R.string.instruction_step_6))
            OutlinedTextField(
                value = credentialsBlob,
                onValueChange = onCredentialsBlobChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.credentials_blob_label)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE0A81D),
                    unfocusedBorderColor = Color(0xFF8A6A35).copy(alpha = 0.75f),
                    focusedLabelColor = Color(0xFFE0A81D),
                    cursorColor = Color(0xFFE0A81D),
                ),
                minLines = 4,
            )
        }
    }
}

@Composable
private fun TokenExampleFullscreenDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.token_popup_example),
                contentDescription = stringResource(R.string.instruction_view_example),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 560.dp, maxHeight = 760.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun StepText(
    number: String,
    text: String,
    highlights: List<String> = emptyList(),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = highlightedStepText(
                text = text.removePrefix("$number.").trim(),
                highlights = highlights,
                highlightColor = MaterialTheme.colorScheme.onSurface,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun highlightedStepText(
    text: String,
    highlights: List<String>,
    highlightColor: Color,
): AnnotatedString {
    if (highlights.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val next = highlights
                .mapNotNull { word ->
                    val found = text.indexOf(word, startIndex = index)
                    if (found >= 0) found to word else null
                }
                .minByOrNull { it.first }

            if (next == null) {
                append(text.substring(index))
                break
            }

            append(text.substring(index, next.first))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = highlightColor)) {
                append(next.second)
            }
            index = next.first + next.second.length
        }
    }
}

@Composable
private fun TokenLinkButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.alpha(if (enabled) 1f else 0.52f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (enabled) Color(0xFFE0A81D) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(stringResource(R.string.instruction_open_link))
        Box(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.ArrowOutward, contentDescription = null)
    }
}

@Composable
private fun EndpointSettingsBlock(
    endpoint: String,
    selectedPreset: EndpointPreset,
    selectedIntensiveCampus: IntensiveCampus,
    onEndpointChange: (String) -> Unit,
    onPresetSelected: (EndpointPreset) -> Unit,
    onIntensiveCampusSelected: (IntensiveCampus) -> Unit,
) {
    val showCustomEndpoint = selectedPreset == EndpointPreset.CUSTOM ||
        (selectedPreset == EndpointPreset.INTENSIVE && selectedIntensiveCampus == IntensiveCampus.OTHER)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EndpointSegmentedControl(
            selectedPreset = selectedPreset,
            intensiveEnabled = true,
            onPresetSelected = onPresetSelected,
        )

        AnimatedVisibility(visible = selectedPreset == EndpointPreset.INTENSIVE) {
            IntensiveCampusSelector(
                selectedCampus = selectedIntensiveCampus,
                onCampusSelected = onIntensiveCampusSelected,
            )
        }

            AnimatedVisibility(visible = showCustomEndpoint) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.endpoint_label)) },
                placeholder = { Text(stringResource(R.string.endpoint_other_campus_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
                Text(
                    text = stringResource(R.string.endpoint_custom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EndpointHintImages()
            }
        }
    }
}

@Composable
private fun IntensiveCampusSelector(
    selectedCampus: IntensiveCampus,
    onCampusSelected: (IntensiveCampus) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IntensiveCampus.entries.forEach { campus ->
            EndpointChip(
                text = when (campus) {
                    IntensiveCampus.MOSCOW -> stringResource(R.string.intensive_campus_moscow)
                    IntensiveCampus.NOVOSIBIRSK -> stringResource(R.string.intensive_campus_novosibirsk)
                    IntensiveCampus.KAZAN -> stringResource(R.string.intensive_campus_kazan)
                    IntensiveCampus.OTHER -> stringResource(R.string.intensive_campus_other)
                },
                selected = selectedCampus == campus,
                enabled = AndroidEndpointSecrets.isFixedCampusAvailable(campus),
                onClick = { onCampusSelected(campus) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EndpointHintImages() {
    var browserExpanded by remember { mutableStateOf(false) }
    var mobileExpanded by remember { mutableStateOf(false) }
    var fullscreenImage by remember { mutableStateOf<EndpointHintImage?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { browserExpanded = !browserExpanded }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.endpoint_view_browser_example), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { mobileExpanded = !mobileExpanded }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.endpoint_view_mobile_example), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedVisibility(visible = browserExpanded) {
            Image(
                painter = painterResource(R.drawable.endpoint_browser_hint),
                contentDescription = stringResource(R.string.endpoint_view_browser_example),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreenImage = EndpointHintImage.BROWSER },
                contentScale = ContentScale.Fit,
            )
        }
        AnimatedVisibility(visible = mobileExpanded) {
            Image(
                painter = painterResource(R.drawable.endpoint_mobile_hint),
                contentDescription = stringResource(R.string.endpoint_view_mobile_example),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreenImage = EndpointHintImage.MOBILE },
                contentScale = ContentScale.Fit,
            )
        }
    }
    fullscreenImage?.let { image ->
        EndpointHintFullscreenDialog(
            image = image,
            onDismiss = { fullscreenImage = null },
        )
    }
}

private enum class EndpointHintImage {
    BROWSER,
    MOBILE,
}

@Composable
private fun EndpointHintFullscreenDialog(
    image: EndpointHintImage,
    onDismiss: () -> Unit,
) {
    val contentDescription = when (image) {
        EndpointHintImage.BROWSER -> stringResource(R.string.endpoint_view_browser_example)
        EndpointHintImage.MOBILE -> stringResource(R.string.endpoint_view_mobile_example)
    }
    val painter = when (image) {
        EndpointHintImage.BROWSER -> painterResource(R.drawable.endpoint_browser_hint)
        EndpointHintImage.MOBILE -> painterResource(R.drawable.endpoint_mobile_hint)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 760.dp, maxHeight = 820.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun EndpointSegmentedControl(
    selectedPreset: EndpointPreset,
    intensiveEnabled: Boolean,
    onPresetSelected: (EndpointPreset) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp),
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EndpointSegment(
                text = stringResource(R.string.endpoint_preset_base),
                selected = selectedPreset == EndpointPreset.BASE,
                enabled = true,
                onClick = { onPresetSelected(EndpointPreset.BASE) },
                modifier = Modifier.weight(1f),
            )
            EndpointSegment(
                text = stringResource(R.string.endpoint_preset_intensive),
                selected = selectedPreset == EndpointPreset.INTENSIVE,
                enabled = intensiveEnabled,
                onClick = { onPresetSelected(EndpointPreset.INTENSIVE) },
                modifier = Modifier.weight(1f),
            )
            EndpointSegment(
                text = stringResource(R.string.endpoint_preset_custom),
                selected = selectedPreset == EndpointPreset.CUSTOM,
                enabled = true,
                onClick = { onPresetSelected(EndpointPreset.CUSTOM) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EndpointSegment(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "endpointSegmentContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        label = "endpointSegmentContent",
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
private fun EndpointChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        label = "endpointChipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        label = "endpointChipContent",
    )

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
private fun DetectedCredentialBlock(
    userId: String,
    token: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.detected_credentials_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.detected_user_id_value,
                    userId.ifBlank { stringResource(R.string.status_not_found) },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.detected_token_value,
                    if (token.isBlank()) {
                        stringResource(R.string.status_not_found)
                    } else {
                        token.takeLast(6).padStart(token.length, '•')
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WizardButtons(
    isCheckingConnection: Boolean,
    canProceed: Boolean,
    onNext: () -> Unit,
) {
    val enabled = !isCheckingConnection && canProceed
    val buttonShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = buttonShape,
        color = if (enabled) Color.Transparent else Color(0xFF4C453B),
    ) {
        Button(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFF8D66A), Color(0xFFE0A81D))))
                    } else {
                        Modifier
                    },
                ),
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF33260F),
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color(0xFFD6C8B2),
            ),
        ) {
            Text(
                if (isCheckingConnection) {
                    stringResource(R.string.connection_checking)
                } else {
                    stringResource(R.string.action_next)
                },
            )
        }
    }
}

@Composable
private fun SettingsDashboard(
    state: MainUiState,
    viewModel: MainViewModel,
    refreshCooldownNowMs: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatusCard(
            state = state,
            onOpenUpdate = viewModel::openUpdateDialog,
        )
        if (state.lastError == SyncError.UNAUTHORIZED) {
            TokenRecoveryCard(
                onResetData = viewModel::clearAllData,
            )
        }
        QrCard(
            state = state,
            refreshCooldownNowMs = refreshCooldownNowMs,
            onRefresh = viewModel::refreshNow,
            onAutoRefreshEnabledChange = viewModel::setAutoRefreshEnabled,
            onMaxBrightnessEnabledChange = viewModel::setMaxBrightnessEnabled,
        )
        WidgetInstallCard()
        UpdateCenterCard(
            state = state.update,
            onAutomaticChecksChange = viewModel::setAutomaticUpdateChecksEnabled,
            onCheck = viewModel::checkForUpdates,
            onInstall = viewModel::requestUpdateInstallation,
        )
        ClearDataCard(onClearData = viewModel::clearAllData)
        CreditsCard()
        DonateCard()
    }
}

@Composable
private fun UpdateCenterCard(
    state: UpdateUiState,
    onAutomaticChecksChange: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
) {
    val isBusy = state.status == UpdateStatus.CHECKING || state.status == UpdateStatus.DOWNLOADING
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val availableColor = if (dark) Color(0xFFF2C64D) else Color(0xFFB77900)

    DashboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_center_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.release?.let { release ->
                    Text(
                        text = stringResource(R.string.update_available_inline, release.tag.removePrefix("v")),
                        style = MaterialTheme.typography.bodySmall,
                        color = availableColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    )
                } ?: if (state.status == UpdateStatus.UP_TO_DATE) {
                    Text(
                        text = stringResource(R.string.update_current),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                    )
                } else Unit
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_auto_check),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.update_auto_check_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.automaticChecksEnabled,
                    onCheckedChange = onAutomaticChecksChange,
                    enabled = !isBusy,
                )
            }

            if (state.status == UpdateStatus.CHECKING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodySmall)
            }

            if (state.status == UpdateStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { state.downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.update_downloading, state.downloadProgress),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.release != null && !isBusy) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = DashboardActionButtonShape,
                ) {
                    Text(stringResource(R.string.update_install), textAlign = TextAlign.Center)
                }
            }

            if (state.release == null && !state.automaticChecksEnabled && !isBusy) {
                OutlinedButton(
                    onClick = onCheck,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = DashboardActionButtonShape,
                ) {
                    Text(stringResource(R.string.update_check_now), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onRetry: () -> Unit,
) {
    val release = state.release
    val changelogItems = remember(release?.changelog) { release?.changelogItems().orEmpty() }
    val errorText = state.message?.let { message ->
        stringResource(
            when (message) {
                UpdateMessage.CHECK_FAILED -> R.string.update_check_failed
                UpdateMessage.DOWNLOAD_FAILED -> R.string.update_download_failed
                UpdateMessage.DOWNLOAD_INTEGRITY_FAILED -> R.string.update_integrity_failed
                UpdateMessage.INSTALL_FAILED -> R.string.update_install_failed
                UpdateMessage.INSTALL_PERMISSION_REQUIRED -> R.string.update_install_permission_required
                UpdateMessage.APK_SIGNATURE_MISMATCH -> R.string.update_signature_mismatch
                UpdateMessage.APK_INCOMPATIBLE -> R.string.update_apk_incompatible
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 720.dp)
                    .clickable { },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.update_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            release?.let {
                                Text(
                                    text = stringResource(R.string.update_available, it.tag.removePrefix("v")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.update_close))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (changelogItems.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.update_changes_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            changelogItems.forEach { item ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text("•", color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        if (state.status == UpdateStatus.DOWNLOADING) {
                            LinearProgressIndicator(
                                progress = { state.downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(R.string.update_downloading, state.downloadProgress),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (state.status == UpdateStatus.CHECKING) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.update_checking),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        errorText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.message == UpdateMessage.INSTALL_PERMISSION_REQUIRED) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when {
                            state.waitingForInstallPermission -> Button(
                                onClick = onOpenInstallPermission,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = DashboardActionButtonShape,
                            ) {
                                Text(stringResource(R.string.update_allow_installation), textAlign = TextAlign.Center)
                            }
                            state.status == UpdateStatus.READY_TO_INSTALL -> Button(
                                onClick = onInstall,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = DashboardActionButtonShape,
                            ) {
                                Text(stringResource(R.string.update_install), textAlign = TextAlign.Center)
                            }
                            state.status == UpdateStatus.DOWNLOADING -> Unit
                            state.message?.canRetryUpdate() == true -> Button(
                                onClick = onRetry,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = DashboardActionButtonShape,
                            ) {
                                Text(stringResource(R.string.update_retry), textAlign = TextAlign.Center)
                            }
                            release != null && state.message == null && state.status != UpdateStatus.CHECKING -> Button(
                                onClick = onDownload,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = DashboardActionButtonShape,
                            ) {
                                Text(stringResource(R.string.update_download), textAlign = TextAlign.Center)
                            }
                        }

                        OutlinedButton(
                            onClick = if (state.status == UpdateStatus.DOWNLOADING) onCancelDownload else onDismiss,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = DashboardActionButtonShape,
                        ) {
                            Text(
                                text = stringResource(
                                    when {
                                        state.status == UpdateStatus.DOWNLOADING -> R.string.update_cancel_download
                                        state.message == UpdateMessage.CHECK_FAILED && release == null -> R.string.update_close
                                        state.message == UpdateMessage.APK_SIGNATURE_MISMATCH -> R.string.update_close
                                        state.message == UpdateMessage.APK_INCOMPATIBLE -> R.string.update_close
                                        else -> R.string.update_later
                                    },
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearDataCard(onClearData: () -> Unit) {
    var confirmVisible by remember { mutableStateOf(false) }

    DashboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.clear_data_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.clear_data_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (confirmVisible) {
                Text(
                    text = stringResource(R.string.clear_data_confirm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(
                    onClick = onClearData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DashboardActionButtonHeight),
                    shape = DashboardActionButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.56f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    ),
                ) {
                    Text(
                        stringResource(R.string.action_clear_all_data_confirm),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { confirmVisible = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DashboardActionButtonHeight),
                    shape = DashboardActionButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.56f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    ),
                ) {
                    Text(
                        stringResource(R.string.action_clear_all_data),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: MainUiState,
    onOpenUpdate: () -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    val readiness = state.qrReadiness()
    val hasUpdate = state.update.release != null
    val headline = when {
        hasUpdate && state.update.status == UpdateStatus.DOWNLOADING -> stringResource(
            R.string.update_status_downloading,
            state.update.downloadProgress,
        )
        hasUpdate && state.update.status == UpdateStatus.READY_TO_INSTALL -> stringResource(R.string.update_status_ready)
        hasUpdate -> stringResource(
            R.string.update_status_available,
            state.update.release.tag.removePrefix("v"),
        )
        state.syncInProgress -> stringResource(R.string.status_qr_refreshing)
        readiness == QrReadiness.READY -> stringResource(R.string.status_qr_fresh)
        readiness == QrReadiness.CHECK_REQUIRED -> stringResource(R.string.status_qr_check_required)
        readiness == QrReadiness.EXPIRED -> stringResource(R.string.status_qr_expired)
        else -> stringResource(R.string.status_qr_missing)
    }
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val palette = if (hasUpdate) {
        StatusPalette(
            container = if (dark) Color(0xFF332612) else Color(0xFFFFF2D3),
            border = if (dark) Color(0xFF6A5123) else Color(0xFFEACD8A),
            badge = if (dark) Color(0xFFF2C64D) else Color(0xFFF5D68A),
            headline = if (dark) Color(0xFFFFE7B5) else Color(0xFF6F4700),
            symbolContainer = Color.Transparent,
            symbol = if (dark) Color(0xFF51360A) else Color(0xFF7A4A00),
        )
    } else when (readiness) {
        QrReadiness.READY -> StatusPalette(
            container = if (dark) Color(0xFF1F2A1D) else Color(0xFFF3FBEF),
            border = if (dark) Color(0xFF355B34) else Color(0xFFB8DBA9),
            badge = if (dark) Color(0xFF88D89F) else Color(0xFFCBEFBC),
            headline = if (dark) Color(0xFFA6F0BA) else Color(0xFF247A39),
            symbolContainer = Color.Transparent,
            symbol = if (dark) Color(0xFF153A20) else Color(0xFF1F5C2F),
        )
        QrReadiness.CHECK_REQUIRED -> StatusPalette(
            container = if (dark) Color(0xFF332612) else Color(0xFFFFF2D3),
            border = if (dark) Color(0xFF6A5123) else Color(0xFFEACD8A),
            badge = if (dark) Color(0xFFF2C64D) else Color(0xFFF5D68A),
            headline = if (dark) Color(0xFFFFE7B5) else Color(0xFF6F4700),
            symbolContainer = Color.Transparent,
            symbol = if (dark) Color(0xFF51360A) else Color(0xFF7A4A00),
        )
        else -> StatusPalette(
            container = if (dark) Color(0xFF331E1E) else Color(0xFFFDEAEA),
            border = if (dark) Color(0xFF6A3636) else Color(0xFFE5AAAA),
            badge = if (dark) Color(0xFFDF8C8C) else Color(0xFFF2B9B9),
            headline = if (dark) Color(0xFFFFC8C8) else Color(0xFF9C2F2F),
            symbolContainer = Color.Transparent,
            symbol = if (dark) Color(0xFF5A2222) else Color(0xFF8D2525),
        )
    }
    val textColor = if (dark) Color(0xFFF3EBDD) else Color(0xFF4E4335)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasUpdate) Modifier.clickable(onClick = onOpenUpdate) else Modifier),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container),
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = palette.badge,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                hasUpdate && state.update.status == UpdateStatus.DOWNLOADING -> Icons.Filled.Download
                                hasUpdate && state.update.status == UpdateStatus.READY_TO_INSTALL -> Icons.Filled.InstallMobile
                                hasUpdate -> Icons.Filled.SystemUpdateAlt
                                readiness == QrReadiness.READY -> Icons.Filled.CheckCircle
                                readiness == QrReadiness.CHECK_REQUIRED -> Icons.Filled.Refresh
                                else -> Icons.Filled.Close
                            },
                            contentDescription = null,
                            tint = palette.symbol,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    headline,
                    modifier = Modifier.weight(1f),
                    color = palette.headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.status_toggle_details),
                        tint = textColor,
                    )
                }
            }
            if (detailsExpanded) {
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
                if (state.autoRefreshEnabled && state.nextAutoRefreshAtMs != null) {
                    Text(
                        stringResource(R.string.status_next_auto_refresh, state.nextAutoRefreshAtMs.formatDateTime()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                } else if (!state.autoRefreshEnabled) {
                    Text(
                        stringResource(R.string.status_auto_refresh_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
                if (state.lastError != null) {
                    Text(
                        stringResource(R.string.status_warning, state.lastError.toDisplayString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

private data class StatusPalette(
    val container: Color,
    val border: Color,
    val badge: Color,
    val headline: Color,
    val symbolContainer: Color,
    val symbol: Color,
)

@Composable
private fun TokenRecoveryCard(
    onResetData: () -> Unit,
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.token_recovery_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.token_recovery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onResetData,
                modifier = Modifier.fillMaxWidth(),
                shape = DashboardActionButtonShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.56f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                ),
            ) {
                Text(stringResource(R.string.token_recovery_action))
            }
        }
    }
}

@Composable
private fun QrCard(
    state: MainUiState,
    refreshCooldownNowMs: Long,
    onRefresh: () -> Unit,
    onAutoRefreshEnabledChange: (Boolean) -> Unit,
    onMaxBrightnessEnabledChange: (Boolean) -> Unit,
) {
    val bitmap = remember(state.qrImagePath) {
        state.qrImagePath?.let { BitmapFactory.decodeFile(it) }
    }
    val isManualRefreshBlocked = SyncPolicy.isManualRefreshBlocked(
        state.manualRefreshBlockedUntilMs,
        refreshCooldownNowMs,
    )
    val isIntensiveEndpoint = state.endpointPreset == EndpointPreset.INTENSIVE ||
        AndroidEndpointSecrets.isIntensiveEndpoint(state.endpoint)

    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.app_qr_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

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
                        Text(
                            text = stringResource(R.string.app_qr_empty),
                            color = Color(0xFF465062),
                        )
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
                enabled = !state.syncInProgress && !isManualRefreshBlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DashboardActionButtonHeight),
                shape = DashboardActionButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Box(modifier = Modifier.width(10.dp))
                Text(
                    stringResource(R.string.manual_refresh),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (!isIntensiveEndpoint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.auto_refresh_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.auto_refresh_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.autoRefreshEnabled,
                        onCheckedChange = onAutoRefreshEnabledChange,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.max_brightness_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.max_brightness_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.maxBrightnessEnabled,
                    onCheckedChange = onMaxBrightnessEnabledChange,
                )
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
                        io.github.vgy789.doorDuck.model.ConnectionCheckResult.BOT_NOT_FOUND -> stringResource(R.string.connection_bot_not_found)
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
                if (state.autoRefreshEnabled && state.nextAutoRefreshAtMs != null) {
                    Text(
                        text = stringResource(R.string.status_next_auto_refresh, state.nextAutoRefreshAtMs.formatDateTime()),
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
            Text(
                text = stringResource(R.string.widget_install_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.widget_install_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DashboardActionButtonHeight),
                shape = DashboardActionButtonShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFFFE7B5) else Color(0xFF6F4700),
                ),
            ) {
                Text(
                    stringResource(R.string.widget_install_action),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HelpCard(compact: Boolean = false) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val url = Defaults.rocketTokensUrl
    val missingUrlMessage = stringResource(R.string.error_tokens_url_missing)

    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.instruction_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.token_help_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.instruction_step_1), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_2), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_3), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_4), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_5), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.instruction_step_6), style = MaterialTheme.typography.bodySmall)

            if (!compact) {
                TextButton(
                    onClick = {
                        if (url.isNotBlank()) {
                            uriHandler.openUri(url)
                        } else {
                            Toast.makeText(context, missingUrlMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.instruction_open_link))
                    Box(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun CreditsCard() {
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(R.string.github_url)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { uriHandler.openUri(githubUrl) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFE8B12C),
                )
                Box(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.instruction_github_link),
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
                    text = stringResource(R.string.instruction_s21_login),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DonateCard() {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val primaryTextColor = if (isDark) Color(0xFFF8F1E4) else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDark) Color(0xFFD7C8B2) else MaterialTheme.colorScheme.onSurfaceVariant
    val copyContainerColor = if (isDark) Color(0xFF2B241B) else Color(0xFFFFF8EB)
    val copyBorderColor = if (isDark) Color(0xFF5B4835) else Color(0xFFE8C98E)
    val copyValueColor = if (isDark) Color(0xFFFFE4B5) else Color(0xFF6D4700)
    val phoneValue = Defaults.donatePhoneValue
    val cardValue = Defaults.donateCardValue
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "donateArrowRotation",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = { expanded = !expanded },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.donate_toggle_text),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor,
                )
                Box(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer(rotationZ = arrowRotation),
                    tint = secondaryTextColor,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.donate_transfer_sbp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CopyValuePill(
                        label = stringResource(R.string.donate_phone_label),
                        value = phoneValue,
                        containerColor = copyContainerColor,
                        borderColor = copyBorderColor,
                        labelColor = secondaryTextColor,
                        valueColor = copyValueColor,
                        onClick = {
                            copyToClipboard(context, "donate_phone", phoneValue)
                        },
                    )
                    CopyValuePill(
                        label = stringResource(R.string.donate_card_label),
                        value = cardValue,
                        containerColor = copyContainerColor,
                        borderColor = copyBorderColor,
                        labelColor = secondaryTextColor,
                        valueColor = copyValueColor,
                        onClick = {
                            copyToClipboard(context, "donate_card", cardValue)
                        },
                    )
                    Text(
                        text = stringResource(R.string.donate_recipient),
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.donate_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyValuePill(
    label: String,
    value: String,
    containerColor: Color,
    borderColor: Color,
    labelColor: Color,
    valueColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = valueColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = valueColor,
            )
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
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
        SyncError.RATE_LIMITED -> stringResource(R.string.sync_error_rate_limited)
        SyncError.NETWORK -> stringResource(R.string.sync_error_network)
        SyncError.BOT_NOT_FOUND -> stringResource(R.string.connection_bot_not_found)
        SyncError.BOT_RESPONSE_INVALID -> stringResource(R.string.sync_error_bot_response_invalid)
        SyncError.IMAGE_DOWNLOAD_FAILED -> stringResource(R.string.sync_error_image_download_failed)
        SyncError.UNKNOWN -> stringResource(R.string.sync_error_unknown)
    }
}
