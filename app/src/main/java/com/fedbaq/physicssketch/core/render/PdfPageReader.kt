package com.fedbaq.physicssketch.core.render

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri

object PdfPageReader {
    fun pageCount(context: Context, uri: Uri): Int {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.pageCount
            }
        } ?: 0
    }
}
