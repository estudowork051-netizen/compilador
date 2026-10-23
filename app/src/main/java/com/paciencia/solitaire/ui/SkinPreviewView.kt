package com.paciencia.solitaire.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.customization.SkinPattern

/**
 * View dedicada para renderização ampliada de skins de cartas e avatares na loja.
 * Renderiza o acabamento vetorial completo com gradientes, molduras, padrões temáticos
 * e retratos artísticos autênticos de personagens Dark Fantasy.
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
        strokeWidth = 2.4f
    }
    private val patternFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000.toInt()
        style = Paint.Style.FILL
    }

    private val avatarRenderer = AvatarRenderer()
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

        // Sombra de profundidade 3D
        shadowRectF.set(pad + 4f, pad + 7f, pad + cardW + 4f, pad + cardH + 7f)
        canvas.drawRoundRect(shadowRectF, cornerRadius, cornerRadius, shadowPaint)

        // Brilho místico pulsante da Skin
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = skin.accentColor
            alpha = 160
        }
        val auraRect = RectF(pad - 2f, pad - 2f, pad + cardW + 2f, pad + cardH + 2f)
        canvas.drawRoundRect(auraRect, cornerRadius + 2f, cornerRadius + 2f, auraPaint)

        // Fundo com gradiente nobre da skin
        rectF.set(pad, pad, pad + cardW, pad + cardH)
        bgPaint.shader = LinearGradient(
            pad, pad, pad + cardW, pad + cardH,
            skin.primaryColor, skin.secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)

        // Moldura externa da carta
        borderPaint.color = 0x66FFFFFF.toInt()
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        // Moldura interna ornamental de ouro/acento
        val innerPad = cardW * 0.08f
        rectF.set(pad + innerPad, pad + innerPad, pad + cardW - innerPad, pad + cardH - innerPad)
        innerBorderPaint.color = skin.accentColor
        innerBorderPaint.alpha = 200
        canvas.drawRoundRect(rectF, cornerRadius * 0.7f, cornerRadius * 0.7f, innerBorderPaint)

        // Segunda linha interna sutil
        val innerPad2 = cardW * 0.12f
        rectF.set(pad + innerPad2, pad + innerPad2, pad + cardW - innerPad2, pad + cardH - innerPad2)
        innerBorderPaint.alpha = 100
        canvas.drawRoundRect(rectF, cornerRadius * 0.5f, cornerRadius * 0.5f, innerBorderPaint)

        // Padrão geométrico central da skin
        val cx = pad + cardW * 0.5f
        val cy = pad + cardH * 0.5f
        patternPaint.color = skin.accentColor
        patternPaint.alpha = 230
        patternFillPaint.color = skin.accentColor
        patternFillPaint.alpha = 45
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
                canvas.drawCircle(cx, cy, size * 0.4f, patternPaint)
            }
            SkinPattern.GOLDEN -> {
                // Roseta e cruz radiante barroca
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size, cy)
                path.lineTo(cx, cy + size)
                path.lineTo(cx - size, cy)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawCircle(cx, cy, size * 0.45f, patternPaint)
                canvas.drawLine(cx - size * 0.75f, cy, cx + size * 0.75f, cy, patternPaint)
                canvas.drawLine(cx, cy - size * 0.75f, cx, cy + size * 0.75f, patternPaint)
            }
            SkinPattern.ARCANE -> {
                // Runa de 8 pontas e anel místico
                canvas.drawCircle(cx, cy, size * 0.85f, patternPaint)
                path.moveTo(cx, cy - size * 0.8f)
                path.lineTo(cx + size * 0.25f, cy - size * 0.25f)
                path.lineTo(cx + size * 0.8f, cy)
                path.lineTo(cx + size * 0.25f, cy + size * 0.25f)
                path.lineTo(cx, cy + size * 0.8f)
                path.lineTo(cx - size * 0.25f, cy + size * 0.25f)
                path.lineTo(cx - size * 0.8f, cy)
                path.lineTo(cx - size * 0.25f, cy - size * 0.25f)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawCircle(cx, cy, size * 0.25f, patternFillPaint)
            }
            SkinPattern.SHADOW -> {
                // Escudo gótico de obsidiana e cruz
                path.moveTo(cx - size * 0.7f, cy - size * 0.7f)
                path.lineTo(cx + size * 0.7f, cy - size * 0.7f)
                path.lineTo(cx + size * 0.7f, cy)
                path.lineTo(cx, cy + size * 0.95f)
                path.lineTo(cx - size * 0.7f, cy)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawLine(cx, cy - size * 0.4f, cx, cy + size * 0.5f, patternPaint)
                canvas.drawLine(cx - size * 0.35f, cy - size * 0.05f, cx + size * 0.35f, cy - size * 0.05f, patternPaint)
            }
            SkinPattern.NATURE -> {
                // Folha e vinha sagrada élfica
                val leafW = size * 0.75f
                path.moveTo(cx, cy - size * 0.9f)
                path.quadTo(cx + leafW, cy, cx, cy + size * 0.9f)
                path.quadTo(cx - leafW, cy, cx, cy - size * 0.9f)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawPath(path, patternFillPaint)
                canvas.drawLine(cx, cy - size * 0.7f, cx, cy + size * 0.7f, patternPaint)
            }
            SkinPattern.ICE -> {
                // Floco de neve fractal polar
                val arm = size * 0.85f
                canvas.drawLine(cx, cy - arm, cx, cy + arm, patternPaint)
                canvas.drawLine(cx - arm, cy, cx + arm, cy, patternPaint)
                val diag = arm * 0.7f
                canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, patternPaint)
                canvas.drawLine(cx - diag, cy + diag, cx + diag, cy - diag, patternPaint)
                canvas.drawCircle(cx, cy, size * 0.25f, patternPaint)
            }
            SkinPattern.FLAME -> {
                // Chama ancestral com ponta flamejante
                path.moveTo(cx, cy - size * 0.95f)
                path.quadTo(cx + size * 0.7f, cy, cx, cy + size * 0.85f)
                path.quadTo(cx - size * 0.7f, cy, cx, cy - size * 0.95f)
                path.close()
                canvas.drawPath(path, patternPaint)
                canvas.drawPath(path, patternFillPaint)
                canvas.drawCircle(cx, cy + size * 0.2f, size * 0.22f, patternPaint)
            }
            SkinPattern.COSMIC -> {
                // Astrolábio cósmico e anéis astrais
                canvas.drawCircle(cx, cy, size * 0.45f, patternPaint)
                canvas.save()
                canvas.rotate(45f, cx, cy)
                canvas.drawCircle(cx, cy, size * 0.85f, patternPaint)
                canvas.restore()
                canvas.drawCircle(cx, cy, size * 0.15f, patternFillPaint)
            }
            SkinPattern.ROYAL -> {
                // Coroa imperial ornamentada
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
                canvas.drawPath(path, patternFillPaint)
            }
        }
    }

    private fun drawAvatarPreview(canvas: Canvas, av: Avatar, w: Float, h: Float) {
        val size = Math.min(w, h) * 0.88f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = size * 0.5f

        // Renderiza o retrato vetorial autêntico do personagem
        avatarRenderer.drawAvatar(canvas, av, cx, cy, radius)
    }
}
