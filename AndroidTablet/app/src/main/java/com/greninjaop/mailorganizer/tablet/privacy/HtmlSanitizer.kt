package com.greninjaop.mailorganizer.tablet.privacy

object HtmlSanitizer {
    fun shouldFetchRemoteImages(): Boolean = false

    fun sanitize(html: String): String {
        var out = html
        out = Regex("(?is)<script.*?>.*?</script>").replace(out, "")
        out = Regex("(?is)<img[^>]*>").replace(out, "")
        return out
    }
}
