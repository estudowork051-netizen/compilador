package com.paciencia.solitaire

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity - Tela de Abertura / Capa de Inicialização do Jogo
 *
 * Exibe a imagem de capa personalizada localizada em 'res/drawable/splash_cover'
 * (ou 'res/drawable/splash_screen') por aproximadamente 5 segundos com proporção
 * original preservada (fitCenter) e fundo escuro complementar.
 * Transiciona suavemente para MainActivity sem travar a interface e encerra
 * esta activity para não retornar ao pressionar 'voltar'.
 */
class SplashActivity : AppCompatActivity() {

    private val splashHandler = Handler(Looper.getMainLooper())
    private val splashDurationMs = 5000L // 5 segundos
    private var hasNavigated = false

    private val launchGameRunnable = Runnable {
        openMainActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivSplashCover = findViewById<ImageView>(R.id.ivSplashCover)

        // Carrega dinamicamente a imagem 'splash_cover' ou 'splash_screen' em res/drawable
        val coverResId = resources.getIdentifier("splash_cover", "drawable", packageName).let {
            if (it != 0) it else resources.getIdentifier("splash_screen", "drawable", packageName)
        }
        if (coverResId != 0) {
            ivSplashCover.setImageResource(coverResId)
        }

        // Aguarda os 5 segundos de forma assíncrona sem bloquear a UI thread
        splashHandler.postDelayed(launchGameRunnable, splashDurationMs)
    }

    private fun openMainActivity() {
        if (hasNavigated) return
        hasNavigated = true

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        // Transição suave em fade in/fade out sem telas pretas ou piscadas
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Evita vazamentos de memória ou callbacks órfãos caso a activity seja fechada antes
        splashHandler.removeCallbacks(launchGameRunnable)
    }
}
