package com.example.learnthai.ui.components.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun KedmaneeKeyboardView(
    currentTargetChar: Char?,
    isGuidedMode: Boolean,
    onKeyValidated: (Char) -> Unit,
    onWrongKeyAttempted: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShifted by remember { mutableStateOf(false) }
    var shakingKey by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(shakingKey) {
        if (shakingKey != null) {
            delay(400)
            shakingKey = null
        }
    }

    val isTargetOnShiftLayer = currentTargetChar?.let { KedmaneeLayout.isShiftedChar(it) } == true
    val isShiftExpected = isGuidedMode && isTargetOnShiftLayer && !isShifted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 0.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = if (isShifted) KedmaneeLayout.shiftedRows else KedmaneeLayout.normalRows

        rows.forEachIndexed { rowIndex, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prepend Shift key on row 3 (bottom letter row)
                if (rowIndex == 3) {
                    ShiftKeyButton(
                        isShiftActive = isShifted,
                        isShiftExpected = isShiftExpected,
                        onClick = { isShifted = !isShifted },
                        modifier = Modifier.weight(1.35f)
                    )
                }

                rowKeys.forEach { char ->
                    val isExpected = isGuidedMode && char == currentTargetChar
                    val isShaking = shakingKey == char

                    KedmaneeKeyButton(
                        char = char,
                        isHighlighted = isExpected,
                        isError = isShaking,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (char == currentTargetChar) {
                                shakingKey = null
                                onKeyValidated(char)
                                if (isShifted) isShifted = false // Auto-unshift after successful shifted char
                            } else {
                                shakingKey = char
                                onWrongKeyAttempted(char)
                            }
                        }
                    )
                }

                // Append Space key button on bottom row for ergonomic balance
                if (rowIndex == 3) {
                    SpaceKeyButton(
                        isExpected = isGuidedMode && currentTargetChar == ' ',
                        modifier = Modifier.weight(1.35f),
                        onClick = {
                            if (currentTargetChar == ' ') {
                                onKeyValidated(' ')
                            } else {
                                onWrongKeyAttempted(' ')
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftKeyButton(
    isShiftActive: Boolean,
    isShiftExpected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isShiftActive -> MaterialTheme.colorScheme.primary
            isShiftExpected -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "ShiftBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isShiftActive -> MaterialTheme.colorScheme.onPrimary
            isShiftExpected -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "ShiftContentColor"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = if (isShiftExpected) 1.8.dp else 1.dp,
                color = if (isShiftExpected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = "Shift",
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SpaceKeyButton(
    isExpected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isExpected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isExpected) 1.8.dp else 1.dp,
                color = if (isExpected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "␣",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
