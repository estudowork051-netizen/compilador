package com.paciencia.solitaire.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.AvatarArchetype

/**
 * Renderizador vetorial de retratos artísticos de alta fidelidade para os Avatares Dark Fantasy.
 * Desenha personagens com elmos, capuzes, máscaras e olhos de energia autênticos sem emojis.
 */
class AvatarRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x88000000.toInt()
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val innerAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val charPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val rectF = RectF()

    fun drawAvatar(canvas: Canvas, avatar: Avatar, cx: Float, cy: Float, radius: Float) {
        val r = radius
        if (r <= 4f) return

        // 1. Sombra de Profundidade
        canvas.drawCircle(cx, cy + r * 0.08f, r, shadowPaint)

        // 2. Fundo Gradiente Atmosférico
        bgPaint.shader = RadialGradient(
            cx, cy - r * 0.2f, r * 1.1f,
            avatar.primaryColor, avatar.bgColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, bgPaint)

        // 3. Ilustração do Personagem Fantasia
        drawCharacterPortrait(canvas, avatar, cx, cy, r)

        // 4. Brilho Interno da Moldura
        innerAuraPaint.color = avatar.borderColor
        innerAuraPaint.strokeWidth = r * 0.05f
        innerAuraPaint.alpha = 180
        canvas.drawCircle(cx, cy, r - innerAuraPaint.strokeWidth * 0.5f, innerAuraPaint)

        // 5. Moldura Externa de Metal Nobre
        framePaint.color = avatar.borderColor
        framePaint.strokeWidth = r * 0.08f
        framePaint.alpha = 255
        canvas.drawCircle(cx, cy, r - framePaint.strokeWidth * 0.5f, framePaint)

        // Rebites / Nós nos 4 pontos cardeais
        val rivetR = r * 0.06f
        glowPaint.color = 0xFFFFFFFF.toInt()
        glowPaint.alpha = 220
        canvas.drawCircle(cx, cy - r + framePaint.strokeWidth * 0.5f, rivetR, glowPaint)
        canvas.drawCircle(cx, cy + r - framePaint.strokeWidth * 0.5f, rivetR, glowPaint)
        canvas.drawCircle(cx - r + framePaint.strokeWidth * 0.5f, cy, rivetR, glowPaint)
        canvas.drawCircle(cx + r - framePaint.strokeWidth * 0.5f, cy, rivetR, glowPaint)
    }

    private fun drawCharacterPortrait(canvas: Canvas, avatar: Avatar, cx: Float, cy: Float, r: Float) {
        val s = r * 0.85f
        val arch = avatar.archetype

        // Manto / Ombros de Base
        charPaint.color = avatar.secondaryColor
        path.reset()
        path.moveTo(cx - s * 0.75f, cy + s)
        path.quadTo(cx - s * 0.55f, cy + s * 0.25f, cx, cy + s * 0.35f)
        path.quadTo(cx + s * 0.55f, cy + s * 0.25f, cx + s * 0.75f, cy + s)
        path.close()
        canvas.drawPath(path, charPaint)

        when (arch) {
            AvatarArchetype.ADVENTURER -> {
                // Chapéu com aba dobrada e pena dourada
                charPaint.color = 0xFF451A03.toInt()
                path.reset()
                path.moveTo(cx - s * 0.7f, cy)
                path.quadTo(cx, cy - s * 0.15f, cx + s * 0.65f, cy - s * 0.25f)
                path.lineTo(cx + s * 0.45f, cy + s * 0.1f)
                path.quadTo(cx, cy + s * 0.05f, cx - s * 0.65f, cy + s * 0.08f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Copa do Chapéu
                charPaint.color = 0xFF78350F.toInt()
                path.reset()
                path.moveTo(cx - s * 0.45f, cy)
                path.lineTo(cx - s * 0.35f, cy - s * 0.6f)
                path.quadTo(cx, cy - s * 0.7f, cx + s * 0.35f, cy - s * 0.55f)
                path.lineTo(cx + s * 0.45f, cy - s * 0.1f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Pena dourada
                charPaint.color = 0xFFFBBF24.toInt()
                path.reset()
                path.moveTo(cx + s * 0.15f, cy - s * 0.3f)
                path.quadTo(cx + s * 0.55f, cy - s * 0.85f, cx + s * 0.35f, cy - s * 0.95f)
                path.quadTo(cx + s * 0.2f, cy - s * 0.6f, cx + s * 0.1f, cy - s * 0.25f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Rosto sombreado e olhos afiados
                drawEyes(canvas, cx, cy + s * 0.15f, s * 0.18f, 0xFFFDE68A.toInt())
            }

            AvatarArchetype.ARCANE_MAGE -> {
                // Capuz Arcano
                charPaint.color = 0xFF2E1065.toInt()
                path.reset()
                path.moveTo(cx, cy - s * 0.95f) // Ponta do capuz
                path.lineTo(cx + s * 0.55f, cy + s * 0.2f)
                path.lineTo(cx - s * 0.55f, cy + s * 0.2f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Vazio interior do capuz
                charPaint.color = 0xFF0F0728.toInt()
                canvas.drawCircle(cx, cy - s * 0.15f, s * 0.32f, charPaint)

                // Olhos arcanos brilhantes (chamas astrais)
                drawGlowingEyes(canvas, cx, cy - s * 0.16f, s * 0.22f, 0xFF38BDF8.toInt())

                // Runa mágica na testa
                detailPaint.color = 0xFF818CF8.toInt()
                detailPaint.strokeWidth = s * 0.04f
                canvas.drawLine(cx, cy - s * 0.45f, cx, cy - s * 0.32f, detailPaint)
                canvas.drawLine(cx - s * 0.08f, cy - s * 0.38f, cx + s * 0.08f, cy - s * 0.38f, detailPaint)
            }

            AvatarArchetype.MEDIEVAL_KNIGHT -> {
                // Elmo de Aço Fechado
                charPaint.color = 0xFF475569.toInt()
                rectF.set(cx - s * 0.38f, cy - s * 0.65f, cx + s * 0.38f, cy + s * 0.22f)
                canvas.drawRoundRect(rectF, s * 0.2f, s * 0.2f, charPaint)

                // Pluma do Elmo
                charPaint.color = 0xFFE2E8F0.toInt()
                path.reset()
                path.moveTo(cx, cy - s * 0.65f)
                path.quadTo(cx + s * 0.4f, cy - s * 1.0f, cx - s * 0.1f, cy - s * 0.95f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Visor T-Shaped de Aço Dourado
                detailPaint.color = 0xFFFCD34D.toInt()
                detailPaint.strokeWidth = s * 0.06f
                // Barra horizontal dos olhos
                canvas.drawLine(cx - s * 0.24f, cy - s * 0.12f, cx + s * 0.24f, cy - s * 0.12f, detailPaint)
                // Barra vertical do respirador
                canvas.drawLine(cx, cy - s * 0.12f, cx, cy + s * 0.15f, detailPaint)

                // Brilho interior do visor
                drawEyes(canvas, cx, cy - s * 0.12f, s * 0.15f, 0xFF60A5FA.toInt())
            }

            AvatarArchetype.FOREST_RANGER -> {
                // Capuz de Folhas Élfico
                charPaint.color = 0xFF064E3B.toInt()
                path.reset()
                path.moveTo(cx, cy - s * 0.85f)
                path.quadTo(cx + s * 0.6f, cy - s * 0.2f, cx + s * 0.45f, cy + s * 0.25f)
                path.lineTo(cx - s * 0.45f, cy + s * 0.25f)
                path.quadTo(cx - s * 0.6f, cy - s * 0.2f, cx, cy - s * 0.85f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Folha frontal
                charPaint.color = 0xFF059669.toInt()
                path.reset()
                path.moveTo(cx, cy - s * 0.85f)
                path.quadTo(cx + s * 0.2f, cy - s * 0.5f, cx, cy - s * 0.2f)
                path.quadTo(cx - s * 0.2f, cy - s * 0.5f, cx, cy - s * 0.85f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Olhos verdes esmeralda da floresta
                drawGlowingEyes(canvas, cx, cy - s * 0.08f, s * 0.20f, 0xFF34D399.toInt())
            }

            AvatarArchetype.ICE_KNIGHT -> {
                // Tiara de Cristais Glaciais
                charPaint.color = 0xFF0284C7.toInt()
                canvas.drawCircle(cx, cy - s * 0.1f, s * 0.38f, charPaint)

                // Espinhos de Gelo Polares
                charPaint.color = 0xFFBAE6FD.toInt()
                val crystalPts = listOf(-0.35f, -0.15f, 0f, 0.15f, 0.35f)
                for (offset in crystalPts) {
                    path.reset()
                    val h = if (offset == 0f) s * 0.95f else s * 0.7f
                    path.moveTo(cx + s * (offset - 0.08f), cy - s * 0.2f)
                    path.lineTo(cx + s * offset, cy - h)
                    path.lineTo(cx + s * (offset + 0.08f), cy - s * 0.2f)
                    path.close()
                    canvas.drawPath(path, charPaint)
                }

                // Olhos de gelo ardente
                drawGlowingEyes(canvas, cx, cy - s * 0.05f, s * 0.22f, 0xFFE0F2FE.toInt())
            }

            AvatarArchetype.SHADOW_LORD -> {
                // Chifres Demoníacos de Névoa
                charPaint.color = 0xFF581C87.toInt()
                path.reset()
                path.moveTo(cx - s * 0.25f, cy - s * 0.3f)
                path.quadTo(cx - s * 0.85f, cy - s * 0.85f, cx - s * 0.45f, cy - s * 1.0f)
                path.quadTo(cx - s * 0.4f, cy - s * 0.6f, cx - s * 0.15f, cy - s * 0.35f)
                path.close()
                canvas.drawPath(path, charPaint)

                path.reset()
                path.moveTo(cx + s * 0.25f, cy - s * 0.3f)
                path.quadTo(cx + s * 0.85f, cy - s * 0.85f, cx + s * 0.45f, cy - s * 1.0f)
                path.quadTo(cx + s * 0.4f, cy - s * 0.6f, cx + s * 0.15f, cy - s * 0.35f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Máscara Abissal de Obsidiana
                charPaint.color = 0xFF180B26.toInt()
                path.reset()
                path.moveTo(cx, cy + s * 0.3f)
                path.lineTo(cx + s * 0.38f, cy - s * 0.3f)
                path.lineTo(cx, cy - s * 0.5f)
                path.lineTo(cx - s * 0.38f, cy - s * 0.3f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Olhos Carmesim / Violeta do Abismo
                drawGlowingEyes(canvas, cx, cy - s * 0.18f, s * 0.22f, 0xFFF43F5E.toInt())
            }

            AvatarArchetype.COSMIC_GUARDIAN -> {
                // Vórtice Celestial / Halo Cósmico
                detailPaint.color = 0xFFF472B6.toInt()
                detailPaint.strokeWidth = s * 0.05f
                canvas.drawCircle(cx, cy - s * 0.2f, s * 0.55f, detailPaint)

                // Corpo etéreo do guardião
                charPaint.color = 0xFF4A044E.toInt()
                canvas.drawCircle(cx, cy - s * 0.15f, s * 0.35f, charPaint)

                // Olhos de Supernova Branca Radiante
                drawGlowingEyes(canvas, cx, cy - s * 0.15f, s * 0.22f, 0xFFFFFFFF.toInt())

                // Estrelas orbitais cintilantes
                glowPaint.color = 0xFFFDF4FF.toInt()
                canvas.drawCircle(cx + s * 0.45f, cy - s * 0.4f, s * 0.07f, glowPaint)
                canvas.drawCircle(cx - s * 0.45f, cy - s * 0.1f, s * 0.05f, glowPaint)
                canvas.drawCircle(cx + s * 0.15f, cy - s * 0.65f, s * 0.06f, glowPaint)
            }

            AvatarArchetype.GOLDEN_ALCHEMIST -> {
                // Capuz de Alquimista
                charPaint.color = 0xFF451A03.toInt()
                canvas.drawCircle(cx, cy - s * 0.15f, s * 0.42f, charPaint)

                // Monóculo Dourado com Lente Arcana
                detailPaint.color = 0xFFFBBF24.toInt()
                detailPaint.strokeWidth = s * 0.06f
                canvas.drawCircle(cx + s * 0.14f, cy - s * 0.14f, s * 0.15f, detailPaint)

                // Lente cintilante
                glowPaint.color = 0xFFFDE047.toInt()
                glowPaint.alpha = 230
                canvas.drawCircle(cx + s * 0.14f, cy - s * 0.14f, s * 0.10f, glowPaint)

                // Olho esquerdo nas sombras
                glowPaint.color = 0xFFD97706.toInt()
                canvas.drawCircle(cx - s * 0.14f, cy - s * 0.14f, s * 0.07f, glowPaint)
            }

            AvatarArchetype.MYSTIC_CREATURE -> {
                // Cabeça Draconiana Esculpida
                charPaint.color = 0xFF042F2C.toInt()
                path.reset()
                path.moveTo(cx, cy - s * 0.75f)
                path.lineTo(cx + s * 0.4f, cy - s * 0.1f)
                path.lineTo(cx + s * 0.2f, cy + s * 0.35f)
                path.lineTo(cx, cy + s * 0.45f)
                path.lineTo(cx - s * 0.2f, cy + s * 0.35f)
                path.lineTo(cx - s * 0.4f, cy - s * 0.1f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Chifres Draconianos para trás
                charPaint.color = 0xFF0D9488.toInt()
                path.reset()
                path.moveTo(cx - s * 0.25f, cy - s * 0.5f)
                path.quadTo(cx - s * 0.7f, cy - s * 0.95f, cx - s * 0.85f, cy - s * 0.8f)
                path.quadTo(cx - s * 0.4f, cy - s * 0.4f, cx - s * 0.15f, cy - s * 0.3f)
                path.close()
                canvas.drawPath(path, charPaint)

                path.reset()
                path.moveTo(cx + s * 0.25f, cy - s * 0.5f)
                path.quadTo(cx + s * 0.7f, cy - s * 0.95f, cx + s * 0.85f, cy - s * 0.8f)
                path.quadTo(cx + s * 0.4f, cy - s * 0.4f, cx + s * 0.15f, cy - s * 0.3f)
                path.close()
                canvas.drawPath(path, charPaint)

                // Olhos de Dragão em Fenda Dourada
                drawDragonEyes(canvas, cx, cy - s * 0.08f, s * 0.22f, 0xFFFCD34D.toInt())
            }
        }
    }

    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float, span: Float, color: Int) {
        glowPaint.color = color
        glowPaint.alpha = 240
        canvas.drawCircle(cx - span * 0.5f, cy, span * 0.2f, glowPaint)
        canvas.drawCircle(cx + span * 0.5f, cy, span * 0.2f, glowPaint)
    }

    private fun drawGlowingEyes(canvas: Canvas, cx: Float, cy: Float, span: Float, color: Int) {
        // Aura dos olhos
        glowPaint.color = color
        glowPaint.alpha = 110
        canvas.drawCircle(cx - span * 0.5f, cy, span * 0.35f, glowPaint)
        canvas.drawCircle(cx + span * 0.5f, cy, span * 0.35f, glowPaint)

        // Núcleo intenso
        glowPaint.color = 0xFFFFFFFF.toInt()
        glowPaint.alpha = 255
        canvas.drawCircle(cx - span * 0.5f, cy, span * 0.16f, glowPaint)
        canvas.drawCircle(cx + span * 0.5f, cy, span * 0.16f, glowPaint)
    }

    private fun drawDragonEyes(canvas: Canvas, cx: Float, cy: Float, span: Float, color: Int) {
        glowPaint.color = color
        glowPaint.alpha = 240
        // Esclerótica amarela
        rectF.set(cx - span * 0.6f, cy - span * 0.18f, cx - span * 0.25f, cy + span * 0.18f)
        canvas.drawOval(rectF, glowPaint)
        rectF.set(cx + span * 0.25f, cy - span * 0.18f, cx + span * 0.6f, cy + span * 0.18f)
        canvas.drawOval(rectF, glowPaint)

        // Pupila fenda vertical preta
        detailPaint.color = 0xFF000000.toInt()
        detailPaint.strokeWidth = span * 0.08f
        canvas.drawLine(cx - span * 0.42f, cy - span * 0.16f, cx - span * 0.42f, cy + span * 0.16f, detailPaint)
        canvas.drawLine(cx + span * 0.42f, cy - span * 0.16f, cx + span * 0.42f, cy + span * 0.16f, detailPaint)
    }
}
