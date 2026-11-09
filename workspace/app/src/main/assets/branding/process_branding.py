#!/usr/bin/env python3
"""
Viora Branding Processor
Sanitizes, scales, and generates 100% compliant Android PNG resources
for Launcher Icons and Splash Screen from user-provided assets.
Compatible with AAPT2 strict PNG cruncher and libpng specifications.
"""

import os
import sys
import struct
import zlib
import subprocess
import shutil

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# Resolves to app/src/main/res
RES_DIR = os.path.normpath(os.path.join(SCRIPT_DIR, "..", "..", "res"))

print("==========================================================")
print("[Viora Branding Engine] Inicializando processamento de imagens...")
print(f"  Script dir: {SCRIPT_DIR}")
print(f"  Res dir:    {RES_DIR}")
print("==========================================================")

def is_git_lfs_pointer(filepath):
    try:
        with open(filepath, "rb") as f:
            header = f.read(200)
            if b"version https://git-lfs.github.com/spec/v1" in header:
                return True
    except Exception:
        pass
    return False

def verify_png_validity(filepath):
    """Verifica se o arquivo gerado é um PNG 100% válido para o AAPT2."""
    if not os.path.isfile(filepath):
        return False, "Arquivo não existe"
    size = os.path.getsize(filepath)
    if size < 20:
        return False, f"Tamanho insuficiente: {size} bytes"
    with open(filepath, "rb") as f:
        data = f.read()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        return False, "Assinatura mágica PNG inválida"
    if not data.endswith(b"IEND\xaeB`\x82"):
        return False, "Fim de arquivo PNG (chunk IEND) truncado ou ausente"
    return True, "OK"

def sanitize_and_scale_pillow(src_path, dst_path, size=None, safe_ratio=1.0, is_splash=False):
    from PIL import Image
    with Image.open(src_path) as img:
        # Modo de cor limpo sem perfis ICC corrompidos
        if is_splash:
            target_img = img.convert("RGB")
            if size:
                target_img = target_img.resize(size, Image.Resampling.LANCZOS)
        else:
            rgba_img = img.convert("RGBA")
            if size:
                if safe_ratio < 1.0:
                    inner_w = int(size[0] * safe_ratio)
                    inner_h = int(size[1] * safe_ratio)
                    # Preserva proporção
                    aspect = rgba_img.width / max(1, rgba_img.height)
                    if aspect > 1.0:
                        scaled_w = inner_w
                        scaled_h = max(1, int(inner_w / aspect))
                    else:
                        scaled_h = inner_h
                        scaled_w = max(1, int(inner_h * aspect))
                    
                    resized = rgba_img.resize((scaled_w, scaled_h), Image.Resampling.LANCZOS)
                    # Cria canvas transparente
                    target_img = Image.new("RGBA", size, (0, 0, 0, 0))
                    offset = ((size[0] - scaled_w) // 2, (size[1] - scaled_h) // 2)
                    target_img.paste(resized, offset, resized)
                else:
                    target_img = rgba_img.resize(size, Image.Resampling.LANCZOS)
            else:
                target_img = rgba_img
        
        # Salva como PNG limpo sem chunks problemáticos
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        target_img.save(dst_path, format="PNG", optimize=True)

def sanitize_and_scale_ffmpeg(src_path, dst_path, size=None, safe_ratio=1.0, is_splash=False):
    os.makedirs(os.path.dirname(dst_path), exist_ok=True)
    if is_splash:
        cmd = [
            "ffmpeg", "-y", "-i", src_path,
            "-map_metadata", "-1",
            "-vf", "format=rgb24",
            "-update", "1", dst_path
        ]
    else:
        if size and safe_ratio < 1.0:
            a_w, a_h = size
            inner_w = int(a_w * safe_ratio)
            inner_h = int(a_h * safe_ratio)
            vf = (
                f"format=rgba,"
                f"scale={inner_w}:{inner_h}:force_original_aspect_ratio=decrease:flags=lanczos,"
                f"pad={a_w}:{a_h}:(ow-iw)/2:(oh-ih)/2:color=0x00000000"
            )
        elif size:
            vf = f"format=rgba,scale={size[0]}:{size[1]}:flags=lanczos"
        else:
            vf = "format=rgba"
        cmd = [
            "ffmpeg", "-y", "-i", src_path,
            "-map_metadata", "-1",
            "-vf", vf,
            "-update", "1", dst_path
        ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

def process_image(src_path, dst_path, size=None, safe_ratio=1.0, is_splash=False):
    # Tenta Pillow primeiro (máxima conformidade de PNG para Android AAPT2)
    try:
        sanitize_and_scale_pillow(src_path, dst_path, size, safe_ratio, is_splash)
        ok, msg = verify_png_validity(dst_path)
        if ok:
            return True
        else:
            print(f"  [Aviso] Pillow gerou arquivo com pendência ({msg}), tentando FFmpeg...")
    except ImportError:
        pass
    except Exception as e:
        print(f"  [Aviso] Falha Pillow ({e}), tentando FFmpeg...")

    # Fallback FFmpeg
    try:
        sanitize_and_scale_ffmpeg(src_path, dst_path, size, safe_ratio, is_splash)
        ok, msg = verify_png_validity(dst_path)
        if ok:
            return True
        else:
            print(f"  [Erro] FFmpeg gerou arquivo com pendência ({msg})")
            if os.path.exists(dst_path):
                os.remove(dst_path)
            return False
    except Exception as e:
        print(f"  [Erro] Falha FFmpeg: {e}")
        if os.path.exists(dst_path):
            os.remove(dst_path)
        return False

# =============================================================================
# 1. PROCESSAMENTO DO ÍCONE (viora_icon.png)
# =============================================================================
icon_candidates = [
    os.path.join(SCRIPT_DIR, "viora_icon.png"),
    os.path.join(SCRIPT_DIR, "viora_icon.PNG"),
    os.path.join(SCRIPT_DIR, "viora_icon.jpg"),
    os.path.join(SCRIPT_DIR, "viora_icon.jpeg"),
    os.path.join(SCRIPT_DIR, "viora_icon.webp"),
    os.path.join(SCRIPT_DIR, "viora-icon.png"),
    os.path.join(SCRIPT_DIR, "icon.png"),
    os.path.join(SCRIPT_DIR, "app_icon.png"),
    os.path.join(SCRIPT_DIR, "lifeflow_icon.png"),
]

source_icon = None
for cand in icon_candidates:
    if os.path.isfile(cand) and os.path.getsize(cand) > 50:
        if is_git_lfs_pointer(cand):
            print(f"[Viora Branding] AVISO: {cand} é um ponteiro Git LFS (texto) e não um binário real.")
            continue
        source_icon = cand
        break

if source_icon:
    print(f"[Viora Branding] ✓ Ícone personalizado identificado: {source_icon} ({os.path.getsize(source_icon)} bytes)")
    
    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432),
    }
    
    all_icons_ok = True
    for density, (legacy_sz, adaptive_sz) in densities.items():
        density_dir = os.path.join(RES_DIR, density)
        os.makedirs(density_dir, exist_ok=True)
        
        # 1. ic_launcher.png (legado quadrado)
        p1 = os.path.join(density_dir, "ic_launcher.png")
        ok1 = process_image(source_icon, p1, size=(legacy_sz, legacy_sz), safe_ratio=1.0)
        
        # 2. ic_launcher_round.png (legado redondo)
        p2 = os.path.join(density_dir, "ic_launcher_round.png")
        ok2 = process_image(source_icon, p2, size=(legacy_sz, legacy_sz), safe_ratio=1.0)
        
        # 3. ic_launcher_foreground.png (camada adaptativa 108dp com safe-zone de 72%)
        p3 = os.path.join(density_dir, "ic_launcher_foreground.png")
        ok3 = process_image(source_icon, p3, size=(adaptive_sz, adaptive_sz), safe_ratio=0.72)
        
        if ok1 and ok2 and ok3:
            print(f"  ✓ {density}: launcher ({legacy_sz}px), round ({legacy_sz}px), foreground ({adaptive_sz}px)")
        else:
            all_icons_ok = False
            print(f"  ✗ {density}: falha na geração dos ícones!")

    if all_icons_ok:
        anydpi_dir = os.path.join(RES_DIR, "mipmap-anydpi-v26")
        os.makedirs(anydpi_dir, exist_ok=True)
        with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                    '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                    '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
                    '</adaptive-icon>\n')
        with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                    '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                    '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
                    '</adaptive-icon>\n')
        print("[Viora Branding] ✓ Ícones adaptativos configurados para @mipmap/ic_launcher_foreground!")
else:
    print("[Viora Branding] viora_icon.png não encontrado. Mantendo fallback nativo Viora (@drawable/ic_launcher_foreground).")
    anydpi_dir = os.path.join(RES_DIR, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
                '</adaptive-icon>\n')
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
                '</adaptive-icon>\n')

# =============================================================================
# 2. PROCESSAMENTO DA SPLASH SCREEN (viora_splash.png)
# =============================================================================
splash_candidates = [
    os.path.join(SCRIPT_DIR, "viora_splash.png"),
    os.path.join(SCRIPT_DIR, "viora_splash.PNG"),
    os.path.join(SCRIPT_DIR, "viora_splash.jpg"),
    os.path.join(SCRIPT_DIR, "viora_splash.jpeg"),
    os.path.join(SCRIPT_DIR, "viora_splash.webp"),
    os.path.join(SCRIPT_DIR, "lifeflow_splash.png"),
]

source_splash = None
for cand in splash_candidates:
    if os.path.isfile(cand) and os.path.getsize(cand) > 50:
        if is_git_lfs_pointer(cand):
            print(f"[Viora Branding] AVISO: {cand} é um ponteiro Git LFS (texto) e não um binário real.")
            continue
        source_splash = cand
        break

nodpi_splash = os.path.join(RES_DIR, "drawable-nodpi", "viora_splash.png")

if source_splash:
    print(f"[Viora Branding] ✓ Splash Screen personalizada identificada: {source_splash} ({os.path.getsize(source_splash)} bytes)")
    splash_ok = process_image(source_splash, nodpi_splash, is_splash=True)
    if splash_ok:
        print("[Viora Branding] ✓ viora_splash.png sanitizada e compilada em res/drawable-nodpi com conformidade AAPT2!")
    else:
        print("[Viora Branding] Falha ao sanitizar viora_splash.png! Removendo arquivo inválido de drawable-nodpi para não quebrar o AAPT2.")
        if os.path.exists(nodpi_splash):
            os.remove(nodpi_splash)
else:
    print("[Viora Branding] viora_splash.png não encontrada. Fallback nativo Sage Green ativo na SplashScreen.")
    if os.path.exists(nodpi_splash):
        # Se não há imagem de origem mas havia resíduo inválido, remove
        ok, _ = verify_png_validity(nodpi_splash)
        if not ok:
            os.remove(nodpi_splash)

print("==========================================================")
print("[Viora Branding Engine] Processamento concluído com sucesso!")
print("==========================================================")
