package com.sunrider.graphapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private const val MIN_SIZE = 2
private const val MAX_SIZE = 20

@Composable
fun MatrixInputScreen(
    onApply: (size: Int, matrix: List<List<Int>>, isWeighted: Boolean, isDirected: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableIntStateOf(5) }
    var matrixValues by remember { mutableStateOf(List(MAX_SIZE) { List(MAX_SIZE) { "0" } }) }
    var isDirected by remember { mutableStateOf(false) }
    var isWeighted by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Матрица смежности",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                matrixValues = List(MAX_SIZE) { List(MAX_SIZE) { "0" } }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Очистить",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))

                // Size stepper section
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "РАЗМЕР МАТРИЦЫ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StepperButton(
                            icon = Icons.Filled.Remove,
                            enabled = size > MIN_SIZE,
                            onClick = { if (size > MIN_SIZE) size-- }
                        )
                        Slider(
                            value = size.toFloat(),
                            onValueChange = { size = it.toInt().coerceIn(MIN_SIZE, MAX_SIZE) },
                            valueRange = MIN_SIZE.toFloat()..MAX_SIZE.toFloat(),
                            steps = MAX_SIZE - MIN_SIZE - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        StepperButton(
                            icon = Icons.Filled.Add,
                            enabled = size < MAX_SIZE,
                            onClick = { if (size < MAX_SIZE) size++ }
                        )
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${size}×${size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))

                // Toggle chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillToggle(
                        label = "Ориентированный",
                        selected = isDirected,
                        onClick = { isDirected = !isDirected }
                    )
                    PillToggle(
                        label = "Взвешенный",
                        selected = isWeighted,
                        onClick = {
                            val nowWeighted = !isWeighted
                            isWeighted = nowWeighted
                            if (!nowWeighted) {
                                matrixValues = matrixValues.map { row ->
                                    row.map { cell ->
                                        if ((cell.toIntOrNull() ?: 0) > 0) "1" else "0"
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        // Matrix grid
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
            focusManager = focusManager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // CTA bar
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = {
                            val parsed = (0 until size).map { r ->
                                (0 until size).map { c ->
                                    val v = matrixValues[r][c].toIntOrNull() ?: 0
                                    if (v < 0) 0 else v
                                }
                            }
                            onApply(size, parsed, isWeighted, isDirected)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Построить граф",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PillToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun MatrixGrid(
    size: Int,
    matrixValues: List<List<String>>,
    isDirected: Boolean,
    isWeighted: Boolean,
    onValueChange: (row: Int, col: Int, value: String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    val cellSize = 44.dp
    val rowHeaderWidth = 24.dp
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Column headers
            Row {
                Spacer(Modifier.width(rowHeaderWidth + 4.dp))
                for (j in 0 until size) {
                    Box(
                        modifier = Modifier
                            .width(cellSize)
                            .height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${j + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))

            for (i in 0 until size) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(rowHeaderWidth)
                            .height(cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                            letterSpacing = 0.4.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    for (j in 0 until size) {
                        MatrixCell(
                            row = i,
                            col = j,
                            value = matrixValues[i][j],
                            isDiagonal = i == j,
                            isMirrored = !isDirected && j < i,
                            isWeighted = isWeighted,
                            cellSize = cellSize,
                            onValueChange = onValueChange,
                            focusManager = focusManager
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MatrixCell(
    row: Int,
    col: Int,
    value: String,
    isDiagonal: Boolean,
    isMirrored: Boolean,
    isWeighted: Boolean,
    cellSize: androidx.compose.ui.unit.Dp,
    onValueChange: (Int, Int, String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    val primary = MaterialTheme.colorScheme.primary
    val numeric = value.toIntOrNull() ?: 0
    val active = numeric > 0

    val bg = when {
        isDiagonal -> MaterialTheme.colorScheme.surfaceContainerHighest
        active -> primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        isDiagonal -> primary.copy(alpha = 0.4f)
        active -> primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isDiagonal) primary.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isDiagonal) {
            Text(
                text = "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = fg
            )
        } else if (isMirrored) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = fg
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }.take(3)
                    if (!isWeighted) {
                        val v = filtered.toIntOrNull() ?: 0
                        onValueChange(row, col, if (v > 0) "1" else "0")
                    } else {
                        onValueChange(row, col, filtered)
                    }
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
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
                    focusedBorderColor = primary,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
