package com.managesc.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.managesc.app.MainActivity
import com.managesc.app.R
import com.managesc.app.data.VpsDbHelper
import com.managesc.app.ui.expiryInfo
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        checkExpiry(applicationContext)
        return Result.success()
    }

    private fun checkExpiry(context: Context) {
        val list = VpsDbHelper(context).getAll()
        val expiring = mutableListOf<String>()
        for (v in list) {
            if (v.tipeAkun.equals("Unlimited", true) || v.masaAktif.isBlank()) continue
            val info = expiryInfo(v)
            if (info.days in 0..7) {
                expiring.add("${v.username} (${v.ipVps}) — ${if (info.days == 0) "hari ini" else "${info.days} hari lagi"}")
            }
        }
        if (expiring.isNotEmpty()) showNotification(context, expiring)
    }

    private fun showNotification(context: Context, items: List<String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "expiry_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId, "VPS Kadaluarsa",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = items.joinToString("\n")
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_expiry_title))
            .setContentText("${items.size} VPS akan kadaluarsa")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, notif)
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "expiry_check",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }
    }
}
