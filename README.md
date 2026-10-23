# Paciência Solitaire (Klondike) para Android — Edição Premium

Jogo nativo completo de cartas **Paciência (Klondike Solitaire)** para Android, desenvolvido em **Kotlin**, com sistema de progressão por níveis (1 a 50+), 5 cenários visuais cinematográficos vivos, controles híbridos refinados (Arrastar e Soltar com realce de destinos válidos + Toque Inteligente), galeria de coleções de skins e avatares, sistema de economia de moedas, anúncios oficiais AdMob (Intersticial e Recompensado) e persistência local robusta.

---

## 📱 Como Baixar e Instalar Diretamente no Celular (SEM Android Studio)

Se você não possui ou tem dificuldades com o Android Studio, o projeto já está 100% configurado para **compilar automaticamente na nuvem pelo GitHub Actions**:

1. **Acesse o seu repositório no GitHub** pelo navegador do celular ou computador.
2. Clique na aba superior **"Actions"**.
3. Clique no workflow mais recente: **"Gerar APK da Paciencia"** (ou acione manualmente via *Run workflow*).
4. Ao término da compilação (geralmente leva ~2 minutos), desça a página até a seção **"Artifacts"**.
5. Clique em **"Paciencia-Solitaire-APK"** para baixar o arquivo `.zip` contendo o APK (`app-debug.apk`).
6. No celular Android:
   - Abra o arquivo baixado ou descompacte o `.zip`.
   - Toque em `app-debug.apk`.
   - Caso o Android solicite, autorize "Instalar aplicativos de fontes desconhecidas" para o navegador/gerenciador de arquivos.
   - O jogo será instalado diretamente no seu celular, pronto para jogar!

*(Além disso, o projeto inclui uma versão Web completa e autônoma — `paciencia_web_app.html` — que roda diretamente em qualquer navegador mobile ou desktop sem necessidade de instalação!)*

---

## 🃏 Visão Geral do Jogo

- **Mecânica Clássica**: Baralho tradicional de 52 cartas sem coringas, 7 colunas em cascata, monte de compra (Stock), descarte (Waste) e 4 fundações (do Ás ao Rei por naipe).
- **Progressão por Níveis**: Mais de 50 níveis com sementes determinísticas para desafios equilibrados e reprodutíveis.
- **5 Cenários Cinematográficos Vivos**:
  1. **Floresta Encantada** (*Níveis 1 a 10*): Atmosfera mística esmeralda com múltiplos planos de copas, raios crepusculares com oscilação sutil, névoa terrena e vaga-lumes bioluminescentes pulsantes.
  2. **Montanhas Geladas** (*Níveis 11 a 20*): Cordilheira ártica em planos de profundidade recortados, aurora boreal suave no céu polar, reflexos glaciais azulados e flocos de neve caindo.
  3. **Deserto Dourado** (*Níveis 21 a 30*): Dunas sinuosas com relevo de sombras, miragem solar quente pulsante, cristas iluminadas em ouro e partículas de areia sopradas pelo vento.
  4. **Reino Medieval** (*Níveis 31 a 40*): Cidadela gótica com torres e ameias, flâmulas balançando ao vento, janelas iluminadas por velas aconchegantes e tochas dinâmicas crepitantes com halos pulsantes.
  5. **Noite Cósmica** (*Níveis 41 em diante*): Nebulosas multicamadas (violeta, magenta e ciano celeste), estrelas de alta magnitude com cintilação e poeira interestelar flutuante.
- **Controles Híbridos e Animações Premium**:
  - *Realce de Destinos Válidos*: Ao arrastar uma carta ou sequência, as fundações e colunas que aceitam o movimento brilham com aura esmeralda suave pulsante.
  - *Feedback de Movimento Inválido*: Resposta tátil e visual com shake horizontal sutil e flash carmesim na pilha de origem.
  - *Efeito de Revelação*: Brilho de partículas esmeralda ao virar a carta do topo oculta.
  - *Efeito de Fundação e Coluna Completa*: Faíscas douradas ao posicionar cartas nas fundações e aura brilhante comemorativa ao fechar colunas completas.
  - *Chuva de Estrelas na Vitória*: Cascata contínua de estrelas douradas e esmeraldas ao vencer uma partida.
- **Galeria de Coleções (Loja de Personalização)**:
  - **8 Skins de Cartas Colecionáveis**: Clássica (*Tradição*), Dourada (*Relíquias Douradas*), Arcana (*Arcano*), Sombria (*Sombras & Mistério*), Natureza (*Natureza*), Gelo (*Gelo*), Cósmica (*Cósmico*) e Real (*Reino Medieval*).
  - **8 Avatares Exclusivos**: Aventureiro, Mago, Cavaleiro, Guardião da Floresta, Espírito do Gelo, Viajante do Deserto, Rei Medieval e Explorador Cósmico.
  - **Prévia Ampliada Premium**: Painel de visualização dinâmica antes de comprar, permitindo inspecionar o acabamento e os detalhes do verso da carta sem descontar moedas.
  - **Descoberta de Itens**: Celebração especial com aviso de descoberta ao desbloquear novos cosméticos.
- **Economia e Monetização Integrada**:
  - Sistema de moedas persistente (10 moedas por vitória de nível).
  - Anúncio em vídeo recompensado oficial Google AdMob (3 moedas por vídeo assistido com botão dedicado).
  - Anúncios intersticiais não-intrusivos apenas na tela de vitória.
  - Funcionamento 100% offline preservado: se não houver internet, a partida continua normalmente sem bloquear o jogador.

---

## 🛠️ Tecnologias e Especificações

- **Linguagem**: Kotlin 1.9.22
- **Android Gradle Plugin (AGP)**: 8.2.2
- **Gradle**: 8.2
- **Compile SDK / Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0 Nougat — compatibilidade com 99%+ dos aparelhos)
- **Renderização**: Canvas nativo do Android (`Custom View`) com aceleração gráfica por hardware
- **Persistência**: `SharedPreferences` + JSON nativo
- **Orientação**: Retrato (`portrait`)

---

## 📁 Estrutura do Projeto

```
PacienciaSolitaire/
├── build.gradle.kts                     # Configuração raiz do Gradle
├── settings.gradle.kts                   # Módulos e repositórios
├── gradle.properties                     # Propriedades da JVM e AndroidX
├── gradle/wrapper/                       # Gradle Wrapper 8.2
├── .github/workflows/android.yml         # Compilação e geração automática do APK na nuvem
├── app/
│   ├── build.gradle.kts                 # Configuração do módulo app e dependências (AdMob)
│   ├── proguard-rules.pro               # Regras Proguard
│   └── src/main/
│       ├── AndroidManifest.xml          # Manifesto do aplicativo com permissões e App ID
│       ├── res/
│       │   ├── values/                  # Strings (PT-BR), Cores e Temas
│       │   ├── drawable/                # Ícones vetoriais e fundos de botões/diálogos
│       │   ├── mipmap-anydpi-v26/       # Ícones adaptativos do aplicativo
│       │   └── layout/                  # Layouts XML da tela principal e diálogos da loja e vitória
│       └── java/com/paciencia/solitaire/
│           ├── MainActivity.kt          # Orquestrador da UI, timers, loja e ciclo de vida
│           ├── ads/                     # Integração oficial com SDK Google AdMob (Intersticial e Recompensado)
│           ├── customization/           # Modelos de CardSkin, SkinPattern e Avatar com coleções
│           ├── model/                   # Modelos: Card, Suit, Rank, CardPile, PileType, MoveAction
│           ├── engine/                  # Motor de regras do Klondike e embaralhamento determinístico
│           ├── scenery/                 # Sistema de partículas e os 5 cenários temáticos vivos
│           │   └── themes/              # Implementações vetoriais dos 5 temas
│           ├── data/                    # Economia de moedas, progresso, estatísticas e serialização
│           └── ui/                      # SolitaireView, CardRenderer, SkinPreviewView e SoundFeedback
```

---

## 🚀 Como Abrir e Executar no Android Studio

1. Abra o **Android Studio** (versão Flamingo, Hedgehog, Iguana, Jellyfish ou Koala).
2. Na tela inicial, clique em **Open** (ou no menu `File -> Open`).
3. Navegue até o diretório `PacienciaSolitaire` e selecione a pasta raiz.
4. Aguarde a sincronização do Gradle (*Gradle Sync*). O Android Studio baixará automaticamente as dependências configuradas.
5. Conecte um dispositivo Android via USB com depuração ativada ou inicie um Emulador (*Android Virtual Device*).
6. Clique no botão **Run** (ícone de play verde ▶) ou pressione `Shift + F10`.

---

## 📦 Como Gerar o APK Localmente

### Gerar APK de Depuração (Debug APK):
No terminal da raiz do projeto:
```bash
./gradlew assembleDebug
```
O arquivo APK gerado estará em:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testes Unitários Automatizados

O projeto conta com suíte de testes unitários para o motor Klondike e economia (`KlondikeEngineTest.kt` e `GameEconomyAndCustomizationTest.kt`), validando:
- Integridade estrita das 52 cartas (sem duplicações ou perdas).
- Distribuição inicial das 7 colunas em cascata, monte e descarte.
- Regras de movimentação (alternância de cores, ordem decrescente, Rei em coluna vazia).
- Regras de fundação (mesmo naipe, ordem ascendente do Ás ao Rei).
- Ciclo completo de compra e reciclagem do monte.
- Conservação de cartas no desfazer (*Undo*).
- Condição de vitória com todas as fundações completas.
- Economia de moedas: concessão única por vitória, recompensa por anúncio, integridade do catálogo e verificação de coleções de skins e avatares.

Para executar os testes:
```bash
./gradlew testDebugUnitTest --stacktrace
```

---

## 🛠️ Registro de Atualizações (Changelog v1.2.0 — Upgrade Visual Premium)

- **Galeria de Itens Colecionáveis**: Introdução de `SkinPreviewView` para renderização ampliada e em tempo real do verso da carta e dos avatares na loja, organização por coleções temáticas (Tradição, Relíquias Douradas, Arcano, Sombras, Natureza, Gelo, Cosmos e Realeza) e fluxo de descoberta de novo cosmético.
- **Upgrades Cinematográficos nos 5 Cenários**:
  - *Floresta Encantada*: Vaga-lumes bioluminescentes vivos com pulsação suave, raios solares dinâmicos filtrados por copas e névoa terrena.
  - *Montanhas Geladas*: Cordilheira ártica em planos de profundidade recortados, reflexos azulados glaciais e aurora boreal mística.
  - *Deserto Dourado*: Dunas onduladas com cristas iluminadas em ouro e miragem solar pulsante.
  - *Reino Medieval*: Torres góticas com ameias, flâmulas ao vento, janelas aconchegantes iluminadas por velas e tochas crepitantes com halos dinâmicos.
  - *Noite Cósmica*: Nebulosas multicamadas com pulsação celeste suave e constelações cintilantes de múltiplas magnitudes.
- **Animações de Jogabilidade**:
  - Realce verde-esmeralda em destinos válidos para as cartas arrastadas.
  - Resposta visual com vibração suave (*shake*) e flash carmesim para jogadas inválidas.
  - Efeito de brilho de faíscas ao revelar cartas ocultas e ao mover cartas para as fundações.
  - Cascata contínua de estrelas comemorativas na tela após a vitória da partida.
- **Estabilidade e Correção de Referências**: Proteção estrita do `firstCard` no tap-to-move e blindagem do desempilhamento em toques sucessivos.
