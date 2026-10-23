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
 * Cenário: Floresta Encantada
 * Atmosfera mística esmeralda com copas em múltiplos planos, névoa terrena suave,
 * raios crepusculares dinâmicos e vaga-lumes bioluminescentes pulsantes.
 */
class EnchantedForestTheme : SceneryTheme {
    override val type = SceneryType.ENCHANTED_FOREST
    override val accentColor = 0xFF34D399.toInt()
    override val slotBorderColor = 0x6634D399.toInt()
    override val slotBackgroundColor = 0x2205180E.toInt()
    override val cardBackGradient = intArrayOf(0xFF0F3824.toInt(), 0xFF05180E.toInt())

    private val bgPaint = Paint()
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val distantForestPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foregroundTreesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fireflyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fireflyCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFDE047.toInt()
    }

    private val rayPath = Path()
    private val distantTreePath = Path()
    private val foreTreePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val now = System.currentTimeMillis()

        // 1. Fundo em gradiente vertical verde místico profundo
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF0D281A.toInt(), 0xFF04120A.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Raios suaves de luz solar filtrada pelas copas com oscilação sutil
        val rayOffset = Math.sin(now * 0.0008).toFloat() * (width * 0.03f)
        rayPaint.shader = LinearGradient(
            0f, 0f, width * 0.7f + rayOffset, height * 0.65f,
            0x26A7F3D0.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        rayPath.reset()
        rayPath.moveTo(0f, 0f)
        rayPath.lineTo(width * 0.35f, 0f)
        rayPath.lineTo(width * 0.85f + rayOffset, height * 0.85f)
        rayPath.lineTo(width * 0.25f + rayOffset, height * 0.85f)
        rayPath.close()
        canvas.drawPath(rayPath, rayPaint)

        // 3. Camada 1: Silhueta de floresta distante (fundo)
        distantForestPaint.color = 0x2E020B05.toInt()
        distantTreePath.reset()
        distantTreePath.moveTo(0f, height)
        val distBase = height * 0.80f
        distantTreePath.lineTo(0f, distBase)
        var curX = 0f
        val step = width / 7f
        while (curX < width) {
            distantTreePath.lineTo(curX + step * 0.5f, distBase - 55f)
            distantTreePath.lineTo(curX + step, distBase)
            curX += step
        }
        distantTreePath.lineTo(width, height)
        distantTreePath.close()
        canvas.drawPath(distantTreePath, distantForestPaint)

        // 4. Camada 2: Floresta em primeiro plano com troncos e folhagens mais escuras
        foregroundTreesPaint.color = 0x55010603.toInt()
        foreTreePath.reset()
        foreTreePath.moveTo(0f, height)
        val foreBase = height * 0.88f
        foreTreePath.lineTo(0f, foreBase)
        curX = 0f
        val foreStep = width / 5f
        while (curX < width) {
            foreTreePath.lineTo(curX + foreStep * 0.5f, foreBase - 35f)
            foreTreePath.lineTo(curX + foreStep, foreBase)
            curX += foreStep
        }
        foreTreePath.lineTo(width, height)
        foreTreePath.close()
        canvas.drawPath(foreTreePath, foregroundTreesPaint)

        // 5. Névoa suave na base terrena
        mistPaint.shader = LinearGradient(
            0f, height * 0.82f, 0f, height,
            0x00000000.toInt(), 0x2210B981.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.82f, width, height, mistPaint)

        // 6. Vaga-lumes bioluminescentes vivos com pulsação suave
        val fireflies = arrayOf(
            floatArrayOf(width * 0.15f, height * 0.62f, 0.0f),
            floatArrayOf(width * 0.42f, height * 0.52f, 1.8f),
            floatArrayOf(width * 0.78f, height * 0.58f, 3.2f),
            floatArrayOf(width * 0.88f, height * 0.72f, 4.5f),
            floatArrayOf(width * 0.28f, height * 0.76f, 2.4f)
        )

        for (ff in fireflies) {
            val fx = ff[0] + Math.sin(now * 0.001 + ff[2]).toFloat() * 12f
            val fy = ff[1] + Math.cos(now * 0.0015 + ff[2]).toFloat() * 8f
            val pulse = (Math.sin(now * 0.003 + ff[2]) * 0.5 + 0.5).toFloat() // 0.0 .. 1.0
            val alphaGlow = (pulse * 90).toInt().coerceIn(0, 255)

            // Halo suave translúcido
            fireflyPaint.color = 0x0034D399.toInt() or (alphaGlow shl 24)
            canvas.drawCircle(fx, fy, 14f * pulse + 6f, fireflyPaint)

            // Ponto de luz central
            fireflyCorePaint.alpha = (pulse * 200 + 55).toInt().coerceIn(0, 255)
            canvas.drawCircle(fx, fy, 2.5f, fireflyCorePaint)
        }
    }

    override fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int) {
        system.clear()
        val random = Random(42)
        val count = maxParticles.coerceAtMost(35)

        for (i in 0 until count) {
            val isLeaf = i % 2 == 0
            if (isLeaf) {
                // Folhas verdes balançando
                system.addParticle(
                    Particle(
                        x = random.nextFloat() * width,
                        y = random.nextFloat() * height,
                        vx = (random.nextFloat() * 0.6f + 0.2f),
                        vy = (random.nextFloat() * 0.8f + 0.4f),
                        size = random.nextFloat() * 6f + 5f,
                        alpha = random.nextInt(90) + 120,
                        rotation = random.nextFloat() * 360f,
                        rotationSpeed = (random.nextFloat() - 0.5f) * 2f,
                        color = 0xFF4ADE80.toInt(),
                        shape = Particle.Shape.LEAF
                    )
                )
            } else {
                // Partículas luminosas de pólen mágico
                system.addParticle(
                    Particle(
                        x = random.nextFloat() * width,
                        y = random.nextFloat() * height,
                        vx = (random.nextFloat() - 0.5f) * 0.4f,
                        vy = -random.nextFloat() * 0.5f - 0.1f,
                        size = random.nextFloat() * 3f + 2f,
                        alpha = random.nextInt(120) + 100,
                        color = 0xFFFDE047.toInt(),
                        shape = Particle.Shape.CIRCLE
                    )
                )
            }
        }
    }
}
