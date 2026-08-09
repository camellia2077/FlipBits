@echo off
setlocal
python "%~dp0run.py" --lang py --dir-over-files --dir-max-depth 2 %*
exit /b %ERRORLEVEL%
