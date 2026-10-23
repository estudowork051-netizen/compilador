package com.paciencia.solitaire.scenery.themes

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.paciencia.solitaire.scenery.Particle
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import java.util.Random

class GoldenDesertTheme : SceneryTheme {
    override val type = SceneryType.GOLDEN_DESERT
    override val accentColor = 0xFFF59E0B.toInt()
    override val slotBorderColor = 0x66F59E0B.toInt()
    override val slotBackgroundColor = 0x221F1106.toInt()
    override val cardBackGradient = intArrayOf(0xFF78350F.toInt(), 0xFF1F1106.toInt())

    private val bgPaint = Paint()
    private val dunePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dunePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Céu de entardecer desértico: laranja âmbar a marrom terracota
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF451A03.toInt(), 0xFF1F1106.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Brilho do sol poente no horizonte
        sunGlowPaint.shader = LinearGradient(
            width * 0.5f, height * 0.5f, width * 0.5f, height * 0.8f,
            0x44FBBF24.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.5f, height * 0.65f, width * 0.45f, sunGlowPaint)

        // Dunas onduladas no horizonte
        dunePaint.color = 0x55271207.toInt()
        dunePath.reset()
        dunePath.moveTo(0f, height)
        val dY = height * 0.78f
        dunePath.lineTo(0f, dY)
        dunePath.quadTo(width * 0.25f, dY - 40f, width * 0.55f, dY + 10f)
        dunePath.quadTo(width * 0.8f, dY + 40f, width, dY - 25f)
        dunePath.lineTo(width, height)
        dunePath.close()
        canvas.drawPath(dunePath, dunePaint)
    }

    override fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int) {
        system.clear()
        val random = Random(202)
        val count = maxParticles.coerceAtMost(35)

        for (i in 0 until count) {
            // Partículas douradas de areia impulsionadas por brisa suave
            system.addParticle(
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    vx = random.nextFloat() * 1.5f + 0.5f, // deslocamento lateral do vento
                    vy = (random.nextFloat() - 0.3f) * 0.4f,
                    size = random.nextFloat() * 2.5f + 1f,
                    alpha = random.nextInt(110) + 90,
                    color = 0xFFFDE68A.toInt(),
                    shape = Particle.Shape.CIRCLE
                )
            )
        }
    }
}
