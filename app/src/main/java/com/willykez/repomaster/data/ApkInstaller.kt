package com.willykez.repomaster.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Turns a downloaded GitHub Actions artifact (always a zip — GitHub wraps every artifact,
 * even a single file, in one) into a system install prompt.
 *
 * The extracted APK is written to its own cache subfolder (`apk-installs/`) rather than
 * anywhere under the app's public repo storage — this is disposable install media, not
 * something that belongs in a repo, and keeping it in one dedicated subfolder is what lets
 * the [FileProvider] declaration in the manifest grant read access to *just* that folder
 * instead of the whole cache dir.
 */
object ApkInstaller {

    sealed class Result {
        data class Ready(val installIntent: Intent) : Result()
        object NoApkInArchive : Result()
        data class Failed(val message: String) : Result()
    }

    private const val CACHE_SUBFOLDER = "apk-installs"

    suspend fun extractAndBuildInstallIntent(
        context: Context,
        zipBytes: ByteArray,
        artifactName: String,
    ): Result = withContext(Dispatchers.IO) {
        try {
            val outDir = File(context.cacheDir, CACHE_SUBFOLDER)
            // Clear anything left from a previous install so this folder never accumulates
            // stale APKs across runs — it only ever needs to hold the one being installed now.
            outDir.deleteRecursively()
            outDir.mkdirs()

            val apkFile = File(outDir, "${artifactName.ifBlank { "build" }}.apk")
            var found = false

            ZipInputStream(zipBytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        apkFile.outputStream().use { out -> zip.copyTo(out) }
                        found = true
                        break
                    }
                    entry = zip.nextEntry
                }
            }

            if (!found) return@withContext Result.NoApkInArchive

            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Result.Ready(intent)
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Couldn't extract the APK from this build output")
        }
    }
}
