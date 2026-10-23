package com.paciencia.solitaire.scenery.themes

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.paciencia.solitaire.scenery.Particle
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import java.util.Random

/**
 * Cenário: Deserto Dourado
 * Atmosfera cinematográfica com dunas onduladas em camadas, cristas iluminadas pelo pôr do sol,
 * miragem atmosférica dourada e partículas de areia sopradas pelo vento.
 */
class GoldenDesertTheme : SceneryTheme {
    override val type = SceneryType.GOLDEN_DESERT
    override val accentColor = 0xFFF59E0B.toInt()
    override val slotBorderColor = 0x66F59E0B.toInt()
    override val slotBackgroundColor = 0x221F1106.toInt()
    override val cardBackGradient = intArrayOf(0xFF78350F.toInt(), 0xFF1F1106.toInt())

    private val bgPaint = Paint()
    private val distantDunePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foregroundDunePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crestLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0x55FDE68A.toInt()
    }

    private val distantDunePath = Path()
    private val foreDunePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val now = System.currentTimeMillis()

        // 1. Céu crepuscular de deserto: âmbar quente para terracota profundo
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF381404.toInt(), 0xFF160B03.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Brilho do sol poente e miragem atmosférica no horizonte
        val sunPulse = (Math.sin(now * 0.001) * 0.08 + 1.0).toFloat()
        sunGlowPaint.shader = RadialGradient(
            width * 0.5f, height * 0.62f, width * 0.55f * sunPulse,
            0x3DFBBF24.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width * 0.5f, height * 0.62f, width * 0.55f * sunPulse, sunGlowPaint)

        // 3. Camada 1: Dunas distantes no horizonte
        distantDunePaint.color = 0x4A251006.toInt()
        distantDunePath.reset()
        distantDunePath.moveTo(0f, height)
        val dY1 = height * 0.74f
        distantDunePath.lineTo(0f, dY1)
        distantDunePath.quadTo(width * 0.28f, dY1 - 45f, width * 0.60f, dY1 + 15f)
        distantDunePath.quadTo(width * 0.85f, dY1 + 50f, width, dY1 - 20f)
        distantDunePath.lineTo(width, height)
        distantDunePath.close()
        canvas.drawPath(distantDunePath, distantDunePaint)

        // 4. Camada 2: Duna frontal sinuosa com relevo e sombra
        foregroundDunePaint.color = 0x66180A04.toInt()
        foreDunePath.reset()
        foreDunePath.moveTo(0f, height)
        val dY2 = height * 0.82f
        foreDunePath.lineTo(0f, dY2)
        foreDunePath.quadTo(width * 0.35f, dY2 + 35f, width * 0.70f, dY2 - 35f)
        foreDunePath.quadTo(width * 0.88f, dY2 - 55f, width, dY2 - 30f)
        foreDunePath.lineTo(width, height)
        foreDunePath.close()
        canvas.drawPath(foreDunePath, foregroundDunePaint)

        // 5. Crista iluminada com brilho dourado sobre a duna principal
        canvas.drawPath(foreDunePath, crestLightPaint)
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
                    vx = random.nextFloat() * 1.6f + 0.6f, // deslocamento lateral do vento
                    vy = (random.nextFloat() - 0.35f) * 0.4f,
                    size = random.nextFloat() * 2.8f + 1f,
                    alpha = random.nextInt(120) + 90,
                    color = 0xFFFDE68A.toInt(),
                    shape = Particle.Shape.CIRCLE
                )
            )
        }
    }
}
