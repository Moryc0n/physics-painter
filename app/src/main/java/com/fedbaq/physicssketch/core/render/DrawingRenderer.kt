package com.fedbaq.physicssketch.core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.fedbaq.physicssketch.core.model.AngleMarkerObject
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.CircleObject
import com.fedbaq.physicssketch.core.model.DrawableObject
import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.FreehandStroke
import com.fedbaq.physicssketch.core.model.ImagePageObject
import com.fedbaq.physicssketch.core.model.LineObject
import com.fedbaq.physicssketch.core.model.RectObject
import com.fedbaq.physicssketch.core.model.TextObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DrawingRenderer(
    private val context: Context? = null,
) {
    private val imageCache = mutableMapOf<String, Bitmap?>()

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 5f
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 64f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }

    fun render(
        canvas: Canvas,
        document: DrawingDocument,
        selectedObjectId: String? = null,
        includeWhiteBackground: Boolean = true,
        includeImportedPages: Boolean = true,
    ) {
        if (includeWhiteBackground) {
            canvas.drawColor(Color.WHITE)
        }
        document.objects
            .filter { includeImportedPages || it !is ImagePageObject }
            .forEach { drawObject(canvas, it) }
        document.objects.firstOrNull { it.id == selectedObjectId }?.let { selected ->
            drawSelection(canvas, selected)
        }
    }

    fun exportBitmap(document: DrawingDocument, width: Int, height: Int, includeWhiteBackground: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        render(
            canvas = canvas,
            document = document,
            selectedObjectId = null,
            includeWhiteBackground = includeWhiteBackground,
            includeImportedPages = includeWhiteBackground,
        )
        return bitmap
    }

    private fun drawObject(canvas: Canvas, item: DrawableObject) {
        when (item) {
            is FreehandStroke -> drawStroke(canvas, item)
            is ArrowObject -> drawArrow(canvas, item)
            is LineObject -> canvas.drawLine(item.start.x, item.start.y, item.end.x, item.end.y, strokePaint)
            is CircleObject -> {
                val b = item.bounds()
                drawRotated(canvas, item.rotationDegrees, b.center.x, b.center.y) {
                    canvas.drawOval(RectF(b.left, b.top, b.right, b.bottom), strokePaint)
                }
            }
            is RectObject -> {
                val b = item.bounds()
                drawRotated(canvas, item.rotationDegrees, b.center.x, b.center.y) {
                    canvas.drawRect(RectF(b.left, b.top, b.right, b.bottom), strokePaint)
                }
            }
            is TextObject -> {
                textPaint.typeface = Typeface.create(Typeface.SERIF, if (item.italic) Typeface.ITALIC else Typeface.NORMAL)
                val b = item.bounds()
                drawRotated(canvas, item.rotationDegrees, b.center.x, b.center.y) {
                    drawFormattedText(canvas, item)
                }
            }
            is AngleMarkerObject -> {
                val b = item.bounds()
                canvas.drawArc(RectF(b.left, b.top, b.right, b.bottom), item.startAngleDegrees, item.sweepDegrees, false, strokePaint)
            }
            is ImagePageObject -> {
                val b = item.bounds()
                val rect = RectF(b.left, b.top, b.right, b.bottom)
                val bitmap = loadBitmap(item)
                if (bitmap == null) {
                    drawRotated(canvas, item.rotationDegrees, b.center.x, b.center.y) {
                        canvas.drawRect(rect, strokePaint)
                    }
                } else {
                    drawRotated(canvas, item.rotationDegrees, b.center.x, b.center.y) {
                        canvas.drawBitmap(bitmap, null, rect, null)
                    }
                }
            }
        }
    }

    private fun drawSelection(canvas: Canvas, selected: DrawableObject) {
        val b = selected.bounds()
        val rotationDegrees = when (selected) {
            is CircleObject -> selected.rotationDegrees
            is RectObject -> selected.rotationDegrees
            is TextObject -> selected.rotationDegrees
            is ImagePageObject -> selected.rotationDegrees
            else -> 0f
        }
        drawRotated(canvas, rotationDegrees, b.center.x, b.center.y) {
            canvas.drawRect(RectF(b.left, b.top, b.right, b.bottom), selectionPaint)
        }
    }

    private inline fun drawRotated(canvas: Canvas, degrees: Float, pivotX: Float, pivotY: Float, draw: () -> Unit) {
        if (degrees == 0f) {
            draw()
            return
        }
        canvas.save()
        canvas.rotate(degrees, pivotX, pivotY)
        draw()
        canvas.restore()
    }

    private fun drawStroke(canvas: Canvas, stroke: FreehandStroke) {
        if (stroke.points.size < 2) return
        strokePaint.strokeWidth = stroke.strokeWidth
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, strokePaint)
        strokePaint.strokeWidth = 5f
    }

    private fun drawArrow(canvas: Canvas, arrow: ArrowObject) {
        canvas.drawLine(arrow.start.x, arrow.start.y, arrow.end.x, arrow.end.y, strokePaint)
        val angle = atan2((arrow.end.y - arrow.start.y).toDouble(), (arrow.end.x - arrow.start.x).toDouble())
        val headLength = 28.0
        val first = angle + Math.toRadians(150.0)
        val second = angle - Math.toRadians(150.0)
        canvas.drawLine(
            arrow.end.x,
            arrow.end.y,
            (arrow.end.x + cos(first) * headLength).toFloat(),
            (arrow.end.y + sin(first) * headLength).toFloat(),
            strokePaint,
        )
        canvas.drawLine(
            arrow.end.x,
            arrow.end.y,
            (arrow.end.x + cos(second) * headLength).toFloat(),
            (arrow.end.y + sin(second) * headLength).toFloat(),
            strokePaint,
        )
    }

    private fun drawFormattedText(canvas: Canvas, item: TextObject) {
        val baseSize = item.sizePx
        val scriptSize = baseSize * 0.6f
        var x = item.position.x
        val baseY = item.position.y
        val segments = parseTextSegments(item.text)

        segments.forEach { segment ->
            val isScript = segment.style != TextSegmentStyle.Normal
            textPaint.textSize = if (isScript) scriptSize else baseSize
            val y = when (segment.style) {
                TextSegmentStyle.Normal -> baseY
                TextSegmentStyle.Subscript -> baseY + baseSize * 0.32f
                TextSegmentStyle.Superscript -> baseY - baseSize * 0.48f
            }
            canvas.drawText(segment.value, x, y, textPaint)
            x += textPaint.measureText(segment.value)
        }
        if (item.vector && item.text.isNotBlank()) {
            drawVectorMark(canvas, item, measureFormattedTextWidth(segments, baseSize, scriptSize))
        }
        textPaint.textSize = 64f
    }

    private fun measureFormattedTextWidth(segments: List<TextSegment>, baseSize: Float, scriptSize: Float): Float {
        var width = 0f
        segments.forEach { segment ->
            textPaint.textSize = if (segment.style == TextSegmentStyle.Normal) baseSize else scriptSize
            width += textPaint.measureText(segment.value)
        }
        return width
    }

    private fun drawVectorMark(canvas: Canvas, item: TextObject, textWidth: Float) {
        val startX = item.position.x
        val endX = item.position.x + textWidth.coerceAtLeast(item.sizePx * 0.45f)
        val y = item.position.y - item.sizePx * 1.05f
        val headLength = item.sizePx * 0.18f
        val headHeight = item.sizePx * 0.12f

        canvas.drawLine(startX, y, endX, y, strokePaint)
        canvas.drawLine(endX, y, endX - headLength, y - headHeight, strokePaint)
        canvas.drawLine(endX, y, endX - headLength, y + headHeight, strokePaint)
    }

    private fun parseTextSegments(text: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var index = 0
        while (index < text.length) {
            val marker = text[index]
            if ((marker == '_' || marker == '^') && index + 1 < text.length) {
                val start = index + 1
                var end = start
                while (end < text.length && text[end] != '_' && text[end] != '^') {
                    end++
                }
                segments += TextSegment(
                    value = text.substring(start, end),
                    style = if (marker == '_') TextSegmentStyle.Subscript else TextSegmentStyle.Superscript,
                )
                index = end
            } else {
                val start = index
                var end = index
                while (end < text.length && text[end] != '_' && text[end] != '^') {
                    end++
                }
                if (end == start) {
                    end++
                }
                segments += TextSegment(text.substring(start, end), TextSegmentStyle.Normal)
                index = end
            }
        }
        return segments
    }

    private fun loadBitmap(item: ImagePageObject): Bitmap? {
        val appContext = context ?: return null
        val cacheKey = "${item.sourceUri}#${item.pageIndex}"
        return imageCache.getOrPut(cacheKey) {
            runCatching {
                val uri = Uri.parse(item.sourceUri)
                val mimeType = appContext.contentResolver.getType(uri).orEmpty()
                if (mimeType == "application/pdf" || item.sourceUri.endsWith(".pdf", ignoreCase = true)) {
                    appContext.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            val pageIndex = item.pageIndex.coerceIn(0, renderer.pageCount - 1)
                            val page = renderer.openPage(pageIndex)
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            bitmap
                        }
                    }
                } else {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }.getOrNull()
        }
    }

    private data class TextSegment(
        val value: String,
        val style: TextSegmentStyle,
    )

    private enum class TextSegmentStyle {
        Normal,
        Subscript,
        Superscript,
    }
}
