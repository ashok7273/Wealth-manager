package com.example.money_manager.ui.entry

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.DefaultCatalog
import com.example.money_manager.data.NEW_TRANSACTION_ID
import com.example.money_manager.data.AccountRepository
import com.example.money_manager.data.CategoryRepository
import com.example.money_manager.data.RecurrenceEngine
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.TransactionRepository
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.InstallmentPeriod
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceSpec
import com.example.money_manager.data.model.RepeatFrequency
import com.example.money_manager.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EntryViewModel(
    private val repository: TransactionRepository = ServiceLocator.transactionRepository,
    private val engine: RecurrenceEngine = ServiceLocator.recurrenceEngine,
    accountRepository: AccountRepository = ServiceLocator.accountRepository,
    categoryRepository: CategoryRepository = ServiceLocator.categoryRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.accounts
    val categories: StateFlow<List<Category>> = categoryRepository.categories

    private val _form = MutableStateFlow(EntryFormState())
    val form: StateFlow<EntryFormState> = _form.asStateFlow()

    private val _sheet = MutableStateFlow(EntrySheet.NONE)
    val sheet: StateFlow<EntrySheet> = _sheet.asStateFlow()

    /** Called by the navigation layer when the screen is opened, not during composition. */
    fun start(transactionId: Long) {
        if (transactionId == NEW_TRANSACTION_ID) {
            val fallback = accounts.value.firstOrNull { !it.hidden } ?: accounts.value.firstOrNull()
            _form.value = EntryFormState(account = fallback ?: DefaultCatalog.defaultAccount)
            // Account is pre-filled, but its choices stay open so it is easy to change.
            _sheet.value = EntrySheet.ACCOUNT
        } else {
            _form.value = repository.findById(transactionId)?.toFormState() ?: EntryFormState()
            _sheet.value = EntrySheet.NONE
        }
    }

    /** Opens a blank entry already charged to [accountId]. */
    fun startForAccount(accountId: Long) {
        val account = accounts.value.firstOrNull { it.id == accountId }
        if (account == null) {
            start(NEW_TRANSACTION_ID)
            return
        }
        _form.value = EntryFormState(account = account)
        _sheet.value = firstUnfilledStep(_form.value)
    }

    /** Opens a blank transfer that settles the card [accountId], leaving the payer to pick. */
    fun startCardPayment(accountId: Long) {
        val card = accounts.value.firstOrNull { it.id == accountId }
        if (card == null) {
            start(NEW_TRANSACTION_ID)
            return
        }
        val payer = accounts.value.firstOrNull {
            !it.hidden && it.id != card.id && !it.group.isCard
        }
        _form.value = EntryFormState(
            type = TransactionType.TRANSFER,
            account = payer,
            toAccount = card
        )
        _sheet.value = EntrySheet.ACCOUNT
    }

    fun setType(type: TransactionType) {        _form.update {
            it.copy(
                type = type,
                // A category from the previous type is meaningless here.
                category = if (type == TransactionType.TRANSFER) null else it.category?.takeIf { c -> c.type == type },
                toAccount = if (type == TransactionType.TRANSFER) it.toAccount else null,
                feeVisible = type == TransactionType.TRANSFER && it.feeVisible,
                // Installments are expense-only.
                recurrence = it.recurrence?.takeIf { r ->
                    r.mode != RecurrenceMode.INSTALLMENT || type == TransactionType.EXPENSE
                }
            )
        }
        _sheet.value = firstUnfilledStep(_form.value)
    }

    fun setDate(date: LocalDate) = _form.update {
        it.copy(dateTime = LocalDateTime.of(date, it.dateTime.toLocalTime()))
    }

    fun setTime(time: LocalTime) = _form.update {
        it.copy(dateTime = LocalDateTime.of(it.dateTime.toLocalDate(), time))
    }

    fun setAccount(account: Account) {
        _form.update { it.copy(account = account) }
        advanceFrom(EntrySheet.ACCOUNT)
    }

    fun setToAccount(account: Account) {
        _form.update { it.copy(toAccount = account) }
        advanceFrom(EntrySheet.TO_ACCOUNT)
    }

    fun setCategory(category: Category) {
        _form.update { it.copy(category = category) }
        advanceFrom(EntrySheet.CATEGORY)
    }

    fun swapAccounts() = _form.update { it.copy(account = it.toAccount, toAccount = it.account) }

    fun setNote(note: String) = _form.update { it.copy(note = note) }

    fun setDescription(description: String) = _form.update { it.copy(description = description) }

    fun setPhoto(name: String?) = _form.update { it.copy(photoPath = name) }
    fun showFeeField() {
        _form.update { it.copy(feeVisible = true) }
        _sheet.value = EntrySheet.FEE
    }

    fun removeFeeField() {
        _form.update { it.copy(feeVisible = false, feeText = "") }
        if (_sheet.value == EntrySheet.FEE) _sheet.value = EntrySheet.NONE
    }

    fun openSheet(sheet: EntrySheet) {
        _sheet.value = sheet
    }

    fun closeSheet() {
        _sheet.value = EntrySheet.NONE
    }

    fun onKeypadKey(key: Char) = editActiveAmount { it.keypadAppend(key) }

    fun onKeypadBackspace() = editActiveAmount { it.keypadBackspace() }

    fun onKeypadClear() = editActiveAmount { "" }

    /** Collapses the expression to its result, e.g. "500+200" becomes "700". */
    fun onKeypadEquals() = editActiveAmount { text ->
        text.toMinorUnits()?.takeIf { it >= 0L }?.toAmountText() ?: text
    }

    fun onKeypadDone() {
        onKeypadEquals()
        closeSheet()
    }

    private inline fun editActiveAmount(transform: (String) -> String) {
        val isFee = _sheet.value == EntrySheet.FEE
        _form.update {
            if (isFee) it.copy(feeText = transform(it.feeText)) else it.copy(amountText = transform(it.amountText))
        }
    }

    /** Saves and returns null, or returns a validation error message. */
    fun save(): String? {
        val state = _form.value
        state.validate()?.let { return it }

        val rule = if (state.isEditing) null else state.toRecurrenceRule()
        if (rule == null) {
            repository.upsert(state.toTransaction())
        } else {
            engine.addRule(rule)
        }
        return null
    }

    fun validationError(): String? = _form.value.validate()

    fun setRecurrenceMode(mode: RecurrenceMode?) = _form.update { state ->
        state.copy(
            recurrence = mode?.let { newMode ->
                state.recurrence?.copy(mode = newMode) ?: RecurrenceSpec(mode = newMode)
            }
        )
    }

    fun setRepeatFrequency(frequency: RepeatFrequency) = _form.update {
        it.copy(recurrence = it.recurrence?.copy(frequency = frequency))
    }

    fun setInstallmentPeriod(period: InstallmentPeriod) = _form.update {
        it.copy(recurrence = it.recurrence?.copy(period = period))
    }

    fun setRecurrenceCount(count: Int) = _form.update {
        it.copy(
            recurrence = it.recurrence?.copy(
                count = count.coerceIn(RecurrenceSpec.MIN_COUNT, RecurrenceSpec.MAX_COUNT)
            )
        )
    }

    fun delete() {
        val id = _form.value.id
        if (id != NEW_TRANSACTION_ID) repository.delete(id)
    }

    /** Save, then reset for the next entry, keeping type/date/account. */
    fun saveAndContinue(): String? {
        val state = _form.value
        val error = save()
        if (error != null) return error
        _form.value = EntryFormState(
            type = state.type,
            dateTime = state.dateTime,
            account = state.account,
            toAccount = state.toAccount
        )
        _sheet.value = firstUnfilledStep(_form.value)
        return null
    }

    private fun advanceFrom(step: EntrySheet) {
        val state = _form.value
        val steps = stepsFor(state.type)
        val index = steps.indexOf(step)
        _sheet.value = if (index == -1) {
            EntrySheet.AMOUNT
        } else {
            steps.drop(index + 1).firstOrNull { !it.isFilled(state) } ?: EntrySheet.AMOUNT
        }
    }

    private fun firstUnfilledStep(state: EntryFormState): EntrySheet =
        stepsFor(state.type).firstOrNull { !it.isFilled(state) } ?: EntrySheet.AMOUNT

    private fun stepsFor(type: TransactionType): List<EntrySheet> =
        if (type == TransactionType.TRANSFER) {
            listOf(EntrySheet.ACCOUNT, EntrySheet.TO_ACCOUNT, EntrySheet.AMOUNT)
        } else {
            listOf(EntrySheet.ACCOUNT, EntrySheet.CATEGORY, EntrySheet.AMOUNT)
        }

    private fun EntrySheet.isFilled(state: EntryFormState): Boolean = when (this) {
        EntrySheet.ACCOUNT -> state.account != null
        EntrySheet.TO_ACCOUNT -> state.toAccount != null
        EntrySheet.CATEGORY -> state.category != null
        // The keypad is always the last stop, never skipped.
        else -> false
    }
}
