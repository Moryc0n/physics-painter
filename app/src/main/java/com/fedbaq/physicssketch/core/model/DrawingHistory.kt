package com.fedbaq.physicssketch.core.model

data class DrawingHistory(
    val current: DrawingDocument,
    private val undoStack: List<DrawingDocument> = emptyList(),
    private val redoStack: List<DrawingDocument> = emptyList(),
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(next: DrawingDocument): DrawingHistory {
        if (next == current) return this
        return copy(
            current = next,
            undoStack = (undoStack + current).takeLast(MAX_HISTORY),
            redoStack = emptyList(),
        )
    }

    fun undo(): DrawingHistory {
        val previous = undoStack.lastOrNull() ?: return this
        return copy(
            current = previous,
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + current,
        )
    }

    fun redo(): DrawingHistory {
        val next = redoStack.lastOrNull() ?: return this
        return copy(
            current = next,
            undoStack = undoStack + current,
            redoStack = redoStack.dropLast(1),
        )
    }

    companion object {
        private const val MAX_HISTORY = 80
    }
}
