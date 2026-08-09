@echo off
setlocal
python "%~dp0run.py" --lang cpp %*
exit /b %ERRORLEVEL%
