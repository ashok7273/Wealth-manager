package com.example.money_manager.data.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class TransactionType { INCOME, EXPENSE, TRANSFER }

data class Category(
    val id: Long,
    val name: String,
    val emoji: String,
    val type: TransactionType
)

data class AccountGroup(
    val id: Long,
    val name: String,
    /** Card groups get settlement and payment dates. */
    val isCard: Boolean = false,
    val sortOrder: Int = 0
)

data class Account(
    val id: Long,
    val name: String,
    val group: AccountGroup,
    val openingBalanceMinor: Long = 0L,
    val description: String = "",
    val hidden: Boolean = false,
    /** Card only. Day of month the statement closes, 1..28. */
    val settlementDay: Int? = null,
    /** Card only. Day of month the bill is paid, 1..28. */
    val paymentDay: Int? = null
)

data class Transaction(
    val id: Long,
    /** Amount in minor units (paise). Always positive; direction comes from [type]. */
    val amountMinor: Long,
    val type: TransactionType,
    /** Null for transfers. */
    val category: Category?,
    val account: Account,
    /** Destination account; only set for transfers. */
    val toAccount: Account? = null,
    /** Optional transfer fee in minor units. */
    val feeMinor: Long = 0,
    val dateTime: LocalDateTime,
    val note: String = "",
    val description: String = "",
    /** File name of an attached photo inside the app's attachments folder. */
    val photoPath: String? = null
) {
    val date: LocalDate get() = dateTime.toLocalDate()
}
