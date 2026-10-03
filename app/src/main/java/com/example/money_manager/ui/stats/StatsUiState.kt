package com.example.money_manager.ui.stats

import androidx.compose.ui.graphics.Color
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class StatsPeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ANNUALLY("Annually"),
    PERIOD("Period")
}

data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)
}

private val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun LocalDate.shortDate(): String = shortDateFormatter.format(this)

fun StatsPeriod.rangeFor(anchor: LocalDate, custom: DateRange): DateRange = when (this) {
    StatsPeriod.WEEKLY -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        DateRange(start, start.plusDays(6))
    }

    StatsPeriod.MONTHLY -> YearMonth.from(anchor).let { DateRange(it.atDay(1), it.atEndOfMonth()) }

    StatsPeriod.ANNUALLY -> DateRange(
        LocalDate.of(anchor.year, 1, 1),
        LocalDate.of(anchor.year, 12, 31)
    )

    StatsPeriod.PERIOD -> if (custom.start.isAfter(custom.end)) {
        DateRange(custom.end, custom.start)
    } else {
        custom
    }
}

fun StatsPeriod.shift(anchor: LocalDate, direction: Int): LocalDate {
    val step = direction.toLong()
    return when (this) {
        StatsPeriod.WEEKLY -> anchor.plusWeeks(step)
        StatsPeriod.MONTHLY -> anchor.plusMonths(step)
        StatsPeriod.ANNUALLY -> anchor.plusYears(step)
        StatsPeriod.PERIOD -> anchor
    }
}

data class MonthPoint(val yearMonth: YearMonth, val amountMinor: Long)

/** Totals for one category over the [months] ending at [endMonth], oldest first. */
fun categoryMonthlyTrend(
    transactions: List<Transaction>,
    categoryId: Long,
    endMonth: YearMonth,
    months: Int = 6
): List<MonthPoint> {
    val relevant = transactions.filter { it.category?.id == categoryId }
    return (months - 1 downTo 0).map { back ->
        val month = endMonth.minusMonths(back.toLong())
        MonthPoint(
            yearMonth = month,
            amountMinor = relevant
                .filter { YearMonth.from(it.date) == month }
                .sumOf { it.amountMinor }
        )
    }
}

fun StatsPeriod.title(range: DateRange): String = when (this) {
    StatsPeriod.MONTHLY -> monthFormatter.format(range.start)
    StatsPeriod.ANNUALLY -> range.start.year.toString()
    else -> "${range.start.shortDate()} ~ ${range.end.shortDate()}"
}

data class CategorySlice(
    val category: Category,
    val amountMinor: Long,
    val entryCount: Int,
    val fraction: Float,
    val color: Color
)

data class StatsUiState(
    val incomeMinor: Long,
    val expenseMinor: Long,
    val slices: List<CategorySlice>
) {
    val totalMinor: Long get() = slices.sumOf { it.amountMinor }
    val entryCount: Int get() = slices.sumOf { it.entryCount }
    val isEmpty: Boolean get() = slices.isEmpty()
}

/** Warm-to-cool ramp; the largest slice always gets the first colour. */
val CategoryPalette = listOf(
    Color(0xFFF2726F),
    Color(0xFFF79256),
    Color(0xFFFDBF4A),
    Color(0xFFF0DC4E),
    Color(0xFFA8DA6A),
    Color(0xFF5FCB86),
    Color(0xFF46C6C2),
    Color(0xFF48A9F8),
    Color(0xFF7C6BF2),
    Color(0xFFC86BF2),
    Color(0xFFF26BAE),
    Color(0xFF9AA4B2)
)

fun buildStats(
    transactions: List<Transaction>,
    range: DateRange,
    type: TransactionType
): StatsUiState {
    val inRange = transactions.filter { it.date in range }
    val income = inRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expense = inRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

    // Transfers move money between accounts and belong to neither side of the breakdown.
    val ofType = inRange.filter { it.type == type && it.category != null }
    val total = ofType.sumOf { it.amountMinor }

    val slices = ofType
        .groupBy { requireNotNull(it.category) }
        .map { (category, items) -> Triple(category, items.sumOf { it.amountMinor }, items.size) }
        .sortedByDescending { (_, amount, _) -> amount }
        .mapIndexed { index, (category, amount, count) ->
            CategorySlice(
                category = category,
                amountMinor = amount,
                entryCount = count,
                fraction = if (total == 0L) 0f else amount.toFloat() / total,
                color = CategoryPalette[index % CategoryPalette.size]
            )
        }

    return StatsUiState(incomeMinor = income, expenseMinor = expense, slices = slices)
}

fun categoryTransactions(
    transactions: List<Transaction>,
    range: DateRange,
    categoryId: Long
): List<Transaction> = transactions
    .filter { it.date in range && it.category?.id == categoryId }
    .sortedWith(compareByDescending<Transaction> { it.dateTime }.thenByDescending { it.id })
