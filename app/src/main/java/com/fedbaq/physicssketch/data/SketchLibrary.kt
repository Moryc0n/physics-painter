package com.fedbaq.physicssketch.data

import com.fedbaq.physicssketch.core.model.DrawingSet
import kotlinx.serialization.Serializable

@Serializable
data class SketchLibrary(
    val sets: List<DrawingSet>,
) {
    fun selectedOrDefault(selectedSetId: String?): DrawingSet {
        return sets.firstOrNull { it.id == selectedSetId } ?: sets.first()
    }

    fun deleteDrawing(setId: String, drawingId: String): DrawingDeleteResult {
        val targetSet = sets.firstOrNull { it.id == setId } ?: selectedOrDefault(setId)
        val remainingDrawings = targetSet.drawings.filterNot { it.id == drawingId }

        val nextSets = if (remainingDrawings.isEmpty()) {
            sets.filterNot { it.id == targetSet.id }
        } else {
            sets.map { set ->
                if (set.id == targetSet.id) set.copy(drawings = remainingDrawings) else set
            }
        }.ifEmpty {
            listOf(DrawingSet.defaultSet())
        }

        val selectedSet = nextSets.firstOrNull { it.id == targetSet.id } ?: nextSets.first()
        return DrawingDeleteResult(
            library = copy(sets = nextSets),
            selectedSetId = selectedSet.id,
            selectedDrawingId = selectedSet.drawings.first().id,
        )
    }

    companion object {
        fun initial(): SketchLibrary = SketchLibrary(sets = listOf(DrawingSet.defaultSet()))
    }
}

data class DrawingDeleteResult(
    val library: SketchLibrary,
    val selectedSetId: String,
    val selectedDrawingId: String,
)
