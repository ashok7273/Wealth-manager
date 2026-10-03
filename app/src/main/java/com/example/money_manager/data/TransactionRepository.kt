package com.example.money_manager.data

import android.content.Context
import com.example.money_manager.data.backup.BackupManager
import com.example.money_manager.data.local.AppDatabase
import com.example.money_manager.data.lock.AppLock
import com.example.money_manager.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    val transactions: StateFlow<List<Transaction>>

    fun upsert(transaction: Transaction)

    fun upsertAll(transactions: List<Transaction>)

    fun delete(id: Long)

    fun deleteAll(ids: Set<Long>)

    fun deleteByAccount(accountId: Long)

    fun deleteByCategory(categoryId: Long)

    fun findById(id: Long): Transaction?
}

const val NEW_TRANSACTION_ID = 0L

object ServiceLocator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var accountRepository: AccountRepository
        private set
    lateinit var accountGroupRepository: AccountGroupRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var transactionRepository: TransactionRepository
        private set
    lateinit var recurrenceRuleRepository: RecurrenceRuleRepository
        private set
    lateinit var recurrenceEngine: RecurrenceEngine
        private set
    lateinit var appLock: AppLock
        private set
    lateinit var backupManager: BackupManager
        private set

    fun init(context: Context) {
        if (::transactionRepository.isInitialized) return
        val database = AppDatabase.build(context.applicationContext)

        accountGroupRepository = RoomAccountGroupRepository(
            database.accountGroupDao(),
            database.accountDao(),
            scope
        )
        accountRepository = RoomAccountRepository(
            database.accountDao(),
            accountGroupRepository.groups,
            scope
        )
        categoryRepository = RoomCategoryRepository(database.categoryDao(), scope)
        transactionRepository = RoomTransactionRepository(
            dao = database.transactionDao(),
            accountsFlow = accountRepository.accounts,
            categoriesFlow = categoryRepository.categories,
            scope = scope
        )
        recurrenceRuleRepository = RoomRecurrenceRuleRepository(
            dao = database.recurrenceRuleDao(),
            accountsFlow = accountRepository.accounts,
            categoriesFlow = categoryRepository.categories,
            scope = scope
        )
        recurrenceEngine = RecurrenceEngine(transactionRepository, recurrenceRuleRepository)
        appLock = AppLock(context)
        backupManager = BackupManager(context.applicationContext, database)
    }
}
