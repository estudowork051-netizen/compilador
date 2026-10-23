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

class MedievalKingdomTheme : SceneryTheme {
    override val type = SceneryType.MEDIEVAL_KINGDOM
    override val accentColor = 0xFFC084FC.toInt()
    override val slotBorderColor = 0x66C084FC.toInt()
    override val slotBackgroundColor = 0x2212091C.toInt()
    override val cardBackGradient = intArrayOf(0xFF3B1D54.toInt(), 0xFF12091C.toInt())

    private val bgPaint = Paint()
    private val castlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val torchGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val castlePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Céu crepuscular místico: púrpura profundo a azul-noite
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF2E1065.toInt(), 0xFF0F061E.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Silhueta das muralhas e torres do castelo
        castlePaint.color = 0x440A0314.toInt()
        castlePath.reset()
        castlePath.moveTo(0f, height)
        val cY = height * 0.8f
        castlePath.lineTo(0f, cY)
        // Torre esquerda
        castlePath.lineTo(width * 0.15f, cY)
        castlePath.lineTo(width * 0.15f, cY - 70f)
        castlePath.lineTo(width * 0.28f, cY - 70f)
        castlePath.lineTo(width * 0.28f, cY - 20f)
        // Muralha central com ameias
        castlePath.lineTo(width * 0.65f, cY - 20f)
        // Grande torre do castelo
        castlePath.lineTo(width * 0.65f, cY - 95f)
        castlePath.lineTo(width * 0.82f, cY - 95f)
        castlePath.lineTo(width * 0.82f, cY)
        castlePath.lineTo(width, cY)
        castlePath.lineTo(width, height)
        castlePath.close()
        canvas.drawPath(castlePath, castlePaint)

        // Brilho quente de tochas nas ameias
        torchGlowPaint.color = 0x44F97316.toInt()
        canvas.drawCircle(width * 0.21f, cY - 65f, 18f, torchGlowPaint)
        canvas.drawCircle(width * 0.73f, cY - 90f, 22f, torchGlowPaint)
    }

    override fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int) {
        system.clear()
        val random = Random(303)
        val count = maxParticles.coerceAtMost(35)

        for (i in 0 until count) {
            // Faíscas e centelhas incandescentes de tochas subindo suavemente
            system.addParticle(
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    vx = (random.nextFloat() - 0.4f) * 0.6f,
                    vy = -random.nextFloat() * 1.2f - 0.4f,
                    size = random.nextFloat() * 3f + 1.5f,
                    alpha = random.nextInt(140) + 100,
                    color = if (i % 2 == 0) 0xFFFB923C.toInt() else 0xFFFBBF24.toInt(),
                    shape = Particle.Shape.SPARK
                )
            )
        }
    }
}
