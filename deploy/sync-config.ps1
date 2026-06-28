#Requires -Version 5.1
<#
.SYNOPSIS
  로컬 config-repo 를 원격 도커 서버 /data/config-repo 로 동기화합니다.

.DESCRIPTION
  C:\MSA\config-repo\*.yml → cwuser@192.168.55.223:/data/config-repo/

.PARAMETER ServerHost
  원격 서버 IP (기본: 192.168.55.223)

.PARAMETER ServerUser
  SSH 사용자 (기본: cwuser)

.PARAMETER RemotePath
  원격 config-repo 경로 (기본: /data/config-repo)

.EXAMPLE
  .\sync-config.ps1
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

if (-not $ServerHost)  { $ServerHost  = $DeployServerHost }
if (-not $ServerUser)  { $ServerUser  = $DeployServerUser }
if (-not $RemotePath)  { $RemotePath  = $DeployRemoteConfig }

$remote = "${ServerUser}@${ServerHost}"

function Test-CommandExists([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

if (-not (Test-CommandExists "ssh")) {
    throw "ssh 명령을 찾을 수 없습니다. Windows OpenSSH 클라이언트를 설치해 주세요."
}
if (-not (Test-CommandExists "scp")) {
    throw "scp 명령을 찾을 수 없습니다. Windows OpenSSH 클라이언트를 설치해 주세요."
}

if (-not (Test-Path $DeployLocalConfig)) {
    throw "로컬 config-repo 가 없습니다: $DeployLocalConfig"
}

$files = Get-ChildItem -Path $DeployLocalConfig -Filter "*.yml" -File | Sort-Object Name
if ($files.Count -eq 0) {
    throw "동기화할 yml 파일이 없습니다: $DeployLocalConfig"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " MSA Config Sync" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Source : $DeployLocalConfig"
Write-Host " Target : ${remote}:${RemotePath}"
Write-Host " Files  : $($files.Count)"
Write-Host ""

Write-Host "[1/3] 원격 디렉터리 확인..." -ForegroundColor Yellow
ssh $remote "mkdir -p '$RemotePath' && chmod 755 '$RemotePath'"
if ($LASTEXITCODE -ne 0) {
    throw "원격 디렉터리 생성 실패: ${remote}:${RemotePath}"
}

Write-Host "[2/3] SCP 업로드..." -ForegroundColor Yellow
$uploaded = 0
foreach ($file in $files) {
    $dest = "${remote}:${RemotePath}/$($file.Name)"
    Write-Host ("  -> {0}" -f $file.Name)
    & scp $file.FullName $dest
    if ($LASTEXITCODE -ne 0) {
        throw "업로드 실패: $($file.Name)"
    }
    $uploaded++
}

Write-Host "[3/3] 원격 파일 검증..." -ForegroundColor Yellow
$remoteList = ssh $remote "ls -1 '$RemotePath'/*.yml 2>/dev/null | xargs -n1 basename | sort"
if ($LASTEXITCODE -ne 0) {
    throw "원격 config-repo 검증 실패"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " 동기화 완료 ($uploaded files)" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "원격 config-repo:" -ForegroundColor DarkGray
Write-Host $remoteList -ForegroundColor DarkGray
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Yellow
Write-Host "  1) 서버 접속: ssh $remote"
Write-Host "  2) 재시작   : bash /home/cwuser/msa/deploy/server-restart-services.sh"
Write-Host "     또는     : docker restart config-server && sleep 3 && docker restart user-service gateway-service"
Write-Host ""
