package com.ble.signal.analyzer.export

/** RFC4180-style escaping shared by every exported CSV schema. */
object CsvEscaper {
    fun escape(value: String): String {
        val needsQuotes = value.any { character ->
            character == ',' || character == '"' || character == '\n' || character == '\r'
        }
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun row(values: Iterable<String>): String = values.joinToString(",", transform = ::escape)

    fun document(headers: List<String>, rows: List<List<String>>): String = buildString {
        append(row(headers))
        append(CRLF)
        rows.forEach { values ->
            append(row(values))
            append(CRLF)
        }
    }

    private const val CRLF = "\r\n"
}
