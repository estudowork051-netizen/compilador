package com.paciencia.solitaire.scenery

import android.graphics.Canvas

/**
 * Interface base para os cinco ambientes visuais do jogo.
 */
interface SceneryTheme {
    val type: SceneryType
    val accentColor: Int
    val slotBorderColor: Int
    val slotBackgroundColor: Int
    val cardBackGradient: IntArray

    /**
     * Renderiza o fundo procedural do cenário (gradientes, relevos, silhuetas).
     */
    fun drawBackground(canvas: Canvas, width: Float, height: Float)

    /**
     * Inicializa a população de partículas características deste ambiente.
     */
    fun initParticles(system: ParticleSystem, width: Float, height: Float, maxParticles: Int)
}
