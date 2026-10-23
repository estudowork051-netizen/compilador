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

/**
 * Cenário: Montanhas Geladas
 * Atmosfera ártica com cordilheiras em múltiplos planos de profundidade,
 * reflexos azulados glaciais, aurora boreal sutil e névoa polar.
 */
class FrozenMountainsTheme : SceneryTheme {
    override val type = SceneryType.FROZEN_MOUNTAINS
    override val accentColor = 0xFF7DD3FC.toInt()
    override val slotBorderColor = 0x667DD3FC.toInt()
    override val slotBackgroundColor = 0x220D1924.toInt()
    override val cardBackGradient = intArrayOf(0xFF1E3A5F.toInt(), 0xFF0D1924.toInt())

    private val bgPaint = Paint()
    private val auroraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val distantMountainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val midMountainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val snowRidgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val distantPath = Path()
    private val midPath = Path()
    private val ridgePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val now = System.currentTimeMillis()

        // 1. Fundo ártico azul-noite profundo
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF122233.toInt(), 0xFF081018.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Aurora glacial suave no céu superior com oscilação mística
        val auroraShift = Math.sin(now * 0.0006).toFloat() * 30f
        auroraPaint.shader = LinearGradient(
            width * 0.2f + auroraShift, 0f, width * 0.8f - auroraShift, height * 0.45f,
            0x2638BDF8.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height * 0.5f, auroraPaint)

        // 3. Camada 1: Picos distantes (fundo)
        distantMountainPaint.color = 0x301E354D.toInt()
        distantPath.reset()
        distantPath.moveTo(0f, height)
        val dM = height * 0.68f
        distantPath.lineTo(0f, dM)
        distantPath.lineTo(width * 0.22f, dM - 75f)
        distantPath.lineTo(width * 0.48f, dM + 15f)
        distantPath.lineTo(width * 0.72f, dM - 95f)
        distantPath.lineTo(width, dM - 20f)
        distantPath.lineTo(width, height)
        distantPath.close()
        canvas.drawPath(distantPath, distantMountainPaint)

        // 4. Camada 2: Montanhas intermediárias recortadas
        midMountainPaint.color = 0x550C1B2B.toInt()
        midPath.reset()
        midPath.moveTo(0f, height)
        val mM = height * 0.76f
        midPath.lineTo(0f, mM)
        midPath.lineTo(width * 0.15f, mM - 90f)
        midPath.lineTo(width * 0.38f, mM - 25f)
        midPath.lineTo(width * 0.62f, mM - 110f)
        midPath.lineTo(width * 0.85f, mM - 45f)
        midPath.lineTo(width, mM - 15f)
        midPath.lineTo(width, height)
        midPath.close()
        canvas.drawPath(midPath, midMountainPaint)

        // 5. Reflexos azulados de neve nas cristas frontais
        snowRidgePaint.shader = LinearGradient(
            width * 0.5f, mM - 110f, width * 0.5f, mM,
            0x44BAE6FD.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        ridgePath.reset()
        ridgePath.moveTo(width * 0.55f, mM - 110f)
        ridgePath.lineTo(width * 0.62f, mM - 110f)
        ridgePath.lineTo(width * 0.72f, mM - 30f)
        ridgePath.lineTo(width * 0.50f, mM - 30f)
        ridgePath.close()
        canvas.drawPath(ridgePath, snowRidgePaint)

        // 6. Névoa ártica na base das montanhas
        fogPaint.shader = LinearGradient(
            0f, height * 0.72f, 0f, height,
            0x00000000.toInt(), 0x2E38BDF8.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.72f, width, height, fogPaint)
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
                    vx = (random.nextFloat() - 0.5f) * 0.5f,
                    vy = random.nextFloat() * 1.3f + 0.5f,
                    size = random.nextFloat() * 3.5f + 1.5f,
                    alpha = random.nextInt(130) + 110,
                    color = 0xFFF0F9FF.toInt(),
                    shape = Particle.Shape.CIRCLE
                )
            )
        }
    }
}
