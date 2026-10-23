==============================================================================================
🃏 PACIÊNCIA KLONDIKE ANDROID — GUIA DE ATUALIZAÇÃO DA TELA DE ABERTURA (SPLASH SCREEN 5s)
==============================================================================================

Olá! O projeto foi atualizado com sucesso para incluir a sua Capa de Abertura Personalizada com
exibição elegante de aproximadamente 5 segundos e transição suave para a mesa de jogo!

Todas as mecânicas, regras, sistema de níveis, moedas, skins, avatares, cenários e economia
foram rigorosamente preservados intactos.

----------------------------------------------------------------------------------------------
1. LOCAL EXATO ONDE VOCÊ DEVE COLOCAR A SUA IMAGEM
----------------------------------------------------------------------------------------------
No projeto nativo Android, o caminho exato é:

    app/src/main/res/drawable/splash_cover.png

Também é aceito como alternativa:
    app/src/main/res/drawable/splash_screen.png

(Se você for rodar via Web/HTML, basta colocar o arquivo "splash_cover.png" na mesma pasta
onde estiver o arquivo "paciencia_web_app.html").

----------------------------------------------------------------------------------------------
2. NOME DE ARQUIVO RECOMENDADO
----------------------------------------------------------------------------------------------
Nome recomendado:
    splash_cover.png

O código foi preparado de forma inteligente para detectar automaticamente:
    - splash_cover (prioritário)
    - splash_screen (alternativo)

----------------------------------------------------------------------------------------------
3. FORMATOS E DIMENSÕES RECOMENDADAS
----------------------------------------------------------------------------------------------
Formatos aceitos:
    - PNG (.png) — Recomendado (máxima fidelidade e nitidez)
    - WEBP (.webp) — Aceito (leve e moderno)
    - JPEG (.jpg / .jpeg) — Aceito

Dimensões recomendadas para telas de smartphones Android:
    - Resolução recomendada: 1080 x 2400 pixels (proporção 9:20, padrão dos celulares modernos)
      ou 1080 x 1920 pixels (proporção 9:16, Full HD tradicional).
    - O layout foi configurado com android:scaleType="fitCenter" e adjustViewBounds="true" sobre
      um fundo complementar escuro (#0F172A). Isso garante que, independentemente do formato ou
      tamanho da tela do aparelho, a sua arte NUNCA será esticada, cortada ou distorcida!

----------------------------------------------------------------------------------------------
4. PRECISO SUBSTITUIR UM ARQUIVO EXISTENTE OU APENAS ADICIONAR UM NOVO?
----------------------------------------------------------------------------------------------
Você só precisa ADICIONAR o seu arquivo "splash_cover.png" dentro da pasta:
    app/src/main/res/drawable/

Não existia nenhuma imagem anterior com esse nome, portanto é apenas colar ou adicionar
o seu arquivo na pasta. Não há risco de quebrar nada.

----------------------------------------------------------------------------------------------
5. COMO FUNCIONA A ESPERA DE APROXIMADAMENTE 5 SEGUNDOS
----------------------------------------------------------------------------------------------
A abertura foi implementada de maneira nativa e profissional através da SplashActivity:
1. Ao tocar no ícone do aplicativo, o Android abre imediatamente a SplashActivity.
2. A tela é renderizada instantaneamente sem telas pretas ou brancas (utiliza o tema
   Theme.PacienciaSolitaire.Splash com o mesmo tom escuro do jogo).
3. A imagem personalizada é exibida em destaque absoluto e tela limpa (sem menus, botões ou cartas por cima).
4. Um Handler assíncrono (Looper.getMainLooper()) conta exatamente 5000ms (5 segundos)
   SEM bloquear a interface ou congelar o aplicativo.
5. Ao concluir os 5 segundos, a SplashActivity inicia a MainActivity com transição suave
   (fade in / fade out) e chama finish().
6. Como o finish() é executado, se o jogador pressionar o botão "Voltar" durante uma partida,
   o jogo NÃO volta para a tela de abertura.

----------------------------------------------------------------------------------------------
6. ARQUIVOS CRIADOS E MODIFICADOS
----------------------------------------------------------------------------------------------
Arquivos criados:
- app/src/main/java/com/paciencia/solitaire/SplashActivity.kt
  -> Activity dedicada para exibição da capa por 5s, carregamento dinâmico e transição limpa.
- app/src/main/res/layout/activity_splash.xml
  -> Layout da tela de abertura com ImageView ajustada (fitCenter) e fundo escuro.

Arquivos modificados:
- app/src/main/AndroidManifest.xml
  -> Registrou a SplashActivity com a intent-filter MAIN/LAUNCHER e colocou a MainActivity
     como atividade principal do jogo.
- app/src/main/res/values/styles.xml
  -> Adicionou o estilo Theme.PacienciaSolitaire.Splash (sem barra de título e sem cintilação).
- paciencia_web_app.html
  -> Também recebeu a camada de Splash Screen de 5 segundos com fade-out automático para
     testes imediatos no celular ou navegador.

----------------------------------------------------------------------------------------------
7. COMO COMPILAR E GERAR O APK SEM PRECISAR DO ANDROID STUDIO
----------------------------------------------------------------------------------------------
Se você não tem o Android Studio funcionando no seu computador, você pode gerar o APK
100% de graça e automaticamente pelo GitHub Actions:

PASSO A PASSO (EM 2 MINUTOS):
1. No seu repositório no GitHub onde está o código, adicione a sua imagem em:
   app/src/main/res/drawable/splash_cover.png
2. Faça commit e push para o GitHub.
3. No site do GitHub, clique na aba "Actions".
4. Selecione o fluxo "Gerar APK da Paciencia" no menu esquerdo e clique no botão "Run workflow".
5. O GitHub executará a compilação completa nos servidores do Ubuntu automaticamente.
6. Quando finalizar com ícone verde (✓), clique nele, role até o final na seção "Artifacts"
   e baixe o "Paciencia-Solitaire-APK".
7. Dentro do ZIP estará o "app-debug.apk" pronto para você instalar direto no celular!

----------------------------------------------------------------------------------------------
8. ARQUIVOS DISPONÍVEIS NESTE DIRETÓRIO:
----------------------------------------------------------------------------------------------
- paciencia_web_app.html            -> Jogo completo pronto para rodar em qualquer celular/PC
- paciencia-solitaire-android.zip   -> Projeto Android nativo atualizado (pronto para GitHub / Android Studio)
- paciencia-solitaire-android.tar.gz-> Mesmo projeto Android em formato tar.gz
- README.txt                        -> Este guia completo de instruções
==============================================================================================
