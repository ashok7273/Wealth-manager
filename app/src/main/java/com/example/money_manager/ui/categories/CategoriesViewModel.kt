package com.example.money_manager.ui.categories

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.CategoryRepository
import com.example.money_manager.data.NEW_CATEGORY_ID
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.TransactionRepository
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import kotlinx.coroutines.flow.StateFlow

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository = ServiceLocator.categoryRepository,
    private val transactionRepository: TransactionRepository = ServiceLocator.transactionRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.categories
    val transactions: StateFlow<List<Transaction>> = transactionRepository.transactions

    fun add(name: String, type: TransactionType) {
        categoryRepository.add(
            Category(
                id = NEW_CATEGORY_ID,
                name = name.trim(),
                emoji = "",
                type = type
            )
        )
    }

    fun rename(category: Category, name: String) {
        categoryRepository.update(category.copy(name = name.trim()))
    }

    fun reorder(type: TransactionType, ordered: List<Category>) {
        categoryRepository.reorder(type, ordered)
    }

    /** Removes the category and every transaction filed under it. */
    fun delete(category: Category) {
        transactionRepository.deleteByCategory(category.id)
        categoryRepository.delete(category.id)
    }

    fun transactionCountFor(categoryId: Long): Int =
        transactions.value.count { it.category?.id == categoryId }
}
