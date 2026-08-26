package com.example.ui.components

data class DrawingHistory(
    val history: List<List<AnnotationObject>> = listOf(emptyList()),
    val currentIndex: Int = 0
) {
    val current: List<AnnotationObject> get() = history[currentIndex]
    
    fun add(drawings: List<AnnotationObject>): DrawingHistory {
        val newHistory = history.take(currentIndex + 1) + listOf(drawings)
        return copy(history = newHistory, currentIndex = newHistory.size - 1)
    }
    
    fun undo(): DrawingHistory = copy(currentIndex = maxOf(0, currentIndex - 1))
    
    fun redo(): DrawingHistory = copy(currentIndex = minOf(history.size - 1, currentIndex + 1))
    
    val canUndo: Boolean get() = currentIndex > 0
    val canRedo: Boolean get() = currentIndex < history.size - 1
}
