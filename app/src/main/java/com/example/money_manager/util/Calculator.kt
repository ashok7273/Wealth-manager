package com.example.money_manager.util

import java.math.BigDecimal
import java.math.RoundingMode

const val OP_ADD = '+'
const val OP_SUBTRACT = '\u2212'
const val OP_MULTIPLY = '\u00D7'
const val OP_DIVIDE = '\u00F7'

private val OPERATORS = charArrayOf(OP_ADD, OP_SUBTRACT, OP_MULTIPLY, OP_DIVIDE)

fun Char.isCalculatorOperator(): Boolean = this in OPERATORS

/** The number currently being typed, i.e. everything after the last operator. */
fun String.currentOperand(): String = takeLastWhile { !it.isCalculatorOperator() }

fun String.hasOperator(): Boolean = any { it.isCalculatorOperator() }

/**
 * Evaluates an infix expression of positive decimals with + − × ÷ and normal precedence.
 * A trailing operator is ignored. Returns null when the expression is empty or malformed.
 */
fun evaluateExpression(expression: String): BigDecimal? {
    val values = mutableListOf<BigDecimal>()
    val operators = mutableListOf<Char>()
    val operand = StringBuilder()

    for (char in expression) {
        if (char.isCalculatorOperator()) {
            val value = operand.toString().toBigDecimalOrNull() ?: return null
            values += value
            operators += char
            operand.clear()
        } else {
            operand.append(char)
        }
    }
    if (operand.isEmpty()) {
        if (operators.isEmpty()) return null
        operators.removeAt(operators.lastIndex)
    } else {
        values += operand.toString().toBigDecimalOrNull() ?: return null
    }
    if (values.isEmpty()) return null

    var index = 0
    while (index < operators.size) {
        val operator = operators[index]
        if (operator == OP_MULTIPLY || operator == OP_DIVIDE) {
            val left = values[index]
            val right = values[index + 1]
            if (operator == OP_DIVIDE && right.signum() == 0) return null
            values[index] = if (operator == OP_MULTIPLY) {
                left.multiply(right)
            } else {
                left.divide(right, 2, RoundingMode.HALF_UP)
            }
            values.removeAt(index + 1)
            operators.removeAt(index)
        } else {
            index++
        }
    }

    var result = values.first()
    operators.forEachIndexed { position, operator ->
        val next = values[position + 1]
        result = if (operator == OP_ADD) result.add(next) else result.subtract(next)
    }
    return result.setScale(2, RoundingMode.HALF_UP)
}

private fun String.toBigDecimalOrNull(): BigDecimal? =
    if (isEmpty()) null else runCatching { BigDecimal(this) }.getOrNull()
