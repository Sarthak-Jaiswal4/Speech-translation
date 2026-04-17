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
                unzipIfNeeded(file)
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
        if (extractedDir.exists()) return
        extractedDir.mkdirs()

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
    }
}
