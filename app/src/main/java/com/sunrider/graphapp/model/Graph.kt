package com.sunrider.graphapp.model

data class Edge(val from: Int, val to: Int) {
    fun contains(vertex: Int) = from == vertex || to == vertex
    fun other(vertex: Int) = if (from == vertex) to else from
    override fun toString(): String = "($from, $to)"
}

data class Graph(
    val vertices: Set<Int> = emptySet(),
    val edges: Set<Edge> = emptySet(),
    val isDirected: Boolean = false,
    val weights: Map<Edge, Int> = emptyMap()
) {
    val vertexCount get() = vertices.size
    val edgeCount get() = edges.size

    fun normalizeEdge(from: Int, to: Int): Edge =
        if (isDirected) Edge(from, to) else Edge(minOf(from, to), maxOf(from, to))

    fun normalizeEdge(edge: Edge): Edge =
        if (isDirected) edge else Edge(minOf(edge.from, edge.to), maxOf(edge.from, edge.to))

    fun addVertex(id: Int) = copy(vertices = vertices + id)

    fun removeVertex(id: Int) = copy(
        vertices = vertices - id,
        edges = edges.filter { !it.contains(id) }.toSet(),
        weights = weights.filter { !it.key.contains(id) }
    )

    fun addEdge(from: Int, to: Int, weight: Int = 1): Graph {
        if (from == to || from !in vertices || to !in vertices) return this
        val edge = normalizeEdge(from, to)
        if (edge in edges) return this
        return copy(
            edges = edges + edge,
            weights = weights + (edge to weight)
        )
    }

    fun removeEdge(edge: Edge): Graph {
        val normalized = normalizeEdge(edge)
        return copy(
            edges = edges - normalized,
            weights = weights - normalized
        )
    }

    fun hasEdge(from: Int, to: Int): Boolean {
        val edge = normalizeEdge(from, to)
        return edge in edges
    }

    fun getWeight(edge: Edge): Int = weights[normalizeEdge(edge)] ?: 1
    fun getWeight(from: Int, to: Int): Int = weights[normalizeEdge(from, to)] ?: 1

    fun setWeight(edge: Edge, weight: Int): Graph {
        val normalized = normalizeEdge(edge)
        return copy(weights = weights + (normalized to weight))
    }

    fun neighbors(vertex: Int): Set<Int> =
        edges.filter { it.contains(vertex) }.map { it.other(vertex) }.toSet()

    fun outNeighbors(vertex: Int): Set<Int> =
        edges.filter { it.from == vertex }.map { it.to }.toSet()

    fun contractEdge(edge: Edge): Graph {
        val normalized = normalizeEdge(edge)
        val keep = minOf(edge.from, edge.to)
        val remove = maxOf(edge.from, edge.to)
        val newVertices = vertices - remove
        val newEdges = edges
            .filter { it != normalized }
            .map { e ->
                normalizeEdge(
                    if (e.from == remove) keep else e.from,
                    if (e.to == remove) keep else e.to
                )
            }
            .filter { it.from != it.to }
            .toSet()
        return copy(vertices = newVertices, edges = newEdges, weights = emptyMap())
    }

    fun isComplete(): Boolean {
        if (vertices.size <= 1) return true
        val n = vertices.size
        return edgeCount == n * (n - 1) / 2
    }

    fun findNonEdge(): Edge? {
        val vList = vertices.sorted()
        for (i in vList.indices) {
            for (j in i + 1 until vList.size) {
                if (!hasEdge(vList[i], vList[j])) {
                    return normalizeEdge(vList[i], vList[j])
                }
            }
        }
        return null
    }

    fun getAdjacencyMatrix(): Pair<List<Int>, List<List<Int>>> {
        val sortedVertices = vertices.sorted()
        val matrix = sortedVertices.map { i ->
            sortedVertices.map { j ->
                if (hasEdge(i, j)) 1 else 0
            }
        }
        return sortedVertices to matrix
    }

    fun getWeightedAdjacencyMatrix(): Pair<List<Int>, List<List<Int>>> {
        val sortedVertices = vertices.sorted()
        val matrix = sortedVertices.map { i ->
            sortedVertices.map { j ->
                if (hasEdge(i, j)) getWeight(i, j) else 0
            }
        }
        return sortedVertices to matrix
    }

    fun isConnected(): Boolean {
        if (vertices.isEmpty()) return true
        val visited = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(vertices.first())
        visited.add(vertices.first())
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            for (n in neighbors(v)) {
                if (n !in visited) {
                    visited.add(n)
                    queue.add(n)
                }
            }
        }
        return visited.size == vertices.size
    }
}
