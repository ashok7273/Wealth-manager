package com.example.money_manager.data

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.Transaction
import java.time.LocalDateTime
import java.time.YearMonth

/** Demo entries inserted once, when the database is first created. */
object SampleData {

    private val cash = DefaultCatalog.seedAccounts[0]
    private val bank = DefaultCatalog.seedAccounts[1]
    private val card = DefaultCatalog.seedAccounts[2]

    private val food = DefaultCatalog.expenseCategories.first { it.name == "Food" }
    private val beauty = DefaultCatalog.expenseCategories.first { it.name == "Beauty" }
    private val transport = DefaultCatalog.expenseCategories.first { it.name == "Transport" }
    private val apparel = DefaultCatalog.expenseCategories.first { it.name == "Apparel" }
    private val household = DefaultCatalog.expenseCategories.first { it.name == "Household" }
    private val health = DefaultCatalog.expenseCategories.first { it.name == "Health" }
    private val social = DefaultCatalog.expenseCategories.first { it.name == "Social Life" }
    private val allowance = DefaultCatalog.incomeCategories.first { it.name == "Allowance" }
    private val salary = DefaultCatalog.incomeCategories.first { it.name == "Salary" }

    fun seed(): List<Transaction> {
        val thisMonth = YearMonth.now()
        val all = mutableListOf<Transaction>()
        var id = 1L

        fun add(
            month: YearMonth,
            day: Int,
            amount: Long,
            category: Category,
            account: Account,
            note: String
        ) {
            val safeDay = day.coerceIn(1, month.lengthOfMonth())
            all += Transaction(
                id = id++,
                amountMinor = amount,
                type = category.type,
                category = category,
                account = account,
                dateTime = LocalDateTime.of(month.year, month.month, safeDay, 12, 0),
                note = note
            )
        }

        listOf(thisMonth, thisMonth.minusMonths(1), thisMonth.minusMonths(2)).forEach { m ->
            add(m, 1, 3_500_000, salary, bank, "monthly salary")
            add(m, 2, 1_800_000, household, bank, "house rent")
            add(m, 3, 129_900, household, bank, "electricity bill")
            add(m, 5, 49_900, social, bank, "mobile recharge")
            add(m, 8, 66_500, food, cash, "grocery")
            add(m, 8, 24_000, transport, cash, "cab to office")
            add(m, 11, 51_200, beauty, card, "herbal powders")
            add(m, 14, 189_000, apparel, card, "running shoes")
            add(m, 17, 8_600, food, cash, "grocery")
            add(m, 19, 34_500, health, cash, "medicines")
            add(m, 21, 23_100, allowance, bank, "pluxee meal")
            add(m, 24, 91_800, food, cash, "grocery")
            add(m, 26, 9_500, transport, cash, "metro card top up")
            add(m, 28, 53_200, food, cash, "grocery")
        }

        return all
    }
}
