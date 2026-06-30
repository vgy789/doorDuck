package io.github.vgy789.doorDuck.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun semanticVersionParsesAndComparesReleaseTags() {
        assertEquals(SemanticVersion(1, 5, 0), SemanticVersion.parse("v1.5.0"))
        assertEquals(SemanticVersion(1, 5, 0), SemanticVersion.parse("1.5.0-beta1"))
        assertTrue(SemanticVersion.parse("v2.0.0")!! > SemanticVersion.parse("1.99.99")!!)
        assertNull(SemanticVersion.parse("release-1.5"))
    }

    @Test
    fun releaseRequiresStableReleaseAndBothAssets() {
        val valid = releaseDto(
            assets = listOf(
                GitHubReleaseAssetDto("doorDuck-latest.apk", "https://github.com/vgy789/doorDuck/releases/download/v1.5.0/doorDuck-latest.apk"),
                GitHubReleaseAssetDto("doorDuck-latest.apk.sha256", "https://github.com/vgy789/doorDuck/releases/download/v1.5.0/doorDuck-latest.apk.sha256"),
            ),
        )
        assertEquals("v1.5.0", valid.toAppRelease()?.tag)
        assertNull(valid.copy(prerelease = true).toAppRelease())
        assertNull(valid.copy(assets = valid.assets.dropLast(1)).toAppRelease())
    }

    @Test
    fun automaticCheckIsDisabledByDefaultAndDueAfterOneDay() {
        val now = 2 * UpdateRepository.CHECK_INTERVAL_MS
        assertFalse(isAutomaticCheckDue(UpdateSettings(), now))
        assertFalse(
            isAutomaticCheckDue(
                UpdateSettings(automaticChecksEnabled = true, lastCheckedAtMs = now - 1_000L),
                now,
            ),
        )
        assertTrue(
            isAutomaticCheckDue(
                UpdateSettings(
                    automaticChecksEnabled = true,
                    lastCheckedAtMs = now - UpdateRepository.CHECK_INTERVAL_MS,
                ),
                now,
            ),
        )
    }

    @Test
    fun checksumParsingAndHashingAreDeterministic() {
        val expected = "a".repeat(64)
        assertEquals(expected, "$expected  doorDuck-latest.apk".extractSha256())
        assertNull("not-a-checksum".extractSha256())

        val file = File.createTempFile("doorduck-update", ".apk")
        try {
            file.writeText("doorDuck")
            assertEquals("fabf2c8b4a25cbae172f652a36dc36a4cefc81bebc23bf7904bfdd382b04a95f", sha256(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun updateUrlsMustUseTrustedHttpsHosts() {
        requireTrustedReleaseUrl("https://github.com/vgy789/doorDuck/releases/download/v1.5.0/app.apk")
        requireTrustedReleaseUrl("https://objects.githubusercontent.com/github-production-release-asset/app.apk")
        assertThrows(IllegalArgumentException::class.java) {
            requireTrustedReleaseUrl("http://github.com/vgy789/doorDuck/app.apk")
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireTrustedReleaseUrl("https://github.com.evil.example/app.apk")
        }
    }

    @Test
    fun installationPreflightUsesFreshReleaseOrSilentFallback() {
        val cached = appRelease("v1.5.1")
        val fresh = appRelease("v1.5.2")

        assertEquals(
            fresh,
            selectReleaseForInstallation(UpdateCheckResult.Available(fresh), cached),
        )
        assertEquals(
            cached,
            selectReleaseForInstallation(UpdateCheckResult.Failed(IllegalStateException("offline")), cached),
        )
        assertNull(selectReleaseForInstallation(UpdateCheckResult.UpToDate, cached))
    }

    @Test
    fun incompatibleSignatureIsNotRetryableAsDownload() {
        assertEquals(
            UpdateMessage.APK_SIGNATURE_MISMATCH,
            ApkCompatibilityIssue.SIGNATURE_MISMATCH.toUpdateMessage(),
        )
        assertFalse(UpdateMessage.APK_SIGNATURE_MISMATCH.canRetryUpdate())
        assertFalse(UpdateMessage.APK_INCOMPATIBLE.canRetryUpdate())
        assertTrue(UpdateMessage.DOWNLOAD_FAILED.canRetryUpdate())
        assertTrue(UpdateMessage.DOWNLOAD_INTEGRITY_FAILED.canRetryUpdate())
    }

    private fun releaseDto(assets: List<GitHubReleaseAssetDto>) = GitHubReleaseDto(
        tagName = "v1.5.0",
        name = "doorDuck 1.5.0",
        body = "Changes",
        publishedAt = "2026-06-23T00:00:00Z",
        htmlUrl = "https://github.com/vgy789/doorDuck/releases/tag/v1.5.0",
        assets = assets,
    )

    private fun appRelease(tag: String) = AppRelease(
        tag = tag,
        title = tag,
        changelog = "",
        publishedAt = "",
        releaseUrl = "https://github.com/vgy789/doorDuck/releases/tag/$tag",
        apkUrl = "https://github.com/vgy789/doorDuck/releases/download/$tag/doorDuck-latest.apk",
        checksumUrl = "https://github.com/vgy789/doorDuck/releases/download/$tag/doorDuck-latest.apk.sha256",
    )
}
