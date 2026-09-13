package de.filefly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.filefly.common.Role
import de.filefly.core.data.SettingsStore
import de.filefly.core.network.ApiResult
import de.filefly.core.network.FileFlyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Einstellungen: Server-URL + Chunk-Größe anzeigen/ändern, Verbindung testen (/health),
// aktuelle Rolle + base_path zeigen, abmelden. Update-Check läuft über den Updater-Screen.
class SettingsViewModel(
    private val api: FileFlyApi,
    private val settings: SettingsStore,
) : ViewModel() {
    data class UiState(
        val serverUrl: String = "",
        val chunkSizeKb: Int = SettingsStore.DEFAULT_CHUNK_KB,
        val role: Role = Role.UNKNOWN,
        val basePath: String = "",
        val connectionResult: String? = null,
        val busy: Boolean = false,
        val loggedOut: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    serverUrl = settings.serverUrl.first(),
                    chunkSizeKb = settings.chunkSizeKb.first(),
                    role = settings.role.first(),
                    basePath = settings.basePath.first().ifBlank { "/" },
                )
        }
    }

    fun onServerUrlChange(url: String) {
        _state.value = _state.value.copy(serverUrl = url, connectionResult = null)
    }

    fun onChunkSizeChange(kb: Int) {
        _state.value = _state.value.copy(chunkSizeKb = kb)
        viewModelScope.launch { settings.setChunkSizeKb(kb) }
    }

    fun saveServerUrl() {
        viewModelScope.launch { settings.setServerUrl(_state.value.serverUrl.trim()) }
    }

    fun testConnection() {
        _state.value = _state.value.copy(busy = true, connectionResult = null)
        viewModelScope.launch {
            settings.setServerUrl(_state.value.serverUrl.trim())
            _state.value =
                when (val result = api.health()) {
                    is ApiResult.Ok -> _state.value.copy(busy = false, connectionResult = "✓ erreichbar")
                    is ApiResult.Failure -> _state.value.copy(busy = false, connectionResult = "✗ ${result.message}")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.logout()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}
