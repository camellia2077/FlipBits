@echo off
setlocal
py "%~dp0run.py" replace --auto-fix-json %*
