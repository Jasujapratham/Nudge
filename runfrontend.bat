@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Nudge Frontend

where node >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js was not found in PATH. Install Node.js 18+ first.
  pause
  exit /b 1
)

if not exist "frontend\package.json" (
  echo [ERROR] frontend\package.json was not found.
  pause
  exit /b 1
)

cd /d "%~dp0frontend"
if not exist "node_modules" (
  echo Installing frontend dependencies...
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed.
    pause
    exit /b 1
  )
)

echo Starting Nudge frontend...
call npm run dev
pause
