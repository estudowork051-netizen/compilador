package com.paciencia.solitaire.scenery.themes

import android.graphics.Canvas
import android.graphics.Color
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
 * Cenário: Reino Medieval
 * Atmosfera de fantasia heróica com cidadela gótica, tochas crepitantes com reflexo dinâmico,
 * janelas iluminadas por velas, flâmulas ao vento e faíscas incandescentes.
 */
class MedievalKingdomTheme : SceneryTheme {
    override val type = SceneryType.MEDIEVAL_KINGDOM
    override val accentColor = 0xFFC084FC.toInt()
    override val slotBorderColor = 0x66C084FC.toInt()
    override val slotBackgroundColor = 0x2212091C.toInt()
    override val cardBackGradient = intArrayOf(0xFF3B1D54.toInt(), 0xFF12091C.toInt())

    private val bgPaint = Paint()
    private val distantCitadelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val castlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val torchGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val torchCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFBEB.toInt()
    }
    private val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE11D48.toInt()
    }
    private val bannerPolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4AF37.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val distantPath = Path()
    private val castlePath = Path()
    private val bannerPath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val now = System.currentTimeMillis()

        // 1. Céu crepuscular místico: púrpura profundo a azul-noite
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF270E43.toInt(), 0xFF0B0414.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Silhueta de muralhas distantes no horizonte (Camada 1)
        distantCitadelPaint.color = 0x24180A28.toInt()
        distantPath.reset()
        distantPath.moveTo(0f, height)
        val dY = height * 0.74f
        distantPath.lineTo(0f, dY)
        distantPath.lineTo(width * 0.35f, dY - 40f)
        distantPath.lineTo(width * 0.45f, dY - 80f)
        distantPath.lineTo(width * 0.55f, dY - 40f)
        distantPath.lineTo(width * 0.85f, dY - 20f)
        distantPath.lineTo(width, dY - 50f)
        distantPath.lineTo(width, height)
        distantPath.close()
        canvas.drawPath(distantPath, distantCitadelPaint)

        // 3. Castelo em primeiro plano com torres e ameias (Camada 2)
        castlePaint.color = 0x66080210.toInt()
        castlePath.reset()
        castlePath.moveTo(0f, height)
        val cY = height * 0.80f
        castlePath.lineTo(0f, cY)

        // Torre esquerda com ameias
        castlePath.lineTo(width * 0.12f, cY)
        castlePath.lineTo(width * 0.12f, cY - 75f)
        castlePath.lineTo(width * 0.15f, cY - 75f)
        castlePath.lineTo(width * 0.15f, cY - 70f)
        castlePath.lineTo(width * 0.18f, cY - 70f)
        castlePath.lineTo(width * 0.18f, cY - 75f)
        castlePath.lineTo(width * 0.22f, cY - 75f)
        castlePath.lineTo(width * 0.22f, cY - 25f)

        // Muralha central
        castlePath.lineTo(width * 0.62f, cY - 25f)

        // Torre principal e pináculo
        castlePath.lineTo(width * 0.62f, cY - 110f)
        castlePath.lineTo(width * 0.65f, cY - 110f)
        castlePath.lineTo(width * 0.65f, cY - 105f)
        castlePath.lineTo(width * 0.68f, cY - 105f)
        castlePath.lineTo(width * 0.68f, cY - 110f)
        castlePath.lineTo(width * 0.76f, cY - 110f)
        castlePath.lineTo(width * 0.76f, cY)
        castlePath.lineTo(width, cY)
        castlePath.lineTo(width, height)
        castlePath.close()
        canvas.drawPath(castlePath, castlePaint)

        // 4. Janelas em arco com luz de velas acolhedora
        windowPaint.color = 0x88FDE047.toInt()
        canvas.drawRoundRect(width * 0.155f, cY - 50f, width * 0.185f, cY - 32f, 6f, 6f, windowPaint)
        canvas.drawRoundRect(width * 0.665f, cY - 80f, width * 0.715f, cY - 55f, 8f, 8f, windowPaint)

        // 5. Flâmulas balançando com o vento no topo das torres
        val bannerWave = Math.sin(now * 0.003).toFloat() * 6f
        // Haste e flâmula torre esquerda
        canvas.drawLine(width * 0.17f, cY - 75f, width * 0.17f, cY - 95f, bannerPolePaint)
        bannerPath.reset()
        bannerPath.moveTo(width * 0.17f, cY - 95f)
        bannerPath.lineTo(width * 0.22f + bannerWave, cY - 90f)
        bannerPath.lineTo(width * 0.17f, cY - 85f)
        bannerPath.close()
        canvas.drawPath(bannerPath, bannerPaint)

        // 6. Tochas com cintilação dinâmica e halos pulsantes
        val torch1Flicker = (Math.sin(now * 0.008) * 0.15 + Math.cos(now * 0.013) * 0.1 + 0.85).toFloat()
        val torch2Flicker = (Math.sin(now * 0.007 + 1.2) * 0.15 + Math.cos(now * 0.011) * 0.1 + 0.85).toFloat()

        drawTorch(canvas, width * 0.20f, cY - 68f, torch1Flicker)
        drawTorch(canvas, width * 0.74f, cY - 102f, torch2Flicker)
    }

    private fun drawTorch(canvas: Canvas, x: Float, y: Float, flicker: Float) {
        val radius = 22f * flicker
        torchGlowPaint.shader = RadialGradient(
            x, y, radius,
            0x66F97316.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius, torchGlowPaint)
        canvas.drawCircle(x, y, 3.5f * flicker, torchCorePaint)
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
                    vy = -random.nextFloat() * 1.3f - 0.5f,
                    size = random.nextFloat() * 3f + 1.5f,
                    alpha = random.nextInt(140) + 100,
                    color = if (i % 2 == 0) 0xFFFB923C.toInt() else 0xFFFDE047.toInt(),
                    shape = Particle.Shape.SPARK
                )
            )
        }
    }
}
