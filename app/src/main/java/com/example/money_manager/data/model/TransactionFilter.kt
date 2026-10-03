package com.example.money_manager.data.model

/**
 * Empty sets mean "no constraint". Within a dimension the ids are OR-ed;
 * the dimensions are AND-ed together.
 */
data class TransactionFilter(
    val incomeCategoryIds: Set<Long> = emptySet(),
    val expenseCategoryIds: Set<Long> = emptySet(),
    val accountIds: Set<Long> = emptySet()
) {
    val categoryIds: Set<Long> get() = incomeCategoryIds + expenseCategoryIds

    val isEmpty: Boolean
        get() = incomeCategoryIds.isEmpty() && expenseCategoryIds.isEmpty() && accountIds.isEmpty()

    val selectionCount: Int
        get() = incomeCategoryIds.size + expenseCategoryIds.size + accountIds.size

    fun matches(transaction: Transaction): Boolean {
        val categories = categoryIds
        if (categories.isNotEmpty()) {
            val id = transaction.category?.id ?: return false
            if (id !in categories) return false
        }
        if (accountIds.isNotEmpty()) {
            val fromMatches = transaction.account.id in accountIds
            val toMatches = transaction.toAccount?.id in accountIds
            if (!fromMatches && !toMatches) return false
        }
        return true
    }
}
