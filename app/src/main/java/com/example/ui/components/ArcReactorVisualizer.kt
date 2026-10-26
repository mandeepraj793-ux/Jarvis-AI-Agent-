package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcReactorCore
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    rmsAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorMotion")

    // Rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // Breathing pulse for idle & speaking
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // Dynamic amplitude scale when listening
    val effectiveScale = if (isListening) {
        1.0f + (rmsAmplitude * 0.45f)
    } else if (isSpeaking) {
        pulse * 1.08f
    } else {
        pulse
    }

    val primaryColor = when {
        isThinking -> NeonAmber
        isListening -> CyanGlow
        isSpeaking -> CyanBright
        else -> ElectricBlue
    }

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.85f

            // 1. Outer dashed telemetry ring
            val numTicks = 24
            val tickRadius = radius * 0.98f
            for (i in 0 until numTicks) {
                val angleDeg = (i * (360f / numTicks) + rotation) % 360f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val startX = center.x + (tickRadius - 6.dp.toPx()) * cos(angleRad).toFloat()
                val startY = center.y + (tickRadius - 6.dp.toPx()) * sin(angleRad).toFloat()
                val endX = center.x + tickRadius * cos(angleRad).toFloat()
                val endY = center.y + tickRadius * sin(angleRad).toFloat()

                val tickAlpha = if (i % 4 == 0) 0.8f else 0.3f
                drawLine(
                    color = primaryColor.copy(alpha = tickAlpha),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 4 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Middle arc segment ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Accent rotating arcs
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryColor.copy(alpha = 0f), primaryColor, primaryColor.copy(alpha = 0f))
                ),
                startAngle = rotation,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
                size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Glowing Core Reactor
            val coreRadius = (radius * 0.48f) * effectiveScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ArcReactorCore.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.4f
                ),
                radius = coreRadius * 1.35f,
                center = center
            )

            // Central solid core
            drawCircle(
                color = ArcReactorCore,
                radius = coreRadius * 0.65f,
                center = center
            )

            // Central concentric line
            drawCircle(
                color = primaryColor,
                radius = coreRadius * 0.65f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
