package com.sunrider.graphapp.model

data class Edge(val from: Int, val to: Int) {
    fun contains(vertex: Int) = from == vertex || to == vertex
    fun other(vertex: Int) = if (from == vertex) to else from

    override fun equals(other: Any?): Boolean {
        if (other !is Edge) return false
        return (from == other.from && to == other.to) || (from == other.to && to == other.from)
    }

    override fun hashCode(): Int = minOf(from, to) * 31 + maxOf(from, to)
    override fun toString(): String = "($from, $to)"
}

data class Graph(
    val vertices: Set<Int> = emptySet(),
    val edges: Set<Edge> = emptySet()
) {
    val vertexCount get() = vertices.size
    val edgeCount get() = edges.size

    fun addVertex(id: Int) = copy(vertices = vertices + id)

    fun removeVertex(id: Int) = Graph(
        vertices = vertices - id,
        edges = edges.filter { !it.contains(id) }.toSet()
    )

    fun addEdge(from: Int, to: Int): Graph {
        if (from == to || from !in vertices || to !in vertices) return this
        val edge = Edge(from, to)
        if (edge in edges) return this
        return copy(edges = edges + edge)
    }

    fun removeEdge(edge: Edge) = copy(edges = edges - edge)

    fun hasEdge(from: Int, to: Int) = Edge(from, to) in edges

    fun neighbors(vertex: Int): Set<Int> =
        edges.filter { it.contains(vertex) }.map { it.other(vertex) }.toSet()

    fun contractEdge(edge: Edge): Graph {
        val keep = minOf(edge.from, edge.to)
        val remove = maxOf(edge.from, edge.to)
        val newVertices = vertices - remove
        val newEdges = edges
            .filter { it != edge }
            .map { e ->
                Edge(
                    if (e.from == remove) keep else e.from,
                    if (e.to == remove) keep else e.to
                )
            }
            .filter { it.from != it.to }
            .toSet()
        return Graph(newVertices, newEdges)
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
                    return Edge(vList[i], vList[j])
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
