package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingSerializationTest {
    @Test
    fun documentJsonPreservesArrowObjectsForPortableStorage() {
        val json = Json { classDiscriminator = "type" }
        val document = DrawingDocument(
            id = "drawing-1",
            title = "Рисунок 1",
            objects = listOf(ArrowObject("arrow-1", SketchPoint(1f, 2f), SketchPoint(3f, 4f))),
        )

        val encoded = json.encodeToString(document)
        val restored = json.decodeFromString<DrawingDocument>(encoded)

        assertTrue(encoded.contains("\"type\":\"arrow\""))
        assertEquals("drawing-1", restored.id)
        assertTrue(restored.objects.single() is ArrowObject)
    }

    @Test
    fun imagePageJsonPreservesPdfPageIndex() {
        val json = Json { classDiscriminator = "type" }
        val document = DrawingDocument(
            id = "drawing-1",
            title = "Страница PDF",
            objects = listOf(
                ImagePageObject(
                    id = "page-1",
                    frame = com.fedbaq.physicssketch.core.geometry.SketchBounds(0f, 0f, 100f, 200f),
                    sourceUri = "content://physics/lecture.pdf",
                    pageIndex = 3,
                ),
            ),
        )

        val restored = json.decodeFromString<DrawingDocument>(json.encodeToString(document))

        val page = restored.objects.single() as ImagePageObject
        assertEquals(3, page.pageIndex)
    }

    @Test
    fun textJsonPreservesVectorFlagWithoutChangingCase() {
        val json = Json { classDiscriminator = "type" }
        val document = DrawingDocument(
            id = "drawing-1",
            title = "Вектор",
            objects = listOf(
                TextObject(
                    id = "text-1",
                    position = SketchPoint(1f, 2f),
                    text = "vV_0",
                    italic = true,
                    vector = true,
                ),
            ),
        )

        val restored = json.decodeFromString<DrawingDocument>(json.encodeToString(document))

        val text = restored.objects.single() as TextObject
        assertEquals("vV_0", text.text)
        assertTrue(text.vector)
    }
}
