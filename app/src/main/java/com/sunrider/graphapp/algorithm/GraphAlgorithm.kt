package com.sunrider.graphapp.algorithm

import androidx.compose.ui.geometry.Offset
import com.sunrider.graphapp.model.Edge
import com.sunrider.graphapp.model.Graph
import com.sunrider.graphapp.model.Polynomial

data class GraphHistoryStep(
    val graph: Graph,
    val positions: Map<Int, Offset>,
    val vertexLabels: Map<Int, List<Int>>,
    val description: String,
    val highlightedEdge: Edge? = null
)

data class RecursionNode(
    val label: String,
    val graph: Graph,
    val positions: Map<Int, Offset>,
    val vertexLabels: Map<Int, List<Int>>,
    val history: List<GraphHistoryStep>,
    val depth: Int,
    val polynomial: Polynomial,
    val isBaseCase: Boolean,
    val baseCaseDescription: String = "",
    val edge: Edge? = null,
    val leftChild: RecursionNode? = null,
    val rightChild: RecursionNode? = null,
    val operation: String = "-"
)

data class LevelEntry(
    val label: String,
    val graph: Graph,
    val positions: Map<Int, Offset>,
    val vertexLabels: Map<Int, List<Int>>,
    val history: List<GraphHistoryStep>,
    val edge: Edge? = null,
    val isBaseCase: Boolean,
    val formulaText: String,
    val detailText: String = "",
    val polynomial: Polynomial
)

data class RecursionLevel(
    val depth: Int,
    val isBackSubstitution: Boolean = false,
    val entries: List<LevelEntry>
)

data class AlgorithmResult(
    val polynomial: Polynomial,
    val levels: List<RecursionLevel>,
    val chromaticNumber: Int,
    val coloringCount: Long
)

/** Форматирует подпись вершины: если к ней слиты несколько исходных id — показывает "(1,2)". */
fun formatVertexLabel(vertexId: Int, labels: Map<Int, List<Int>>): String {
    if (labels.isEmpty()) return vertexId.toString()
    val ids = labels[vertexId] ?: listOf(vertexId)
    return if (ids.size <= 1) (ids.firstOrNull() ?: vertexId).toString()
    else "(${ids.joinToString(",")})"
}

/** Обновляет карту меток вершин при стягивании ребра: все исходные id объединяются в оставшейся вершине. */
fun contractLabels(labels: Map<Int, List<Int>>, edge: Edge): Map<Int, List<Int>> {
    val keep = minOf(edge.from, edge.to)
    val remove = maxOf(edge.from, edge.to)
    val keepLabels = labels[keep] ?: listOf(keep)
    val removeLabels = labels[remove] ?: listOf(remove)
    val merged = (keepLabels + removeLabels).distinct().sorted()
    return (labels - remove) + (keep to merged)
}

fun computeChromaticInfo(polynomial: Polynomial, maxVertices: Int): Pair<Int, Long> {
    if (polynomial.isZero) return Pair(0, 0L)
    for (k in 1..maxOf(maxVertices, 1)) {
        val value = polynomial.evaluate(k.toLong())
        if (value > 0) return Pair(k, value)
    }
    return Pair(maxVertices, polynomial.evaluate(maxVertices.toLong()))
}

interface GraphAlgorithm {
    val name: String
    val description: String
    fun execute(graph: Graph, vertexPositions: Map<Int, Offset>): AlgorithmResult
}

fun contractPositions(
    positions: Map<Int, Offset>,
    edge: Edge
): Map<Int, Offset> {
    val keep = minOf(edge.from, edge.to)
    val remove = maxOf(edge.from, edge.to)
    val posKeep = positions[keep]
    val posRemove = positions[remove]
    val merged = posKeep ?: posRemove ?: Offset.Zero
    return (positions - remove) + (keep to merged)
}

/** Рекурсивно собирает все листовые (базовые) узлы с накопленным знаком (+1 или -1). */
fun collectLeavesWithSigns(node: RecursionNode, sign: Int = 1): List<Pair<RecursionNode, Int>> {
    if (node.isBaseCase) return listOf(node to sign)
    val rightSign = if (node.operation == "-") -sign else sign
    return collectLeavesWithSigns(node.leftChild!!, sign) +
            collectLeavesWithSigns(node.rightChild!!, rightSign)
}

/** Извлекает краткое имя типа графа из baseCaseDescription, например "K3" или "2 независ. вершин". */
fun extractGraphLabel(node: RecursionNode): String {
    val desc = node.baseCaseDescription
    val parenStart = desc.lastIndexOf('(')
    val parenEnd = desc.lastIndexOf(')')
    return if (parenStart >= 0 && parenEnd > parenStart)
        desc.substring(parenStart + 1, parenEnd)
    else
        node.label
}

fun buildLevels(root: RecursionNode): List<RecursionLevel> {
    val nodesByDepth = mutableMapOf<Int, MutableList<RecursionNode>>()
    val queue: ArrayDeque<RecursionNode> = ArrayDeque()
    queue.add(root)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        nodesByDepth.getOrPut(node.depth) { mutableListOf() }.add(node)
        node.leftChild?.let { queue.add(it) }
        node.rightChild?.let { queue.add(it) }
    }

    val levels = mutableListOf<RecursionLevel>()
    val maxDepth = nodesByDepth.keys.maxOrNull() ?: 0

    // Decomposition levels (top-down)
    for (depth in 0..maxDepth) {
        val nodes = nodesByDepth[depth] ?: continue
        val entries = nodes.map { node ->
            if (node.isBaseCase) {
                LevelEntry(
                    label = node.label,
                    graph = node.graph,
                    positions = node.positions,
                    vertexLabels = node.vertexLabels,
                    history = node.history,
                    isBaseCase = true,
                    formulaText = "Граф ${node.label} — базовый случай",
                    detailText = node.baseCaseDescription,
                    polynomial = node.polynomial
                )
            } else {
                val left = node.leftChild!!
                val right = node.rightChild!!
                val operationDesc = if (node.operation == "-") "удаляем" else "добавляем"
                val leftDesc = if (node.operation == "-") "без ребра → ${left.label}" else "с ребром → ${left.label}"
                LevelEntry(
                    label = node.label,
                    graph = node.graph,
                    positions = node.positions,
                    vertexLabels = node.vertexLabels,
                    history = node.history,
                    edge = node.edge,
                    isBaseCase = false,
                    formulaText = "Для графа ${node.label} $operationDesc ребро ${node.edge} и стягиваем вершины ${node.edge}",
                    detailText = "$leftDesc,  стягивание → ${right.label}",
                    polynomial = node.polynomial
                )
            }
        }
        levels.add(RecursionLevel(depth = depth, isBackSubstitution = false, entries = entries))
    }

    // Back-substitution: 2 фиксированных шага
    val leaves = collectLeavesWithSigns(root)

    // Группируем листья по полиному (одинаковые типы графов → суммируем знаки)
    val groupKeys = mutableListOf<String>()
    val groupNode = mutableMapOf<String, RecursionNode>()
    val groupCount = mutableMapOf<String, Int>()
    for ((node, sign) in leaves) {
        val key = node.polynomial.toString()
        if (key !in groupCount) {
            groupKeys.add(key)
            groupNode[key] = node
            groupCount[key] = 0
        }
        groupCount[key] = groupCount[key]!! + sign
    }
    val nonZeroKeys = groupKeys.filter { (groupCount[it] ?: 0) != 0 }

    fun termLabel(i: Int, label: String, count: Int): String {
        val abs = kotlin.math.abs(count)
        val prefix = if (abs == 1) "" else "$abs·"
        return when {
            i == 0 && count > 0 -> "$prefix$label"
            i == 0 && count < 0 -> "−$prefix$label"
            count > 0 -> " + $prefix$label"
            else -> " − $prefix$label"
        }
    }

    // Шаг 1: "P(G) = K3 + 2·K2" — типы базовых графов
    val step1Text = nonZeroKeys.mapIndexed { i, key ->
        termLabel(i, extractGraphLabel(groupNode[key]!!), groupCount[key]!!)
    }.joinToString("")

    // Шаг 2: "= (poly1) + 2·(poly2) = итог"
    val step2Text = nonZeroKeys.mapIndexed { i, key ->
        termLabel(i, "(${groupNode[key]!!.polynomial})", groupCount[key]!!)
    }.joinToString("")

    levels.add(
        RecursionLevel(
            depth = maxDepth + 1,
            isBackSubstitution = true,
            entries = listOf(
                LevelEntry(
                    label = root.label,
                    graph = root.graph,
                    positions = root.positions,
                    vertexLabels = root.vertexLabels,
                    history = root.history,
                    isBaseCase = false,
                    formulaText = "P(${root.label}) = $step1Text",
                    polynomial = root.polynomial
                )
            )
        )
    )
    levels.add(
        RecursionLevel(
            depth = maxDepth + 2,
            isBackSubstitution = true,
            entries = listOf(
                LevelEntry(
                    label = root.label,
                    graph = root.graph,
                    positions = root.positions,
                    vertexLabels = root.vertexLabels,
                    history = root.history,
                    isBaseCase = false,
                    formulaText = "= $step2Text",
                    detailText = "P(${root.label}) = ${root.polynomial}",
                    polynomial = root.polynomial
                )
            )
        )
    )

    return levels
}
