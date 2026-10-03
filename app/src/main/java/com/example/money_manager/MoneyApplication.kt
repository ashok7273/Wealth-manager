package com.example.money_manager

import android.app.Application
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.notify.PaymentReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoneyApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        PaymentReminders.ensureChannel(this)

        // Keeps reminder alarms in step with card payment dates as they are edited.
        scope.launch {
            ServiceLocator.accountRepository.accounts.collectLatest { accounts ->
                PaymentReminders.sync(this@MoneyApplication, accounts)
            }
        }
    }
}
