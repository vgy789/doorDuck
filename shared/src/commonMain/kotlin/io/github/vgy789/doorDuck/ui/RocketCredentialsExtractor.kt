package io.github.vgy789.doorDuck.ui

data class ExtractedRocketCredentials(
    val authToken: String?,
    val userId: String?,
)

object RocketCredentialsExtractor {
    private val tokenRegex = Regex("\\b[A-Za-z0-9_-]{43}\\b")
    private val userIdRegex = Regex("\\b[A-Za-z0-9_-]{17}\\b")

    fun extract(raw: String): ExtractedRocketCredentials {
        return ExtractedRocketCredentials(
            authToken = tokenRegex.find(raw)?.value,
            userId = userIdRegex.find(raw)?.value,
        )
    }
}
