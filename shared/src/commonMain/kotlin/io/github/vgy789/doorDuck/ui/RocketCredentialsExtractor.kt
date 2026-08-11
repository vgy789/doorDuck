package io.github.vgy789.doorDuck.ui

data class ExtractedRocketCredentials(
    val authToken: String?,
    val userId: String?,
)

object RocketCredentialsExtractor {
    private val credentialRunRegex = Regex("[A-Za-z0-9_-]+")

    fun extract(raw: String): ExtractedRocketCredentials {
        val credentialRuns = credentialRunRegex.findAll(raw).map { it.value }
        val token = credentialRuns.firstOrNull { it.length == TOKEN_LENGTH }
        val userId = credentialRuns.firstOrNull { it.length == USER_ID_LENGTH }

        return ExtractedRocketCredentials(
            authToken = token,
            userId = userId,
        )
    }

    private const val TOKEN_LENGTH = 43
    private const val USER_ID_LENGTH = 17
}
