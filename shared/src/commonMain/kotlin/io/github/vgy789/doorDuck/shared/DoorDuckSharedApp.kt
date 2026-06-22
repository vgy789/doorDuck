package io.github.vgy789.doorDuck.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.IntensiveCampus
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.platform.DoorDuckPlatformServices
import io.github.vgy789.doorDuck.platform.PersistedDoorDuckState
import io.github.vgy789.doorDuck.platform.PlatformQrRefreshResult
import io.github.vgy789.doorDuck.platform.formatEpochMillis
import io.github.vgy789.doorDuck.platform.isValidQrImageBase64
import io.github.vgy789.doorDuck.ui.InputSanitizer
import io.github.vgy789.doorDuck.ui.RocketCredentialsExtractor
import io.github.vgy789.doorDuck.ui.WizardStep
import kotlinx.coroutines.launch

private enum class ScreenMode {
    WIZARD,
    HOME,
    SETTINGS,
}

private enum class SharedEndpointPreset {
    BASE,
    INTENSIVE,
    CUSTOM,
}

@Composable
fun DoorDuckSharedApp() {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFFF2C64D),
                secondary = Color(0xFFE88C3A),
                tertiary = Color(0xFF8FD6A0),
                background = Color(0xFF17130F),
                surface = Color(0xFF231C15),
                onPrimary = Color(0xFF2D220F),
                onSurface = Color(0xFFF9F0E1),
                onSurfaceVariant = Color(0xFFD4C5AF),
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFE0A81D),
                secondary = Color(0xFFE88C3A),
                tertiary = Color(0xFF5DAE77),
                background = Color(0xFFFFFBF3),
                surface = Color(0xFFFFFEFB),
                onPrimary = Color(0xFF33260F),
                onSurface = Color(0xFF2D2418),
                onSurfaceVariant = Color(0xFF6F5E47),
            )
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DoorDuckSharedScreen()
        }
    }
}

@Composable
private fun DoorDuckSharedScreen() {
    val strings = SharedStrings.forLanguage(DoorDuckPlatformServices.currentLanguageCode())
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var initialized by remember { mutableStateOf(false) }
    var screenMode by remember { mutableStateOf(ScreenMode.WIZARD) }
    var wizardStep by remember { mutableStateOf(WizardStep.CREDENTIALS) }
    var endpoint by remember { mutableStateOf(Defaults.defaultEndpoint) }
    var endpointPreset by remember { mutableStateOf(SharedEndpointPreset.BASE) }
    var wizardEndpoint by remember { mutableStateOf(Defaults.defaultEndpoint) }
    var wizardEndpointPreset by remember { mutableStateOf(SharedEndpointPreset.BASE) }
    var wizardIntensiveCampus by remember { mutableStateOf(IntensiveCampus.MOSCOW) }
    var wizardCredentialsBlob by remember { mutableStateOf("") }
    var wizardUserId by remember { mutableStateOf("") }
    var wizardToken by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var hasStoredCredentials by remember { mutableStateOf(false) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    var isRefreshingQr by remember { mutableStateOf(false) }
    var lastConnectionResult by remember { mutableStateOf<ConnectionCheckResult?>(null) }
    var connectionCheckPassed by remember { mutableStateOf(false) }
    var lastSuccessAtMs by remember { mutableStateOf<Long?>(null) }
    var expiresAtMs by remember { mutableStateOf<Long?>(null) }
    var manualRefreshBlockedUntilMs by remember { mutableStateOf<Long?>(null) }
    var lastSyncError by remember { mutableStateOf<SyncError?>(null) }
    var qrImageBase64 by remember { mutableStateOf<String?>(null) }
    var imageValidationStatus by remember { mutableStateOf(QrImageValidationStatus.UNKNOWN) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val stored = DoorDuckPlatformServices.loadPersistedState()
        if (stored != null) {
            endpoint = stored.endpoint
            endpointPreset = endpoint.toSharedEndpointPreset()
            wizardEndpoint = endpoint
            wizardEndpointPreset = endpointPreset
            wizardIntensiveCampus = endpoint.toIntensiveCampus()
            userId = stored.userId
            token = stored.authToken
            lastSuccessAtMs = stored.lastSuccessAtMs
            expiresAtMs = stored.expiresAtMs
            manualRefreshBlockedUntilMs = stored.manualRefreshBlockedUntilMs
            lastConnectionResult = stored.lastConnectionResult
            lastSyncError = stored.lastSyncError
            qrImageBase64 = stored.qrImageBase64
            imageValidationStatus = stored.imageValidationStatus
            connectionCheckPassed = stored.lastConnectionResult == ConnectionCheckResult.SUCCESS
            hasStoredCredentials = stored.userId.isNotBlank() && stored.authToken.isNotBlank()
            screenMode = if (hasStoredCredentials) ScreenMode.HOME else ScreenMode.WIZARD
            wizardStep = if (hasStoredCredentials) WizardStep.DONE else WizardStep.CREDENTIALS
        }
        initialized = true
    }

    if (!initialized) {
        LoadingScreen(strings.loading)
        return
    }

    fun resetValidation() {
        connectionCheckPassed = false
        lastConnectionResult = null
    }

    fun onWizardCredentialsBlobChange(value: String) {
        val extracted = RocketCredentialsExtractor.extract(value)
        wizardCredentialsBlob = value.trim()
        wizardUserId = InputSanitizer.noWhitespace(extracted.userId.orEmpty())
        wizardToken = InputSanitizer.noWhitespace(extracted.authToken.orEmpty())
        resetValidation()
    }

    fun saveLocalState(result: ConnectionCheckResult?) {
        scope.launch {
            DoorDuckPlatformServices.savePersistedState(
                PersistedDoorDuckState(
                    endpoint = endpoint,
                    authToken = token,
                    userId = userId,
                    lastSuccessAtMs = lastSuccessAtMs,
                    expiresAtMs = expiresAtMs,
                    manualRefreshBlockedUntilMs = manualRefreshBlockedUntilMs,
                    lastConnectionResult = result,
                    lastSyncError = lastSyncError,
                    qrImageBase64 = qrImageBase64,
                    imageValidationStatus = imageValidationStatus,
                ),
            )
        }
    }

    fun runCheckConnection(
        candidateEndpoint: String = endpoint,
        candidateToken: String = token,
        candidateUserId: String = userId,
        applyOnSuccess: Boolean = true,
        onSuccess: () -> Unit = {},
    ) {
        scope.launch {
            val normalizedEndpoint = InputSanitizer.endpoint(candidateEndpoint)
            val normalizedToken = InputSanitizer.noWhitespace(candidateToken)
            val normalizedUserId = InputSanitizer.noWhitespace(candidateUserId)

            if (!normalizedEndpoint.startsWith("https://")) {
                infoMessage = strings.errorEndpointHttps
                return@launch
            }
            if (normalizedUserId.isBlank() && normalizedToken.isBlank()) {
                infoMessage = strings.errorCredentialsBlobRequired
                return@launch
            }
            if (normalizedUserId.isBlank()) {
                infoMessage = strings.errorUserIdRequired
                return@launch
            }
            if (normalizedToken.isBlank()) {
                infoMessage = strings.errorTokenRequired
                return@launch
            }

            isCheckingConnection = true
            infoMessage = null
            val result = DoorDuckPlatformServices.verifyCredentials(
                endpoint = normalizedEndpoint,
                credentials = Credentials(authToken = normalizedToken, userId = normalizedUserId),
            )
            isCheckingConnection = false
            lastConnectionResult = result
            connectionCheckPassed = result == ConnectionCheckResult.SUCCESS

            if (result == ConnectionCheckResult.SUCCESS) {
                if (applyOnSuccess) {
                    endpoint = normalizedEndpoint
                    endpointPreset = endpoint.toSharedEndpointPreset()
                    userId = normalizedUserId
                    token = normalizedToken
                }
                lastSuccessAtMs = DoorDuckPlatformServices.currentTimeMillis()
                hasStoredCredentials = true
                lastSyncError = null
                saveLocalState(result)
                onSuccess()
            } else {
                infoMessage = strings.connectionMessage(result)
            }
        }
    }

    fun refreshQrNow() {
        scope.launch {
            val normalizedEndpoint = InputSanitizer.endpoint(endpoint)
            val normalizedToken = InputSanitizer.noWhitespace(token)
            val normalizedUserId = InputSanitizer.noWhitespace(userId)

            endpoint = normalizedEndpoint
            token = normalizedToken
            userId = normalizedUserId

            if (!normalizedEndpoint.startsWith("https://")) {
                infoMessage = strings.errorEndpointHttps
                return@launch
            }
            if (normalizedUserId.isBlank()) {
                infoMessage = strings.errorUserIdRequired
                return@launch
            }
            if (normalizedToken.isBlank()) {
                infoMessage = strings.errorTokenRequired
                return@launch
            }
            val nowMs = DoorDuckPlatformServices.currentTimeMillis()
            if (isRefreshingQr || SyncPolicy.isManualRefreshBlocked(manualRefreshBlockedUntilMs, nowMs)) {
                infoMessage = strings.infoRefreshShortCooldown
                return@launch
            }

            manualRefreshBlockedUntilMs = SyncPolicy.nextManualRefreshAllowedAt(nowMs)
            saveLocalState(lastConnectionResult)
            isRefreshingQr = true
            infoMessage = null
            when (
                val result = DoorDuckPlatformServices.refreshQrCode(
                    endpoint = normalizedEndpoint,
                    credentials = Credentials(authToken = normalizedToken, userId = normalizedUserId),
                )
            ) {
                is PlatformQrRefreshResult.Success -> {
                    qrImageBase64 = result.qrImageBase64
                    imageValidationStatus = if (isValidQrImageBase64(result.qrImageBase64)) {
                        QrImageValidationStatus.VALID
                    } else {
                        QrImageValidationStatus.INVALID
                    }
                    lastSuccessAtMs = result.receivedAtMs
                    expiresAtMs = result.expiresAtMs
                    lastSyncError = null
                    lastConnectionResult = ConnectionCheckResult.SUCCESS
                    connectionCheckPassed = true
                    hasStoredCredentials = true
                    saveLocalState(lastConnectionResult)
                    infoMessage = strings.infoQrRefreshed
                }

                is PlatformQrRefreshResult.Failure -> {
                    lastSyncError = result.error
                    saveLocalState(lastConnectionResult)
                    infoMessage = strings.syncErrorMessage(result.error)
                }

                PlatformQrRefreshResult.NotConfigured -> {
                    lastSyncError = SyncError.NOT_CONFIGURED
                    saveLocalState(lastConnectionResult)
                    infoMessage = strings.syncErrorMessage(lastSyncError)
                }
            }
            isRefreshingQr = false
        }
    }

    fun switchToHome(message: String? = null) {
        screenMode = ScreenMode.HOME
        wizardStep = WizardStep.DONE
        wizardEndpoint = endpoint
        wizardEndpointPreset = endpointPreset
        wizardIntensiveCampus = endpoint.toIntensiveCampus()
        wizardCredentialsBlob = ""
        wizardUserId = ""
        wizardToken = ""
        infoMessage = message
    }

    fun switchToSettings(message: String? = null) {
        screenMode = ScreenMode.SETTINGS
        wizardStep = WizardStep.DONE
        infoMessage = message
    }

    fun finishWizard() {
        runCheckConnection(
            candidateEndpoint = wizardEndpoint,
            candidateToken = wizardToken,
            candidateUserId = wizardUserId,
        ) {
            switchToHome()
            if (qrImageBase64.isNullOrBlank()) {
                refreshQrNow()
            }
        }
    }

    fun selectEndpointPreset(preset: SharedEndpointPreset) {
        val intensiveCampus = if (hasFixedIntensiveEndpoints()) {
            IntensiveCampus.MOSCOW
        } else {
            IntensiveCampus.OTHER
        }
        val selectedEndpoint = when (preset) {
            SharedEndpointPreset.BASE -> Defaults.baseEndpoint
            SharedEndpointPreset.INTENSIVE -> intensiveCampus.endpointIfAvailable() ?: Defaults.baseEndpoint
            SharedEndpointPreset.CUSTOM -> ""
        }
        wizardEndpoint = selectedEndpoint
        wizardEndpointPreset = preset
        if (preset == SharedEndpointPreset.INTENSIVE) {
            wizardIntensiveCampus = intensiveCampus
        }
        resetValidation()
    }

    fun selectIntensiveCampus(campus: IntensiveCampus) {
        wizardEndpoint = campus.endpointIfAvailable().orEmpty()
        wizardEndpointPreset = SharedEndpointPreset.INTENSIVE
        wizardIntensiveCampus = campus
        resetValidation()
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(screenMode) {
        scrollState.animateScrollTo(0)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(doorDuckBackgroundBrush())
                .padding(paddingValues)
                .padding(20.dp)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DoorDuckHeader(
                strings = strings,
                actionLabel = null,
                onAction = null,
            )

            when (screenMode) {
                ScreenMode.WIZARD -> {
                    WizardCard(
                        strings = strings,
                        endpoint = wizardEndpoint,
                        selectedEndpointPreset = wizardEndpointPreset,
                        selectedIntensiveCampus = wizardIntensiveCampus,
                        wizardCredentialsBlob = wizardCredentialsBlob,
                        userId = wizardUserId,
                        token = wizardToken,
                        isCheckingConnection = isCheckingConnection,
                        onEndpointChange = {
                            wizardEndpoint = it
                            wizardEndpointPreset = if (wizardEndpointPreset == SharedEndpointPreset.INTENSIVE) {
                                SharedEndpointPreset.INTENSIVE
                            } else {
                                SharedEndpointPreset.CUSTOM
                            }
                            resetValidation()
                        },
                        onEndpointPresetSelected = ::selectEndpointPreset,
                        onIntensiveCampusSelected = ::selectIntensiveCampus,
                        onWizardCredentialsBlobChange = ::onWizardCredentialsBlobChange,
                        onOpenTokensPage = {
                            val url = InputSanitizer.tokensPageUrl(wizardEndpoint)
                            if (url.isNotBlank()) {
                                uriHandler.openUri(url)
                            }
                        },
                        onNext = ::finishWizard,
                    )
                }

                ScreenMode.HOME -> {
                    HomeDashboardScreen(
                        strings = strings,
                        endpoint = endpoint,
                        userId = userId,
                        token = token,
                        lastSuccessAtMs = lastSuccessAtMs,
                        expiresAtMs = expiresAtMs,
                        lastConnectionResult = lastConnectionResult,
                        lastSyncError = lastSyncError,
                        qrImageBase64 = qrImageBase64,
                        imageValidationStatus = imageValidationStatus,
                        isCheckingConnection = isCheckingConnection,
                        isRefreshingQr = isRefreshingQr,
                        onEndpointChange = {
                            endpoint = it
                            resetValidation()
                        },
                        onUserIdChange = {
                            userId = InputSanitizer.noWhitespace(it)
                            resetValidation()
                        },
                        onTokenChange = {
                            token = InputSanitizer.noWhitespace(it)
                            resetValidation()
                        },
                        onSave = {
                            saveLocalState(lastConnectionResult)
                            infoMessage = strings.infoSettingsSaved
                        },
                        onCheckConnection = {
                            runCheckConnection {
                                infoMessage = strings.infoSettingsSaved
                            }
                        },
                        widgetInfoMessage = infoMessage.takeIf { it == strings.widgetHelpMessage },
                        onRefreshQr = { refreshQrNow() },
                        onOpenTokensPage = {
                            val url = InputSanitizer.tokensPageUrl(endpoint)
                            if (url.isNotBlank()) {
                                uriHandler.openUri(url)
                            }
                        },
                        onOpenGithubPage = { uriHandler.openUri(strings.githubUrl) },
                        onWidgetAction = { infoMessage = strings.widgetHelpMessage },
                    )
                }

                ScreenMode.SETTINGS -> {
                    SettingsDashboardScreen(
                        strings = strings,
                        endpoint = endpoint,
                        userId = userId,
                        token = token,
                        lastSuccessAtMs = lastSuccessAtMs,
                        expiresAtMs = expiresAtMs,
                        lastConnectionResult = lastConnectionResult,
                        lastSyncError = lastSyncError,
                        qrImageBase64 = qrImageBase64,
                        imageValidationStatus = imageValidationStatus,
                        isCheckingConnection = isCheckingConnection,
                        isRefreshingQr = isRefreshingQr,
                        connectionExpanded = true,
                        onToggleConnectionExpanded = {},
                        onEndpointChange = {
                            endpoint = it
                            resetValidation()
                        },
                        onUserIdChange = {
                            userId = InputSanitizer.noWhitespace(it)
                            resetValidation()
                        },
                        onTokenChange = {
                            token = InputSanitizer.noWhitespace(it)
                            resetValidation()
                        },
                        onOpenTokensPage = {
                            val url = InputSanitizer.tokensPageUrl(endpoint)
                            if (url.isNotBlank()) {
                                uriHandler.openUri(url)
                            }
                        },
                        onOpenGithubPage = { uriHandler.openUri(strings.githubUrl) },
                        onSave = {
                            saveLocalState(lastConnectionResult)
                            infoMessage = strings.infoSettingsSaved
                        },
                        onCheckConnection = {
                            runCheckConnection {
                                infoMessage = strings.infoSettingsSaved
                            }
                        },
                        widgetInfoMessage = infoMessage.takeIf { it == strings.widgetHelpMessage },
                        onRefreshQr = { refreshQrNow() },
                        onWidgetAction = { infoMessage = strings.widgetHelpMessage },
                    )
                }
            }

            infoMessage
                ?.takeUnless { it == strings.widgetHelpMessage }
                ?.let { InfoCard(message = it) }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(message)
        }
    }
}

@Composable
private fun HeroCard(strings: SharedStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0B5FFF), Color(0xFF3B82F6), Color(0xFFFF7A00)),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.appTitle, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text(
                strings.appSubtitle,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun WizardCard(
    strings: SharedStrings,
    endpoint: String,
    selectedEndpointPreset: SharedEndpointPreset,
    selectedIntensiveCampus: IntensiveCampus,
    wizardCredentialsBlob: String,
    userId: String,
    token: String,
    isCheckingConnection: Boolean,
    onEndpointChange: (String) -> Unit,
    onEndpointPresetSelected: (SharedEndpointPreset) -> Unit,
    onIntensiveCampusSelected: (IntensiveCampus) -> Unit,
    onWizardCredentialsBlobChange: (String) -> Unit,
    onOpenTokensPage: () -> Unit,
    onNext: () -> Unit,
) {
    val extracted = remember(wizardCredentialsBlob) {
        RocketCredentialsExtractor.extract(wizardCredentialsBlob)
    }
    val detectedUserId = InputSanitizer.noWhitespace(extracted.userId.orEmpty())
    val detectedToken = InputSanitizer.noWhitespace(extracted.authToken.orEmpty())
    val detectedText = buildString {
        if (detectedUserId.isNotBlank()) {
            append(strings.detectedUserIdValue.replace("%s", detectedUserId))
        }
        if (detectedToken.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(strings.detectedTokenValue.replace("%s", detectedToken.takeLast(6).padStart(detectedToken.length, '•')))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            strings.wizardTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.welcomeBody,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "1. ${strings.endpointTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                EndpointSettingsBlock(
                    strings = strings,
                    endpoint = endpoint,
                    selectedPreset = selectedEndpointPreset,
                    selectedIntensiveCampus = selectedIntensiveCampus,
                    onEndpointChange = onEndpointChange,
                    onEndpointPresetSelected = onEndpointPresetSelected,
                    onIntensiveCampusSelected = onIntensiveCampusSelected,
                )
            }
        }

        WizardInstructionBlock(
            strings = strings,
            wizardCredentialsBlob = wizardCredentialsBlob,
            onWizardCredentialsBlobChange = onWizardCredentialsBlobChange,
            onOpenTokensPage = onOpenTokensPage,
        )

        if (wizardCredentialsBlob.isNotBlank() && detectedText.isNotBlank()) {
            CopyBlock(strings.detectedCredentialsTitle, detectedText)
        }

        val canProceed = wizardCredentialsBlob.isNotBlank()
        val nextEnabled = !isCheckingConnection && canProceed
        val nextShape = RoundedCornerShape(18.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = nextShape,
            color = if (nextEnabled) Color.Transparent else Color(0xFF4C453B),
        ) {
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (nextEnabled) {
                            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFF8D66A), Color(0xFFE0A81D))))
                        } else {
                            Modifier
                        },
                    ),
                shape = nextShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF33260F),
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color(0xFFD6C8B2),
                ),
            ) {
                Text(if (isCheckingConnection) strings.connectionChecking else strings.actionNext)
            }
        }
    }
}

@Composable
private fun WizardInstructionBlock(
    strings: SharedStrings,
    wizardCredentialsBlob: String,
    onWizardCredentialsBlobChange: (String) -> Unit,
    onOpenTokensPage: () -> Unit,
) {
    var exampleExpanded by remember { mutableStateOf(false) }
    var exampleFullscreen by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "2. ${strings.instructionTitle}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SharedStepText(
                number = "1",
                text = strings.instructionStep1,
                highlights = listOf("по кнопке ниже", "using the button below"),
            )
            SharedStepText(
                number = "2",
                text = strings.instructionStep2,
                highlights = listOf("verter@student.21-school.ru"),
            )
            OutlinedButton(
                onClick = onOpenTokensPage,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE0A81D)),
            ) {
                Text(strings.instructionOpenLink)
                Box(modifier = Modifier.width(8.dp))
                Text("↗")
            }
            SharedStepText(
                number = "3",
                text = strings.instructionStep3,
                highlights = listOf("Add new Personal", "Add"),
            )
            SharedStepText(
                number = "4",
                text = strings.instructionStep4,
                highlights = listOf("введи пароль", "Enter your Rocket.Chat password"),
            )
            SharedStepText(
                number = "5",
                text = strings.instructionStep5,
                highlights = listOf("Token", "Id", "token", "user Id"),
            )
            TextButton(onClick = { exampleExpanded = !exampleExpanded }) {
                Text(if (exampleExpanded) strings.instructionHideExample else strings.instructionViewExample)
            }
            AnimatedVisibility(visible = exampleExpanded) {
                PlatformTokenExamplePreview(
                    contentDescription = strings.instructionViewExample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { exampleFullscreen = true },
                )
            }
            if (exampleFullscreen) {
                TokenExampleFullscreenDialog(
                    strings = strings,
                    onDismiss = { exampleFullscreen = false },
                )
            }
            SharedStepText(number = "6", text = strings.instructionStep6)
            OutlinedTextField(
                value = wizardCredentialsBlob,
                onValueChange = onWizardCredentialsBlobChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.credentialsBlobLabel) },
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
private fun TokenExampleFullscreenDialog(
    strings: SharedStrings,
    onDismiss: () -> Unit,
) {
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
            PlatformTokenExamplePreview(
                contentDescription = strings.instructionViewExample,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 560.dp, maxHeight = 760.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { },
            )
        }
    }
}

@Composable
private fun SharedStepText(
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
            text = highlightedSharedStepText(
                text = text.removePrefix("$number.").trim(),
                highlights = highlights,
                highlightColor = MaterialTheme.colorScheme.onSurface,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun highlightedSharedStepText(
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
private fun EndpointSettingsBlock(
    strings: SharedStrings,
    endpoint: String,
    selectedPreset: SharedEndpointPreset,
    selectedIntensiveCampus: IntensiveCampus,
    onEndpointChange: (String) -> Unit,
    onEndpointPresetSelected: (SharedEndpointPreset) -> Unit,
    onIntensiveCampusSelected: (IntensiveCampus) -> Unit,
) {
    val showCustomEndpoint = selectedPreset == SharedEndpointPreset.CUSTOM ||
        (selectedPreset == SharedEndpointPreset.INTENSIVE && selectedIntensiveCampus == IntensiveCampus.OTHER)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EndpointSegmentedControl(
            strings = strings,
            selectedPreset = selectedPreset,
            intensiveEnabled = true,
            onEndpointPresetSelected = onEndpointPresetSelected,
        )
        AnimatedVisibility(visible = selectedPreset == SharedEndpointPreset.INTENSIVE) {
            IntensiveCampusSelector(
                strings = strings,
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
                    label = { Text(strings.endpointLabel) },
                    placeholder = { Text(strings.endpointOtherCampusPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Text(
                    text = strings.endpointCustomHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EndpointHintImages(strings = strings)
            }
        }
    }
}

@Composable
private fun IntensiveCampusSelector(
    strings: SharedStrings,
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
                    IntensiveCampus.MOSCOW -> strings.intensiveCampusMoscow
                    IntensiveCampus.NOVOSIBIRSK -> strings.intensiveCampusNovosibirsk
                    IntensiveCampus.KAZAN -> strings.intensiveCampusKazan
                    IntensiveCampus.OTHER -> strings.intensiveCampusOther
                },
                selected = selectedCampus == campus,
                enabled = campus.isAvailableIntensiveCampus(),
                onClick = { onCampusSelected(campus) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EndpointHintImages(strings: SharedStrings) {
    var browserExpanded by remember { mutableStateOf(false) }
    var mobileExpanded by remember { mutableStateOf(false) }
    var fullscreenImage by remember { mutableStateOf<EndpointHintImage?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { browserExpanded = !browserExpanded }, modifier = Modifier.weight(1f)) {
                Text(strings.endpointViewBrowserExample, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { mobileExpanded = !mobileExpanded }, modifier = Modifier.weight(1f)) {
                Text(strings.endpointViewMobileExample, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedVisibility(visible = browserExpanded) {
            PlatformEndpointHintPreview(
                image = EndpointHintImage.BROWSER,
                contentDescription = strings.endpointViewBrowserExample,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreenImage = EndpointHintImage.BROWSER },
            )
        }
        AnimatedVisibility(visible = mobileExpanded) {
            PlatformEndpointHintPreview(
                image = EndpointHintImage.MOBILE,
                contentDescription = strings.endpointViewMobileExample,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreenImage = EndpointHintImage.MOBILE },
            )
        }
    }
    fullscreenImage?.let { image ->
        EndpointHintFullscreenDialog(
            strings = strings,
            image = image,
            onDismiss = { fullscreenImage = null },
        )
    }
}

@Composable
private fun EndpointHintFullscreenDialog(
    strings: SharedStrings,
    image: EndpointHintImage,
    onDismiss: () -> Unit,
) {
    val contentDescription = when (image) {
        EndpointHintImage.BROWSER -> strings.endpointViewBrowserExample
        EndpointHintImage.MOBILE -> strings.endpointViewMobileExample
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
            PlatformEndpointHintPreview(
                image = image,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 760.dp, maxHeight = 820.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { },
            )
        }
    }
}

@Composable
private fun EndpointSegmentedControl(
    strings: SharedStrings,
    selectedPreset: SharedEndpointPreset,
    intensiveEnabled: Boolean,
    onEndpointPresetSelected: (SharedEndpointPreset) -> Unit,
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
                text = strings.endpointPresetBase,
                selected = selectedPreset == SharedEndpointPreset.BASE,
                enabled = true,
                onClick = { onEndpointPresetSelected(SharedEndpointPreset.BASE) },
                modifier = Modifier.weight(1f),
            )
            EndpointSegment(
                text = strings.endpointPresetIntensive,
                selected = selectedPreset == SharedEndpointPreset.INTENSIVE,
                enabled = intensiveEnabled,
                onClick = { onEndpointPresetSelected(SharedEndpointPreset.INTENSIVE) },
                modifier = Modifier.weight(1f),
            )
            EndpointSegment(
                text = strings.endpointPresetCustom,
                selected = selectedPreset == SharedEndpointPreset.CUSTOM,
                enabled = true,
                onClick = { onEndpointPresetSelected(SharedEndpointPreset.CUSTOM) },
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
private fun HomeDashboardScreen(
    strings: SharedStrings,
    endpoint: String,
    userId: String,
    token: String,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    qrImageBase64: String?,
    imageValidationStatus: QrImageValidationStatus,
    isCheckingConnection: Boolean,
    isRefreshingQr: Boolean,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit,
    widgetInfoMessage: String?,
    onRefreshQr: () -> Unit,
    onOpenTokensPage: () -> Unit,
    onOpenGithubPage: () -> Unit,
    onWidgetAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DoorDuckStatusCard(
            strings = strings,
            lastSuccessAtMs = lastSuccessAtMs,
            expiresAtMs = expiresAtMs,
            lastConnectionResult = lastConnectionResult,
            lastSyncError = lastSyncError,
            qrImageBase64 = qrImageBase64,
            imageValidationStatus = imageValidationStatus,
            isRefreshingQr = isRefreshingQr,
        )
        DoorDuckQrCard(
            strings = strings,
            qrImageBase64 = qrImageBase64,
            isRefreshingQr = isRefreshingQr,
            onRefreshQr = onRefreshQr,
        )
        DoorDuckWidgetCard(strings = strings, onWidgetAction = onWidgetAction)
        widgetInfoMessage?.let { InfoCard(message = it) }
        DoorDuckCreditsCard(strings = strings, onOpenGithubPage = onOpenGithubPage)
    }
}

@Composable
private fun SettingsDashboardScreen(
    strings: SharedStrings,
    endpoint: String,
    userId: String,
    token: String,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    qrImageBase64: String?,
    imageValidationStatus: QrImageValidationStatus,
    isCheckingConnection: Boolean,
    isRefreshingQr: Boolean,
    connectionExpanded: Boolean,
    onToggleConnectionExpanded: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit,
    widgetInfoMessage: String?,
    onRefreshQr: () -> Unit,
    onOpenTokensPage: () -> Unit,
    onOpenGithubPage: () -> Unit,
    onWidgetAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DoorDuckConnectionCard(
            strings = strings,
            expanded = true,
            showExpandToggle = false,
            endpoint = endpoint,
            userId = userId,
            token = token,
            isCheckingConnection = isCheckingConnection,
            lastConnectionResult = lastConnectionResult,
            onExpandedChange = onToggleConnectionExpanded,
            onEndpointChange = onEndpointChange,
            onUserIdChange = onUserIdChange,
            onTokenChange = onTokenChange,
            onSave = onSave,
            onCheckConnection = onCheckConnection,
        )
        DoorDuckWidgetCard(strings = strings, onWidgetAction = onWidgetAction)
        widgetInfoMessage?.let { InfoCard(message = it) }
        DoorDuckHelpCard(
            strings = strings,
            onOpenTokensPage = onOpenTokensPage,
        )
        DoorDuckCreditsCard(strings = strings, onOpenGithubPage = onOpenGithubPage)
    }
}

@Composable
private fun SettingsCard(
    strings: SharedStrings,
    endpoint: String,
    userId: String,
    token: String,
    hasStoredCredentials: Boolean,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    qrImageBase64: String?,
    isCheckingConnection: Boolean,
    isRefreshingQr: Boolean,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onOpenTokensPage: () -> Unit,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit,
    onRefreshQr: () -> Unit,
    onClear: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.settingsTitle, style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.endpointLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSave) {
                    Text(strings.actionSave)
                }
                Button(onClick = onCheckConnection, enabled = !isCheckingConnection) {
                    Text(if (isCheckingConnection) strings.connectionChecking else strings.actionCheckConnection)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRefreshQr, enabled = !isRefreshingQr) {
                    Text(if (isRefreshingQr) strings.connectionChecking else strings.actionRefreshNow)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClear) {
                    Text(strings.actionClearSaved)
                }
            }

            StatusCard(
                strings = strings,
                endpoint = endpoint,
                userId = userId,
                hasStoredCredentials = hasStoredCredentials,
                lastSuccessAtMs = lastSuccessAtMs,
                expiresAtMs = expiresAtMs,
                lastConnectionResult = lastConnectionResult,
                lastSyncError = lastSyncError,
                isRefreshingQr = isRefreshingQr,
            )
            QrPreviewCard(strings = strings, qrImageBase64 = qrImageBase64)
            InstructionCard(strings = strings, onOpenTokensPage = onOpenTokensPage)
            PlatformGapCard(strings = strings)
        }
    }
}

@Composable
private fun HomeCard(
    strings: SharedStrings,
    endpoint: String,
    userId: String,
    hasStoredCredentials: Boolean,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    qrImageBase64: String?,
    isRefreshingQr: Boolean,
    onRefreshQr: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.homeTitle, style = MaterialTheme.typography.headlineSmall)
            Text(strings.homeSubtitle, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRefreshQr, enabled = !isRefreshingQr) {
                    Text(if (isRefreshingQr) strings.connectionChecking else strings.actionRefreshNow)
                }
                Button(onClick = onOpenSettings) {
                    Text(strings.actionOpenSettings)
                }
            }

            QrPreviewCard(strings = strings, qrImageBase64 = qrImageBase64)
            StatusCard(
                strings = strings,
                endpoint = endpoint,
                userId = userId,
                hasStoredCredentials = hasStoredCredentials,
                lastSuccessAtMs = lastSuccessAtMs,
                expiresAtMs = expiresAtMs,
                lastConnectionResult = lastConnectionResult,
                lastSyncError = lastSyncError,
                isRefreshingQr = isRefreshingQr,
            )
        }
    }
}

@Composable
private fun StatusCard(
    strings: SharedStrings,
    endpoint: String,
    userId: String,
    hasStoredCredentials: Boolean,
    lastSuccessAtMs: Long?,
    expiresAtMs: Long?,
    lastConnectionResult: ConnectionCheckResult?,
    lastSyncError: SyncError?,
    isRefreshingQr: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.statusTitle, style = MaterialTheme.typography.titleMedium)
            Text("${strings.endpointLabel}: $endpoint")
            Text("${strings.userIdLabel}: ${userId.ifBlank { strings.statusNotSet }}")
            Text("${strings.statusStored}: ${if (hasStoredCredentials) strings.statusYes else strings.statusNo}")
            Text("${strings.statusRefreshInProgress}: ${if (isRefreshingQr) strings.statusYes else strings.statusNo}")
            Text("${strings.statusLastError}: ${lastSyncError?.let(strings::syncErrorMessage) ?: strings.statusNone}")
            Text("${strings.statusLastCheck}: ${lastSuccessAtMs?.let(::formatEpochMillis) ?: strings.statusNever}")
            Text("${strings.statusExpiresAt}: ${expiresAtMs?.let(::formatEpochMillis) ?: strings.statusUnknown}")
            Text("${strings.statusConnection}: ${strings.connectionMessage(lastConnectionResult)}")
        }
    }
}

@Composable
private fun QrPreviewCard(
    strings: SharedStrings,
    qrImageBase64: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.qrTitle, style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun InstructionCard(
    strings: SharedStrings,
    onOpenTokensPage: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(strings.instructionTitle, style = MaterialTheme.typography.titleMedium)
            Text(strings.instructionStep1)
            Text(strings.instructionStep2)
            Text(strings.instructionStep3)
            Text(strings.instructionStep4)
            Text(strings.instructionStep5)
            Text(strings.instructionStep6)
            TextButton(onClick = onOpenTokensPage) {
                Text(strings.instructionOpenLink)
            }
        }
    }
}

@Composable
private fun PlatformGapCard(strings: SharedStrings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(strings.iosStatusTitle, style = MaterialTheme.typography.titleMedium)
            Text(strings.iosStatusBody)
        }
    }
}

@Composable
private fun CopyBlock(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(message: String) {
    DoorDuckInfoBanner(message = message)
}

private fun String.toSharedEndpointPreset(): SharedEndpointPreset {
    val endpoint = InputSanitizer.endpoint(this)
    return when {
        endpoint == Defaults.baseEndpoint -> SharedEndpointPreset.BASE
        endpoint.isKnownIntensiveEndpoint() -> SharedEndpointPreset.INTENSIVE
        else -> SharedEndpointPreset.CUSTOM
    }
}

private fun String.toIntensiveCampus(): IntensiveCampus {
    return when (InputSanitizer.endpoint(this)) {
        Defaults.intensiveMskEndpoint -> IntensiveCampus.MOSCOW
        Defaults.intensiveNskEndpoint -> IntensiveCampus.NOVOSIBIRSK
        Defaults.intensiveKznEndpoint -> IntensiveCampus.KAZAN
        else -> IntensiveCampus.OTHER
    }
}

private fun IntensiveCampus.endpointIfAvailable(): String? {
    return endpoint?.takeIf { it.isNotBlank() }
}

private fun IntensiveCampus.isAvailableIntensiveCampus(): Boolean {
    return this == IntensiveCampus.OTHER || !endpointIfAvailable().isNullOrBlank()
}

private fun hasFixedIntensiveEndpoints(): Boolean {
    return listOf(
        Defaults.intensiveMskEndpoint,
        Defaults.intensiveNskEndpoint,
        Defaults.intensiveKznEndpoint,
    ).any { it.isNotBlank() }
}

private fun String.isKnownIntensiveEndpoint(): Boolean {
    val endpoint = InputSanitizer.endpoint(this)
    if (endpoint.isBlank()) return false
    return endpoint in setOf(
        Defaults.intensiveMskEndpoint,
        Defaults.intensiveNskEndpoint,
        Defaults.intensiveKznEndpoint,
    ).filterTo(mutableSetOf()) { it.isNotBlank() } ||
        endpoint.substringAfter("https://").substringBefore('/').contains("intensive")
}
