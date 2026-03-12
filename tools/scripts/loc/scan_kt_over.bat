@echo off
setlocal
python "%~dp0run.py" --lang kt %*
exit /b %ERRORLEVEL%
