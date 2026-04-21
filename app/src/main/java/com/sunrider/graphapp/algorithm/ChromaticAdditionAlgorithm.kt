package com.sunrider.graphapp.algorithm

import androidx.compose.ui.geometry.Offset
import com.sunrider.graphapp.model.Graph
import com.sunrider.graphapp.model.Polynomial

class ChromaticAdditionAlgorithm : GraphAlgorithm {

    override val name = "Хром. полином (добавление)"
    override val description = "Вычисление хроматического полинома методом добавления рёбер"

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
            val desc = if (n == 0) {
                "1 (пустой граф)"
            } else {
                val formula = (0 until n).joinToString("·") { i ->
                    if (i == 0) "x" else "(x-$i)"
                }
                "$formula (K$n)"
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

        // Decomposition: P(G, x) = P(G + e, x) + P(G / e, x)
        val nonEdge = graph.findNonEdge()!!

        val addedGraph = graph.addEdge(nonEdge.from, nonEdge.to)
        val contractedGraph = graph.contractEdge(nonEdge)
        val contractedPositions = contractPositions(positions, nonEdge)
        val contractedLabels = contractLabels(vertexLabels, nonEdge)

        val leftLabel = label + "₁"
        val rightLabel = label + "₂"

        val leftHistory = history + GraphHistoryStep(
            graph = addedGraph,
            positions = positions,
            vertexLabels = vertexLabels,
            description = "Добавляем ребро ${nonEdge.from}-${nonEdge.to} → $leftLabel",
            highlightedEdge = nonEdge
        )
        val rightHistory = history + GraphHistoryStep(
            graph = contractedGraph,
            positions = contractedPositions,
            vertexLabels = contractedLabels,
            description = "Стягиваем ребро ${nonEdge.from}-${nonEdge.to} → $rightLabel",
            highlightedEdge = nonEdge
        )

        val leftNode = compute(addedGraph, positions, vertexLabels, leftHistory, leftLabel, depth + 1)
        val rightNode = compute(contractedGraph, contractedPositions, contractedLabels, rightHistory, rightLabel, depth + 1)

        val result = leftNode.polynomial + rightNode.polynomial

        return RecursionNode(
            label = label,
            graph = graph,
            positions = positions,
            vertexLabels = vertexLabels,
            history = history,
            depth = depth,
            polynomial = result,
            isBaseCase = false,
            edge = nonEdge,
            leftChild = leftNode,
            rightChild = rightNode,
            operation = "+"
        )
    }
}
