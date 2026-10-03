package com.example.money_manager.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "account_groups")
data class AccountGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCard: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long,
    val openingBalanceMinor: Long,
    val description: String,
    val hidden: Boolean,
    val settlementDay: Int?,
    val paymentDay: Int?,
    val sortOrder: Int
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val type: String,
    val sortOrder: Int
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: String,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val feeMinor: Long,
    /** ISO-8601 local date-time. */
    val dateTime: String,
    val note: String,
    val description: String,
    val photoPath: String? = null
)

@Entity(tableName = "recurrence_rules")
data class RecurrenceRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val frequency: String,
    val period: String,
    val count: Int,
    val generatedCount: Int,
    val startDateTime: String,
    val amountMinor: Long,
    val type: String,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val feeMinor: Long,
    val note: String,
    val description: String
)

@Dao
interface AccountGroupDao {
    @Query("SELECT * FROM account_groups ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountGroupEntity>>

    @Query("SELECT * FROM account_groups ORDER BY sortOrder, id")
    suspend fun getAll(): List<AccountGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: AccountGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<AccountGroupEntity>)

    @Update
    suspend fun update(group: AccountGroupEntity)

    @Query("DELETE FROM account_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM account_groups")
    suspend fun clear()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM account_groups")
    suspend fun maxSortOrder(): Int
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM accounts WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)

    @Query("DELETE FROM accounts")
    suspend fun clear()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM accounts")
    suspend fun maxSortOrder(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Update
    suspend fun updateAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun clear()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxSortOrder(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateTime, id")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Dao
interface RecurrenceRuleDao {
    @Query("SELECT * FROM recurrence_rules ORDER BY id")
    fun observeAll(): Flow<List<RecurrenceRuleEntity>>

    @Query("SELECT * FROM recurrence_rules ORDER BY id")
    suspend fun getAll(): List<RecurrenceRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurrenceRuleEntity): Long

    @Update
    suspend fun updateAll(rules: List<RecurrenceRuleEntity>)

    @Query("DELETE FROM recurrence_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurrence_rules")
    suspend fun clear()
}
