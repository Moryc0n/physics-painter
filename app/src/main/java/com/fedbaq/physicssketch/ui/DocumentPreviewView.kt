package com.fedbaq.physicssketch.ui

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.render.DrawingRenderer

class DocumentPreviewView(context: Context) : View(context) {
    var document: DrawingDocument = DrawingDocument("preview", "preview")
        set(value) {
            field = value
            invalidate()
        }

    private val renderer = DrawingRenderer(context)

    override fun onDraw(canvas: Canvas) {
        val bounds = document.objects.map { it.bounds() }
        val contentRight = bounds.maxOfOrNull { it.right }?.coerceAtLeast(1f) ?: width.toFloat()
        val contentBottom = bounds.maxOfOrNull { it.bottom }?.coerceAtLeast(1f) ?: height.toFloat()
        val scale = minOf(width / contentRight, height / contentBottom, 0.5f).coerceAtLeast(0.1f)

        canvas.save()
        canvas.scale(scale, scale)
        renderer.render(canvas, document, selectedObjectId = null, includeWhiteBackground = true)
        canvas.restore()
    }
}
