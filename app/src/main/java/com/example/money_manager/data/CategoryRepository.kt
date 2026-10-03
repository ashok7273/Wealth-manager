package com.example.money_manager.data

import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.TransactionType
import kotlinx.coroutines.flow.StateFlow

const val NEW_CATEGORY_ID = 0L

interface CategoryRepository {
    val categories: StateFlow<List<Category>>
    fun add(category: Category)
    fun update(category: Category)
    fun delete(id: Long)

    /** Replaces the ordering of one type; the stored list order is the display order. */
    fun reorder(type: TransactionType, ordered: List<Category>)
}

fun List<Category>.ofType(type: TransactionType): List<Category> = filter { it.type == type }
