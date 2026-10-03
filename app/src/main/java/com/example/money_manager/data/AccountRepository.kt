package com.example.money_manager.data

import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.AccountGroup
import kotlinx.coroutines.flow.StateFlow

const val NEW_ACCOUNT_ID = 0L
const val NEW_GROUP_ID = 0L

interface AccountRepository {
    val accounts: StateFlow<List<Account>>
    fun add(account: Account)
    fun update(account: Account)
    fun delete(id: Long)
    fun findById(id: Long): Account?
}

interface AccountGroupRepository {
    val groups: StateFlow<List<AccountGroup>>
    fun add(group: AccountGroup)
    fun update(group: AccountGroup)
    fun delete(id: Long)
}
