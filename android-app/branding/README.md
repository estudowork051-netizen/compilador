# DoHsktoP — Diretório de Entrada do Ícone Oficial da Aplicação

## Localização Canônica da Imagem Master

Este é o local oficial e exclusivo para o fornecimento do ícone mestre do **DoHsktoP**:

- **Caminho relativo à raiz do repositório:** `android-app/branding/app-icon.png`
- **Nome exato do arquivo:** `app-icon.png`
- **Formato aceito:** PNG (preferencialmente PNG com canal alfa/transparência)
- **Resolução recomendada:** `1024 × 1024` pixels (composição quadrada, mantendo os elementos importantes na área segura central de 66% a 72% para corte circular/adaptativo do Android).

---

## Como Incorporar ou Substituir o Ícone Futuramente

1. Salve ou copie a sua imagem na resolução de 1024×1024 substituindo o arquivo:
   ```bash
   android-app/branding/app-icon.png
   ```

2. Execute o gerador automático de recursos de densidade (`mipmap-*` e adaptativos):
   ```bash
   python3 scripts/generate_app_icons.py
   ```
   *O script redimensiona automaticamente a imagem master nas densidades `mdpi` (48x48), `hdpi` (72x72), `xhdpi` (96x96), `xxhdpi` (144x144) e `xxxhdpi` (192x192), além das camadas adaptativas de primeiro e segundo plano.*

3. Execute o script de compilação ou construa o APK com o Gradle:
   ```bash
   python3 scripts/build_runner.py android
   ```
   ou no terminal Android:
   ```bash
   cd android-app && ./gradlew assembleDebug
   ```

4. O novo APK conterá todos os recursos mipmap e adaptativos compilados.
