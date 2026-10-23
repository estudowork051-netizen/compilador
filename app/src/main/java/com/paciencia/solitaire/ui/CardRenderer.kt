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
 * Renderizador vetorial de alta precisão de cartas, naipes, pilhas e molduras com
 * suporte integral à identidade Dark Fantasy das 8 skins colecionáveis (frente e verso).
 */
class CardRenderer {

    private val cardFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
    }

    private val cardBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardBackInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
    }

    private val cardBackPatternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
    }

    private val cornerOrnamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
    }

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC2626.toInt() // Carmesim vivo de alto contraste
        style = Paint.Style.FILL
    }

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt() // Obsidian profundo
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val ambientShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000.toInt()
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000.toInt()
        style = Paint.Style.FILL
    }

    private val highlightAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f
    }

    private val courtBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val courtBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val courtTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val slotBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val rectF = RectF()
    private val shadowRectF = RectF()
    private val suitPath = Path()
    private val ornamentPath = Path()

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
        val cornerRadius = width * 0.09f

        // 1. Sombra volumétrica realista com camada dupla (ambient + directional)
        if (elevation <= 0f) {
            shadowRectF.set(x + 1.5f, y + 2.5f, x + width + 1.5f, y + height + 2.5f)
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, ambientShadowPaint)
        } else {
            val ambientOffset = elevation * 0.3f
            shadowRectF.set(x, y + ambientOffset, x + width, y + height + ambientOffset)
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, ambientShadowPaint)

            val dirOffset = elevation * 0.65f
            shadowRectF.set(
                x + dirOffset * 0.2f,
                y + dirOffset,
                x + width + dirOffset * 0.2f,
                y + height + dirOffset
            )
            canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, shadowPaint)
        }

        rectF.set(x, y, x + width, y + height)

        // 2. Renderização do corpo da carta (Frente estilizada pela Skin ou Verso temático)
        if (card.isFaceUp) {
            drawFaceUpCard(canvas, card, x, y, width, height, cornerRadius, skin)
        } else {
            drawFaceDownCard(canvas, x, y, width, height, cornerRadius, theme, skin)
        }

        // 3. Realce luminoso multicamadas com a energia mística da skin ativa
        if (isHighlighted) {
            // Aura de energia externa suave
            highlightAuraPaint.color = skin.accentColor
            highlightAuraPaint.alpha = 110
            highlightAuraPaint.strokeWidth = 7f
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, highlightAuraPaint)

            // Contorno brilhante nítido
            highlightPaint.color = skin.accentColor
            highlightPaint.alpha = 255
            highlightPaint.strokeWidth = 3f
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, highlightPaint)
        }
    }

    /**
     * Renderiza a face frontal da carta personalizada pela skin equipada.
     */
    private fun drawFaceUpCard(
        canvas: Canvas,
        card: Card,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        skin: CardSkin
    ) {
        // Paleta nobre de pergaminho sutilmente harmonizada com o reino da skin
        val (faceColorTop, faceColorBottom) = when (skin.pattern) {
            SkinPattern.GOLDEN -> Pair(0xFFFFFDF5.toInt(), 0xFFFAF0D7.toInt())
            SkinPattern.ARCANE -> Pair(0xFFF8FAFC.toInt(), 0xFFEDE9FE.toInt())
            SkinPattern.NATURE -> Pair(0xFFF7FDF9.toInt(), 0xFFECFDF5.toInt())
            SkinPattern.ICE -> Pair(0xFFF0F9FF.toInt(), 0xFFE0F2FE.toInt())
            SkinPattern.SHADOW -> Pair(0xFFF5F5F7.toInt(), 0xFFE4E4E7.toInt())
            SkinPattern.COSMIC -> Pair(0xFFFAF5FF.toInt(), 0xFFF3E8FF.toInt())
            SkinPattern.ROYAL -> Pair(0xFFFFF1F2.toInt(), 0xFFFFE4E6.toInt())
            SkinPattern.FLAME -> Pair(0xFFFFF8F0.toInt(), 0xFFFEE6D6.toInt())
            SkinPattern.CLASSIC -> Pair(0xFFFFFDF9.toInt(), 0xFFF3EFE6.toInt())
        }

        cardFacePaint.shader = LinearGradient(
            x, y, x, y + height,
            faceColorTop, faceColorBottom,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardFacePaint)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBorderPaint)

        // Moldura interna de filigrana na cor de destaque da Skin equipada
        val innerPad = width * 0.055f
        shadowRectF.set(x + innerPad, y + innerPad, x + width - innerPad, y + height - innerPad)
        cardInnerBorderPaint.color = skin.accentColor
        cardInnerBorderPaint.alpha = 180
        canvas.drawRoundRect(shadowRectF, cornerRadius * 0.7f, cornerRadius * 0.7f, cardInnerBorderPaint)

        // Ornamentos nos 4 cantos da face da carta de acordo com o padrão da skin
        cornerOrnamentPaint.color = skin.accentColor
        cornerOrnamentPaint.alpha = 200
        drawCornerOrnaments(canvas, skin.pattern, x, y, width, height, innerPad, cornerOrnamentPaint)

        // Textos e Naipes com alto contraste visual
        val suitPaint = if (card.isRed) redPaint else blackPaint
        textPaint.color = suitPaint.color
        val textSize = width * 0.23f
        textPaint.textSize = textSize

        // Índice no canto superior esquerdo
        val leftMargin = width * 0.09f
        val topMargin = height * 0.17f
        canvas.drawText(card.rank.label, x + leftMargin, y + topMargin, textPaint)

        val suitSize = width * 0.14f
        drawSuit(
            canvas,
            card.suit,
            x + leftMargin + (textPaint.measureText(card.rank.label) * 0.5f),
            y + topMargin + suitSize * 1.15f,
            suitSize,
            suitPaint
        )

        // Índice no canto inferior direito rotacionado 180 graus
        canvas.save()
        canvas.rotate(180f, x + width * 0.5f, y + height * 0.5f)
        canvas.drawText(card.rank.label, x + leftMargin, y + topMargin, textPaint)
        drawSuit(
            canvas,
            card.suit,
            x + leftMargin + (textPaint.measureText(card.rank.label) * 0.5f),
            y + topMargin + suitSize * 1.15f,
            suitSize,
            suitPaint
        )
        canvas.restore()

        // Desenho central da carta (Figuras heráldicas da corte, Ás monumental ou naipes)
        drawCenterEmblem(canvas, card, x + width * 0.5f, y + height * 0.5f, width, height, suitPaint, skin)
    }

    /**
     * Desenha os 4 ornamentos nos cantos da carta frontal com símbolos da coleção.
     */
    private fun drawCornerOrnaments(
        canvas: Canvas,
        pattern: SkinPattern,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        pad: Float,
        paint: Paint
    ) {
        val s = width * 0.038f
        val left = x + pad + 2.5f
        val right = x + width - pad - 2.5f
        val top = y + pad + 2.5f
        val bottom = y + height - pad - 2.5f

        when (pattern) {
            SkinPattern.ARCANE -> {
                drawMiniDiamond(canvas, left, top, s, paint)
                drawMiniDiamond(canvas, right, top, s, paint)
                drawMiniDiamond(canvas, left, bottom, s, paint)
                drawMiniDiamond(canvas, right, bottom, s, paint)
            }
            SkinPattern.GOLDEN -> {
                drawMiniCross(canvas, left, top, s, paint)
                drawMiniCross(canvas, right, top, s, paint)
                drawMiniCross(canvas, left, bottom, s, paint)
                drawMiniCross(canvas, right, bottom, s, paint)
            }
            SkinPattern.NATURE -> {
                drawMiniLeaf(canvas, left, top, s, paint)
                drawMiniLeaf(canvas, right, top, s, paint)
                drawMiniLeaf(canvas, left, bottom, s, paint)
                drawMiniLeaf(canvas, right, bottom, s, paint)
            }
            SkinPattern.ICE -> {
                drawMiniIce(canvas, left, top, s, paint)
                drawMiniIce(canvas, right, top, s, paint)
                drawMiniIce(canvas, left, bottom, s, paint)
                drawMiniIce(canvas, right, bottom, s, paint)
            }
            SkinPattern.SHADOW -> {
                drawMiniGothic(canvas, left, top, s, paint)
                drawMiniGothic(canvas, right, top, s, paint)
                drawMiniGothic(canvas, left, bottom, s, paint)
                drawMiniGothic(canvas, right, bottom, s, paint)
            }
            SkinPattern.COSMIC -> {
                drawMiniStar(canvas, left, top, s, paint)
                drawMiniStar(canvas, right, top, s, paint)
                drawMiniStar(canvas, left, bottom, s, paint)
                drawMiniStar(canvas, right, bottom, s, paint)
            }
            SkinPattern.ROYAL -> {
                drawMiniCrown(canvas, left, top, s, paint)
                drawMiniCrown(canvas, right, top, s, paint)
                drawMiniCrown(canvas, left, bottom, s, paint)
                drawMiniCrown(canvas, right, bottom, s, paint)
            }
            SkinPattern.FLAME -> {
                drawMiniFlame(canvas, left, top, s, paint)
                drawMiniFlame(canvas, right, top, s, paint)
                drawMiniFlame(canvas, left, bottom, s, paint)
                drawMiniFlame(canvas, right, bottom, s, paint)
            }
            SkinPattern.CLASSIC -> {
                canvas.drawCircle(left, top, s * 0.7f, paint)
                canvas.drawCircle(right, top, s * 0.7f, paint)
                canvas.drawCircle(left, bottom, s * 0.7f, paint)
                canvas.drawCircle(right, bottom, s * 0.7f, paint)
            }
        }
    }

    private fun drawMiniDiamond(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx, cy - s)
        ornamentPath.lineTo(cx + s * 0.7f, cy)
        ornamentPath.lineTo(cx, cy + s)
        ornamentPath.lineTo(cx - s * 0.7f, cy)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
    }

    private fun drawMiniCross(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        canvas.drawLine(cx - s, cy, cx + s, cy, paint)
        canvas.drawLine(cx, cy - s, cx, cy + s, paint)
        canvas.drawCircle(cx, cy, s * 0.4f, paint)
    }

    private fun drawMiniLeaf(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx, cy - s)
        ornamentPath.quadTo(cx + s, cy, cx, cy + s)
        ornamentPath.quadTo(cx - s, cy, cx, cy - s)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
    }

    private fun drawMiniIce(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        canvas.drawLine(cx - s, cy, cx + s, cy, paint)
        canvas.drawLine(cx, cy - s, cx, cy + s, paint)
        val d = s * 0.7f
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, paint)
        canvas.drawLine(cx - d, cy + d, cx + d, cy - d, paint)
    }

    private fun drawMiniGothic(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx - s * 0.6f, cy - s)
        ornamentPath.lineTo(cx + s * 0.6f, cy - s)
        ornamentPath.lineTo(cx, cy + s)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
    }

    private fun drawMiniStar(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx, cy - s)
        ornamentPath.lineTo(cx + s * 0.25f, cy - s * 0.25f)
        ornamentPath.lineTo(cx + s, cy)
        ornamentPath.lineTo(cx + s * 0.25f, cy + s * 0.25f)
        ornamentPath.lineTo(cx, cy + s)
        ornamentPath.lineTo(cx - s * 0.25f, cy + s * 0.25f)
        ornamentPath.lineTo(cx - s, cy)
        ornamentPath.lineTo(cx - s * 0.25f, cy - s * 0.25f)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
    }

    private fun drawMiniCrown(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx - s, cy + s * 0.5f)
        ornamentPath.lineTo(cx + s, cy + s * 0.5f)
        ornamentPath.lineTo(cx + s * 0.8f, cy - s * 0.5f)
        ornamentPath.lineTo(cx + s * 0.35f, cy)
        ornamentPath.lineTo(cx, cy - s * 0.7f)
        ornamentPath.lineTo(cx - s * 0.35f, cy)
        ornamentPath.lineTo(cx - s * 0.8f, cy - s * 0.5f)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
    }

    private fun drawMiniFlame(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        ornamentPath.reset()
        ornamentPath.moveTo(cx, cy - s)
        ornamentPath.quadTo(cx + s * 0.7f, cy, cx, cy + s * 0.7f)
        ornamentPath.quadTo(cx - s * 0.7f, cy, cx, cy - s)
        ornamentPath.close()
        canvas.drawPath(ornamentPath, paint)
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
        suitPaint: Paint,
        skin: CardSkin
    ) {
        when (card.rank) {
            Rank.ACE -> {
                // Ás monumental com anel radiante na cor da skin
                val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = skin.accentColor
                    alpha = 75
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                canvas.drawCircle(cx, cy, width * 0.27f, auraPaint)

                val largeSize = width * 0.44f
                drawSuit(canvas, card.suit, cx, cy, largeSize, suitPaint)
            }
            Rank.JACK, Rank.QUEEN, Rank.KING -> {
                // Caixa nobre heráldica com moldura dourada e fundo perolado
                val boxWidth = width * 0.60f
                val boxHeight = height * 0.48f
                rectF.set(cx - boxWidth * 0.5f, cy - boxHeight * 0.5f, cx + boxWidth * 0.5f, cy + boxHeight * 0.5f)

                courtBoxBgPaint.color = 0x1A000000 or (skin.accentColor and 0x00FFFFFF)
                canvas.drawRoundRect(rectF, 7f, 7f, courtBoxBgPaint)

                courtBoxPaint.color = skin.accentColor
                courtBoxPaint.alpha = 220
                courtBoxPaint.strokeWidth = 2f
                canvas.drawRoundRect(rectF, 7f, 7f, courtBoxPaint)

                // Marca d'água sutil do naipe ao fundo
                val watermarkSize = width * 0.30f
                val oldAlpha = suitPaint.alpha
                suitPaint.alpha = 26
                drawSuit(canvas, card.suit, cx, cy - 2f, watermarkSize, suitPaint)
                suitPaint.alpha = oldAlpha

                // Letra da figura (J, Q, K)
                courtTextPaint.color = suitPaint.color
                courtTextPaint.textSize = width * 0.40f
                canvas.drawText(card.rank.label, cx, cy + width * 0.13f, courtTextPaint)

                // Pequeno naipe nobre sob a letra
                val smallSuitSize = width * 0.15f
                drawSuit(canvas, card.suit, cx, cy + boxHeight * 0.36f, smallSuitSize, suitPaint)
            }
            else -> {
                // Cartas numéricas (2 a 10): naipe proporcional central
                val midSize = width * 0.34f
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
        val colorStart = if (skin.id == CardSkin.CLASSIC.id) theme.cardBackGradient[0] else skin.primaryColor
        val colorEnd = if (skin.id == CardSkin.CLASSIC.id) theme.cardBackGradient[1] else skin.secondaryColor

        cardBackPaint.shader = LinearGradient(
            x, y, x + width, y + height,
            colorStart, colorEnd,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBackPaint)

        // Moldura externa da carta de baralho
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, cardBorderPaint)

        // Moldura interna ornamental com a cor de destaque da skin
        val pad = width * 0.08f
        rectF.set(x + pad, y + pad, x + width - pad, y + height - pad)
        cardBackInnerBorderPaint.color = skin.accentColor
        cardBackInnerBorderPaint.alpha = 210
        canvas.drawRoundRect(rectF, cornerRadius * 0.7f, cornerRadius * 0.7f, cardBackInnerBorderPaint)

        // Segunda moldura fina para profundidade
        val pad2 = width * 0.12f
        rectF.set(x + pad2, y + pad2, x + width - pad2, y + height - pad2)
        cardBackInnerBorderPaint.alpha = 90
        canvas.drawRoundRect(rectF, cornerRadius * 0.5f, cornerRadius * 0.5f, cardBackInnerBorderPaint)

        val cx = x + width * 0.5f
        val cy = y + height * 0.5f
        cardBackPatternPaint.color = skin.accentColor
        cardBackPatternPaint.alpha = 240

        drawSkinPattern(canvas, skin.pattern, cx, cy, width, height)
    }

    /**
     * Desenha geometricamente o padrão decorativo central de cada skin no verso.
     */
    private fun drawSkinPattern(
        canvas: Canvas,
        pattern: SkinPattern,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float
    ) {
        val size = width * 0.26f
        suitPath.reset()

        when (pattern) {
            SkinPattern.CLASSIC -> {
                // Losango clássico de baralhos finos com roseta interna
                suitPath.moveTo(cx, cy - size)
                suitPath.lineTo(cx + size, cy)
                suitPath.lineTo(cx, cy + size)
                suitPath.lineTo(cx - size, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)

                canvas.drawCircle(cx, cy, size * 0.4f, cardBackPatternPaint)
            }
            SkinPattern.GOLDEN -> {
                // Duplo losango com cruz radiante dourada e núcleo solar
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

                canvas.drawLine(cx - size * 0.8f, cy, cx + size * 0.8f, cy, cardBackPatternPaint)
                canvas.drawLine(cx, cy - size * 0.8f, cx, cy + size * 0.8f, cardBackPatternPaint)
            }
            SkinPattern.ARCANE -> {
                // Anel rúnico místico com estrela de 4 pontas
                suitPath.addCircle(cx, cy, size * 0.92f, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)

                suitPath.reset()
                suitPath.moveTo(cx, cy - size * 0.88f)
                suitPath.lineTo(cx + size * 0.22f, cy - size * 0.22f)
                suitPath.lineTo(cx + size * 0.88f, cy)
                suitPath.lineTo(cx + size * 0.22f, cy + size * 0.22f)
                suitPath.lineTo(cx, cy + size * 0.88f)
                suitPath.lineTo(cx - size * 0.22f, cy + size * 0.22f)
                suitPath.lineTo(cx - size * 0.88f, cy)
                suitPath.lineTo(cx - size * 0.22f, cy - size * 0.22f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.SHADOW -> {
                // Brasão sombrio com ponta de lâmina
                suitPath.moveTo(cx - size * 0.75f, cy - size * 0.75f)
                suitPath.lineTo(cx + size * 0.75f, cy - size * 0.75f)
                suitPath.lineTo(cx + size * 0.75f, cy)
                suitPath.lineTo(cx, cy + size * 1.0f)
                suitPath.lineTo(cx - size * 0.75f, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)

                canvas.drawLine(cx, cy - size * 0.75f, cx, cy + size * 0.75f, cardBackPatternPaint)
            }
            SkinPattern.NATURE -> {
                // Folhas élficas e vinhas entrelaçadas
                val leafW = size * 0.8f
                suitPath.moveTo(cx, cy - size * 0.95f)
                suitPath.quadTo(cx + leafW, cy, cx, cy + size * 0.95f)
                suitPath.quadTo(cx - leafW, cy, cx, cy - size * 0.95f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)

                suitPath.reset()
                suitPath.moveTo(cx - size * 0.95f, cy)
                suitPath.quadTo(cx, cy + leafW, cx + size * 0.95f, cy)
                suitPath.quadTo(cx, cy - leafW, cx - size * 0.95f, cy)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.ICE -> {
                // Cristal de gelo fractal sextavado
                val arm = size * 0.9f
                canvas.drawLine(cx, cy - arm, cx, cy + arm, cardBackPatternPaint)
                canvas.drawLine(cx - arm * 0.866f, cy - arm * 0.5f, cx + arm * 0.866f, cy + arm * 0.5f, cardBackPatternPaint)
                canvas.drawLine(cx - arm * 0.866f, cy + arm * 0.5f, cx + arm * 0.866f, cy - arm * 0.5f, cardBackPatternPaint)

                canvas.drawCircle(cx, cy, size * 0.35f, cardBackPatternPaint)
            }
            SkinPattern.COSMIC -> {
                // Órbita planetária com estrela central
                val orbitW = size * 1.15f
                val orbitH = size * 0.48f
                rectF.set(cx - orbitW, cy - orbitH, cx + orbitW, cy + orbitH)
                canvas.save()
                canvas.rotate(35f, cx, cy)
                suitPath.addOval(rectF, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
                canvas.restore()

                canvas.save()
                canvas.rotate(-35f, cx, cy)
                suitPath.reset()
                suitPath.addOval(rectF, Path.Direction.CW)
                canvas.drawPath(suitPath, cardBackPatternPaint)
                canvas.restore()

                canvas.drawCircle(cx, cy, size * 0.35f, cardBackPatternPaint)
            }
            SkinPattern.ROYAL -> {
                // Coroa imperial nobre de cavaleiro
                val crownW = size * 0.9f
                val crownH = size * 0.7f
                suitPath.moveTo(cx - crownW, cy + crownH * 0.5f)
                suitPath.lineTo(cx - crownW, cy - crownH * 0.5f)
                suitPath.lineTo(cx - crownW * 0.45f, cy)
                suitPath.lineTo(cx, cy - crownH * 0.85f)
                suitPath.lineTo(cx + crownW * 0.45f, cy)
                suitPath.lineTo(cx + crownW, cy - crownH * 0.5f)
                suitPath.lineTo(cx + crownW, cy + crownH * 0.5f)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
            }
            SkinPattern.FLAME -> {
                // Chama ancestral com ponta flamejante e brasas
                val flameH = size * 0.95f
                suitPath.moveTo(cx, cy - flameH)
                suitPath.quadTo(cx + size * 0.7f, cy, cx, cy + size * 0.85f)
                suitPath.quadTo(cx - size * 0.7f, cy, cx, cy - flameH)
                suitPath.close()
                canvas.drawPath(suitPath, cardBackPatternPaint)
                canvas.drawCircle(cx, cy + size * 0.2f, size * 0.25f, cardBackPatternPaint)
            }
        }
    }

    /**
     * Desenha um espaço vazio de pilha com chanfro esculpido em pedra e cantoneiras.
     */
    fun drawEmptySlot(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        theme: SceneryTheme,
        watermarkSuit: Suit? = null,
        accentColor: Int = theme.accentColor
    ) {
        val cornerRadius = width * 0.09f
        rectF.set(x, y, x + width, y + height)

        // Fundo em cavidade entalhada
        slotBgPaint.color = theme.slotBackgroundColor
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, slotBgPaint)

        // Chanfro interno para profundidade de encaixe
        slotBevelPaint.color = 0x44000000
        slotBevelPaint.strokeWidth = 2.5f
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, slotBevelPaint)

        // Borda suave com a cor de destaque do tema
        slotPaint.color = accentColor
        slotPaint.alpha = 110
        slotPaint.strokeWidth = 1.8f
        val innerPad = 1.5f
        rectF.set(x + innerPad, y + innerPad, x + width - innerPad, y + height - innerPad)
        canvas.drawRoundRect(rectF, cornerRadius * 0.85f, cornerRadius * 0.85f, slotPaint)

        // Cantoneiras ornamentais
        val bLen = width * 0.20f
        bracketPaint.color = accentColor
        bracketPaint.alpha = 140
        canvas.drawLine(x, y + bLen, x, y, bracketPaint)
        canvas.drawLine(x, y, x + bLen, y, bracketPaint)

        canvas.drawLine(x + width - bLen, y, x + width, y, bracketPaint)
        canvas.drawLine(x + width, y, x + width, y + bLen, bracketPaint)

        canvas.drawLine(x, y + height - bLen, x, y + height, bracketPaint)
        canvas.drawLine(x, y + height, x + bLen, y + height, bracketPaint)

        canvas.drawLine(x + width - bLen, y + height, x + width, y + height, bracketPaint)
        canvas.drawLine(x + width, y + height - bLen, x + width, y + height, bracketPaint)

        // Marca d'água dos naipes nas fundações
        if (watermarkSuit != null) {
            val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                alpha = 130
                style = Paint.Style.STROKE
                strokeWidth = 2.4f
            }
            drawSuit(canvas, watermarkSuit, x + width * 0.5f, y + height * 0.5f, width * 0.38f, watermarkPaint)
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
                suitPath.moveTo(cx, cy - r * 0.95f)
                suitPath.lineTo(cx + r * 0.75f, cy)
                suitPath.lineTo(cx, cy + r * 0.95f)
                suitPath.lineTo(cx - r * 0.75f, cy)
                suitPath.close()
            }
            Suit.SPADES -> {
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

                suitPath.moveTo(cx - r * 0.15f, cy + r * 0.3f)
                suitPath.lineTo(cx - r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.35f, cy + r * 0.95f)
                suitPath.lineTo(cx + r * 0.15f, cy + r * 0.3f)
                suitPath.close()
            }
            Suit.CLUBS -> {
                val circleR = r * 0.42f
                suitPath.addCircle(cx, cy - circleR * 0.85f, circleR, Path.Direction.CW)
                suitPath.addCircle(cx - circleR * 0.85f, cy + circleR * 0.2f, circleR, Path.Direction.CW)
                suitPath.addCircle(cx + circleR * 0.85f, cy + circleR * 0.2f, circleR, Path.Direction.CW)

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
