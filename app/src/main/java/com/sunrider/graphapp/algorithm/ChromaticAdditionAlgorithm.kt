package com.sunrider.graphapp.algorithm

import androidx.compose.ui.geometry.Offset
import com.sunrider.graphapp.model.Graph
import com.sunrider.graphapp.model.Polynomial

class ChromaticAdditionAlgorithm : GraphAlgorithm {

    override val name = "Хром. полином (добавление)"
    override val description = "Вычисление хроматического полинома методом добавления рёбер"

    override fun execute(graph: Graph, vertexPositions: Map<Int, Offset>): AlgorithmResult {
        val root = compute(graph, vertexPositions, "G", 0)
        val levels = buildLevels(root)
        val (chi, count) = computeChromaticInfo(root.polynomial, graph.vertexCount)
        return AlgorithmResult(root.polynomial, levels, chi, count)
    }

    private fun compute(
        graph: Graph,
        positions: Map<Int, Offset>,
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

        val leftLabel = label + "₁"
        val rightLabel = label + "₂"

        val leftNode = compute(addedGraph, positions, leftLabel, depth + 1)
        val rightNode = compute(contractedGraph, contractedPositions, rightLabel, depth + 1)

        val result = leftNode.polynomial + rightNode.polynomial

        return RecursionNode(
            label = label,
            graph = graph,
            positions = positions,
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
