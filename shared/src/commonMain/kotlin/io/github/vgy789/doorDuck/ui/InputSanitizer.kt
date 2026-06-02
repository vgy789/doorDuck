package io.github.vgy789.doorDuck.ui

object InputSanitizer {
    private val whitespaceRegex = "\\s+".toRegex()

    fun noWhitespace(raw: String): String {
        return raw.trim().replace(whitespaceRegex, "")
    }

    fun endpoint(raw: String): String {
        val compact = noWhitespace(raw)
        if (compact.isBlank()) return ""

        val withoutScheme = compact
            .removePrefix("https://")
            .removePrefix("http://")
        val host = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .trim('.')

        if (host.isBlank()) return ""
        return "https://$host/api/v1"
    }

    fun tokensPageUrl(raw: String): String {
        val endpoint = endpoint(raw)
        if (endpoint.isBlank()) return ""
        return endpoint.removeSuffix("/api/v1") + "/account/tokens"
    }
}
