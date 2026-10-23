# Paciência Solitaire (Klondike) para Android

Jogo nativo completo de cartas **Paciência (Klondike Solitaire)** para Android, desenvolvido em **Kotlin**, com sistema de progressão por níveis (1 a 50+), 5 cenários visuais desbloqueáveis, controles híbridos (Arrastar e Soltar + Toque Inteligente), efeitos ambientais leves em Canvas e persistência local completa.

---

## 🃏 Visão Geral do Jogo

- **Mecânica Clássica**: Baralho tradicional de 52 cartas sem coringas, 7 colunas em cascata, monte de compra (Stock), descarte (Waste) e 4 fundações (do Ás ao Rei por naipe).
- **Progressão por Níveis**: Mais de 50 níveis com sementes determinísticas para desafios equilibrados e reprodutíveis.
- **5 Cenários Desbloqueáveis**:
  1. **Floresta Encantada** (*Níveis 1 a 10*): Atmosfera mística esmeralda com raios de sol, folhas e pólen flutuante.
  2. **Montanhas Geladas** (*Níveis 11 a 20*): Picos nevados, névoa ártica e flocos de neve caindo suavemente.
  3. **Deserto Dourado** (*Níveis 21 a 30*): Dunas douradas ao pôr do sol com brisa e partículas de areia.
  4. **Reino Medieval** (*Níveis 31 a 40*): Castelo fortificado crepuscular, ameias e fagulhas de tochas incandescentes.
  5. **Noite Cósmica** (*Níveis 41 em diante*): Nebulosa interestelar, estrelas cintilantes e poeira cósmica.
- **Controles Híbridos**:
  - *Drag & Drop*: Arraste de cartas individuais ou sequências inteiras com sombras dinâmicas.
  - *Tap-to-Move*: Toque simples para movimentação automática prioritária para fundações ou colunas válidas.
- **Recursos Auxiliares**:
  - Desfazer movimentos ilimitados (*Undo*).
  - Auto-completar inteligente quando a partida estiver solucionada.
  - Cronômetro e contador de jogadas.
  - Persistência contínua: retome a partida de onde parou mesmo se fechar o aplicativo.
  - Modo Economia de Bateria e controle de vibração/efeitos.

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
├── app/
│   ├── build.gradle.kts                 # Configuração do módulo app
│   ├── proguard-rules.pro               # Regras Proguard
│   └── src/main/
│       ├── AndroidManifest.xml          # Manifesto do aplicativo
│       ├── res/
│       │   ├── values/                  # Strings (PT-BR), Cores e Temas
│       │   ├── drawable/                # Ícones vetoriais e fundos de botões/diálogos
│       │   ├── mipmap-anydpi-v26/       # Ícones adaptativos do aplicativo
│       │   └── layout/                  # Layouts XML da tela principal e diálogos
│       └── java/com/paciencia/solitaire/
│           ├── MainActivity.kt          # Orquestrador principal da UI e ciclo de vida
│           ├── model/                   # Modelos: Card, Suit, Rank, CardPile, PileType, MoveAction
│           ├── engine/                  # Motor de regras do Klondike e embaralhamento
│           ├── scenery/                 # Sistema de partículas e os 5 cenários temáticos
│           ├── data/                    # Persistência de níveis, preferências e estado da partida
│           └── ui/                      # Renderizador vetorial de cartas, Custom View e feedback háptico
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

## 📦 Como Gerar o APK

### Gerar APK de Depuração (Debug APK):
No terminal do Android Studio ou na linha de comando:
```bash
./gradlew assembleDebug
```
O arquivo APK gerado estará em:
`app/build/outputs/apk/debug/app-debug.apk`

### Gerar APK de Produção (Release APK):
```bash
./gradlew assembleRelease
```
O arquivo APK gerado estará em:
`app/build/outputs/apk/release/app-release-unsigned.apk`
