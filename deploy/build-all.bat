@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0.."
set "SERVICES=eureka-server config-server gateway-service login-service user-service crm-service"

echo ========================================
echo  MSA JAR Build
echo  local dev  - profile: local
echo  server dep - profile: prod
echo ========================================

set "FAILED=0"
for %%S in (%SERVICES%) do (
    echo.
    echo [BUILD] %%S
    pushd "%ROOT%\%%S" || (
        echo [FAIL] directory not found: %%S
        set "FAILED=1"
        goto :done
    )
    call gradlew.bat bootJar -x test
    if errorlevel 1 (
        echo [FAIL] %%S
        set "FAILED=1"
        popd
        goto :done
    )
    popd
    echo [OK] %%S ^> build\libs\*.jar
)

:done
echo.
if "%FAILED%"=="1" (
    echo ========================================
    echo  BUILD FAILED
    echo ========================================
    exit /b 1
)

echo ========================================
echo  BUILD SUCCESS
echo  Next: deploy\copy-jars.ps1
echo ========================================
endlocal
