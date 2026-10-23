package com.paciencia.solitaire.scenery.themes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.paciencia.solitaire.scenery.Particle
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import java.util.Random

/**
 * Cenário: Noite Cósmica
 * Atmosfera interestelar profunda com nebulosas multicamadas (azul celeste, violeta e magenta),
 * constelações cintilantes de múltiplas magnitudes e poeira estelar flutuante.
 */
class CosmicNightTheme : SceneryTheme {
    override val type = SceneryType.COSMIC_NIGHT
    override val accentColor = 0xFF818CF8.toInt()
    override val slotBorderColor = 0x66818CF8.toInt()
    override val slotBackgroundColor = 0x22050212.toInt()
    override val cardBackGradient = intArrayOf(0xFF1E1038.toInt(), 0xFF050212.toInt())

    private val bgPaint = Paint()
    private val nebulaPaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nebulaPaint2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nebulaPaint3 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val now = System.currentTimeMillis()

        // 1. Céu cósmico profundo de azul-índigo para negro espacial
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF0D0824.toInt(), 0xFF010004.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Nebulosa cósmica violeta-índigo superior com pulsação suave
        val neb1Pulse = (Math.sin(now * 0.0005) * 0.05 + 1.0).toFloat()
        nebulaPaint1.shader = RadialGradient(
            width * 0.35f, height * 0.35f, width * 0.65f * neb1Pulse,
            0x386366F1.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.35f, height * 0.35f, width * 0.65f * neb1Pulse, nebulaPaint1)

        // 3. Nebulosa cósmica magenta no quadrante inferior
        val neb2Pulse = (Math.cos(now * 0.0007) * 0.06 + 1.0).toFloat()
        nebulaPaint2.shader = RadialGradient(
            width * 0.82f, height * 0.72f, width * 0.55f * neb2Pulse,
            0x2EEC4899.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.82f, height * 0.72f, width * 0.55f * neb2Pulse, nebulaPaint2)

        // 4. Nebulosa ciano-celeste sutil no quadrante inferior esquerdo
        nebulaPaint3.shader = RadialGradient(
            width * 0.15f, height * 0.80f, width * 0.45f,
            0x2238BDF8.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.15f, height * 0.80f, width * 0.45f, nebulaPaint3)

        // 5. Estrelas fixas proeminentes no fundo com cintilação
        val prominentStars = arrayOf(
            floatArrayOf(width * 0.12f, height * 0.18f, 0.0f),
            floatArrayOf(width * 0.48f, height * 0.12f, 1.5f),
            floatArrayOf(width * 0.85f, height * 0.22f, 3.1f),
            floatArrayOf(width * 0.25f, height * 0.50f, 2.2f),
            floatArrayOf(width * 0.70f, height * 0.45f, 4.3f),
            floatArrayOf(width * 0.90f, height * 0.85f, 0.8f)
        )

        for (star in prominentStars) {
            val twinkle = (Math.sin(now * 0.0025 + star[2]) * 0.5 + 0.5).toFloat()
            val alphaGlow = (twinkle * 120).toInt().coerceIn(0, 255)
            starPaint.color = 0x00818CF8.toInt() or (alphaGlow shl 24)
            canvas.drawCircle(star[0], star[1], 8f * twinkle + 4f, starPaint)
            starCorePaint.alpha = (twinkle * 180 + 75).toInt().coerceIn(0, 255)
            canvas.drawCircle(star[0], star[1], 2f, starCorePaint)
        }
    }

    override fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int) {
        system.clear()
        val random = Random(404)
        val count = maxParticles.coerceAtMost(45)

        for (i in 0 until count) {
            val isStar = i % 3 != 0
            if (isStar) {
                // Estrelas cintilantes com rotação lenta
                system.addParticle(
                    Particle(
                        x = random.nextFloat() * width,
                        y = random.nextFloat() * height,
                        vx = (random.nextFloat() - 0.5f) * 0.15f,
                        vy = (random.nextFloat() - 0.5f) * 0.15f,
                        size = random.nextFloat() * 4f + 2f,
                        alpha = random.nextInt(150) + 90,
                        rotation = random.nextFloat() * 360f,
                        rotationSpeed = (random.nextFloat() - 0.5f) * 0.8f,
                        color = 0xFFE0E7FF.toInt(),
                        shape = Particle.Shape.STAR
                    )
                )
            } else {
                // Poeira cósmica luminosa
                system.addParticle(
                    Particle(
                        x = random.nextFloat() * width,
                        y = random.nextFloat() * height,
                        vx = (random.nextFloat() - 0.5f) * 0.25f,
                        vy = (random.nextFloat() - 0.5f) * 0.25f,
                        size = random.nextFloat() * 2f + 1f,
                        alpha = random.nextInt(100) + 80,
                        color = 0xFFA5B4FC.toInt(),
                        shape = Particle.Shape.CIRCLE
                    )
                )
            }
        }
    }
}
