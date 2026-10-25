package com.example.learnthai.ui.components.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KedmaneeKeyButton(
    char: Char,
    isHighlighted: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 48.dp
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0f at 0 with FastOutSlowInEasing
                    -8f at 50 with FastOutSlowInEasing
                    8f at 100 with FastOutSlowInEasing
                    -6f at 150 with FastOutSlowInEasing
                    6f at 200 with FastOutSlowInEasing
                    0f at 300 with FastOutSlowInEasing
                }
            )
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.errorContainer
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(150),
        label = "KeyBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isHighlighted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(150),
        label = "KeyBorderColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.onErrorContainer
            isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "KeyTextColor"
    )

    Box(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .height(keyHeight)
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (isHighlighted || isError) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val displayText = remember(char) { KedmaneeLayout.getDisplayString(char) }
        Text(
            text = displayText,
            color = textColor,
            fontSize = if (KedmaneeLayout.isCombiningMark(char)) 14.sp else 18.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
        )
    }
}
