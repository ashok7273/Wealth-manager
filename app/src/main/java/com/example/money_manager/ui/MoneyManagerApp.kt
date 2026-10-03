package com.example.money_manager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.NEW_ACCOUNT_ID
import com.example.money_manager.data.NEW_TRANSACTION_ID
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.ui.common.AppDestination
import com.example.money_manager.ui.accounts.AccountStatsScreen
import com.example.money_manager.ui.accounts.AccountDetailScreen
import com.example.money_manager.ui.accounts.AccountsScreen
import com.example.money_manager.ui.accounts.AccountsViewModel
import com.example.money_manager.ui.accounts.AddAccountScreen
import com.example.money_manager.ui.accounts.ManageAccountGroupsScreen
import com.example.money_manager.ui.accounts.ManageAccountListScreen
import com.example.money_manager.ui.accounts.ManageAccountsMode
import com.example.money_manager.ui.accounts.ManageAccountsScreen
import com.example.money_manager.ui.entry.EntryScreen
import com.example.money_manager.ui.entry.EntryViewModel
import com.example.money_manager.ui.filter.FilterScreen
import com.example.money_manager.ui.home.DayDetailScreen
import com.example.money_manager.ui.home.HomeScreen
import com.example.money_manager.ui.home.HomeViewModel
import com.example.money_manager.ui.lock.LockScreen
import com.example.money_manager.ui.lock.PasscodeSettingsScreen
import com.example.money_manager.ui.lock.SetPasscodeScreen
import com.example.money_manager.ui.lock.VerifyPasscodeScreen
import com.example.money_manager.ui.more.BackupScreen
import com.example.money_manager.ui.more.MoreScreen
import com.example.money_manager.ui.more.SettingsListScreen
import com.example.money_manager.ui.recurring.ManageRecurringScreen
import com.example.money_manager.ui.stats.CategoryDetailScreen
import com.example.money_manager.ui.categories.ManageCategoriesScreen
import com.example.money_manager.ui.stats.StatsScreen
import com.example.money_manager.ui.stats.StatsViewModel
import java.time.LocalDate

private sealed interface Screen {
    object Transactions : Screen
    object Stats : Screen
    object Accounts : Screen
    data class AccountForm(val accountId: Long) : Screen
    data class ManageAccounts(val mode: ManageAccountsMode) : Screen
    object AccountStats : Screen
    data class AccountDetail(val accountId: Long) : Screen
    data class CategoryDetail(val categoryId: Long) : Screen
    data class DayDetail(val date: LocalDate) : Screen
    object Filter : Screen
    object More : Screen
    object AccountSettings : Screen
    object Configuration : Screen
    object ManageAccountGroups : Screen
    object ManageAccountList : Screen
    object ManageRecurring : Screen
    object Backup : Screen
    data class PasscodeVerify(val target: Screen) : Screen
    object PasscodeSettings : Screen
    object PasscodeSet : Screen
    data class ManageCategories(val type: TransactionType) : Screen
    object Entry : Screen
}

@Composable
fun MoneyManagerApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Transactions) }
    val entryViewModel: EntryViewModel = viewModel()
    val statsViewModel: StatsViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val accountsViewModel: AccountsViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    ServiceLocator.recurrenceEngine.materialiseDue()
                    ServiceLocator.appLock.onForegrounded()
                }

                Lifecycle.Event.ON_STOP -> ServiceLocator.appLock.onBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isLocked by ServiceLocator.appLock.isLocked.collectAsState()
    if (isLocked) {
        LockScreen()
        return
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun switchTab(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun openEntry(id: Long) {
        entryViewModel.start(id)
        backStack.add(Screen.Entry)
    }

    fun openEntryForAccount(accountId: Long) {
        entryViewModel.startForAccount(accountId)
        backStack.add(Screen.Entry)
    }

    fun openCardPayment(accountId: Long) {
        entryViewModel.startCardPayment(accountId)
        backStack.add(Screen.Entry)
    }

    val onNavigate: (AppDestination) -> Unit = { destination ->
        when (destination) {
            AppDestination.TRANSACTIONS -> switchTab(Screen.Transactions)
            AppDestination.STATS -> {
                statsViewModel.resetToCurrentMonth()
                switchTab(Screen.Stats)
            }

            AppDestination.ACCOUNTS -> switchTab(Screen.Accounts)
            AppDestination.MORE -> switchTab(Screen.More)
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    when (val screen = backStack.last()) {
        Screen.Transactions -> HomeScreen(
            onAddTransaction = { openEntry(NEW_TRANSACTION_ID) },
            onEditTransaction = ::openEntry,
            onDayClick = { backStack.add(Screen.DayDetail(it)) },
            onOpenFilter = { backStack.add(Screen.Filter) },
            onNavigate = onNavigate,
            viewModel = homeViewModel
        )

        Screen.Filter -> FilterScreen(
            onClose = ::pop,
            onApply = { filter ->
                homeViewModel.applyFilter(filter)
                pop()
            },
            viewModel = homeViewModel
        )

        is Screen.DayDetail -> DayDetailScreen(
            initialDate = screen.date,
            onBack = ::pop,
            onEditTransaction = ::openEntry
        )

        Screen.Stats -> StatsScreen(
            onNavigate = onNavigate,
            onCategoryClick = { backStack.add(Screen.CategoryDetail(it)) },
            viewModel = statsViewModel
        )

        Screen.Accounts -> AccountsScreen(
            onNavigate = onNavigate,
            onAddAccount = { backStack.add(Screen.AccountForm(NEW_ACCOUNT_ID)) },
            onShowHide = {
                backStack.add(Screen.ManageAccounts(ManageAccountsMode.SHOW_HIDE))
            },
            onDeleteAccounts = {
                backStack.add(Screen.ManageAccounts(ManageAccountsMode.DELETE))
            },
            onOpenStats = {
                accountsViewModel.resetStatsMonth()
                backStack.add(Screen.AccountStats)
            },
            onAccountClick = { backStack.add(Screen.AccountDetail(it)) },
            onPayCard = ::openCardPayment,
            viewModel = accountsViewModel
        )

        is Screen.AccountDetail -> AccountDetailScreen(
            accountId = screen.accountId,
            onBack = ::pop,
            onEditTransaction = ::openEntry,
            onAddTransaction = ::openEntryForAccount,
            onEditAccount = { backStack.add(Screen.AccountForm(it)) },
            viewModel = accountsViewModel
        )

        is Screen.AccountForm -> AddAccountScreen(
            onClose = ::pop,
            accountId = screen.accountId,
            viewModel = accountsViewModel
        )

        is Screen.ManageAccounts -> ManageAccountsScreen(
            mode = screen.mode,
            onBack = ::pop,
            viewModel = accountsViewModel
        )

        Screen.AccountStats -> AccountStatsScreen(
            onBack = ::pop,
            viewModel = accountsViewModel
        )

        is Screen.CategoryDetail -> CategoryDetailScreen(
            categoryId = screen.categoryId,
            onBack = ::pop,
            onEditTransaction = ::openEntry,
            viewModel = statsViewModel
        )

        Screen.Entry -> EntryScreen(
            onClose = ::pop,
            onManageCategories = { backStack.add(Screen.ManageCategories(it)) },
            viewModel = entryViewModel
        )

        is Screen.ManageCategories -> ManageCategoriesScreen(
            type = screen.type,
            onBack = ::pop
        )

        Screen.More -> MoreScreen(
            onNavigate = onNavigate,
            onOpenConfiguration = { backStack.add(Screen.Configuration) },
            onOpenAccounts = { backStack.add(Screen.AccountSettings) },
            onOpenPasscode = {
                backStack.add(
                    if (ServiceLocator.appLock.isEnabled.value) {
                        Screen.PasscodeVerify(Screen.PasscodeSettings)
                    } else {
                        Screen.PasscodeSet
                    }
                )
            },
            onOpenBackup = {
                backStack.add(
                    if (ServiceLocator.appLock.isEnabled.value) {
                        Screen.PasscodeVerify(Screen.Backup)
                    } else {
                        Screen.Backup
                    }
                )
            }
        )

        Screen.Backup -> BackupScreen(onBack = ::pop)

        Screen.AccountSettings -> SettingsListScreen(
            title = "Accounts",
            options = listOf(
                "Account Group" to { backStack.add(Screen.ManageAccountGroups) },
                "Account setting" to { backStack.add(Screen.ManageAccountList) }
            ),
            onBack = ::pop
        )

        Screen.Configuration -> SettingsListScreen(
            title = "Configuration",
            options = listOf(
                "Income category setting" to {
                    backStack.add(Screen.ManageCategories(TransactionType.INCOME))
                },
                "Expense category setting" to {
                    backStack.add(Screen.ManageCategories(TransactionType.EXPENSE))
                },
                "Repeat setting" to { backStack.add(Screen.ManageRecurring) }
            ),
            onBack = ::pop
        )

        Screen.ManageAccountGroups -> ManageAccountGroupsScreen(
            onBack = ::pop,
            viewModel = accountsViewModel
        )

        Screen.ManageAccountList -> ManageAccountListScreen(
            onBack = ::pop,
            onAddAccount = { backStack.add(Screen.AccountForm(NEW_ACCOUNT_ID)) },
            onShowHide = {
                backStack.add(Screen.ManageAccounts(ManageAccountsMode.SHOW_HIDE))
            },
            onDeleteAccounts = {
                backStack.add(Screen.ManageAccounts(ManageAccountsMode.DELETE))
            },
            onAccountClick = { backStack.add(Screen.AccountForm(it)) },
            viewModel = accountsViewModel
        )

        Screen.ManageRecurring -> ManageRecurringScreen(onBack = ::pop)

        Screen.PasscodeSet -> SetPasscodeScreen(onDone = ::pop, onCancel = ::pop)

        is Screen.PasscodeVerify -> VerifyPasscodeScreen(
            onVerified = {
                pop()
                backStack.add(screen.target)
            },
            onCancel = ::pop
        )

        Screen.PasscodeSettings -> PasscodeSettingsScreen(
            onBack = ::pop,
            onChangePasscode = { backStack.add(Screen.PasscodeSet) },
            onTurnedOff = ::pop
        )
    }
}
