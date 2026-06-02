package io.github.vgy789.doorDuck.data

import android.content.Context
import java.io.File

class QrImageStore(context: Context) {
    private val storageDir = File(context.filesDir, "qr_images").apply { mkdirs() }
    private val targetFile = File(storageDir, "latest_qr.png")

    fun save(imageBytes: ByteArray): String {
        val temp = File(storageDir, "latest_qr.tmp")
        temp.outputStream().use { out ->
            out.write(imageBytes)
            out.flush()
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
        temp.renameTo(targetFile)
        return targetFile.absolutePath
    }

    fun clear() {
        storageDir.deleteRecursively()
        storageDir.mkdirs()
    }
}
