package com.sunrider.graphapp.algorithm

import androidx.compose.ui.geometry.Offset
import com.sunrider.graphapp.model.Graph
import com.sunrider.graphapp.model.Polynomial
import com.sunrider.graphapp.model.toSuperscript

class ChromaticPolynomialAlgorithm : GraphAlgorithm {

    override val name = "Хром. полином (удаление)"
    override val description = "Вычисление хроматического полинома методом удаления-стягивания (две леммы)"

    override fun execute(graph: Graph, vertexPositions: Map<Int, Offset>): AlgorithmResult {
        val initialLabels = graph.vertices.associateWith { listOf(it) }
        val initialHistory = listOf(
            GraphHistoryStep(
                graph = graph,
                positions = vertexPositions,
                vertexLabels = initialLabels,
                description = "Исходный граф G"
            )
        )
        val root = compute(graph, vertexPositions, initialLabels, initialHistory, "G", 0)
        val levels = buildLevels(root)
        val (chi, count) = computeChromaticInfo(root.polynomial, graph.vertexCount)
        return AlgorithmResult(root.polynomial, levels, chi, count)
    }

    private fun compute(
        graph: Graph,
        positions: Map<Int, Offset>,
        vertexLabels: Map<Int, List<Int>>,
        history: List<GraphHistoryStep>,
        label: String,
        depth: Int
    ): RecursionNode {
        val n = graph.vertexCount

        // Base case: complete graph K_n
        if (graph.isComplete()) {
            val poly = if (n == 0) Polynomial.monomial(1, 0) else Polynomial.fallingFactorial(n)
            val desc = when (n) {
                0 -> "1 (пустой граф)"
                1 -> "x (O₁)"
                else -> {
                    val formula = (0 until n).joinToString("·") { i ->
                        if (i == 0) "x" else "(x-$i)"
                    }
                    "$formula (O$n)"
                }
            }
            return RecursionNode(
                label = label,
                graph = graph,
                positions = positions,
                vertexLabels = vertexLabels,
                history = history,
                depth = depth,
                polynomial = poly,
                isBaseCase = true,
                baseCaseDescription = desc
            )
        }

        // Base case: no edges (independent vertices)
        if (graph.edges.isEmpty()) {
            val poly = Polynomial.kPow(n)
            return RecursionNode(
                label = label,
                graph = graph,
                positions = positions,
                vertexLabels = vertexLabels,
                history = history,
                depth = depth,
                polynomial = poly,
                isBaseCase = true,
                baseCaseDescription = "x${n.toSuperscript()} (O$n)"
            )
        }

        // Decomposition: P(G, x) = P(G - e, x) - P(G / e, x)
        val edge = graph.edges.first()

        val deletedGraph = graph.removeEdge(edge)
        val contractedGraph = graph.contractEdge(edge)
        val contractedPositions = contractPositions(positions, edge)
        val contractedLabels = contractLabels(vertexLabels, edge)

        val leftLabel = label + "₁"
        val rightLabel = label + "₂"

        val leftHistory = history + GraphHistoryStep(
            graph = deletedGraph,
            positions = positions,
            vertexLabels = vertexLabels,
            description = "Удаляем ребро ${edge.from}-${edge.to} → $leftLabel",
            highlightedEdge = edge
        )
        val rightHistory = history + GraphHistoryStep(
            graph = contractedGraph,
            positions = contractedPositions,
            vertexLabels = contractedLabels,
            description = "Стягиваем ребро ${edge.from}-${edge.to} → $rightLabel",
            highlightedEdge = edge
        )

        val leftNode = compute(deletedGraph, positions, vertexLabels, leftHistory, leftLabel, depth + 1)
        val rightNode = compute(contractedGraph, contractedPositions, contractedLabels, rightHistory, rightLabel, depth + 1)

        val result = leftNode.polynomial - rightNode.polynomial

        return RecursionNode(
            label = label,
            graph = graph,
            positions = positions,
            vertexLabels = vertexLabels,
            history = history,
            depth = depth,
            polynomial = result,
            isBaseCase = false,
            edge = edge,
            leftChild = leftNode,
            rightChild = rightNode,
            operation = "-"
        )
    }
}
