package com.rwpiri.uptime.data

import retrofit2.HttpException

class AuthenticationExpiredException : IllegalStateException("Session expired. Please log in again.")
class InvalidTokenException : IllegalStateException("The API token was rejected.")

class UptimeRepository(
    private val settings: SettingsStore,
    private val tokens: EncryptedTokenStore,
) {
    suspend fun targets(): List<TargetWithChecks> = api().getTargets()

    suspend fun verifyAndSaveToken(token: String) {
        try { api().verifyToken(bearer(token)) }
        catch (error: HttpException) { if (error.code() == 401) throw InvalidTokenException() else throw error }
        tokens.save(token)
    }

    suspend fun create(draft: TargetDraft): Target = authenticated { api().createTarget(
        bearer(requireToken()),
        CreateTargetRequest(draft.name.trim(), draft.url.trim(), draft.schedule.trim()),
    ) }

    suspend fun update(id: Long, draft: TargetDraft): Target = authenticated { api().updateTarget(
        bearer(requireToken()),
        id,
        UpdateTargetRequest(draft.name.trim(), draft.schedule.trim()),
    ) }

    suspend fun delete(id: Long) = authenticated { api().deleteTarget(bearer(requireToken()), id) }

    suspend fun incidents(): List<Incident> = authenticated { api().getIncidents(bearer(requireToken())) }
    suspend fun latestIncident(targetId: Long, type: String): Incident = authenticated {
        api().getLatestIncident(bearer(requireToken()), targetId, type)
    }
    suspend fun markIncidentRead(id: Long) = authenticated { api().markIncidentRead(bearer(requireToken()), id) }
    suspend fun markAllIncidentsRead() = authenticated { api().markAllIncidentsRead(bearer(requireToken())) }

    suspend fun getToken(): String? = tokens.get()
    suspend fun clearToken() = tokens.clear()
    suspend fun serverUrl(): String = settings.getServerUrl()
    suspend fun saveServerUrl(value: String) = settings.setServerUrl(value)

    private suspend fun requireToken(): String = tokens.get() ?: error("Login required")

    private suspend fun <T> authenticated(action: suspend () -> T): T = try { action() }
    catch (error: HttpException) {
        if (error.code() == 401) { tokens.clear(); throw AuthenticationExpiredException() }
        throw error
    }

    private suspend fun api(): UptimeApi = UptimeApiFactory.create(settings.getServerUrl())
        ?: error("Set a valid server URL in Settings")

    private fun bearer(token: String) = "Bearer ${token.trim()}"
}
