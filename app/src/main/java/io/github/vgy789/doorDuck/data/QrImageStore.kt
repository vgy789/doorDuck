package io.github.vgy789.doorDuck.data

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import java.io.IOException
import java.util.UUID

class QrImageStore(context: Context) {
    private val storageDir = File(context.filesDir, "qr_images").apply { mkdirs() }

    fun stageCandidate(imageBytes: ByteArray): String {
        val candidateName = "qr_${System.currentTimeMillis()}_${UUID.randomUUID()}.png"
        val candidateFile = File(storageDir, candidateName)
        val temp = File(storageDir, "$candidateName.tmp")
        temp.outputStream().use { out ->
            out.write(imageBytes)
            out.flush()
        }
        if (!temp.renameTo(candidateFile)) {
            temp.delete()
            throw IOException("Failed to promote QR candidate file")
        }
        return candidateFile.absolutePath
    }

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    fun deleteAllExcept(activePath: String?) {
        storageDir.listFiles().orEmpty().forEach { file ->
            if (file.absolutePath != activePath) {
                runCatching { file.deleteRecursively() }
            }
        }
    }

    fun isValidImage(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val bitmap = BitmapFactory.decodeFile(path) ?: return false
        val isValid = bitmap.width > 0 && bitmap.height > 0
        bitmap.recycle()
        return isValid
    }

    fun clear() {
        storageDir.deleteRecursively()
        storageDir.mkdirs()
    }
}
