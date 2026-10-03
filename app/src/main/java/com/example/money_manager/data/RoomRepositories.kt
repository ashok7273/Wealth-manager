package com.example.money_manager.data

import com.example.money_manager.data.local.AccountDao
import com.example.money_manager.data.local.AccountEntity
import com.example.money_manager.data.local.AccountGroupDao
import com.example.money_manager.data.local.AccountGroupEntity
import com.example.money_manager.data.local.CategoryDao
import com.example.money_manager.data.local.CategoryEntity
import com.example.money_manager.data.local.RecurrenceRuleDao
import com.example.money_manager.data.local.RecurrenceRuleEntity
import com.example.money_manager.data.local.TransactionDao
import com.example.money_manager.data.local.TransactionEntity
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.AccountGroup
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.InstallmentPeriod
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceRule
import com.example.money_manager.data.model.RecurrenceSpec
import com.example.money_manager.data.model.RepeatFrequency
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// ---------- mapping ----------

private fun AccountGroupEntity.toModel() = AccountGroup(
    id = id,
    name = name,
    isCard = isCard,
    sortOrder = sortOrder
)

private fun AccountGroup.toEntity() = AccountGroupEntity(
    id = id,
    name = name,
    isCard = isCard,
    sortOrder = sortOrder
)

private fun AccountEntity.toModel(groups: Map<Long, AccountGroup>) = Account(
    id = id,
    name = name,
    group = groups[groupId] ?: AccountGroup(groupId, "Others"),
    openingBalanceMinor = openingBalanceMinor,
    description = description,
    hidden = hidden,
    settlementDay = settlementDay,
    paymentDay = paymentDay
)

private fun Account.toEntity(sortOrder: Int) = AccountEntity(
    id = id,
    name = name,
    groupId = group.id,
    openingBalanceMinor = openingBalanceMinor,
    description = description,
    hidden = hidden,
    settlementDay = settlementDay,
    paymentDay = paymentDay,
    sortOrder = sortOrder
)

private fun CategoryEntity.toModel() = Category(
    id = id,
    name = name,
    emoji = emoji,
    type = TransactionType.valueOf(type)
)

private fun Category.toEntity(sortOrder: Int) = CategoryEntity(
    id = id,
    name = name,
    emoji = emoji,
    type = type.name,
    sortOrder = sortOrder
)

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amountMinor = amountMinor,
    type = type.name,
    categoryId = category?.id,
    accountId = account.id,
    toAccountId = toAccount?.id,
    feeMinor = feeMinor,
    dateTime = dateTime.toString(),
    note = note,
    description = description,
    photoPath = photoPath
)

private fun TransactionEntity.toModel(
    accounts: Map<Long, Account>,
    categories: Map<Long, Category>
): Transaction? {
    val account = accounts[accountId] ?: return null
    return Transaction(
        id = id,
        amountMinor = amountMinor,
        type = TransactionType.valueOf(type),
        category = categoryId?.let { categories[it] },
        account = account,
        toAccount = toAccountId?.let { accounts[it] },
        feeMinor = feeMinor,
        dateTime = LocalDateTime.parse(dateTime),
        note = note,
        description = description,
        photoPath = photoPath
    )
}

// ---------- repositories ----------

class RoomAccountGroupRepository(
    private val dao: AccountGroupDao,
    private val accountDao: AccountDao,
    private val scope: CoroutineScope
) : AccountGroupRepository {

    override val groups: StateFlow<List<AccountGroup>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(group: AccountGroup) {
        scope.launch {
            dao.insert(group.copy(sortOrder = dao.maxSortOrder() + 1).toEntity().copy(id = 0))
        }
    }

    override fun update(group: AccountGroup) {
        scope.launch { dao.update(group.toEntity()) }
    }

    override fun delete(id: Long) {
        scope.launch {
            accountDao.deleteByGroup(id)
            dao.delete(id)
        }
    }
}

class RoomAccountRepository(
    private val dao: AccountDao,
    groupsFlow: StateFlow<List<AccountGroup>>,
    private val scope: CoroutineScope
) : AccountRepository {

    override val accounts: StateFlow<List<Account>> =
        combine(dao.observeAll(), groupsFlow) { entities, groups ->
            val groupsById = groups.associateBy { it.id }
            entities.map { it.toModel(groupsById) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(account: Account) {
        scope.launch {
            dao.insert(account.toEntity(sortOrder = dao.maxSortOrder() + 1).copy(id = 0))
        }
    }

    override fun update(account: Account) {
        scope.launch {
            val order = accounts.value.indexOfFirst { it.id == account.id }.coerceAtLeast(0)
            dao.update(account.toEntity(order))
        }
    }

    override fun delete(id: Long) {
        scope.launch { dao.delete(id) }
    }

    override fun findById(id: Long): Account? = accounts.value.firstOrNull { it.id == id }
}

class RoomCategoryRepository(
    private val dao: CategoryDao,
    private val scope: CoroutineScope
) : CategoryRepository {

    override val categories: StateFlow<List<Category>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(category: Category) {
        scope.launch {
            dao.insert(category.toEntity(sortOrder = dao.maxSortOrder() + 1).copy(id = 0))
        }
    }

    override fun update(category: Category) {
        scope.launch {
            val order = categories.value.indexOfFirst { it.id == category.id }.coerceAtLeast(0)
            dao.update(category.toEntity(order))
        }
    }

    override fun delete(id: Long) {
        scope.launch { dao.delete(id) }
    }

    override fun reorder(type: TransactionType, ordered: List<Category>) {
        scope.launch {
            val others = categories.value.filterNot { it.type == type }
            val entities = ordered.mapIndexed { index, category -> category.toEntity(index) } +
                others.mapIndexed { index, category ->
                    category.toEntity(ordered.size + index)
                }
            dao.updateAll(entities)
        }
    }
}

class RoomTransactionRepository(
    private val dao: TransactionDao,
    accountsFlow: StateFlow<List<Account>>,
    categoriesFlow: StateFlow<List<Category>>,
    private val scope: CoroutineScope
) : TransactionRepository {

    override val transactions: StateFlow<List<Transaction>> =
        combine(dao.observeAll(), accountsFlow, categoriesFlow) { entities, accounts, categories ->
            val accountsById = accounts.associateBy { it.id }
            val categoriesById = categories.associateBy { it.id }
            entities.mapNotNull { it.toModel(accountsById, categoriesById) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun upsert(transaction: Transaction) {
        scope.launch {
            val entity = transaction.toEntity()
            dao.upsert(if (entity.id == NEW_TRANSACTION_ID) entity.copy(id = 0) else entity)
        }
    }

    override fun upsertAll(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return
        scope.launch {
            dao.upsertAll(
                transactions.map { transaction ->
                    val entity = transaction.toEntity()
                    if (entity.id == NEW_TRANSACTION_ID) entity.copy(id = 0) else entity
                }
            )
        }
    }

    override fun delete(id: Long) {
        scope.launch { dao.delete(id) }
    }

    override fun deleteAll(ids: Set<Long>) {
        if (ids.isEmpty()) return
        scope.launch { dao.deleteAll(ids.toList()) }
    }

    override fun deleteByAccount(accountId: Long) {
        scope.launch { dao.deleteByAccount(accountId) }
    }

    override fun deleteByCategory(categoryId: Long) {
        scope.launch { dao.deleteByCategory(categoryId) }
    }

    override fun findById(id: Long): Transaction? =
        transactions.value.firstOrNull { it.id == id }
}

class RoomRecurrenceRuleRepository(
    private val dao: RecurrenceRuleDao,
    accountsFlow: StateFlow<List<Account>>,
    categoriesFlow: StateFlow<List<Category>>,
    private val scope: CoroutineScope
) : RecurrenceRuleRepository {

    override val rules: StateFlow<List<RecurrenceRule>> =
        combine(dao.observeAll(), accountsFlow, categoriesFlow) { entities, accounts, categories ->
            val accountsById = accounts.associateBy { it.id }
            val categoriesById = categories.associateBy { it.id }
            entities.mapNotNull { entity ->
                val account = accountsById[entity.accountId] ?: return@mapNotNull null
                RecurrenceRule(
                    id = entity.id,
                    spec = RecurrenceSpec(
                        mode = RecurrenceMode.valueOf(entity.mode),
                        frequency = RepeatFrequency.valueOf(entity.frequency),
                        period = InstallmentPeriod.valueOf(entity.period),
                        count = entity.count
                    ),
                    startDateTime = LocalDateTime.parse(entity.startDateTime),
                    generatedCount = entity.generatedCount,
                    template = Transaction(
                        id = NEW_TRANSACTION_ID,
                        amountMinor = entity.amountMinor,
                        type = TransactionType.valueOf(entity.type),
                        category = entity.categoryId?.let { categoriesById[it] },
                        account = account,
                        toAccount = entity.toAccountId?.let { accountsById[it] },
                        feeMinor = entity.feeMinor,
                        dateTime = LocalDateTime.parse(entity.startDateTime),
                        note = entity.note,
                        description = entity.description
                    )
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(rule: RecurrenceRule) {
        scope.launch { dao.insert(rule.toEntity().copy(id = 0)) }
    }

    override fun update(rules: List<RecurrenceRule>) {
        if (rules.isEmpty()) return
        scope.launch { dao.updateAll(rules.map { it.toEntity() }) }
    }

    override fun delete(id: Long) {
        scope.launch { dao.delete(id) }
    }
}

private fun RecurrenceRule.toEntity() = RecurrenceRuleEntity(
    id = id,
    mode = spec.mode.name,
    frequency = spec.frequency.name,
    period = spec.period.name,
    count = spec.count,
    generatedCount = generatedCount,
    startDateTime = startDateTime.toString(),
    amountMinor = template.amountMinor,
    type = template.type.name,
    categoryId = template.category?.id,
    accountId = template.account.id,
    toAccountId = template.toAccount?.id,
    feeMinor = template.feeMinor,
    note = template.note,
    description = template.description
)
