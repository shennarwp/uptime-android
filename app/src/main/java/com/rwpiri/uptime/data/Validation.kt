package com.rwpiri.uptime.data

import java.net.URI
import java.net.URISyntaxException

private const val MAX_TARGET_NAME_LENGTH = 100

object InputValidation {
    fun serverUrl(raw: String): String? = UptimeApiFactory.validateBaseUrl(raw)

    fun targetName(raw: String): String? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> "Name is required"
            value.length > MAX_TARGET_NAME_LENGTH -> "Name must be at most $MAX_TARGET_NAME_LENGTH characters"
            value.any(Char::isISOControl) -> "Name must not contain control characters"
            else -> null
        }
    }

    fun targetUrl(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return "URL is required"
        val uri = try { URI(value) } catch (_: URISyntaxException) { return "Enter a valid URL" }
        return when {
            uri.scheme !in setOf("http", "https") -> "URL must use http or https"
            uri.host.isNullOrBlank() -> "URL must include a host"
            else -> null
        }
    }

    fun schedule(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return "Schedule is required"
        if (value.startsWith("@")) {
            val descriptor = value.substringBefore(' ')
            val valid = descriptor in setOf("@yearly", "@annually", "@monthly", "@weekly", "@daily", "@midnight", "@hourly", "@every")
            val interval = value.substringAfter(' ', "")
            val validInterval = interval.matches(Regex("\\d+(ms|s|m|h)"))
            return if (valid && (descriptor != "@every" || validInterval)) null else "Enter a valid cron schedule"
        }
        val fields = value.split(Regex("\\s+"))
        return if (fields.size == 6 && fields.all { it.matches(Regex("[0-9*/?,LW#-]+")) }) null else
            "Use a six-field cron schedule (second minute hour day month weekday)"
    }

    fun targetDraft(draft: TargetDraft, includeUrl: Boolean): String? =
        targetName(draft.name) ?: schedule(draft.schedule) ?: if (includeUrl) targetUrl(draft.url) else null
}
