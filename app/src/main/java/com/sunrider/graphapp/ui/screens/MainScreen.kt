package com.sunrider.graphapp.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunrider.graphapp.algorithm.GraphAlgorithm
import com.sunrider.graphapp.algorithm.LevelEntry
import com.sunrider.graphapp.algorithm.RecursionLevel
import com.sunrider.graphapp.ui.components.GraphCanvas
import com.sunrider.graphapp.ui.components.GraphPreview
import com.sunrider.graphapp.viewmodel.EditMode
import com.sunrider.graphapp.viewmodel.GraphViewModel

@Composable
fun MainScreen(viewModel: GraphViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // Info bar
        InfoBar(
            vertexCount = viewModel.graph.vertexCount,
            edgeCount = viewModel.graph.edgeCount
        )

        // Mode selector
        ModeSelector(
            currentMode = viewModel.editMode,
            onModeChanged = { viewModel.editMode = it }
        )

        // Graph canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GraphCanvas(
                graph = viewModel.graph,
                vertexPositions = viewModel.vertexPositions,
                selectedVertex = viewModel.selectedVertex,
                highlightedEdge = null,
                editMode = viewModel.editMode,
                canvasScale = viewModel.canvasScale,
                canvasOffset = viewModel.canvasOffset,
                onTap = { offset ->
                    val vertex = viewModel.findVertexAt(offset)
                    when (viewModel.editMode) {
                        EditMode.ADD_VERTEX -> {
                            if (vertex == null) viewModel.addVertex(offset)
                        }
                        EditMode.ADD_EDGE -> {
                            if (vertex != null) viewModel.onVertexTap(vertex)
                        }
                        EditMode.DELETE -> {
                            if (vertex != null) {
                                viewModel.deleteVertex(vertex)
                            } else {
                                val edge = viewModel.findEdgeAt(offset)
                                if (edge != null) viewModel.deleteEdge(edge)
                            }
                        }
                        EditMode.MOVE -> {}
                    }
                },
                onDragVertex = { id, pos -> viewModel.moveVertex(id, pos) },
                onTransformChange = { scale, offset ->
                    viewModel.updateCanvasTransform(scale, offset)
                },
                findVertexAt = { viewModel.findVertexAt(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Reset view button
            if (viewModel.canvasScale != 1f || viewModel.canvasOffset != Offset.Zero) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { viewModel.resetCanvasTransform() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text("1:1", fontSize = 11.sp)
                }
            }

            // Adjacency matrix panel
            if (viewModel.showAdjacencyMatrix && viewModel.graph.vertices.isNotEmpty()) {
                AdjacencyMatrixPanel(
                    graph = viewModel.graph,
                    onHide = { viewModel.toggleAdjacencyMatrix() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Action bar
            ActionBar(
                algorithms = viewModel.algorithms,
                onClear = { viewModel.clearGraph() },
                onExecute = { viewModel.executeAlgorithm(it) },
                onShowMatrix = { viewModel.toggleAdjacencyMatrix() },
                graphIsEmpty = viewModel.graph.vertices.isEmpty(),
                showingMatrix = viewModel.showAdjacencyMatrix,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Result panel
            val showResult = viewModel.showResult && viewModel.algorithmResult != null
            if (showResult) {
                viewModel.algorithmResult?.let { result ->
                    ResultPanel(
                        levels = result.levels,
                        currentLevelIndex = viewModel.currentLevelIndex,
                        polynomialDisplay = result.polynomial.toDisplayString(),
                        chromaticNumber = result.chromaticNumber,
                        coloringCount = result.coloringCount,
                        onPrevious = { viewModel.previousLevel() },
                        onNext = { viewModel.nextLevel() },
                        onGoToLevel = { viewModel.goToLevel(it) },
                        onHide = { viewModel.hideResult() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBar(vertexCount: Int, edgeCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Вершин: $vertexCount  |  Рёбер: $edgeCount",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ModeSelector(currentMode: EditMode, onModeChanged: (EditMode) -> Unit) {
    val modes = listOf(
        EditMode.ADD_VERTEX to "Вершина",
        EditMode.ADD_EDGE to "Ребро",
        EditMode.DELETE to "Удалить",
        EditMode.MOVE to "Двигать"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = mode == currentMode
            if (isSelected) {
                Button(
                    onClick = {},
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(label, fontSize = 14.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { onModeChanged(mode) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(label, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    algorithms: List<GraphAlgorithm>,
    onClear: () -> Unit,
    onExecute: (GraphAlgorithm) -> Unit,
    onShowMatrix: () -> Unit,
    graphIsEmpty: Boolean,
    showingMatrix: Boolean,
    modifier: Modifier = Modifier
) {
    var showAlgorithmMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
    Column {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onClear,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Очистить")
        }

        if (showingMatrix) {
            Button(
                onClick = onShowMatrix,
                enabled = !graphIsEmpty
            ) {
                Text("Матрица")
            }
        } else {
            OutlinedButton(
                onClick = onShowMatrix,
                enabled = !graphIsEmpty
            ) {
                Text("Матрица")
            }
        }

        Spacer(Modifier.weight(1f))

        Box {
            FilledTonalButton(
                onClick = {
                    if (algorithms.size == 1) {
                        onExecute(algorithms[0])
                    } else {
                        showAlgorithmMenu = true
                    }
                },
                enabled = !graphIsEmpty
            ) {
                Text("Вычислить")
            }
            DropdownMenu(
                expanded = showAlgorithmMenu,
                onDismissRequest = { showAlgorithmMenu = false }
            ) {
                algorithms.forEach { algo ->
                    DropdownMenuItem(
                        text = { Text(algo.name) },
                        onClick = {
                            showAlgorithmMenu = false
                            onExecute(algo)
                        }
                    )
                }
            }
        }
    }
    } // Column
    } // Surface
}

@Composable
private fun ResultPanel(
    levels: List<RecursionLevel>,
    currentLevelIndex: Int,
    polynomialDisplay: String,
    chromaticNumber: Int,
    coloringCount: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToLevel: (Int) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentLevelIndex) {
        listState.animateScrollToItem(currentLevelIndex)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Polynomial + chromatic info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = polynomialDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(
                                text = "Хроматическое число",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "χ(G) = $chromaticNumber",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Раскрасок в $chromaticNumber цвет(а/ов)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "P(G, $chromaticNumber) = $coloringCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Level navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = currentLevelIndex > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("<")
                }

                Text(
                    text = "Шаг ${currentLevelIndex + 1} из ${levels.size}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = onNext,
                    enabled = currentLevelIndex < levels.size - 1,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(">")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Level list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                itemsIndexed(levels) { index, level ->
                    LevelItem(
                        level = level,
                        levelIndex = index,
                        isCurrent = index == currentLevelIndex,
                        onClick = { onGoToLevel(index) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Hide button
            TextButton(
                onClick = onHide,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Скрыть")
            }
        }
    }
}

@Composable
private fun LevelItem(
    level: RecursionLevel,
    levelIndex: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    val borderColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val title = if (level.isBackSubstitution) {
        "Подстановка (ур. ${level.depth})"
    } else {
        "Разложение (ур. ${level.depth})"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isCurrent) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(bgColor)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Level title
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (level.isBackSubstitution)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(4.dp))

        // Entries
        level.entries.forEach { entry ->
            LevelEntryItem(entry = entry, showGraph = !level.isBackSubstitution)
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun LevelEntryItem(entry: LevelEntry, showGraph: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini graph preview
        if (showGraph) {
            GraphPreview(
                graph = entry.graph,
                vertexPositions = entry.positions,
                highlightedEdge = entry.edge,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Formula
            Text(
                text = entry.formulaText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            // Detail
            if (entry.detailText.isNotEmpty()) {
                Text(
                    text = entry.detailText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Show polynomial result for base cases
            if (entry.isBaseCase) {
                Text(
                    text = "= ${entry.polynomial}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun AdjacencyMatrixPanel(
    graph: com.sunrider.graphapp.model.Graph,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (sortedVertices, matrix) = graph.getAdjacencyMatrix()
    val cellSize = 36.dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Матрица смежности",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp)
            ) {
                Column {
                    // Header row
                    Row {
                        // Empty corner cell
                        Box(
                            modifier = Modifier.size(cellSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        sortedVertices.forEach { v ->
                            Box(
                                modifier = Modifier.size(cellSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$v",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Matrix rows
                    matrix.forEachIndexed { rowIndex, row ->
                        Row {
                            // Row header
                            Box(
                                modifier = Modifier.size(cellSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sortedVertices[rowIndex]}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            row.forEachIndexed { colIndex, value ->
                                val bgColor = when {
                                    rowIndex == colIndex -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    value == 1 -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(2.dp)
                                        )
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$value",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (value == 1) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (value == 1)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = onHide) {
                Text("Скрыть")
            }
        }
    }
}
