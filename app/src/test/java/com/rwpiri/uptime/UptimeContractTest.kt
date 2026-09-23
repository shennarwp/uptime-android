package com.rwpiri.uptime

import com.rwpiri.uptime.data.InputValidation
import com.rwpiri.uptime.data.UptimeApiFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UptimeContractTest {
    @Test fun normalizesServerOriginAndVersionedPath() {
        assertEquals("https://monitor.example/api/v1/", UptimeApiFactory.normalizeBaseUrl(" https://monitor.example/ "))
        assertEquals("http://localhost:8080/api/v1/", UptimeApiFactory.normalizeBaseUrl("http://localhost:8080/api/v1"))
    }

    @Test fun rejectsAmbiguousServerUrls() {
        assertNull(UptimeApiFactory.normalizeBaseUrl("monitor.example"))
        assertNull(UptimeApiFactory.normalizeBaseUrl("https://monitor.example/dashboard"))
        assertNull(UptimeApiFactory.normalizeBaseUrl("https://monitor.example/api/v1?token=x"))
    }

    @Test fun validatesBackendTargetRules() {
        assertNull(InputValidation.targetName("Production"))
        assertEquals("URL must use http or https", InputValidation.targetUrl("ftp://example.com"))
        assertEquals("Use a six-field cron schedule (second minute hour day month weekday)", InputValidation.schedule("*/5 * * * *"))
        assertNull(InputValidation.schedule("0 */5 * * * *"))
        assertNull(InputValidation.schedule("@hourly"))
        assertEquals("Enter a valid cron schedule", InputValidation.schedule("@bogus"))
    }
}
