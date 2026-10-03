package com.example.money_manager.ui.home

import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.util.formatAmount
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

data class DayGroup(
    val date: LocalDate,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val items: List<Transaction>
)

data class MonthSummary(
    val incomeMinor: Long,
    val expenseMinor: Long
) {
    val totalMinor: Long get() = incomeMinor - expenseMinor
}

data class MonthUiState(
    val yearMonth: YearMonth,
    val summary: MonthSummary,
    val days: List<DayGroup>,
    val isFiltered: Boolean
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

/**
 * Builds the Daily-tab state for one month.
 * [query] matches the note, the category name, or the amount text (e.g. "231" or "231.00").
 */
fun buildMonthState(
    transactions: List<Transaction>,
    yearMonth: YearMonth,
    query: String
): MonthUiState {
    val monthTxns = transactions.filter { YearMonth.from(it.date) == yearMonth }
    val trimmed = query.trim()

    val visible = if (trimmed.isEmpty()) {
        monthTxns
    } else {
        monthTxns.filter { it.matches(trimmed) }
    }

    val days = visible
        .groupBy { it.date }
        .toSortedMap(compareByDescending<LocalDate> { it })
        .map { (date, items) ->
            DayGroup(
                date = date,
                incomeMinor = items.sumOfType(TransactionType.INCOME),
                expenseMinor = items.sumOfType(TransactionType.EXPENSE),
                items = items.sortedByDescending { it.id }
            )
        }

    return MonthUiState(
        yearMonth = yearMonth,
        summary = MonthSummary(
            incomeMinor = visible.sumOfType(TransactionType.INCOME),
            expenseMinor = visible.sumOfType(TransactionType.EXPENSE)
        ),
        days = days,
        isFiltered = trimmed.isNotEmpty()
    )
}

fun buildDayGroup(transactions: List<Transaction>, date: LocalDate): DayGroup {
    val items = transactions.filter { it.date == date }.sortedByDescending { it.id }
    return DayGroup(
        date = date,
        incomeMinor = items.sumOfType(TransactionType.INCOME),
        expenseMinor = items.sumOfType(TransactionType.EXPENSE),
        items = items
    )
}

private fun List<Transaction>.sumOfType(type: TransactionType): Long =
    filter { it.type == type }.sumOf { it.amountMinor }

private fun List<Transaction>.applyQuery(query: String): List<Transaction> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) this else filter { it.matches(trimmed) }
}

/** Sunday is the first column of the calendar grid. */
private fun LocalDate.weekStart(): LocalDate = minusDays((dayOfWeek.value % 7).toLong())

// ---------- Calendar tab ----------

data class CalendarCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val incomeMinor: Long,
    val expenseMinor: Long
) {
    val netMinor: Long get() = incomeMinor - expenseMinor
    val hasBoth: Boolean get() = incomeMinor != 0L && expenseMinor != 0L
}

fun buildCalendarCells(
    transactions: List<Transaction>,
    yearMonth: YearMonth,
    query: String
): List<CalendarCell> {
    val byDate = transactions.applyQuery(query).groupBy { it.date }
    val gridStart = yearMonth.atDay(1).weekStart()
    val cellCount = ceil(
        (yearMonth.atDay(1).dayOfWeek.value % 7 + yearMonth.lengthOfMonth()) / 7.0
    ).toInt() * 7

    return List(cellCount) { index ->
        val date = gridStart.plusDays(index.toLong())
        val items = byDate[date].orEmpty()
        CalendarCell(
            date = date,
            inMonth = YearMonth.from(date) == yearMonth,
            incomeMinor = items.sumOfType(TransactionType.INCOME),
            expenseMinor = items.sumOfType(TransactionType.EXPENSE)
        )
    }
}

// ---------- Monthly tab ----------

data class WeekSummary(
    val start: LocalDate,
    val end: LocalDate,
    val incomeMinor: Long,
    val expenseMinor: Long
) {
    val totalMinor: Long get() = incomeMinor - expenseMinor
}

data class MonthRow(
    val yearMonth: YearMonth,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val weeks: List<WeekSummary>
) {
    val totalMinor: Long get() = incomeMinor - expenseMinor
    val hasData: Boolean get() = incomeMinor != 0L || expenseMinor != 0L
}

data class YearUiState(
    val year: Int,
    val summary: MonthSummary,
    val months: List<MonthRow>
)

fun buildYearState(
    transactions: List<Transaction>,
    year: Int,
    query: String
): YearUiState {
    val filtered = transactions.applyQuery(query)

    val months = (12 downTo 1).map { monthValue ->
        val yearMonth = YearMonth.of(year, monthValue)
        val monthTxns = filtered.filter { YearMonth.from(it.date) == yearMonth }

        // Weeks overlapping the month, so a week spanning a boundary is counted in full.
        val weeks = (1..yearMonth.lengthOfMonth())
            .map { yearMonth.atDay(it).weekStart() }
            .distinct()
            .sortedDescending()
            .map { start ->
                val end = start.plusDays(6)
                val inWeek = filtered.filter { it.date >= start && it.date <= end }
                WeekSummary(
                    start = start,
                    end = end,
                    incomeMinor = inWeek.sumOfType(TransactionType.INCOME),
                    expenseMinor = inWeek.sumOfType(TransactionType.EXPENSE)
                )
            }

        MonthRow(
            yearMonth = yearMonth,
            incomeMinor = monthTxns.sumOfType(TransactionType.INCOME),
            expenseMinor = monthTxns.sumOfType(TransactionType.EXPENSE),
            weeks = weeks
        )
    }

    return YearUiState(
        year = year,
        summary = MonthSummary(
            incomeMinor = months.sumOf { it.incomeMinor },
            expenseMinor = months.sumOf { it.expenseMinor }
        ),
        months = months
    )
}

// ---------- Total tab ----------

data class AccountTotal(
    val accountName: String,
    val incomeMinor: Long,
    val expenseMinor: Long
)

data class TotalsUiState(
    val yearMonth: YearMonth,
    val summary: MonthSummary,
    /** Percentage change in expenses against the previous month; null when there is no baseline. */
    val changePercent: Int?,
    val accounts: List<AccountTotal>,
    val transferMinor: Long
)

fun buildTotalsState(
    transactions: List<Transaction>,
    yearMonth: YearMonth,
    query: String
): TotalsUiState {
    val filtered = transactions.applyQuery(query)
    val monthTxns = filtered.filter { YearMonth.from(it.date) == yearMonth }
    val expense = monthTxns.sumOfType(TransactionType.EXPENSE)

    val previousExpense = filtered
        .filter { YearMonth.from(it.date) == yearMonth.minusMonths(1) }
        .sumOfType(TransactionType.EXPENSE)

    val accounts = monthTxns
        .filter { it.type != TransactionType.TRANSFER }
        .groupBy { it.account.name }
        .map { (name, items) ->
            AccountTotal(
                accountName = name,
                incomeMinor = items.sumOfType(TransactionType.INCOME),
                expenseMinor = items.sumOfType(TransactionType.EXPENSE)
            )
        }
        .sortedByDescending { it.expenseMinor }

    return TotalsUiState(
        yearMonth = yearMonth,
        summary = MonthSummary(
            incomeMinor = monthTxns.sumOfType(TransactionType.INCOME),
            expenseMinor = expense
        ),
        changePercent = if (previousExpense == 0L) {
            null
        } else {
            (((expense - previousExpense) * 100.0) / previousExpense).roundToInt()
        },
        accounts = accounts,
        transferMinor = monthTxns
            .filter { it.type == TransactionType.TRANSFER }
            .sumOf { it.amountMinor }
    )
}

private fun Transaction.matches(query: String): Boolean {
    if (note.contains(query, ignoreCase = true)) return true
    if (category?.name?.contains(query, ignoreCase = true) == true) return true
    if (account.name.contains(query, ignoreCase = true)) return true
    if (toAccount?.name?.contains(query, ignoreCase = true) == true) return true

    val digits = query.filter { it.isDigit() || it == '.' }
    if (digits.isEmpty()) return false
    val plain = amountMinor.formatAmount().replace(",", "")
    return plain.contains(digits) || plain.removeSuffix(".00").contains(digits)
}
