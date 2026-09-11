package com.fedbaq.physicssketch.core.tools

enum class DrawingTool {
    Select,
    Pen,
    Eraser,
    Text,
    Angle,
    Shape,
}

data class ShapeTool(
    val id: String,
    val title: String,
)
