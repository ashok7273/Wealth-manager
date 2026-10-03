package com.example.money_manager.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val amountFormat = DecimalFormat(
    "#,##,##0.00",
    DecimalFormatSymbols(Locale.US)
)

private val wholeAmountFormat = DecimalFormat(
    "#,##,##0",
    DecimalFormatSymbols(Locale.US)
)

const val CURRENCY_SYMBOL = "\u20B9"

/** Formats minor units (paise) as `1,653.00`. */
fun Long.formatAmount(): String = amountFormat.format(this / 100.0)

/** Formats minor units (paise) as `₹1,653.00`. */
fun Long.formatMoney(): String = CURRENCY_SYMBOL + formatAmount()

/** Compact form for chart labels: `₹532`, `₹2.2k`, `₹1.8L`, `₹3.4Cr`. */
fun Long.formatCompactMoney(): String {
    val value = this / 100.0
    val scaled: Double
    val suffix: String
    when {
        value >= 1_00_00_000 -> { scaled = value / 1_00_00_000; suffix = "Cr" }
        value >= 1_00_000 -> { scaled = value / 1_00_000; suffix = "L" }
        value >= 1_000 -> { scaled = value / 1_000; suffix = "k" }
        else -> { scaled = value; suffix = "" }
    }
    val text = if (suffix.isEmpty()) {
        String.format(Locale.US, "%.0f", scaled)
    } else {
        String.format(Locale.US, "%.1f", scaled).removeSuffix(".0")
    }
    return CURRENCY_SYMBOL + text + suffix
}

/** Formats minor units with an explicit sign, e.g. `-37,191.00`. */
fun Long.formatSigned(): String =
    if (this < 0) "-" + (-this).formatAmount() else formatAmount()

/** Rupees only, no decimals — for tight cells such as the calendar grid. */
fun Long.formatWholeAmount(): String = wholeAmountFormat.format(this / 100.0)

fun Long.formatSignedWhole(): String =
    if (this < 0) "-" + (-this).formatWholeAmount() else formatWholeAmount()

private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val monthYearFormatter = DateTimeFormatter.ofPattern("MM.yyyy", Locale.ENGLISH)
private val dayTitleFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy (EEE)", Locale.ENGLISH)
private val entryDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy (EEE)", Locale.ENGLISH)
private val entryTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun YearMonth.title(): String = monthTitleFormatter.format(this)

fun LocalDate.weekdayShort(): String = weekdayFormatter.format(this)

fun LocalDate.monthYearLabel(): String = monthYearFormatter.format(this)

fun LocalDate.dayTitle(): String = dayTitleFormatter.format(this)

fun LocalDateTime.entryDateLabel(): String = entryDateFormatter.format(this)

fun LocalDateTime.entryTimeLabel(): String = entryTimeFormatter.format(this).lowercase(Locale.ENGLISH)
