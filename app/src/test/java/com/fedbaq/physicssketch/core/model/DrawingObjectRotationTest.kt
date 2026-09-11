package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchBounds
import com.fedbaq.physicssketch.core.geometry.SketchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingObjectRotationTest {
    @Test
    fun lineRotatesPointsAroundItsCenter() {
        val line = LineObject(
            id = "line-1",
            start = SketchPoint(0f, 0f),
            end = SketchPoint(10f, 0f),
        )

        val rotated = line.rotatedBy(90f) as LineObject

        assertEquals(5f, rotated.start.x, 0.001f)
        assertEquals(-5f, rotated.start.y, 0.001f)
        assertEquals(5f, rotated.end.x, 0.001f)
        assertEquals(5f, rotated.end.y, 0.001f)
    }

    @Test
    fun rectangleStoresUnlimitedRotation() {
        val rect = RectObject(
            id = "rect-1",
            topLeft = SketchPoint(0f, 0f),
            bottomRight = SketchPoint(10f, 20f),
        )

        val rotated = rect.rotatedBy(450f) as RectObject

        assertEquals(450f, rotated.rotationDegrees, 0.001f)
    }

    @Test
    fun textRotationPreservesVectorFlagAndCase() {
        val text = TextObject(
            id = "text-1",
            position = SketchPoint(10f, 20f),
            text = "vV_0",
            italic = true,
            vector = true,
        )

        val rotated = text.rotatedBy(-45f) as TextObject

        assertEquals("vV_0", rotated.text)
        assertTrue(rotated.vector)
        assertEquals(-45f, rotated.rotationDegrees, 0.001f)
    }

    @Test
    fun imageRotationKeepsFrameAndStoresAngle() {
        val image = ImagePageObject(
            id = "image-1",
            frame = SketchBounds(0f, 0f, 100f, 200f),
            sourceUri = "content://lecture/page.png",
        )

        val rotated = image.rotatedBy(30f) as ImagePageObject

        assertEquals(image.frame, rotated.frame)
        assertEquals(30f, rotated.rotationDegrees, 0.001f)
    }
}
