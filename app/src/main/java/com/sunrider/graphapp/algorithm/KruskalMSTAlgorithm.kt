package com.sunrider.graphapp.algorithm

import com.sunrider.graphapp.model.Edge
import com.sunrider.graphapp.model.Graph

data class MSTResult(
    val edges: Set<Edge>,
    val totalWeight: Int
)

class KruskalMSTAlgorithm {

    fun execute(graph: Graph): MSTResult? {
        if (graph.vertices.size < 2) return null
        if (!graph.isConnected()) return null

        val parent = mutableMapOf<Int, Int>()
        val rank = mutableMapOf<Int, Int>()
        graph.vertices.forEach {
            parent[it] = it
            rank[it] = 0
        }

        fun find(x: Int): Int {
            if (parent[x] != x) parent[x] = find(parent[x]!!)
            return parent[x]!!
        }

        fun union(a: Int, b: Int): Boolean {
            val ra = find(a)
            val rb = find(b)
            if (ra == rb) return false
            when {
                rank[ra]!! < rank[rb]!! -> parent[ra] = rb
                rank[ra]!! > rank[rb]!! -> parent[rb] = ra
                else -> {
                    parent[rb] = ra
                    rank[ra] = rank[ra]!! + 1
                }
            }
            return true
        }

        val sortedEdges = graph.edges.sortedBy { graph.getWeight(it) }
        val mstEdges = mutableSetOf<Edge>()
        var totalWeight = 0

        for (edge in sortedEdges) {
            if (union(edge.from, edge.to)) {
                mstEdges.add(edge)
                totalWeight += graph.getWeight(edge)
                if (mstEdges.size == graph.vertexCount - 1) break
            }
        }

        return if (mstEdges.size == graph.vertexCount - 1) {
            MSTResult(mstEdges, totalWeight)
        } else null
    }
}
