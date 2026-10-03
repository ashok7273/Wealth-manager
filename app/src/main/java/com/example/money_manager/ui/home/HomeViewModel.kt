package com.example.money_manager.ui.home

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.TransactionRepository
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

enum class HomeTab(val label: String) {
    DAILY("Daily"),
    CALENDAR("Calendar"),
    MONTHLY("Monthly"),
    TOTAL("Total");

    /** The Monthly tab pages by year; the others page by month. */
    val isYearPaged: Boolean get() = this == MONTHLY
}

class HomeViewModel(
    private val repository: TransactionRepository = ServiceLocator.transactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.transactions

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(HomeTab.DAILY)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    fun applyFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun clearFilter() {
        _filter.value = TransactionFilter()
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        repository.deleteAll(_selectedIds.value)
        _selectedIds.value = emptySet()
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedMonth.value = _selectedMonth.value.withYear(year)
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun openSearch() {
        _searchActive.value = true
    }

    fun closeSearch() {
        _searchActive.value = false
        _query.value = ""
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    companion object {
        /** First page of the month pager. */
        val PAGER_ANCHOR: YearMonth = YearMonth.of(2000, 1)
        const val PAGER_MONTH_COUNT = 1_200
        const val PAGER_ANCHOR_YEAR = 2000
        const val PAGER_YEAR_COUNT = 100

        fun pageOf(month: YearMonth): Int =
            (month.year - PAGER_ANCHOR.year) * 12 + (month.monthValue - PAGER_ANCHOR.monthValue)

        fun monthOf(page: Int): YearMonth = PAGER_ANCHOR.plusMonths(page.toLong())

        fun yearPageOf(year: Int): Int = (year - PAGER_ANCHOR_YEAR).coerceIn(0, PAGER_YEAR_COUNT - 1)

        fun yearOf(page: Int): Int = PAGER_ANCHOR_YEAR + page
    }
}
