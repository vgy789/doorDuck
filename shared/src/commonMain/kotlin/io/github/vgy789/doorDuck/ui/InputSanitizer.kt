package io.github.vgy789.doorDuck.ui

object InputSanitizer {
    private val whitespaceRegex = "\\s+".toRegex()

    fun noWhitespace(raw: String): String {
        return raw.trim().replace(whitespaceRegex, "")
    }

    fun endpoint(raw: String): String {
        return noWhitespace(raw)
    }
}
