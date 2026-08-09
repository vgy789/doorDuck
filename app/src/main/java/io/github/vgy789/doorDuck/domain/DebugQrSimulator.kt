package io.github.vgy789.doorDuck.domain

import android.graphics.Bitmap
import io.github.vgy789.doorDuck.AppContainer
import io.github.vgy789.doorDuck.model.QrImageValidationStatus
import java.io.ByteArrayOutputStream

/**
 * Creates a local QR-shaped image for debug builds when the Rocket.Chat service
 * is unavailable. It deliberately does not create credentials or pretend that
 * the account is configured; it only exercises the image and presentation path.
 */
object DebugQrSimulator {
    private const val QR_SIZE_MODULES = 37
    private const val MODULE_SIZE_PX = 8
    private const val EXPIRATION_MS = 15L * 60L * 1_000L

    suspend fun install(container: AppContainer, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!container.settingsStore.tryStartSync(nowMs)) return false
        return try {
            val path = container.imageStore.stageCandidate(createImage(nowMs))
            val previousPath = container.settingsStore.getSnapshot().localImagePath
            val expiresAtMs = nowMs + EXPIRATION_MS
            container.settingsStore.saveSyncSuccess(
                path = path,
                receivedAtMs = nowMs,
                expiresAtMs = expiresAtMs,
                nextAutoRefreshAtMs = expiresAtMs,
                imageValidationStatus = QrImageValidationStatus.VALID,
            )
            container.imageStore.deleteIfExists(previousPath)
            container.imageStore.deleteAllExcept(path)
            container.widgetUpdateCoordinator.forceWidgetUpdateNow()
            true
        } finally {
            container.settingsStore.clearInProgress()
        }
    }

    private fun createImage(seed: Long): ByteArray {
        val dimension = QR_SIZE_MODULES * MODULE_SIZE_PX
        val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF111111.toInt()
        for (y in 0 until dimension) {
            for (x in 0 until dimension) bitmap.setPixel(x, y, white)
        }

        fun module(x: Int, y: Int, value: Boolean) {
            if (!value) return
            val left = x * MODULE_SIZE_PX
            val top = y * MODULE_SIZE_PX
            for (dy in 0 until MODULE_SIZE_PX) {
                for (dx in 0 until MODULE_SIZE_PX) bitmap.setPixel(left + dx, top + dy, black)
            }
        }

        fun finder(left: Int, top: Int) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    module(left + x, top + y, x == 0 || y == 0 || x == 6 || y == 6 || (x in 2..4 && y in 2..4))
                }
            }
        }

        finder(4, 4)
        finder(QR_SIZE_MODULES - 11, 4)
        finder(4, QR_SIZE_MODULES - 11)

        var state = seed xor -7046029254386353131L
        for (y in 0 until QR_SIZE_MODULES) {
            for (x in 0 until QR_SIZE_MODULES) {
                val inFinder = (x in 4..10 && y in 4..10) ||
                    (x in QR_SIZE_MODULES - 11..QR_SIZE_MODULES - 5 && y in 4..10) ||
                    (x in 4..10 && y in QR_SIZE_MODULES - 11..QR_SIZE_MODULES - 5)
                if (!inFinder) {
                    state = state * 6364136223846793005L + 1442695040888963407L
                    module(x, y, (state ushr 61) and 1L == 1L)
                }
            }
        }

        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
