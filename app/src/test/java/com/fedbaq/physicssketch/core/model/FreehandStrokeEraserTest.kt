package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class FreehandStrokeEraserTest {
    @Test
    fun eraserSplitsStrokeIntoRemainingPieces() {
        val stroke = FreehandStroke(
            id = "stroke",
            points = listOf(
                SketchPoint(0f, 0f),
                SketchPoint(10f, 0f),
                SketchPoint(20f, 0f),
                SketchPoint(30f, 0f),
                SketchPoint(40f, 0f),
            ),
        )

        val pieces = stroke.erasedBy(center = SketchPoint(20f, 0f), radius = 5f)

        assertEquals(2, pieces.size)
        assertEquals(listOf(SketchPoint(0f, 0f), SketchPoint(10f, 0f)), pieces[0].points)
        assertEquals(listOf(SketchPoint(30f, 0f), SketchPoint(40f, 0f)), pieces[1].points)
    }

    @Test
    fun eraserDropsTinySinglePointFragments() {
        val stroke = FreehandStroke(
            id = "stroke",
            points = listOf(
                SketchPoint(0f, 0f),
                SketchPoint(10f, 0f),
                SketchPoint(20f, 0f),
            ),
        )

        val pieces = stroke.erasedBy(center = SketchPoint(10f, 0f), radius = 6f)

        assertEquals(emptyList<FreehandStroke>(), pieces)
    }

    @Test
    fun erasedPiecesKeepOriginalStrokeWidth() {
        val stroke = FreehandStroke(
            id = "stroke",
            points = listOf(
                SketchPoint(0f, 0f),
                SketchPoint(10f, 0f),
                SketchPoint(20f, 0f),
                SketchPoint(30f, 0f),
                SketchPoint(40f, 0f),
            ),
            strokeWidth = 12f,
        )

        val pieces = stroke.erasedBy(center = SketchPoint(20f, 0f), radius = 5f)

        assertEquals(12f, pieces[0].strokeWidth, 0.001f)
        assertEquals(12f, pieces[1].strokeWidth, 0.001f)
    }
}
