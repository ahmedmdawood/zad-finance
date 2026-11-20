package com.ahmed.zad

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ZadNotifications {

    const val CHANNEL_DIGEST = "zad_digest"
    const val CHANNEL_REMINDER = "zad_reminder"
    const val CHANNEL_QUICK_ADD = "zad_quick_add"

    const val ACTION_WEEKLY_DIGEST = "com.ahmed.zad.WEEKLY_DIGEST"
    const val ACTION_DAILY_REMINDER = "com.ahmed.zad.DAILY_REMINDER"
    const val EXTRA_OPEN_ADD_EXPENSE = "open_add_expense"

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels(context: Context) {
        manager(context).createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_DIGEST, "الملخص الأسبوعي", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_REMINDER, "تذكير يومي", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_QUICK_ADD, "إضافة سريعة", NotificationManager.IMPORTANCE_LOW),
            )
        )
    }

    /** Persistent low-priority notification that opens the add-expense form directly. */
    fun showQuickAdd(context: Context, show: Boolean) {
        if (!show) {
            manager(context).cancel(100)
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_ADD_EXPENSE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_QUICK_ADD)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("زاد")
            .setContentText("اضغط لتسجيل مصروف بسرعة")
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager(context).notify(100, notification)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ZadAlarmReceiver::class.java).setAction(ACTION_DAILY_REMINDER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        var next = LocalDate.now().atTime(hour, minute)
        if (next.isBefore(LocalDateTime.now())) next = next.plusDays(1)
        alarm.setRepeating(
            AlarmManager.RTC,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            intent,
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(
            PendingIntent.getBroadcast(
                context, 1,
                Intent(context, ZadAlarmReceiver::class.java).setAction(ACTION_DAILY_REMINDER),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
    }

    fun scheduleWeeklyDigest(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, ZadAlarmReceiver::class.java).setAction(ACTION_WEEKLY_DIGEST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        var next = LocalDate.now().with(DayOfWeek.FRIDAY).atTime(18, 0)
        if (next.isBefore(LocalDateTime.now())) next = next.plusWeeks(1)
        alarm.setRepeating(
            AlarmManager.RTC,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY * 7,
            intent,
        )
    }

    fun cancelWeeklyDigest(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(
            PendingIntent.getBroadcast(
                context, 2,
                Intent(context, ZadAlarmReceiver::class.java).setAction(ACTION_WEEKLY_DIGEST),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
    }

    fun weeklySummary(context: Context): String = runBlocking(Dispatchers.IO) {
        val dao = ZadDatabase.get(context).dao()
        val transactions = dao.transactionsFlow().first()
        val weekStart = LocalDate.now().minusDays(7)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val week = transactions.filter { it.type == "EXPENSE" && it.createdAt >= weekStart }
        if (week.isEmpty()) return@runBlocking "الأسبوع ده مفيش مصروفات مسجلة. برافو!"
        val total = week.sumOf { it.amountPiasters }
        val top = week.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountPiasters } }
            .entries.sortedByDescending { it.value }.take(3)
        buildString {
            append("صرفت ${formatMoney(total)} الأسبوع ده.")
            if (top.isNotEmpty()) {
                append(" أكتر فئات: ")
                append(top.joinToString("، ") { "${it.key} (${formatMoney(it.value)})" })
            }
        }
    }
}

class ZadAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ZadNotifications.createChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        when (intent.action) {
            ZadNotifications.ACTION_DAILY_REMINDER -> {
                val notification = NotificationCompat.Builder(context, ZadNotifications.CHANNEL_REMINDER)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("زاد")
                    .setContentText("متنساش تسجل مصاريف النهاردة")
                    .setContentIntent(openApp)
                    .setAutoCancel(true)
                    .build()
                manager.notify(200, notification)
            }
            ZadNotifications.ACTION_WEEKLY_DIGEST -> {
                val summary = ZadNotifications.weeklySummary(context)
                val notification = NotificationCompat.Builder(context, ZadNotifications.CHANNEL_DIGEST)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("ملخص الأسبوع")
                    .setContentText(summary)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
                    .setContentIntent(openApp)
                    .setAutoCancel(true)
                    .build()
                manager.notify(300, notification)
            }
        }
    }
}
