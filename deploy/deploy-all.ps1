#Requires -Version 5.1
<#
.SYNOPSIS
  빌드 → config 동기화 → JAR 복사 → (선택) 서버 재시작 을 한 번에 실행합니다.

.EXAMPLE
  .\deploy-all.ps1
  .\deploy-all.ps1 -SkipBuild
  .\deploy-all.ps1 -RestartRemote -DeployMode recreate
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$RestartRemote,
    [ValidateSet("restart", "recreate")]
    [string]$DeployMode = "restart"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "deploy.settings.ps1")

$remote = "$DeployServerUser@$DeployServerHost"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " MSA Full Deploy Pipeline" -ForegroundColor Cyan
Write-Host " Target: $remote" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "[STEP 1/3] Build JARs..." -ForegroundColor Yellow
    $buildScript = Join-Path $PSScriptRoot "build-all.bat"
    & cmd.exe /c "`"$buildScript`""
    if ($LASTEXITCODE -ne 0) { throw "build-all.bat 실패" }
} else {
    Write-Host ""
    Write-Host "[STEP 1/3] Build skipped" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "[STEP 2/3] Sync config-repo..." -ForegroundColor Yellow
& (Join-Path $PSScriptRoot "sync-config.ps1")

Write-Host ""
Write-Host "[STEP 3/3] Copy JARs..." -ForegroundColor Yellow
& (Join-Path $PSScriptRoot "copy-jars.ps1")

if ($RestartRemote) {
    Write-Host ""
    Write-Host "[STEP 4] Remote restart (DEPLOY_MODE=$DeployMode)..." -ForegroundColor Yellow
    $remoteScript = "/home/cwuser/msa/deploy/server-restart-services.sh"
    ssh $remote "DEPLOY_MODE=$DeployMode bash $remoteScript"
    if ($LASTEXITCODE -ne 0) { throw "원격 재시작 실패" }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " Deploy pipeline complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

if (-not $RestartRemote) {
    Write-Host ""
    Write-Host "서버 재시작:" -ForegroundColor Yellow
    Write-Host "  ssh $remote"
    Write-Host "  DEPLOY_MODE=$DeployMode bash /home/cwuser/msa/deploy/server-restart-services.sh"
}
