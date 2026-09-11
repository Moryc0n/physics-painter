package com.fedbaq.physicssketch.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Test

class GeometryTest {
    @Test
    fun distanceToSegmentReturnsZeroForPointOnLine() {
        val distance = Geometry.distanceToSegment(
            point = SketchPoint(5f, 0f),
            start = SketchPoint(0f, 0f),
            end = SketchPoint(10f, 0f),
        )

        assertEquals(0f, distance, 0.001f)
    }

    @Test
    fun angleBetweenPerpendicularLinesIsNinetyDegrees() {
        val angle = Geometry.angleBetweenLines(
            firstStart = SketchPoint(0f, 0f),
            firstEnd = SketchPoint(10f, 0f),
            secondStart = SketchPoint(0f, 0f),
            secondEnd = SketchPoint(0f, 8f),
        )

        assertEquals(90f, angle, 0.001f)
    }

    @Test
    fun resizedBoundsKeepCenterWhenScaled() {
        val resized = Geometry.scaleBoundsFromCenter(
            bounds = SketchBounds(left = 10f, top = 20f, right = 110f, bottom = 220f),
            scale = 0.5f,
        )

        assertEquals(35f, resized.left, 0.001f)
        assertEquals(70f, resized.top, 0.001f)
        assertEquals(85f, resized.right, 0.001f)
        assertEquals(170f, resized.bottom, 0.001f)
    }

    @Test
    fun fitBoundsInsideCanvasKeepsAspectRatio() {
        val fitted = Geometry.fitBoundsInside(
            sourceWidth = 900f,
            sourceHeight = 1200f,
            canvasWidth = 1200f,
            canvasHeight = 900f,
            padding = 50f,
        )

        assertEquals(312.5f, fitted.left, 0.001f)
        assertEquals(50f, fitted.top, 0.001f)
        assertEquals(887.5f, fitted.right, 0.001f)
        assertEquals(850f, fitted.bottom, 0.001f)
    }

    @Test
    fun rotatePointTurnsAroundCenter() {
        val rotated = Geometry.rotatePoint(
            point = SketchPoint(10f, 0f),
            center = SketchPoint(0f, 0f),
            degrees = 90f,
        )

        assertEquals(0f, rotated.x, 0.001f)
        assertEquals(10f, rotated.y, 0.001f)
    }
}
