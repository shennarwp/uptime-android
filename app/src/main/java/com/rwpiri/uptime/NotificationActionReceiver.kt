package com.rwpiri.uptime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_READ) return
        val targetId = intent.getLongExtra(EXTRA_TARGET_ID, -1L)
        val incidentType = intent.getStringExtra(EXTRA_INCIDENT_TYPE).orEmpty()
        val incidentTimestamp = intent.getStringExtra(EXTRA_INCIDENT_TIMESTAMP)
        val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (targetId < 0L || incidentType.isBlank() || notificationTag.isBlank() || notificationId < 0) return

        // Dismiss immediately; WorkManager retries the server acknowledgement if
        // the phone is offline or the request fails transiently.
        NotificationManagerCompat.from(context).cancel(notificationTag, notificationId)
        val input = Data.Builder()
            .putLong(MarkIncidentReadWorker.KEY_TARGET_ID, targetId)
            .putString(MarkIncidentReadWorker.KEY_INCIDENT_TYPE, incidentType)
            .putString(MarkIncidentReadWorker.KEY_INCIDENT_TIMESTAMP, incidentTimestamp)
            .build()
        val request = OneTimeWorkRequestBuilder<MarkIncidentReadWorker>()
            .setInputData(input)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        val uniqueName = "mark-incident-read-$targetId-$incidentType-${incidentTimestamp.orEmpty().hashCode()}"
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val ACTION_MARK_READ = "com.rwpiri.uptime.MARK_NOTIFICATION_READ"
        const val EXTRA_TARGET_ID = "com.rwpiri.uptime.NOTIFICATION_TARGET_ID"
        const val EXTRA_INCIDENT_TYPE = "com.rwpiri.uptime.NOTIFICATION_INCIDENT_TYPE"
        const val EXTRA_INCIDENT_TIMESTAMP = "com.rwpiri.uptime.NOTIFICATION_INCIDENT_TIMESTAMP"
        const val EXTRA_NOTIFICATION_TAG = "com.rwpiri.uptime.NOTIFICATION_TAG"
        const val EXTRA_NOTIFICATION_ID = "com.rwpiri.uptime.NOTIFICATION_ID"
        const val NOTIFICATION_ID = 0
    }
}

class MarkIncidentReadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val targetId = inputData.getLong(KEY_TARGET_ID, -1L)
        val incidentType = inputData.getString(KEY_INCIDENT_TYPE).orEmpty()
        val incidentTimestamp = inputData.getString(KEY_INCIDENT_TIMESTAMP)
        if (targetId < 0L || incidentType.isBlank()) return Result.failure()

        return runCatching {
            val repository = (applicationContext as UptimeApplication).container.repository
            val incidents = repository.incidents()
            val matching = incidents.filter { it.targetId == targetId && it.type == incidentType }
            val incident = incidentTimestamp?.let { timestamp ->
                matching.firstOrNull { it.timestamp == timestamp }
            } ?: matching.firstOrNull()
            if (incident != null && !incident.isRead) repository.markIncidentRead(incident.id)
            Result.success()
        }.getOrElse { error ->
            if (error is retrofit2.HttpException && error.code() == 401) Result.failure()
            else Result.retry()
        }
    }

    companion object {
        const val KEY_TARGET_ID = "target_id"
        const val KEY_INCIDENT_TYPE = "incident_type"
        const val KEY_INCIDENT_TIMESTAMP = "incident_timestamp"
    }
}
