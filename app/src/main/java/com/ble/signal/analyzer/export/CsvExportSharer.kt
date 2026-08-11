package com.ble.signal.analyzer.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes a temporary UTF-8 CSV file and delegates its destination to Android's share sheet. */
class CsvExportSharer(private val context: Context) {
    fun share(document: CsvExportDocument, chooserTitle: String, nowMillis: Long) {
        val exportDirectory = File(context.cacheDir, EXPORT_DIRECTORY)
        check(exportDirectory.exists() || exportDirectory.mkdirs()) {
            "Unable to create export cache directory"
        }
        deleteExpiredExports(exportDirectory, nowMillis)

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

    private fun deleteExpiredExports(exportDirectory: File, nowMillis: Long) {
        exportDirectory.listFiles()
            ?.filter { file ->
                file.isFile && nowMillis - file.lastModified() >= MAX_EXPORT_AGE_MILLIS
            }
            ?.forEach(File::delete)
    }

    private companion object {
        const val EXPORT_DIRECTORY = "export"
        const val CSV_MIME_TYPE = "text/csv"
        const val MAX_EXPORT_AGE_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
