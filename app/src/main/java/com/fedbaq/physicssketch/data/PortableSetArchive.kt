package com.fedbaq.physicssketch.data

import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.DrawingSet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object PortableSetArchive {
    private val json = Json {
        classDiscriminator = "type"
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun writeJsonEntries(set: DrawingSet, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            writeEntry(zip, "set.json", encodeSet(set))
            set.drawings.forEachIndexed { index, document ->
                writeEntry(zip, "drawings/${index + 1}.json", encodeDocument(document))
            }
        }
    }

    fun readSet(inputStream: InputStream): DrawingSet {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == "set.json") {
                    return json.decodeFromString(zip.readBytes().decodeToString())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("Archive does not contain set.json")
    }

    fun encodeSet(set: DrawingSet): String = json.encodeToString(set)

    fun encodeDocument(document: DrawingDocument): String = json.encodeToString(document)

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
