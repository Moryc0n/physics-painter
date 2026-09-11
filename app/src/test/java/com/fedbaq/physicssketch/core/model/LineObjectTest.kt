package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class LineObjectTest {
    @Test
    fun resizingHorizontalLineEndChangesOnlyLength() {
        val line = LineObject(
            id = "line",
            start = SketchPoint(10f, 20f),
            end = SketchPoint(110f, 20f),
        )

        val resized = line.resizedEndpoint(
            movingStart = false,
            point = SketchPoint(160f, 80f),
        )

        assertEquals(SketchPoint(10f, 20f), resized.start)
        assertEquals(SketchPoint(160f, 20f), resized.end)
    }

    @Test
    fun resizingVerticalLineStartChangesOnlyLength() {
        val line = LineObject(
            id = "line",
            start = SketchPoint(40f, 20f),
            end = SketchPoint(40f, 140f),
        )

        val resized = line.resizedEndpoint(
            movingStart = true,
            point = SketchPoint(100f, 60f),
        )

        assertEquals(SketchPoint(40f, 60f), resized.start)
        assertEquals(SketchPoint(40f, 140f), resized.end)
    }

    @Test
    fun resizingDiagonalLineMovesEndpointToPointer() {
        val line = LineObject(
            id = "line",
            start = SketchPoint(10f, 20f),
            end = SketchPoint(110f, 80f),
        )

        val resized = line.resizedEndpoint(
            movingStart = false,
            point = SketchPoint(160f, 120f),
        )

        assertEquals(SketchPoint(10f, 20f), resized.start)
        assertEquals(SketchPoint(160f, 120f), resized.end)
    }
}
