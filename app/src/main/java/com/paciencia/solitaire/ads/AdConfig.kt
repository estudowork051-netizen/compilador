package com.paciencia.solitaire.ads

/**
 * Configuração central de identificadores e parâmetros dos anúncios AdMob.
 *
 * IMPORTANTE PARA PUBLICAÇÃO EM PRODUÇÃO:
 * 1. Crie uma conta no Google AdMob (https://admob.google.com).
 * 2. Cadastre o aplicativo e crie os blocos de anúncio Intersticial e Recompensado.
 * 3. Atualize o Application ID em 'app/src/main/AndroidManifest.xml'.
 * 4. Insira os IDs reais de produção nas constantes PROD_* abaixo.
 * 5. Mude a flag IS_TEST_MODE para false antes de gerar o APK de Release final.
 */
object AdConfig {

    /**
     * Define se o aplicativo está utilizando anúncios de teste oficiais do Google AdMob.
     * Mantenha SEMPRE 'true' durante desenvolvimento e testes para não violar as políticas do AdMob.
     * Altere para 'false' apenas no build oficial de lançamento em produção.
     */
    var IS_TEST_MODE: Boolean = true

    // Identificadores Oficiais de Teste do Google AdMob
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Identificadores de Produção (Substitua pelos IDs reais gerados no seu painel AdMob)
    const val PROD_INTERSTITIAL_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    const val PROD_REWARDED_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ"

    /**
     * Retorna o ID da unidade de anúncio Intersticial ativo.
     */
    fun getInterstitialAdUnitId(): String {
        return if (IS_TEST_MODE) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
    }

    /**
     * Retorna o ID da unidade de anúncio Recompensado ativo.
     */
    fun getRewardedAdUnitId(): String {
        return if (IS_TEST_MODE) TEST_REWARDED_ID else PROD_REWARDED_ID
    }
}
