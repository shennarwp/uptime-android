package com.rwpiri.uptime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rwpiri.uptime.data.Check
import com.rwpiri.uptime.data.TargetWithChecks
import com.rwpiri.uptime.data.WorkerState
import com.rwpiri.uptime.data.latestCheck
import com.rwpiri.uptime.data.parseCheckedAt
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
                val checks = target.checks.filter { check -> lastRun == null || parseCheckedAt(check.checkedAt)?.isAfter(lastRun) == true }
                val newest = latestCheck(checks)
                if (newest != null) {
                    val oldState = nextStates[target.id.toString()]
                    if (newest.isUp && oldState == false) {
                        notify(target, "Recovered", "${target.name} is back up", "going_up")
                    }
                    if (!newest.isUp) notify(target, "Down", downMessage(target, newest), "going_down")
                    nextStates[target.id.toString()] = newest.isUp
                }
                certificateNotice(target, now, nextCertAlerts)
            }
        } else {
            targets.forEach { target ->
                latestCheck(target.checks)?.let { nextStates[target.id.toString()] = it.isUp }
            }
        }

        app.container.workerState.save(
            WorkerState(now.toString(), nextStates, nextCertAlerts),
        )
        return Result.success()
    }

    private fun certificateNotice(target: TargetWithChecks, now: Instant, sent: MutableMap<String, String>) {
        val expiry = target.certExpiresAt?.let(::parseCheckedAt) ?: return
        val days = ceil(Duration.between(now, expiry).toHours() / 24.0).toInt()
        if (days > 30) return
        val key = target.id.toString()
        val bucket = if (days <= 10) now.toString().take(10) else "30"
        if (sent[key] != bucket) {
            val incidentType = when {
                days < 0 -> "cert_expired"
                days <= 10 -> "cert_10_days"
                else -> "cert_30_days"
            }
            notify(target, "Certificate expiry", "${target.name} certificate expires in $days day(s)", incidentType)
            sent[key] = bucket
        }
    }

    private fun downMessage(target: TargetWithChecks, check: Check): String = buildString {
        append(target.name)
        check.statusCode?.let { append(" returned HTTP $it") }
        check.errorMessage?.takeIf(String::isNotBlank)?.let { append(": $it") }
    }

    private fun notify(target: TargetWithChecks, title: String, text: String, incidentType: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationChannels.ensure(applicationContext)
        val markReadIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_TARGET_ID, target.id)
            putExtra(NotificationActionReceiver.EXTRA_INCIDENT_TYPE, incidentType)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, target.id.toInt())
        }
        val markReadPendingIntent = android.app.PendingIntent.getBroadcast(
            applicationContext,
            target.id.toInt(),
            markReadIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(
                TaskStackBuilder.create(applicationContext).run {
                    addNextIntentWithParentStack(Intent(applicationContext, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_OPEN_INCIDENTS
                        putExtra(MainActivity.EXTRA_NOTIFICATION_TARGET_ID, target.id)
                        putExtra(MainActivity.EXTRA_NOTIFICATION_INCIDENT_TYPE, incidentType)
                    })
                    getPendingIntent(target.id.toInt(), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                },
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.notification_icon,
                    "Mark as read",
                    markReadPendingIntent,
                ).build(),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(target.id.toInt(), notification)
    }

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
