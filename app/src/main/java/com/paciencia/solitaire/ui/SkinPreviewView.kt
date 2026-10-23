package com.paciencia.solitaire.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.customization.SkinPattern

/**
 * View dedicada para renderização ampliada de skins de cartas e avatares na loja.
 * Renderiza o acabamento vetorial completo com gradientes, molduras e padrões temáticos.
 */
class SkinPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var cardSkin: CardSkin? = CardSkin.CLASSIC
        set(value) {
            field = value
            avatar = null
            invalidate()
        }

    var avatar: Avatar? = null
        set(value) {
            field = value
            cardSkin = null
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x55FFFFFF.toInt()
    }
    private val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000.toInt()
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val rectF = RectF()
    private val shadowRectF = RectF()
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val skin = cardSkin
        val av = avatar

        if (skin != null) {
            drawSkinCardPreview(canvas, skin, w, h)
        } else if (av != null) {
            drawAvatarPreview(canvas, av, w, h)
        }
    }

    private fun drawSkinCardPreview(canvas: Canvas, skin: CardSkin, w: Float, h: Float) {
        val pad = w * 0.06f
        val cardW = w - pad * 2f
        val cardH = h - pad * 2f
        val cornerRadius = cardW * 0.10f

        // Sombra de profundidade
        shadowRectF.set(pad + 3f, pad + 5f, pad + cardW + 3f, pad + cardH + 5f)
        canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, shadowPaint)

        // Brilho místico suave da Skin
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = skin.accentColor
            alpha = 130
        }
        val auraRect = RectF(pad - 2f, pad - 2f, pad + cardW + 2f, pad + cardH + 2f)
        canvas.drawRoundRect(auraRect, cornerRadius + 2f, cornerRadius + 2f, auraPaint)

        // Fundo com gradiente da skin
        rectF.set(pad, pad, pad + cardW, pad + cardH)
        bgPaint.shader = LinearGradient(
            pad, pad, pad + cardW, pad + cardH,
            skin.primaryColor, skin.secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)

        // Moldura externa da carta
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        // Moldura interna ornamental
        val innerPad = cardW * 0.08f
        rectF.set(pad + innerPad, pad + innerPad, pad + cardW - innerPad, pad + cardH - innerPad)
        innerBorderPaint.color = skin.accentColor
        innerBorderPaint.alpha = 180
        canvas.drawRoundRect(rectF, cornerRadius * 0.7f, cornerRadius * 0.7f, innerBorderPaint)

        // Padrão geométrico central da skin
        val cx = pad + cardW * 0.5f
        val cy = pad + cardH * 0.5f
        patternPaint.color = skin.accentColor
        patternPaint.alpha = 220
        drawSkinPattern(canvas, skin.pattern, cx, cy, cardW)
    }

    private fun drawSkinPattern(canvas: Canvas, pattern: SkinPattern, cx: Float, cy: Float, cardW: Float) {
        val size = cardW * 0.28f
        path.reset()

        when (pattern) {
            SkinPattern.CLASSIC -> {
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size, cy)
                path.lineTo(cx, cy + size)
                path.lineTo(cx - size, cy)
                path.close()
                canvas.drawPath(path, patternPaint)
            }
            SkinPattern.GOLDEN -> {
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size, cy)
                path.lineTo(cx, cy + size)
                path.lineTo(cx - size, cy)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawCircle(cx, cy, size * 0.4f, patternPaint)
            }
            SkinPattern.ARCANE -> {
                canvas.drawCircle(cx, cy, size * 0.85f, patternPaint)
                path.moveTo(cx, cy - size * 0.8f)
                path.lineTo(cx + size * 0.2f, cy - size * 0.2f)
                path.lineTo(cx + size * 0.8f, cy)
                path.lineTo(cx + size * 0.2f, cy + size * 0.2f)
                path.lineTo(cx, cy + size * 0.8f)
                path.lineTo(cx - size * 0.2f, cy + size * 0.2f)
                path.lineTo(cx - size * 0.8f, cy)
                path.lineTo(cx - size * 0.2f, cy - size * 0.2f)
                path.close()
                canvas.drawPath(path, patternPaint)
            }
            SkinPattern.SHADOW -> {
                path.moveTo(cx - size * 0.7f, cy - size * 0.7f)
                path.lineTo(cx + size * 0.7f, cy - size * 0.7f)
                path.lineTo(cx + size * 0.7f, cy)
                path.lineTo(cx, cy + size * 0.95f)
                path.lineTo(cx - size * 0.7f, cy)
                path.close()
                canvas.drawPath(path, patternPaint)
            }
            SkinPattern.NATURE -> {
                val leafW = size * 0.75f
                path.moveTo(cx, cy - size * 0.9f)
                path.quadTo(cx + leafW, cy, cx, cy + size * 0.9f)
                path.quadTo(cx - leafW, cy, cx, cy - size * 0.9f)
                path.close()
                canvas.drawPath(path, patternPaint)
            }
            SkinPattern.ICE -> {
                val arm = size * 0.85f
                canvas.drawLine(cx, cy - arm, cx, cy + arm, patternPaint)
                canvas.drawLine(cx - arm, cy, cx + arm, cy, patternPaint)
                val diag = arm * 0.7f
                canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, patternPaint)
                canvas.drawLine(cx - diag, cy + diag, cx + diag, cy - diag, patternPaint)
            }
            SkinPattern.COSMIC -> {
                canvas.drawCircle(cx, cy, size * 0.45f, patternPaint)
                canvas.save()
                canvas.rotate(45f, cx, cy)
                canvas.drawCircle(cx, cy, size * 0.85f, patternPaint)
                canvas.restore()
            }
            SkinPattern.ROYAL -> {
                val crownW = size * 0.85f
                val crownH = size * 0.65f
                path.moveTo(cx - crownW, cy + crownH * 0.5f)
                path.lineTo(cx + crownW, cy + crownH * 0.5f)
                path.lineTo(cx + crownW * 0.85f, cy - crownH * 0.5f)
                path.lineTo(cx + crownW * 0.4f, cy)
                path.lineTo(cx, cy - crownH * 0.7f)
                path.lineTo(cx - crownW * 0.4f, cy)
                path.lineTo(cx - crownW * 0.85f, cy - crownH * 0.5f)
                path.close()
                canvas.drawPath(path, patternPaint)
            }
        }
    }

    private fun drawAvatarPreview(canvas: Canvas, av: Avatar, w: Float, h: Float) {
        val size = Math.min(w, h) * 0.85f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = size * 0.5f

        // Sombra
        canvas.drawCircle(cx, cy + 4f, radius, shadowPaint)

        // Fundo do Avatar
        bgPaint.shader = null
        bgPaint.color = av.bgColor
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Borda brilhante
        borderPaint.color = av.borderColor
        borderPaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, radius - 2f, borderPaint)

        // Emoji central
        textPaint.textSize = size * 0.55f
        textPaint.color = Color.WHITE
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(av.iconEmoji, cx, textY, textPaint)
    }
}
