package com.fedbaq.physicssketch.core.geometry

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Serializable
data class SketchPoint(
    val x: Float,
    val y: Float,
) {
    fun translated(dx: Float, dy: Float): SketchPoint = copy(x = x + dx, y = y + dy)
}

@Serializable
data class SketchBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: SketchPoint get() = SketchPoint((left + right) / 2f, (top + bottom) / 2f)

    fun translated(dx: Float, dy: Float): SketchBounds = copy(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy,
    )

    fun contains(point: SketchPoint, padding: Float = 0f): Boolean {
        return point.x in (left - padding)..(right + padding) &&
            point.y in (top - padding)..(bottom + padding)
    }

    companion object {
        fun fromPoints(points: List<SketchPoint>): SketchBounds {
            val xs = points.map { it.x }
            val ys = points.map { it.y }
            return SketchBounds(
                left = xs.minOrNull() ?: 0f,
                top = ys.minOrNull() ?: 0f,
                right = xs.maxOrNull() ?: 0f,
                bottom = ys.maxOrNull() ?: 0f,
            )
        }

        fun fromCorners(a: SketchPoint, b: SketchPoint): SketchBounds {
            return SketchBounds(
                left = min(a.x, b.x),
                top = min(a.y, b.y),
                right = max(a.x, b.x),
                bottom = max(a.y, b.y),
            )
        }
    }
}

object Geometry {
    fun distance(a: SketchPoint, b: SketchPoint): Float = hypot(a.x - b.x, a.y - b.y)

    fun angleDegrees(center: SketchPoint, point: SketchPoint): Float {
        return Math.toDegrees(atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble())).toFloat()
    }

    fun rotatePoint(point: SketchPoint, center: SketchPoint, degrees: Float): SketchPoint {
        val radians = Math.toRadians(degrees.toDouble())
        val dx = point.x - center.x
        val dy = point.y - center.y
        return SketchPoint(
            x = (center.x + dx * cos(radians) - dy * sin(radians)).toFloat(),
            y = (center.y + dx * sin(radians) + dy * cos(radians)).toFloat(),
        )
    }

    fun distanceToSegment(point: SketchPoint, start: SketchPoint, end: SketchPoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0f && dy == 0f) return distance(point, start)

        val t = (((point.x - start.x) * dx) + ((point.y - start.y) * dy)) / ((dx * dx) + (dy * dy))
        val clamped = t.coerceIn(0f, 1f)
        val projection = SketchPoint(start.x + clamped * dx, start.y + clamped * dy)
        return distance(point, projection)
    }

    fun angleBetweenLines(
        firstStart: SketchPoint,
        firstEnd: SketchPoint,
        secondStart: SketchPoint,
        secondEnd: SketchPoint,
    ): Float {
        val ax = firstEnd.x - firstStart.x
        val ay = firstEnd.y - firstStart.y
        val bx = secondEnd.x - secondStart.x
        val by = secondEnd.y - secondStart.y
        val aLength = hypot(ax, ay)
        val bLength = hypot(bx, by)
        if (aLength == 0f || bLength == 0f) return 0f

        val dot = ((ax * bx) + (ay * by)) / (aLength * bLength)
        val degrees = Math.toDegrees(acos(dot.coerceIn(-1f, 1f)).toDouble()).toFloat()
        return if (degrees > 180f) abs(360f - degrees) else degrees
    }

    fun scaleBoundsFromCenter(bounds: SketchBounds, scale: Float): SketchBounds {
        val safeScale = scale.coerceAtLeast(0.1f)
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val halfWidth = bounds.width * safeScale / 2f
        val halfHeight = bounds.height * safeScale / 2f
        return SketchBounds(
            left = centerX - halfWidth,
            top = centerY - halfHeight,
            right = centerX + halfWidth,
            bottom = centerY + halfHeight,
        )
    }

    fun fitBoundsInside(
        sourceWidth: Float,
        sourceHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        padding: Float,
    ): SketchBounds {
        val safeSourceWidth = sourceWidth.coerceAtLeast(1f)
        val safeSourceHeight = sourceHeight.coerceAtLeast(1f)
        val maxWidth = (canvasWidth - padding * 2f).coerceAtLeast(1f)
        val maxHeight = (canvasHeight - padding * 2f).coerceAtLeast(1f)
        val sourceRatio = safeSourceWidth / safeSourceHeight
        val frameWidth = if (maxWidth / maxHeight <= sourceRatio) maxWidth else maxHeight * sourceRatio
        val frameHeight = frameWidth / sourceRatio
        return SketchBounds(
            left = (canvasWidth - frameWidth) / 2f,
            top = (canvasHeight - frameHeight) / 2f,
            right = (canvasWidth + frameWidth) / 2f,
            bottom = (canvasHeight + frameHeight) / 2f,
        )
    }
}
