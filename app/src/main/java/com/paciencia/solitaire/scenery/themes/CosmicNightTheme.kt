package com.paciencia.solitaire.scenery.themes

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.paciencia.solitaire.scenery.Particle
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import java.util.Random

class CosmicNightTheme : SceneryTheme {
    override val type = SceneryType.COSMIC_NIGHT
    override val accentColor = 0xFF818CF8.toInt()
    override val slotBorderColor = 0x66818CF8.toInt()
    override val slotBackgroundColor = 0x22050212.toInt()
    override val cardBackGradient = intArrayOf(0xFF1E1038.toInt(), 0xFF050212.toInt())

    private val bgPaint = Paint()
    private val nebulaPaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nebulaPaint2 = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Céu cósmico profundo de azul-índigo para negro espacial
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF0F0C29.toInt(), 0xFF020005.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Nebulosa cósmica violeta
        nebulaPaint1.shader = RadialGradient(
            width * 0.3f, height * 0.4f, width * 0.6f,
            0x336366F1.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.3f, height * 0.4f, width * 0.6f, nebulaPaint1)

        // Nebulosa cósmica magenta no quadrante inferior
        nebulaPaint2.shader = RadialGradient(
            width * 0.8f, height * 0.75f, width * 0.5f,
            0x26EC4899.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.8f, height * 0.75f, width * 0.5f, nebulaPaint2)
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
