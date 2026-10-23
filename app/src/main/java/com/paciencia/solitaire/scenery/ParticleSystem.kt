package com.paciencia.solitaire.scenery

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Sistema de partículas ultra-leve renderizado diretamente em Canvas.
 */
class ParticleSystem {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun clear() {
        particles.clear()
    }

    fun addParticle(particle: Particle) {
        particles.add(particle)
    }

    fun update(screenWidth: Float, screenHeight: Float) {
        for (i in 0 until particles.size) {
            particles[i].update(screenWidth, screenHeight)
        }
    }

    fun draw(canvas: Canvas) {
        for (i in 0 until particles.size) {
            val p = particles[i]
            paint.color = p.color
            paint.alpha = p.alpha

            when (p.shape) {
                Particle.Shape.CIRCLE -> {
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                Particle.Shape.LEAF -> {
                    canvas.save()
                    canvas.translate(p.x, p.y)
                    canvas.rotate(p.rotation)
                    path.reset()
                    path.moveTo(0f, -p.size)
                    path.quadTo(p.size, 0f, 0f, p.size)
                    path.quadTo(-p.size, 0f, 0f, -p.size)
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
                Particle.Shape.STAR -> {
                    canvas.save()
                    canvas.translate(p.x, p.y)
                    canvas.rotate(p.rotation)
                    val s = p.size
                    // Desenha estrela de 4 pontas
                    path.reset()
                    path.moveTo(0f, -s)
                    path.lineTo(s * 0.25f, -s * 0.25f)
                    path.lineTo(s, 0f)
                    path.lineTo(s * 0.25f, s * 0.25f)
                    path.lineTo(0f, s)
                    path.lineTo(-s * 0.25f, s * 0.25f)
                    path.lineTo(-s, 0f)
                    path.lineTo(-s * 0.25f, -s * 0.25f)
                    path.close()
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
                Particle.Shape.SPARK -> {
                    canvas.drawRect(p.x - p.size * 0.5f, p.y - p.size, p.x + p.size * 0.5f, p.y + p.size, paint)
                }
            }
        }
    }
}
