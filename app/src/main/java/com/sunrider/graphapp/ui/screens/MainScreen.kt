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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunrider.graphapp.algorithm.GraphAlgorithm
import com.sunrider.graphapp.algorithm.GraphHistoryStep
import com.sunrider.graphapp.algorithm.LevelEntry
import com.sunrider.graphapp.algorithm.RecursionLevel
import com.sunrider.graphapp.ui.components.GraphCanvas
import com.sunrider.graphapp.ui.components.GraphPreview
import com.sunrider.graphapp.ui.theme.DirectedAccent
import com.sunrider.graphapp.ui.theme.MstTeal
import com.sunrider.graphapp.ui.theme.MstTealBright
import com.sunrider.graphapp.ui.theme.WeightedAccent
import com.sunrider.graphapp.viewmodel.EditMode
import com.sunrider.graphapp.viewmodel.GraphViewModel

private sealed class AlgoChoice(val displayName: String) {
    data class Algo(val algorithm: GraphAlgorithm) : AlgoChoice(algorithm.name)
    object Mst : AlgoChoice("МОД (Краскал)")
}

@Composable
fun MainScreen(viewModel: GraphViewModel, onOpenMatrixInput: () -> Unit = {}, modifier: Modifier = Modifier) {
    var selectedChoice by remember {
        mutableStateOf<AlgoChoice>(
            viewModel.algorithms.firstOrNull()?.let { AlgoChoice.Algo(it) } ?: AlgoChoice.Mst
        )
    }
    var detailEntry by remember { mutableStateOf<LevelEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        InfoBar(
            vertexCount = viewModel.graph.vertexCount,
            edgeCount = viewModel.graph.edgeCount,
            isDirected = viewModel.isDirected,
            isWeighted = viewModel.isWeighted,
            onToggleDirected = { viewModel.toggleDirected() },
            onToggleWeighted = { viewModel.toggleWeighted() },
            canToggleDirected = viewModel.graph.edges.isEmpty()
        )

        // Canvas area takes remaining space, with floating MST badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) {
            val mstEdges = if (viewModel.showMstResult) viewModel.mstResult?.edges ?: emptySet() else emptySet()

            GraphCanvas(
                graph = viewModel.graph,
                vertexPositions = viewModel.vertexPositions,
                selectedVertex = viewModel.selectedVertex,
                highlightedEdge = null,
                editMode = viewModel.editMode,
                canvasScale = viewModel.canvasScale,
                canvasOffset = viewModel.canvasOffset,
                mstEdges = mstEdges,
                isWeighted = viewModel.isWeighted,
                onTap = { offset ->
                    val vertex = viewModel.findVertexAt(offset)
                    when (viewModel.editMode) {
                        EditMode.ADD_VERTEX -> {
                            if (vertex == null) viewModel.addVertex(offset)
                        }
                        EditMode.ADD_EDGE -> {
                            if (vertex != null) {
                                viewModel.onVertexTap(vertex)
                            } else if (viewModel.isWeighted) {
                                val edge = viewModel.findEdgeAt(offset)
                                if (edge != null) viewModel.startEditWeight(edge)
                            }
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

            // Empty state hint
            if (viewModel.graph.vertices.isEmpty()) {
                EmptyCanvasHint(modifier = Modifier.align(Alignment.Center))
            }

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

            if (viewModel.showMstResult && viewModel.mstResult != null) {
                MstActiveBadge(
                    onDismiss = { viewModel.clearMstResult() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                )
            }

            if (viewModel.showAdjacencyMatrix && viewModel.graph.vertices.isNotEmpty()) {
                AdjacencyMatrixPanel(
                    graph = viewModel.graph,
                    isWeighted = viewModel.isWeighted,
                    onHide = { viewModel.toggleAdjacencyMatrix() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

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
                        onGraphClick = { entry -> detailEntry = entry },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        ModeSelector(
            currentMode = viewModel.editMode,
            onModeChanged = { viewModel.editMode = it }
        )

        ActionBar(
            algorithms = viewModel.algorithms,
            selectedChoice = selectedChoice,
            onSelectChoice = { selectedChoice = it },
            graphIsEmpty = viewModel.graph.vertices.isEmpty(),
            isDirected = viewModel.isDirected,
            onRun = {
                when (val c = selectedChoice) {
                    is AlgoChoice.Algo -> viewModel.executeAlgorithm(c.algorithm)
                    AlgoChoice.Mst -> viewModel.executeMST()
                }
            },
            onToggleMatrix = { viewModel.toggleAdjacencyMatrix() },
            onClear = { viewModel.clearGraph() },
            onOpenMatrixInput = onOpenMatrixInput
        )
    }

    viewModel.editingWeightEdge?.let { edge ->
        WeightEditDialog(
            edge = edge,
            currentWeight = viewModel.graph.getWeight(edge),
            onConfirm = { viewModel.confirmEditWeight(it) },
            onDismiss = { viewModel.cancelEditWeight() }
        )
    }

    detailEntry?.let { entry ->
        GraphDetailDialog(
            entry = entry,
            onDismiss = { detailEntry = null }
        )
    }
}

// ─── Top info bar ─────────────────────────────────────────────────────────────
@Composable
private fun InfoBar(
    vertexCount: Int,
    edgeCount: Int,
    isDirected: Boolean,
    isWeighted: Boolean,
    onToggleDirected: () -> Unit,
    onToggleWeighted: () -> Unit,
    canToggleDirected: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◈", fontSize = 14.sp, color = primary)
                }
                Text(
                    text = "GraphApp",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.weight(1f))

                CountBadge(label = "V", value = vertexCount)
                CountBadge(label = "E", value = edgeCount)

                if (isDirected) TypeChip("ОРИЕНТ.", DirectedAccent)
                if (isWeighted) TypeChip("ВЗВЕШ.", WeightedAccent)

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
            // Type toggles row (compact)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip(
                    label = "Ориентированный",
                    selected = isDirected,
                    enabled = canToggleDirected,
                    onClick = onToggleDirected
                )
                ToggleChip(
                    label = "Взвешенный",
                    selected = isWeighted,
                    enabled = true,
                    onClick = onToggleWeighted
                )
            }
        }
    }
}

@Composable
private fun CountBadge(label: String, value: Int) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: $value",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = primary
        )
    }
}

@Composable
private fun TypeChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyCanvasHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Нажмите на холст, чтобы добавить вершину",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Здесь отобразится ваш граф",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Bottom mode selector ─────────────────────────────────────────────────────
@Composable
private fun ModeSelector(currentMode: EditMode, onModeChanged: (EditMode) -> Unit) {
    val modes = listOf(
        Triple(EditMode.ADD_VERTEX, "Вершина", Icons.Filled.Add),
        Triple(EditMode.ADD_EDGE, "Ребро", Icons.Filled.Timeline),
        Triple(EditMode.DELETE, "Удалить", Icons.Filled.Close),
        Triple(EditMode.MOVE, "Двигать", Icons.Filled.OpenWith),
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEach { (mode, label, icon) ->
                    val selected = mode == currentMode
                    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val fg = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable { onModeChanged(mode) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = fg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}

// ─── Bottom action bar ────────────────────────────────────────────────────────
@Composable
private fun ActionBar(
    algorithms: List<GraphAlgorithm>,
    selectedChoice: AlgoChoice,
    onSelectChoice: (AlgoChoice) -> Unit,
    graphIsEmpty: Boolean,
    isDirected: Boolean,
    onRun: () -> Unit,
    onToggleMatrix: () -> Unit,
    onClear: () -> Unit,
    onOpenMatrixInput: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Algorithm selector pill
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isDirected) { menuExpanded = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedChoice.displayName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (!isDirected) {
                            algorithms.forEach { algo ->
                                DropdownMenuItem(
                                    text = { Text(algo.name) },
                                    onClick = {
                                        onSelectChoice(AlgoChoice.Algo(algo))
                                        menuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(AlgoChoice.Mst.displayName) },
                                onClick = {
                                    onSelectChoice(AlgoChoice.Mst)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Run button
                Button(
                    onClick = onRun,
                    enabled = !graphIsEmpty && !isDirected,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Запуск", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // More icon (matrix / clear / input)
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { moreExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Действия",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Матрица смежности") },
                            onClick = {
                                moreExpanded = false
                                onToggleMatrix()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ввод матрицы…") },
                            onClick = {
                                moreExpanded = false
                                onOpenMatrixInput()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Очистить граф") },
                            onClick = {
                                moreExpanded = false
                                onClear()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── MST badge ────────────────────────────────────────────────────────────────
@Composable
private fun MstActiveBadge(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MstTeal.copy(alpha = 0.18f))
            .border(1.dp, MstTeal.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onDismiss() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MstTealBright, CircleShape)
        )
        Text(
            text = "МОД АКТИВЕН",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MstTealBright,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Weight edit dialog ───────────────────────────────────────────────────────
@Composable
private fun WeightEditDialog(
    edge: com.sunrider.graphapp.model.Edge,
    currentWeight: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember(edge) { mutableStateOf(currentWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вес ребра ${edge.from} → ${edge.to}") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        weightText = newValue
                    }
                },
                label = { Text("Вес (положительное число)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weightText.toIntOrNull()
                    if (w != null && w > 0) onConfirm(w)
                },
                enabled = (weightText.toIntOrNull() ?: 0) > 0
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ─── Result panel ─────────────────────────────────────────────────────────────
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
    onGraphClick: (LevelEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentLevelIndex) { listState.animateScrollToItem(currentLevelIndex) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Хроматический полином",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.weight(1f)
                )
                NavCircle(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onPrevious)
                Spacer(Modifier.width(4.dp))
                NavCircle(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = onNext)
                Spacer(Modifier.width(4.dp))
                NavCircle(icon = Icons.Filled.Close, onClick = onHide)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = polynomialDisplay,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBlock(label = "χ(G)", value = chromaticNumber.toString())
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )
                StatBlock(label = "P(G, $chromaticNumber)", value = coloringCount.toString())
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Шаг ${currentLevelIndex + 1} из ${levels.size}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                itemsIndexed(levels) { index, level ->
                    LevelItem(
                        level = level,
                        isCurrent = index == currentLevelIndex,
                        onClick = { onGoToLevel(index) },
                        onGraphClick = onGraphClick
                    )
                }
            }
        }
    }
}

@Composable
private fun NavCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.6.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LevelItem(
    level: RecursionLevel,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onGraphClick: (LevelEntry) -> Unit
) {
    val bgColor = if (isCurrent)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surface

    val title = if (level.isBackSubstitution)
        "Подстановка (ур. ${level.depth})"
    else
        "Разложение (ур. ${level.depth})"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (isCurrent) 0.5f else 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        level.entries.forEach { entry ->
            LevelEntryItem(
                entry = entry,
                showGraph = !level.isBackSubstitution,
                onGraphClick = { onGraphClick(entry) }
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun LevelEntryItem(
    entry: LevelEntry,
    showGraph: Boolean = true,
    onGraphClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showGraph) {
            GraphPreview(
                graph = entry.graph,
                vertexPositions = entry.positions,
                highlightedEdge = entry.edge,
                vertexLabels = entry.vertexLabels,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onGraphClick() }
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.formulaText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (entry.detailText.isNotEmpty()) {
                Text(
                    text = entry.detailText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.isBaseCase) {
                Text(
                    text = "= ${entry.polynomial}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Adjacency matrix overlay panel ───────────────────────────────────────────
@Composable
private fun AdjacencyMatrixPanel(
    graph: com.sunrider.graphapp.model.Graph,
    isWeighted: Boolean,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (sortedVertices, matrix) = if (isWeighted) graph.getWeightedAdjacencyMatrix()
    else graph.getAdjacencyMatrix()
    val cellSize = 36.dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Матрица смежности",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                NavCircle(icon = Icons.Filled.Close, onClick = onHide)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp)
            ) {
                Column {
                    Row {
                        Box(modifier = Modifier.size(cellSize))
                        sortedVertices.forEach { v ->
                            Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$v",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    matrix.forEachIndexed { rowIndex, row ->
                        Row {
                            Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${sortedVertices[rowIndex]}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            row.forEachIndexed { colIndex, value ->
                                val diag = rowIndex == colIndex
                                val bg = when {
                                    diag -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    value > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (diag) "—" else "$value",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (diag)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else if (value > 0)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Graph detail dialog (fullscreen with history) ────────────────────────────
@Composable
private fun GraphDetailDialog(
    entry: LevelEntry,
    onDismiss: () -> Unit
) {
    val history = entry.history
    var stepIndex by remember(entry) { mutableStateOf((history.size - 1).coerceAtLeast(0)) }
    val currentStep: GraphHistoryStep? = history.getOrNull(stepIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Граф ${entry.label}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    NavCircle(icon = Icons.Filled.Close, onClick = onDismiss)
                }

                Spacer(Modifier.height(8.dp))

                // Step description
                if (currentStep != null) {
                    Text(
                        text = "Шаг ${stepIndex + 1} из ${history.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentStep.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Graph area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    if (currentStep != null) {
                        GraphPreview(
                            graph = currentStep.graph,
                            vertexPositions = currentStep.positions,
                            highlightedEdge = currentStep.highlightedEdge,
                            vertexLabels = currentStep.vertexLabels,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Polynomial for this entry
                Text(
                    text = "P(${entry.label}) = ${entry.polynomial}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                // Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (stepIndex > 0) stepIndex-- },
                        enabled = stepIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Назад",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Назад", fontSize = 13.sp)
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "${stepIndex + 1} / ${history.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { if (stepIndex < history.size - 1) stepIndex++ },
                        enabled = stepIndex < history.size - 1,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Далее", fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Далее",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
