package com.greninjaop.mailorganizer.data.calendar

/**
 * Minimal JSON parser and writer for the Google Calendar API client
 * (Phase 15).
 *
 * Same rule as the rest of the data path: no kotlinx.serialization, no
 * org.json (that's an android.jar stub in unit tests). This parser handles
 * the nested shapes the Calendar API returns (objects, arrays, strings,
 * numbers, booleans, null) and the writer escapes request bodies safely.
 *
 * Pure Kotlin — unit-tested on the JVM.
 */
sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val raw: String) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

/** Convenience accessors — null when the shape doesn't match. */
fun JsonValue.obj(key: String): JsonValue.Obj? =
    (this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Obj

fun JsonValue.arr(key: String): List<JsonValue>? =
    ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Arr)?.items

fun JsonValue.str(key: String): String? =
    ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Str)?.value

fun JsonValue.bool(key: String): Boolean? =
    ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Bool)?.value

fun JsonValue.num(key: String): Long? =
    ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Num)?.raw?.toLongOrNull()

object CalendarJson {

    /**
     * Parses a JSON document. Returns null for malformed input — never
     * throws.
     */
    fun parse(text: String): JsonValue? {
        return runCatching {
            val parser = Parser(text.trim())
            val value = parser.parseValue()
            parser.skipWs()
            if (!parser.atEnd()) return null
            value
        }.getOrNull()
    }

    /** Serializes a [JsonValue] to compact JSON. */
    fun stringify(value: JsonValue): String = buildString {
        appendValue(value)
    }

    /** JSON string escaping for request bodies. */
    fun escape(raw: String): String = buildString {
        for (c in raw) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    private fun StringBuilder.appendValue(value: JsonValue) {
        when (value) {
            is JsonValue.Null -> append("null")
            is JsonValue.Bool -> append(if (value.value) "true" else "false")
            is JsonValue.Num -> append(value.raw)
            is JsonValue.Str -> append('"').append(escape(value.value)).append('"')
            is JsonValue.Arr -> {
                append('[')
                value.items.forEachIndexed { i, item ->
                    if (i > 0) append(',')
                    appendValue(item)
                }
                append(']')
            }
            is JsonValue.Obj -> {
                append('{')
                value.fields.entries.forEachIndexed { i, (k, v) ->
                    if (i > 0) append(',')
                    append('"').append(escape(k)).append("\":")
                    appendValue(v)
                }
                append('}')
            }
        }
    }

    private class Parser(val s: String) {
        var i = 0

        fun atEnd(): Boolean = i >= s.length
        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): JsonValue {
            skipWs()
            require(i < s.length) { "empty" }
            return when (val c = s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't' -> {
                    require(s.startsWith("true", i)) { "bad literal" }
                    i += 4
                    JsonValue.Bool(true)
                }
                'f' -> {
                    require(s.startsWith("false", i)) { "bad literal" }
                    i += 5
                    JsonValue.Bool(false)
                }
                'n' -> {
                    require(s.startsWith("null", i)) { "bad literal" }
                    i += 4
                    JsonValue.Null
                }
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("unexpected '$c'")
            }
        }

        private fun parseObject(): JsonValue.Obj {
            i++ // {
            val fields = linkedMapOf<String, JsonValue>()
            skipWs()
            if (i < s.length && s[i] == '}') {
                i++
                return JsonValue.Obj(fields)
            }
            while (true) {
                skipWs()
                require(i < s.length && s[i] == '"') { "key" }
                val key = parseString()
                skipWs()
                require(i < s.length && s[i] == ':') { "colon" }
                i++
                fields[key] = parseValue()
                skipWs()
                require(i < s.length) { "truncated" }
                when (s[i]) {
                    ',' -> { i++; continue }
                    '}' -> { i++; return JsonValue.Obj(fields) }
                    else -> throw IllegalArgumentException("expected , or }")
                }
            }
        }

        private fun parseArray(): JsonValue.Arr {
            i++ // [
            val items = mutableListOf<JsonValue>()
            skipWs()
            if (i < s.length && s[i] == ']') {
                i++
                return JsonValue.Arr(items)
            }
            while (true) {
                items.add(parseValue())
                skipWs()
                require(i < s.length) { "truncated" }
                when (s[i]) {
                    ',' -> { i++; continue }
                    ']' -> { i++; return JsonValue.Arr(items) }
                    else -> throw IllegalArgumentException("expected , or ]")
                }
            }
        }

        private fun parseString(): String {
            require(s[i] == '"')
            i++
            val sb = StringBuilder()
            while (true) {
                require(i < s.length) { "unterminated string" }
                val c = s[i]
                if (c == '"') {
                    i++
                    return sb.toString()
                }
                if (c == '\\') {
                    i++
                    require(i < s.length) { "bad escape" }
                    when (s[i]) {
                        '"', '\\', '/' -> sb.append(s[i])
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            require(i + 4 < s.length) { "bad unicode escape" }
                            val hex = s.substring(i + 1, i + 5)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> throw IllegalArgumentException("bad escape")
                    }
                    i++
                } else {
                    sb.append(c)
                    i++
                }
            }
        }

        private fun parseNumber(): JsonValue.Num {
            val start = i
            if (i < s.length && s[i] == '-') i++
            require(i < s.length && s[i] in '0'..'9') { "bad number" }
            while (i < s.length && (s[i] in '0'..'9' || s[i] == '.' ||
                        s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')
            ) i++
            return JsonValue.Num(s.substring(start, i))
        }
    }
}
