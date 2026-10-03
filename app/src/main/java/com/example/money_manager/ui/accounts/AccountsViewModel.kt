package com.example.money_manager.ui.accounts

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.AccountRepository
import com.example.money_manager.data.AccountGroupRepository
import com.example.money_manager.data.NEW_ACCOUNT_ID
import com.example.money_manager.data.NEW_GROUP_ID
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.TransactionRepository
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.AccountGroup
import com.example.money_manager.data.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class AccountsViewModel(
    private val accountRepository: AccountRepository = ServiceLocator.accountRepository,
    private val groupRepository: AccountGroupRepository = ServiceLocator.accountGroupRepository,
    private val transactionRepository: TransactionRepository = ServiceLocator.transactionRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.accounts
    val groups: StateFlow<List<AccountGroup>> = groupRepository.groups
    val transactions: StateFlow<List<Transaction>> = transactionRepository.transactions

    private val _statsMonth = MutableStateFlow(YearMonth.now())
    val statsMonth: StateFlow<YearMonth> = _statsMonth.asStateFlow()

    fun shiftStatsMonth(direction: Int) {
        _statsMonth.value = _statsMonth.value.plusMonths(direction.toLong())
    }

    fun resetStatsMonth() {
        _statsMonth.value = YearMonth.now()
    }

    fun addAccount(
        name: String,
        group: AccountGroup,
        openingBalanceMinor: Long,
        description: String,
        settlementDay: Int? = null,
        paymentDay: Int? = null
    ) {
        accountRepository.add(
            Account(
                id = NEW_ACCOUNT_ID,
                name = name.trim(),
                group = group,
                openingBalanceMinor = openingBalanceMinor,
                description = description.trim(),
                settlementDay = settlementDay,
                paymentDay = paymentDay
            )
        )
    }

    fun toggleHidden(account: Account) {
        accountRepository.update(account.copy(hidden = !account.hidden))
    }
    fun updateAccount(
        account: Account,
        name: String,
        group: AccountGroup,
        openingBalanceMinor: Long,
        description: String,
        settlementDay: Int? = null,
        paymentDay: Int? = null
    ) {
        accountRepository.update(
            account.copy(
                name = name.trim(),
                group = group,
                openingBalanceMinor = openingBalanceMinor,
                description = description.trim(),
                settlementDay = settlementDay,
                paymentDay = paymentDay
            )
        )
    }

    /** Removes the account and every transaction that referenced it. */
    fun deleteAccount(account: Account) {
        transactionRepository.deleteByAccount(account.id)
        accountRepository.delete(account.id)
    }

    fun transactionCountFor(accountId: Long): Int =
        transactions.value.count { it.account.id == accountId || it.toAccount?.id == accountId }

    fun addGroup(name: String, isCard: Boolean) {
        groupRepository.add(AccountGroup(id = NEW_GROUP_ID, name = name.trim(), isCard = isCard))
    }

    fun updateGroup(group: AccountGroup, name: String, isCard: Boolean) {
        groupRepository.update(group.copy(name = name.trim(), isCard = isCard))
    }

    /** Removes the group along with its accounts and their transactions. */
    fun deleteGroup(group: AccountGroup) {
        accounts.value.filter { it.group.id == group.id }
            .forEach { transactionRepository.deleteByAccount(it.id) }
        groupRepository.delete(group.id)
    }

    fun accountCountFor(groupId: Long): Int = accounts.value.count { it.group.id == groupId }
}
