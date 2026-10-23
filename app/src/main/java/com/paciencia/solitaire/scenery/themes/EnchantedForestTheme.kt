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

class EnchantedForestTheme : SceneryTheme {
    override val type = SceneryType.ENCHANTED_FOREST
    override val accentColor = 0xFF34D399.toInt()
    override val slotBorderColor = 0x6634D399.toInt()
    override val slotBackgroundColor = 0x2205180E.toInt()
    override val cardBackGradient = intArrayOf(0xFF0F3824.toInt(), 0xFF05180E.toInt())

    private val bgPaint = Paint()
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPath = Path()
    private val treePath = Path()

    override fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Fundo em gradiente vertical verde místico
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            0xFF0F2E1E.toInt(), 0xFF05180E.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Raios suaves de luz solar atravessando as copas
        rayPaint.shader = LinearGradient(
            0f, 0f, width * 0.7f, height * 0.6f,
            0x33A7F3D0.toInt(), 0x00000000.toInt(),
            Shader.TileMode.CLAMP
        )
        rayPath.reset()
        rayPath.moveTo(0f, 0f)
        rayPath.lineTo(width * 0.4f, 0f)
        rayPath.lineTo(width * 0.9f, height * 0.8f)
        rayPath.lineTo(width * 0.3f, height * 0.8f)
        rayPath.close()
        canvas.drawPath(rayPath, rayPaint)

        // Silhueta distante de árvores no terço inferior
        silhouettePaint.color = 0x33031008.toInt()
        treePath.reset()
        treePath.moveTo(0f, height)
        val treeBase = height * 0.85f
        treePath.lineTo(0f, treeBase)
        var curX = 0f
        val step = width / 6f
        while (curX < width) {
            treePath.lineTo(curX + step * 0.5f, treeBase - 45f)
            treePath.lineTo(curX + step, treeBase)
            curX += step
        }
        treePath.lineTo(width, height)
        treePath.close()
        canvas.drawPath(treePath, silhouettePaint)
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
