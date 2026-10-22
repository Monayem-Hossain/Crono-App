package com.monayem.crono

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class CronoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCTWorker()
    }

    private fun setupCTWorker() {
        val workRequest = PeriodicWorkRequestBuilder<CTWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
