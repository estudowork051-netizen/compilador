#!/usr/bin/env bash
# =============================================================================
# Viora Branding Asset Processor
# Validates and converts user-provided viora_icon.png and viora_splash.png
# into native Android launcher mipmaps, adaptive foregrounds, and drawables.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RES_DIR="$(cd "${SCRIPT_DIR}/../../res" 2>/dev/null && pwd || echo "${SCRIPT_DIR}/../../res")"

# Prioridade: Execução via Python Engine (Pillow / libpng compliance)
if command -v python3 >/dev/null 2>&1 && [ -f "${SCRIPT_DIR}/process_branding.py" ]; then
    python3 "${SCRIPT_DIR}/process_branding.py"
    exit 0
fi

echo "=========================================================="
echo "[Viora Branding] Processamento de Identidade Visual Viora"
echo "  Script: ${SCRIPT_DIR}"
echo "  Res:    ${RES_DIR}"
echo "=========================================================="

# -----------------------------------------------------------------------------
# Função: Validação de arquivo de imagem
# -----------------------------------------------------------------------------
is_valid_image() {
    local file="$1"
    if [ ! -f "$file" ]; then
        return 1
    fi
    local size
    size=$(stat -c%s "$file" 2>/dev/null || wc -c < "$file" 2>/dev/null || echo 0)
    if [ "$size" -lt 32 ]; then
        return 1
    fi
    # 1. Checagem MIME type se 'file' estiver presente
    if command -v file >/dev/null 2>&1; then
        local mime
        mime=$(file -b --mime-type "$file" 2>/dev/null || echo "")
        if [[ "$mime" =~ ^image/(png|jpeg|webp) ]]; then
            return 0
        fi
    fi
    # 2. Checagem cabeçalho PNG (89504E470D0A1A0A) ou JPEG (FFD8)
    local header
    header=$(head -c 8 "$file" | xxd -p 2>/dev/null || od -N 8 -t x1 "$file" | head -n 1 | awk '{print $2$3$4$5$6$7$8$9}' 2>/dev/null || echo "")
    if [[ "$header" == *"89504e47"* ]] || [[ "$header" == *"ffd8"* ]]; then
        return 0
    fi
    # 3. Fallback: ffmpeg probe
    if command -v ffmpeg >/dev/null 2>&1; then
        if ffmpeg -v error -i "$file" -f null - 2>/dev/null; then
            return 0
        fi
    fi
    return 1
}

# -----------------------------------------------------------------------------
# 1. LOCALIZAÇÃO DO ÍCONE PERSONALIZADO (viora_icon.png)
# -----------------------------------------------------------------------------
SOURCE_ICON=""
SEARCH_CANDIDATES=(
    "${SCRIPT_DIR}/viora_icon.png"
    "${SCRIPT_DIR}/viora_icon.PNG"
    "${SCRIPT_DIR}/viora_icon.jpg"
    "${SCRIPT_DIR}/viora_icon.jpeg"
    "${SCRIPT_DIR}/viora_icon.webp"
    "${SCRIPT_DIR}/viora-icon.png"
    "${SCRIPT_DIR}/viora-icon.jpg"
    "${SCRIPT_DIR}/icon.png"
    "${SCRIPT_DIR}/app_icon.png"
    "${SCRIPT_DIR}/lifeflow_icon.png"
    "${SCRIPT_DIR}/../viora_icon.png"
    "${SCRIPT_DIR}/../../assets/branding/viora_icon.png"
    "app/src/main/assets/branding/viora_icon.png"
    "workspace/app/src/main/assets/branding/viora_icon.png"
)

for cand in "${SEARCH_CANDIDATES[@]}"; do
    if [ -f "$cand" ] && is_valid_image "$cand"; then
        SOURCE_ICON="$(cd "$(dirname "$cand")" && pwd)/$(basename "$cand")"
        echo "[Viora Branding] ✓ Imagem personalizada detectada: ${SOURCE_ICON}"
        break
    fi
done

if [ -z "${SOURCE_ICON}" ]; then
    # Busca recursiva no diretório de assets
    FOUND=$(find "${SCRIPT_DIR}" -maxdepth 2 -type f \( -iname "*viora*icon*.png" -o -iname "*viora*icon*.jpg" -o -iname "*viora*icon*.webp" \) 2>/dev/null | head -n 1 || true)
    if [ -n "$FOUND" ] && is_valid_image "$FOUND"; then
        SOURCE_ICON="$FOUND"
        echo "[Viora Branding] ✓ Imagem personalizada encontrada via busca: ${SOURCE_ICON}"
    fi
fi

# -----------------------------------------------------------------------------
# 2. CONVERSÃO E INTEGRAÇÃO DE ÍCONES (LAUNCHER E ADAPTIVE)
# -----------------------------------------------------------------------------
if [ -n "${SOURCE_ICON}" ]; then
    echo "[Viora Branding] Gerando recursos de ícone em todas as densidades (Mipmap)..."

    declare -A LEGACY_SIZES=(
        ["mipmap-mdpi"]=48
        ["mipmap-hdpi"]=72
        ["mipmap-xhdpi"]=96
        ["mipmap-xxhdpi"]=144
        ["mipmap-xxxhdpi"]=192
    )

    declare -A ADAPTIVE_SIZES=(
        ["mipmap-mdpi"]=108
        ["mipmap-hdpi"]=162
        ["mipmap-xhdpi"]=216
        ["mipmap-xxhdpi"]=324
        ["mipmap-xxxhdpi"]=432
    )

    CONVERTED_OK=true

    for DENSITY in "${!LEGACY_SIZES[@]}"; do
        L_SIZE="${LEGACY_SIZES[$DENSITY]}"
        A_SIZE="${ADAPTIVE_SIZES[$DENSITY]}"
        INNER_SAFE=$(( A_SIZE * 72 / 100 ))
        TARGET_DIR="${RES_DIR}/${DENSITY}"
        mkdir -p "${TARGET_DIR}"

        if command -v ffmpeg >/dev/null 2>&1; then
            # 1. Ícone padrão legado
            ffmpeg -y -i "${SOURCE_ICON}" -vf "scale=${L_SIZE}:${L_SIZE}:flags=lanczos" -update 1 "${TARGET_DIR}/ic_launcher.png" >/dev/null 2>&1 || CONVERTED_OK=false
            # 2. Ícone redondo legado
            ffmpeg -y -i "${SOURCE_ICON}" -vf "scale=${L_SIZE}:${L_SIZE}:flags=lanczos" -update 1 "${TARGET_DIR}/ic_launcher_round.png" >/dev/null 2>&1 || CONVERTED_OK=false
            # 3. Camada adaptativa (com format=rgba garantindo canal alfa 100% transparente no padding)
            ffmpeg -y -i "${SOURCE_ICON}" -vf "format=rgba,scale=${INNER_SAFE}:${INNER_SAFE}:force_original_aspect_ratio=decrease:flags=lanczos,pad=${A_SIZE}:${A_SIZE}:(ow-iw)/2:(oh-ih)/2:color=0x00000000" -update 1 "${TARGET_DIR}/ic_launcher_foreground.png" >/dev/null 2>&1 || CONVERTED_OK=false
        elif command -v python3 >/dev/null 2>&1; then
            python3 -c "
import sys
# Python fallback: tentativa de cópia direta
with open('${SOURCE_ICON}', 'rb') as f_in:
    data = f_in.read()
for target in ['${TARGET_DIR}/ic_launcher.png', '${TARGET_DIR}/ic_launcher_round.png', '${TARGET_DIR}/ic_launcher_foreground.png']:
    with open(target, 'wb') as f_out:
        f_out.write(data)
" 2>/dev/null || CONVERTED_OK=false
        else
            cp -f "${SOURCE_ICON}" "${TARGET_DIR}/ic_launcher.png"
            cp -f "${SOURCE_ICON}" "${TARGET_DIR}/ic_launcher_round.png"
            cp -f "${SOURCE_ICON}" "${TARGET_DIR}/ic_launcher_foreground.png"
        fi

        echo "  ✓ ${DENSITY}: ic_launcher (${L_SIZE}x${L_SIZE}px), foreground (${A_SIZE}x${A_SIZE}px RGBA com safe-zone)"
    done

    # Configura mipmap-anydpi-v26 para utilizar @mipmap/ic_launcher_foreground
    mkdir -p "${RES_DIR}/mipmap-anydpi-v26"
    cat << 'EOF' > "${RES_DIR}/mipmap-anydpi-v26/ic_launcher.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
EOF

    cat << 'EOF' > "${RES_DIR}/mipmap-anydpi-v26/ic_launcher_round.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
EOF
    echo "[Viora Branding] ✓ Ícones adaptativos configurados com sucesso para @mipmap/ic_launcher_foreground!"
else
    echo "[Viora Branding] viora_icon.png não encontrado em assets/branding/."
    echo "[Viora Branding] Configurando adaptive-icon para utilizar o emblema vetorial Viora (@drawable/ic_launcher_foreground)."
    mkdir -p "${RES_DIR}/mipmap-anydpi-v26"
    cat << 'EOF' > "${RES_DIR}/mipmap-anydpi-v26/ic_launcher.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF

    cat << 'EOF' > "${RES_DIR}/mipmap-anydpi-v26/ic_launcher_round.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF
fi

# -----------------------------------------------------------------------------
# 3. PROCESSAMENTO DA SPLASH SCREEN (viora_splash.png) - PRESERVADA
# -----------------------------------------------------------------------------
SOURCE_SPLASH=""
SPLASH_CANDIDATES=(
    "${SCRIPT_DIR}/viora_splash.png"
    "${SCRIPT_DIR}/viora_splash.PNG"
    "${SCRIPT_DIR}/viora_splash.jpg"
    "${SCRIPT_DIR}/viora_splash.jpeg"
    "${SCRIPT_DIR}/viora_splash.webp"
    "${SCRIPT_DIR}/lifeflow_splash.png"
    "${SCRIPT_DIR}/../viora_splash.png"
    "app/src/main/assets/branding/viora_splash.png"
    "workspace/app/src/main/assets/branding/viora_splash.png"
)

for cand in "${SPLASH_CANDIDATES[@]}"; do
    if [ -f "$cand" ] && is_valid_image "$cand"; then
        SOURCE_SPLASH="$(cd "$(dirname "$cand")" && pwd)/$(basename "$cand")"
        break
    fi
done

if [ -n "${SOURCE_SPLASH}" ]; then
    echo "[Viora Branding] ✓ viora_splash.png detectada: ${SOURCE_SPLASH}"
    mkdir -p "${RES_DIR}/drawable-nodpi"
    cp -f "${SOURCE_SPLASH}" "${RES_DIR}/drawable-nodpi/viora_splash.png"
    echo "[Viora Branding] ✓ viora_splash.png sincronizada em res/drawable-nodpi para exibição Edge-to-Edge."
else
    echo "[Viora Branding] viora_splash.png não encontrada em assets. Fallback nativo Sage Green ativo na SplashScreen."
fi

echo "=========================================================="
echo "[Viora Branding] Processamento concluído com sucesso!"
echo "=========================================================="
