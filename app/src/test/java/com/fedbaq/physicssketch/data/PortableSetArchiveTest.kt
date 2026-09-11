package com.fedbaq.physicssketch.data

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.DrawingSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableSetArchiveTest {
    @Test
    fun roundTripPreservesSetAndDrawingObjects() {
        val set = DrawingSet(
            id = "set-1",
            title = "Лекция 1",
            drawings = listOf(
                DrawingDocument(
                    id = "drawing-1",
                    title = "Сила",
                    objects = listOf(ArrowObject("arrow-1", SketchPoint(10f, 20f), SketchPoint(40f, 20f))),
                ),
            ),
        )
        val bytes = ByteArrayOutputStream()

        PortableSetArchive.writeJsonEntries(set, bytes)
        val restored = PortableSetArchive.readSet(ByteArrayInputStream(bytes.toByteArray()))

        assertEquals("Лекция 1", restored.title)
        assertEquals("Сила", restored.drawings.single().title)
        assertTrue(restored.drawings.single().objects.single() is ArrowObject)
    }
}
