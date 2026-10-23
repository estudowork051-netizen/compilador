package com.paciencia.solitaire.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.paciencia.solitaire.engine.KlondikeEngine
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import com.paciencia.solitaire.scenery.ParticleSystem
import com.paciencia.solitaire.scenery.SceneryManager
import com.paciencia.solitaire.scenery.SceneryTheme
import com.paciencia.solitaire.scenery.SceneryType
import kotlin.math.hypot

/**
 * Custom View interativa de alto desempenho para o jogo Paciência.
 * Renderiza o cenário procedural, sistema de partículas, cartas vetoriais,
 * e gerencia os gestos de arrastar-e-soltar e toque inteligente.
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

    var soundFeedback: SoundFeedback? = null
    var onMoveMade: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    var isParticlesEnabled: Boolean = true
    var isPowerSaveMode: Boolean = false

    private val cardRenderer = CardRenderer()
    private val particleSystem = ParticleSystem()

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
                cardRenderer.drawCard(canvas, topCard, stockRect.left, stockRect.top, cardWidth, cardHeight, theme)
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
                    cardRenderer.drawCard(canvas, card, wasteRect.left, wasteRect.top, cardWidth, cardHeight, theme)
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
                        cardRenderer.drawCard(canvas, card, rect.left, rect.top, cardWidth, cardHeight, theme)
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
                    cardRenderer.drawCard(canvas, card, rect.left, currentY, cardWidth, cardHeight, theme)
                    currentY += if (card.isFaceUp) cardOverlapFaceUp else cardOverlapFaceDown
                }
            }
        }

        // 7. Renderiza as cartas flutuantes que estão sendo arrastadas pelo dedo com destaque e sombra elevada
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
                    isHighlighted = true
                )
                currentY += cardOverlapFaceUp
            }
        }

        // Mantém a animação suave a 60 FPS se houver partículas ativas
        if (isParticlesEnabled && !isPowerSaveMode) {
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
                            // Se a carta do topo estiver virada para baixo, permite revelá-la ao tocar
                            if (!touchedCard.isFaceUp && touchedIndex == cards.size - 1) {
                                touchedCard.isFaceUp = true
                                soundFeedback?.cardPlaced(this)
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
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && draggedCards.isNotEmpty()) {
                    val fromType = dragSourcePileType ?: return true
                    val moveDistance = hypot((x - touchDownX).toDouble(), (y - touchDownY).toDouble()).toFloat()

                    // Se o deslocamento foi muito curto (< 14px), considera como TOQUE (Tap-to-Move Inteligente)
                    if (moveDistance < 14f) {
                        val firstCard = draggedCards.first()
                        val autoMoved = eng.autoMove(firstCard, fromType)
                        if (autoMoved) {
                            soundFeedback?.cardPlaced(this)
                            onMoveMade?.invoke()
                            checkVictory()
                        } else {
                            soundFeedback?.invalidMove(this)
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
                            onMoveMade?.invoke()
                            checkVictory()
                        } else {
                            soundFeedback?.invalidMove(this)
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

    private fun checkVictory() {
        if (engine?.isWon == true) {
            soundFeedback?.victoryCelebration()
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
}
