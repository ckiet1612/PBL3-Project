param(
    [string]$AppName = "Sales Management System",
    [string]$AppVersion = "1.0.0",
    [string]$Vendor = "PBL3",
    [string]$MainJar = "pbl3-project-0.0.1-SNAPSHOT.jar",
    [string]$OutputDir = "dist\installers\windows",
    [string]$JPackageTempDir = "target\jpackage\windows",
    [string]$JPackageInputDir = "target\jpackage\input-windows",
    [string]$IconPath = "src\main\resources\AppIcon\AppIcon.ico",
    [string]$ProvisioningApiBaseUrl = $env:PROVISIONING_API_BASE_URL,
    [string]$ProvisioningApiKey = $env:PROVISIONING_API_KEY,
    [string]$ProvisioningApiAllowLocal = $env:PROVISIONING_API_ALLOW_LOCAL
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RootDir

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage was not found. Install/use a full JDK 21+ and make sure jpackage is on PATH."
}

if ([string]::IsNullOrWhiteSpace($ProvisioningApiBaseUrl)) {
    throw "PROVISIONING_API_BASE_URL is required for release packaging. Example: `$env:PROVISIONING_API_BASE_URL='https://provisioning.example.com'"
}

if (-not $ProvisioningApiBaseUrl.StartsWith("https://")) {
    if ($ProvisioningApiAllowLocal -ne "true") {
        throw "Release desktop packages must use an HTTPS provisioning API URL. For local/demo packaging, set PROVISIONING_API_ALLOW_LOCAL=true."
    }
    if ($ProvisioningApiBaseUrl -notmatch "^http://(localhost|127\.|0\.0\.0\.0|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)") {
        throw "Local/demo packaging only allows localhost or private LAN HTTP URLs."
    }
}

if (-not (Test-Path $IconPath)) {
    throw "Icon file not found: $IconPath"
}

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -and -not (Get-Command wix.exe -ErrorAction SilentlyContinue)) {
    Write-Warning "WiX Toolset was not found on PATH. jpackage requires WiX to build MSI installers."
}

Write-Host "Building desktop release jar..."
& .\mvnw.cmd -q -Pdesktop-release -DskipTests package

$MainJarPath = Join-Path "target" $MainJar
if (-not (Test-Path $MainJarPath)) {
    throw "Main jar not found: $MainJarPath"
}

Remove-Item -Recurse -Force $JPackageTempDir, $JPackageInputDir, $OutputDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $JPackageTempDir, $JPackageInputDir, $OutputDir | Out-Null
Copy-Item -Path $MainJarPath -Destination (Join-Path $JPackageInputDir $MainJar)

$JavaOptions = @(
    "-DAPP_DESKTOP_RELEASE=true",
    "-DPROVISIONING_API_BASE_URL=$ProvisioningApiBaseUrl",
    "-Dapp.client.version=$AppVersion",
    "-Dspring.profiles.active=tenant-client",
    "-Dspring.main.web-application-type=none"
)

if (-not [string]::IsNullOrWhiteSpace($ProvisioningApiKey)) {
    $JavaOptions += "-DPROVISIONING_API_KEY=$ProvisioningApiKey"
}

if ($ProvisioningApiAllowLocal -eq "true") {
    $JavaOptions += "-DPROVISIONING_API_ALLOW_LOCAL=true"
}

$JPackageArgs = @(
    "--type", "msi",
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--description", "Sales Management System desktop client",
    "--dest", $OutputDir,
    "--temp", $JPackageTempDir,
    "--input", $JPackageInputDir,
    "--main-jar", $MainJar,
    "--icon", $IconPath,
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser"
)

foreach ($Option in $JavaOptions) {
    $JPackageArgs += @("--java-options", $Option)
}

Write-Host "Packaging MSI..."
& jpackage @JPackageArgs

Write-Host "MSI output:"
Get-ChildItem -Path $OutputDir -Filter "*.msi" | ForEach-Object { $_.FullName }
