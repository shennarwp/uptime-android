package com.rwpiri.uptime.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Check(
    val id: Long,
    @SerialName("target_id") val targetId: Long,
    @SerialName("checked_at") val checkedAt: String,
    @SerialName("is_up") val isUp: Boolean,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("response_time_ms") val responseTimeMs: Long? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
data class Target(
    val id: Long,
    val name: String,
    val url: String,
    val schedule: String,
    @SerialName("cert_expires_at") val certExpiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TargetWithChecks(
    val id: Long,
    val name: String,
    val url: String,
    val schedule: String,
    @SerialName("cert_expires_at") val certExpiresAt: String? = null,
    val checks: List<Check> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class CreateTargetRequest(val name: String, val url: String, val schedule: String)

@Serializable
data class UpdateTargetRequest(val name: String, val schedule: String)

data class TargetDraft(
    val name: String = "",
    val url: String = "",
    val schedule: String = "0 */5 * * * *",
)

@Serializable
data class Incident(
    val id: Long,
    @SerialName("target_id") val targetId: Long,
    @SerialName("target_name") val targetName: String,
    @SerialName("target_url") val targetUrl: String,
    val type: String,
    val cause: String? = null,
    val timestamp: String,
    @SerialName("is_read") val isRead: Boolean = false,
    val resolved: Boolean = false,
)
