package io.github.vgy789.doorDuck.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vgy789.doorDuck.AppContainer
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.config.AndroidEndpointSecrets
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.IntensiveCampus
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import io.github.vgy789.doorDuck.model.QrReadiness
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.domain.SyncPolicy
import io.github.vgy789.doorDuck.update.InstallResult
import io.github.vgy789.doorDuck.update.UpdateCheckResult
import io.github.vgy789.doorDuck.update.UpdateMessage
import io.github.vgy789.doorDuck.update.UpdateStatus
import io.github.vgy789.doorDuck.update.UpdateUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

enum class ScreenMode {
    WIZARD,
    SETTINGS,
}

enum class EndpointPreset {
    BASE,
    INTENSIVE,
    CUSTOM,
}

data class MainUiState(
    val mode: ScreenMode = ScreenMode.WIZARD,
    val wizardStep: WizardStep = WizardStep.CREDENTIALS,
    val endpoint: String = Defaults.defaultEndpoint,
    val endpointPreset: EndpointPreset = EndpointPreset.BASE,
    val wizardEndpoint: String = Defaults.defaultEndpoint,
    val wizardEndpointPreset: EndpointPreset = EndpointPreset.BASE,
    val wizardIntensiveCampus: IntensiveCampus = IntensiveCampus.MOSCOW,
    val wizardCredentialsBlob: String = "",
    val wizardAuthToken: String = "",
    val wizardUserId: String = "",
    val authToken: String = "",
    val userId: String = "",
    val hasStoredCredentials: Boolean = false,
    val autoRefreshEnabled: Boolean = true,
    val maxBrightnessEnabled: Boolean = false,
    val qrImagePath: String? = null,
    val imageValidationStatus: QrImageValidationStatus = QrImageValidationStatus.UNKNOWN,
    val lastSuccessAtMs: Long? = null,
    val expiresAtMs: Long? = null,
    val nextAutoRefreshAtMs: Long? = null,
    val manualRefreshBlockedUntilMs: Long? = null,
    val syncInProgress: Boolean = false,
    val isConnectionCheckInProgress: Boolean = false,
    val lastConnectionCheckResult: ConnectionCheckResult? = null,
    val connectionCheckPassed: Boolean = false,
    val lastError: SyncError? = null,
    val infoMessage: String? = null,
    val update: UpdateUiState = UpdateUiState(),
)

class MainViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var dueRefreshQueued = false
    private var validationMigrationDone = false
    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            appContainer.settingsStore
                .observeAppState(appContainer.credentialsStore.observeHasCredentials())
                .collect { state ->
                    if (!validationMigrationDone && state.snapshot.localImagePath != null &&
                        state.snapshot.imageValidationStatus == QrImageValidationStatus.UNKNOWN
                    ) {
                        validationMigrationDone = true
                        val migratedStatus = if (appContainer.imageStore.isValidImage(state.snapshot.localImagePath)) {
                            QrImageValidationStatus.VALID
                        } else {
                            QrImageValidationStatus.INVALID
                        }
                        appContainer.settingsStore.setImageValidationStatus(migratedStatus)
                    }
                    if (
                        state.hasCredentials &&
                        state.settings.autoRefreshEnabled &&
                        !state.settings.endpoint.isIntensiveEndpoint() &&
                        !state.snapshot.isSyncInProgress &&
                        !dueRefreshQueued &&
                        SyncPolicy.shouldRefreshNow(
                            autoRefreshEnabled = state.settings.autoRefreshEnabled,
                            localImagePath = state.snapshot.localImagePath,
                            expiresAtMs = state.snapshot.expiresAtMs,
                            nextAutoRefreshAtMs = state.snapshot.nextAutoRefreshAtMs,
                            lastError = state.snapshot.lastError,
                        )
                    ) {
                        dueRefreshQueued = true
                        appContainer.settingsStore.setSyncInProgress(true)
                        appContainer.workScheduler.enqueueAutomaticRefresh(delayMs = 0L, attempt = 0)
                        appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
                    }
                    val stored = appContainer.credentialsStore.load()
                    _uiState.update { current ->
                        val base = current.copy(
                            endpoint = if (current.endpoint.isBlank()) {
                                state.settings.endpoint
                            } else {
                                current.endpoint
                            },
                            hasStoredCredentials = state.hasCredentials,
                            autoRefreshEnabled = state.settings.autoRefreshEnabled,
                            maxBrightnessEnabled = state.settings.maxBrightnessEnabled,
                            qrImagePath = state.snapshot.localImagePath,
                            imageValidationStatus = state.snapshot.imageValidationStatus,
                            lastSuccessAtMs = state.snapshot.lastSuccessAtMs,
                            expiresAtMs = state.snapshot.expiresAtMs,
                            nextAutoRefreshAtMs = state.snapshot.nextAutoRefreshAtMs,
                            manualRefreshBlockedUntilMs = state.snapshot.manualRefreshBlockedUntilMs,
                            syncInProgress = state.snapshot.isSyncInProgress,
                            lastError = state.snapshot.lastError,
                        )
                        if (initialized) {
                            base
                        } else {
                            initialized = true
                            base.copy(
                                mode = if (state.hasCredentials) ScreenMode.SETTINGS else ScreenMode.WIZARD,
                                wizardStep = if (state.hasCredentials) WizardStep.DONE else WizardStep.CREDENTIALS,
                                endpointPreset = base.endpoint.toEndpointPreset(),
                                wizardEndpoint = base.endpoint,
                                wizardEndpointPreset = base.endpoint.toEndpointPreset(),
                                wizardIntensiveCampus = base.endpoint.toIntensiveCampus(),
                                wizardCredentialsBlob = "",
                                wizardAuthToken = "",
                                wizardUserId = "",
                                authToken = InputSanitizer.noWhitespace(stored?.authToken.orEmpty()),
                                userId = InputSanitizer.noWhitespace(stored?.userId.orEmpty()),
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val settings = appContainer.updateRepository.settings()
            val cached = appContainer.updateRepository.cachedResult()
            val cachedRelease = (cached as? UpdateCheckResult.Available)?.release
            val readyToInstall = cachedRelease != null &&
                settings.readyReleaseTag == cachedRelease.tag &&
                appContainer.apkUpdateManager.hasReadyApk()
            if (cached == UpdateCheckResult.UpToDate && settings.readyReleaseTag != null) {
                appContainer.updateSettingsStore.setReadyReleaseTag(null)
                appContainer.apkUpdateManager.clear()
            }
            _uiState.update { current ->
                current.copy(
                    update = current.update.copy(
                        automaticChecksEnabled = settings.automaticChecksEnabled,
                        status = when {
                            readyToInstall -> UpdateStatus.READY_TO_INSTALL
                            cached is UpdateCheckResult.Available -> UpdateStatus.AVAILABLE
                            cached == UpdateCheckResult.UpToDate -> UpdateStatus.UP_TO_DATE
                            else -> UpdateStatus.IDLE
                        },
                        release = cachedRelease,
                    ),
                )
            }
            uiState.filter { it.mode == ScreenMode.SETTINGS }.first()
            if (appContainer.updateRepository.shouldRunAutomaticCheck()) {
                checkForUpdates(manual = false)
            }
        }
    }

    fun setAutomaticUpdateChecksEnabled(enabled: Boolean) {
        _uiState.update { it.copy(update = it.update.copy(automaticChecksEnabled = enabled, message = null)) }
        viewModelScope.launch {
            appContainer.updateRepository.setAutomaticChecksEnabled(enabled)
            if (enabled && appContainer.updateRepository.shouldRunAutomaticCheck()) {
                checkForUpdates(manual = false)
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch { checkForUpdates(manual = true) }
    }

    private suspend fun checkForUpdates(manual: Boolean) {
        if (_uiState.value.update.status == UpdateStatus.CHECKING ||
            _uiState.value.update.status == UpdateStatus.DOWNLOADING
        ) return
        _uiState.update { it.copy(update = it.update.copy(status = UpdateStatus.CHECKING, message = null)) }
        when (val result = appContainer.updateRepository.check()) {
            is UpdateCheckResult.Available -> _uiState.update {
                it.copy(update = it.update.copy(status = UpdateStatus.AVAILABLE, release = result.release, message = null))
            }
            UpdateCheckResult.UpToDate -> _uiState.update {
                it.copy(update = it.update.copy(status = UpdateStatus.UP_TO_DATE, release = null, message = null))
            }
            is UpdateCheckResult.Failed -> _uiState.update {
                val cachedRelease = it.update.release
                it.copy(
                    update = it.update.copy(
                        status = if (!manual && cachedRelease != null) UpdateStatus.AVAILABLE else if (manual) UpdateStatus.FAILED else UpdateStatus.IDLE,
                        message = if (manual) UpdateMessage.CHECK_FAILED else null,
                    ),
                )
            }
        }
    }

    fun downloadUpdate() {
        val release = _uiState.value.update.release ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            appContainer.updateSettingsStore.setReadyReleaseTag(null)
            _uiState.update {
                it.copy(update = it.update.copy(status = UpdateStatus.DOWNLOADING, downloadProgress = 0, message = null))
            }
            appContainer.apkUpdateManager.downloadAndVerify(release) { progress ->
                _uiState.update { current ->
                    current.copy(update = current.update.copy(downloadProgress = progress))
                }
            }.onSuccess {
                appContainer.updateSettingsStore.setReadyReleaseTag(release.tag)
                _uiState.update {
                    it.copy(update = it.update.copy(status = UpdateStatus.READY_TO_INSTALL, downloadProgress = 100, message = null))
                }
            }.onFailure {
                if (it is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(update = it.update.copy(status = UpdateStatus.FAILED, message = UpdateMessage.DOWNLOAD_FAILED))
                }
            }
        }
    }

    fun cancelUpdateDownload() {
        downloadJob?.cancel()
        downloadJob = null
        appContainer.apkUpdateManager.cancelDownload()
        viewModelScope.launch { appContainer.updateSettingsStore.setReadyReleaseTag(null) }
        _uiState.update {
            it.copy(update = it.update.copy(status = UpdateStatus.AVAILABLE, downloadProgress = 0, message = null))
        }
    }

    fun installUpdate() {
        when (appContainer.apkUpdateManager.install()) {
            InstallResult.InstallerOpened -> _uiState.update {
                it.copy(update = it.update.copy(waitingForInstallPermission = false, message = null))
            }
            InstallResult.PermissionRequired -> _uiState.update {
                it.copy(
                    update = it.update.copy(
                        waitingForInstallPermission = true,
                        message = UpdateMessage.INSTALL_PERMISSION_REQUIRED,
                    ),
                )
            }
            is InstallResult.Failed -> {
                appContainer.apkUpdateManager.clear()
                viewModelScope.launch { appContainer.updateSettingsStore.setReadyReleaseTag(null) }
                _uiState.update { current ->
                    current.copy(update = current.update.copy(message = UpdateMessage.INSTALL_FAILED))
                }
            }
        }
    }

    fun openUpdateInstallPermissionSettings() {
        appContainer.apkUpdateManager.openInstallPermissionSettings().onFailure {
            _uiState.update { current ->
                current.copy(update = current.update.copy(message = UpdateMessage.INSTALL_FAILED))
            }
        }
    }

    fun openSettings() {
        _uiState.update {
            it.copy(
                mode = ScreenMode.SETTINGS,
                wizardStep = WizardStep.DONE,
                wizardEndpoint = it.endpoint,
                wizardEndpointPreset = it.endpoint.toEndpointPreset(),
                wizardIntensiveCampus = it.endpoint.toIntensiveCampus(),
                wizardCredentialsBlob = "",
                wizardAuthToken = "",
                wizardUserId = "",
                infoMessage = null,
            )
        }
    }

    fun wizardNext() {
        val state = uiState.value

        when (state.wizardStep) {
            WizardStep.CREDENTIALS -> {
                finishWizardWithConnectionCheck()
            }

            WizardStep.DONE -> {
                openSettings()
            }
        }
    }

    fun wizardBack() {
        _uiState.update {
            it.copy(
                wizardStep = WizardStateMachine.previous(it.wizardStep),
                infoMessage = null,
            )
        }
    }

    fun onEndpointChange(value: String) {
        _uiState.update {
            it.copy(
                endpoint = value,
                endpointPreset = EndpointPreset.CUSTOM,
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun onWizardCredentialsBlobChange(value: String) {
        val extracted = RocketCredentialsExtractor.extract(value)
        _uiState.update {
            it.copy(
                wizardCredentialsBlob = value.trim(),
                wizardAuthToken = InputSanitizer.noWhitespace(extracted.authToken.orEmpty()),
                wizardUserId = InputSanitizer.noWhitespace(extracted.userId.orEmpty()),
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun onWizardEndpointChange(value: String) {
        _uiState.update {
            it.copy(
                wizardEndpoint = value,
                wizardEndpointPreset = if (it.wizardEndpointPreset == EndpointPreset.INTENSIVE) {
                    EndpointPreset.INTENSIVE
                } else {
                    EndpointPreset.CUSTOM
                },
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun onTokenChange(value: String) {
        _uiState.update {
            it.copy(
                authToken = InputSanitizer.noWhitespace(value),
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun clearToken() {
        _uiState.update {
            it.copy(
                authToken = "",
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun onUserIdChange(value: String) {
        _uiState.update {
            it.copy(
                userId = InputSanitizer.noWhitespace(value),
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            val state = uiState.value
            val endpoint = InputSanitizer.endpoint(state.endpoint)
            val token = InputSanitizer.noWhitespace(state.authToken)
            val userId = InputSanitizer.noWhitespace(state.userId)

            if (!endpoint.startsWith("https://")) {
                setInfo(R.string.error_endpoint_https)
                return@launch
            }
            if (token.isBlank()) {
                setInfo(R.string.error_token_required)
                return@launch
            }
            if (userId.isBlank()) {
                setInfo(R.string.error_user_id_required)
                return@launch
            }

            _uiState.update { it.copy(isConnectionCheckInProgress = true, infoMessage = null) }
            val result = appContainer.syncService.verifyCredentialsAndBotAccess(
                endpoint = endpoint,
                credentials = Credentials(authToken = token, userId = userId),
            )
            _uiState.update {
                it.copy(
                    isConnectionCheckInProgress = false,
                    lastConnectionCheckResult = result,
                    connectionCheckPassed = result == ConnectionCheckResult.SUCCESS,
                    infoMessage = connectionResultMessage(result),
                )
            }
            if (result == ConnectionCheckResult.SUCCESS) {
                saveAll(
                    endpoint = endpoint,
                    token = token,
                    userId = userId,
                )
                setInfo(R.string.info_settings_saved)
            }
        }
    }

    fun selectWizardEndpointPreset(preset: EndpointPreset) {
        val intensiveCampus = if (AndroidEndpointSecrets.hasFixedIntensiveEndpoints) {
            IntensiveCampus.MOSCOW
        } else {
            IntensiveCampus.OTHER
        }
        val endpoint = when (preset) {
            EndpointPreset.BASE -> Defaults.baseEndpoint
            EndpointPreset.INTENSIVE -> AndroidEndpointSecrets.endpointFor(intensiveCampus) ?: Defaults.baseEndpoint
            EndpointPreset.CUSTOM -> ""
        }
        _uiState.update {
            it.copy(
                wizardEndpoint = endpoint,
                wizardEndpointPreset = preset,
                wizardIntensiveCampus = if (preset == EndpointPreset.INTENSIVE) {
                    intensiveCampus
                } else {
                    it.wizardIntensiveCampus
                },
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun selectWizardIntensiveCampus(campus: IntensiveCampus) {
        val endpoint = AndroidEndpointSecrets.endpointFor(campus).orEmpty()
        _uiState.update {
            it.copy(
                wizardEndpoint = endpoint,
                wizardEndpointPreset = EndpointPreset.INTENSIVE,
                wizardIntensiveCampus = campus,
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            val state = uiState.value
            val nowMs = System.currentTimeMillis()
            if (state.syncInProgress) return@launch
            if (SyncPolicy.isManualRefreshBlocked(state.manualRefreshBlockedUntilMs, nowMs)) {
                setInfo(R.string.info_refresh_short_cooldown)
                return@launch
            }
            val nextAutoRefreshAtMs = state.nextAutoRefreshAtMs
            if (
                state.lastError == SyncError.RATE_LIMITED &&
                nextAutoRefreshAtMs != null &&
                nextAutoRefreshAtMs > nowMs
            ) {
                _uiState.update {
                    it.copy(
                        infoMessage = appContainer.appContext.getString(
                            R.string.info_refresh_deferred_until,
                            DateFormat.getDateTimeInstance().format(Date(nextAutoRefreshAtMs)),
                        ),
                    )
                }
                return@launch
            }
            appContainer.settingsStore.setManualRefreshBlockedUntil(
                SyncPolicy.nextManualRefreshAllowedAt(nowMs),
            )
            appContainer.settingsStore.setSyncInProgress(true)
            appContainer.workScheduler.enqueueManualRefresh()
            appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
            setInfo(R.string.info_refresh_queued)
        }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (uiState.value.endpoint.isIntensiveEndpoint() && enabled) {
                appContainer.settingsStore.setAutoRefreshEnabled(false)
                appContainer.settingsStore.setNextAutoRefreshAt(null)
                appContainer.workScheduler.cancelAutomaticRefresh()
                _uiState.update { it.copy(autoRefreshEnabled = false, nextAutoRefreshAtMs = null) }
                return@launch
            }
            appContainer.settingsStore.setAutoRefreshEnabled(enabled)
            if (!enabled) {
                appContainer.settingsStore.setNextAutoRefreshAt(null)
                appContainer.workScheduler.cancelAutomaticRefresh()
            } else {
                val snapshot = appContainer.settingsStore.getSnapshot()
                val expiresAtMs = snapshot.expiresAtMs
                if (expiresAtMs != null) {
                    val nextAutoRefreshAtMs = snapshot.nextAutoRefreshAtMs
                        ?: SyncPolicy.refreshAtMs(expiresAtMs)
                    appContainer.settingsStore.setNextAutoRefreshAt(nextAutoRefreshAtMs)
                    if (SyncPolicy.shouldRefreshNow(
                            autoRefreshEnabled = true,
                            localImagePath = snapshot.localImagePath,
                            expiresAtMs = expiresAtMs,
                            nextAutoRefreshAtMs = nextAutoRefreshAtMs,
                            lastError = snapshot.lastError,
                        )
                    ) {
                        appContainer.workScheduler.enqueueAutomaticRefresh(delayMs = 0L, attempt = 0)
                    } else {
                        appContainer.workScheduler.scheduleAutomaticRetry(
                            retryAtMs = nextAutoRefreshAtMs,
                            attempt = 0,
                        )
                    }
                }
            }
            _uiState.update {
                it.copy(
                    autoRefreshEnabled = enabled,
                    nextAutoRefreshAtMs = if (enabled) it.nextAutoRefreshAtMs else null,
                )
            }
        }
    }

    fun setMaxBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appContainer.settingsStore.setMaxBrightnessEnabled(enabled)
            _uiState.update { it.copy(maxBrightnessEnabled = enabled) }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            appContainer.workScheduler.cancelAllRefreshWork()
            appContainer.settingsStore.clear()
            appContainer.updateSettingsStore.clear()
            appContainer.apkUpdateManager.clear()
            appContainer.credentialsStore.clear()
            File(appContainer.appContext.applicationInfo.dataDir, "shared_prefs")
                .listFiles()
                ?.forEach { it.deleteRecursively() }
            appContainer.imageStore.clear()
            appContainer.appContext.cacheDir.deleteRecursively()
            appContainer.appContext.cacheDir.mkdirs()
            appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
            _uiState.value = MainUiState(
                mode = ScreenMode.WIZARD,
                infoMessage = appContainer.appContext.getString(R.string.info_all_data_cleared),
            )
            initialized = true
            dueRefreshQueued = false
        }
    }

    fun startCredentialRecovery() {
        _uiState.update {
            it.copy(
                mode = ScreenMode.WIZARD,
                wizardStep = WizardStep.CREDENTIALS,
                wizardEndpoint = it.endpoint,
                wizardEndpointPreset = it.endpoint.toEndpointPreset(),
                wizardIntensiveCampus = it.endpoint.toIntensiveCampus(),
                wizardCredentialsBlob = "",
                wizardAuthToken = "",
                wizardUserId = "",
                lastConnectionCheckResult = null,
                connectionCheckPassed = false,
                infoMessage = null,
            )
        }
    }

    private fun finishWizardWithConnectionCheck() {
        viewModelScope.launch {
            val state = uiState.value
            val endpoint = InputSanitizer.endpoint(state.wizardEndpoint)
            val token = InputSanitizer.noWhitespace(state.wizardAuthToken)
            val userId = InputSanitizer.noWhitespace(state.wizardUserId)

            if (!endpoint.startsWith("https://")) {
                setInfo(R.string.error_endpoint_https)
                return@launch
            }
            if (token.isBlank() || userId.isBlank()) {
                setInfo(R.string.error_credentials_blob_required)
                return@launch
            }

            _uiState.update { it.copy(isConnectionCheckInProgress = true, infoMessage = null) }
            val result = appContainer.syncService.verifyCredentialsAndBotAccess(
                endpoint = endpoint,
                credentials = Credentials(authToken = token, userId = userId),
            )
            if (result != ConnectionCheckResult.SUCCESS) {
                _uiState.update {
                    it.copy(
                        isConnectionCheckInProgress = false,
                        lastConnectionCheckResult = result,
                        connectionCheckPassed = false,
                        infoMessage = connectionResultMessage(result),
                    )
                }
                return@launch
            }

            saveAll(
                endpoint = endpoint,
                token = token,
                userId = userId,
                forceAutoRefreshDisabled = state.wizardEndpointPreset == EndpointPreset.INTENSIVE,
            )
            appContainer.settingsStore.setManualRefreshBlockedUntil(
                SyncPolicy.nextManualRefreshAllowedAt(),
            )
            appContainer.settingsStore.setSyncInProgress(true)
            appContainer.workScheduler.enqueueManualRefresh(showToastOnResult = true)
            appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
            _uiState.update {
                it.copy(
                    mode = ScreenMode.SETTINGS,
                    wizardStep = WizardStep.DONE,
                    isConnectionCheckInProgress = false,
                    lastConnectionCheckResult = ConnectionCheckResult.SUCCESS,
                    connectionCheckPassed = true,
                    infoMessage = null,
                )
            }
        }
    }

    private suspend fun saveAll(
        endpoint: String,
        token: String,
        userId: String,
        forceAutoRefreshDisabled: Boolean = false,
    ) {
        val normalizedEndpoint = InputSanitizer.endpoint(endpoint)
        val normalizedToken = InputSanitizer.noWhitespace(token)
        val normalizedUserId = InputSanitizer.noWhitespace(userId)
        val autoRefreshEnabled = uiState.value.autoRefreshEnabled &&
            !forceAutoRefreshDisabled &&
            !normalizedEndpoint.isIntensiveEndpoint()
        appContainer.settingsStore.updateCredentialsSettings(normalizedEndpoint, autoRefreshEnabled)
        appContainer.settingsStore.clearSyncError()
        if (!autoRefreshEnabled) {
            appContainer.settingsStore.setNextAutoRefreshAt(null)
            appContainer.workScheduler.cancelAutomaticRefresh()
        }
        appContainer.credentialsStore.save(normalizedToken, normalizedUserId)
        appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
        _uiState.update {
            it.copy(
                endpoint = normalizedEndpoint,
                endpointPreset = if (forceAutoRefreshDisabled) EndpointPreset.INTENSIVE else normalizedEndpoint.toEndpointPreset(),
                wizardCredentialsBlob = "",
                authToken = normalizedToken,
                userId = normalizedUserId,
                hasStoredCredentials = true,
                autoRefreshEnabled = autoRefreshEnabled,
            )
        }
    }

    private fun setInfo(@StringRes stringRes: Int) {
        _uiState.update {
            it.copy(infoMessage = appContainer.appContext.getString(stringRes))
        }
    }

    private fun connectionResultMessage(result: ConnectionCheckResult): String {
        val resId = when (result) {
            ConnectionCheckResult.SUCCESS -> R.string.connection_ok
            ConnectionCheckResult.UNAUTHORIZED -> R.string.connection_unauthorized
            ConnectionCheckResult.BOT_NOT_FOUND -> R.string.connection_bot_not_found
            ConnectionCheckResult.BOT_UNAVAILABLE -> R.string.connection_bot_unavailable
            ConnectionCheckResult.NETWORK_ERROR -> R.string.connection_network_error
            ConnectionCheckResult.UNKNOWN -> R.string.connection_unknown_error
        }
        return appContainer.appContext.getString(resId)
    }
}

fun MainUiState.qrReadiness(nowMs: Long = System.currentTimeMillis()): QrReadiness {
    return SyncPolicy.readiness(
        hasImage = !qrImagePath.isNullOrBlank(),
        validationStatus = imageValidationStatus,
        expiresAtMs = expiresAtMs,
        nowMs = nowMs,
    )
}

private fun String.isIntensiveEndpoint(): Boolean {
    return AndroidEndpointSecrets.isIntensiveEndpoint(this)
}

private fun String.toEndpointPreset(): EndpointPreset {
    val endpoint = InputSanitizer.endpoint(this)
    return when {
        endpoint == Defaults.baseEndpoint -> EndpointPreset.BASE
        endpoint.isIntensiveEndpoint() -> EndpointPreset.INTENSIVE
        else -> EndpointPreset.CUSTOM
    }
}

private fun String.toIntensiveCampus(): IntensiveCampus {
    return AndroidEndpointSecrets.campusFor(this)
}

class MainViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(appContainer) as T
    }
}
