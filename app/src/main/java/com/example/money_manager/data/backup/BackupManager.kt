package com.example.money_manager.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.money_manager.data.local.AccountEntity
import com.example.money_manager.data.local.AccountGroupEntity
import com.example.money_manager.data.local.AppDatabase
import com.example.money_manager.data.local.CategoryEntity
import com.example.money_manager.data.local.RecurrenceRuleEntity
import com.example.money_manager.data.local.TransactionEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val MIME_CSV = "text/csv"
private val FolderStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

private const val FILE_TRANSACTIONS = "transactions.csv"
private const val FILE_ACCOUNTS = "accounts.csv"
private const val FILE_GROUPS = "account_groups.csv"
private const val FILE_CATEGORIES = "categories.csv"
private const val FILE_RECURRING = "recurring.csv"

sealed interface BackupResult {
    data class Success(val message: String) : BackupResult
    data class Failure(val message: String) : BackupResult
}

/**
 * Reads and writes plain CSV inside a folder the user picked through the system
 * document picker. No storage permission is involved and nothing leaves the device.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun export(treeUri: Uri): BackupResult {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: return BackupResult.Failure("Could not open that folder")

        val folderName = "MoneyManager_" + FolderStamp.format(LocalDateTime.now())
        val folder = tree.createDirectory(folderName)
            ?: return BackupResult.Failure("Could not create a folder there")

        val groups = database.accountGroupDao().getAll()
        val accounts = database.accountDao().getAll()
        val categories = database.categoryDao().getAll()
        val transactions = database.transactionDao().getAll()
        val rules = database.recurrenceRuleDao().getAll()

        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        val groupsById = groups.associateBy { it.id }

        write(folder, FILE_GROUPS, buildString {
            appendLine(Csv.row(GroupColumns))
            groups.forEach { group ->
                appendLine(Csv.row(listOf(group.name, group.isCard.toString())))
            }
        })

        write(folder, FILE_ACCOUNTS, buildString {
            appendLine(Csv.row(AccountColumns))
            accounts.forEach { account ->
                appendLine(
                    Csv.row(
                        listOf(
                            account.name,
                            groupsById[account.groupId]?.name.orEmpty(),
                            account.openingBalanceMinor.asDecimal(),
                            account.description,
                            account.hidden.toString(),
                            account.settlementDay?.toString().orEmpty(),
                            account.paymentDay?.toString().orEmpty()
                        )
                    )
                )
            }
        })

        write(folder, FILE_CATEGORIES, buildString {
            appendLine(Csv.row(CategoryColumns))
            categories.forEach { category ->
                appendLine(Csv.row(listOf(category.name, category.type, category.emoji)))
            }
        })

        write(folder, FILE_TRANSACTIONS, buildString {
            appendLine(Csv.row(TransactionColumns))
            transactions.forEach { transaction ->
                val dateTime = LocalDateTime.parse(transaction.dateTime)
                appendLine(
                    Csv.row(
                        listOf(
                            dateTime.toLocalDate().toString(),
                            dateTime.toLocalTime().toString(),
                            transaction.type,
                            categoriesById[transaction.categoryId]?.name.orEmpty(),
                            accountsById[transaction.accountId]?.name.orEmpty(),
                            accountsById[transaction.toAccountId]?.name.orEmpty(),
                            transaction.amountMinor.asDecimal(),
                            transaction.feeMinor.asDecimal(),
                            transaction.note,
                            transaction.description
                        )
                    )
                )
            }
        })

        write(folder, FILE_RECURRING, buildString {
            appendLine(Csv.row(RecurringColumns))
            rules.forEach { rule ->
                appendLine(
                    Csv.row(
                        listOf(
                            rule.mode,
                            rule.frequency,
                            rule.period,
                            rule.count.toString(),
                            rule.generatedCount.toString(),
                            rule.startDateTime,
                            rule.amountMinor.asDecimal(),
                            rule.type,
                            categoriesById[rule.categoryId]?.name.orEmpty(),
                            accountsById[rule.accountId]?.name.orEmpty(),
                            accountsById[rule.toAccountId]?.name.orEmpty(),
                            rule.feeMinor.asDecimal(),
                            rule.note,
                            rule.description
                        )
                    )
                )
            }
        })

        return BackupResult.Success(
            "Exported ${transactions.size} transactions to $folderName"
        )
    }

    suspend fun import(treeUri: Uri): BackupResult {
        val picked = DocumentFile.fromTreeUri(context, treeUri)
            ?: return BackupResult.Failure("Could not open that folder")

        val folder = resolveBackupFolder(picked)
            ?: return BackupResult.Failure(
                "No $FILE_TRANSACTIONS found there. Pick the folder holding the CSV files."
            )

        val transactionsCsv = folder.findFile(FILE_TRANSACTIONS)?.let { read(it.uri) }
            ?: return BackupResult.Failure("No $FILE_TRANSACTIONS in that folder")
        val accountsCsv = folder.findFile(FILE_ACCOUNTS)?.let { read(it.uri) }
        val groupsCsv = folder.findFile(FILE_GROUPS)?.let { read(it.uri) }
        val categoriesCsv = folder.findFile(FILE_CATEGORIES)?.let { read(it.uri) }
        val recurringCsv = folder.findFile(FILE_RECURRING)?.let { read(it.uri) }

        val accountRows = accountsCsv?.let { Csv.parse(it).drop(1) }.orEmpty()
        val groupRows = groupsCsv?.let { Csv.parse(it).drop(1) }.orEmpty()
        val categoryRows = categoriesCsv?.let { Csv.parse(it).drop(1) }.orEmpty()
        val transactionRows = Csv.parse(transactionsCsv).drop(1)
        val recurringRows = recurringCsv?.let { Csv.parse(it).drop(1) }.orEmpty()

        val groups = linkedMapOf<String, AccountGroupEntity>()
        groupRows.forEachIndexed { index, row ->
            val name = row.getOrNull(0).orEmpty().ifBlank { return@forEachIndexed }
            groups[name] = AccountGroupEntity(
                id = (index + 1).toLong(),
                name = name,
                isCard = row.getOrNull(1)?.toBooleanStrictOrNull() ?: false,
                sortOrder = index
            )
        }

        fun groupFor(name: String): AccountGroupEntity {
            val key = name.ifBlank { "Others" }
            return groups.getOrPut(key) {
                AccountGroupEntity(
                    id = (groups.size + 1).toLong(),
                    name = key,
                    isCard = false,
                    sortOrder = groups.size
                )
            }
        }

        val accounts = linkedMapOf<String, AccountEntity>()
        accountRows.forEachIndexed { index, row ->
            val name = row.getOrNull(0).orEmpty().ifBlank { return@forEachIndexed }
            accounts[name] = AccountEntity(
                id = (index + 1).toLong(),
                name = name,
                groupId = groupFor(row.getOrNull(1).orEmpty()).id,
                openingBalanceMinor = row.getOrNull(2).toMinor(),
                description = row.getOrNull(3).orEmpty(),
                hidden = row.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                settlementDay = row.getOrNull(5)?.toIntOrNull(),
                paymentDay = row.getOrNull(6)?.toIntOrNull(),
                sortOrder = index
            )
        }

        val categories = linkedMapOf<String, CategoryEntity>()
        categoryRows.forEachIndexed { index, row ->
            val name = row.getOrNull(0).orEmpty().ifBlank { return@forEachIndexed }
            categories[name] = CategoryEntity(
                id = (index + 1).toLong(),
                name = name,
                emoji = row.getOrNull(2).orEmpty(),
                type = row.getOrNull(1).orEmpty().ifBlank { "EXPENSE" },
                sortOrder = index
            )
        }

        // Anything referenced by a transaction but missing from the other files.
        fun accountFor(name: String): AccountEntity? {
            if (name.isBlank()) return null
            return accounts.getOrPut(name) {
                AccountEntity(
                    id = (accounts.size + 1).toLong(),
                    name = name,
                    groupId = groupFor("Others").id,
                    openingBalanceMinor = 0L,
                    description = "",
                    hidden = false,
                    settlementDay = null,
                    paymentDay = null,
                    sortOrder = accounts.size
                )
            }
        }

        fun categoryFor(name: String, type: String): CategoryEntity? {
            if (name.isBlank()) return null
            return categories.getOrPut(name) {
                CategoryEntity(
                    id = (categories.size + 1).toLong(),
                    name = name,
                    emoji = "",
                    type = type,
                    sortOrder = categories.size
                )
            }
        }

        val transactions = mutableListOf<TransactionEntity>()
        transactionRows.forEach { row ->
            val date = row.getOrNull(0).orEmpty()
            if (date.isBlank()) return@forEach
            val time = row.getOrNull(1).orEmpty().ifBlank { "00:00" }
            val type = row.getOrNull(2).orEmpty().ifBlank { "EXPENSE" }
            val account = accountFor(row.getOrNull(4).orEmpty()) ?: return@forEach
            transactions += TransactionEntity(
                id = 0,
                amountMinor = row.getOrNull(6).toMinor(),
                type = type,
                categoryId = categoryFor(row.getOrNull(3).orEmpty(), type)?.id,
                accountId = account.id,
                toAccountId = accountFor(row.getOrNull(5).orEmpty())?.id,
                feeMinor = row.getOrNull(7).toMinor(),
                dateTime = runCatching { LocalDateTime.parse("${date}T$time") }
                    .getOrElse { return@forEach }
                    .toString(),
                note = row.getOrNull(8).orEmpty(),
                description = row.getOrNull(9).orEmpty()
            )
        }

        val rules = mutableListOf<RecurrenceRuleEntity>()
        recurringRows.forEach { row ->
            val account = accountFor(row.getOrNull(9).orEmpty()) ?: return@forEach
            val type = row.getOrNull(7).orEmpty().ifBlank { "EXPENSE" }
            rules += RecurrenceRuleEntity(
                id = 0,
                mode = row.getOrNull(0).orEmpty().ifBlank { "REPEAT" },
                frequency = row.getOrNull(1).orEmpty().ifBlank { "MONTHLY" },
                period = row.getOrNull(2).orEmpty().ifBlank { "MONTHLY" },
                count = row.getOrNull(3)?.toIntOrNull() ?: 12,
                generatedCount = row.getOrNull(4)?.toIntOrNull() ?: 0,
                startDateTime = row.getOrNull(5).orEmpty().ifBlank { return@forEach },
                amountMinor = row.getOrNull(6).toMinor(),
                type = type,
                categoryId = categoryFor(row.getOrNull(8).orEmpty(), type)?.id,
                accountId = account.id,
                toAccountId = accountFor(row.getOrNull(10).orEmpty())?.id,
                feeMinor = row.getOrNull(11).toMinor(),
                note = row.getOrNull(12).orEmpty(),
                description = row.getOrNull(13).orEmpty()
            )
        }

        database.recurrenceRuleDao().clear()
        database.transactionDao().clear()
        database.categoryDao().clear()
        database.accountDao().clear()
        database.accountGroupDao().clear()

        database.accountGroupDao().insertAll(groups.values.toList())
        database.accountDao().insertAll(accounts.values.toList())
        database.categoryDao().insertAll(categories.values.toList())
        database.transactionDao().upsertAll(transactions)
        rules.forEach { database.recurrenceRuleDao().insert(it) }

        return BackupResult.Success("Imported ${transactions.size} transactions")
    }

    /** Accepts either the backup folder itself or a parent holding MoneyManager_* folders. */
    private fun resolveBackupFolder(picked: DocumentFile): DocumentFile? {
        if (picked.findFile(FILE_TRANSACTIONS) != null) return picked
        return picked.listFiles()
            .filter { it.isDirectory && it.findFile(FILE_TRANSACTIONS) != null }
            .maxByOrNull { it.name.orEmpty() }
    }

    private fun write(folder: DocumentFile, name: String, content: String) {
        val file = folder.createFile(MIME_CSV, name) ?: return
        context.contentResolver.openOutputStream(file.uri)?.use { stream ->
            stream.write(content.toByteArray())
        }
    }

    private fun read(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            .orEmpty()

    private companion object {
        val AccountColumns = listOf(
            "name", "group", "openingBalance", "description", "hidden",
            "settlementDay", "paymentDay"
        )
        val CategoryColumns = listOf("name", "type", "icon")
        val GroupColumns = listOf("name", "isCard")
        val TransactionColumns = listOf(
            "date", "time", "type", "category", "account", "toAccount",
            "amount", "fee", "note", "description"
        )
        val RecurringColumns = listOf(
            "mode", "frequency", "period", "count", "generatedCount", "startDateTime",
            "amount", "type", "category", "account", "toAccount", "fee", "note", "description"
        )
    }
}

private fun Long.asDecimal(): String = "%d.%02d".format(this / 100, kotlin.math.abs(this % 100))

private fun String?.toMinor(): Long {
    val text = this?.trim().orEmpty()
    if (text.isEmpty()) return 0L
    val value = text.toDoubleOrNull() ?: return 0L
    return Math.round(value * 100)
}

