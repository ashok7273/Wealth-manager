package com.example.money_manager.data.backup

/** Minimal RFC 4180 helpers; enough for the fields this app writes. */
object Csv {

    fun row(values: List<String>): String = values.joinToString(",") { escape(it) }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Parses a whole document into rows of fields, honouring quoted newlines. */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun endField() {
            fields.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            if (fields.size > 1 || fields.firstOrNull()?.isNotEmpty() == true) rows.add(fields)
            fields = mutableListOf()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes && char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }

                char == '"' -> inQuotes = !inQuotes
                !inQuotes && char == ',' -> endField()
                !inQuotes && char == '\n' -> endRow()
                !inQuotes && char == '\r' -> Unit
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) endRow()
        return rows
    }
}
