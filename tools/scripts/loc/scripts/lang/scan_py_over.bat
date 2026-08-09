@echo off
setlocal
python "%~dp0run.py" --lang py %*
exit /b %ERRORLEVEL%
