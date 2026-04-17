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
        assets.forEachIndexed { index, asset ->
            val file = File(modelsRoot, asset.fileName)
            if (!file.exists()) {
                download(asset, file)
            }

            if (asset.fileName.endsWith(".zip")) {
                validateZipHeader(file)
                unzipIfNeeded(file)
                val extracted = File(modelsRoot, file.nameWithoutExtension)
                if (findVoskModelRoot(extracted) == null) {
                    throw IllegalStateException(
                        "Model extraction failed or archive is corrupt. Clear app data or delete ${file.name} and retry."
                    )
                }
            }

            val progress = ((index + 1).toFloat() / assets.size * 100).toInt()
            onProgress(progress, "Ready: ${asset.id}")
        }
        modelsRoot
    }

    private fun download(asset: ModelAsset, destination: File) {
        val request = Request.Builder().url(asset.url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed for ${asset.id}: ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty response body")
            FileOutputStream(destination).use { output ->
                body.byteStream().copyTo(output)
            }
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
