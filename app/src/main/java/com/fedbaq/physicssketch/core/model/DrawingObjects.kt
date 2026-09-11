package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.Geometry
import com.fedbaq.physicssketch.core.geometry.SketchBounds
import com.fedbaq.physicssketch.core.geometry.SketchPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

private const val HIT_PADDING = 24f

@Serializable
@SerialName("freehand")
data class FreehandStroke(
    override val id: String,
    val points: List<SketchPoint>,
    val strokeWidth: Float = 5f,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds.fromPoints(points)

    override fun hitTest(point: SketchPoint): Boolean {
        return points.zipWithNext().any { (start, end) ->
            Geometry.distanceToSegment(point, start, end) <= HIT_PADDING
        }
    }

    override fun translated(dx: Float, dy: Float): DrawableObject {
        return copy(points = points.map { it.translated(dx, dy) })
    }

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, points = points.map { it.translated(18f, 18f) })

    fun erasedBy(center: SketchPoint, radius: Float): List<FreehandStroke> {
        val remainingSegments = mutableListOf<List<SketchPoint>>()
        val currentSegment = mutableListOf<SketchPoint>()
        var erasedAnyPoint = false

        points.forEach { point ->
            val erased = Geometry.distance(center, point) <= radius

            if (erased) {
                erasedAnyPoint = true
                if (currentSegment.size >= 2) {
                    remainingSegments += currentSegment.toList()
                }
                currentSegment.clear()
            } else {
                currentSegment += point
            }
        }

        if (currentSegment.size >= 2) {
            remainingSegments += currentSegment.toList()
        }

        if (!erasedAnyPoint) return listOf(this)

        return remainingSegments.mapIndexed { index, segment ->
            copy(id = "${id}_erase_$index", points = segment)
        }
    }
}

@Serializable
@SerialName("line")
data class LineObject(
    override val id: String,
    val start: SketchPoint,
    val end: SketchPoint,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds.fromCorners(start, end)

    override fun hitTest(point: SketchPoint): Boolean = Geometry.distanceToSegment(point, start, end) <= HIT_PADDING

    override fun translated(dx: Float, dy: Float): DrawableObject = LineObject(id, start.translated(dx, dy), end.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = LineObject(newId, start.translated(18f, 18f), end.translated(18f, 18f))

    fun resizedEndpoint(movingStart: Boolean, point: SketchPoint): LineObject {
        val moving = if (movingStart) start else end
        val fixed = if (movingStart) end else start
        val next = when {
            moving.y == fixed.y -> SketchPoint(point.x, moving.y)
            moving.x == fixed.x -> SketchPoint(moving.x, point.y)
            else -> point
        }
        return if (movingStart) copy(start = next) else copy(end = next)
    }
}

@Serializable
@SerialName("arrow")
data class ArrowObject(
    override val id: String,
    val start: SketchPoint,
    val end: SketchPoint,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds.fromCorners(start, end)

    override fun hitTest(point: SketchPoint): Boolean = Geometry.distanceToSegment(point, start, end) <= HIT_PADDING

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(start = start.translated(dx, dy), end = end.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, start = start.translated(18f, 18f), end = end.translated(18f, 18f))

    fun resizedEndpoint(movingStart: Boolean, point: SketchPoint, minLength: Float = 28f): ArrowObject {
        val fixed = if (movingStart) end else start
        if (Geometry.distance(fixed, point) < minLength) return this
        return if (movingStart) copy(start = point) else copy(end = point)
    }
}

@Serializable
@SerialName("circle")
data class CircleObject(
    override val id: String,
    val center: SketchPoint,
    val radiusX: Float,
    val radiusY: Float,
    val rotationDegrees: Float = 0f,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds(
        left = center.x - radiusX,
        top = center.y - radiusY,
        right = center.x + radiusX,
        bottom = center.y + radiusY,
    )

    override fun hitTest(point: SketchPoint): Boolean {
        val b = bounds()
        val testPoint = Geometry.rotatePoint(point, b.center, -rotationDegrees)
        return b.contains(testPoint, HIT_PADDING)
    }

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(center = center.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, center = center.translated(18f, 18f))

    companion object {
        fun fromCorners(id: String, start: SketchPoint, end: SketchPoint): CircleObject {
            val left = min(start.x, end.x)
            val top = min(start.y, end.y)
            val right = max(start.x, end.x)
            val bottom = max(start.y, end.y)
            return CircleObject(
                id = id,
                center = SketchPoint((left + right) / 2f, (top + bottom) / 2f),
                radiusX = (right - left) / 2f,
                radiusY = (bottom - top) / 2f,
            )
        }
    }
}

@Serializable
@SerialName("rectangle")
data class RectObject(
    override val id: String,
    val topLeft: SketchPoint,
    val bottomRight: SketchPoint,
    val rotationDegrees: Float = 0f,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds.fromCorners(topLeft, bottomRight)

    override fun hitTest(point: SketchPoint): Boolean {
        val b = bounds()
        val testPoint = Geometry.rotatePoint(point, b.center, -rotationDegrees)
        return b.contains(testPoint, HIT_PADDING)
    }

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(
        topLeft = topLeft.translated(dx, dy),
        bottomRight = bottomRight.translated(dx, dy),
    )

    override fun duplicate(newId: String): DrawableObject = copy(
        id = newId,
        topLeft = topLeft.translated(18f, 18f),
        bottomRight = bottomRight.translated(18f, 18f),
    )
}

@Serializable
@SerialName("text")
data class TextObject(
    override val id: String,
    val position: SketchPoint,
    val text: String,
    val italic: Boolean,
    val vector: Boolean = false,
    val sizePx: Float = 64f,
    val rotationDegrees: Float = 0f,
) : DrawableObject {
    override fun bounds(): SketchBounds {
        val width = max(48f, text.length * sizePx * 0.62f)
        val top = if (vector && text.isNotBlank()) position.y - sizePx * 1.4f else position.y - sizePx
        return SketchBounds(position.x, top, position.x + width, position.y + sizePx * 0.25f)
    }

    override fun hitTest(point: SketchPoint): Boolean {
        val b = bounds()
        val testPoint = Geometry.rotatePoint(point, b.center, -rotationDegrees)
        return b.contains(testPoint, HIT_PADDING)
    }

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(position = position.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, position = position.translated(18f, 18f))
}

@Serializable
@SerialName("angleMarker")
data class AngleMarkerObject(
    override val id: String,
    val vertex: SketchPoint,
    val radius: Float,
    val startAngleDegrees: Float,
    val sweepDegrees: Float,
) : DrawableObject {
    override fun bounds(): SketchBounds = SketchBounds(
        left = vertex.x - radius,
        top = vertex.y - radius,
        right = vertex.x + radius,
        bottom = vertex.y + radius,
    )

    override fun hitTest(point: SketchPoint): Boolean = bounds().contains(point, HIT_PADDING)

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(vertex = vertex.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, vertex = vertex.translated(18f, 18f))
}

@Serializable
@SerialName("imagePage")
data class ImagePageObject(
    override val id: String,
    val frame: SketchBounds,
    val sourceUri: String,
    val pageIndex: Int = 0,
    val rotationDegrees: Float = 0f,
) : DrawableObject {
    override fun bounds(): SketchBounds = frame

    override fun hitTest(point: SketchPoint): Boolean {
        val testPoint = Geometry.rotatePoint(point, frame.center, -rotationDegrees)
        return frame.contains(testPoint, HIT_PADDING)
    }

    override fun translated(dx: Float, dy: Float): DrawableObject = copy(frame = frame.translated(dx, dy))

    override fun duplicate(newId: String): DrawableObject = copy(id = newId, frame = frame.translated(18f, 18f))

    fun scaled(scale: Float): ImagePageObject = copy(frame = Geometry.scaleBoundsFromCenter(frame, scale))
}

fun DrawableObject.rotatedBy(degrees: Float, center: SketchPoint = bounds().center): DrawableObject {
    return when (this) {
        is FreehandStroke -> copy(points = points.map { Geometry.rotatePoint(it, center, degrees) })
        is LineObject -> copy(
            start = Geometry.rotatePoint(start, center, degrees),
            end = Geometry.rotatePoint(end, center, degrees),
        )
        is ArrowObject -> this
        is CircleObject -> copy(rotationDegrees = rotationDegrees + degrees)
        is RectObject -> copy(rotationDegrees = rotationDegrees + degrees)
        is TextObject -> copy(rotationDegrees = rotationDegrees + degrees)
        is AngleMarkerObject -> copy(startAngleDegrees = startAngleDegrees + degrees)
        is ImagePageObject -> copy(rotationDegrees = rotationDegrees + degrees)
    }
}
