package de.filefly.feature.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.filefly.common.InviteUri
import de.filefly.common.Role
import de.filefly.core.data.SettingsStore
import de.filefly.core.network.ApiResult
import de.filefly.core.network.CreateInviteRequest
import de.filefly.core.network.FileFlyApi
import de.filefly.core.network.InviteCreateResponse
import de.filefly.core.network.InviteSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Admin-Invite-Erstellung. Baut die geteilte Invite-URI CLIENT-seitig aus der
// gespeicherten Server-URL + dem vom Server gelieferten Code (das server-`url`-Feld
// kennt die externe Adresse nicht zuverlässig, deshalb hier zusammengesetzt).
class InviteViewModel(
    private val api: FileFlyApi,
    private val settings: SettingsStore,
) : ViewModel() {
    // Rollen, die ein Admin vergeben darf (nicht ADMIN selbst — kein Selbst-Klonen von Vollzugriff).
    val assignableRoles = listOf(Role.USER, Role.SERVICE, Role.GUEST)

    data class Created(
        val response: InviteCreateResponse,
        // Fertige filefly://-URI (für QR + Deep-Link teilen).
        val shareUri: String,
    )

    data class UiState(
        val role: Role = Role.GUEST,
        val basePath: String = "",
        val expiresInHours: String = "",
        val maxUses: String = "1",
        val busy: Boolean = false,
        val error: String? = null,
        val created: Created? = null,
        val invites: List<InviteSummary> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onRoleChange(role: Role) {
        _state.value = _state.value.copy(role = role, error = null)
    }

    fun onBasePathChange(path: String) {
        _state.value = _state.value.copy(basePath = path, error = null)
    }

    fun onExpiresChange(hours: String) {
        _state.value = _state.value.copy(expiresInHours = hours.filter(Char::isDigit), error = null)
    }

    fun onMaxUsesChange(uses: String) {
        _state.value = _state.value.copy(maxUses = uses.filter(Char::isDigit), error = null)
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = api.listInvites()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(invites = result.value)
                is ApiResult.Failure -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun createInvite() {
        _state.value = _state.value.copy(busy = true, error = null, created = null)
        viewModelScope.launch {
            val server = settings.serverUrl.first()
            val request =
                CreateInviteRequest(
                    role = _state.value.role.name.lowercase(),
                    basePath = _state.value.basePath.trim(),
                    expiresInHours = _state.value.expiresInHours.toIntOrNull(),
                    maxUses = _state.value.maxUses.toIntOrNull() ?: 0,
                )
            when (val result = api.createInvite(request)) {
                is ApiResult.Ok -> {
                    val shareUri = InviteUri.build(server, result.value.code)
                    _state.value =
                        _state.value.copy(
                            busy = false,
                            created = Created(response = result.value, shareUri = shareUri),
                        )
                    refresh()
                }
                is ApiResult.Failure ->
                    _state.value = _state.value.copy(busy = false, error = result.message)
            }
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            when (val result = api.revokeInvite(id)) {
                is ApiResult.Ok -> refresh()
                is ApiResult.Failure -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun dismissCreated() {
        _state.value = _state.value.copy(created = null)
    }
}
