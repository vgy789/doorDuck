package io.github.vgy789.doorDuck.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.platform.DoorDuckPlatformServices
import io.github.vgy789.doorDuck.platform.PersistedDoorDuckState
import io.github.vgy789.doorDuck.platform.PlatformQrRefreshResult
import io.github.vgy789.doorDuck.platform.formatEpochMillis
import io.github.vgy789.doorDuck.ui.InputSanitizer
import io.github.vgy789.doorDuck.ui.WizardStateMachine
import io.github.vgy789.doorDuck.ui.WizardStep
import kotlinx.coroutines.launch

private enum class ScreenMode {
    WIZARD,
    HOME,
    SETTINGS,
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
    var wizardStep by remember { mutableStateOf(WizardStep.WELCOME) }
    var endpoint by remember { mutableStateOf(Defaults.defaultEndpoint) }
    var userId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var hasStoredCredentials by remember { mutableStateOf(false) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    var isRefreshingQr by remember { mutableStateOf(false) }
    var lastConnectionResult by remember { mutableStateOf<ConnectionCheckResult?>(null) }
    var connectionCheckPassed by remember { mutableStateOf(false) }
    var lastSuccessAtMs by remember { mutableStateOf<Long?>(null) }
    var expiresAtMs by remember { mutableStateOf<Long?>(null) }
    var lastSyncError by remember { mutableStateOf<SyncError?>(null) }
    var qrImageBase64 by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var connectionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val stored = DoorDuckPlatformServices.loadPersistedState()
        if (stored != null) {
            endpoint = stored.endpoint
            userId = stored.userId
            token = stored.authToken
            lastSuccessAtMs = stored.lastSuccessAtMs
            expiresAtMs = stored.expiresAtMs
            lastConnectionResult = stored.lastConnectionResult
            lastSyncError = stored.lastSyncError
            qrImageBase64 = stored.qrImageBase64
            connectionCheckPassed = stored.lastConnectionResult == ConnectionCheckResult.SUCCESS
            hasStoredCredentials = stored.userId.isNotBlank() && stored.authToken.isNotBlank()
            screenMode = if (hasStoredCredentials) ScreenMode.HOME else ScreenMode.WIZARD
            wizardStep = if (hasStoredCredentials) WizardStep.DONE else WizardStep.WELCOME
            connectionExpanded = false
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

    fun saveLocalState(result: ConnectionCheckResult?) {
        scope.launch {
            DoorDuckPlatformServices.savePersistedState(
                PersistedDoorDuckState(
                    endpoint = endpoint,
                    authToken = token,
                    userId = userId,
                    lastSuccessAtMs = lastSuccessAtMs,
                    expiresAtMs = expiresAtMs,
                    lastConnectionResult = result,
                    lastSyncError = lastSyncError,
                    qrImageBase64 = qrImageBase64,
                ),
            )
        }
    }

    fun runCheckConnection(onSuccess: () -> Unit = {}) {
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
                lastSuccessAtMs = DoorDuckPlatformServices.currentTimeMillis()
                hasStoredCredentials = true
                lastSyncError = null
                saveLocalState(result)
                onSuccess()
            } else {
                saveLocalState(result)
            }
            infoMessage = strings.connectionMessage(result)
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
        connectionExpanded = false
        infoMessage = message
    }

    fun switchToSettings(message: String? = null) {
        screenMode = ScreenMode.SETTINGS
        wizardStep = WizardStep.DONE
        connectionExpanded = true
        infoMessage = message
    }

    fun switchToWizard() {
        screenMode = ScreenMode.WIZARD
        wizardStep = WizardStep.WELCOME
        connectionExpanded = false
        infoMessage = null
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(doorDuckBackgroundBrush())
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DoorDuckHeader(
                strings = strings,
                actionLabel = when (screenMode) {
                    ScreenMode.HOME -> strings.actionRunWizard
                    ScreenMode.SETTINGS -> strings.actionBackToHome
                    ScreenMode.WIZARD -> if (hasStoredCredentials) strings.actionBackToHome else null
                },
                onAction = when (screenMode) {
                    ScreenMode.HOME -> ({ switchToWizard() })
                    ScreenMode.SETTINGS -> ({ switchToHome() })
                    ScreenMode.WIZARD -> if (hasStoredCredentials) ({ switchToHome() }) else null
                },
            )

            when (screenMode) {
                ScreenMode.WIZARD -> {
                    WizardCard(
                        strings = strings,
                        wizardStep = wizardStep,
                        endpoint = endpoint,
                        userId = userId,
                        token = token,
                        isCheckingConnection = isCheckingConnection,
                        onEndpointChange = {
                            endpoint = InputSanitizer.endpoint(it)
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
                        onOpenTokensPage = { uriHandler.openUri(strings.tokensUrl) },
                        onCheckConnection = {
                            runCheckConnection {
                                infoMessage = strings.infoSettingsSaved
                            }
                        },
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (wizardStep != WizardStep.WELCOME) {
                            TextButton(
                                onClick = {
                                    wizardStep = WizardStateMachine.previous(wizardStep)
                                    infoMessage = null
                                },
                            ) {
                                Text(strings.actionBack)
                            }
                        }
                        Button(
                            onClick = {
                                if (wizardStep == WizardStep.CHECK_CONNECTION) {
                                    if (!connectionCheckPassed) {
                                        infoMessage = strings.errorConnectionCheckRequired
                                        return@Button
                                    }
                                    switchToHome(strings.infoWizardDone)
                                    if (qrImageBase64.isNullOrBlank()) {
                                        refreshQrNow()
                                    }
                                } else {
                                    wizardStep = WizardStateMachine.next(wizardStep)
                                    infoMessage = null
                                }
                            },
                            enabled = WizardStateMachine.canProceed(
                                step = wizardStep,
                                userId = userId,
                                token = token,
                                connectionCheckPassed = connectionCheckPassed,
                            ),
                        ) {
                            Text(
                                when (wizardStep) {
                                    WizardStep.WELCOME -> strings.actionStart
                                    WizardStep.USER_ID, WizardStep.TOKEN -> strings.actionNext
                                    WizardStep.CHECK_CONNECTION -> strings.actionFinish
                                    WizardStep.DONE -> strings.actionOpenSettings
                                },
                            )
                        }
                    }
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
                        isCheckingConnection = isCheckingConnection,
                        isRefreshingQr = isRefreshingQr,
                        connectionExpanded = connectionExpanded,
                        onToggleConnectionExpanded = { connectionExpanded = !connectionExpanded },
                        onEndpointChange = {
                            endpoint = InputSanitizer.endpoint(it)
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
                        onRunWizard = { switchToWizard() },
                        onOpenTokensPage = { uriHandler.openUri(strings.tokensUrl) },
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
                        isCheckingConnection = isCheckingConnection,
                        isRefreshingQr = isRefreshingQr,
                        connectionExpanded = connectionExpanded,
                        onToggleConnectionExpanded = { connectionExpanded = !connectionExpanded },
                        onEndpointChange = {
                            endpoint = InputSanitizer.endpoint(it)
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
                        onOpenTokensPage = { uriHandler.openUri(strings.tokensUrl) },
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
                        onRunWizard = { switchToWizard() },
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
    wizardStep: WizardStep,
    endpoint: String,
    userId: String,
    token: String,
    isCheckingConnection: Boolean,
    onEndpointChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onOpenTokensPage: () -> Unit,
    onCheckConnection: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.wizardTitle, style = MaterialTheme.typography.headlineSmall)

            when (wizardStep) {
                WizardStep.WELCOME -> {
                    CopyBlock(strings.welcomeTitle, strings.welcomeBody)
                }

                WizardStep.USER_ID -> {
                    CopyBlock(strings.userIdTitle, strings.userIdHint)
                    OutlinedTextField(
                        value = userId,
                        onValueChange = onUserIdChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.userIdLabel) },
                        singleLine = true,
                    )
                    InstructionCard(strings = strings, onOpenTokensPage = onOpenTokensPage)
                }

                WizardStep.TOKEN -> {
                    CopyBlock(strings.tokenTitle, strings.tokenHint)
                    OutlinedTextField(
                        value = token,
                        onValueChange = onTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.tokenLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                WizardStep.CHECK_CONNECTION -> {
                    CopyBlock(strings.endpointTitle, strings.connectionHint)
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = onEndpointChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.endpointLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    Button(onClick = onCheckConnection, enabled = !isCheckingConnection) {
                        Text(if (isCheckingConnection) strings.connectionChecking else strings.actionCheckConnection)
                    }
                }

                WizardStep.DONE -> {
                    CopyBlock(strings.doneTitle, strings.doneBody)
                }
            }
        }
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
    onRunWizard: () -> Unit,
    onOpenTokensPage: () -> Unit,
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
            isRefreshingQr = isRefreshingQr,
        )
        DoorDuckQrCard(
            strings = strings,
            qrImageBase64 = qrImageBase64,
            isRefreshingQr = isRefreshingQr,
            onRefreshQr = onRefreshQr,
            onRunWizard = onRunWizard,
        )
        DoorDuckWidgetCard(strings = strings, onWidgetAction = onWidgetAction)
        widgetInfoMessage?.let { InfoCard(message = it) }
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
        DoorDuckHelpCard(strings = strings, onOpenTokensPage = onOpenTokensPage)
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
    onRunWizard: () -> Unit,
    onOpenTokensPage: () -> Unit,
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
        DoorDuckHelpCard(strings = strings, onOpenTokensPage = onOpenTokensPage)
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
    onBackToHome: () -> Unit,
    onRunWizard: () -> Unit,
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
                TextButton(onClick = onBackToHome) {
                    Text(strings.actionBackToHome)
                }
                TextButton(onClick = onRunWizard) {
                    Text(strings.actionRunWizard)
                }
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
    onRunWizard: () -> Unit,
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

            TextButton(onClick = onRunWizard) {
                Text(strings.actionRunWizard)
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
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoCard(message: String) {
    DoorDuckInfoBanner(message = message)
}
