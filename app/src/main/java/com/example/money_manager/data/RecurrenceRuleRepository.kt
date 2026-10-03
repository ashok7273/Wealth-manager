package com.example.money_manager.data

import com.example.money_manager.data.model.RecurrenceRule
import com.example.money_manager.data.model.Transaction
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

interface RecurrenceRuleRepository {
    val rules: StateFlow<List<RecurrenceRule>>
    fun add(rule: RecurrenceRule)
    fun update(rules: List<RecurrenceRule>)
    fun delete(id: Long)
}

/**
 * Writes the occurrences of every rule whose due date has passed, then remembers how far
 * each rule has advanced so the same occurrence is never written twice.
 */
class RecurrenceEngine(
    private val transactions: TransactionRepository,
    private val ruleRepository: RecurrenceRuleRepository
) {
    fun materialiseDue(now: LocalDateTime = LocalDateTime.now()) {
        val created = mutableListOf<Transaction>()
        val advanced = mutableListOf<RecurrenceRule>()

        ruleRepository.rules.value.forEach { rule ->
            val (occurrences, reached) = dueOccurrences(rule, now)
            created += occurrences
            if (reached != rule.generatedCount) advanced += rule.copy(generatedCount = reached)
        }

        transactions.upsertAll(created)
        ruleRepository.update(advanced)
    }

    /**
     * Stores a brand-new rule with its already-due occurrences written up front, so they show
     * immediately instead of waiting for the rule to land in the observed list.
     */
    fun addRule(rule: RecurrenceRule, now: LocalDateTime = LocalDateTime.now()) {
        val (created, reached) = dueOccurrences(rule, now)
        transactions.upsertAll(created)
        ruleRepository.add(rule.copy(generatedCount = reached))
    }

    /** The occurrences of [rule] due by [now], plus the occurrence index reached. */
    private fun dueOccurrences(
        rule: RecurrenceRule,
        now: LocalDateTime
    ): Pair<List<Transaction>, Int> {
        val created = mutableListOf<Transaction>()
        var occurrence = rule.generatedCount
        while (occurrence < rule.spec.count) {
            val due = rule.dueDateOf(occurrence)
            if (due.isAfter(now)) break
            created += rule.template.copy(
                id = NEW_TRANSACTION_ID,
                dateTime = due,
                note = rule.noteFor(occurrence)
            )
            occurrence++
        }
        return created to occurrence
    }
}
