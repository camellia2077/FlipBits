@echo off
setlocal
python "%~dp0..\run.py" --scope audio_runtime %*
exit /b %ERRORLEVEL%
