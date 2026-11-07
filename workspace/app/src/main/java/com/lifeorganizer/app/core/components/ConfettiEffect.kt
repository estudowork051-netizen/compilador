package com.lifeorganizer.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.lifeorganizer.app.core.theme.CreamCard
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageAccent
import com.lifeorganizer.app.core.theme.SageLight
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val color: Color,
    val angle: Double,
    val speed: Float,
    val size: Float,
    val rotation: Float
)

/**
 * Lightweight native Compose Canvas celebratory confetti (FASE B6).
 * Spawns 25 tranquil nature-palette particles with radial burst and gravity.
 */
@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }

    val palette = remember {
        listOf(PrimarySage, SageAccent, SageLight, PrimaryDark, CreamCard, Color(0xFFF3C978))
    }

    val particles = remember {
        val rand = Random(42)
        List(28) {
            ConfettiParticle(
                color = palette[rand.nextInt(palette.size)],
                angle = rand.nextDouble(0.0, 2.0 * Math.PI),
                speed = rand.nextFloat() * 260f + 140f,
                size = rand.nextFloat() * 8f + 6f,
                rotation = rand.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    val currentProgress = progress.value
    if (currentProgress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 3f
            val alpha = (1f - currentProgress).coerceIn(0f, 1f)

            for (p in particles) {
                val distance = p.speed * currentProgress
                val gravityDrop = 380f * (currentProgress * currentProgress)
                val x = centerX + (cos(p.angle) * distance).toFloat()
                val y = centerY + (sin(p.angle) * distance).toFloat() + gravityDrop

                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(p.size, p.size * 0.75f)
                )
            }
        }
    }
}
