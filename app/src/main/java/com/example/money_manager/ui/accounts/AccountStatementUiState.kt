package com.example.money_manager.ui.accounts

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

enum class StatementTab(val label: String) {
    DAILY("Daily"),
    MONTHLY("Monthly"),
    ANNUALLY("Annually")
}

data class StatementEntry(
    val transaction: Transaction,
    val deltaMinor: Long,
    val balanceAfterMinor: Long
)

data class StatementDay(
    val date: LocalDate,
    val depositMinor: Long,
    val withdrawalMinor: Long,
    val entries: List<StatementEntry>
)

data class StatementPeriodRow(
    val title: String,
    val rangeLabel: String,
    val year: Int,
    /** Null for annual rows. */
    val monthValue: Int?,
    val depositMinor: Long,
    val withdrawalMinor: Long,
    val closingBalanceMinor: Long,
    val isCurrent: Boolean
) {
    val totalMinor: Long get() = depositMinor - withdrawalMinor
}

data class AccountStatement(
    val rangeLabel: String,
    val depositMinor: Long,
    val withdrawalMinor: Long,
    val closingBalanceMinor: Long,
    val days: List<StatementDay>,
    val periods: List<StatementPeriodRow>
) {
    val totalMinor: Long get() = depositMinor - withdrawalMinor
}

/** Signed effect of a transaction on one account. */
fun Transaction.deltaFor(accountId: Long): Long = when (type) {
    TransactionType.INCOME -> if (account.id == accountId) amountMinor else 0L
    TransactionType.EXPENSE -> if (account.id == accountId) -amountMinor else 0L
    TransactionType.TRANSFER -> {
        var delta = 0L
        if (account.id == accountId) delta -= amountMinor + feeMinor
        if (toAccount?.id == accountId) delta += amountMinor
        delta
    }
}

private fun LocalDate.shortLabel(): String =
    "%02d.%02d.%02d".format(dayOfMonth, monthValue, year % 100)

private fun LocalDate.dayMonthLabel(): String = "%02d.%02d".format(dayOfMonth, monthValue)

/** Every entry touching the account, oldest first, with the running balance after each one. */
private fun runningEntries(account: Account, transactions: List<Transaction>): List<StatementEntry> {
    var balance = account.openingBalanceMinor
    return transactions
        .filter { it.deltaFor(account.id) != 0L }
        .sortedWith(compareBy<Transaction> { it.dateTime }.thenBy { it.id })
        .map { transaction ->
            val delta = transaction.deltaFor(account.id)
            balance += delta
            StatementEntry(transaction, delta, balance)
        }
}

fun buildDailyStatement(
    account: Account,
    transactions: List<Transaction>,
    yearMonth: YearMonth
): AccountStatement {
    val all = runningEntries(account, transactions)
    val range = account.statementRange(yearMonth)
    val inMonth = all.filter { it.transaction.date in range }

    val days = inMonth
        .groupBy { it.transaction.date }
        .toSortedMap(compareByDescending<LocalDate> { it })
        .map { (date, entries) ->
            StatementDay(
                date = date,
                depositMinor = entries.filter { it.deltaMinor > 0L }.sumOf { it.deltaMinor },
                withdrawalMinor = entries.filter { it.deltaMinor < 0L }.sumOf { -it.deltaMinor },
                entries = entries.sortedByDescending { it.transaction.id }
            )
        }

    return AccountStatement(
        rangeLabel = "${range.start.shortLabel()} ~ ${range.endInclusive.shortLabel()}",
        depositMinor = days.sumOf { it.depositMinor },
        withdrawalMinor = days.sumOf { it.withdrawalMinor },
        closingBalanceMinor = balanceAtEndOf(all, account, range.endInclusive),
        days = days,
        periods = emptyList()
    )
}

fun buildMonthlyStatement(
    account: Account,
    transactions: List<Transaction>,
    year: Int
): AccountStatement {
    val all = runningEntries(account, transactions)
    val currentMonth = YearMonth.now()

    val periods = (12 downTo 1).map { monthValue ->
        val month = YearMonth.of(year, monthValue)
        val range = account.statementRange(month)
        val inMonth = all.filter { it.transaction.date in range }
        StatementPeriodRow(
            title = month.month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.ENGLISH
            ),
            rangeLabel = "${range.start.dayMonthLabel()} ~ ${range.endInclusive.dayMonthLabel()}",
            year = year,
            monthValue = monthValue,
            depositMinor = inMonth.filter { it.deltaMinor > 0L }.sumOf { it.deltaMinor },
            withdrawalMinor = inMonth.filter { it.deltaMinor < 0L }.sumOf { -it.deltaMinor },
            closingBalanceMinor = balanceAtEndOf(all, account, range.endInclusive),
            isCurrent = month == currentMonth
        )
    }

    val yearStart = account.statementRange(YearMonth.of(year, 1)).start
    val yearEnd = account.statementRange(YearMonth.of(year, 12)).endInclusive
    return AccountStatement(
        rangeLabel = "${yearStart.shortLabel()} ~ ${yearEnd.shortLabel()}",
        depositMinor = periods.sumOf { it.depositMinor },
        withdrawalMinor = periods.sumOf { it.withdrawalMinor },
        closingBalanceMinor = balanceAtEndOf(all, account, yearEnd),
        days = emptyList(),
        periods = periods
    )
}

fun buildAnnualStatement(
    account: Account,
    transactions: List<Transaction>
): AccountStatement {
    val all = runningEntries(account, transactions)
    val thisYear = LocalDate.now().year
    val earliest = all.minOfOrNull { it.transaction.date.year } ?: thisYear
    val years = (minOf(earliest, thisYear - 2)..thisYear).sortedDescending()

    val periods = years.map { year ->
        val inYear = all.filter { it.transaction.date.year == year }
        StatementPeriodRow(
            title = year.toString(),
            rangeLabel = "01.01.${year % 100} ~ 31.12.${year % 100}",
            year = year,
            monthValue = null,
            depositMinor = inYear.filter { it.deltaMinor > 0L }.sumOf { it.deltaMinor },
            withdrawalMinor = inYear.filter { it.deltaMinor < 0L }.sumOf { -it.deltaMinor },
            closingBalanceMinor = balanceAtEndOf(all, account, YearMonth.of(year, 12).atEndOfMonth()),
            isCurrent = year == thisYear
        )
    }

    val first = years.minOrNull() ?: thisYear
    val last = years.maxOrNull() ?: thisYear
    return AccountStatement(
        rangeLabel = "01.01.${first % 100} ~ 31.12.${last % 100}",
        depositMinor = periods.sumOf { it.depositMinor },
        withdrawalMinor = periods.sumOf { it.withdrawalMinor },
        closingBalanceMinor = periods.firstOrNull()?.closingBalanceMinor
            ?: account.openingBalanceMinor,
        days = emptyList(),
        periods = periods
    )
}

fun annualTitle(account: Account, transactions: List<Transaction>): String {
    val thisYear = LocalDate.now().year
    val earliest = transactions
        .filter { it.deltaFor(account.id) != 0L }
        .minOfOrNull { it.date.year } ?: thisYear
    return "${minOf(earliest, thisYear - 2)} ~ $thisYear"
}

private fun balanceAtEndOf(
    entries: List<StatementEntry>,
    account: Account,
    date: LocalDate
): Long = entries
    .lastOrNull { !it.transaction.date.isAfter(date) }
    ?.balanceAfterMinor
    ?: account.openingBalanceMinor
