package com.example.money_manager.data.model

import java.time.LocalDateTime

enum class RecurrenceMode(val label: String) {
    REPEAT("Repeat"),
    INSTALLMENT("Installment")
}

enum class RepeatFrequency(val label: String, val shortLabel: String) {
    DAILY("Every day", "Daily"),
    EVERY_2_DAYS("Every 2 days", "2 days"),
    EVERY_3_DAYS("Every 3 days", "3 days"),
    WEEKLY("Every week", "Weekly"),
    EVERY_2_WEEKS("Every 2 weeks", "2 weeks"),
    EVERY_4_WEEKS("Every 4 weeks", "4 weeks"),
    MONTHLY("Every month", "Monthly"),
    EVERY_2_MONTHS("Every 2 months", "2 months"),
    EVERY_3_MONTHS("Every 3 months", "Quarterly"),
    EVERY_6_MONTHS("Every 6 months", "Half-yearly"),
    YEARLY("Every year", "Yearly");

    fun advance(start: LocalDateTime, occurrence: Int): LocalDateTime {
        val n = occurrence.toLong()
        return when (this) {
            DAILY -> start.plusDays(n)
            EVERY_2_DAYS -> start.plusDays(2 * n)
            EVERY_3_DAYS -> start.plusDays(3 * n)
            WEEKLY -> start.plusWeeks(n)
            EVERY_2_WEEKS -> start.plusWeeks(2 * n)
            EVERY_4_WEEKS -> start.plusWeeks(4 * n)
            MONTHLY -> start.plusMonths(n)
            EVERY_2_MONTHS -> start.plusMonths(2 * n)
            EVERY_3_MONTHS -> start.plusMonths(3 * n)
            EVERY_6_MONTHS -> start.plusMonths(6 * n)
            YEARLY -> start.plusYears(n)
        }
    }
}

enum class InstallmentPeriod(val label: String, val unitLabel: String) {
    MONTHLY("Monthly", "months"),
    YEARLY("Yearly", "years");

    fun advance(start: LocalDateTime, occurrence: Int): LocalDateTime = when (this) {
        MONTHLY -> start.plusMonths(occurrence.toLong())
        YEARLY -> start.plusYears(occurrence.toLong())
    }
}

data class RecurrenceSpec(
    val mode: RecurrenceMode,
    val frequency: RepeatFrequency = RepeatFrequency.MONTHLY,
    val period: InstallmentPeriod = InstallmentPeriod.MONTHLY,
    val count: Int = DEFAULT_COUNT
) {
    fun dateOf(start: LocalDateTime, occurrence: Int): LocalDateTime = when (mode) {
        RecurrenceMode.REPEAT -> frequency.advance(start, occurrence)
        RecurrenceMode.INSTALLMENT -> period.advance(start, occurrence)
    }

    companion object {
        const val MIN_COUNT = 2
        const val MAX_COUNT = 120
        const val DEFAULT_COUNT = 12
    }
}
