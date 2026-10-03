package com.example.money_manager.ui.entry

import com.example.money_manager.data.NEW_TRANSACTION_ID
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceRule
import com.example.money_manager.data.model.RecurrenceSpec
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.util.currentOperand
import com.example.money_manager.util.entryDateLabel
import com.example.money_manager.util.evaluateExpression
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.isCalculatorOperator
import java.math.RoundingMode
import java.time.LocalDateTime

enum class EntrySheet { NONE, ACCOUNT, TO_ACCOUNT, CATEGORY, AMOUNT, FEE, RECURRENCE }

data class EntryFormState(
    val id: Long = NEW_TRANSACTION_ID,
    val type: TransactionType = TransactionType.EXPENSE,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val account: Account? = null,
    val toAccount: Account? = null,
    val category: Category? = null,
    /** Raw major-unit text as typed, e.g. "1234.5". */
    val amountText: String = "",
    val feeVisible: Boolean = false,
    val feeText: String = "",
    val note: String = "",
    val description: String = "",
    val photoPath: String? = null,
    val recurrence: RecurrenceSpec? = null
) {
    val isEditing: Boolean get() = id != NEW_TRANSACTION_ID

    /** Installments are only offered for expenses. */
    val allowsInstallment: Boolean get() = type == TransactionType.EXPENSE
}

/** Returns an error message, or null when the form can be saved. */
fun EntryFormState.validate(): String? {
    val amount = amountText.toMinorUnits()
    if (amount == null) return "Enter a valid amount"
    if (amount <= 0L) return "Amount must be greater than zero"
    if (account == null) {
        return if (type == TransactionType.TRANSFER) "Select the From account" else "Select an account"
    }
    if (type == TransactionType.TRANSFER) {
        if (toAccount == null) return "Select the To account"
        if (toAccount.id == account.id) return "From and To must be different accounts"
    } else if (category == null) {
        return "Select a category"
    }
    return null
}

fun EntryFormState.toTransaction(): Transaction = Transaction(
    id = id,
    amountMinor = amountText.toMinorUnits() ?: 0L,
    type = type,
    category = if (type == TransactionType.TRANSFER) null else category,
    account = requireNotNull(account),
    toAccount = if (type == TransactionType.TRANSFER) toAccount else null,
    feeMinor = if (type == TransactionType.TRANSFER && feeVisible) feeText.toMinorUnits() ?: 0L else 0L,
    dateTime = dateTime,
    note = note.trim(),
    description = description.trim(),
    photoPath = photoPath
)

fun Transaction.toFormState(): EntryFormState = EntryFormState(
    id = id,
    type = type,
    dateTime = dateTime,
    account = account,
    toAccount = toAccount,
    category = category,
    amountText = amountMinor.toAmountText(),
    feeVisible = feeMinor > 0L,
    feeText = if (feeMinor > 0L) feeMinor.toAmountText() else "",
    note = note,
    description = description,
    photoPath = photoPath
)

/** Builds the schedule to store. The amount entered is what each occurrence will be worth. */
fun EntryFormState.toRecurrenceRule(): RecurrenceRule? {
    val spec = recurrence ?: return null
    return RecurrenceRule(
        id = 0L,
        spec = spec.copy(
            count = spec.count.coerceIn(RecurrenceSpec.MIN_COUNT, RecurrenceSpec.MAX_COUNT)
        ),
        startDateTime = dateTime,
        generatedCount = 0,
        // Occurrences are generated later, so they cannot share one attachment.
        template = toTransaction().copy(id = NEW_TRANSACTION_ID, photoPath = null)
    )
}

/** Short label for the Rep/Inst. button, e.g. "Monthly x12". */
fun RecurrenceSpec.shortLabel(): String = when (mode) {
    RecurrenceMode.REPEAT -> "${frequency.shortLabel} x$count"
    RecurrenceMode.INSTALLMENT -> "$count ${period.unitLabel}"
}

/** Full sentence shown in the confirmation dialog. */
fun EntryFormState.recurrencePreview(): String? {
    val spec = recurrence ?: return null
    val each = (amountText.toMinorUnits() ?: return null).formatMoney()
    val first = spec.dateOf(dateTime, 0).entryDateLabel()
    val last = spec.dateOf(dateTime, spec.count - 1).entryDateLabel()
    val cadence = when (spec.mode) {
        RecurrenceMode.REPEAT -> spec.frequency.label.lowercase()
        RecurrenceMode.INSTALLMENT -> spec.period.label.lowercase()
    }
    return "$each will be added $cadence, ${spec.count} times.\n$first to $last."
}

/** Parses "12+3.5" into 1550 paise. Returns null when the expression is empty or malformed. */
fun String.toMinorUnits(): Long? {
    val value = evaluateExpression(this) ?: return null
    return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
}

fun Long.toAmountText(): String {
    val whole = this / 100L
    val fraction = this % 100L
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
}

fun String.keypadAppend(key: Char): String {
    val operand = currentOperand()
    return when {
        key.isCalculatorOperator() -> when {
            isEmpty() -> this
            last().isCalculatorOperator() -> dropLast(1) + key
            else -> this + key
        }

        key == '.' -> when {
            operand.contains('.') -> this
            operand.isEmpty() -> this + "0."
            else -> this + key
        }

        key.isDigit() -> when {
            operand.contains('.') && operand.substringAfter('.').length >= 2 -> this
            operand == "0" -> dropLast(1) + key
            operand.length >= 12 -> this
            else -> this + key
        }

        else -> this
    }
}

fun String.keypadBackspace(): String = dropLast(1)
