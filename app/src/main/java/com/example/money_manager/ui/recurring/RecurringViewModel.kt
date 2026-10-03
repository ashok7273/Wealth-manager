package com.example.money_manager.ui.recurring

import androidx.lifecycle.ViewModel
import com.example.money_manager.data.RecurrenceRuleRepository
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.model.InstallmentPeriod
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceRule
import com.example.money_manager.data.model.RecurrenceSpec
import com.example.money_manager.data.model.RepeatFrequency
import com.example.money_manager.util.entryDateLabel
import kotlinx.coroutines.flow.StateFlow

class RecurringViewModel(
    private val repository: RecurrenceRuleRepository = ServiceLocator.recurrenceRuleRepository
) : ViewModel() {

    val rules: StateFlow<List<RecurrenceRule>> = repository.rules

    fun summaryOf(rule: RecurrenceRule): String = when (rule.spec.mode) {
        RecurrenceMode.REPEAT -> "${rule.spec.frequency.label} \u00D7 ${rule.spec.count}"
        RecurrenceMode.INSTALLMENT ->
            "${rule.spec.count} ${rule.spec.period.label.lowercase()} instalments"
    }

    fun progressOf(rule: RecurrenceRule): String {
        val done = rule.generatedCount
        return if (done >= rule.spec.count) {
            "Finished"
        } else {
            "$done of ${rule.spec.count} added  \u2022  next ${
                rule.dueDateOf(done).entryDateLabel()
            }"
        }
    }

    /**
     * Edits apply only to occurrences still to come. Entries already created keep their
     * dates and amounts, and still count towards the total.
     */
    fun update(
        rule: RecurrenceRule,
        amountMinor: Long?,
        count: Int,
        frequency: RepeatFrequency,
        period: InstallmentPeriod
    ) {
        val newSpec = rule.spec.copy(
            // Never below what has already been created.
            count = count.coerceIn(
                rule.generatedCount.coerceAtLeast(RecurrenceSpec.MIN_COUNT),
                RecurrenceSpec.MAX_COUNT
            ),
            frequency = frequency,
            period = period
        )

        // A new interval must run on from the last entry created, not be recomputed
        // from the original start date, which would place occurrences in the past.
        val intervalChanged = frequency != rule.spec.frequency || period != rule.spec.period
        val startDateTime = if (intervalChanged && rule.generatedCount > 0) {
            val lastCreated = rule.dueDateOf(rule.generatedCount - 1)
            newSpec.dateOf(lastCreated, -(rule.generatedCount - 1))
        } else {
            rule.startDateTime
        }

        repository.update(
            listOf(
                rule.copy(
                    spec = newSpec,
                    startDateTime = startDateTime,
                    template = rule.template.copy(
                        amountMinor = amountMinor ?: rule.template.amountMinor
                    )
                )
            )
        )
    }

    fun delete(rule: RecurrenceRule) {
        repository.delete(rule.id)
    }
}
