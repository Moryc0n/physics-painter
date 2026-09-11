package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrowObjectTest {
    @Test
    fun resizingEndKeepsStartAndMovesEndToPointer() {
        val arrow = ArrowObject(
            id = "arrow",
            start = SketchPoint(10f, 20f),
            end = SketchPoint(110f, 20f),
        )

        val resized = arrow.resizedEndpoint(
            movingStart = false,
            point = SketchPoint(70f, 90f),
        )

        assertEquals(SketchPoint(10f, 20f), resized.start)
        assertEquals(SketchPoint(70f, 90f), resized.end)
    }

    @Test
    fun resizingStartKeepsArrowHeadAndMovesStartToPointer() {
        val arrow = ArrowObject(
            id = "arrow",
            start = SketchPoint(10f, 20f),
            end = SketchPoint(110f, 20f),
        )

        val resized = arrow.resizedEndpoint(
            movingStart = true,
            point = SketchPoint(70f, 90f),
        )

        assertEquals(SketchPoint(70f, 90f), resized.start)
        assertEquals(SketchPoint(110f, 20f), resized.end)
    }

    @Test
    fun resizingEndpointDoesNotCollapseArrowToPoint() {
        val arrow = ArrowObject(
            id = "arrow",
            start = SketchPoint(10f, 20f),
            end = SketchPoint(110f, 20f),
        )

        val resized = arrow.resizedEndpoint(
            movingStart = false,
            point = SketchPoint(10f, 20f),
        )

        assertEquals(arrow, resized)
    }
}
