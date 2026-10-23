package com.paciencia.solitaire.scenery

/**
 * Representa uma partícula visual individual (folha, floco de neve, grão de areia, estrela, faísca).
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
    var shape: Shape = Shape.CIRCLE
) {
    enum class Shape {
        CIRCLE,
        STAR,
        LEAF,
        SPARK
    }

    fun update(screenWidth: Float, screenHeight: Float) {
        x += vx
        y += vy
        rotation += rotationSpeed

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
