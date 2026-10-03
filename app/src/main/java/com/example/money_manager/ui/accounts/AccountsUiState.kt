package com.example.money_manager.ui.accounts

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.AccountGroup
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import java.time.YearMonth

data class AccountBalance(val account: Account, val balanceMinor: Long)

data class AccountGroupSection(
    val group: AccountGroup,
    val totalMinor: Long,
    val accounts: List<AccountBalance>
)

data class AccountsUiState(
    val sections: List<AccountGroupSection>,
    val assetsMinor: Long,
    val liabilitiesMinor: Long
) {
    val totalMinor: Long get() = assetsMinor - liabilitiesMinor
    val isEmpty: Boolean get() = sections.isEmpty()
}

/**
 * Opening balance plus every movement that touched the account.
 * A transfer leaves the source account with its fee and lands in full on the destination.
 */
fun accountBalance(
    account: Account,
    transactions: List<Transaction>,
    until: YearMonth? = null
): Long {
    var balance = account.openingBalanceMinor
    transactions.forEach { transaction ->
        if (until != null && YearMonth.from(transaction.date) > until) return@forEach
        when (transaction.type) {
            TransactionType.INCOME ->
                if (transaction.account.id == account.id) balance += transaction.amountMinor

            TransactionType.EXPENSE ->
                if (transaction.account.id == account.id) balance -= transaction.amountMinor

            TransactionType.TRANSFER -> {
                if (transaction.account.id == account.id) {
                    balance -= transaction.amountMinor + transaction.feeMinor
                }
                if (transaction.toAccount?.id == account.id) balance += transaction.amountMinor
            }
        }
    }
    return balance
}

fun buildAccountsState(
    accounts: List<Account>,
    transactions: List<Transaction>,
    includeHidden: Boolean = false
): AccountsUiState {
    val visible = if (includeHidden) accounts else accounts.filterNot { it.hidden }
    val balances = visible.map { AccountBalance(it, accountBalance(it, transactions)) }

    val sections = balances
        .groupBy { it.account.group }
        .toList()
        .sortedBy { (group, _) -> group.sortOrder }
        .map { (group, inGroup) ->
            AccountGroupSection(
                group = group,
                totalMinor = inGroup.sumOf { it.balanceMinor },
                accounts = inGroup
            )
        }

    return AccountsUiState(
        sections = sections,
        assetsMinor = balances.filter { it.balanceMinor > 0L }.sumOf { it.balanceMinor },
        liabilitiesMinor = balances.filter { it.balanceMinor < 0L }.sumOf { -it.balanceMinor }
    )
}

data class AccountStatsPoint(
    val month: YearMonth,
    val balanceMinor: Long,
    val incomeMinor: Long,
    val expenseMinor: Long
)

/** Running balance and per-month income/expense for the [months] ending at [endMonth]. */
fun buildAccountStats(
    accounts: List<Account>,
    transactions: List<Transaction>,
    endMonth: YearMonth,
    months: Int = 6
): List<AccountStatsPoint> {
    val visible = accounts.filterNot { it.hidden }
    return (months - 1 downTo 0).map { back ->
        val month = endMonth.minusMonths(back.toLong())
        val inMonth = transactions.filter { YearMonth.from(it.date) == month }
        AccountStatsPoint(
            month = month,
            balanceMinor = visible.sumOf { accountBalance(it, transactions, until = month) },
            incomeMinor = inMonth
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor },
            expenseMinor = inMonth
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountMinor }
        )
    }
}
