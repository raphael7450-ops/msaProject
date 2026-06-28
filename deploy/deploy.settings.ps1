# MSA 배포 공통 설정 (PowerShell 스크립트에서 dot-source)
# 사용: . "$PSScriptRoot\deploy.settings.ps1"

$script:DeployServerHost   = "192.168.55.223"
$script:DeployServerUser    = "cwuser"
$script:DeployRemoteConfig  = "/data/config-repo"
$script:DeployRemoteJars    = "/home/cwuser/msa/jars"
$script:DeployLocalRoot     = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:DeployLocalConfig   = Join-Path $DeployLocalRoot "config-repo"

$script:DeployServices = @(
    "eureka-server",
    "config-server",
    "gateway-service",
    "login-service",
    "user-service",
    "crm-service"
)
