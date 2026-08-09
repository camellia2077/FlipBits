@echo off
setlocal
python "%~dp0run.py" --lang kt --responsibility-risk %*
exit /b %ERRORLEVEL%
