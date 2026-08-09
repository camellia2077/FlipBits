@echo off
setlocal
python "%~dp0..\run.py" --scope audio_api %*
exit /b %ERRORLEVEL%
