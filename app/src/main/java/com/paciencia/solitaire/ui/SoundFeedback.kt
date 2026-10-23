package com.paciencia.solitaire.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Fornece resposta tátil (haptic feedback) de alta fidelidade e leveza.
 */
class SoundFeedback(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isEnabled: Boolean = true

    /**
     * Feedback sutil ao tocar ou selecionar uma carta.
     */
    fun cardTouch(view: View) {
        if (!isEnabled) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Throwable) {
            // Ignora com segurança em dispositivos sem suporte a feedback tátil
        }
    }

    /**
     * Feedback de confirmação ao encaixar uma carta com sucesso.
     */
    fun cardPlaced(view: View) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Throwable) {
            // Fallback para haptic da View se permissão ou vibrador falhar
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Feedback comemorativo de vitória.
     */
    fun victoryCelebration() {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 40, 60, 80, 80, 120)
                val amplitudes = intArrayOf(0, 100, 0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 40, 60, 80, 80, 120), -1)
            }
        } catch (_: Throwable) {
            // Previne qualquer crash por exceção de segurança
        }
    }

    /**
     * Feedback sutil para movimentos inválidos ou bloqueados.
     */
    fun invalidMove(view: View) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 15, 40, 15)
                val amplitudes = intArrayOf(0, 80, 0, 80)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 15, 40, 15), -1)
            }
        } catch (_: Throwable) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } catch (_: Throwable) {}
        }
    }
}
