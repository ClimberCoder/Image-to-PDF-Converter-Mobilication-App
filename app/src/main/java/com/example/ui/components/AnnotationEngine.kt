package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

enum class AnnotationTool {
    NONE, PEN, PENCIL, HIGHLIGHTER, ERASER, TEXT, COMMENT, ARROW, LINE, RECTANGLE, CIRCLE
}

sealed class AnnotationObject {
    abstract val color: Color
    abstract val width: Float
    
    data class Stroke(val path: Path, override val color: Color, override val width: Float, val isHighlighter: Boolean = false) : AnnotationObject()
    data class Line(val start: Offset, val end: Offset, override val color: Color, override val width: Float) : AnnotationObject()
    data class Arrow(val start: Offset, val end: Offset, override val color: Color, override val width: Float) : AnnotationObject()
    data class Rectangle(val rect: Rect, override val color: Color, override val width: Float) : AnnotationObject()
    data class Circle(val center: Offset, val radius: Float, override val color: Color, override val width: Float) : AnnotationObject()
    data class TextBox(val text: String, val position: Offset, override val color: Color, val fontSize: Float) : AnnotationObject() {
        override val width = 0f
    }
}
