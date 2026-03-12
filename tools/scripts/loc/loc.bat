@echo off
setlocal
python "%~dp0run.py" %*
exit /b %ERRORLEVEL%
