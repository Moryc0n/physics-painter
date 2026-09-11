package com.fedbaq.physicssketch.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DrawingDocument(
    val id: String,
    val title: String,
    val objects: List<DrawableObject> = emptyList(),
) {
    fun addObject(item: DrawableObject): DrawingDocument = copy(objects = objects + item)

    fun removeObject(objectId: String): DrawingDocument = copy(objects = objects.filterNot { it.id == objectId })

    fun replaceObject(item: DrawableObject): DrawingDocument {
        return copy(objects = objects.map { existing -> if (existing.id == item.id) item else existing })
    }

    fun replaceObjectWith(objectId: String, replacements: List<DrawableObject>): DrawingDocument {
        return copy(objects = objects.flatMap { existing ->
            if (existing.id == objectId) replacements else listOf(existing)
        })
    }

    fun bringToFront(objectId: String): DrawingDocument {
        val target = objects.firstOrNull { it.id == objectId } ?: return this
        return copy(objects = objects.filterNot { it.id == objectId } + target)
    }
}
