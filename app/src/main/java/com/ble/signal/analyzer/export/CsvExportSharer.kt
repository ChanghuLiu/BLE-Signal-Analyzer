package com.ble.signal.analyzer.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes a temporary UTF-8 CSV file and delegates its destination to Android's share sheet. */
class CsvExportSharer(private val context: Context) {
    fun cleanupExpiredExports(nowMillis: Long) {
        val exportDirectory = File(context.cacheDir, EXPORT_DIRECTORY)
        if (exportDirectory.isDirectory) {
            cachedExportFiles(exportDirectory)
                .filter { nowMillis - it.lastModified() >= MAX_EXPORT_AGE_MILLIS }
                .forEach(File::delete)
        }
    }

    fun countCachedExports(): Int = cachedExportFiles(exportDirectory()).size

    fun clearCachedExports(): Int = cachedExportFiles(exportDirectory())
        .count(File::delete)

    fun share(document: CsvExportDocument, chooserTitle: String, nowMillis: Long) {
        val exportDirectory = exportDirectory()
        check(exportDirectory.exists() || exportDirectory.mkdirs()) {
            "Unable to create export cache directory"
        }
        cleanupExpiredExports(nowMillis)

        val exportFile = File(
            exportDirectory,
            CsvExportFileName.create(document.type, nowMillis),
        )
        exportFile.writeText(document.content, Charsets.UTF_8)
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.export-file-provider",
            exportFile,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = CSV_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newUri(context.contentResolver, exportFile.name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }

    private fun exportDirectory() = File(context.cacheDir, EXPORT_DIRECTORY)

    private fun cachedExportFiles(exportDirectory: File): List<File> =
        exportDirectory.listFiles()
            ?.filter { it.isFile && it.extension.equals("csv", ignoreCase = true) }
            .orEmpty()

    private companion object {
        const val EXPORT_DIRECTORY = "export"
        const val CSV_MIME_TYPE = "text/csv"
        const val MAX_EXPORT_AGE_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
