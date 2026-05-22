package io.github.vgy789.doorDuck.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vgy789.doorDuck.AppContainer
import io.github.vgy789.doorDuck.R
import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import io.github.vgy789.doorDuck.model.Credentials
import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.model.SyncError
import io.github.vgy789.doorDuck.domain.SyncPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

enum class ScreenMode {
    WIZARD,
    SETTINGS,
}

data class MainUiState(
    val mode: ScreenMode = ScreenMode.WIZARD,
    val wizardStep: WizardStep = WizardStep.WELCOME,
    val endpoint: String = Defaults.defaultEndpoint,
    val authToken: String = "",
    val userId: String = "",
    val hasStoredCredentials: Boolean = false,
    val autoRefreshEnabled: Boolean = true,
    val qrImagePath: String? = null,
    val lastSuccessAtMs: Long? = null,
    val expiresAtMs: Long? = null,
    val nextAutoRefreshAtMs: Long? = null,
    val syncInProgress: Boolean = false,
    val isConnectionCheckInProgress: Boolean = false,
    val lastConnectionCheckResult: ConnectionCheckResult? = null,
    val connectionCheckPassed: Boolean = false,
    val lastError: SyncError? = null,
    val infoMessage: String? = null,
)

class MainViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var dueRefreshQueued = false

    init {
        viewModelScope.launch {
            appContainer.settingsStore
                .observeAppState(appContainer.credentialsStore.observeHasCredentials())
                .collect { state ->
                    if (
                        state.hasCredentials &&
                        state.settings.autoRefreshEnabled &&
                        !state.snapshot.isSyncInProgress &&
                        !dueRefreshQueued &&
                        SyncPolicy.shouldRefreshNow(
                            autoRefreshEnabled = state.settings.autoRefreshEnabled,
                            localImagePath = state.snapshot.localImagePath,
                            expiresAtMs = state.snapshot.expiresAtMs,
                            nextAutoRefreshAtMs = state.snapshot.nextAutoRefreshAtMs,
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
                            qrImagePath = state.snapshot.localImagePath,
                            lastSuccessAtMs = state.snapshot.lastSuccessAtMs,
                            expiresAtMs = state.snapshot.expiresAtMs,
                            nextAutoRefreshAtMs = state.snapshot.nextAutoRefreshAtMs,
                            syncInProgress = state.snapshot.isSyncInProgress,
                            lastError = state.snapshot.lastError,
                        )
                        if (initialized) {
                            base
                        } else {
                            initialized = true
                            base.copy(
                                mode = if (state.hasCredentials) ScreenMode.SETTINGS else ScreenMode.WIZARD,
                                wizardStep = if (state.hasCredentials) WizardStep.DONE else WizardStep.WELCOME,
                                authToken = InputSanitizer.noWhitespace(stored?.authToken.orEmpty()),
                                userId = InputSanitizer.noWhitespace(stored?.userId.orEmpty()),
                            )
                        }
                    }
                }
        }
    }

    fun openWizard() {
        _uiState.update {
            it.copy(
                mode = ScreenMode.WIZARD,
                wizardStep = WizardStep.WELCOME,
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
                infoMessage = null,
            )
        }
    }

    fun openSettings() {
        _uiState.update {
            it.copy(
                mode = ScreenMode.SETTINGS,
                wizardStep = WizardStep.DONE,
                infoMessage = null,
            )
        }
    }

    fun wizardNext() {
        val state = uiState.value
        val canProceed = WizardStateMachine.canProceed(
            step = state.wizardStep,
            userId = state.userId,
            token = state.authToken,
            connectionCheckPassed = state.connectionCheckPassed,
        )

        when (state.wizardStep) {
            WizardStep.WELCOME -> {
                _uiState.update { it.copy(wizardStep = WizardStateMachine.next(it.wizardStep), infoMessage = null) }
            }

            WizardStep.USER_ID -> {
                if (!canProceed) {
                    setInfo(R.string.error_user_id_required)
                    return
                }
                _uiState.update { it.copy(wizardStep = WizardStateMachine.next(it.wizardStep), infoMessage = null) }
            }

            WizardStep.TOKEN -> {
                if (!canProceed) {
                    setInfo(R.string.error_token_required)
                    return
                }
                _uiState.update {
                    it.copy(
                        wizardStep = WizardStateMachine.next(it.wizardStep),
                        connectionCheckPassed = false,
                        lastConnectionCheckResult = null,
                        infoMessage = null,
                    )
                }
            }

            WizardStep.CHECK_CONNECTION -> {
                if (!canProceed) {
                    setInfo(R.string.error_connection_check_required)
                    return
                }
                completeWizard()
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
                endpoint = InputSanitizer.endpoint(value),
                connectionCheckPassed = false,
                lastConnectionCheckResult = null,
            )
        }
    }

    fun resetEndpointToDefault() {
        _uiState.update {
            it.copy(
                endpoint = Defaults.defaultEndpoint,
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

    fun refreshNow() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.syncInProgress) return@launch
            val nextAutoRefreshAtMs = state.nextAutoRefreshAtMs
            if (
                state.lastError == SyncError.RATE_LIMITED &&
                nextAutoRefreshAtMs != null &&
                nextAutoRefreshAtMs > System.currentTimeMillis()
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
            appContainer.settingsStore.setSyncInProgress(true)
            appContainer.workScheduler.enqueueManualRefresh()
            appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
            setInfo(R.string.info_refresh_queued)
        }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
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

    private fun completeWizard() {
        viewModelScope.launch {
            val state = uiState.value
            val endpoint = InputSanitizer.endpoint(state.endpoint)
            if (!state.connectionCheckPassed) {
                setInfo(R.string.error_connection_check_required)
                return@launch
            }

            saveAll(
                endpoint = endpoint,
                token = state.authToken,
                userId = state.userId,
            )
            appContainer.settingsStore.setSyncInProgress(true)
            appContainer.workScheduler.enqueueManualRefresh(showToastOnResult = true)
            appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
            _uiState.update {
                it.copy(
                    mode = ScreenMode.SETTINGS,
                    wizardStep = WizardStep.DONE,
                    infoMessage = appContainer.appContext.getString(R.string.info_wizard_done),
                )
            }
        }
    }

    private suspend fun saveAll(
        endpoint: String,
        token: String,
        userId: String,
    ) {
        val normalizedEndpoint = InputSanitizer.endpoint(endpoint)
        val normalizedToken = InputSanitizer.noWhitespace(token)
        val normalizedUserId = InputSanitizer.noWhitespace(userId)
        val autoRefreshEnabled = uiState.value.autoRefreshEnabled
        appContainer.settingsStore.updateCredentialsSettings(normalizedEndpoint, autoRefreshEnabled)
        appContainer.credentialsStore.save(normalizedToken, normalizedUserId)
        appContainer.widgetUpdateCoordinator.forceWidgetUpdateNow()
        _uiState.update {
            it.copy(
                endpoint = normalizedEndpoint,
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
            ConnectionCheckResult.BOT_UNAVAILABLE -> R.string.connection_bot_unavailable
            ConnectionCheckResult.NETWORK_ERROR -> R.string.connection_network_error
            ConnectionCheckResult.UNKNOWN -> R.string.connection_unknown_error
        }
        return appContainer.appContext.getString(resId)
    }
}

class MainViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(appContainer) as T
    }
}
