@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Nudge Backend

if not exist ".env" (
  echo [ERROR] .env file not found.
  echo Copy .env.example to .env and add your own GEMINI_API_KEY.
  pause
  exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
  if not "%%A"=="" set "%%A=%%B"
)

if not defined GEMINI_API_KEY (
  echo [ERROR] GEMINI_API_KEY is missing in .env
  pause
  exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Java was not found in PATH. Install JDK 21+ first.
  pause
  exit /b 1
)

if not exist "backend\pom.xml" (
  echo [ERROR] backend\pom.xml was not found.
  pause
  exit /b 1
)

cd /d "%~dp0backend"
echo Starting Nudge backend on http://localhost:8080 ...
if exist "mvnw.cmd" (
  call mvnw.cmd spring-boot:run
) else (
  call mvn spring-boot:run
)

pause
