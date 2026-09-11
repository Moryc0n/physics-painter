package com.fedbaq.physicssketch.core.render

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PngExporter {
    fun writePng(bitmap: Bitmap, outputStream: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }

    fun sharePng(context: Context, bitmap: Bitmap, filename: String) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, filename)
        FileOutputStream(file).use { stream ->
            writePng(bitmap, stream)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Toast.makeText(context, "PNG готов", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent.createChooser(intent, "Экспорт PNG"))
    }
}
