package com.rwpiri.uptime.data

import java.time.Instant

/** Parses an RFC3339 check timestamp, returning null when the server data is malformed. */
internal fun parseCheckedAt(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

/** Selects the latest check by parsed timestamp, with the check id as a stable tie-breaker. */
internal fun latestCheck(checks: List<Check>): Check? = checks
    .mapNotNull { check -> parseCheckedAt(check.checkedAt)?.let { check to it } }
    .maxWithOrNull(compareBy<Pair<Check, Instant>> { it.second }.thenBy { it.first.id })
    ?.first

/** Orders checks newest-first by parsed timestamp, with the check id as a stable tie-breaker. */
internal fun checksNewestFirst(checks: List<Check>): List<Check> = checks
    .mapNotNull { check -> parseCheckedAt(check.checkedAt)?.let { check to it } }
    .sortedWith(compareByDescending<Pair<Check, Instant>> { it.second }.thenByDescending { it.first.id })
    .map { it.first }
