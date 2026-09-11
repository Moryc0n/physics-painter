package com.fedbaq.physicssketch.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingHistoryTest {
    @Test
    fun undoRestoresPreviousDocumentAndEnablesRedo() {
        val empty = DrawingDocument(id = "drawing", title = "Рисунок")
        val changed = empty.addObject(LineObject("line", com.fedbaq.physicssketch.core.geometry.SketchPoint(0f, 0f), com.fedbaq.physicssketch.core.geometry.SketchPoint(10f, 0f)))

        val history = DrawingHistory(empty)
            .record(changed)
            .undo()

        assertEquals(empty, history.current)
        assertTrue(history.canRedo)
    }

    @Test
    fun redoRestoresUndoneDocument() {
        val empty = DrawingDocument(id = "drawing", title = "Рисунок")
        val changed = empty.copy(title = "Новое имя")

        val history = DrawingHistory(empty)
            .record(changed)
            .undo()
            .redo()

        assertEquals(changed, history.current)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun recordingAfterUndoClearsRedo() {
        val first = DrawingDocument(id = "drawing", title = "1")
        val second = first.copy(title = "2")
        val third = first.copy(title = "3")

        val history = DrawingHistory(first)
            .record(second)
            .undo()
            .record(third)

        assertEquals(third, history.current)
        assertFalse(history.canRedo)
    }
}
