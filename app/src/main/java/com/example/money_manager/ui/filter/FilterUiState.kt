package com.example.money_manager.ui.filter

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionFilter
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.data.ofType
import java.time.YearMonth

enum class FilterTab(val label: String) {
    INCOME("Income"),
    EXPENSES("Expenses"),
    ACCOUNT("Account")
}

data class FilterCategoryRow(val category: Category, val amountMinor: Long)

data class FilterAccountRow(
    val account: Account,
    val incomeMinor: Long,
    val expenseMinor: Long
)

data class FilterUiState(
    val incomeRows: List<FilterCategoryRow>,
    val expenseRows: List<FilterCategoryRow>,
    val accountRows: List<FilterAccountRow>,
    val monthIncomeMinor: Long,
    val monthExpenseMinor: Long,
    val selectedIncomeMinor: Long,
    val selectedExpenseMinor: Long,
    val hasSelection: Boolean
) {
    val incomeFraction: Float
        get() = if (monthIncomeMinor == 0L) 0f else selectedIncomeMinor.toFloat() / monthIncomeMinor

    val expenseFraction: Float
        get() = if (monthExpenseMinor == 0L) 0f else selectedExpenseMinor.toFloat() / monthExpenseMinor

    /** With nothing ticked the amounts show the whole month for context, while the rings read 0%. */
    val displayIncomeMinor: Long
        get() = if (hasSelection) selectedIncomeMinor else monthIncomeMinor

    val displayExpenseMinor: Long
        get() = if (hasSelection) selectedExpenseMinor else monthExpenseMinor

    val displayTotalMinor: Long get() = displayIncomeMinor - displayExpenseMinor
}

fun buildFilterState(
    transactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    yearMonth: YearMonth,
    draft: TransactionFilter
): FilterUiState {
    val monthTxns = transactions.filter { YearMonth.from(it.date) == yearMonth }

    fun rowsFor(categories: List<Category>) = categories.map { category ->
        FilterCategoryRow(
            category = category,
            amountMinor = monthTxns
                .filter { it.category?.id == category.id }
                .sumOf { it.amountMinor }
        )
    }

    val accountRows = accounts.filterNot { it.hidden }.map { account ->
        val ofAccount = monthTxns.filter { it.account.id == account.id }
        FilterAccountRow(
            account = account,
            incomeMinor = ofAccount
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor },
            expenseMinor = ofAccount
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountMinor }
        )
    }

    val selected = if (draft.isEmpty) emptyList() else monthTxns.filter { draft.matches(it) }

    return FilterUiState(
        incomeRows = rowsFor(categories.ofType(TransactionType.INCOME)),
        expenseRows = rowsFor(categories.ofType(TransactionType.EXPENSE)),
        accountRows = accountRows,
        monthIncomeMinor = monthTxns
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountMinor },
        monthExpenseMinor = monthTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor },
        selectedIncomeMinor = selected
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountMinor },
        selectedExpenseMinor = selected
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor },
        hasSelection = !draft.isEmpty
    )
}
