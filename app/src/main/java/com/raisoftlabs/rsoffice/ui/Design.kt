package com.raisoftlabs.rsoffice.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft, slowly drifting colour wash behind every screen (cheap: 2 circles). */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val light = scheme.background.luminance() > 0.5f
    val transition = rememberInfiniteTransition(label = "wash")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension
            val topCenter = Offset(size.width * 0.94f, size.height * (0.02f + 0.07f * drift))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Indigo.copy(alpha = if (light) 0.14f else 0.20f),
                        Color.Transparent
                    ),
                    center = topCenter,
                    radius = radius * 0.72f
                ),
                radius = radius * 0.72f,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.04f, size.height * (0.90f - 0.05f * drift))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Violet.copy(alpha = if (light) 0.10f else 0.16f),
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = radius * 0.62f
                ),
                radius = radius * 0.62f,
                center = bottomCenter
            )
        }
        content()
    }
}

/** Tap feedback with an iOS-style springy squish. */
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

/** Frosted card: subtle top-to-bottom gloss, hairline edge, springy press. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val light = scheme.background.luminance() > 0.5f
    val shape = RoundedCornerShape(radius)
    val top = if (light) Color.White else Color(0xFF26262A)
    val bottom = if (light) Color(0xFFF6F7FB) else scheme.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) Modifier.bounceClick { onClick() } else Modifier
            )
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(
                width = 1.dp,
                color = if (light) Color(0xFFFFFFFF) else scheme.outlineVariant.copy(alpha = 0.8f),
                shape = shape
            )
            .padding(padding),
        content = content
    )
}

/** Rounded square icon with a glossy two-tone gradient — the app-suite look. */
@Composable
fun GradientIcon(
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Section title with an optional trailing text action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .bounceClick { onAction() }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    action,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** Small uppercase tag used on hero cards. */
@Composable
fun GlossTag(text: String, icon: ImageVector? = null, color: Color = Color.White) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = color,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
    }
}
