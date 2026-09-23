package com.rwpiri.uptime

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rwpiri.uptime.data.EncryptedTokenStore
import com.rwpiri.uptime.data.SettingsStore
import com.rwpiri.uptime.data.UptimeRepository
import com.rwpiri.uptime.data.WorkerStateStore
import java.util.concurrent.TimeUnit

class UptimeApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class AppContainer(application: Application) {
    private val settings = SettingsStore(application)
    private val tokens = EncryptedTokenStore(application)
    val repository = UptimeRepository(settings, tokens)
    val workerState = WorkerStateStore(application)
}
