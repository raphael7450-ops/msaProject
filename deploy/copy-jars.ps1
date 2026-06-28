#Requires -Version 5.1
<#
.SYNOPSIS
  Copies built boot JARs to the remote Docker server.
.DESCRIPTION
  Each module build/libs/*.jar (except -plain) -> cwuser@192.168.55.223:/home/cwuser/msa/jars/<service>.jar
#>
[CmdletBinding()]
param(
    [string]$ServerHost,
    [string]$ServerUser,
    [string]$RemotePath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "deploy.settings.ps1")

if (-not $ServerHost) { $ServerHost = $DeployServerHost }
if (-not $ServerUser) { $ServerUser = $DeployServerUser }
if (-not $RemotePath) { $RemotePath = $DeployRemoteJars }

$remote = "${ServerUser}@${ServerHost}"

function Test-CommandExists([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-BootJar([string]$ServiceDir) {
    $libDir = Join-Path $ServiceDir "build\libs"
    if (-not (Test-Path $libDir)) {
        return $null
    }

    $candidates = Get-ChildItem -Path $libDir -Filter "*.jar" -File |
        Where-Object { $_.Name -notmatch "-plain\.jar$" } |
        Sort-Object LastWriteTime -Descending

    return $candidates | Select-Object -First 1
}

if (-not (Test-CommandExists "ssh")) { throw "ssh command not found." }
if (-not (Test-CommandExists "scp")) { throw "scp command not found." }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " MSA JAR Copy Pipeline" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Source root : $DeployLocalRoot"
Write-Host " Target      : ${remote}:${RemotePath}"
Write-Host ""

Write-Host "[1/3] Checking remote JAR directory..." -ForegroundColor Yellow
ssh $remote "mkdir -p '$RemotePath' && chmod 755 '$RemotePath'"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create remote directory: ${remote}:${RemotePath}"
}

Write-Host "[2/3] Uploading JAR files..." -ForegroundColor Yellow

$copied = 0
$skipped = 0
$failed = 0

foreach ($service in $DeployServices) {
    $serviceDir = Join-Path $DeployLocalRoot $service
    $jar = Get-BootJar $serviceDir

    if (-not $jar) {
        Write-Warning "[SKIP] $service - JAR not found. Run build-all.bat first."
        $skipped++
        continue
    }

    $remoteName = "$service.jar"
    $remoteDest = "${remote}:${RemotePath}/${remoteName}"
    $sizeMb = [math]::Round($jar.Length / 1MB, 2)

    Write-Host ("  {0,-18} {1,8} MB  ->  {2}" -f $service, $sizeMb, $remoteName)

    & scp $jar.FullName $remoteDest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] $service" -ForegroundColor Red
        $failed++
        continue
    }

    $copied++
}

$remoteDeployDir = "/home/cwuser/msa/deploy"
Write-Host "[3/3] Uploading deploy scripts..." -ForegroundColor Yellow
ssh $remote "mkdir -p '$remoteDeployDir' && chmod 755 '$remoteDeployDir'"

$scriptFiles = @(
    (Join-Path $PSScriptRoot "server-restart-services.sh"),
    (Join-Path $PSScriptRoot "deploy.env")
)
foreach ($scriptFile in $scriptFiles) {
    if (-not (Test-Path $scriptFile)) {
        Write-Warning "[SKIP] File not found: $scriptFile"
        continue
    }
    $baseName = Split-Path $scriptFile -Leaf
    Write-Host "  -> $baseName"
    & scp $scriptFile "${remote}:${remoteDeployDir}/${baseName}"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to upload script: $baseName"
    }
}
ssh $remote "chmod +x '${remoteDeployDir}/server-restart-services.sh'"

Write-Host ""
if ($failed -gt 0) {
    throw "JAR copy failed: $failed errors occurred."
}

Write-Host "========================================" -ForegroundColor Green
Write-Host " Copy Completed (Success: $copied / Skipped: $skipped)" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next Step (Run on Remote Server):" -ForegroundColor Yellow
Write-Host "  ssh $remote"
Write-Host "  DEPLOY_MODE=recreate bash /home/cwuser/msa/deploy/server-restart-services.sh"
Write-Host ""