package com.example.money_manager.ui.stats

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.TransactionRepository
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth

class StatsViewModel(
    repository: TransactionRepository = ServiceLocator.transactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.transactions

    private val _period = MutableStateFlow(StatsPeriod.MONTHLY)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    private val _anchor = MutableStateFlow(LocalDate.now())
    val anchor: StateFlow<LocalDate> = _anchor.asStateFlow()

    private val _customRange = MutableStateFlow(
        YearMonth.now().let { DateRange(it.atDay(1), it.atEndOfMonth()) }
    )
    val customRange: StateFlow<DateRange> = _customRange.asStateFlow()

    private val _type = MutableStateFlow(TransactionType.EXPENSE)
    val type: StateFlow<TransactionType> = _type.asStateFlow()

    fun setPeriod(period: StatsPeriod) {
        _period.value = period
    }

    /** Called when the Stats tab is entered, so it always opens on the current month. */
    fun resetToCurrentMonth() {
        _period.value = StatsPeriod.MONTHLY
        _anchor.value = LocalDate.now()
    }

    fun setType(type: TransactionType) {
        _type.value = type
    }

    fun shiftRange(direction: Int) {
        _anchor.value = _period.value.shift(_anchor.value, direction)
    }

    fun setCustomStart(date: LocalDate) {
        _customRange.value = _customRange.value.copy(start = date)
    }

    fun setCustomEnd(date: LocalDate) {
        _customRange.value = _customRange.value.copy(end = date)
    }
}
