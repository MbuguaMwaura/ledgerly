package app.ledgerly.android.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.ledgerly.android.MainActivity
import app.ledgerly.android.R
import app.ledgerly.shared.domain.rules.REMINDER_DAYS_AHEAD

/**
 * The daily "nothing logged today" nudge. The shared app state works out the next week of times
 * (skipping today once something is logged); this keeps one inexact alarm per time, replacing them
 * whenever the times change. The times are kept in preferences so they survive a restart.
 */
object Reminders {
    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1
    private const val PREFS = "reminders"
    private const val KEY_TIMES = "times"
    private const val KEY_MESSAGE = "message"
    private const val EXTRA_MESSAGE = "message"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Replaces every scheduled reminder with [times] (epoch ms). An empty list cancels them all. */
    fun schedule(context: Context, times: List<Long>, message: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TIMES, times.joinToString(","))
            .putString(KEY_MESSAGE, message)
            .apply()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        for (slot in 0 until REMINDER_DAYS_AHEAD) {
            alarms.cancel(pendingIntent(context, slot, message))
        }
        times.filter { it > now }.take(REMINDER_DAYS_AHEAD).forEachIndexed { slot, time ->
            // Inexact on purpose: a few minutes late is fine for a nudge, and it needs no special permission.
            alarms.set(AlarmManager.RTC_WAKEUP, time, pendingIntent(context, slot, message))
        }
    }

    /** The app is open, so a nudge still in the shade has done its job. */
    fun clearShown(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** After a reboot or an app update, alarms are gone: put back the ones still ahead. */
    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val times = prefs.getString(KEY_TIMES, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        schedule(context, times, prefs.getString(KEY_MESSAGE, "").orEmpty())
    }

    internal fun show(context: Context, message: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun pendingIntent(context: Context, slot: Int, message: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            slot,
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_MESSAGE, message),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    internal fun messageFrom(intent: Intent): String? = intent.getStringExtra(EXTRA_MESSAGE)
}

/** Fires each reminder alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = Reminders.messageFrom(intent)?.takeIf { it.isNotEmpty() } ?: return
        Reminders.show(context, message)
    }
}

/** Puts the alarms back after the phone restarts or the app is updated. */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Reminders.restore(context)
        }
    }
}
