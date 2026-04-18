package com.assignment.ondevicetranslation.data.download

import android.content.Context
import com.assignment.ondevicetranslation.data.model.ModelAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ModelDownloadManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val modelsRoot: File = File(context.filesDir, "models").apply { mkdirs() }

    companion object {
        fun findVoskModelRoot(searchRoot: File): File? {
            if (!searchRoot.exists()) return null
            if (isVoskModelDir(searchRoot)) return searchRoot
            return searchRoot.walkTopDown().maxDepth(8).firstOrNull { isVoskModelDir(it) }
        }

        private fun isVoskModelDir(dir: File): Boolean {
            if (!dir.isDirectory) return false
            return File(dir, "am").isDirectory && File(dir, "conf").isDirectory
        }
    }

    suspend fun ensureAssets(
        assets: List<ModelAsset>,
        onProgress: (Int, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (assets.isEmpty()) return@withContext modelsRoot
        assets.forEachIndexed { index, asset ->
            val file = File(modelsRoot, asset.fileName)
            val rangeStart = (index.toFloat() / assets.size * 100f)
            val rangeEnd = ((index + 1).toFloat() / assets.size * 100f)
            if (!file.exists()) {
                download(asset, file) { fraction ->
                    val progress = (rangeStart + (rangeEnd - rangeStart) * fraction)
                        .toInt()
                        .coerceIn(0, 100)
                    onProgress(progress, "Downloading ${asset.id}... $progress%")
                }
            }

            if (asset.fileName.endsWith(".zip")) {
                ensureValidZip(asset, file)
                val extracted = File(modelsRoot, file.nameWithoutExtension)
                if (findVoskModelRoot(extracted) == null) {
                    throw IllegalStateException(
                        "Model extraction failed or archive is corrupt. Clear app data or delete ${file.name} and retry."
                    )
                }
            }

            val progress = rangeEnd.toInt().coerceIn(0, 100)
            onProgress(progress, "Ready: ${asset.id}")
        }
        modelsRoot
    }

    private fun ensureValidZip(asset: ModelAsset, file: File) {
        var attempts = 0
        var lastError: Exception? = null
        while (attempts < 2) {
            attempts++
            try {
                validateZipHeader(file)
                unzipIfNeeded(file)
                return
            } catch (e: Exception) {
                lastError = e
                file.delete()
                File(modelsRoot, file.nameWithoutExtension).deleteRecursively()
                if (attempts < 2) {
                    download(asset, file) { }
                }
            }
        }
        throw IllegalStateException(
            "Failed to prepare ${asset.fileName} after retry: ${lastError?.message}",
            lastError
        )
    }

    private fun download(asset: ModelAsset, destination: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(asset.url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed for ${asset.id}: ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty response body")
            val totalBytes = body.contentLength().takeIf { it > 0L }
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var bytesWritten = 0L
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        output.write(buffer, 0, bytesRead)
                        bytesWritten += bytesRead
                        if (totalBytes != null) {
                            onProgress((bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            onProgress(1f)
        }
    }

    private fun unzipIfNeeded(archive: File) {
        val extractedDir = File(modelsRoot, archive.nameWithoutExtension)
        val existingValid = extractedDir.exists() && findVoskModelRoot(extractedDir) != null
        if (existingValid) return
        if (extractedDir.exists()) {
            extractedDir.deleteRecursively()
        }
        extractedDir.mkdirs()

        try {
            ZipInputStream(archive.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outputFile = File(extractedDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            extractedDir.deleteRecursively()
            throw IllegalStateException("Failed to unzip ${archive.name}: ${e.message}", e)
        }
    }

    private fun validateZipHeader(file: File) {
        if (file.length() < 22L) {
            file.delete()
            throw IllegalStateException("Model download incomplete (${file.name}). Check network and try again.")
        }
        file.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != header.size) {
                file.delete()
                throw IllegalStateException("Cannot read model file ${file.name}")
            }
            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4b.toByte()
            if (!isZip) {
                file.delete()
                throw IllegalStateException(
                    "Downloaded file is not a valid zip (blocked or error page). Deleted ${file.name}; retry download."
                )
            }
        }
    }
}
