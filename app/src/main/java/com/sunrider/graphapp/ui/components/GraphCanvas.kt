package com.sunrider.graphapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sunrider.graphapp.model.Edge
import com.sunrider.graphapp.model.Graph
import com.sunrider.graphapp.viewmodel.EditMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val VERTEX_RADIUS = 24f
private const val ARROW_SIZE = 14f

@Composable
fun GraphCanvas(
    graph: Graph,
    vertexPositions: Map<Int, Offset>,
    selectedVertex: Int?,
    highlightedEdge: Edge?,
    editMode: EditMode,
    canvasScale: Float,
    canvasOffset: Offset,
    mstEdges: Set<Edge> = emptySet(),
    isWeighted: Boolean = false,
    onTap: (Offset) -> Unit,
    onDragVertex: (Int, Offset) -> Unit,
    onTransformChange: (scale: Float, offset: Offset) -> Unit,
    findVertexAt: (Offset) -> Int?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var draggedVertex by remember { mutableStateOf<Int?>(null) }

    val latestScale = rememberUpdatedState(canvasScale)
    val latestOffset = rememberUpdatedState(canvasOffset)

    fun screenToWorld(p: Offset): Offset =
        (p - latestOffset.value) / latestScale.value

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (latestScale.value * zoomChange).coerceIn(0.1f, 10f)
        onTransformChange(newScale, latestOffset.value + panChange)
    }

    Canvas(
        modifier = modifier
            .background(Color(0xFFF5F5F5))
            .transformable(transformableState)
            .pointerInput(editMode) {
                when (editMode) {
                    EditMode.MOVE -> awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startVertex = findVertexAt(screenToWorld(down.position))

                        if (startVertex != null) {
                            draggedVertex = startVertex
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                onDragVertex(startVertex, screenToWorld(change.position))
                            }
                            draggedVertex = null
                        }
                    }

                    else -> detectTapGestures { offset ->
                        onTap(screenToWorld(offset))
                    }
                }
            }
    ) {
        withTransform({
            translate(canvasOffset.x, canvasOffset.y)
            scale(canvasScale, canvasScale, pivot = Offset.Zero)
        }) {
            drawGraph(graph, vertexPositions, selectedVertex, highlightedEdge, mstEdges, isWeighted)

            // Подписи вершин
            graph.vertices.forEach { id ->
                val pos = vertexPositions[id] ?: return@forEach
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(id.toString()),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        pos.x - textLayout.size.width / 2,
                        pos.y - textLayout.size.height / 2
                    )
                )
            }

            // Веса рёбер
            if (isWeighted) {
                graph.edges.forEach { edge ->
                    val from = vertexPositions[edge.from] ?: return@forEach
                    val to = vertexPositions[edge.to] ?: return@forEach
                    val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
                    val dx = to.x - from.x
                    val dy = to.y - from.y
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    // Смещение перпендикулярно ребру
                    val offsetX = -dy / len * 12f
                    val offsetY = dx / len * 12f
                    val weight = graph.getWeight(edge)
                    val isMst = edge in mstEdges
                    val weightLayout = textMeasurer.measure(
                        text = AnnotatedString(weight.toString()),
                        style = TextStyle(
                            color = if (isMst) Color(0xFF2E7D32) else Color(0xFF424242),
                            fontSize = 11.sp
                        )
                    )
                    drawText(
                        textLayoutResult = weightLayout,
                        topLeft = Offset(
                            mid.x + offsetX - weightLayout.size.width / 2,
                            mid.y + offsetY - weightLayout.size.height / 2
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGraph(
    graph: Graph,
    positions: Map<Int, Offset>,
    selectedVertex: Int?,
    highlightedEdge: Edge?,
    mstEdges: Set<Edge>,
    isWeighted: Boolean
) {
    graph.edges.forEach { edge ->
        val from = positions[edge.from] ?: return@forEach
        val to = positions[edge.to] ?: return@forEach
        val isHighlighted = highlightedEdge == edge
        val isMst = edge in mstEdges

        val edgeColor = when {
            isHighlighted -> Color(0xFFE53935)
            isMst -> Color(0xFF4CAF50)
            else -> Color(0xFF757575)
        }
        val strokeWidth = when {
            isHighlighted -> 5f
            isMst -> 4f
            else -> 2.5f
        }

        if (graph.isDirected) {
            drawArrowEdge(from, to, edgeColor, strokeWidth)
        } else {
            drawLine(
                color = edgeColor,
                start = from,
                end = to,
                strokeWidth = strokeWidth
            )
        }
    }

    graph.vertices.forEach { id ->
        val pos = positions[id] ?: return@forEach
        val isSelected = id == selectedVertex
        drawCircle(
            color = if (isSelected) Color(0xFFFFC107) else Color(0xFF1E88E5),
            radius = VERTEX_RADIUS,
            center = pos
        )
        drawCircle(
            color = if (isSelected) Color(0xFFFF8F00) else Color(0xFF1565C0),
            radius = VERTEX_RADIUS,
            center = pos,
            style = Stroke(width = 2.5f)
        )
    }
}

private fun DrawScope.drawArrowEdge(
    from: Offset,
    to: Offset,
    color: Color,
    strokeWidth: Float
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 1f) return

    val ux = dx / dist
    val uy = dy / dist

    // Укорачиваем линию до границы вершины
    val endX = to.x - ux * VERTEX_RADIUS
    val endY = to.y - uy * VERTEX_RADIUS
    val startX = from.x + ux * VERTEX_RADIUS
    val startY = from.y + uy * VERTEX_RADIUS

    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth
    )

    // Стрелка
    val angle = atan2(dy.toDouble(), dx.toDouble())
    val arrowAngle = PI / 6
    val ax1 = endX - ARROW_SIZE * cos(angle - arrowAngle).toFloat()
    val ay1 = endY - ARROW_SIZE * sin(angle - arrowAngle).toFloat()
    val ax2 = endX - ARROW_SIZE * cos(angle + arrowAngle).toFloat()
    val ay2 = endY - ARROW_SIZE * sin(angle + arrowAngle).toFloat()

    val path = Path().apply {
        moveTo(endX, endY)
        lineTo(ax1, ay1)
        lineTo(ax2, ay2)
        close()
    }
    drawPath(path, color)
}

@Composable
fun GraphPreview(
    graph: Graph,
    vertexPositions: Map<Int, Offset> = emptyMap(),
    highlightedEdge: Edge? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.background(Color(0xFFEEEEEE))
    ) {
        if (graph.vertices.isEmpty()) return@Canvas

        val sourcePositions = if (vertexPositions.isNotEmpty()) vertexPositions
        else autoLayout(graph, size.width, size.height)
        val positions = scaleToFit(sourcePositions, size.width, size.height, padding = 10f)
        val r = 8f

        graph.edges.forEach { edge ->
            val from = positions[edge.from] ?: return@forEach
            val to = positions[edge.to] ?: return@forEach
            val isHighlighted = highlightedEdge == edge
            drawLine(
                color = if (isHighlighted) Color(0xFFE53935) else Color(0xFF9E9E9E),
                start = from,
                end = to,
                strokeWidth = if (isHighlighted) 3f else 1.5f
            )
        }

        graph.vertices.forEach { id ->
            val pos = positions[id] ?: return@forEach
            drawCircle(color = Color(0xFF1E88E5), radius = r, center = pos)
        }
    }
}

fun scaleToFit(
    positions: Map<Int, Offset>,
    width: Float,
    height: Float,
    padding: Float = 10f
): Map<Int, Offset> {
    if (positions.isEmpty()) return emptyMap()
    if (positions.size == 1) return positions.mapValues { Offset(width / 2, height / 2) }

    val xs = positions.values.map { it.x }
    val ys = positions.values.map { it.y }
    val minX = xs.min(); val maxX = xs.max()
    val minY = ys.min(); val maxY = ys.max()
    val rangeX = (maxX - minX).coerceAtLeast(1f)
    val rangeY = (maxY - minY).coerceAtLeast(1f)
    val scale = minOf((width - 2 * padding) / rangeX, (height - 2 * padding) / rangeY)
    val srcCx = (minX + maxX) / 2
    val srcCy = (minY + maxY) / 2
    return positions.mapValues { (_, pos) ->
        Offset(width / 2 + (pos.x - srcCx) * scale, height / 2 + (pos.y - srcCy) * scale)
    }
}

private fun autoLayout(graph: Graph, width: Float, height: Float): Map<Int, Offset> {
    val vertices = graph.vertices.sorted()
    if (vertices.isEmpty()) return emptyMap()
    if (vertices.size == 1) return mapOf(vertices[0] to Offset(width / 2, height / 2))
    val cx = width / 2; val cy = height / 2
    val radius = minOf(width, height) / 2 - 16f
    return vertices.mapIndexed { index, id ->
        val angle = 2.0 * PI * index / vertices.size - PI / 2
        id to Offset(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat())
    }.toMap()
}
