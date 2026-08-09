@echo off
setlocal
python "%~dp0..\run.py" --scope audio_cli %*
exit /b %ERRORLEVEL%
