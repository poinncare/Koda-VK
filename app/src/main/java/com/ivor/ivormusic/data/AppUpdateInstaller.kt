package com.ivor.ivormusic.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Downloads a GitHub release APK, verifies it, and hands it to Android's installer. */
class AppUpdateInstaller(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun download(asset: ApkAsset, onProgress: (Int?) -> Unit): File =
        withContext(Dispatchers.IO) {
            val source = asset.downloadUrl.toHttpUrlOrNull()
                ?: throw IOException("Invalid update URL")
            if (!source.isHttps || source.host != "github.com") {
                throw IOException("Update must come from GitHub")
            }

            val safeName = asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .takeIf { it.endsWith(".apk", ignoreCase = true) }
                ?: "koda-update.apk"
            val updateDir = File(appContext.cacheDir, "updates")
            if (!updateDir.exists() && !updateDir.mkdirs()) {
                throw IOException("Cannot create update directory")
            }
            val destination = File(updateDir, safeName)
            val partial = File(updateDir, "$safeName.part")

            val request = Request.Builder()
                .url(source)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Koda-VK-Updater")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("GitHub returned HTTP ${response.code}")
                    val body = response.body ?: throw IOException("GitHub returned an empty APK")
                    val expectedSize = asset.size.takeIf { it > 0 } ?: body.contentLength().takeIf { it > 0 }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloaded = 0L
                    partial.outputStream().buffered().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                downloaded += read
                                onProgress(expectedSize?.let { ((downloaded * 100L) / it).toInt().coerceIn(0, 100) })
                            }
                        }
                    }

                    if (expectedSize != null && downloaded != expectedSize) {
                        throw IOException("Downloaded APK size does not match the release")
                    }
                    asset.digest?.removePrefix("sha256:")?.takeIf { it.length == 64 }?.let { expected ->
                        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                        if (!actual.equals(expected, ignoreCase = true)) {
                            throw IOException("Downloaded APK checksum does not match GitHub")
                        }
                    }
                }

                if (destination.exists() && !destination.delete()) {
                    throw IOException("Cannot replace the previous update")
                }
                if (!partial.renameTo(destination)) throw IOException("Cannot finalize the downloaded update")
                onProgress(100)
                destination
            } catch (error: Throwable) {
                partial.delete()
                throw error
            }
        }

    fun launchInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}
