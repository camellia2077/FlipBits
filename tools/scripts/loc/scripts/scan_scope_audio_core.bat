@echo off
setlocal
python "%~dp0..\run.py" --scope audio_core %*
exit /b %ERRORLEVEL%
