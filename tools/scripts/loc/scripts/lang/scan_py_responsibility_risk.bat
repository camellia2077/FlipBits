@echo off
setlocal
python "%~dp0run.py" --lang py --responsibility-risk %*
exit /b %ERRORLEVEL%
