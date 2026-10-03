package com.example.money_manager.data.model

import java.time.LocalDateTime

/**
 * A scheduled series. Entries are not written up front; the engine materialises each
 * occurrence once its due date has arrived.
 */
data class RecurrenceRule(
    val id: Long,
    val spec: RecurrenceSpec,
    val startDateTime: LocalDateTime,
    val generatedCount: Int,
    /** Field template for each occurrence; its own id and dateTime are ignored. */
    val template: Transaction
) {
    fun dueDateOf(occurrence: Int): LocalDateTime = spec.dateOf(startDateTime, occurrence)

    fun noteFor(occurrence: Int): String = when (spec.mode) {
        RecurrenceMode.REPEAT -> template.note
        RecurrenceMode.INSTALLMENT -> "${template.note} (${occurrence + 1}/${spec.count})".trim()
    }
}
