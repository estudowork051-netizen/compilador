package com.paciencia.solitaire.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.customization.SkinPattern
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import com.paciencia.solitaire.scenery.SceneryTheme

/**
 * Renderizador vetorial de alta precisão de cartas, naipes e pilhas com suporte a skins customizáveis.
 */
class CardRenderer {

    private val cardFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val cardBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardBackInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val cardBackPatternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1AFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE11D48.toInt() // Crimson de alto contraste
        style = Paint.Style.FILL
    }

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt() // Obsidian black profundo
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val ambientShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26000000.toInt()
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000.toInt()
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF59E0B.toInt() // Destaque âmbar/dourado
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val courtBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1A000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val courtTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rectF = RectF()
    private val shadowRectF = RectF()
    private val suitPath = Path()

    /**
     * Desenha uma carta completa (frente ou verso) na posição especificada.
     */
    fun drawCard(
        canvas: Canvas,
        card: Card,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        theme: SceneryTheme,
        elevation: Float = 0f,
        isHighlighted: Boolean = false,
        skin: CardSkin = CardSkin.CLASSIC
    ) {
        val cornerRadius = width * 0.08f

        // Sombra suave ambiente em repouso
        if (elevation <= 0f) {
            shadowRectF.set(x + 1f, y + 2f, x + width + 1f, y + height + 2f)
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, ambientShadowPaint)
        } else {
            // Sombra dinâmica elevada com dupla camada para profundidade 3D
            val ambientOffset = elevation * 0.25f
            shadowRectF.set(x, y + ambientOffset, x + width, y + height + ambientOffset)
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, ambientShadowPaint)

            val dirOffset = elevation * 0.5f
            shadowRectF.set(
                x + dirOffset * 0.2f,
                y + dirOffset,
                x + width + dirOffset * 0.2f,
                y + height + dirOffset
            )
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, shadowPaint)
        }

        rectF.set(x, y, x + width, y + height)

        if (card.isFaceUp) {
            drawFaceUpCard(canvas, card, x, y, width, height, cornerRadius)
        } else {
            drawFaceDownCard(canvas, x, y, width, height, cornerRadius, theme, skin)
        }

        // Realce de seleção ao arrastar ou selecionar a carta
        if (isHighlighted) {
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, highlightPaint)
        }
    }

    /**
     * Renderiza a face frontal da carta.
     */
    private fun drawFaceUpCard(
        canvas: Canvas,
        card: Card,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        cornerRadius: Float
    ) {
        // Fundo branco limpo
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardFacePaint)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBorderPaint)

        val suitPaint = if (card.isRed) redPaint else blackPaint
        textPaint.color = suitPaint.color
        val textSize = width * 0.22f
        textPaint.textSize = textSize

        // Desenha número e naipe no canto superior esquerdo
        val leftMargin = width * 0.09f
        val topMargin = height * 0.16f
        canvas.drawText(card.rank.label, x + leftMargin, y + topMargin, textPaint)

        val suitSize = width * 0.14f
        drawSuit(
            canvas,
            card.suit,
            x + leftMargin + (textPaint.measureText(card.rank.label) * 0.5f),
            y + topMargin + suitSize * 1.1f,
            suitSize,
            suitPaint
        )

        // Desenha número e naipe no canto inferior direito invertido (180°)
        canvas.save()
        canvas.rotate(180f, x + width * 0.5f, y + height * 0.5f)
        canvas.drawText(card.rank.label, x + leftMargin, y + topMargin, textPaint)
        drawSuit(
            canvas,
            card.suit,
            x + leftMargin + (textPaint.measureText(card.rank.label) * 0.5f),
            y + topMargin + suitSize * 1.1f,
            suitSize,
            suitPaint
        )
        canvas.restore()

        // Desenho central da carta
        drawCenterEmblem(canvas, card, x + width * 0.5f, y + height * 0.5f, width, height, suitPaint)
    }

    /**
     * Desenha o emblema ou arranjo central da carta.
     */
    private fun drawCenterEmblem(
        canvas: Canvas,
        card: Card,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        suitPaint: Paint
    ) {
        when (card.rank) {
            Rank.ACE -> {
                // Grande naipe decorativo central
                val largeSize = width * 0.42f
                drawSuit(canvas, card.suit, cx, cy, largeSize, suitPaint)
            }
            Rank.JACK, Rank.QUEEN, Rank.KING -> {
                // Borda nobre interna dourada e letra monumental da figura
                val boxWidth = width * 0.55f
                val boxHeight = height * 0.45f
                rectF.set(cx - boxWidth * 0.5f, cy - boxHeight * 0.5f, cx + boxWidth * 0.5f, cy + boxHeight * 0.5f)
                courtBoxPaint.color = 0x33D4AF37.toInt()
                courtBoxPaint.strokeWidth = 1.8f
                canvas.drawRoundRect(rectF, 6f, 6f, courtBoxPaint)

                courtTextPaint.color = suitPaint.color
                courtTextPaint.textSize = width * 0.38f
                canvas.drawText(card.rank.label, cx, cy + width * 0.14f, courtTextPaint)

                // Pequeno naipe ornamental abaixo da letra
                val smallSuitSize = width * 0.16f
                drawSuit(canvas, card.suit, cx, cy + boxHeight * 0.36f, smallSuitSize, suitPaint)
            }
            else -> {
                // Cartas numéricas (2 a 10): naipe proporcional central
                val midSize = width * 0.32f
                drawSuit(canvas, card.suit, cx, cy, midSize, suitPaint)
            }
        }
    }

    /**
     * Renderiza o verso elegante da carta, customizado pela skin selecionada e pelo tema.
     */
    private fun drawFaceDownCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        theme: SceneryTheme,
        skin: CardSkin
    ) {
        // Cores do verso da skin ativa ou do cenário se for a skin clássica
        val colorStart = if (skin.id == CardSkin.CLASSIC.id) theme.cardBackGradient[0] else skin.primaryColor
        val colorEnd = if (skin.id == CardSkin.CLASSIC.id) theme.cardBackGradient[1] else skin.secondaryColor

        cardBackPaint.shader = LinearGradient(
            x, y, x + width, y + height,
            colorStart, colorEnd,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBackPaint)

        // Moldura branca externa da carta de baralho
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBorderPaint)

        // Moldura interna com a cor de destaque da skin
        val pad = width * 0.08f
        rectF.set(x + pad, y + pad, x + width - pad, y + height - pad)
        cardBackInnerBorderPaint.color = skin.accentColor
        cardBackInnerBorderPaint.alpha = 150
        canvas.drawRoundRect(rectF, cornerRadius * 0.7f, cornerRadius * 0.7f, cardBackInnerBorderPaint)

        val cx = x + width * 0.5f
        val cy = y + height * 0.5f
        cardBackPatternPaint.color = skin.accentColor
        cardBackPatternPaint.alpha = 190

        drawSkinPattern(canvas, skin.pattern, cx, cy, width, height)
    }

    /**
     * Desenha geometricamente o padrão decorativo central de cada skin.
     */
    private fun drawSkinPattern(
        canvas: Canvas,
        pattern: SkinPattern,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float
    ) {
        val size = width * 0.24f
        suitPath.reset()

        when (pattern) {
            SkinPattern.CLASSIC -> {
                // Losango clássico de baralhos finos
                suitPath.moveTo(cx, cy - size)
                suitPath.lineTo(cx + size, cy)
                suitPath.lineTo(cx, cy + size)
                suitPath.lineTo(cx - size, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.GOLDEN -> {
                // Duplo losango com cruz radiante dourada
                suitPath.moveTo(cx, cy - size)
                suitPath.lineTo(cx + size, cy)
                suitPath.lineTo(cx, cy + size)
                suitPath.lineTo(cx - size, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)

                // Núcleo solar
                val innerR = size * 0.45f
                suitPath.reset()
                suitPath.addCircle(cx, cy, innerR, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.ARCANE -> {
                // Anel místico com estrela de 4 pontas
                suitPath.addCircle(cx, cy, size * 0.9f, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)

                suitPath.reset()
                suitPath.moveTo(cx, cy - size * 0.85f)
                suitPath.lineTo(cx + size * 0.2f, cy - size * 0.2f)
                suitPath.lineTo(cx + size * 0.85f, cy)
                suitPath.lineTo(cx + size * 0.2f, cy + size * 0.2f)
                suitPath.lineTo(cx, cy + size * 0.85f)
                suitPath.lineTo(cx - size * 0.2f, cy + size * 0.2f)
                suitPath.lineTo(cx - size * 0.85f, cy)
                suitPath.lineTo(cx - size * 0.2f, cy - size * 0.2f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.SHADOW -> {
                // Brasão sombrio com ponta de lâmina
                suitPath.moveTo(cx - size * 0.7f, cy - size * 0.7f)
                suitPath.lineTo(cx + size * 0.7f, cy - size * 0.7f)
                suitPath.lineTo(cx + size * 0.7f, cy)
                suitPath.lineTo(cx, cy + size * 0.95f)
                suitPath.lineTo(cx - size * 0.7f, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.NATURE -> {
                // Pétalas ou folhas esmeralda entrelaçadas
                val leafW = size * 0.75f
                suitPath.moveTo(cx, cy - size * 0.9f)
                suitPath.quadTo(cx + leafW, cy, cx, cy + size * 0.9f)
                suitPath.quadTo(cx - leafW, cy, cx, cy - size * 0.9f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.ICE -> {
                // Cristal de gelo fractal
                val arm = size * 0.85f
                // Eixo vertical
                suitPath.moveTo(cx, cy - arm)
                suitPath.lineTo(cx, cy + arm)
                // Eixo diagonal 1
                suitPath.moveTo(cx - arm * 0.866f, cy - arm * 0.5f)
                suitPath.lineTo(cx + arm * 0.866f, cy + arm * 0.5f)
                // Eixo diagonal 2
                suitPath.moveTo(cx - arm * 0.866f, cy + arm * 0.5f)
                suitPath.lineTo(cx + arm * 0.866f, cy - arm * 0.5f)
                canvas.drawPath(suitPath, cardBackPatternPaint)

                // Núcleo cristalino
                suitPath.reset()
                suitPath.addCircle(cx, cy, size * 0.3f, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.COSMIC -> {
                // Galáxia cósmica com órbita estelar
                val orbitW = size * 1.1f
                val orbitH = size * 0.45f
                rectF.set(cx - orbitW, cy - orbitH, cx + orbitW, cy + orbitH)
                canvas.save()
                canvas.rotate(35f, cx, cy)
                suitPath.addOval(rectF, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
                canvas.restore()

                // Estrela central cintilante
                suitPath.reset()
                suitPath.addCircle(cx, cy, size * 0.35f, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.ROYAL -> {
                // Coroa imperial de 3 pontas
                val crownW = size * 0.85f
                val crownH = size * 0.65f
                suitPath.moveTo(cx - crownW, cy + crownH * 0.5f)
                suitPath.lineTo(cx - crownW, cy - crownH * 0.5f)
                suitPath.lineTo(cx - crownW * 0.45f, cy)
                suitPath.lineTo(cx, cy - crownH * 0.8f)
                suitPath.lineTo(cx + crownW * 0.45f, cy)
                suitPath.lineTo(cx + crownW, cy - crownH * 0.5f)
                suitPath.lineTo(cx + crownW, cy + crownH * 0.5f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
        }
    }

    /**
     * Desenha um espaço vazio de pilha (Tableau, Stock, Foundation).
     */
    fun drawEmptySlot(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        theme: SceneryTheme,
        watermarkSuit: Suit? = null
    ) {
        val cornerRadius = width * 0.08f
        rectF.set(x, y, x + width, y + height)

        slotBgPaint.color = theme.slotBackgroundColor
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, slotBgPaint)

        slotPaint.color = theme.slotBorderColor
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, slotPaint)

        if (watermarkSuit != null) {
            val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.slotBorderColor
                alpha = 80
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            drawSuit(canvas, watermarkSuit, x + width * 0.5f, y + height * 0.5f, width * 0.35f, watermarkPaint)
        }
    }

    /**
     * Desenha geometricamente um dos quatro naipes tradicionais com Paths vetoriais puros.
     */
    fun drawSuit(
        canvas: Canvas,
        suit: Suit,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint
    ) {
        val r = size * 0.5f
        suitPath.reset()

        when (suit) {
            Suit.HEARTS -> {
                // Coração: dois arcos superiores e ponta inferior
                suitPath.moveTo(cx, cy + r * 0.8f)
                suitPath.cubicTo(
                    cx - r * 1.1f, cy + r * 0.1f,
                    cx - r * 1.1f, cy - r * 0.7f,
                    cx - r * 0.45f, cy - r * 0.7f
                )
                suitPath.cubicTo(
                    cx - r * 0.15f, cy - r * 0.7f,
                    cx, cy - r * 0.4f,
                    cx, cy - r * 0.3f
                )
                suitPath.cubicTo(
                    cx, cy - r * 0.4f,
                    cx + r * 0.15f, cy - r * 0.7f,
                    cx + r * 0.45f, cy - r * 0.7f
                )
                suitPath.cubicTo(
                    cx + r * 1.1f, cy - r * 0.7f,
                    cx + r * 1.1f, cy + r * 0.1f,
                    cx, cy + r * 0.8f
                )
                suitPath.close()
            }
            Suit.DIAMONDS -> {
                // Ouros: losango geométrico elegante
                suitPath.moveTo(cx, cy - r * 0.95f)
                suitPath.lineTo(cx + r * 0.75f, cy)
                suitPath.lineTo(cx, cy + r * 0.95f)
                suitPath.lineTo(cx - r * 0.75f, cy)
                suitPath.close()
            }
            Suit.SPADES -> {
                // Espadas: coração invertido com haste na base
                suitPath.moveTo(cx, cy - r * 0.9f)
                suitPath.cubicTo(
                    cx + r * 1.1f, cy - r * 0.2f,
                    cx + r * 1.1f, cy + r * 0.5f,
                    cx + r * 0.45f, cy + r * 0.5f
                )
                suitPath.cubicTo(
                    cx + r * 0.15f, cy + r * 0.5f,
                    cx + r * 0.1f, cy + r * 0.3f,
                    cx, cy + r * 0.3f
                )
                suitPath.cubicTo(
                    cx - r * 0.1f, cy + r * 0.3f,
                    cx - r * 0.15f, cy + r * 0.5f,
                    cx - r * 0.45f, cy + r * 0.5f
                )
                suitPath.cubicTo(
                    cx - r * 1.1f, cy + r * 0.5f,
                    cx - r * 1.1f, cy - r * 0.2f,
                    cx, cy - r * 0.9f
                )
                suitPath.close()
                // Haste inferior
                suitPath.moveTo(cx - r * 0.15f, cy + r * 0.3f)
                suitPath.lineTo(cx - r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.15f, cy + r * 0.3f)
                suitPath.close()
            }
            Suit.CLUBS -> {
                // Paus: três círculos e haste central
                val circleR = r * 0.42f
                // Círculo superior
                suitPath.addCircle(cx, cy - circleR * 0.85f, circleR, Path.Direction.CW)
                // Círculo esquerdo
                suitPath.addCircle(cx - circleR * 0.85f, cy + circleR * 0.2f, circleR, Path.Direction.CW)
                // Círculo direito
                suitPath.addCircle(cx + circleR * 0.85f, cy + circleR * 0.2f, circleR, Path.Direction.CW)
                // Haste triangular inferior
                suitPath.moveTo(cx - r * 0.15f, cy + circleR * 0.2f)
                suitPath.lineTo(cx - r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.15f, cy + r * 0.2f)
                suitPath.close()
            }
        }

        canvas.drawPath(suitPath, paint)
    }
}
