package com.sunrider.graphapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.sunrider.graphapp.algorithm.AlgorithmResult
import com.sunrider.graphapp.algorithm.ChromaticAdditionAlgorithm
import com.sunrider.graphapp.algorithm.ChromaticPolynomialAlgorithm
import com.sunrider.graphapp.algorithm.GraphAlgorithm
import com.sunrider.graphapp.algorithm.KruskalMSTAlgorithm
import com.sunrider.graphapp.algorithm.MSTResult
import com.sunrider.graphapp.model.Edge
import com.sunrider.graphapp.model.Graph

enum class EditMode { ADD_VERTEX, ADD_EDGE, DELETE, MOVE }

class GraphViewModel : ViewModel() {

    var graph by mutableStateOf(Graph())
        private set

    var vertexPositions by mutableStateOf<Map<Int, Offset>>(emptyMap())
        private set

    var editMode by mutableStateOf(EditMode.ADD_VERTEX)

    var selectedVertex by mutableStateOf<Int?>(null)
        private set

    var algorithmResult by mutableStateOf<AlgorithmResult?>(null)
        private set

    var currentLevelIndex by mutableIntStateOf(0)
        private set

    var showResult by mutableStateOf(false)
        private set

    var showAdjacencyMatrix by mutableStateOf(false)
        private set

    var canvasScale by mutableFloatStateOf(1f)
        private set

    var canvasOffset by mutableStateOf(Offset.Zero)
        private set

    var isDirected by mutableStateOf(false)
        private set

    var isWeighted by mutableStateOf(false)
        private set

    var mstResult by mutableStateOf<MSTResult?>(null)
        private set

    var showMstResult by mutableStateOf(false)
        private set

    var editingWeightEdge by mutableStateOf<Edge?>(null)
        private set

    private var nextVertexId = 1

    fun updateCanvasTransform(scale: Float, offset: Offset) {
        canvasScale = scale.coerceIn(0.1f, 10f)
        canvasOffset = offset
    }

    fun resetCanvasTransform() {
        canvasScale = 1f
        canvasOffset = Offset.Zero
    }

    val algorithms: List<GraphAlgorithm> = listOf(
        ChromaticPolynomialAlgorithm(),
        ChromaticAdditionAlgorithm()
    )

    fun toggleDirected() {
        if (graph.edges.isNotEmpty()) return // нельзя менять тип если есть рёбра
        isDirected = !isDirected
        graph = graph.copy(isDirected = isDirected)
        clearMstResult()
    }

    fun toggleWeighted() {
        isWeighted = !isWeighted
        clearMstResult()
    }

    fun addVertex(position: Offset) {
        val id = nextVertexId++
        graph = graph.addVertex(id)
        vertexPositions = vertexPositions + (id to position)
    }

    fun onVertexTap(id: Int) {
        when (editMode) {
            EditMode.ADD_EDGE -> {
                val sel = selectedVertex
                if (sel == null || sel == id) {
                    selectedVertex = if (sel == id) null else id
                } else {
                    graph = graph.addEdge(sel, id)
                    selectedVertex = null
                    clearMstResult()
                }
            }
            EditMode.DELETE -> deleteVertex(id)
            else -> {}
        }
    }

    fun deleteVertex(id: Int) {
        graph = graph.removeVertex(id)
        vertexPositions = vertexPositions - id
        if (selectedVertex == id) selectedVertex = null
        clearMstResult()
    }

    fun deleteEdge(edge: Edge) {
        graph = graph.removeEdge(edge)
        clearMstResult()
    }

    fun moveVertex(id: Int, newPosition: Offset) {
        vertexPositions = vertexPositions + (id to newPosition)
    }

    fun startEditWeight(edge: Edge) {
        editingWeightEdge = edge
    }

    fun confirmEditWeight(weight: Int) {
        val edge = editingWeightEdge ?: return
        if (weight > 0) {
            graph = graph.setWeight(edge, weight)
            clearMstResult()
        }
        editingWeightEdge = null
    }

    fun cancelEditWeight() {
        editingWeightEdge = null
    }

    fun clearGraph() {
        graph = Graph(isDirected = isDirected)
        vertexPositions = emptyMap()
        selectedVertex = null
        nextVertexId = 1
        algorithmResult = null
        showResult = false
        currentLevelIndex = 0
        clearMstResult()
        resetCanvasTransform()
    }

    fun executeAlgorithm(algorithm: GraphAlgorithm) {
        if (graph.vertices.isEmpty()) return
        if (graph.isDirected) return // хроматические алгоритмы только для ненаправленных
        algorithmResult = algorithm.execute(graph, vertexPositions)
        currentLevelIndex = 0
        showResult = true
    }

    fun executeMST() {
        if (graph.vertices.size < 2) return
        if (graph.isDirected) return // MST только для ненаправленных
        val result = KruskalMSTAlgorithm().execute(graph)
        mstResult = result
        showMstResult = result != null
    }

    fun clearMstResult() {
        mstResult = null
        showMstResult = false
    }

    fun nextLevel() {
        algorithmResult?.let {
            if (currentLevelIndex < it.levels.size - 1) currentLevelIndex++
        }
    }

    fun previousLevel() {
        if (currentLevelIndex > 0) currentLevelIndex--
    }

    fun goToLevel(index: Int) {
        algorithmResult?.let {
            if (index in it.levels.indices) currentLevelIndex = index
        }
    }

    fun hideResult() {
        showResult = false
    }

    fun toggleAdjacencyMatrix() {
        showAdjacencyMatrix = !showAdjacencyMatrix
    }

    fun applyAdjacencyMatrix(size: Int, matrix: List<List<Int>>, isWeightedInput: Boolean, isDirectedInput: Boolean) {
        // Determine starting vertex ID (continue from existing)
        val startId = if (graph.vertices.isEmpty()) 1 else (graph.vertices.max() + 1)
        val ids = (startId until startId + size).toList()

        // Update directed/weighted flags if graph is empty
        if (graph.vertices.isEmpty()) {
            isDirected = isDirectedInput
            isWeighted = isWeightedInput
            graph = graph.copy(isDirected = isDirectedInput)
        } else if (isWeightedInput) {
            isWeighted = true
        }

        // Add vertices
        var g = graph
        for (id in ids) {
            g = g.addVertex(id)
        }

        // Add edges from matrix
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = matrix[i][j]
                if (value > 0 && i != j) {
                    if (!isDirectedInput && j < i) continue // avoid duplicates for undirected
                    g = g.addEdge(ids[i], ids[j], value)
                }
            }
        }
        graph = g

        // Update nextVertexId
        nextVertexId = (graph.vertices.maxOrNull() ?: 0) + 1

        // Auto-layout new vertices in a circle
        val existingPositions = vertexPositions.toMutableMap()
        val newIds = ids.filter { it !in existingPositions }
        if (newIds.isNotEmpty()) {
            val cx = 500f
            val cy = 500f
            val radius = 300f
            val angleStep = 2.0 * kotlin.math.PI / newIds.size
            newIds.forEachIndexed { index, id ->
                val angle = angleStep * index - kotlin.math.PI / 2
                existingPositions[id] = androidx.compose.ui.geometry.Offset(
                    (cx + radius * kotlin.math.cos(angle)).toFloat(),
                    (cy + radius * kotlin.math.sin(angle)).toFloat()
                )
            }
            vertexPositions = existingPositions
        }

        clearMstResult()
    }

    fun findVertexAt(position: Offset, radius: Float = 50f): Int? {
        return vertexPositions.entries.minByOrNull { (_, pos) ->
            (pos - position).getDistance()
        }?.let { (id, pos) ->
            if ((pos - position).getDistance() <= radius) id else null
        }
    }

    fun findEdgeAt(position: Offset, threshold: Float = 30f): Edge? {
        return graph.edges.minByOrNull { edge ->
            distanceToEdge(position, edge)
        }?.let { edge ->
            if (distanceToEdge(position, edge) <= threshold) edge else null
        }
    }

    private fun distanceToEdge(point: Offset, edge: Edge): Float {
        val from = vertexPositions[edge.from] ?: return Float.MAX_VALUE
        val to = vertexPositions[edge.to] ?: return Float.MAX_VALUE

        val dx = to.x - from.x
        val dy = to.y - from.y
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return (point - from).getDistance()

        val t = ((point.x - from.x) * dx + (point.y - from.y) * dy) / lenSq
        val clamped = t.coerceIn(0f, 1f)
        val closest = Offset(from.x + clamped * dx, from.y + clamped * dy)
        return (point - closest).getDistance()
    }
}
