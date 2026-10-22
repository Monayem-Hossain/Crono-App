package com.monayem.crono

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monayem.crono.data.AppDatabase
import com.monayem.crono.data.TimeUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CTWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.scheduleDao()
        val notificationManager = CTNotificationManager(applicationContext)

        val calendar = Calendar.getInstance()
        val todayDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)

        val schedule = dao.getSchedule().first() ?: return Result.success()
        val subjectsWithClassTests = dao.getSubjectsWithClassTests(schedule.id).first()

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)

        // Find the most recent/immediate CT
        val immediateCT = subjectsWithClassTests.flatMap { swct ->
            swct.classTestEntries.map { swct.subject to it }
        }.filter { (_, ct) ->
            !ct.isConfirmed && (ct.date == todayDateStr || (sdf.parse(ct.date)
                ?.after(calendar.time) == true))
        }.sortedBy { (_, ct) ->
            sdf.parse(ct.date)?.time ?: Long.MAX_VALUE
        }.firstOrNull()

        if (immediateCT != null) {
            val (subject, ct) = immediateCT
            if (ct.date == todayDateStr) {
                if (isTimeNear(ct.startTime, calendar)) {
                    notificationManager.showCTUpcomingNotification(subject, ct)
                } else if (isTimePassed(ct.startTime, calendar)) {
                    notificationManager.showCTConfirmationNotification(subject, ct)
                }
            }
        }

        return Result.success()
    }

    private fun isTimeNear(targetTime: String, calendar: Calendar): Boolean {
        val targetMinutes = TimeUtils.getMinutesOfDay(targetTime) ?: return false
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val diff = targetMinutes - currentMinutes
        return diff in 0..60
    }

    private fun isTimePassed(timeStr: String, currentTime: Calendar): Boolean =
        TimeUtils.isTimePassed(timeStr, currentTime)
}
