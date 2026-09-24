package com.rwpiri.uptime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_READ) return
        val targetId = intent.getLongExtra(EXTRA_TARGET_ID, -1L)
        val incidentType = intent.getStringExtra(EXTRA_INCIDENT_TYPE).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (targetId < 0L || incidentType.isBlank() || notificationId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as UptimeApplication).container.repository
                val incident = repository.latestIncident(targetId, incidentType)
                if (!incident.isRead) repository.markIncidentRead(incident.id)
                NotificationManagerCompat.from(context).cancel(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_READ = "com.rwpiri.uptime.MARK_NOTIFICATION_READ"
        const val EXTRA_TARGET_ID = "com.rwpiri.uptime.NOTIFICATION_TARGET_ID"
        const val EXTRA_INCIDENT_TYPE = "com.rwpiri.uptime.NOTIFICATION_INCIDENT_TYPE"
        const val EXTRA_NOTIFICATION_ID = "com.rwpiri.uptime.NOTIFICATION_ID"
    }
}
