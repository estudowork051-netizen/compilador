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

class FrozenMountainsTheme : SceneryTheme {
    override val type = SceneryType.FROZEN_MOUNTAINS
    override val accentColor = 0xFF7DD3FC.toInt()
    override val slotBorderColor = 0x667DD3FC.toInt()
    override val slotBackgroundColor = 0x220D1924.toInt()
    override val cardBackGradient = intArrayOf(0xFF1E3A5F.toInt(), 0xFF0D1924.toInt())

    private val bgPaint = Paint()
    private val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val snowCapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mountainPath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Fundo em gradiente ártico azul-acinzentado
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF1A2E40.toInt(), 0xFF0D1924.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Picos de montanhas na metade inferior
        mountainPaint.color = 0x44142433.toInt()
        mountainPath.reset()
        mountainPath.moveTo(0f, height)
        val mY = height * 0.72f
        mountainPath.lineTo(0f, mY)
        mountainPath.lineTo(width * 0.25f, mY - 80f)
        mountainPath.lineTo(width * 0.5f, mY)
        mountainPath.lineTo(width * 0.75f, mY - 110f)
        mountainPath.lineTo(width, mY - 20f)
        mountainPath.lineTo(width, height)
        mountainPath.close()
        canvas.drawPath(mountainPath, mountainPaint)

        // Névoa suave na base
        fogPaint.shader = LinearGradient(
            0f, height * 0.75f, 0f, height,
            0x00FFFFFF.toInt(), 0x22BAE6FD.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.75f, width, height, fogPaint)
    }

    override fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int) {
        system.clear()
        val random = Random(101)
        val count = maxParticles.coerceAtMost(40)

        for (i in 0 until count) {
            // Flocos de neve flutuando e caindo serenamente
            system.addParticle(
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    vx = (random.nextFloat() - 0.5f) * 0.4f,
                    vy = random.nextFloat() * 1.2f + 0.6f,
                    size = random.nextFloat() * 3.5f + 1.5f,
                    alpha = random.nextInt(130) + 100,
                    color = 0xFFF0F9FF.toInt(),
                    shape = Particle.Shape.CIRCLE
                )
            )
        }
    }
}
