@echo off 
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "SERVICE_DIR=%PROJECT_ROOT%\crm-service"
set "SERVICE_NAME=crm-service"
set "IMAGE_NAME=crm-service:latest"
set "TAR_NAME=crm-service-image.tar"

set "SERVER_HOST=192.168.55.223"
set "SERVER_USER=cwuser"
set "REMOTE_DEPLOY_DIR=/home/cwuser/msa/deploy"
set "DOCKER_NETWORK=msa-net"
set "SPRING_PROFILE=prod"
set "HOST_PORT=8084"
set "CONTAINER_PORT=8084"
set "SKIP_BUILD=0"

:parse_args
if "%~1" == "" goto args_done
if /I "%~1" == "--skip-build" set "SKIP_BUILD=1"
if /I "%~1" == "--port" (
    set "HOST_PORT=%~2"
    set "CONTAINER_PORT=%~2"
    shift
)
shift
goto parse_args
:args_done

echo.
echo ========================================
echo  CRM Service Docker Deploy
echo ========================================
echo  Server : %SERVER_USER%@%SERVER_HOST%
echo  Port   : %HOST_PORT%
echo  Profile: %SPRING_PROFILE%
echo ========================================
echo.

where ssh >nul 2>&1 || (
    echo [ERROR] ssh command not found.
    exit /b 1
)
where scp >nul 2>&1 || (
    echo [ERROR] scp command not found.
    exit /b 1
)
where docker >nul 2>&1 || (
    echo [ERROR] docker command not found.
    exit /b 1
)

if "%SKIP_BUILD%" == "0" (
    echo [1/4] Building Gradle bootJar...
    pushd "%SERVICE_DIR%"
    call gradlew.bat clean bootJar
    if errorlevel 1 (
        echo [ERROR] Gradle build failed.
        popd
        exit /b 1
    )
    popd
    echo [OK] Build completed.
) else (
    echo [1/4] Skipping build (--skip-build)
)

echo.
echo [2/4] Building Docker image...
pushd "%SERVICE_DIR%"
docker build -t %IMAGE_NAME% .
if errorlevel 1 (
    echo [ERROR] Docker image build failed.
    popd
    exit /b 1
)
popd
echo [OK] Image build completed: %IMAGE_NAME%

echo.
echo [3/4] Transferring deploy script and image...
ssh %SERVER_USER%@%SERVER_HOST% "mkdir -p %REMOTE_DEPLOY_DIR%"
scp "%PROJECT_ROOT%\deploy\deploy-crm-remote.sh" %SERVER_USER%@%SERVER_HOST%:%REMOTE_DEPLOY_DIR%/
if errorlevel 1 (
    echo [ERROR] Remote script upload failed.
    exit /b 1
)
ssh %SERVER_USER%@%SERVER_HOST% "chmod +x %REMOTE_DEPLOY_DIR%/deploy-crm-remote.sh"

docker save -o "%SERVICE_DIR%\%TAR_NAME%" %IMAGE_NAME%
if errorlevel 1 (
    echo [ERROR] docker save failed.
    exit /b 1
)

scp "%SERVICE_DIR%\%TAR_NAME%" %SERVER_USER%@%SERVER_HOST%:%REMOTE_DEPLOY_DIR%/
if errorlevel 1 (
    echo [ERROR] scp transfer failed.
    exit /b 1
)
del /f /q "%SERVICE_DIR%\%TAR_NAME%" >nul 2>&1
echo [OK] Image transfer completed.

echo.
echo [4/4] Regenerating remote container...
ssh %SERVER_USER%@%SERVER_HOST% "bash %REMOTE_DEPLOY_DIR%/deploy-crm-remote.sh %HOST_PORT% %CONTAINER_PORT% %SPRING_PROFILE% %DOCKER_NETWORK%"
if errorlevel 1 (
    echo [ERROR] Remote deploy failed.
    exit /b 1
)

echo.
echo ========================================
echo  DEPLOY COMPLETED
echo ========================================
echo  Gateway CRM : http://%SERVER_HOST%:8081/crm-service/
echo  Direct API  : http://%SERVER_HOST%:%HOST_PORT%/leads
echo  Deal Stats  : http://%SERVER_HOST%:%HOST_PORT%/deals/dashboard/stats
echo ========================================
echo.

endlocal