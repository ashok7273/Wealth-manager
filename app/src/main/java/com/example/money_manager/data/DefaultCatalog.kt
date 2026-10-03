package com.example.money_manager.data

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.AccountGroup
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.TransactionType

/** Built-in groups, accounts and categories, inserted when the database is first created. */
object DefaultCatalog {

    val seedGroups = listOf(
        AccountGroup(1, "Cash"),
        AccountGroup(2, "Accounts"),
        AccountGroup(3, "Card", isCard = true),
        AccountGroup(4, "Debit Card"),
        AccountGroup(5, "Savings"),
        AccountGroup(6, "Top-Up/Prepaid"),
        AccountGroup(7, "Investments"),
        AccountGroup(8, "Overdrafts"),
        AccountGroup(9, "Loan", isCard = true),
        AccountGroup(10, "Insurance"),
        AccountGroup(11, "Others")
    ).mapIndexed { index, group -> group.copy(sortOrder = index) }

    val seedAccounts = listOf(
        Account(1, "Cash", seedGroups[0], 50_000_00L),
        Account(2, "Account", seedGroups[1], 250_000_00L),
        Account(3, "Card", seedGroups[2], 0L)
    )

    /** Pre-selected on a new entry. */
    val defaultAccount = seedAccounts[1]

    val expenseCategories = listOf(
        Category(1, "Food", "\uD83C\uDF5C", TransactionType.EXPENSE),
        Category(2, "Social Life", "\uD83D\uDC6B", TransactionType.EXPENSE),
        Category(3, "Pets", "\uD83D\uDC36", TransactionType.EXPENSE),
        Category(4, "Transport", "\uD83D\uDE95", TransactionType.EXPENSE),
        Category(5, "Culture", "\uD83D\uDDBC\uFE0F", TransactionType.EXPENSE),
        Category(6, "Household", "\uD83E\uDE91", TransactionType.EXPENSE),
        Category(7, "Apparel", "\uD83D\uDC54", TransactionType.EXPENSE),
        Category(8, "Beauty", "\uD83D\uDC84", TransactionType.EXPENSE),
        Category(9, "Health", "\uD83E\uDDD8", TransactionType.EXPENSE),
        Category(10, "Education", "\uD83D\uDCD9", TransactionType.EXPENSE),
        Category(11, "Gift", "\uD83C\uDF81", TransactionType.EXPENSE),
        Category(12, "Other", "\u2795", TransactionType.EXPENSE)
    )

    val incomeCategories = listOf(
        Category(101, "Allowance", "\uD83E\uDD11", TransactionType.INCOME),
        Category(102, "Salary", "\uD83D\uDCB0", TransactionType.INCOME),
        Category(103, "Petty cash", "\uD83D\uDCB5", TransactionType.INCOME),
        Category(104, "Bonus", "\uD83C\uDF96\uFE0F", TransactionType.INCOME),
        Category(105, "Other", "\u2795", TransactionType.INCOME)
    )
}
