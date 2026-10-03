package com.example.money_manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.money_manager.data.DefaultCatalog
import com.example.money_manager.data.SampleData
import com.example.money_manager.data.model.Transaction

@Database(
    entities = [
        AccountGroupEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurrenceRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountGroupDao(): AccountGroupDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao

    companion object {
        /** Seeded once, when the database file is first created. */
        const val SEED_SAMPLE_TRANSACTIONS = false

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "money_manager.db")
                // Only reached by installing an older build over a newer one.
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        seed(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // A destructive downgrade recreates the tables without calling onCreate.
                        if (isUnseeded(db)) seed(db)
                    }
                })
                .build()

        private fun isUnseeded(db: SupportSQLiteDatabase): Boolean {
            val query = "SELECT (SELECT COUNT(*) FROM account_groups) + " +
                "(SELECT COUNT(*) FROM categories)"
            return db.query(query).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) == 0
            }
        }

        private fun seed(db: SupportSQLiteDatabase) {
            DefaultCatalog.seedGroups.forEach { group ->
                db.execSQL(
                    "INSERT INTO account_groups (id, name, isCard, sortOrder) VALUES (?, ?, ?, ?)",
                    arrayOf(group.id, group.name, if (group.isCard) 1 else 0, group.sortOrder)
                )
            }

            DefaultCatalog.seedAccounts.forEachIndexed { index, account ->
                db.execSQL(
                    "INSERT INTO accounts (id, name, groupId, openingBalanceMinor, " +
                        "description, hidden, settlementDay, paymentDay, sortOrder) " +
                        "VALUES (?, ?, ?, ?, ?, 0, NULL, NULL, ?)",
                    arrayOf(
                        account.id,
                        account.name,
                        account.group.id,
                        account.openingBalanceMinor,
                        account.description,
                        index
                    )
                )
            }

            (DefaultCatalog.expenseCategories + DefaultCatalog.incomeCategories)
                .forEachIndexed { index, category ->
                    db.execSQL(
                        "INSERT INTO categories (id, name, emoji, type, sortOrder) " +
                            "VALUES (?, ?, ?, ?, ?)",
                        arrayOf(category.id, category.name, category.emoji, category.type.name, index)
                    )
                }

            if (SEED_SAMPLE_TRANSACTIONS) {
                SampleData.seed().forEach { transaction -> insertSample(db, transaction) }
            }
        }

        private fun insertSample(db: SupportSQLiteDatabase, transaction: Transaction) {
            db.execSQL(
                "INSERT INTO transactions (amountMinor, type, categoryId, accountId, " +
                    "toAccountId, feeMinor, dateTime, note, description) " +
                    "VALUES (?, ?, ?, ?, NULL, 0, ?, ?, '')",
                arrayOf(
                    transaction.amountMinor,
                    transaction.type.name,
                    transaction.category?.id,
                    transaction.account.id,
                    transaction.dateTime.toString(),
                    transaction.note
                )
            )
        }
    }
}
