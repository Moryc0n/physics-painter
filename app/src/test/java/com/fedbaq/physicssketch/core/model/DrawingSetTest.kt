package com.fedbaq.physicssketch.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingSetTest {
    @Test
    fun renameDrawingUpdatesOnlyTargetDocument() {
        val first = DrawingDocument(id = "first", title = "Рисунок 1")
        val second = DrawingDocument(id = "second", title = "Рисунок 2")
        val set = DrawingSet(id = "set", title = "Лекция", drawings = listOf(first, second))

        val renamed = set.renameDrawing("second", "Схема силы")

        assertEquals("Рисунок 1", renamed.drawings[0].title)
        assertEquals("Схема силы", renamed.drawings[1].title)
    }

    @Test
    fun blankDrawingNameIsIgnored() {
        val document = DrawingDocument(id = "drawing", title = "Старое имя")
        val set = DrawingSet(id = "set", title = "Лекция", drawings = listOf(document))

        val renamed = set.renameDrawing("drawing", " ")

        assertEquals("Старое имя", renamed.drawings.first().title)
    }
}
