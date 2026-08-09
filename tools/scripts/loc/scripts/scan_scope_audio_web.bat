@echo off
setlocal
python "%~dp0..\run.py" --scope audio_web %*
exit /b %ERRORLEVEL%
