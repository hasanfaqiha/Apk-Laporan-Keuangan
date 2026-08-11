package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.viewmodel.formatRupiah
import java.util.concurrent.TimeUnit

const val BILL_REMINDER_WORK = "bill_reminder_worker"
const val BILL_REMINDER_IMMEDIATE_WORK = "bill_reminder_immediate_worker"
const val BILL_REMINDER_CHANNEL_ID = "bill_reminders_channel"

/**
 * Periodic + one-time background worker that checks unpaid bills and posts
 * due/overdue notifications, and auto-generates credit-card bills for past months.
 * This replaces the previous launch-only reminder logic so users are notified
 * even when the app is not opened.
 */
class BillReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = FinanceDatabase.getDatabase(applicationContext)
            val repository = FinanceRepository(db.financeDao)

            repository.generateCreditCardBills()

            val bills = db.financeDao.getAllBillsDirect()
            val dueBills = bills.filter { !it.isPaid }
            if (dueBills.isNotEmpty()) {
                notifyDueBills(dueBills)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyDueBills(bills: List<Bill>) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BILL_REMINDER_CHANNEL_ID,
                "Pengingat Tagihan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jatuh tempo tagihan"
            }
            manager.createNotificationChannel(channel)
        }

        val now = System.currentTimeMillis()
        val threeDaysInMs = 3 * 24 * 60 * 60 * 1000L

        bills.forEach { bill ->
            val diff = bill.dueDateMillis - now
            val isOverdue = diff < 0
            val isDueSoon = diff in 0..threeDaysInMs
            if (!isOverdue && !isDueSoon) return@forEach

            val title = if (isOverdue) {
                "Tagihan Melewati Batas Tempo!"
            } else {
                "Pengingat Tagihan Terdekat"
            }
            val content = if (isOverdue) {
                "Tagihan '${bill.title}' sebesar ${formatRupiah(bill.amount)} telah melewati jatuh tempo!"
            } else {
                "Tagihan '${bill.title}' sebesar ${formatRupiah(bill.amount)} jatuh tempo dalam waktu dekat."
            }

            val builder = NotificationCompat.Builder(applicationContext, BILL_REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            manager.notify(bill.id, builder.build())
        }
    }
}

/**
 * Schedules the bill reminder worker:
 *  - a one-time run immediately (so reminders fire right after launch), and
 *  - a periodic daily run (so reminders continue even if the app is not opened).
 */
fun scheduleBillReminderWork(context: Context) {
    val workManager = WorkManager.getInstance(context)

    workManager.enqueueUniqueWork(
        BILL_REMINDER_IMMEDIATE_WORK,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<BillReminderWorker>().build()
    )

    val periodicRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
        .build()
    workManager.enqueueUniquePeriodicWork(
        BILL_REMINDER_WORK,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicRequest
    )
}
