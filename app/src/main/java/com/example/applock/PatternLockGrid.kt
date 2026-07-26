package com.example.applock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PatternLockGrid(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    onPatternStart: () -> Unit,
    onDotSelected: (Int) -> Unit,
    onPatternCompleted: () -> Unit
) {
    val dotStates = remember { List(9) { mutableStateOf(false) } }
    val linePoints = remember { mutableStateListOf<Pair<Offset, Offset>>() }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(isError, isSuccess) {
        if (isError || isSuccess) {
            delay(350)
            dotStates.forEach { it.value = false }
            linePoints.clear()
            currentDragOffset = null
        }
    }

    val dotColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFFF6B6B)
            isSuccess -> Color(0xFF51CF66)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(200),
        label = "dotColor"
    )

    Box(modifier = modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                onPatternStart()
                dotStates.forEach { it.value = false }
                linePoints.clear()
            },
            onDrag = { change, _ ->
                currentDragOffset = change.position
                val col = (change.position.x / (size.width / 3f)).toInt().coerceIn(0, 2)
                val row = (change.position.y / (size.height / 3f)).toInt().coerceIn(0, 2)
                val index = row * 3 + col

                if (index in 0..8 && !dotStates[index].value) {
                    val lastSelected = dotStates.indexOfLast { it.value }
                    dotStates[index].value = true
                    onDotSelected(index)

                    if (lastSelected >= 0 && lastSelected != index) {
                        val prevCol = lastSelected % 3
                        val prevRow = lastSelected / 3
                        val prevOffset = Offset(
                            (prevCol + 0.5f) * (size.width / 3f),
                            (prevRow + 0.5f) * (size.height / 3f)
                        )
                        val currOffset = Offset(
                            (col + 0.5f) * (size.width / 3f),
                            (row + 0.5f) * (size.height / 3f)
                        )
                        linePoints.add(prevOffset to currOffset)
                    }
                }
            },
            onDragEnd = {
                currentDragOffset = null
                onPatternCompleted()
            },
            onDragCancel = {
                currentDragOffset = null
                onPatternCompleted()
            }
        )
    }) {
        // Draw connecting lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()

            linePoints.forEach { (start, end) ->
                drawLine(
                    color = dotColor.copy(alpha = 0.75f),
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            currentDragOffset?.let { drag ->
                val lastDot = dotStates.indexOfLast { it.value }
                if (lastDot >= 0) {
                    val col = lastDot % 3
                    val row = lastDot / 3
                    val center = Offset(
                        (col + 0.5f) * (size.width / 3f),
                        (row + 0.5f) * (size.height / 3f)
                    )
                    drawLine(
                        color = dotColor.copy(alpha = 0.4f),
                        start = center,
                        end = drag,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 3x3 Dot Grid
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val isSelected = dotStates[index].value

                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(tween(150)),
                                exit = scaleOut(tween(150))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(dotColor.copy(alpha = 0.2f), CircleShape)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 22.dp else 14.dp)
                                    .background(
                                        if (isSelected) dotColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                                    .animateContentSize(tween(150))
                            )
                        }
                    }
                }
            }
        }
    }
}
