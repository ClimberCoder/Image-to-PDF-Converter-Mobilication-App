package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DrawingOverlay(
    activeTool: AnnotationTool,
    drawings: List<AnnotationObject>,
    onDrawingsChanged: (List<AnnotationObject>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentStart by remember { mutableStateOf<Offset?>(null) }
    var currentEnd by remember { mutableStateOf<Offset?>(null) }
    
    val currentColor = if (activeTool == AnnotationTool.HIGHLIGHTER) Color.Yellow.copy(alpha = 0.4f) else Color.Black
    val currentWidth = if (activeTool == AnnotationTool.HIGHLIGHTER) 24f else 6f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeTool) {
                if (activeTool != AnnotationTool.NONE && activeTool != AnnotationTool.ERASER) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (activeTool == AnnotationTool.PEN || activeTool == AnnotationTool.PENCIL || activeTool == AnnotationTool.HIGHLIGHTER) {
                                val path = Path()
                                path.moveTo(offset.x, offset.y)
                                currentPath = path
                            } else {
                                currentStart = offset
                                currentEnd = offset
                            }
                        },
                        onDragEnd = {
                            if (currentPath != null) {
                                val newObj = AnnotationObject.Stroke(currentPath!!, currentColor, currentWidth, activeTool == AnnotationTool.HIGHLIGHTER)
                                onDrawingsChanged(drawings + newObj)
                                currentPath = null
                            } else if (currentStart != null && currentEnd != null) {
                                val s = currentStart!!
                                val e = currentEnd!!
                                val newObj = when (activeTool) {
                                    AnnotationTool.LINE -> AnnotationObject.Line(s, e, currentColor, currentWidth)
                                    AnnotationTool.ARROW -> AnnotationObject.Arrow(s, e, currentColor, currentWidth)
                                    AnnotationTool.RECTANGLE -> AnnotationObject.Rectangle(Rect(s, e), currentColor, currentWidth)
                                    AnnotationTool.CIRCLE -> AnnotationObject.Circle(s, (s - e).getDistance(), currentColor, currentWidth)
                                    else -> null
                                }
                                if (newObj != null) onDrawingsChanged(drawings + newObj)
                                currentStart = null
                                currentEnd = null
                            }
                        },
                        onDragCancel = {
                            currentPath = null
                            currentStart = null
                            currentEnd = null
                        },
                        onDrag = { change, _ ->
                            if (currentPath != null) {
                                currentPath?.lineTo(change.position.x, change.position.y)
                            } else {
                                currentEnd = change.position
                            }
                        }
                    )
                } else if (activeTool == AnnotationTool.ERASER) {
                    detectDragGestures { change, _ ->
                        // Phase 11: Smart Eraser - remove one object at a time.
                        // We check if the touch point intersects the object bounding box or path roughly.
                        val touch = change.position
                        val remaining = drawings.filterNot { obj ->
                            when (obj) {
                                is AnnotationObject.Stroke -> obj.path.getBounds().contains(touch) // Simple bounds check for now
                                is AnnotationObject.Line -> Rect(obj.start, obj.end).contains(touch)
                                is AnnotationObject.Arrow -> Rect(obj.start, obj.end).contains(touch)
                                is AnnotationObject.Rectangle -> obj.rect.contains(touch)
                                is AnnotationObject.Circle -> Rect(obj.center, obj.radius).contains(touch)
                                else -> false
                            }
                        }
                        if (remaining.size < drawings.size) {
                            onDrawingsChanged(remaining)
                        }
                    }
                }
            }
    ) {
        // Draw existing objects
        drawings.forEach { obj ->
            when (obj) {
                is AnnotationObject.Stroke -> {
                    drawPath(
                        path = obj.path,
                        color = obj.color,
                        style = Stroke(width = obj.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                is AnnotationObject.Line -> {
                    drawLine(color = obj.color, start = obj.start, end = obj.end, strokeWidth = obj.width, cap = StrokeCap.Round)
                }
                is AnnotationObject.Arrow -> {
                    drawLine(color = obj.color, start = obj.start, end = obj.end, strokeWidth = obj.width, cap = StrokeCap.Round)
                    // Draw arrowhead roughly
                    drawCircle(color = obj.color, radius = obj.width * 2, center = obj.end)
                }
                is AnnotationObject.Rectangle -> {
                    drawRect(color = obj.color, topLeft = obj.rect.topLeft, size = obj.rect.size, style = Stroke(width = obj.width))
                }
                is AnnotationObject.Circle -> {
                    drawCircle(color = obj.color, center = obj.center, radius = obj.radius, style = Stroke(width = obj.width))
                }
                else -> {}
            }
        }
        
        // Draw current preview
        if (currentPath != null) {
            drawPath(path = currentPath!!, color = currentColor, style = Stroke(width = currentWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else if (currentStart != null && currentEnd != null) {
            val s = currentStart!!
            val e = currentEnd!!
            when (activeTool) {
                AnnotationTool.LINE -> drawLine(color = currentColor, start = s, end = e, strokeWidth = currentWidth, cap = StrokeCap.Round)
                AnnotationTool.ARROW -> {
                    drawLine(color = currentColor, start = s, end = e, strokeWidth = currentWidth, cap = StrokeCap.Round)
                    drawCircle(color = currentColor, radius = currentWidth * 2, center = e)
                }
                AnnotationTool.RECTANGLE -> drawRect(color = currentColor, topLeft = Rect(s, e).topLeft, size = Rect(s, e).size, style = Stroke(width = currentWidth))
                AnnotationTool.CIRCLE -> drawCircle(color = currentColor, center = s, radius = (s - e).getDistance(), style = Stroke(width = currentWidth))
                else -> {}
            }
        }
    }
}
