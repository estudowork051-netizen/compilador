package com.paciencia.solitaire.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.customization.SkinPattern
import com.paciencia.solitaire.customization.VictoryCelebrationType
import com.paciencia.solitaire.engine.KlondikeEngine
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import com.paciencia.solitaire.scenery.Particle
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryManager
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import kotlin.math.hypot

/**
 * Custom View interativa de alto desempenho para o jogo Paciência.
 * Renderiza o cenário procedural, sistema de partículas, cartas vetoriais com skins,
 * efeitos de coluna completa e gerencia gestos de arrastar e soltar e toque inteligente.
 */
class SolitaireView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var engine: KlondikeEngine? = null
    var theme: SceneryTheme = SceneryManager.getTheme(SceneryType.ENCHANTED_FOREST)
        set(value) {
            field = value
            initParticles()
            invalidate()
        }

    var cardSkin: CardSkin = CardSkin.CLASSIC
        set(value) {
            field = value
            invalidate()
        }

    var avatar: Avatar = Avatar.ADVENTURER
        set(value) {
            field = value
            invalidate()
        }

    var soundFeedback: SoundFeedback? = null
    var onMoveMade: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    var isParticlesEnabled: Boolean = true
    var isPowerSaveMode: Boolean = false

    private val cardRenderer = CardRenderer()
    private val particleSystem = ParticleSystem()

    // Efeitos visuais de coluna concluída
    private data class ColumnEffect(
        val col: Int,
        val startTime: Long = System.currentTimeMillis(),
        val durationMs: Long = 1000L
    )
    private val activeColumnEffects = mutableListOf<ColumnEffect>()
    private val columnCompletedState = BooleanArray(7)
    private val columnGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = 0xFFFBBF24.toInt()
    }

    // Realce de destinos válidos ao arrastar cartas
    private val validTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = 0xFF34D399.toInt()
    }

    // Resposta visual para movimentos inválidos (Shake e flash vermelho)
    private var invalidMoveStartTime: Long = 0L
    private var invalidMovePileType: PileType? = null
    private val invalidMoveFlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = 0xFFEF4444.toInt()
    }

    // Realce de Dica (Hint Mágico com aura dourada)
    private var activeHint: com.paciencia.solitaire.engine.HintMove? = null
    private var hintStartTime: Long = 0L
    private val hintGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0xFFF59E0B.toInt()
    }

    // Celebração de Vitória Temática Personalizada por Skin
    private var isVictoryCelebrationActive = false
    private var victoryCelebrationStartTime = 0L
    private var victoryCelebrationSkin: CardSkin? = null
    private val victoryWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Dimensões calculadas proporcionalmente
    private var cardWidth: Float = 0f
    private var cardHeight: Float = 0f
    private var horizontalGap: Float = 0f
    private var topMargin: Float = 0f
    private var tableauTopY: Float = 0f
    private var cardOverlapFaceDown: Float = 0f
    private var cardOverlapFaceUp: Float = 0f

    // Áreas retangulares das pilhas para detecção de toques
    private val stockRect = RectF()
    private val wasteRect = RectF()
    private val foundationRects = Array(4) { RectF() }
    private val tableauRects = Array(7) { RectF() }

    // Estado do gesto de arrastar (Drag and Drop)
    private var isDragging = false
    private var draggedCards = mutableListOf<Card>()
    private var dragSourcePileType: PileType? = null
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var dragTouchOffsetX = 0f
    private var dragTouchOffsetY = 0f

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val screenW = w.toFloat()
        val screenH = h.toFloat()

        // Espaçamento horizontal entre as 7 colunas (8 intervalos)
        horizontalGap = screenW * 0.02f
        cardWidth = (screenW - (8f * horizontalGap)) / 7f
        cardHeight = cardWidth * 1.42f // Proporção padrão de baralho clássico

        // Margens superior e inferior considerando as barras de HUD
        topMargin = screenH * 0.08f
        tableauTopY = topMargin + cardHeight + (screenH * 0.025f)

        cardOverlapFaceDown = cardHeight * 0.18f
        cardOverlapFaceUp = cardHeight * 0.28f

        // Mapeia posições das pilhas superiores
        val topY = topMargin
        stockRect.set(horizontalGap, topY, horizontalGap + cardWidth, topY + cardHeight)
        val wasteX = horizontalGap * 2f + cardWidth
        wasteRect.set(wasteX, topY, wasteX + cardWidth, topY + cardHeight)

        // As 4 fundações ocupam as colunas 3, 4, 5 e 6
        for (i in 0 until 4) {
            val fCol = 3 + i
            val fX = horizontalGap + fCol * (cardWidth + horizontalGap)
            foundationRects[i].set(fX, topY, fX + cardWidth, topY + cardHeight)
        }

        // As 7 colunas de Tableau
        for (i in 0 until 7) {
            val tX = horizontalGap + i * (cardWidth + horizontalGap)
            tableauRects[i].set(tX, tableauTopY, tX + cardWidth, screenH - (screenH * 0.1f))
        }

        initParticles()
    }

    private fun initParticles() {
        if (width <= 0 || height <= 0) return
        val maxParticles = if (isPowerSaveMode) 15 else 40
        theme.initParticles(particleSystem, width.toFloat(), height.toFloat(), maxParticles)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Renderiza o cenário procedural do tema ativo
        theme.drawBackground(canvas, w, h)

        // 2. Atualiza e renderiza partículas se ativadas
        if (isParticlesEnabled) {
            particleSystem.update(w, h)
            particleSystem.draw(canvas)
        }

        // 3. Renderiza Monte de Compras (Stock)
        if (eng.stockPile.isEmpty) {
            cardRenderer.drawEmptySlot(canvas, stockRect.left, stockRect.top, cardWidth, cardHeight, theme)
        } else {
            val topCard = eng.stockPile.top()
            if (topCard != null) {
                cardRenderer.drawCard(canvas, topCard, stockRect.left, stockRect.top, cardWidth, cardHeight, theme, skin = cardSkin)
            }
        }

        // 4. Renderiza Descarte (Waste)
        if (eng.wastePile.isEmpty) {
            cardRenderer.drawEmptySlot(canvas, wasteRect.left, wasteRect.top, cardWidth, cardHeight, theme)
        } else {
            // Se a carta do topo do waste estiver sendo arrastada, desenha a carta logo abaixo ou slot vazio se for a única
            val cards = eng.wastePile.cards
            val isTopDragged = isDragging && dragSourcePileType == PileType.Waste
            if (isTopDragged && cards.size == 1) {
                cardRenderer.drawEmptySlot(canvas, wasteRect.left, wasteRect.top, cardWidth, cardHeight, theme)
            } else {
                val drawIndex = if (isTopDragged && cards.size > 1) cards.size - 2 else cards.size - 1
                if (drawIndex in cards.indices) {
                    val card = cards[drawIndex]
                    cardRenderer.drawCard(canvas, card, wasteRect.left, wasteRect.top, cardWidth, cardHeight, theme, skin = cardSkin)
                }
            }
        }

        // 5. Renderiza as 4 Fundações
        val foundationSuits = arrayOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
        for (i in 0 until 4) {
            val fPile = eng.foundationPiles[i]
            val rect = foundationRects[i]

            if (fPile.isEmpty) {
                cardRenderer.drawEmptySlot(canvas, rect.left, rect.top, cardWidth, cardHeight, theme, foundationSuits[i])
            } else {
                val isTopDragged = isDragging && dragSourcePileType == fPile.type
                val cards = fPile.cards
                if (isTopDragged && cards.size == 1) {
                    cardRenderer.drawEmptySlot(canvas, rect.left, rect.top, cardWidth, cardHeight, theme, foundationSuits[i])
                } else {
                    val drawIndex = if (isTopDragged && cards.size > 1) cards.size - 2 else cards.size - 1
                    if (drawIndex in cards.indices) {
                        val card = cards[drawIndex]
                        cardRenderer.drawCard(canvas, card, rect.left, rect.top, cardWidth, cardHeight, theme, skin = cardSkin)
                    }
                }
            }
        }

        // 6. Renderiza as 7 Colunas de Tableau
        for (col in 0 until 7) {
            val tPile = eng.tableauPiles[col]
            val rect = tableauRects[col]
            var currentY = rect.top

            if (tPile.isEmpty) {
                cardRenderer.drawEmptySlot(canvas, rect.left, rect.top, cardWidth, cardHeight, theme)
            } else {
                val cards = tPile.cards
                val dragIndex = if (isDragging && dragSourcePileType == tPile.type) {
                    (cards.size - draggedCards.size).coerceIn(0, cards.size)
                } else {
                    cards.size
                }

                for (idx in 0 until dragIndex) {
                    val card = cards[idx]
                    cardRenderer.drawCard(canvas, card, rect.left, currentY, cardWidth, cardHeight, theme, skin = cardSkin)
                    currentY += if (card.isFaceUp) cardOverlapFaceUp else cardOverlapFaceDown
                }
            }
        }

        // 7. Renderiza brilho e aura dourada das colunas recém-concluídas
        val now = System.currentTimeMillis()
        val effectIterator = activeColumnEffects.iterator()
        while (effectIterator.hasNext()) {
            val effect = effectIterator.next()
            val elapsed = now - effect.startTime
            if (elapsed > effect.durationMs) {
                effectIterator.remove()
            } else {
                val progress = elapsed.toFloat() / effect.durationMs
                val alpha = (Math.sin(progress * Math.PI) * 220).toInt().coerceIn(0, 255)
                columnGlowPaint.alpha = alpha
                columnGlowPaint.strokeWidth = 4f + (1f - progress) * 6f
                val colRect = tableauRects[effect.col]
                canvas.drawRoundRect(colRect.left - 4f, colRect.top - 4f, colRect.right + 4f, colRect.bottom + 4f, 16f, 16f, columnGlowPaint)
            }
        }

        // 8. Realce de destinos válidos quando uma carta estiver sendo arrastada
        if (isDragging && draggedCards.isNotEmpty()) {
            val pulse = (Math.sin(now * 0.008) * 0.35 + 0.65).toFloat()
            validTargetPaint.alpha = (pulse * 220).toInt()
            val firstDragged = draggedCards.first()

            // Destinos em Fundações
            if (draggedCards.size == 1) {
                for (i in 0 until 4) {
                    val fPile = eng.foundationPiles[i]
                    if (dragSourcePileType != fPile.type && com.paciencia.solitaire.engine.MoveValidator.canMoveToFoundation(firstDragged, fPile)) {
                        val r = foundationRects[i]
                        canvas.drawRoundRect(r.left - 2f, r.top - 2f, r.right + 2f, r.bottom + 2f, cardWidth * 0.08f, cardWidth * 0.08f, validTargetPaint)
                    }
                }
            }

            // Destinos em Colunas do Tableau
            for (col in 0 until 7) {
                val tPile = eng.tableauPiles[col]
                if (dragSourcePileType != tPile.type && com.paciencia.solitaire.engine.MoveValidator.canMoveToTableau(draggedCards, tPile)) {
                    val r = tableauRects[col]
                    val topY = if (tPile.isEmpty) r.top else r.top + calculateCardYOffset(tPile.cards, tPile.cards.size - 1)
                    canvas.drawRoundRect(r.left - 2f, topY - 2f, r.right + 2f, topY + cardHeight + 2f, cardWidth * 0.08f, cardWidth * 0.08f, validTargetPaint)
                }
            }
        }

        // 9. Resposta visual para movimentos inválidos (Flash avermelhado na pilha de origem)
        val invalidElapsed = now - invalidMoveStartTime
        if (invalidElapsed < 260 && invalidMovePileType != null) {
            val progress = invalidElapsed.toFloat() / 260f
            invalidMoveFlashPaint.alpha = ((1f - progress) * 230).toInt()
            when (val pType = invalidMovePileType) {
                is PileType.Tableau -> {
                    val r = tableauRects[pType.col]
                    val shake = (Math.sin(progress * Math.PI * 4.0) * 6.0 * (1.0 - progress)).toFloat()
                    canvas.drawRoundRect(r.left + shake - 2f, r.top - 2f, r.right + shake + 2f, r.bottom + 2f, cardWidth * 0.08f, cardWidth * 0.08f, invalidMoveFlashPaint)
                }
                is PileType.Foundation -> {
                    val r = foundationRects[pType.index]
                    val shake = (Math.sin(progress * Math.PI * 4.0) * 6.0 * (1.0 - progress)).toFloat()
                    canvas.drawRoundRect(r.left + shake - 2f, r.top - 2f, r.right + shake + 2f, r.bottom + 2f, cardWidth * 0.08f, cardWidth * 0.08f, invalidMoveFlashPaint)
                }
                PileType.Waste -> {
                    val shake = (Math.sin(progress * Math.PI * 4.0) * 6.0 * (1.0 - progress)).toFloat()
                    canvas.drawRoundRect(wasteRect.left + shake - 2f, wasteRect.top - 2f, wasteRect.right + shake + 2f, wasteRect.bottom + 2f, cardWidth * 0.08f, cardWidth * 0.08f, invalidMoveFlashPaint)
                }
                else -> {}
            }
        }

        // 10. Renderiza as cartas flutuantes que estão sendo arrastadas pelo dedo com destaque e sombra elevada
        if (isDragging && draggedCards.isNotEmpty()) {
            var currentY = dragCurrentY - dragTouchOffsetY
            val currentX = dragCurrentX - dragTouchOffsetX

            for (card in draggedCards) {
                cardRenderer.drawCard(
                    canvas,
                    card,
                    currentX,
                    currentY,
                    cardWidth,
                    cardHeight,
                    theme,
                    elevation = 24f,
                    isHighlighted = true,
                    skin = cardSkin
                )
                currentY += cardOverlapFaceUp
            }
        }

        // 11. Realce de Dica Ativa (Aura mágica dourada pulsante)
        val hintElapsed = now - hintStartTime
        val currentHint = activeHint
        if (currentHint != null && hintElapsed < 3000L) {
            val pulse = (Math.sin(hintElapsed * 0.008) * 0.35 + 0.65).toFloat()
            hintGlowPaint.alpha = (pulse * 240).toInt()
            hintGlowPaint.strokeWidth = 3.5f + pulse * 2.5f

            val srcRect = getRectForPile(currentHint.from)
            if (srcRect != null) {
                canvas.drawRoundRect(srcRect.left - 3f, srcRect.top - 3f, srcRect.right + 3f, srcRect.bottom + 3f, cardWidth * 0.1f, cardWidth * 0.1f, hintGlowPaint)
            }
            val destRect = getRectForPile(currentHint.to)
            if (destRect != null) {
                canvas.drawRoundRect(destRect.left - 3f, destRect.top - 3f, destRect.right + 3f, destRect.bottom + 3f, cardWidth * 0.1f, cardWidth * 0.1f, hintGlowPaint)
            }
        }

        // 12. Celebração de Vitória Temática Exclusiva da Skin Equipada
        if (isVictoryCelebrationActive) {
            val celebrationElapsed = now - victoryCelebrationStartTime
            val skin = victoryCelebrationSkin ?: cardSkin
            if (celebrationElapsed < 3600L) {
                drawVictoryCelebration(canvas, skin, celebrationElapsed, w, h)
            } else {
                isVictoryCelebrationActive = false
            }
        } else if (eng.isWon && isParticlesEnabled && !isPowerSaveMode && Math.random() < 0.25) {
            particleSystem.addParticle(
                Particle(
                    x = (Math.random() * width).toFloat(),
                    y = height * 0.15f,
                    vx = (Math.random().toFloat() - 0.5f) * 4f,
                    vy = Math.random().toFloat() * 3f + 1f,
                    size = (Math.random() * 5 + 4).toFloat(),
                    alpha = 255,
                    color = cardSkin.accentColor,
                    shape = Particle.Shape.STAR
                )
            )
        }

        // Mantém a animação suave a 60 FPS se houver partículas ativas, efeitos em andamento, dica, arraste ou celebração
        if ((isParticlesEnabled && !isPowerSaveMode) || activeColumnEffects.isNotEmpty() || invalidElapsed < 260 || (currentHint != null && hintElapsed < 3000L) || isDragging || eng.isWon || isVictoryCelebrationActive) {
            postInvalidateOnAnimation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eng = engine ?: return super.onTouchEvent(event)
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = x
                touchDownY = y
                dragCurrentX = x
                dragCurrentY = y
                isDragging = false
                draggedCards.clear()
                dragSourcePileType = null

                // 1. Toque no Monte de Compras (Stock)
                if (stockRect.contains(x, y)) {
                    val didDraw = eng.drawOrRecycleStock()
                    if (didDraw) {
                        soundFeedback?.cardTouch(this)
                        onMoveMade?.invoke()
                        invalidate()
                    }
                    return true
                }

                // 2. Toque no Descarte (Waste)
                if (wasteRect.contains(x, y) && eng.wastePile.isNotEmpty) {
                    val topCard = eng.wastePile.top()
                    if (topCard != null) {
                        isDragging = true
                        dragSourcePileType = PileType.Waste
                        draggedCards.add(topCard)
                        dragTouchOffsetX = x - wasteRect.left
                        dragTouchOffsetY = y - wasteRect.top
                        soundFeedback?.cardTouch(this)
                        invalidate()
                        return true
                    }
                }

                // 3. Toque nas Fundações
                for (i in 0 until 4) {
                    if (foundationRects[i].contains(x, y)) {
                        val fPile = eng.foundationPiles[i]
                        val top = fPile.top()
                        if (top != null) {
                            isDragging = true
                            dragSourcePileType = fPile.type
                            draggedCards.add(top)
                            dragTouchOffsetX = x - foundationRects[i].left
                            dragTouchOffsetY = y - foundationRects[i].top
                            soundFeedback?.cardTouch(this)
                            invalidate()
                            return true
                        }
                    }
                }

                // 4. Toque nas Colunas de Tableau
                for (col in 0 until 7) {
                    val tPile = eng.tableauPiles[col]
                    val tRect = tableauRects[col]
                    if (x >= tRect.left && x <= tRect.right && y >= tRect.top) {
                        val cards = tPile.cards
                        if (cards.isEmpty()) continue

                        // Calcula posições das cartas de baixo para cima para encontrar qual foi tocada
                        var cardY = tRect.top
                        var touchedIndex = -1

                        for (idx in cards.indices) {
                            val card = cards[idx]
                            val isLast = idx == cards.size - 1
                            val cardBottom = if (isLast) cardY + cardHeight else cardY + (if (card.isFaceUp) cardOverlapFaceUp else cardOverlapFaceDown)

                            if (y >= cardY && y <= cardBottom) {
                                touchedIndex = idx
                            }
                            cardY += if (card.isFaceUp) cardOverlapFaceUp else cardOverlapFaceDown
                        }

                        // Se tocou abaixo da última carta mas ainda na área da última
                        if (touchedIndex == -1 && y <= cardY + cardHeight) {
                            touchedIndex = cards.size - 1
                        }

                        if (touchedIndex != -1) {
                            val touchedCard = cards[touchedIndex]
                            // Se a carta do topo estiver virada para baixo, permite revelá-la ao tocar com brilho
                            if (!touchedCard.isFaceUp && touchedIndex == cards.size - 1) {
                                touchedCard.isFaceUp = true
                                soundFeedback?.cardPlaced(this)
                                if (isParticlesEnabled && !isPowerSaveMode) {
                                    val cx = (tRect.left + tRect.right) * 0.5f
                                    val cy = cardY + cardHeight * 0.5f
                                    for (p in 0 until 8) {
                                        particleSystem.addParticle(
                                            Particle(
                                                x = cx,
                                                y = cy,
                                                vx = (Math.random().toFloat() - 0.5f) * 2.8f,
                                                vy = -(Math.random().toFloat() * 2.5f + 0.8f),
                                                size = 4.5f,
                                                alpha = 220,
                                                color = 0xFF6EE7B7.toInt(),
                                                shape = Particle.Shape.SPARK
                                            )
                                        )
                                    }
                                }
                                onMoveMade?.invoke()
                                invalidate()
                                return true
                            }

                            // Apenas cartas reveladas (viradas para cima) podem ser pegas
                            if (touchedCard.isFaceUp) {
                                isDragging = true
                                dragSourcePileType = tPile.type
                                draggedCards.addAll(tPile.cardsFrom(touchedIndex))
                                dragTouchOffsetX = x - tRect.left
                                dragTouchOffsetY = y - (tRect.top + calculateCardYOffset(cards, touchedIndex))
                                soundFeedback?.cardTouch(this)
                                invalidate()
                                return true
                            }
                        }
                    }
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragCurrentX = x
                    dragCurrentY = y
                    if (isParticlesEnabled && !isPowerSaveMode && Math.random() < 0.45) {
                        emitSkinDragTrail(x, y)
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && draggedCards.isNotEmpty()) {
                    val fromType = dragSourcePileType ?: return true
                    val firstCard = draggedCards.firstOrNull()
                    val moveDistance = hypot((x - touchDownX).toDouble(), (y - touchDownY).toDouble()).toFloat()

                    // Se o deslocamento foi muito curto (< 14px), considera como TOQUE (Tap-to-Move Inteligente)
                    if (moveDistance < 14f) {
                        if (firstCard != null) {
                            val autoMoved = eng.autoMove(firstCard, fromType)
                            if (autoMoved) {
                                soundFeedback?.cardPlaced(this)
                                checkColumnCompletions()
                                onMoveMade?.invoke()
                                checkVictory()
                            } else {
                                triggerInvalidMove(fromType)
                            }
                        }
                    } else {
                        // ARRASTAR E SOLTAR (Drag & Drop)
                        val targetPileType = findDropTarget(x, y)
                        var success = false

                        if (targetPileType != null && targetPileType != fromType) {
                            success = eng.moveCards(fromType, targetPileType, draggedCards.size)
                        }

                        if (success) {
                            soundFeedback?.cardPlaced(this)
                            if (targetPileType is PileType.Foundation) {
                                emitFoundationSparkles(targetPileType.index)
                            }
                            checkColumnCompletions()
                            onMoveMade?.invoke()
                            checkVictory()
                        } else {
                            triggerInvalidMove(fromType)
                        }
                    }

                    isDragging = false
                    draggedCards.clear()
                    dragSourcePileType = null
                    invalidate()
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Resposta visual para movimentos inválidos: shake horizontal e flash carmesim.
     */
    fun triggerInvalidMove(pileType: PileType?) {
        invalidMoveStartTime = System.currentTimeMillis()
        invalidMovePileType = pileType
        soundFeedback?.invalidMove(this)
        invalidate()
    }

    /**
     * Efeito comemorativo de faíscas ao mover carta para fundação.
     */
    private fun emitFoundationSparkles(index: Int) {
        if (!isParticlesEnabled || isPowerSaveMode || index !in 0 until 4) return
        val r = foundationRects[index]
        val cx = (r.left + r.right) * 0.5f
        val cy = (r.top + r.bottom) * 0.5f
        for (i in 0 until 10) {
            val vx = (Math.random().toFloat() - 0.5f) * 3.5f
            val vy = -(Math.random().toFloat() * 3f + 1f)
            particleSystem.addParticle(
                Particle(
                    x = cx,
                    y = cy,
                    vx = vx,
                    vy = vy,
                    size = 5.5f,
                    alpha = 240,
                    color = 0xFFFDE047.toInt(),
                    shape = Particle.Shape.SPARK
                )
            )
        }
    }

    /**
     * Verifica se alguma coluna de tableau atingiu sequência completa e dispara o efeito comemorativo.
     */
    fun checkColumnCompletions() {
        val eng = engine ?: return
        for (col in 0 until 7) {
            val pile = eng.tableauPiles[col]
            val cards = pile.cards
            var isComplete = false

            if (cards.isNotEmpty() && cards.all { it.isFaceUp }) {
                var validSeq = true
                for (i in 0 until cards.size - 1) {
                    if (!cards[i + 1].rank.isOneBelow(cards[i].rank) || cards[i + 1].isRed == cards[i].isRed) {
                        validSeq = false
                        break
                    }
                }
                if (validSeq && (cards.last().rank == Rank.ACE || (cards.first().rank == Rank.KING && cards.size >= 4))) {
                    isComplete = true
                }
            }

            if (isComplete && !columnCompletedState[col]) {
                columnCompletedState[col] = true
                triggerColumnCompleteEffect(col)
            } else if (!isComplete) {
                columnCompletedState[col] = false
            }
        }
    }

    private fun triggerColumnCompleteEffect(col: Int) {
        activeColumnEffects.add(ColumnEffect(col))
        soundFeedback?.columnCompleted(this)

        if (isParticlesEnabled && !isPowerSaveMode && col in 0 until 7) {
            val rect = tableauRects[col]
            val cx = (rect.left + rect.right) * 0.5f
            val cy = rect.top + cardHeight * 0.5f
            for (i in 0 until 14) {
                val vx = (Math.random().toFloat() - 0.5f) * 4.5f
                val vy = -(Math.random().toFloat() * 3.5f + 1.5f)
                particleSystem.addParticle(
                    Particle(
                        x = cx,
                        y = cy,
                        vx = vx,
                        vy = vy,
                        size = 7f,
                        alpha = 240,
                        color = 0xFFFBBF24.toInt(),
                        shape = Particle.Shape.SPARK
                    )
                )
            }
        }
        invalidate()
    }

    private fun checkVictory() {
        if (engine?.isWon == true) {
            soundFeedback?.victoryCelebration()
            startSkinVictoryCelebration(cardSkin)
            onVictory?.invoke()
        }
    }

    /**
     * Localiza qual pilha está sob as coordenadas de liberação do toque.
     */
    private fun findDropTarget(x: Float, y: Float): PileType? {
        // Verifica fundações
        for (i in 0 until 4) {
            val rect = foundationRects[i]
            if (rect.contains(x, y)) {
                return PileType.Foundation(i)
            }
        }

        // Verifica colunas de tableau
        for (col in 0 until 7) {
            val rect = tableauRects[col]
            if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                return PileType.Tableau(col)
            }
        }

        return null
    }

    private fun calculateCardYOffset(cards: List<Card>, targetIndex: Int): Float {
        var offset = 0f
        for (i in 0 until targetIndex) {
            offset += if (cards[i].isFaceUp) cardOverlapFaceUp else cardOverlapFaceDown
        }
        return offset
    }

    /**
     * Retorna a área retangular de uma pilha para destaque e efeitos.
     */
    fun getRectForPile(type: PileType): RectF? {
        val eng = engine ?: return null
        return when (type) {
            is PileType.Stock -> stockRect
            is PileType.Waste -> wasteRect
            is PileType.Foundation -> if (type.index in 0 until 4) foundationRects[type.index] else null
            is PileType.Tableau -> {
                if (type.col in 0 until 7) {
                    val r = tableauRects[type.col]
                    val pile = eng.tableauPiles[type.col]
                    val topY = if (pile.isEmpty) r.top else r.top + calculateCardYOffset(pile.cards, pile.cards.size - 1)
                    RectF(r.left, topY, r.right, topY + cardHeight)
                } else null
            }
        }
    }

    /**
     * Exibe o efeito de dica visual destacando as pilhas de origem e destino.
     */
    fun showHint(hint: com.paciencia.solitaire.engine.HintMove) {
        activeHint = hint
        hintStartTime = System.currentTimeMillis()
        soundFeedback?.cardPicked(this)
        invalidate()
    }

    /**
     * Dispara a celebração mágica exclusiva de vitória de acordo com a skin equipada.
     */
    fun startSkinVictoryCelebration(skin: CardSkin) {
        isVictoryCelebrationActive = true
        victoryCelebrationStartTime = System.currentTimeMillis()
        victoryCelebrationSkin = skin
        spawnInitialVictoryBurst(skin)
        invalidate()
    }

    private fun spawnInitialVictoryBurst(skin: CardSkin) {
        if (!isParticlesEnabled || isPowerSaveMode) return
        val cx = width * 0.5f
        val cy = height * 0.45f

        // Onda expansiva de energia
        particleSystem.addParticle(
            Particle(
                x = cx,
                y = cy,
                size = 14f,
                alpha = 255,
                color = skin.accentColor,
                shape = Particle.Shape.RING,
                scaleSpeed = 18f,
                isTemporary = true,
                life = 1f,
                maxLife = 1f
            )
        )

        // Explosão de 36 partículas temáticas
        val count = 36
        for (i in 0 until count) {
            val angle = (Math.PI * 2 * i / count).toFloat()
            val speed = (Math.random() * 6 + 3).toFloat()
            val vx = Math.cos(angle.toDouble()).toFloat() * speed
            val vy = Math.sin(angle.toDouble()).toFloat() * speed

            val shape = when (skin.celebrationType) {
                VictoryCelebrationType.ARCANE_SURGE -> Particle.Shape.RUNE
                VictoryCelebrationType.LIVING_FOREST -> Particle.Shape.LEAF
                VictoryCelebrationType.GLACIAL_BURST -> Particle.Shape.CRYSTAL
                VictoryCelebrationType.ANCESTRAL_FLAME -> Particle.Shape.SPARK
                VictoryCelebrationType.GOLDEN_MAJESTY,
                VictoryCelebrationType.COSMIC_SUPERNOVA,
                VictoryCelebrationType.ROYAL_TRIUMPH -> Particle.Shape.STAR
                else -> Particle.Shape.CIRCLE
            }

            particleSystem.addParticle(
                Particle(
                    x = cx,
                    y = cy,
                    vx = vx,
                    vy = vy,
                    size = (Math.random() * 6 + 5).toFloat(),
                    alpha = 255,
                    color = if (i % 2 == 0) skin.accentColor else skin.trailColor,
                    shape = shape,
                    rotationSpeed = (Math.random().toFloat() - 0.5f) * 12f,
                    isTemporary = true,
                    life = 1.2f,
                    maxLife = 1.2f
                )
            )
        }
    }

    private fun drawVictoryCelebration(canvas: Canvas, skin: CardSkin, elapsed: Long, w: Float, h: Float) {
        val progress = (elapsed.toFloat() / 3600f).coerceIn(0f, 1f)
        val cx = w * 0.5f
        val cy = h * 0.45f

        // Onda de energia expansiva desenhada no canvas
        val waveRadius = w * progress * 0.85f
        val waveAlpha = ((1f - progress) * 200).toInt().coerceIn(0, 255)
        victoryWavePaint.color = skin.accentColor
        victoryWavePaint.alpha = waveAlpha
        victoryWavePaint.strokeWidth = 6f * (1f - progress * 0.5f)
        canvas.drawCircle(cx, cy, waveRadius, victoryWavePaint)

        // Segunda onda com atraso
        if (progress > 0.25f) {
            val wave2Prog = (progress - 0.25f) / 0.75f
            val wave2Radius = w * wave2Prog * 0.85f
            val wave2Alpha = ((1f - wave2Prog) * 160).toInt().coerceIn(0, 255)
            victoryWavePaint.alpha = wave2Alpha
            victoryWavePaint.strokeWidth = 4f
            canvas.drawCircle(cx, cy, wave2Radius, victoryWavePaint)
        }

        // Emissão contínua temática durante os 3.6s
        if (isParticlesEnabled && !isPowerSaveMode && Math.random() < 0.65) {
            when (skin.celebrationType) {
                VictoryCelebrationType.ARCANE_SURGE -> {
                    val pX = (Math.random() * w).toFloat()
                    particleSystem.addParticle(
                        Particle(
                            x = pX,
                            y = h * 0.85f,
                            vx = (Math.sin(elapsed * 0.005 + pX) * 2.5).toFloat(),
                            vy = -(Math.random().toFloat() * 4.5f + 2f),
                            size = 8f,
                            alpha = 240,
                            color = skin.accentColor,
                            shape = Particle.Shape.RUNE,
                            rotationSpeed = 6f,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
                VictoryCelebrationType.GOLDEN_MAJESTY -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = 0f,
                            vx = (Math.random().toFloat() - 0.5f) * 2f,
                            vy = (Math.random().toFloat() * 4.5f + 3f),
                            size = 7f,
                            alpha = 240,
                            color = 0xFFFBBF24.toInt(),
                            shape = Particle.Shape.STAR,
                            rotationSpeed = 8f,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
                VictoryCelebrationType.LIVING_FOREST -> {
                    val angle = (elapsed * 0.008 + Math.random() * 3).toFloat()
                    val r = (Math.random() * w * 0.4).toFloat()
                    particleSystem.addParticle(
                        Particle(
                            x = cx + Math.cos(angle.toDouble()).toFloat() * r,
                            y = cy + Math.sin(angle.toDouble()).toFloat() * r,
                            vx = (Math.random().toFloat() - 0.5f) * 3f,
                            vy = -(Math.random().toFloat() * 3.5f + 1f),
                            size = 9f,
                            alpha = 240,
                            color = 0xFF34D399.toInt(),
                            shape = Particle.Shape.LEAF,
                            rotationSpeed = 7f,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
                VictoryCelebrationType.GLACIAL_BURST -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h).toFloat(),
                            vx = (Math.random().toFloat() - 0.5f) * 2f,
                            vy = (Math.random().toFloat() - 0.5f) * 2f,
                            size = 8f,
                            alpha = 240,
                            color = 0xFF7DD3FC.toInt(),
                            shape = Particle.Shape.CRYSTAL,
                            rotationSpeed = 5f,
                            isTemporary = true,
                            life = 0.9f,
                            maxLife = 0.9f
                        )
                    )
                }
                VictoryCelebrationType.ABYSSAL_SHADOW -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h * 0.8f + h * 0.1f).toFloat(),
                            vx = (Math.random().toFloat() - 0.5f) * 3f,
                            vy = -(Math.random().toFloat() * 2.5f + 0.5f),
                            size = 12f,
                            alpha = 180,
                            color = 0xFFC084FC.toInt(),
                            shape = Particle.Shape.CIRCLE,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
                VictoryCelebrationType.COSMIC_SUPERNOVA -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h * 0.5f).toFloat(),
                            vx = (Math.random().toFloat() * 5f + 2f),
                            vy = (Math.random().toFloat() * 5f + 2f),
                            size = 8f,
                            alpha = 240,
                            color = 0xFFE879F9.toInt(),
                            shape = Particle.Shape.STAR,
                            rotationSpeed = 10f,
                            isTemporary = true,
                            life = 0.8f,
                            maxLife = 0.8f
                        )
                    )
                }
                VictoryCelebrationType.ROYAL_TRIUMPH -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h * 0.3f).toFloat(),
                            vx = (Math.random().toFloat() - 0.5f) * 4f,
                            vy = (Math.random().toFloat() * 4f + 2f),
                            size = 8f,
                            alpha = 240,
                            color = if (Math.random() < 0.5) 0xFFF59E0B.toInt() else 0xFFE11D48.toInt(),
                            shape = Particle.Shape.SPARK,
                            rotationSpeed = 8f,
                            isTemporary = true,
                            life = 1.1f,
                            maxLife = 1.1f
                        )
                    )
                }
                VictoryCelebrationType.ANCESTRAL_FLAME -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h * 0.6f + h * 0.4f).toFloat(),
                            vx = (Math.random().toFloat() - 0.5f) * 4f,
                            vy = -(Math.random().toFloat() * 5.5f + 3f),
                            size = 9f,
                            alpha = 240,
                            color = if (Math.random() < 0.6) 0xFFFB923C.toInt() else 0xFFEF4444.toInt(),
                            shape = Particle.Shape.SPARK,
                            rotationSpeed = 9f,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
                VictoryCelebrationType.CLASSIC_FIREWORKS -> {
                    particleSystem.addParticle(
                        Particle(
                            x = (Math.random() * w).toFloat(),
                            y = (Math.random() * h * 0.4f).toFloat(),
                            vx = (Math.random().toFloat() - 0.5f) * 4f,
                            vy = (Math.random().toFloat() * 3.5f + 1f),
                            size = 6f,
                            alpha = 240,
                            color = if (Math.random() < 0.5) 0xFF60A5FA.toInt() else 0xFFE2E8F0.toInt(),
                            shape = Particle.Shape.STAR,
                            rotationSpeed = 6f,
                            isTemporary = true,
                            life = 1f,
                            maxLife = 1f
                        )
                    )
                }
            }
        }
    }

    private fun emitSkinDragTrail(x: Float, y: Float) {
        val shape = when (cardSkin.pattern) {
            SkinPattern.ARCANE -> Particle.Shape.RUNE
            SkinPattern.NATURE -> Particle.Shape.LEAF
            SkinPattern.ICE -> Particle.Shape.CRYSTAL
            SkinPattern.GOLDEN, SkinPattern.ROYAL, SkinPattern.COSMIC -> Particle.Shape.STAR
            else -> Particle.Shape.CIRCLE
        }
        particleSystem.addParticle(
            Particle(
                x = x + (Math.random().toFloat() - 0.5f) * cardWidth * 0.4f,
                y = y + (Math.random().toFloat() - 0.5f) * cardHeight * 0.4f,
                vx = (Math.random().toFloat() - 0.5f) * 1.5f,
                vy = (Math.random().toFloat() - 0.5f) * 1.5f - 0.8f,
                size = (Math.random() * 4 + 3).toFloat(),
                alpha = 200,
                color = cardSkin.trailColor,
                shape = shape,
                isTemporary = true,
                life = 0.8f,
                maxLife = 0.8f
            )
        )
    }
}
