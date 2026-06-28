@echo off
cls
echo ====================================================
echo   Starting MSA Local Development Automatically...
echo ====================================================
echo.

REM [1] Eureka Server
echo [1/6] Starting eureka-server (Port: 8761)...
start "[1] Eureka Server" cmd /k "cd /d %~dp0..\eureka-server && gradlew.bat bootRun"
timeout /t 5 > nul

REM [2] Config Server
echo [2/6] Starting config-server (Port: 8888)...
start "[2] Config Server" cmd /k "cd /d %~dp0..\config-server && gradlew.bat bootRun"
timeout /t 8 > nul

REM [3] User Service
echo [3/6] Starting user-service (Port: 8082)...
start "[3] User Service" cmd /k "cd /d %~dp0..\user-service && gradlew.bat bootRun"
timeout /t 3 > nul

REM [4] Login Service
echo [4/6] Starting login-service (Port: 8088)...
start "[4] Login Service" cmd /k "cd /d %~dp0..\login-service && gradlew.bat bootRun"
timeout /t 3 > nul

REM [5] CRM Service
echo [5/6] Starting crm-service (Port: 8083)...
start "[5] CRM Service" cmd /k "cd /d %~dp0..\crm-service && gradlew.bat bootRun"
timeout /t 3 > nul

REM [6] Gateway Service
echo [6/6] Starting gateway-service (Port: 8081)...
start "[6] Gateway Service" cmd /k "cd /d %~dp0..\gateway-service && gradlew.bat bootRun"

echo.
echo ====================================================
echo   All boot commands have been sent successfully.
echo   Check each terminal window for build status!
echo ====================================================
pause