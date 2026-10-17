package com.greninjaop.mailorganizer.core.email

/**
 * HTML sanitizer for untrusted email content (Phase 5).
 *
 * Email HTML is attacker-controlled input. This sanitizer enforces:
 * - No script execution: `<script>`/`<style>`/`<iframe>`/`<object>`/`<embed>`/
 *   `<applet>`/`<form>`/`<meta>`/`<link>`/`<base>`/`<frame>` elements (and their
 *   content) are removed entirely.
 * - No event-handler attributes (`on*`) and no inline `style` attributes
 *   (blocks `position:fixed` overlay tricks and `expression()`-style vectors).
 * - No `javascript:` / `vbscript:` / `data:` URLs in linkable attributes.
 * - HTML comments are stripped.
 *
 * Everything else (paragraphs, links with safe schemes, tables, images with
 * `http(s):` sources, …) is preserved so legitimate mail still renders.
 *
 * Pure Kotlin, no dependencies — the patterns are deliberately simple and
 * bounded to avoid catastrophic backtracking on hostile input.
 *
 * Also provides [htmlToText]: tag-stripping fallback used when a message has
 * no `text/plain` part. Script/style content never leaks into the text.
 */
object HtmlSanitizer {

    // Elements whose *content* is also removed (case-insensitive, DOTALL).
    private val dangerousElements = Regex(
        "<(script|style|iframe|object|embed|applet|form|frameset|frame|noframes|noscript)" +
            "\\b[^>]*>.*?</\\1\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    // Void/head elements with no meaningful content to keep.
    private val voidElements = Regex(
        "<(meta|link|base|title)\\b[^>]*/?>",
        RegexOption.IGNORE_CASE,
    )
    // Unclosed executable elements: browsers treat everything after `<script`
    // as script content, so remove to end of input (applied after paired removal).
    private val unclosedExecutable = Regex(
        "<(script|iframe|object|embed|applet)\\b.*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    // Leftover tag fragments (e.g. `<style` without `>`) after paired removal.
    private val danglingTags = Regex(
        "<(style|form|meta|link|base|frameset|frame|noscript)\\b[^>]*>?",
        RegexOption.IGNORE_CASE,
    )
    private val comments = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    // Declarations that are not content and must never render as text:
    // `<!DOCTYPE html ...>` (its tag name starts with `!`, so the
    // renderer's tag regex never matched it and it leaked through as a
    // visible "<!DOCTYPE html ...>" paragraph — seen in a Reddit digest),
    // `<?xml ...?>` processing instructions, and `<![CDATA[...]]>`.
    private val declarations = Regex(
        "<!DOCTYPE[^>]*>|<\\?.*?\\?>|<!\\[CDATA\\[.*?\\]\\]>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    // Event handlers: onload="…", onclick='…', onerror=… (unquoted).
    private val eventHandlers = Regex(
        "\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        RegexOption.IGNORE_CASE,
    )
    private val styleAttributes = Regex(
        "\\s+style\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        RegexOption.IGNORE_CASE,
    )
    // javascript:/vbscript:/data: in href/src/action/etc. — the attribute is dropped.
    private val dangerousUrls = Regex(
        "\\s+(href|src|action|cite|background|poster|data)\\s*=\\s*(\"[^\"]*\"|'[^']*')",
        RegexOption.IGNORE_CASE,
    )
    private val dangerousScheme = Regex(
        "^\\s*(javascript|vbscript|data)\\s*:",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Returns sanitized HTML safe for rendering. Never throws; on unexpected
     * input returns the input with all tags stripped.
     */
    fun sanitize(html: String): String {
        if (html.isEmpty()) return html
        return try {
            var out = stripDangerous(html)
            out = eventHandlers.replace(out, "")
            out = styleAttributes.replace(out, "")
            out = dangerousUrls.replace(out) { match ->
                val quoted = match.groupValues[2]
                val url = quoted.removeSurrounding("\"").removeSurrounding("'")
                if (dangerousScheme.containsMatchIn(url)) "" else match.value
            }
            out
        } catch (_: Exception) {
            // Fail closed: if anything goes wrong, return plain text.
            htmlToText(html)
        }
    }

    private fun stripDangerous(html: String): String {
        var out = comments.replace(html, "")
        out = declarations.replace(out, "")
        out = dangerousElements.replace(out, "")
        out = voidElements.replace(out, "")
        out = unclosedExecutable.replace(out, "")
        out = danglingTags.replace(out, "")
        return out
    }

    // ---- HTML → plain text ----

    private val blockBreaks = Regex(
        "<(br|p|div|tr|h1|h2|h3|h4|h5|h6|li|ul|ol|blockquote|hr|table|section|article)\\b[^>]*>",
        RegexOption.IGNORE_CASE,
    )
    private val anyTag = Regex("<[^>]+>")

    /**
     * Best-effort HTML→text conversion. Script/style/iframe content is dropped
     * (never leaks into the text), block elements become line breaks, entities
     * are decoded, whitespace is collapsed.
     */
    fun htmlToText(html: String): String {
        if (html.isEmpty()) return ""
        return try {
            var out = stripDangerous(html)
            out = blockBreaks.replace(out, "\n\n")
            out = anyTag.replace(out, "")
            out = decodeEntities(out)
            // Lone newlines are source-formatting whitespace, not paragraphs.
            out = out.replace(Regex("(?<!\\n)\\n(?!\\n)"), " ")
            // Horizontal whitespace collapses; paragraph breaks survive.
            out = out.replace(Regex("[^\\S\\n]+"), " ")
            out = out.replace(Regex("\\n{3,}"), "\n\n")
            out.trim()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Decodes HTML entities in [text]. Single-pass (no double-decoding:
     * `&amp;bull;` stays `&bull;`, it does not become `•`).
     *
     * Covers the named entities that actually appear in mail
     * (`&bull;` → `•`, `&mdash;`, quotes, currency, accented letters…).
     * Numeric entities (`&#233;`, `&#xE9;`) decode via code point.
     * `&zwnj;` / `&zwj;` decode to nothing, and literal zero-width
     * characters (U+200B/U+200C/U+200D/U+FEFF) are stripped — senders
     * (e.g. Reddit digests) pad with hundreds of them, and they must
     * never render as a wall of literal `&zwnj;` text. Unknown or
     * invalid entities are left as-is (never lose user text).
     *
     * Shared by [htmlToText] and the rich renderer (SafeHtmlRenderer),
     * so both paths decode identically.
     */
    fun decodeEntities(text: String): String {
        if (!text.contains('&') && !ZERO_WIDTH_CHARS.containsMatchIn(text)) return text
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '&') {
                val semi = text.indexOf(';', i + 1).takeIf { it in (i + 1)..(i + 12) }
                if (semi != null) {
                    val name = text.substring(i + 1, semi)
                    val decoded = NAMED_ENTITIES[name]
                        ?: decodeNumericEntity(name)
                    if (decoded != null) {
                        out.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        // Strip zero-width characters (invisible padding/tracking).
        return ZERO_WIDTH_CHARS.replace(out.toString(), "")
    }

    private fun decodeNumericEntity(name: String): String? {
        val num = when {
            name.startsWith("#x", ignoreCase = true) -> name.substring(2).toIntOrNull(16)
            name.startsWith("#") -> name.substring(1).toIntOrNull()
            else -> return null
        } ?: return null
        // Reject surrogates / out-of-range / control chars silently.
        if (num !in 0x20..0x10FFFF || num in 0xD800..0xDFFF) return null
        return String(Character.toChars(num))
    }

    private val ZERO_WIDTH_CHARS = Regex("[\\u200B\\u200C\\u200D\\uFEFF]")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to " ", "bull" to "•",
        // Zero-width joiners: decode to nothing (stripped).
        "zwnj" to "", "zwj" to "", "lrm" to "", "rlm" to "",
        "copy" to "©", "reg" to "®", "trade" to "™",
        "mdash" to "—", "ndash" to "–", "hellip" to "…",
        "lsquo" to "‘", "rsquo" to "’", "sbquo" to "‚",
        "ldquo" to "“", "rdquo" to "”", "bdquo" to "„",
        "laquo" to "«", "raquo" to "»", "lsaquo" to "‹", "rsaquo" to "›",
        "dagger" to "†", "Dagger" to "‡", "permil" to "‰",
        "prime" to "′", "Prime" to "″",
        "times" to "×", "divide" to "÷", "plusmn" to "±", "deg" to "°",
        "euro" to "€", "pound" to "£", "yen" to "¥", "cent" to "¢",
        "sect" to "§", "para" to "¶", "middot" to "·",
        "iexcl" to "¡", "iquest" to "¿", "szlig" to "ß",
        "agrave" to "à", "aacute" to "á", "acirc" to "â", "atilde" to "ã",
        "auml" to "ä", "aring" to "å", "aelig" to "æ", "ccedil" to "ç",
        "egrave" to "è", "eacute" to "é", "ecirc" to "ê", "euml" to "ë",
        "igrave" to "ì", "iacute" to "í", "icirc" to "î", "iuml" to "ï",
        "ntilde" to "ñ", "ograve" to "ò", "oacute" to "ó", "ocirc" to "ô",
        "otilde" to "õ", "ouml" to "ö", "oslash" to "ø",
        "ugrave" to "ù", "uacute" to "ú", "ucirc" to "û", "uuml" to "ü",
        "yacute" to "ý", "yuml" to "ÿ",
        "Agrave" to "À", "Aacute" to "Á", "Acirc" to "Â", "Atilde" to "Ã",
        "Auml" to "Ä", "Aring" to "Å", "AElig" to "Æ", "Ccedil" to "Ç",
        "Egrave" to "È", "Eacute" to "É", "Ecirc" to "Ê", "Euml" to "Ë",
        "Igrave" to "Ì", "Iacute" to "Í", "Icirc" to "Î", "Iuml" to "Ï",
        "Ntilde" to "Ñ", "Ograve" to "Ò", "Oacute" to "Ó", "Ocirc" to "Ô",
        "Otilde" to "Õ", "Ouml" to "Ö", "Oslash" to "Ø",
        "Ugrave" to "Ù", "Uacute" to "Ú", "Ucirc" to "Û", "Uuml" to "Ü",
        "Yacute" to "Ý",
    )
}
