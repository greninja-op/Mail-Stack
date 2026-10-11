package com.greninjaop.mailorganizer.core

/**
 * Minimal recursive-descent JSON parser (Phase 4/3 follow-up).
 *
 * Dependency-free and pure JVM: `org.json` is an android.jar stub in unit
 * tests, and no JSON library is on the app's dependency graph. Supports
 * objects, arrays, strings (with escapes), numbers, booleans, and null.
 * Malformed input yields null — never throws.
 *
 * Used by the Google API clients (Gmail sync, Calendar, Tasks). Unit-tested.
 */
object JsonParser {

    sealed interface JsonValue {
        data class Obj(val fields: Map<String, JsonValue>) : JsonValue
        data class Arr(val items: List<JsonValue>) : JsonValue
        data class Str(val value: String) : JsonValue
        data class Num(val raw: String) : JsonValue {
            fun asLong(): Long? = raw.toLongOrNull()
            fun asDouble(): Double? = raw.toDoubleOrNull()
        }
        data object True : JsonValue
        data object False : JsonValue
        data object Null : JsonValue
    }

    // Convenience accessors (null-safe navigation over the tree).
    fun JsonValue.obj(key: String): JsonValue.Obj? =
        (this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Obj

    fun JsonValue.string(key: String): String? =
        ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Str)?.value

    fun JsonValue.long(key: String): Long? =
        ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Num)?.asLong()

    fun JsonValue.array(key: String): List<JsonValue>? =
        ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Arr)?.items

    fun JsonValue.bool(key: String): Boolean? = when ((this as? JsonValue.Obj)?.fields?.get(key)) {
        is JsonValue.True -> true
        is JsonValue.False -> false
        else -> null
    }

    fun parse(text: String): JsonValue? {
        val p = Parser(text)
        p.skipWs()
        val v = p.parseValue() ?: return null
        p.skipWs()
        return if (p.atEnd()) v else null
    }

    private class Parser(private val s: String) {
        var i = 0

        fun atEnd(): Boolean = i >= s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): JsonValue? {
            if (i >= s.length) return null
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()?.let { JsonValue.Str(it) }
                't' -> parseLiteral("true", JsonValue.True)
                'f' -> parseLiteral("false", JsonValue.False)
                'n' -> parseLiteral("null", JsonValue.Null)
                '-', in '0'..'9' -> parseNumber()
                else -> null
            }
        }

        private fun parseLiteral(literal: String, value: JsonValue): JsonValue? {
            if (s.startsWith(literal, i)) {
                i += literal.length
                return value
            }
            return null
        }

        private fun parseObject(): JsonValue.Obj? {
            i++ // {
            val fields = mutableMapOf<String, JsonValue>()
            skipWs()
            if (i < s.length && s[i] == '}') {
                i++
                return JsonValue.Obj(fields)
            }
            while (true) {
                skipWs()
                val key = parseString() ?: return null
                skipWs()
                if (i >= s.length || s[i] != ':') return null
                i++
                skipWs()
                val value = parseValue() ?: return null
                fields[key] = value
                skipWs()
                if (i >= s.length) return null
                when (s[i]) {
                    ',' -> { i++; continue }
                    '}' -> { i++; return JsonValue.Obj(fields) }
                    else -> return null
                }
            }
        }

        private fun parseArray(): JsonValue.Arr? {
            i++ // [
            val items = mutableListOf<JsonValue>()
            skipWs()
            if (i < s.length && s[i] == ']') {
                i++
                return JsonValue.Arr(items)
            }
            while (true) {
                skipWs()
                items.add(parseValue() ?: return null)
                skipWs()
                if (i >= s.length) return null
                when (s[i]) {
                    ',' -> { i++; continue }
                    ']' -> { i++; return JsonValue.Arr(items) }
                    else -> return null
                }
            }
        }

        private fun parseString(): String? {
            if (i >= s.length || s[i] != '"') return null
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i]
                if (c == '"') {
                    i++
                    return sb.toString()
                }
                if (c == '\\') {
                    i++
                    if (i >= s.length) return null
                    when (s[i]) {
                        '"', '\\', '/' -> sb.append(s[i])
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 4 >= s.length) return null
                            val code = s.substring(i + 1, i + 5).toIntOrNull(16) ?: return null
                            sb.append(code.toChar())
                            i += 4
                        }
                        else -> return null
                    }
                    i++
                } else {
                    sb.append(c)
                    i++
                }
            }
            return null
        }

        private fun parseNumber(): JsonValue.Num? {
            val start = i
            if (i < s.length && s[i] == '-') i++
            if (i >= s.length) return null
            if (s[i] == '0') {
                i++
            } else if (s[i] in '1'..'9') {
                while (i < s.length && s[i] in '0'..'9') i++
            } else {
                return null
            }
            if (i < s.length && s[i] == '.') {
                i++
                if (i >= s.length || s[i] !in '0'..'9') return null
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i >= s.length || s[i] !in '0'..'9') return null
                while (i < s.length && s[i] in '0'..'9') i++
            }
            return JsonValue.Num(s.substring(start, i))
        }
    }
}
