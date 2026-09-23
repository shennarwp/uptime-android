package com.rwpiri.uptime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rwpiri.uptime.data.Check
import com.rwpiri.uptime.data.TargetWithChecks
import com.rwpiri.uptime.data.WorkerState
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as UptimeApplication
        val repository = app.container.repository
        val previous = app.container.workerState.get()
        val now = Instant.now()
        val targets = runCatching { repository.targets() }.getOrElse { return Result.retry() }
        val baseline = previous.lastRun == null
        val lastRun = previous.lastRun?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val nextStates = previous.lastStates.toMutableMap()
        val nextCertAlerts = previous.certificateAlerts.toMutableMap()

        if (!baseline) {
            targets.forEach { target ->
                val checks = target.checks.filter { check -> lastRun == null || parseInstant(check.checkedAt)?.isAfter(lastRun) == true }
                val newest = checks.maxByOrNull { it.checkedAt }
                if (newest != null) {
                    val oldState = nextStates[target.id.toString()]
                    if (newest.isUp && oldState == false) notify(target, "Recovered", "${target.name} is back up")
                    if (!newest.isUp) notify(target, "Down", downMessage(target, newest))
                    nextStates[target.id.toString()] = newest.isUp
                }
                certificateNotice(target, now, nextCertAlerts)
            }
        } else {
            targets.forEach { target ->
                target.checks.maxByOrNull { it.checkedAt }?.let { nextStates[target.id.toString()] = it.isUp }
            }
        }

        app.container.workerState.save(
            WorkerState(now.toString(), nextStates, nextCertAlerts),
        )
        return Result.success()
    }

    private fun certificateNotice(target: TargetWithChecks, now: Instant, sent: MutableMap<String, String>) {
        val expiry = target.certExpiresAt?.let(::parseInstant) ?: return
        val days = ceil(Duration.between(now, expiry).toHours() / 24.0).toInt()
        if (days > 30) return
        val key = target.id.toString()
        val bucket = if (days <= 10) now.toString().take(10) else "30"
        if (sent[key] != bucket) {
            notify(target, "Certificate expiry", "${target.name} certificate expires in $days day(s)")
            sent[key] = bucket
        }
    }

    private fun downMessage(target: TargetWithChecks, check: Check): String = buildString {
        append(target.name)
        check.statusCode?.let { append(" returned HTTP $it") }
        check.errorMessage?.takeIf(String::isNotBlank)?.let { append(": $it") }
    }

    private fun notify(target: TargetWithChecks, title: String, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationChannels.ensure(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.ALERTS)
            .setSmallIcon(com.rwpiri.uptime.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(target.id.toInt(), notification)
    }

    private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

    companion object { const val WORK_NAME = "uptime-alert-poll" }
}

object NotificationChannels {
    const val ALERTS = "uptime-alerts"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(ALERTS, "Uptime alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Down, recovery, and certificate expiry alerts"
            },
        )
    }
}
