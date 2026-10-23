package com.paciencia.solitaire.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Camada desacoplada de gerenciamento de anúncios Google AdMob.
 *
 * Características:
 * - Não bloqueia a jogabilidade nem as transições caso os anúncios falhem ou estejam indisponíveis.
 * - Pré-carregamento assíncrono para exibição instantânea.
 * - Proteção contra cliques múltiplos simultâneos e recompensa duplicada.
 * - Callbacks formais para vitória e bonificação.
 */
class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isShowingAd = false

    var onRewardedAdAvailabilityChanged: ((Boolean) -> Unit)? = null

    val isRewardedAdReady: Boolean
        get() = rewardedAd != null && !isShowingAd

    val isInterstitialAdReady: Boolean
        get() = interstitialAd != null && !isShowingAd

    /**
     * Inicializa o SDK do Google Mobile Ads e inicia o pré-carregamento.
     */
    fun initialize() {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google Mobile Ads inicializado com status: $status")
                loadInterstitialAd()
                loadRewardedAd()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Falha segura ao inicializar MobileAds", t)
        }
    }

    /**
     * Pré-carrega um anúncio intersticial se ainda não estiver carregado.
     */
    fun loadInterstitialAd() {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getInterstitialAdUnitId()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Anúncio intersticial carregado com sucesso.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Falha ao carregar intersticial: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Exibe o anúncio intersticial de encerramento de partida.
     * Se o anúncio não estiver pronto, chama imediatamente [onAdDismissed] para não bloquear o jogador.
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || isShowingAd) {
            Log.d(TAG, "Intersticial não disponível. Prosseguindo sem bloqueio.")
            onAdDismissed()
            loadInterstitialAd()
            return
        }

        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Intersticial fechado pelo usuário.")
                interstitialAd = null
                isShowingAd = false
                loadInterstitialAd()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Falha ao exibir intersticial: ${adError.message}")
                interstitialAd = null
                isShowingAd = false
                loadInterstitialAd()
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Intersticial exibido na tela.")
            }
        }

        try {
            ad.show(activity)
        } catch (t: Throwable) {
            Log.e(TAG, "Exceção ao chamar show() no intersticial", t)
            isShowingAd = false
            interstitialAd = null
            onAdDismissed()
        }
    }

    /**
     * Pré-carrega um anúncio em vídeo recompensado.
     */
    fun loadRewardedAd() {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getRewardedAdUnitId()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    onRewardedAdAvailabilityChanged?.invoke(true)
                    Log.d(TAG, "Vídeo recompensado carregado com sucesso.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    onRewardedAdAvailabilityChanged?.invoke(false)
                    Log.w(TAG, "Falha ao carregar vídeo recompensado: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Exibe o vídeo recompensado.
     * @param onUserEarnedReward Chamado apenas quando a rede confirmar que o vídeo foi assistido até o fim.
     * @param onAdClosed Chamado quando o anúncio fechar ou falhar.
     * @return true se o anúncio começou a ser exibido, false se estava indisponível.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (coinsReward: Int) -> Unit,
        onAdClosed: (rewardGranted: Boolean) -> Unit
    ): Boolean {
        val ad = rewardedAd
        if (ad == null || isShowingAd) {
            Log.d(TAG, "Vídeo recompensado indisponível no momento.")
            loadRewardedAd()
            return false
        }

        isShowingAd = true
        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Vídeo recompensado fechado. Recompensa concedida: $rewardEarned")
                rewardedAd = null
                isShowingAd = false
                onRewardedAdAvailabilityChanged?.invoke(false)
                loadRewardedAd()
                onAdClosed(rewardEarned)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Falha ao exibir vídeo recompensado: ${adError.message}")
                rewardedAd = null
                isShowingAd = false
                onRewardedAdAvailabilityChanged?.invoke(false)
                loadRewardedAd()
                onAdClosed(false)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Vídeo recompensado exibido.")
            }
        }

        try {
            ad.show(activity) { rewardItem ->
                // Callback oficial do Google AdMob disparado quando a recompensa é validada
                rewardEarned = true
                Log.d(TAG, "Usuário completou o vídeo! Tipo: ${rewardItem.type}, Qtd: ${rewardItem.amount}")
                onUserEarnedReward(rewardItem.amount)
            }
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Exceção ao exibir vídeo recompensado", t)
            isShowingAd = false
            rewardedAd = null
            onAdClosed(false)
            return false
        }
    }

    companion object {
        private const val TAG = "AdManager"
    }
}
