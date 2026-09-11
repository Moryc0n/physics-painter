package com.fedbaq.physicssketch.core.tools

import com.fedbaq.physicssketch.core.geometry.SketchPoint
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.CircleObject
import com.fedbaq.physicssketch.core.model.DrawableObject
import com.fedbaq.physicssketch.core.model.LineObject
import com.fedbaq.physicssketch.core.model.RectObject
import java.util.UUID

object ShapeRegistry {
    const val ARROW = "arrow"
    const val CIRCLE = "circle"
    const val RECTANGLE = "rectangle"
    const val HORIZONTAL_LINE = "horizontal_line"
    const val VERTICAL_LINE = "vertical_line"
    const val DIAGONAL_LINE = "diagonal_line"

    private data class ShapeDefinition(
        val tool: ShapeTool,
        val factory: (String, SketchPoint, SketchPoint) -> DrawableObject,
    )

    private val definitions = listOf(
        ShapeDefinition(ShapeTool(ARROW, "Стрелка")) { id, start, end -> ArrowObject(id, start, end) },
        ShapeDefinition(ShapeTool(CIRCLE, "Окружность")) { id, start, end -> CircleObject.fromCorners(id, start, end) },
        ShapeDefinition(ShapeTool(RECTANGLE, "Квадрат")) { id, start, end -> RectObject(id, start, end) },
        ShapeDefinition(ShapeTool(HORIZONTAL_LINE, "Горизонтальная")) { id, start, end ->
            LineObject(id, start, SketchPoint(end.x, start.y))
        },
        ShapeDefinition(ShapeTool(VERTICAL_LINE, "Вертикальная")) { id, start, end ->
            LineObject(id, start, SketchPoint(start.x, end.y))
        },
        ShapeDefinition(ShapeTool(DIAGONAL_LINE, "Наклонная")) { id, start, end -> LineObject(id, start, end) },
    )

    val tools: List<ShapeTool> = definitions.map { it.tool }

    fun create(id: String, start: SketchPoint, end: SketchPoint): DrawableObject {
        val definition = definitions.firstOrNull { it.tool.id == id }
            ?: error("Unknown shape id: $id")
        return definition.factory(UUID.randomUUID().toString(), start, end)
    }
}
