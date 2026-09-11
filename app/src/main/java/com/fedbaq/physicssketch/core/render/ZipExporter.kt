package com.fedbaq.physicssketch.core.render

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fedbaq.physicssketch.core.model.DrawingSet
import com.fedbaq.physicssketch.data.PortableSetArchive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter {
    fun writeSetArchive(context: Context, set: DrawingSet, outputStream: OutputStream) {
        val renderer = DrawingRenderer(context)

        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("set.json"))
            zip.write(PortableSetArchive.encodeSet(set).toByteArray())
            zip.closeEntry()

            set.drawings.forEachIndexed { index, document ->
                val number = index + 1
                zip.putNextEntry(ZipEntry("drawings/$number.json"))
                zip.write(PortableSetArchive.encodeDocument(document).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("png/$number-no-background.png"))
                val bitmap = renderer.exportBitmap(document, 2400, 1600, includeWhiteBackground = false)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, zip)
                zip.closeEntry()
            }
        }
    }

    fun shareSet(context: Context, set: DrawingSet) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "${set.title.sanitizedFilename()}.zip")
        FileOutputStream(file).use { output ->
            writeSetArchive(context, set, output)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Toast.makeText(context, "ZIP готов", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent.createChooser(intent, "Экспорт ZIP"))
    }

    private fun String.sanitizedFilename(): String {
        return lowercase()
            .replace(Regex("[^a-zа-я0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "physics_sketch_set" }
    }
}
