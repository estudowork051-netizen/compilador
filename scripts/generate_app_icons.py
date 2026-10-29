#!/usr/bin/env python3
"""
DoHsktoP - Utilitário de Geração de Ícones da Aplicação Android
Redimensiona a imagem mestre em 'android-app/branding/app-icon.png' para todas
as densidades requeridas pelo Android (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
e configura as camadas de ícone adaptativo (API 26+).
"""

import os
import sys
import subprocess

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BRANDING_DIR = os.path.join(PROJECT_ROOT, "android-app", "branding")
MASTER_ICON = os.path.join(BRANDING_DIR, "app-icon.png")
RES_DIR = os.path.join(PROJECT_ROOT, "android-app", "app", "src", "main", "res")

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

ANYDPI_DIR = os.path.join(RES_DIR, "drawable-anydpi-v26")

def ensure_anydpi_xml():
    os.makedirs(ANYDPI_DIR, exist_ok=True)
    
    # ic_launcher.xml para Android 8.0+
    ic_launcher_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(ANYDPI_DIR, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(ic_launcher_xml)

    # ic_launcher_round.xml para Android 8.0+
    ic_launcher_round_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(ANYDPI_DIR, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(ic_launcher_round_xml)
    print("  [OK] Recursos adaptativos criados em res/drawable-anydpi-v26/")

def generate_icons():
    print("=" * 70)
    print("  DOHSKTOP - GERADOR DE ÍCONES OFICIAIS ANDROID")
    print("=" * 70)
    print(f"Diretório master: {BRANDING_DIR}")
    print(f"Arquivo master esperado: {MASTER_ICON}")

    ensure_anydpi_xml()

    if not os.path.isfile(MASTER_ICON):
        print("\n[AVISO] Imagem master 'app-icon.png' ainda não foi inserida pelo usuário.")
        print("  -> O aplicativo continuará compilando normalmente usando os ícones")
        print("     vetoriais Cyber-Zombie / Retro-Tech em res/drawable/ic_launcher.xml")
        print(f"  -> Para utilizar sua arte oficial, copie o PNG para:\n     {MASTER_ICON}")
        print("  -> E em seguida execute novamente este script: python3 scripts/generate_app_icons.py\n")
        return True

    print("\n[INFO] Imagem master encontrada! Gerando densidades mipmap com alta qualidade...")
    for folder, size in DENSITIES.items():
        out_dir = os.path.join(RES_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)

        target_launcher = os.path.join(out_dir, "ic_launcher.png")
        target_round = os.path.join(out_dir, "ic_launcher_round.png")

        # Geração com ffmpeg usando filtro lanczos para máxima nitidez
        cmd_square = [
            "ffmpeg", "-y", "-i", MASTER_ICON,
            "-vf", f"scale={size}:{size}:flags=lanczos",
            target_launcher
        ]
        cmd_round = [
            "ffmpeg", "-y", "-i", MASTER_ICON,
            "-vf", f"scale={size}:{size}:flags=lanczos",
            target_round
        ]
        try:
            subprocess.run(cmd_square, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
            subprocess.run(cmd_round, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
            print(f"  [GERADO] {folder}/ic_launcher.png ({size}x{size})")
        except Exception as e:
            print(f"  [ERRO] Falha ao processar {folder}: {e}")
            return False

    print("\n[SUCESSO] Todos os ícones de densidade gerados com sucesso!")
    return True

if __name__ == "__main__":
    success = generate_icons()
    sys.exit(0 if success else 1)
