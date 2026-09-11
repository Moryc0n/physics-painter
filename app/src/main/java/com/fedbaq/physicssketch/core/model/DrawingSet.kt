package com.fedbaq.physicssketch.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DrawingSet(
    val id: String,
    val title: String,
    val drawings: List<DrawingDocument>,
) {
    fun renamed(title: String): DrawingSet = copy(title = title.ifBlank { this.title })

    fun addBlankDrawing(): DrawingSet {
        val nextNumber = drawings.size + 1
        return copy(
            drawings = drawings + DrawingDocument(
                id = UUID.randomUUID().toString(),
                title = "Рисунок $nextNumber",
            ),
        )
    }

    fun removeDrawing(drawingId: String): DrawingSet {
        val next = drawings.filterNot { it.id == drawingId }
        return if (next.isEmpty()) copy(drawings = listOf(blankDrawing("Рисунок 1"))) else copy(drawings = next)
    }

    fun replaceDrawing(document: DrawingDocument): DrawingSet {
        return copy(drawings = drawings.map { if (it.id == document.id) document else it })
    }

    fun renameDrawing(drawingId: String, title: String): DrawingSet {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return this
        return copy(drawings = drawings.map { if (it.id == drawingId) it.copy(title = cleanTitle) else it })
    }

    companion object {
        fun defaultSet(): DrawingSet = DrawingSet(
            id = UUID.randomUUID().toString(),
            title = "Лекция 1",
            drawings = listOf(blankDrawing("Рисунок 1")),
        )

        fun blankDrawing(title: String): DrawingDocument = DrawingDocument(
            id = UUID.randomUUID().toString(),
            title = title,
        )
    }
}
