package de.filefly.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.filefly.core.data.SettingsStore
import de.filefly.core.network.ApiResult
import de.filefly.core.network.FileFlyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Onboarding: Server-URL setzen, Verbindung testen (/health), Invite-Code einlösen
// (/auth/invite/validate) und Session speichern. Danach kennt die App Rolle + base_path.
class OnboardingViewModel(
    private val api: FileFlyApi,
    private val settings: SettingsStore,
) : ViewModel() {
    data class UiState(
        val serverUrl: String = "",
        val inviteCode: String = "",
        val connectionOk: Boolean? = null,
        val busy: Boolean = false,
        val error: String? = null,
        val done: Boolean = false,
        val grantedRole: String? = null,
        val grantedBasePath: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onServerUrlChange(url: String) {
        _state.value = _state.value.copy(serverUrl = url, connectionOk = null, error = null)
    }

    fun onInviteChange(code: String) {
        _state.value = _state.value.copy(inviteCode = code.trim(), error = null)
    }

    // Speichert die URL und ruft /health, um die Erreichbarkeit zu prüfen.
    fun testConnection() {
        val url = _state.value.serverUrl.trim()
        if (url.isBlank()) {
            _state.value = _state.value.copy(error = "Bitte Server-URL eingeben.")
            return
        }
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            settings.setServerUrl(url)
            when (val result = api.health()) {
                is ApiResult.Ok ->
                    _state.value = _state.value.copy(busy = false, connectionOk = true)
                is ApiResult.Failure ->
                    _state.value =
                        _state.value.copy(busy = false, connectionOk = false, error = result.message)
            }
        }
    }

    // Löst den Invite ein und speichert Token + Rolle + base_path.
    fun redeemInvite() {
        val code = _state.value.inviteCode
        if (code.isBlank()) {
            _state.value = _state.value.copy(error = "Bitte Invite-Code eingeben.")
            return
        }
        // Sicherstellen, dass die URL persistiert ist (falls "Testen" übersprungen wurde).
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            settings.setServerUrl(_state.value.serverUrl.trim())
            when (val result = api.validateInvite(code)) {
                is ApiResult.Ok -> {
                    val token = result.value
                    settings.saveSession(
                        token = token.accessToken,
                        role = token.role,
                        basePath = token.basePath,
                        permissions = token.permissions,
                    )
                    _state.value =
                        _state.value.copy(
                            busy = false,
                            done = true,
                            grantedRole = token.role,
                            grantedBasePath = token.basePath.ifBlank { "/" },
                        )
                }
                is ApiResult.Failure ->
                    _state.value = _state.value.copy(busy = false, error = result.message)
            }
        }
    }
}
