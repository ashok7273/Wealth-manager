package com.example.money_manager.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.money_manager.data.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Fires on a card's payment day, then re-arms itself for the next month. */
class CardReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accountId = intent.getLongExtra(PaymentReminders.EXTRA_ACCOUNT_ID, -1L)
        if (accountId == -1L) return
        val name = intent.getStringExtra(PaymentReminders.EXTRA_ACCOUNT_NAME) ?: "Card"
        val day = intent.getIntExtra(PaymentReminders.EXTRA_PAYMENT_DAY, 0)

        PaymentReminders.notifyDue(context, accountId, name)
        if (day in 1..31) PaymentReminders.schedule(context, accountId, name, day)
    }
}

/** Alarms do not survive a reboot, so they are rebuilt once the device is up. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        ServiceLocator.init(appContext)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val accounts = withTimeoutOrNull(8_000) {
                    ServiceLocator.accountRepository.accounts.first { it.isNotEmpty() }
                }
                PaymentReminders.sync(appContext, accounts.orEmpty())
            } finally {
                pending.finish()
            }
        }
    }
}
