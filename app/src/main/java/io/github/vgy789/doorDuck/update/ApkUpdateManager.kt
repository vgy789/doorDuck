package io.github.vgy789.doorDuck.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.coroutineContext

sealed interface InstallResult {
    data object InstallerOpened : InstallResult
    data object PermissionRequired : InstallResult
    data class Incompatible(val issue: ApkCompatibilityIssue, val cause: Throwable) : InstallResult
    data class Failed(val cause: Throwable) : InstallResult
}

enum class ApkCompatibilityIssue {
    INVALID_APK,
    NOT_NEWER,
    SIGNATURE_MISMATCH,
}

sealed interface ApkDownloadResult {
    data class Ready(val file: File) : ApkDownloadResult
    data class DownloadFailed(val cause: Throwable) : ApkDownloadResult
    data class IntegrityFailed(val cause: Throwable) : ApkDownloadResult
    data class Incompatible(val issue: ApkCompatibilityIssue, val cause: Throwable) : ApkDownloadResult
}

private class ApkCompatibilityException(
    val issue: ApkCompatibilityIssue,
    message: String,
) : SecurityException(message)

class ApkUpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
) {
    private val updateDir = File(context.filesDir, "updates")
    private val readyApk = File(updateDir, "doorDuck-update.apk")

    fun cleanup() {
        updateDir.mkdirs()
        updateDir.listFiles().orEmpty().forEach { file ->
            if (file.name.endsWith(".part") || System.currentTimeMillis() - file.lastModified() > READY_APK_MAX_AGE_MS) {
                file.delete()
            }
        }
    }

    fun clear() {
        updateDir.deleteRecursively()
        updateDir.mkdirs()
    }

    fun hasReadyApk(): Boolean = readyApk.isFile

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    suspend fun downloadAndVerify(
        release: AppRelease,
        onProgress: (Int) -> Unit,
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        val tempApk = File(updateDir, "doorDuck-update.apk.part")
        try {
            requireTrustedReleaseUrl(release.apkUrl)
            requireTrustedReleaseUrl(release.checksumUrl)
            updateDir.mkdirs()
            tempApk.delete()
            readyApk.delete()

            val expectedChecksum = downloadText(release.checksumUrl).extractSha256()
                ?: return@withContext ApkDownloadResult.IntegrityFailed(
                    IOException("Release checksum is invalid"),
                )
            downloadFile(release.apkUrl, tempApk, onProgress)
            val actualChecksum = sha256(tempApk)
            if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                tempApk.delete()
                return@withContext ApkDownloadResult.IntegrityFailed(
                    SecurityException("Downloaded APK checksum does not match"),
                )
            }
            try {
                verifyApk(tempApk)
            } catch (error: ApkCompatibilityException) {
                tempApk.delete()
                return@withContext ApkDownloadResult.Incompatible(error.issue, error)
            }
            if (!tempApk.renameTo(readyApk)) throw IOException("Cannot finalize downloaded APK")
            onProgress(100)
            ApkDownloadResult.Ready(readyApk)
        } catch (cancelled: CancellationException) {
            tempApk.delete()
            readyApk.delete()
            throw cancelled
        } catch (throwable: Throwable) {
            tempApk.delete()
            readyApk.delete()
            ApkDownloadResult.DownloadFailed(throwable)
        }
    }

    fun cancelDownload() {
        File(updateDir, "doorDuck-update.apk.part").delete()
    }

    fun install(): InstallResult {
        if (!readyApk.isFile) return InstallResult.Failed(IOException("Downloaded APK is missing"))
        return try {
            verifyApk(readyApk)
            if (!canInstallPackages()) {
                return InstallResult.PermissionRequired
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", readyApk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            InstallResult.InstallerOpened
        } catch (error: ApkCompatibilityException) {
            InstallResult.Incompatible(error.issue, error)
        } catch (throwable: Throwable) {
            InstallResult.Failed(throwable)
        }
    }

    fun openInstallPermissionSettings(): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw UnsupportedOperationException("Install source permission is not required on this Android version")
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun downloadText(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "doorDuck updater").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Checksum HTTP ${response.code}")
            requireTrustedReleaseUrl(response.request.url.toString())
            val body = response.body ?: throw IOException("Checksum response is empty")
            if (body.contentLength() > MAX_CHECKSUM_BYTES) throw IOException("Checksum response is too large")
            body.string().also {
                if (it.length.toLong() > MAX_CHECKSUM_BYTES) throw IOException("Checksum response is too large")
            }
        }
    }

    private suspend fun downloadFile(url: String, target: File, onProgress: (Int) -> Unit) {
        val request = Request.Builder().url(url).header("User-Agent", "doorDuck updater").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("APK download HTTP ${response.code}")
            requireTrustedReleaseUrl(response.request.url.toString())
            val body = response.body ?: throw IOException("APK response is empty")
            val total = body.contentLength()
            if (total > MAX_APK_BYTES) throw IOException("APK is too large")
            body.byteStream().use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) throw IOException("APK is too large")
                        if (total > 0) {
                            val progress = ((downloaded * 100L) / total).toInt().coerceIn(0, 99)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
            if (target.length() <= 0L) throw IOException("Downloaded APK is empty")
        }
    }

    private fun verifyApk(file: File) {
        val packageManager = context.packageManager
        val archive = packageInfoForArchive(packageManager, file)
            ?: throw ApkCompatibilityException(ApkCompatibilityIssue.INVALID_APK, "Downloaded file is not an APK")
        val installed = packageInfoForInstalled(packageManager, context.packageName)
        if (archive.packageName != context.packageName) {
            throw ApkCompatibilityException(ApkCompatibilityIssue.INVALID_APK, "APK package name does not match")
        }
        if (archive.longVersionCodeCompat() <= installed.longVersionCodeCompat()) {
            throw ApkCompatibilityException(ApkCompatibilityIssue.NOT_NEWER, "APK version is not newer than installed version")
        }
        if (signerDigests(archive) != signerDigests(installed) || signerDigests(archive).isEmpty()) {
            throw ApkCompatibilityException(ApkCompatibilityIssue.SIGNATURE_MISMATCH, "APK signing certificate does not match")
        }
    }

    @Suppress("DEPRECATION")
    private fun packageInfoForArchive(packageManager: PackageManager, file: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    private fun packageInfoForInstalled(packageManager: PackageManager, packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            info.signatures
        }
        return signatures.orEmpty().mapTo(mutableSetOf()) { sha256(it.toByteArray()) }
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val READY_APK_MAX_AGE_MS = 24L * 60L * 60L * 1_000L
        private const val MAX_APK_BYTES = 100L * 1024L * 1024L
        private const val MAX_CHECKSUM_BYTES = 16L * 1024L
    }
}

internal fun requireTrustedReleaseUrl(value: String) {
    val uri = runCatching { URI(value) }.getOrNull()
        ?: throw IllegalArgumentException("Invalid update URL")
    val host = uri.host?.lowercase(Locale.US).orEmpty()
    val trustedHost = host == "github.com" || host.endsWith(".githubusercontent.com")
    require(uri.scheme.equals("https", ignoreCase = true) && trustedHost) { "Untrusted update URL" }
}

internal fun String.extractSha256(): String? =
    Regex("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])").find(this)?.value

internal fun sha256(file: File): String = file.inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().toHex()
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
