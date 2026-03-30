package com.sunrider.graphapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MatrixInputScreen(
    onApply: (size: Int, matrix: List<List<Int>>, isWeighted: Boolean, isDirected: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableIntStateOf(0) }
    var sizeText by remember { mutableStateOf("") }
    var matrixCreated by remember { mutableStateOf(false) }
    var matrixValues by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isDirected by remember { mutableStateOf(false) }
    var isWeighted by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Ввод матрицы смежности",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Graph type toggles
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isDirected,
                onClick = { isDirected = !isDirected },
                label = { Text("Ориентированный", fontSize = 13.sp) },
                enabled = !matrixCreated
            )
            FilterChip(
                selected = isWeighted,
                onClick = {
                    isWeighted = !isWeighted
                    if (matrixCreated && !isWeighted) {
                        matrixValues = matrixValues.map { row ->
                            row.map { cell ->
                                val v = cell.toIntOrNull() ?: 0
                                if (v > 0) "1" else "0"
                            }
                        }
                    }
                },
                label = { Text("Взвешенный", fontSize = 13.sp) }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!matrixCreated) {
            // Size input step
            SizeInputStep(
                sizeText = sizeText,
                onSizeTextChange = { sizeText = it },
                onConfirm = {
                    val n = sizeText.toIntOrNull()
                    if (n != null && n in 1..20) {
                        size = n
                        matrixValues = List(n) { List(n) { "0" } }
                        matrixCreated = true
                    }
                }
            )
        } else {
            // Matrix grid step
            Text(
                text = "Матрица ${size}x${size}  (вершины ${1}..${size})",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            MatrixGrid(
                size = size,
                matrixValues = matrixValues,
                isDirected = isDirected,
                isWeighted = isWeighted,
                onValueChange = { row, col, value ->
                    matrixValues = matrixValues.mapIndexed { r, rowList ->
                        rowList.mapIndexed { c, cell ->
                            when {
                                r == row && c == col -> value
                                !isDirected && r == col && c == row && row != col -> value
                                else -> cell
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        matrixCreated = false
                        sizeText = ""
                        size = 0
                        matrixValues = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сбросить")
                }

                Button(
                    onClick = {
                        val parsed = matrixValues.map { row ->
                            row.map { cell ->
                                val v = cell.toIntOrNull() ?: 0
                                if (v < 0) 0 else v
                            }
                        }
                        onApply(size, parsed, isWeighted, isDirected)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Применить")
                }
            }
        }
    }
}

@Composable
private fun SizeInputStep(
    sizeText: String,
    onSizeTextChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "Введите количество вершин",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = sizeText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                    onSizeTextChange(newValue)
                }
            },
            label = { Text("Количество вершин (1–20)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            singleLine = true,
            modifier = Modifier.width(220.dp)
        )

        Spacer(Modifier.height(16.dp))

        val n = sizeText.toIntOrNull()
        Button(
            onClick = onConfirm,
            enabled = n != null && n in 1..20
        ) {
            Text("Создать матрицу")
        }
    }
}

@Composable
private fun MatrixGrid(
    size: Int,
    matrixValues: List<List<String>>,
    isDirected: Boolean,
    isWeighted: Boolean,
    onValueChange: (row: Int, col: Int, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = 52.dp
    val headerSize = 36.dp
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header row
            Row {
                Box(
                    modifier = Modifier.size(headerSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text("", style = MaterialTheme.typography.bodySmall)
                }
                for (j in 0 until size) {
                    Box(
                        modifier = Modifier.size(width = cellSize, height = headerSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${j + 1}",
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
            for (i in 0 until size) {
                Row {
                    // Row header
                    Box(
                        modifier = Modifier.size(width = headerSize, height = cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    for (j in 0 until size) {
                        val isDiagonal = i == j
                        val isMirrored = !isDirected && j < i

                        val bgColor = when {
                            isDiagonal -> MaterialTheme.colorScheme.surfaceContainerHighest
                            isMirrored -> MaterialTheme.colorScheme.surfaceContainerHigh
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
                            if (isDiagonal) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (isMirrored) {
                                // Show mirrored value (read-only) for undirected
                                Text(
                                    text = matrixValues[i][j],
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                OutlinedTextField(
                                    value = matrixValues[i][j],
                                    onValueChange = { newVal ->
                                        val filtered = newVal.filter { it.isDigit() }
                                        val clamped = if (filtered.length > 3) filtered.take(3) else filtered
                                        if (!isWeighted) {
                                            // Only 0 or 1
                                            val v = clamped.toIntOrNull() ?: 0
                                            onValueChange(i, j, if (v > 0) "1" else "0")
                                        } else {
                                            onValueChange(i, j, clamped)
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                    ),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .size(cellSize)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

