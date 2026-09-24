package com.rwpiri.uptime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rwpiri.uptime.data.TargetDraft
import com.rwpiri.uptime.data.TargetWithChecks
import com.rwpiri.uptime.data.Incident
import com.rwpiri.uptime.data.UptimeRepository
import com.rwpiri.uptime.data.AuthenticationExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class UptimeUiState(
    val loading: Boolean = true,
    val targets: List<TargetWithChecks> = emptyList(),
    val serverUrl: String = "",
    val loggedIn: Boolean = false,
    val error: String? = null,
    val lastSyncAt: String? = null,
    val requiresLogin: Boolean = false,
    val refreshing: Boolean = false,
    val incidents: List<Incident> = emptyList(),
)

class UptimeViewModel(private val repository: UptimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(UptimeUiState())
    val state: StateFlow<UptimeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val previous = _state.value
        val hasContent = previous.serverUrl.isNotBlank()
        _state.value = previous.copy(loading = !hasContent, refreshing = hasContent, error = null)
        runCatching {
            val url = repository.serverUrl()
            val loggedIn = repository.getToken() != null
            if (url.isBlank()) UptimeUiState(loading = false, serverUrl = url, loggedIn = loggedIn)
            else UptimeUiState(false, repository.targets(), url, loggedIn, lastSyncAt = Instant.now().toString(), incidents = if (loggedIn) repository.incidents() else emptyList())
        }.onSuccess { _state.value = it.copy(refreshing = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, refreshing = false, error = it.message ?: "Request failed") }
    }

    fun saveServerUrl(url: String) = viewModelScope.launch {
        repository.saveServerUrl(url)
        refresh()
    }

    fun login(token: String) = viewModelScope.launch {
        runCatching { repository.verifyAndSaveToken(token) }
            .onSuccess { _state.value = _state.value.copy(error = null, requiresLogin = false); refresh() }
            .onFailure { _state.value = _state.value.copy(error = it.message ?: "Login failed") }
    }

    fun logout() = viewModelScope.launch { repository.clearToken(); refresh() }

    fun refreshIncidents() = viewModelScope.launch {
        runCatching { repository.incidents() }.onSuccess { incidents ->
            _state.value = _state.value.copy(incidents = incidents)
        }.onFailure { error -> handleMutationError(error, "Unable to load incidents") }
    }

    fun markIncidentRead(id: Long) = viewModelScope.launch {
        runCatching { repository.markIncidentRead(id) }.onSuccess {
            _state.value = _state.value.copy(incidents = _state.value.incidents.map { incident ->
                if (incident.id == id) incident.copy(isRead = true) else incident
            })
        }.onFailure { error -> handleMutationError(error, "Unable to mark incident read") }
    }

    fun markAllIncidentsRead() = viewModelScope.launch {
        runCatching { repository.markAllIncidentsRead() }.onSuccess {
            _state.value = _state.value.copy(incidents = _state.value.incidents.map { it.copy(isRead = true) })
        }.onFailure { error -> handleMutationError(error, "Unable to mark incidents read") }
    }

    fun create(draft: TargetDraft) = mutate { repository.create(draft) }
    fun update(id: Long, draft: TargetDraft) = mutate { repository.update(id, draft) }
    fun delete(id: Long) = viewModelScope.launch {
        runCatching { repository.delete(id) }.onSuccess { refresh() }
            .onFailure { error -> handleMutationError(error, "Delete failed") }
    }

    private fun mutate(action: suspend () -> Any) = viewModelScope.launch {
        runCatching { action() }.onSuccess { refresh() }
            .onFailure { error -> handleMutationError(error, "Request failed") }
    }

    private fun handleMutationError(error: Throwable, fallback: String) {
        _state.value = if (error is AuthenticationExpiredException) {
            _state.value.copy(loggedIn = false, requiresLogin = true, error = error.message)
        } else _state.value.copy(error = error.message ?: fallback)
    }
}

class UptimeViewModelFactory(private val repository: UptimeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = UptimeViewModel(repository) as T
}
