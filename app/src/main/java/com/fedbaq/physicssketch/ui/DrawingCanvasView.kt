package com.fedbaq.physicssketch.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.fedbaq.physicssketch.core.geometry.Geometry
import com.fedbaq.physicssketch.core.geometry.SketchBounds
import com.fedbaq.physicssketch.core.geometry.SketchPoint
import com.fedbaq.physicssketch.core.model.AngleMarkerObject
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.CircleObject
import com.fedbaq.physicssketch.core.model.DrawableObject
import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.DrawingHistory
import com.fedbaq.physicssketch.core.model.FreehandStroke
import com.fedbaq.physicssketch.core.model.ImagePageObject
import com.fedbaq.physicssketch.core.model.LineObject
import com.fedbaq.physicssketch.core.model.RectObject
import com.fedbaq.physicssketch.core.model.TextObject
import com.fedbaq.physicssketch.core.model.rotatedBy
import com.fedbaq.physicssketch.core.render.DrawingRenderer
import com.fedbaq.physicssketch.core.tools.DrawingTool
import com.fedbaq.physicssketch.core.tools.ShapeRegistry
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class DrawingCanvasView(context: Context) : View(context) {
    var document: DrawingDocument = DrawingDocument("demo", "Новый рисунок")
        set(value) {
            if (field.id != value.id) {
                history = DrawingHistory(value)
                selectedObjectId = null
                draftShape = null
                isRotatingSelection = false
                rotationCenter = null
                onObjectSelected(null)
                onObjectActionBarVisibleChanged(false)
                onHistoryChanged(false, false)
            }
            field = value
            invalidate()
        }

    var activeTool: DrawingTool = DrawingTool.Pen
    var activeShapeId: String = ShapeRegistry.ARROW
    var textValue: String = "α"
    var italicText: Boolean = true
    var vectorText: Boolean = false
    var onDocumentChanged: (DrawingDocument) -> Unit = {}
    var onObjectSelected: (DrawableObject?) -> Unit = {}
    var onObjectActionBarVisibleChanged: (Boolean) -> Unit = {}
    var onHistoryChanged: (canUndo: Boolean, canRedo: Boolean) -> Unit = { _, _ -> }
    var eraserRadius: Float = 30f
    var penStrokeWidth: Float = 5f

    private val renderer = DrawingRenderer(context)
    private var history = DrawingHistory(document)
    private var selectedObjectId: String? = null
    private var dragStart: SketchPoint? = null
    private var lastMovePoint: SketchPoint? = null
    private var moveStartDocument: DrawingDocument? = null
    private var draftStroke: MutableList<SketchPoint>? = null
    private var draftShape: DrawableObject? = null
    private var isMovingSelection = false
    private var isResizingSelection = false
    private var resizeHandle: ResizeHandle? = null
    private var resizeStartDocument: DrawingDocument? = null
    private var resizeStartObject: DrawableObject? = null
    private var isRotatingSelection = false
    private var rotationStartDocument: DrawingDocument? = null
    private var rotationLastAngleDegrees: Float? = null
    private var rotationCenter: SketchPoint? = null
    private val angleSelection = mutableListOf<Pair<SketchPoint, SketchPoint>>()

    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val handleArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val rotationWheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 25, 118, 210)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val rotationWheelTouchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 25, 118, 210)
        style = Paint.Style.FILL
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val point = e.toSketchPoint()
            val selected = findTopObject(point) ?: return
            selectedObjectId = selected.id
            lastMovePoint = point
            moveStartDocument = document
            draftStroke = null
            draftShape = null
            isRotatingSelection = false
            rotationCenter = null
            isMovingSelection = true
            onObjectSelected(selected)
            onObjectActionBarVisibleChanged(false)
            invalidate()
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.toSketchPoint())
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        renderer.render(canvas, document, selectedObjectId, includeWhiteBackground = true)
        draftStroke?.let { points ->
            renderer.render(canvas, DrawingDocument("draft", "draft", listOf(FreehandStroke("draft", points, penStrokeWidth))), null, false)
        }
        draftShape?.let { shape ->
            renderer.render(canvas, DrawingDocument("draftShape", "draftShape", listOf(shape)), null, false)
        }
        document.objects.firstOrNull { it.id == selectedObjectId }?.let { selected ->
            if (isRotatingSelection) {
                drawRotationWheel(canvas, selected)
            }
            if (!isMovingSelection && !isResizingSelection && !isRotatingSelection) {
                drawResizeHandles(canvas, selected)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = event.toSketchPoint()
        if (isRotatingSelection) {
            handleRotationEvent(event, point)
            return true
        }

        if (isResizingSelection) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> resizeSelectedTo(point)
                MotionEvent.ACTION_UP -> finishResize(canceled = false)
                MotionEvent.ACTION_CANCEL -> finishResize(canceled = true)
            }
            return true
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN && beginResizeIfNeeded(point)) {
            return true
        }

        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStart = point
                when (activeTool) {
                    DrawingTool.Pen -> draftStroke = mutableListOf(point)
                    DrawingTool.Eraser -> eraseAt(point)
                    DrawingTool.Shape -> draftShape = null
                    else -> Unit
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isMovingSelection) {
                    moveSelectedTo(point)
                } else if (activeTool == DrawingTool.Eraser) {
                    eraseAt(point)
                } else if (activeTool == DrawingTool.Shape) {
                    updateDraftShape(point)
                } else {
                    draftStroke?.add(point)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                finishGesture(point, canceled = false)
            }
            MotionEvent.ACTION_CANCEL -> {
                finishGesture(point, canceled = true)
            }
        }
        return true
    }

    fun deleteSelected() {
        val id = selectedObjectId ?: return
        updateDocument(document.removeObject(id))
        selectedObjectId = null
        isRotatingSelection = false
        rotationCenter = null
        onObjectSelected(null)
        onObjectActionBarVisibleChanged(false)
    }

    fun duplicateSelected() {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } ?: return
        updateDocument(document.addObject(selected.duplicate(UUID.randomUUID().toString())))
        onObjectSelected(document.objects.firstOrNull { it.id == selectedObjectId })
        onObjectActionBarVisibleChanged(true)
    }

    fun toggleRotationWheel() {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } ?: return
        if (selected is ArrowObject) return
        isRotatingSelection = !isRotatingSelection
        rotationStartDocument = if (isRotatingSelection) document else null
        rotationLastAngleDegrees = null
        rotationCenter = if (isRotatingSelection) selected.bounds().center else null
        onObjectActionBarVisibleChanged(false)
        invalidate()
    }

    fun undo() {
        history = history.undo()
        applyHistoryCurrent()
    }

    fun redo() {
        history = history.redo()
        applyHistoryCurrent()
    }

    fun resizeSelectedImage(scale: Float) {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } as? ImagePageObject ?: return
        val resized = selected.scaled(scale)
        updateDocument(document.replaceObject(resized))
        onObjectSelected(resized)
    }

    fun fitSelectedImageToCanvas() {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } as? ImagePageObject ?: return
        val padding = 48f
        val canvasWidth = width.toFloat().coerceAtLeast(1f)
        val canvasHeight = height.toFloat().coerceAtLeast(1f)
        val sourceBounds = selected.bounds()
        val fitted = selected.copy(
            frame = Geometry.fitBoundsInside(
                sourceWidth = sourceBounds.width,
                sourceHeight = sourceBounds.height,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                padding = padding,
            ),
        )
        updateDocument(document.replaceObject(fitted))
        onObjectSelected(fitted)
    }

    fun exportBitmap(includeWhiteBackground: Boolean) = renderer.exportBitmap(document, width.coerceAtLeast(1), height.coerceAtLeast(1), includeWhiteBackground)

    private fun finishGesture(point: SketchPoint, canceled: Boolean) {
        if (isMovingSelection) {
            val startDocument = moveStartDocument
            val finalDocument = document
            isMovingSelection = false
            lastMovePoint = null
            moveStartDocument = null
            dragStart = null
            draftShape = null
            if (startDocument != null && finalDocument != startDocument) {
                history = history.record(finalDocument)
                onHistoryChanged(history.canUndo, history.canRedo)
            }
            onObjectSelected(document.objects.firstOrNull { it.id == selectedObjectId })
            onObjectActionBarVisibleChanged(true)
            return
        }

        if (canceled) {
            draftStroke = null
            draftShape = null
            dragStart = null
            invalidate()
            return
        }

        val start = dragStart ?: point
        when (activeTool) {
            DrawingTool.Pen -> {
                val points = draftStroke.orEmpty()
                if (points.size > 1) {
                    updateDocument(document.addObject(FreehandStroke(UUID.randomUUID().toString(), points, penStrokeWidth)))
                }
            }
            DrawingTool.Shape -> {
                if (Geometry.distance(start, point) > 12f) {
                    updateDocument(document.addObject(ShapeRegistry.create(activeShapeId, start, point)))
                }
            }
            else -> Unit
        }
        draftStroke = null
        draftShape = null
        dragStart = null
        invalidate()
    }

    private fun handleTap(point: SketchPoint) {
        if (selectedObjectId != null) {
            selectedObjectId = null
            isRotatingSelection = false
            rotationCenter = null
            onObjectSelected(null)
            onObjectActionBarVisibleChanged(false)
            invalidate()
            return
        }

        when (activeTool) {
            DrawingTool.Select -> {
                val selected = findTopObject(point)
                selectedObjectId = selected?.id
                onObjectSelected(selected)
                onObjectActionBarVisibleChanged(selected != null)
                invalidate()
            }
            DrawingTool.Eraser -> {
                eraseAt(point)
            }
            DrawingTool.Text -> {
                if (textValue.isNotBlank()) {
                    val text = TextObject(
                        id = UUID.randomUUID().toString(),
                        position = point,
                        text = textValue,
                        italic = italicText,
                        vector = vectorText,
                    )
                    updateDocument(document.addObject(text))
                }
            }
            DrawingTool.Angle -> selectLineForAngle(point)
            else -> Unit
        }
    }

    private fun selectLineForAngle(point: SketchPoint) {
        val line = findTopObject(point).lineEndpoints() ?: return
        angleSelection.add(line)
        if (angleSelection.size == 2) {
            val first = angleSelection[0]
            val second = angleSelection[1]
            val sweep = Geometry.angleBetweenLines(first.first, first.second, second.first, second.second)
            val startAngle = Math.toDegrees(atan2((first.second.y - first.first.y).toDouble(), (first.second.x - first.first.x).toDouble())).toFloat()
            val marker = AngleMarkerObject(UUID.randomUUID().toString(), first.first, 54f, startAngle, sweep)
            updateDocument(document.addObject(marker))
            angleSelection.clear()
        }
    }

    private fun moveSelectedTo(point: SketchPoint) {
        val last = lastMovePoint ?: point
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } ?: return
        val moved = selected.translated(point.x - last.x, point.y - last.y)
        updateDocumentWithoutHistory(document.replaceObject(moved))
        onObjectSelected(moved)
        onObjectActionBarVisibleChanged(false)
        lastMovePoint = point
    }

    private fun updateDraftShape(point: SketchPoint) {
        val start = dragStart ?: return
        draftShape = if (Geometry.distance(start, point) > 12f) {
            ShapeRegistry.create(activeShapeId, start, point)
        } else {
            null
        }
        invalidate()
    }

    private fun beginResizeIfNeeded(point: SketchPoint): Boolean {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } ?: return false
        val handle = selected.resizeHandleAt(point) ?: return false
        resizeHandle = handle
        resizeStartObject = selected
        resizeStartDocument = document
        isRotatingSelection = false
        rotationCenter = null
        isResizingSelection = true
        dragStart = null
        draftStroke = null
        draftShape = null
        onObjectActionBarVisibleChanged(false)
        invalidate()
        return true
    }

    private fun resizeSelectedTo(point: SketchPoint) {
        val startObject = resizeStartObject ?: return
        val handle = resizeHandle ?: return
        val resized = startObject.resizedBy(handle, point)
        updateDocumentWithoutHistory(document.replaceObject(resized))
        onObjectSelected(resized)
    }

    private fun finishResize(canceled: Boolean) {
        val startDocument = resizeStartDocument
        val finalDocument = document
        isResizingSelection = false
        resizeHandle = null
        resizeStartObject = null
        resizeStartDocument = null

        if (canceled && startDocument != null) {
            document = startDocument
            onDocumentChanged(startDocument)
        } else if (startDocument != null && finalDocument != startDocument) {
            history = history.record(finalDocument)
            onHistoryChanged(history.canUndo, history.canRedo)
        }

        onObjectSelected(document.objects.firstOrNull { it.id == selectedObjectId })
        onObjectActionBarVisibleChanged(selectedObjectId != null)
        invalidate()
    }

    private fun handleRotationEvent(event: MotionEvent, point: SketchPoint) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                rotationLastAngleDegrees = Geometry.angleDegrees(selectedRotationCenter() ?: point, point)
            }
            MotionEvent.ACTION_MOVE -> rotateSelectedTo(point)
            MotionEvent.ACTION_UP -> finishRotation(canceled = false)
            MotionEvent.ACTION_CANCEL -> finishRotation(canceled = true)
        }
    }

    private fun rotateSelectedTo(point: SketchPoint) {
        val selected = document.objects.firstOrNull { it.id == selectedObjectId } ?: return
        if (selected is ArrowObject) return
        val center = rotationCenter ?: selected.bounds().center
        val previousAngle = rotationLastAngleDegrees
        val nextAngle = Geometry.angleDegrees(center, point)
        if (previousAngle == null) {
            rotationLastAngleDegrees = nextAngle
            return
        }
        val rotated = selected.rotatedBy(shortestAngleDelta(previousAngle, nextAngle), center)
        updateDocumentWithoutHistory(document.replaceObject(rotated))
        onObjectSelected(rotated)
        rotationLastAngleDegrees = nextAngle
    }

    private fun finishRotation(canceled: Boolean) {
        val startDocument = rotationStartDocument
        val finalDocument = document
        isRotatingSelection = false
        rotationStartDocument = null
        rotationLastAngleDegrees = null
        rotationCenter = null

        if (canceled && startDocument != null) {
            document = startDocument
            onDocumentChanged(startDocument)
        } else if (startDocument != null && finalDocument != startDocument) {
            history = history.record(finalDocument)
            onHistoryChanged(history.canUndo, history.canRedo)
        }

        onObjectSelected(document.objects.firstOrNull { it.id == selectedObjectId })
        onObjectActionBarVisibleChanged(selectedObjectId != null)
        invalidate()
    }

    private fun selectedRotationCenter(): SketchPoint? {
        return rotationCenter ?: document.objects.firstOrNull { it.id == selectedObjectId }?.bounds()?.center
    }

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = to - from
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        return delta
    }

    private fun drawResizeHandles(canvas: Canvas, selected: DrawableObject) {
        selected.resizeHandleCenters().forEach { (_, center) ->
            val radius = 11f
            canvas.drawOval(
                RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                handleFillPaint,
            )
            canvas.drawOval(
                RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                handleStrokePaint,
            )

            val b = selected.bounds()
            val objectCenter = SketchPoint((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
            val angle = Math.toDegrees(atan2((center.y - objectCenter.y).toDouble(), (center.x - objectCenter.x).toDouble())).toFloat()
            drawHandleArrow(canvas, center, angle)
        }
    }

    private fun drawHandleArrow(canvas: Canvas, center: SketchPoint, angleDegrees: Float) {
        val path = Path().apply {
            moveTo(center.x + 7f, center.y)
            lineTo(center.x - 3f, center.y - 5f)
            lineTo(center.x - 3f, center.y + 5f)
            close()
        }
        canvas.save()
        canvas.rotate(angleDegrees, center.x, center.y)
        canvas.drawPath(path, handleArrowPaint)
        canvas.restore()
    }

    private fun drawRotationWheel(canvas: Canvas, selected: DrawableObject) {
        val b = selected.bounds()
        val center = b.center
        val radius = (max(b.width, b.height) / 2f + 42f).coerceAtLeast(58f)
        canvas.drawCircle(center.x, center.y, radius, rotationWheelPaint)
        canvas.drawCircle(center.x + radius, center.y, 10f, rotationWheelTouchPaint)
    }

    private fun eraseAt(point: SketchPoint) {
        val selected = findTopObject(point) ?: return
        val nextDocument = when (selected) {
            is FreehandStroke -> {
                val pieces = selected.erasedBy(point, eraserRadius)
                document.replaceObjectWith(selected.id, pieces)
            }
            else -> document.removeObject(selected.id)
        }
        if (nextDocument != document) {
            selectedObjectId = null
            onObjectSelected(null)
            onObjectActionBarVisibleChanged(false)
            updateDocument(nextDocument)
        }
    }

    private fun findTopObject(point: SketchPoint): DrawableObject? {
        return document.objects.asReversed().firstOrNull { it.hitTest(point) }
    }

    private fun updateDocument(next: DrawingDocument) {
        history = history.record(next)
        document = next
        onDocumentChanged(next)
        onHistoryChanged(history.canUndo, history.canRedo)
    }

    private fun updateDocumentWithoutHistory(next: DrawingDocument) {
        if (next == document) return
        document = next
        onDocumentChanged(next)
        invalidate()
    }

    private fun applyHistoryCurrent() {
        document = history.current
        selectedObjectId = null
        isRotatingSelection = false
        rotationCenter = null
        onObjectSelected(null)
        onObjectActionBarVisibleChanged(false)
        onDocumentChanged(history.current)
        onHistoryChanged(history.canUndo, history.canRedo)
    }

    private fun MotionEvent.toSketchPoint(): SketchPoint = SketchPoint(x, y)

    private fun DrawableObject?.lineEndpoints(): Pair<SketchPoint, SketchPoint>? {
        return when (this) {
            is LineObject -> start to end
            is ArrowObject -> start to end
            else -> null
        }
    }

    private fun DrawableObject.resizeHandleCenters(): List<Pair<ResizeHandle, SketchPoint>> {
        return when (this) {
            is ArrowObject -> listOf(
                ResizeHandle.Start to start,
                ResizeHandle.End to end,
            )
            is LineObject -> listOf(
                ResizeHandle.Start to start,
                ResizeHandle.End to end,
            )
            else -> {
                val b = bounds()
                listOf(
                    ResizeHandle.TopLeft to SketchPoint(b.left, b.top),
                    ResizeHandle.TopRight to SketchPoint(b.right, b.top),
                    ResizeHandle.BottomLeft to SketchPoint(b.left, b.bottom),
                    ResizeHandle.BottomRight to SketchPoint(b.right, b.bottom),
                )
            }
        }
    }

    private fun DrawableObject.resizeHandleAt(point: SketchPoint): ResizeHandle? {
        return resizeHandleCenters()
            .firstOrNull { (_, center) -> Geometry.distance(point, center) <= 34f }
            ?.first
    }

    private fun DrawableObject.resizedBy(handle: ResizeHandle, point: SketchPoint): DrawableObject {
        return when (this) {
            is ArrowObject -> resizedArrow(handle, point)
            is LineObject -> resizedLine(handle, point)
            else -> resizedToBounds(resizedBounds(handle, point))
        }
    }

    private fun ArrowObject.resizedArrow(handle: ResizeHandle, point: SketchPoint): ArrowObject {
        return resizedEndpoint(
            movingStart = handle == ResizeHandle.Start,
            point = point,
        )
    }

    private fun LineObject.resizedLine(handle: ResizeHandle, point: SketchPoint): LineObject {
        return resizedEndpoint(
            movingStart = handle == ResizeHandle.Start,
            point = point,
        )
    }

    private fun DrawableObject.resizedBounds(handle: ResizeHandle, point: SketchPoint): SketchBounds {
        val b = bounds()
        val minSize = 24f
        return when (handle) {
            ResizeHandle.TopLeft -> SketchBounds(
                left = min(point.x, b.right - minSize),
                top = min(point.y, b.bottom - minSize),
                right = b.right,
                bottom = b.bottom,
            )
            ResizeHandle.TopRight -> SketchBounds(
                left = b.left,
                top = min(point.y, b.bottom - minSize),
                right = max(point.x, b.left + minSize),
                bottom = b.bottom,
            )
            ResizeHandle.BottomLeft -> SketchBounds(
                left = min(point.x, b.right - minSize),
                top = b.top,
                right = b.right,
                bottom = max(point.y, b.top + minSize),
            )
            ResizeHandle.BottomRight -> SketchBounds(
                left = b.left,
                top = b.top,
                right = max(point.x, b.left + minSize),
                bottom = max(point.y, b.top + minSize),
            )
            ResizeHandle.Start,
            ResizeHandle.End -> b
        }
    }

    private fun DrawableObject.resizedToBounds(nextBounds: SketchBounds): DrawableObject {
        val oldBounds = bounds()
        return when (this) {
            is CircleObject -> CircleObject.fromCorners(id, SketchPoint(nextBounds.left, nextBounds.top), SketchPoint(nextBounds.right, nextBounds.bottom))
            is RectObject -> copy(topLeft = SketchPoint(nextBounds.left, nextBounds.top), bottomRight = SketchPoint(nextBounds.right, nextBounds.bottom))
            is ImagePageObject -> copy(frame = nextBounds)
            is AngleMarkerObject -> {
                val radius = min(nextBounds.width, nextBounds.height).coerceAtLeast(24f) / 2f
                copy(
                    vertex = SketchPoint((nextBounds.left + nextBounds.right) / 2f, (nextBounds.top + nextBounds.bottom) / 2f),
                    radius = radius,
                )
            }
            is TextObject -> {
                val scale = max(nextBounds.width / oldBounds.width.coerceAtLeast(1f), nextBounds.height / oldBounds.height.coerceAtLeast(1f))
                    .coerceAtLeast(0.25f)
                copy(
                    position = SketchPoint(nextBounds.left, nextBounds.bottom - sizePx * 0.25f * scale),
                    sizePx = sizePx * scale,
                )
            }
            is FreehandStroke -> copy(points = points.map { it.scaledFrom(oldBounds, nextBounds) })
            else -> this
        }
    }

    private fun SketchPoint.scaledFrom(oldBounds: SketchBounds, nextBounds: SketchBounds): SketchPoint {
        val oldWidth = oldBounds.width.coerceAtLeast(1f)
        val oldHeight = oldBounds.height.coerceAtLeast(1f)
        val xRatio = (x - oldBounds.left) / oldWidth
        val yRatio = (y - oldBounds.top) / oldHeight
        return SketchPoint(
            x = nextBounds.left + nextBounds.width * xRatio,
            y = nextBounds.top + nextBounds.height * yRatio,
        )
    }

    private enum class ResizeHandle {
        TopLeft,
        TopRight,
        BottomLeft,
        BottomRight,
        Start,
        End,
    }
}
