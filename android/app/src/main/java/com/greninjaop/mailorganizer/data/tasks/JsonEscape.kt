package com.greninjaop.mailorganizer.data.tasks

/**
 * Escapes a string for embedding in a JSON string literal.
 * Used when building Tasks API request bodies by hand. (Parsing uses
 * the shared [com.greninjaop.mailorganizer.core.JsonParser].)
 */
internal fun jsonEscape(raw: String): String {
    val sb = StringBuilder(raw.length + 8)
    for (c in raw) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (c < ' ') {
                sb.append("\\u%04x".format(c.code))
            } else {
                sb.append(c)
            }
        }
    }
    return sb.toString()
}
