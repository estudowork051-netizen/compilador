package com.lifeorganizer.app.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asAndroidBitmap
import com.lifeorganizer.app.core.theme.PrimaryDark
import com.lifeorganizer.app.core.theme.PrimarySage
import com.lifeorganizer.app.core.theme.SageLight
import kotlinx.coroutines.delay

/**
 * Tela de Apresentação ao Iniciar (Splash Screen)
 * Exibida por 3 a 4 segundos na abertura normal do aplicativo.
 * Suporta imagem personalizada fornecida pelo usuário (viora_splash.png) preenchendo
 * integralmente a tela do dispositivo (edge-to-edge com ContentScale.Crop, sem barras ou margens)
 * e preserva fallback visual nativo com tema Sage Green & Nature quando ausente.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    // Carrega de forma segura viora_splash.png (ou lifeflow_splash.png) de assets ou drawable
    val customSplashBitmap = remember {
        try {
            val assetsList = context.assets.list("branding") ?: emptyArray()
            val fileName = when {
                assetsList.contains("viora_splash.png") -> "branding/viora_splash.png"
                assetsList.contains("lifeflow_splash.png") -> "branding/lifeflow_splash.png"
                else -> null
            }
            if (fileName != null) {
                context.assets.open(fileName).use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } else {
                val resId = context.resources.getIdentifier("viora_splash", "drawable", context.packageName)
                if (resId != 0) {
                    android.graphics.BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Identifica cor de fundo dominante da imagem para harmonia visual completa
    val dominantBackgroundColor = remember(customSplashBitmap) {
        if (customSplashBitmap != null) {
            try {
                val bmp = customSplashBitmap.asAndroidBitmap()
                val samplePixel = bmp.getPixel(
                    (bmp.width / 2).coerceIn(0, bmp.width - 1),
                    4.coerceAtMost(bmp.height - 1)
                )
                Color(samplePixel)
            } catch (_: Exception) {
                Color(0xFFFDFBF7)
            }
        } else {
            Color(0xFFFDFBF7)
        }
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
        // Duração total entre 3 e 4 segundos conforme especificação
        delay(3200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (customSplashBitmap != null) {
            // Imagem personalizada preenchendo toda a tela do celular (Edge-to-Edge)
            Image(
                bitmap = customSplashBitmap,
                contentDescription = "Viora Apresentação",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha.value),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        } else {
            // Apresentação elegante Sage Green & Nature (Fallback quando sem imagem personalizada)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .padding(32.dp)
            ) {
                // Viora Nature & Time Icon
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(color = SageLight.copy(alpha = 0.55f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val w = size.width
                        val h = size.height

                        // Circular arc of time
                        val arcPath = Path().apply {
                            addArc(
                                oval = androidx.compose.ui.geometry.Rect(
                                    left = w * 0.15f,
                                    top = h * 0.15f,
                                    right = w * 0.85f,
                                    bottom = h * 0.85f
                                ),
                                startAngleDegrees = 110f,
                                sweepAngleDegrees = 290f
                            )
                        }
                        drawPath(
                            path = arcPath,
                            color = Color(0xFF4A7C59),
                            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Sprouting Leaf
                        val leafPath = Path().apply {
                            moveTo(w * 0.5f, h * 0.75f)
                            cubicTo(
                                w * 0.72f, h * 0.65f,
                                w * 0.78f, h * 0.38f,
                                w * 0.5f, h * 0.22f
                            )
                            cubicTo(
                                w * 0.55f, h * 0.42f,
                                w * 0.52f, h * 0.60f,
                                w * 0.5f, h * 0.75f
                            )
                            close()
                        }
                        drawPath(path = leafPath, color = Color(0xFF7EAA8B))

                        // Mindfulness balance point
                        drawCircle(
                            color = Color(0xFF2D5A3C),
                            radius = 6f,
                            center = Offset(w * 0.5f, h * 0.22f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Brand Name
                Text(
                    text = "Viora",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = PrimaryDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Brand Tagline
                Text(
                    text = "Seu ritmo, sua vida.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Nature Loading Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(PrimarySage.copy(alpha = 0.5f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(PrimarySage, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(PrimarySage.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}
