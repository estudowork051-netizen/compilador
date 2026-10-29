@echo off
rem Script de conveniência para compilar e gerar logs de todos os componentes no Windows
set SCRIPT_DIR=%~dp0
python "%SCRIPT_DIR%build_runner.py" --all
pause
