package com.rwpiri.uptime

import com.rwpiri.uptime.data.InputValidation
import com.rwpiri.uptime.data.Check
import com.rwpiri.uptime.data.UptimeApiFactory
import com.rwpiri.uptime.data.checksNewestFirst
import com.rwpiri.uptime.data.latestCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UptimeContractTest {
    @Test fun normalizesServerOriginAndVersionedPath() {
        assertEquals("https://monitor.example/api/v1/", UptimeApiFactory.normalizeBaseUrl(" https://monitor.example/ "))
        assertEquals("http://localhost:8080/api/v1/", UptimeApiFactory.normalizeBaseUrl("http://localhost:8080/api/v1"))
        assertEquals("https://monitor.example/api/v1/", UptimeApiFactory.normalizeBaseUrl("https://monitor.example/api/"))
    }

    @Test fun rejectsAmbiguousServerUrls() {
        assertNull(UptimeApiFactory.normalizeBaseUrl("monitor.example"))
        assertNull(UptimeApiFactory.normalizeBaseUrl("https://monitor.example/dashboard"))
        assertNull(UptimeApiFactory.normalizeBaseUrl("https://monitor.example/api/v1?token=x"))
        assertEquals(
            "Server URL path must be empty, /api, or /api/v1",
            InputValidation.serverUrl("https://monitor.example/dashboard"),
        )
    }

    @Test fun validatesBackendTargetRules() {
        assertNull(InputValidation.targetName("Production"))
        assertEquals("URL must use http or https", InputValidation.targetUrl("ftp://example.com"))
        assertEquals("Use a six-field cron schedule (second minute hour day month weekday)", InputValidation.schedule("*/5 * * * *"))
        assertNull(InputValidation.schedule("0 */5 * * * *"))
        assertNull(InputValidation.schedule("@hourly"))
        assertEquals("Enter a valid cron schedule", InputValidation.schedule("@bogus"))
    }

    @Test fun ordersChecksByInstantRatherThanTimestampText() {
        val olderInstantWithLaterLocalTime = check(1, "2026-09-24T12:30:00+02:00")
        val newerInstant = check(2, "2026-09-24T11:00:00Z")

        assertEquals(newerInstant, latestCheck(listOf(newerInstant, olderInstantWithLaterLocalTime)))
        assertEquals(
            listOf(newerInstant, olderInstantWithLaterLocalTime),
            checksNewestFirst(listOf(olderInstantWithLaterLocalTime, newerInstant)),
        )
    }

    private fun check(id: Long, checkedAt: String) = Check(
        id = id,
        targetId = 1,
        checkedAt = checkedAt,
        isUp = true,
    )
}
