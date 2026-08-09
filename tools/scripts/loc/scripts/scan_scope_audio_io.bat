@echo off
setlocal
python "%~dp0..\run.py" --scope audio_io %*
exit /b %ERRORLEVEL%
