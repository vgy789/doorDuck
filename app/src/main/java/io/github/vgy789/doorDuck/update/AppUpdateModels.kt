package io.github.vgy789.doorDuck.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    companion object {
        private val pattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$")

        fun parse(value: String): SemanticVersion? {
            val match = pattern.matchEntire(value.trim()) ?: return null
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
            )
        }
    }
}

@Serializable
data class AppRelease(
    val tag: String,
    val title: String,
    val changelog: String,
    val publishedAt: String,
    val releaseUrl: String,
    val apkUrl: String,
    val checksumUrl: String,
)

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    FAILED,
}

enum class UpdateMessage {
    CHECK_FAILED,
    DOWNLOAD_FAILED,
    DOWNLOAD_INTEGRITY_FAILED,
    INSTALL_FAILED,
    INSTALL_PERMISSION_REQUIRED,
    APK_SIGNATURE_MISMATCH,
    APK_INCOMPATIBLE,
}

data class UpdateUiState(
    val automaticChecksEnabled: Boolean = false,
    val status: UpdateStatus = UpdateStatus.IDLE,
    val release: AppRelease? = null,
    val downloadProgress: Int = 0,
    val message: UpdateMessage? = null,
    val waitingForInstallPermission: Boolean = false,
    val isDialogVisible: Boolean = false,
)

internal fun shouldShowUpdateDialog(
    hasRelease: Boolean,
    status: UpdateStatus,
    delayElapsed: Boolean,
    promptDismissed: Boolean,
    manual: Boolean = false,
): Boolean {
    if (manual) return hasRelease || status == UpdateStatus.FAILED
    return delayElapsed && !promptDismissed && hasRelease && status != UpdateStatus.UP_TO_DATE
}

internal fun selectReleaseForInstallation(
    checkResult: UpdateCheckResult,
    fallbackRelease: AppRelease?,
): AppRelease? = when (checkResult) {
    is UpdateCheckResult.Available -> checkResult.release
    is UpdateCheckResult.Failed -> fallbackRelease
    UpdateCheckResult.UpToDate -> null
}

internal fun ApkCompatibilityIssue.toUpdateMessage(): UpdateMessage = when (this) {
    ApkCompatibilityIssue.SIGNATURE_MISMATCH -> UpdateMessage.APK_SIGNATURE_MISMATCH
    ApkCompatibilityIssue.INVALID_APK,
    ApkCompatibilityIssue.NOT_NEWER,
    -> UpdateMessage.APK_INCOMPATIBLE
}

internal fun UpdateMessage.canRetryUpdate(): Boolean = when (this) {
    UpdateMessage.CHECK_FAILED,
    UpdateMessage.DOWNLOAD_FAILED,
    UpdateMessage.DOWNLOAD_INTEGRITY_FAILED,
    UpdateMessage.INSTALL_FAILED,
    -> true
    UpdateMessage.INSTALL_PERMISSION_REQUIRED,
    UpdateMessage.APK_SIGNATURE_MISMATCH,
    UpdateMessage.APK_INCOMPATIBLE,
    -> false
}

internal fun AppRelease.changelogItems(): List<String> = changelog
    .lineSequence()
    .mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("#") || trimmed == "```") return@mapNotNull null
        val item = trimmed
            .replace(Regex("^[-*+]\\s+"), "")
            .replace(Regex("^\\d+[.)]\\s+"), "")
            .replace("**", "")
            .replace("`", "")
            .trim()
        item.takeIf(String::isNotBlank)
    }
    .toList()

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

internal fun GitHubReleaseDto.toAppRelease(): AppRelease? {
    if (draft || prerelease) return null
    val apk = assets.firstOrNull { it.name == "doorDuck-latest.apk" } ?: return null
    val checksum = assets.firstOrNull { it.name == "doorDuck-latest.apk.sha256" } ?: return null
    return AppRelease(
        tag = tagName,
        title = name?.takeIf(String::isNotBlank) ?: tagName,
        changelog = body.orEmpty().trim(),
        publishedAt = publishedAt.orEmpty(),
        releaseUrl = htmlUrl,
        apkUrl = apk.browserDownloadUrl,
        checksumUrl = checksum.browserDownloadUrl,
    )
}
