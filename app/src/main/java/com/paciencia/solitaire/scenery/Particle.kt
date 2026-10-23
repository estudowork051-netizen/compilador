package com.paciencia.solitaire.scenery

/**
 * Representa uma partícula visual individual com suporte a física, ciclo de vida e formas temáticas.
 */
data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var size: Float = 4f,
    var alpha: Int = 255,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var color: Int = 0xFFFFFFFF.toInt(),
    var shape: Shape = Shape.CIRCLE,
    var isTemporary: Boolean = false,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var scaleSpeed: Float = 0f
) {
    enum class Shape {
        CIRCLE,
        STAR,
        LEAF,
        SPARK,
        RUNE,
        CRYSTAL,
        RING
    }

    val isAlive: Boolean
        get() = !isTemporary || life > 0f

    fun update(screenWidth: Float, screenHeight: Float) {
        x += vx
        y += vy
        rotation += rotationSpeed
        size = (size + scaleSpeed).coerceAtLeast(0.5f)

        if (isTemporary) {
            life -= 0.025f
            val ratio = (life / maxLife).coerceIn(0f, 1f)
            alpha = (ratio * 255).toInt()
        } else {
            // Efeito de teletransporte cíclico nas bordas da tela
            if (y > screenHeight + 20) {
                y = -20f
            } else if (y < -20) {
                y = screenHeight + 20f
            }

            if (x > screenWidth + 20) {
                x = -20f
            } else if (x < -20) {
                x = screenWidth + 20f
            }
        }
    }
}

