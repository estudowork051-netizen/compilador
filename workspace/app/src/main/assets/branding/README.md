# Viora — Imagens e Identidade Visual Personalizada

Este diretório (`app/src/main/assets/branding/`) está configurado para receber as imagens de autoria própria do usuário para o aplicativo **Viora**.

---

## 1. Arquivos Suportados e Requisitos

### 1.1 Ícone do Aplicativo (Launcher Icon)
- **Nome exato do arquivo:** `viora_icon.png` (ou `lifeflow_icon.png` para retrocompatibilidade)
- **Formato:** PNG (com ou sem transparência)
- **Resolução recomendada:** 1024 × 1024 pixels (formato quadrado, 1:1)
- **Ícone Adaptativo Android (API 26+):** O processo de build gera automaticamente a camada de primeiro plano (`ic_launcher_foreground.png`) centralizada na área segura de 72dp dentro do canvas de 108dp, evitando qualquer corte por máscaras circulares, squircles ou cantos arredondados de diferentes fabricantes de celulares Android (Samsung, Xiaomi, Motorola, Pixel, etc.).
- **Ícones Legados:** O processo de build também gera automaticamente as versões `ic_launcher.png` e `ic_launcher_round.png` para todas as densidades de tela (`mipmap-mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

### 1.2 Imagem da Apresentação Inicial (Splash Screen)
- **Nome exato do arquivo:** `viora_splash.png` (ou `lifeflow_splash.png` para retrocompatibilidade)
- **Formato:** PNG
- **Resolução recomendada:** 1080 × 1920 pixels ou 1080 × 2400 pixels (orientação vertical / retrato, 9:16 ou 9:20)
- **Preenchimento total da tela:** A `SplashScreen` utiliza renderização de ponta a ponta (*Edge-to-Edge*) com `ContentScale.Crop` e cor de fundo harmonizada, preenchendo 100% da área física da tela (inclusive sob as barras de status e navegação), sem faixas vazias ou barras pretas. A apresentação é exibida suavemente por 3 a 4 segundos na inicialização.

---

## 2. Como Funciona a Integração Automática

O processo de compilação do Viora está 100% automatizado:

1. **Ao adicionar os arquivos neste diretório:**
   Basta colocar `viora_icon.png` e/ou `viora_splash.png` em `app/src/main/assets/branding/`.

2. **Durante a compilação (Local ou GitHub Actions):**
   - O Gradle executa automaticamente a tarefa `processBrandingAssets` antes da geração de recursos.
   - O script `convert_branding_icons.sh` valida a integridade dos arquivos PNG.
   - Os ícones adaptativos e legados são gerados em todas as pastas `res/mipmap-*`.
   - O arquivo `mipmap-anydpi-v26/ic_launcher.xml` é configurado para utilizar `@mipmap/ic_launcher_foreground`.
   - A Splash Screen é copiada para `res/drawable-nodpi/` e empacotada no APK.

3. **Se os arquivos estiverem ausentes:**
   - O aplicativo compila normalmente sem erros, mantendo a identidade visual nativa *Sage Green & Nature* como fallback.

---

## 3. Resumo das Recomendações Técnicas

| Recurso | Arquivo | Resolução Recomendada | Proporção | Formato | Enquadramento |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Ícone do App** | `viora_icon.png` | 1024 × 1024 px | 1:1 (Quadrado) | PNG | Centralizado com margem segura |
| **Splash Screen** | `viora_splash.png` | 1080 × 2400 px | 9:16 ou 9:20 | PNG | Tela cheia (Edge-to-Edge) |
