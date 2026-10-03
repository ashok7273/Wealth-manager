package com.example.money_manager.ui.accounts

import com.example.money_manager.data.model.Account
import java.time.LocalDate
import java.time.YearMonth

/** Days offered for card settlement/payment; 29-31 do not exist in every month. */
val CardDayRange: IntRange = 1..28

/** Settlement day when the account is a card with billing configured, else null. */
val Account.billingDay: Int?
    get() = settlementDay?.coerceIn(CardDayRange)?.takeIf { group.isCard }

/**
 * Window a statement covers for [month]: a card's billing cycle starting on that month's
 * settlement day, otherwise the plain calendar month.
 */
fun Account.statementRange(month: YearMonth): ClosedRange<LocalDate> {
    val day = billingDay ?: return month.atDay(1)..month.atEndOfMonth()
    return month.atDay(day)..month.plusMonths(1).atDay(day).minusDays(1)
}

data class CardCycle(val start: LocalDate, val end: LocalDate, val payDate: LocalDate) {
    fun label(): String =
        "${start.dayMonth()} ~ ${end.dayMonth()} (Pay: ${payDate.dayMonth()})"
}

private fun LocalDate.dayMonth(): String = "%02d/%02d".format(dayOfMonth, monthValue)

/**
 * Returns the closed cycle awaiting payment and the cycle still accruing.
 * A cycle runs from [settlementDay] to the day before the next settlement.
 */
fun cardCycles(
    settlementDay: Int,
    paymentDay: Int,
    today: LocalDate = LocalDate.now()
): Pair<CardCycle, CardCycle> {
    val settlement = settlementDay.coerceIn(CardDayRange)
    val payment = paymentDay.coerceIn(CardDayRange)

    val outstandingStart = if (today.dayOfMonth >= settlement) {
        today.withDayOfMonth(settlement)
    } else {
        today.minusMonths(1).withDayOfMonth(settlement)
    }
    val outstandingEnd = outstandingStart.plusMonths(1).minusDays(1)
    val outstandingPay = outstandingEnd.plusMonths(1).withDayOfMonth(payment)

    val outstanding = CardCycle(outstandingStart, outstandingEnd, outstandingPay)
    val payable = CardCycle(
        start = outstandingStart.minusMonths(1),
        end = outstandingEnd.minusMonths(1),
        payDate = outstandingPay.minusMonths(1)
    )
    return payable to outstanding
}
