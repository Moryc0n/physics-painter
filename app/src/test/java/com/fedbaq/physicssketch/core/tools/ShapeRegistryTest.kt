package com.fedbaq.physicssketch.core.tools

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.CircleObject
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeRegistryTest {
    @Test
    fun createsRegisteredArrowShape() {
        val created = ShapeRegistry.create(
            id = ShapeRegistry.ARROW,
            start = SketchPoint(0f, 0f),
            end = SketchPoint(100f, 50f),
        )

        assertTrue(created is ArrowObject)
    }

    @Test
    fun createsRegisteredCircleShape() {
        val created = ShapeRegistry.create(
            id = ShapeRegistry.CIRCLE,
            start = SketchPoint(10f, 20f),
            end = SketchPoint(50f, 70f),
        )

        assertTrue(created is CircleObject)
    }
}
