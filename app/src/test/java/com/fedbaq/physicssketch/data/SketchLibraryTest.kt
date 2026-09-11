package com.fedbaq.physicssketch.data

import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.DrawingSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SketchLibraryTest {
    @Test
    fun deleteOnlyDrawingRemovesItsSetAndSelectsAnotherSet() {
        val firstDrawing = DrawingDocument(id = "drawing-1", title = "Рисунок 1")
        val secondDrawing = DrawingDocument(id = "drawing-2", title = "Рисунок 2")
        val firstSet = DrawingSet(id = "set-1", title = "Лекция 1", drawings = listOf(firstDrawing))
        val secondSet = DrawingSet(id = "set-2", title = "Лекция 2", drawings = listOf(secondDrawing))
        val library = SketchLibrary(sets = listOf(firstSet, secondSet))

        val result = library.deleteDrawing("set-1", "drawing-1")

        assertEquals(listOf(secondSet), result.library.sets)
        assertEquals("set-2", result.selectedSetId)
        assertEquals("drawing-2", result.selectedDrawingId)
    }

    @Test
    fun deleteOnlyDrawingInOnlySetCreatesFreshDefaultSetForEditor() {
        val drawing = DrawingDocument(id = "drawing", title = "Рисунок 1")
        val set = DrawingSet(id = "set", title = "Лекция 1", drawings = listOf(drawing))
        val library = SketchLibrary(sets = listOf(set))

        val result = library.deleteDrawing("set", "drawing")

        assertEquals(1, result.library.sets.size)
        assertEquals(1, result.library.sets.first().drawings.size)
        assertNotEquals("set", result.selectedSetId)
        assertEquals(result.library.sets.first().id, result.selectedSetId)
        assertEquals(result.library.sets.first().drawings.first().id, result.selectedDrawingId)
    }

    @Test
    fun deleteDrawingKeepsSetWhenOtherDrawingsRemain() {
        val firstDrawing = DrawingDocument(id = "drawing-1", title = "Рисунок 1")
        val secondDrawing = DrawingDocument(id = "drawing-2", title = "Рисунок 2")
        val set = DrawingSet(id = "set", title = "Лекция", drawings = listOf(firstDrawing, secondDrawing))
        val library = SketchLibrary(sets = listOf(set))

        val result = library.deleteDrawing("set", "drawing-1")

        assertEquals("set", result.selectedSetId)
        assertEquals("drawing-2", result.selectedDrawingId)
        assertEquals(listOf(secondDrawing), result.library.sets.first().drawings)
    }
}
