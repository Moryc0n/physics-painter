package com.fedbaq.physicssketch.core.model

import com.fedbaq.physicssketch.core.geometry.SketchBounds
import com.fedbaq.physicssketch.core.geometry.SketchPoint
import kotlinx.serialization.Serializable

@Serializable
sealed interface DrawableObject {
    val id: String
    fun bounds(): SketchBounds
    fun hitTest(point: SketchPoint): Boolean
    fun translated(dx: Float, dy: Float): DrawableObject
    fun duplicate(newId: String = "${id}_copy"): DrawableObject
}
