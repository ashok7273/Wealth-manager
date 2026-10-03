package com.example.money_manager.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.money_manager.MainActivity
import com.example.money_manager.R
import com.example.money_manager.data.model.Account
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Monthly "your card bill is due" reminders, driven by each card's payment day. */
object PaymentReminders {

    private const val CHANNEL_ID = "card_payment_reminders"
    private const val ACTION_REMIND = "com.example.money_manager.CARD_PAYMENT_DUE"
    const val EXTRA_ACCOUNT_ID = "accountId"
    const val EXTRA_ACCOUNT_NAME = "accountName"
    const val EXTRA_PAYMENT_DAY = "paymentDay"

    /** Reminders fire in the morning so there is a full day to act on them. */
    private val REMIND_AT: LocalTime = LocalTime.of(9, 0)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Card payment reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you on the payment date set for each card."
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /** Re-arms one alarm per visible card and clears alarms for every other account. */
    fun sync(context: Context, accounts: List<Account>) {
        if (accounts.isEmpty()) return
        ensureChannel(context)
        accounts.forEach { account ->
            val day = account.paymentDay?.takeIf { account.group.isCard && !account.hidden }
            if (day == null) {
                cancel(context, account.id, account.name)
            } else {
                schedule(context, account.id, account.name, day)
            }
        }
    }

    fun schedule(context: Context, accountId: Long, accountName: String, paymentDay: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = nextOccurrence(paymentDay)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        alarms.set(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pendingIntent(context, accountId, accountName, paymentDay)
        )
    }

    fun cancel(context: Context, accountId: Long, accountName: String) {
        context.getSystemService(AlarmManager::class.java)
            ?.cancel(pendingIntent(context, accountId, accountName, paymentDay = 1))
    }

    fun notifyDue(context: Context, accountId: Long, accountName: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            accountId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("$accountName payment due")
            .setContentText("Today is the payment date for $accountName.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        // Silently dropped when the user has not granted POST_NOTIFICATIONS.
        runCatching {
            NotificationManagerCompat.from(context).notify(accountId.toInt(), notification)
        }
    }

    private fun nextOccurrence(
        paymentDay: Int,
        from: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val thisMonth = onDayOf(from.toLocalDate(), paymentDay)
        return if (thisMonth.isAfter(from)) {
            thisMonth
        } else {
            onDayOf(from.toLocalDate().plusMonths(1), paymentDay)
        }
    }

    private fun onDayOf(reference: LocalDate, day: Int): LocalDateTime =
        LocalDateTime.of(
            reference.withDayOfMonth(day.coerceIn(1, reference.lengthOfMonth())),
            REMIND_AT
        )

    private fun pendingIntent(
        context: Context,
        accountId: Long,
        accountName: String,
        paymentDay: Int
    ): PendingIntent {
        val intent = Intent(context, CardReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_ACCOUNT_ID, accountId)
            putExtra(EXTRA_ACCOUNT_NAME, accountName)
            putExtra(EXTRA_PAYMENT_DAY, paymentDay)
        }
        return PendingIntent.getBroadcast(
            context,
            accountId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
